package com.stadiummate.service;

import com.stadiummate.model.RouteResult;
import com.stadiummate.model.RouteStep;
import com.stadiummate.model.StadiumEdge;
import com.stadiummate.model.StadiumNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Pure-Java A* pathfinding engine over the MetLife Stadium graph.
 *
 * <p><strong>Algorithm</strong>: A* with a Euclidean heuristic based on
 * SVG coordinate distance. Edge weights are adjusted at query-time by
 * applying the crowd-congestion multiplier from {@link CrowdService}.
 *
 * <p><strong>Weight formula</strong>:
 * <pre>
 *   effectiveWeight = baseWeight × (1 + 4 × congestion)
 * </pre>
 * At full congestion (1.0) an edge costs 5× its base weight, making the
 * router strongly prefer alternative paths.
 *
 * <p><strong>Heuristic</strong>: Euclidean distance in SVG pixel space scaled
 * by 0.5 (empirically tuned so h(n) ≤ actual cost, keeping A* admissible).
 *
 * <p>No AI is used in this class — Gemini does language, A* does math.
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    /** Average walking speed in metres per minute at a stadium. */
    private static final double WALK_SPEED_M_PER_MIN = 80.0;

    /**
     * Heuristic scale: converts SVG pixel distance to approximate metres.
     * 800px SVG width ≈ 400m stadium diameter → 0.5 m/px.
     */
    private static final double HEURISTIC_SCALE = 0.5;

    private final GraphService graphService;
    private final CrowdService crowdService;
    private final WeatherService weatherService;

    public RouteService(GraphService graphService, CrowdService crowdService, WeatherService weatherService) {
        this.graphService = graphService;
        this.crowdService = crowdService;
        this.weatherService = weatherService;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Computes the optimal route from {@code sourceId} to {@code destId}
     * using A* with live crowd-congestion weights.
     *
     * @param sourceId starting node ID (e.g. {@code "GATE_A"})
     * @param destId   destination node ID (e.g. {@code "REST_N_ACC"})
     * @return a {@link RouteResult} or {@code null} if no path exists
     */
    public RouteResult findRoute(String sourceId, String destId) {
        return findRoute(sourceId, destId, false);
    }

    public RouteResult findRoute(String sourceId, String destId, boolean accessibleOnly) {
        Map<String, StadiumNode> graph = graphService.getGraph();

        StadiumNode source = graph.get(sourceId);
        StadiumNode dest = graph.get(destId);

        if (source == null || dest == null) {
            log.warn("findRoute called with unknown node(s): {} → {}", sourceId, destId);
            return null;
        }
        if (sourceId.equals(destId)) {
            return buildSingleNodeResult(source);
        }

        // ── A* data structures ─────────────────────────────────────────────
        // gScore[n] = best known actual cost from source to n
        Map<String, Double> gScore = new HashMap<>();
        // fScore[n] = gScore[n] + h(n)  (priority for the open set)
        Map<String, Double> fScore = new HashMap<>();
        // cameFrom[n] = predecessor node on the best-known path to n
        Map<String, String> cameFrom = new HashMap<>();
        // edgeType[n] = type of the edge used to reach n (for step icons)
        Map<String, String> edgeTypeUsed = new HashMap<>();
        // edgeWeight[n] = base weight of edge used to reach n (for real distance)
        Map<String, Double> edgeBaseWeight = new HashMap<>();

        PriorityQueue<NodeEntry> openSet =
                new PriorityQueue<>(Comparator.comparingDouble(e -> e.fScore));
        Set<String> closedSet = new HashSet<>();

        gScore.put(sourceId, 0.0);
        fScore.put(sourceId, heuristic(source, dest));
        openSet.add(new NodeEntry(sourceId, fScore.get(sourceId)));

        // ── Main A* loop ───────────────────────────────────────────────────
        while (!openSet.isEmpty()) {
            NodeEntry current = openSet.poll();
            String currentId = current.nodeId;

            if (closedSet.contains(currentId)) continue;

            if (currentId.equals(destId)) {
                log.debug("A* found path: {} → {} (g={:.1f})",
                        sourceId, destId, gScore.get(destId));
                return buildRouteResult(cameFrom, edgeTypeUsed, edgeBaseWeight,
                        destId, graph);
            }

            closedSet.add(currentId);

            StadiumNode currentNode = graph.get(currentId);
            if (currentNode == null || currentNode.getEdges() == null) continue;

            for (StadiumEdge edge : currentNode.getEdges()) {
                String neighborId = edge.getTo();
                if (closedSet.contains(neighborId)) continue;

                StadiumNode neighbor = graph.get(neighborId);
                if (neighbor == null) continue;

                if (accessibleOnly && !neighbor.isAccessible()) {
                    continue; // Strict accessibility routing
                }

                // Apply crowd multiplier based on the destination zone
                double multiplier = crowdService.getWeightMultiplier(neighbor.getZone());
                double effectiveWeight = edge.getBaseWeight() * multiplier;

                // Apply weather penalty for uncovered paths
                if (weatherService.isRaining() && !edge.isCovered()) {
                    effectiveWeight *= 5.0; // 5x penalty for rain
                }

                double tentativeG = gScore.getOrDefault(currentId, Double.MAX_VALUE)
                        + effectiveWeight;

                if (tentativeG < gScore.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    cameFrom.put(neighborId, currentId);
                    edgeTypeUsed.put(neighborId, edge.getType());
                    edgeBaseWeight.put(neighborId, edge.getBaseWeight());
                    gScore.put(neighborId, tentativeG);
                    double h = heuristic(neighbor, dest);
                    fScore.put(neighborId, tentativeG + h);
                    openSet.add(new NodeEntry(neighborId, fScore.get(neighborId)));
                }
            }
        }

        log.warn("A* found no path from {} to {}", sourceId, destId);
        return null;
    }

    /**
     * Finds the nearest reachable node of a given type from the source,
     * using A* to each candidate and returning the shortest result.
     *
     * @param sourceId       starting node
     * @param type           destination type (e.g. {@code "restroom"})
     * @param accessibleOnly restrict to accessible destinations
     * @return best route, or {@code null} if no candidates are reachable
     */
    public RouteResult findNearestByType(String sourceId, String type,
                                         boolean accessibleOnly) {
        List<StadiumNode> candidates = graphService.findByType(type, accessibleOnly);
        if (candidates.isEmpty()) return null;

        RouteResult best = null;
        for (StadiumNode candidate : candidates) {
            RouteResult route = findRoute(sourceId, candidate.getNodeId(), accessibleOnly);
            if (route != null) {
                if (best == null || route.getTotalDistance() < best.getTotalDistance()) {
                    best = route;
                }
            }
        }
        return best;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private double heuristic(StadiumNode a, StadiumNode b) {
        double dx = a.getSvgX() - b.getSvgX();
        double dy = a.getSvgY() - b.getSvgY();
        return Math.sqrt(dx * dx + dy * dy) * HEURISTIC_SCALE;
    }

    /**
     * Reconstructs the path by walking the {@code cameFrom} map backwards,
     * then assembles a {@link RouteResult} with steps and distance totals.
     */
    private RouteResult buildRouteResult(Map<String, String> cameFrom,
                                          Map<String, String> edgeTypeUsed,
                                          Map<String, Double> edgeBaseWeight,
                                          String endId,
                                          Map<String, StadiumNode> graph) {
        // Reconstruct path
        List<String> path = new ArrayList<>();
        String current = endId;
        while (current != null) {
            path.add(0, current);
            current = cameFrom.get(current);
        }

        // Build steps
        List<RouteStep> steps = new ArrayList<>();
        double totalDistance = 0.0;
        boolean rerouted = false;

        for (int i = 0; i < path.size(); i++) {
            String nodeId = path.get(i);
            StadiumNode node = graph.get(nodeId);
            double dist = (i == 0) ? 0.0 : edgeBaseWeight.getOrDefault(nodeId, 0.0);
            String edgeType = (i == 0) ? "walk" : edgeTypeUsed.getOrDefault(nodeId, "walk");
            totalDistance += dist;

            // Detect if any congested zone was traversed
            if (crowdService.isHighlyCongested(node.getZone())) {
                rerouted = true;
            }

            steps.add(RouteStep.builder()
                    .nodeId(nodeId)
                    .nodeName(node.getName())
                    .instruction(buildInstruction(node, i, path.size(), edgeType))
                    .edgeType(edgeType)
                    .distanceFromPrevious(dist)
                    .build());
        }

        int estimatedMinutes = (int) Math.max(1, Math.ceil(totalDistance / WALK_SPEED_M_PER_MIN));

        return RouteResult.builder()
                .steps(steps)
                .totalDistance(totalDistance)
                .estimatedMinutes(estimatedMinutes)
                .nodePath(path)
                .rerouted(rerouted)
                .build();
    }

    /**
     * Generates a concise English step instruction.
     * The final localised narration is produced by Gemini — these instructions
     * are only used as structured context in the reply-builder prompt.
     */
    private String buildInstruction(StadiumNode node, int stepIndex, int totalSteps,
                                     String edgeType) {
        if (stepIndex == 0) {
            return "Start at " + node.getName();
        }
        if (stepIndex == totalSteps - 1) {
            return "Arrive at " + node.getName();
        }
        return switch (edgeType) {
            case "elevator" -> "Take the elevator at " + node.getName();
            case "ramp"     -> "Use the ramp at " + node.getName();
            default         -> "Continue to " + node.getName();
        };
    }

    private RouteResult buildSingleNodeResult(StadiumNode node) {
        RouteStep step = RouteStep.builder()
                .nodeId(node.getNodeId())
                .nodeName(node.getName())
                .instruction("You are already at " + node.getName())
                .edgeType("walk")
                .distanceFromPrevious(0)
                .build();
        return RouteResult.builder()
                .steps(List.of(step))
                .totalDistance(0)
                .estimatedMinutes(0)
                .nodePath(List.of(node.getNodeId()))
                .rerouted(false)
                .build();
    }

    // ─── Inner class ──────────────────────────────────────────────────────────

    /** Priority-queue entry for the A* open set. */
    private record NodeEntry(String nodeId, double fScore) {}
}
