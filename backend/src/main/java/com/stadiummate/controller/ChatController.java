package com.stadiummate.controller;

import com.stadiummate.model.*;
import com.stadiummate.service.CrowdService;
import com.stadiummate.service.GeminiService;
import com.stadiummate.service.GraphService;
import com.stadiummate.service.RouteService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the main chat endpoint.
 *
 * <p>Request flow for {@code POST /api/chat}:
 * <ol>
 *   <li>Refresh crowd-congestion cache from Firestore</li>
 *   <li>Call Gemini to parse intent → {@link IntentResult}</li>
 *   <li>Resolve source and destination node IDs</li>
 *   <li>Run A* pathfinding with live crowd weights → {@link RouteResult}</li>
 *   <li>Detect rerouted zones for crowd warning</li>
 *   <li>Call Gemini to build localised narration</li>
 *   <li>Return {@link ChatResponse} (narration + route + crowd state)</li>
 * </ol>
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final String DEFAULT_SOURCE = "GATE_A";

    private final GeminiService geminiService;
    private final RouteService routeService;
    private final GraphService graphService;
    private final CrowdService crowdService;

    public ChatController(GeminiService geminiService,
                          RouteService routeService,
                          GraphService graphService,
                          CrowdService crowdService) {
        this.geminiService = geminiService;
        this.routeService = routeService;
        this.graphService = graphService;
        this.crowdService = crowdService;
    }

    // ─── POST /api/chat ───────────────────────────────────────────────────────

    /**
     * Processes a fan's navigation request and returns a localised route + narration.
     *
     * @param request fan message, optional session ID, optional current location
     * @return localised narration, route steps, crowd state
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request: sessionId={}, location={}, message={}",
                request.getSessionId(), request.getCurrentLocation(),
                truncate(request.getMessage(), 80));

        // ── 1. Refresh crowd data ──────────────────────────────────────────
        crowdService.refreshCache();

        // ── 2. Parse intent via Gemini ─────────────────────────────────────
        IntentResult intent = geminiService.parseIntent(request.getMessage());
        log.debug("Intent: type={}, dest={}, lang={}", intent.getDestinationType(),
                intent.getDestinationId(), intent.getLanguage());

        // ── 3. Resolve source node ─────────────────────────────────────────
        String sourceId = resolveSource(request.getCurrentLocation(), intent.getSourceNodeId());

        // ── 4. Resolve destination node and run A* ─────────────────────────
        RouteResult route = resolveRouteForIntent(intent, sourceId);

        // ── 5. Detect congested zones on the route ─────────────────────────
        List<String> congestedZones = detectCongestedZones(route);
        String congestionWarning = null;
        if (!congestedZones.isEmpty() && route != null && route.isRerouted()) {
            congestionWarning = buildCongestionWarning(congestedZones, intent.getLanguage());
        }

        // ── 6. Build localised narration via Gemini ────────────────────────
        String narration = geminiService.buildNarration(
                request.getMessage(),
                intent.getLanguage(),
                route,
                congestedZones,
                intent.getUrgency()
        );

        // ── 7. Assemble and return response ───────────────────────────────
        String sessionId = request.getSessionId() != null && !request.getSessionId().isBlank()
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        ChatResponse response = ChatResponse.builder()
                .narration(narration)
                .language(intent.getLanguage())
                .route(route)
                .congestionWarning(congestionWarning)
                .crowdState(crowdService.getAllCongestionLevels())
                .sessionId(sessionId)
                .build();

        return ResponseEntity.ok(response);
    }

    // ─── GET /api/chat/health ─────────────────────────────────────────────────

    /**
     * Lightweight health check that confirms the graph is loaded.
     * Separate from the Spring Actuator /actuator/health for quick demo verification.
     */
    @GetMapping("/chat/health")
    public ResponseEntity<String> health() {
        int nodeCount = graphService.getAllNodeIds().size();
        return ResponseEntity.ok("StadiumMate OK — " + nodeCount + " nodes loaded");
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String resolveSource(String requestLocation, String intentSource) {
        if (intentSource != null && graphService.getNode(intentSource) != null) {
            return intentSource;
        }
        if (requestLocation != null && !requestLocation.isBlank()
                && graphService.getNode(requestLocation) != null) {
            return requestLocation;
        }
        return DEFAULT_SOURCE;
    }

    private RouteResult resolveRouteForIntent(IntentResult intent, String sourceId) {
        String type = intent.getDestinationType();
        String destId = intent.getDestinationId();

        if (type == null || "unknown".equalsIgnoreCase(type)) {
            return null;
        }

        // Use specific node if Gemini resolved one
        if (destId != null && !destId.isBlank()
                && graphService.getNode(destId) != null) {
            return routeService.findRoute(sourceId, destId);
        }

        // Otherwise find the nearest node of the requested type
        boolean accessible = "restroom_accessible".equalsIgnoreCase(type)
                || "elevator".equalsIgnoreCase(type);
        String normalizedType = "restroom_accessible".equalsIgnoreCase(type)
                ? "restroom" : type;
        return routeService.findNearestByType(sourceId, normalizedType, accessible);
    }

    private List<String> detectCongestedZones(RouteResult route) {
        if (route == null || route.getNodePath() == null) return List.of();
        List<String> congested = new ArrayList<>();
        for (String nodeId : route.getNodePath()) {
            StadiumNode node = graphService.getNode(nodeId);
            if (node != null && crowdService.isHighlyCongested(node.getZone())
                    && !congested.contains(node.getZone())) {
                congested.add(node.getZone());
            }
        }
        return congested;
    }

    private String buildCongestionWarning(List<String> zones, String language) {
        // Simple English warning — Gemini narration will localise this contextually
        return "High congestion detected in: " + String.join(", ", zones)
                + ". Route adjusted for your comfort.";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
