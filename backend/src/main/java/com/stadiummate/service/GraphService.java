package com.stadiummate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.stadiummate.model.StadiumNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Loads and caches the MetLife Stadium node graph in memory.
 *
 * <p>Data source strategy (controlled by {@code stadiummate.graph.source}):
 * <ul>
 *   <li>{@code local}    — loads {@code classpath:data/metlife-graph.json} (fast, no GCP auth needed)</li>
 *   <li>{@code firestore} — loads from Firestore collection {@code stadium_graph}</li>
 * </ul>
 *
 * <p>The in-memory map is populated once at startup ({@link #loadGraph()})
 * and accessed read-only by {@link RouteService} and {@link GeminiService}.
 * For a production system this would be refreshed periodically;
 * for the hackathon demo static load is sufficient.
 */
@Service
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);
    private static final String COLLECTION = "stadium_graph";

    private final Firestore firestore;
    private final ObjectMapper mapper;
    private final String graphSource;

    /** Thread-safe in-memory map: nodeId → StadiumNode. */
    private final Map<String, StadiumNode> graph = new ConcurrentHashMap<>();

    public GraphService(Firestore firestore,
                        ObjectMapper mapper,
                        @Value("${stadiummate.graph.source:local}") String graphSource) {
        this.firestore = firestore;
        this.mapper = mapper;
        this.graphSource = graphSource;
    }

    // ─── Startup ─────────────────────────────────────────────────────────────

    /**
     * Loads the stadium graph at application startup.
     * Fails fast so the service never starts with an empty graph.
     */
    @PostConstruct
    public void loadGraph() {
        try {
            if ("firestore".equalsIgnoreCase(graphSource)) {
                loadFromFirestore();
            } else {
                loadFromLocalJson();
            }
            log.info("Stadium graph loaded: {} nodes", graph.size());
        } catch (Exception e) {
            log.error("Failed to load stadium graph — falling back to local JSON", e);
            try {
                loadFromLocalJson();
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot load stadium graph", ex);
            }
        }
    }

    private void loadFromLocalJson() throws Exception {
        ClassPathResource resource = new ClassPathResource("data/metlife-graph.json");
        try (InputStream is = resource.getInputStream()) {
            List<StadiumNode> nodes = mapper.readValue(is, new TypeReference<>() {});
            nodes.forEach(n -> graph.put(n.getNodeId(), n));
            log.info("Loaded {} nodes from local JSON", nodes.size());
        }
    }

    private void loadFromFirestore() throws Exception {
        List<QueryDocumentSnapshot> docs =
                firestore.collection(COLLECTION).get().get().getDocuments();
        for (DocumentSnapshot doc : docs) {
            StadiumNode node = mapper.convertValue(doc.getData(), StadiumNode.class);
            graph.put(node.getNodeId(), node);
        }
        log.info("Loaded {} nodes from Firestore", docs.size());
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    /**
     * Returns the full in-memory graph (read-only view).
     *
     * @return map of nodeId → {@link StadiumNode}
     */
    public Map<String, StadiumNode> getGraph() {
        return Map.copyOf(graph);
    }

    /**
     * Looks up a single node by ID.
     *
     * @param nodeId the node identifier, e.g. {@code "GATE_A"}
     * @return the {@link StadiumNode} or {@code null} if not found
     */
    public StadiumNode getNode(String nodeId) {
        return graph.get(nodeId);
    }

    /**
     * Finds all nodes matching the given destination type.
     *
     * @param type          node type string (e.g. {@code "restroom"})
     * @param accessibleOnly when {@code true} only returns accessible nodes
     * @return list of matching nodes
     */
    public List<StadiumNode> findByType(String type, boolean accessibleOnly) {
        return graph.values().stream()
                .filter(n -> n.getType().equalsIgnoreCase(type))
                .filter(n -> !accessibleOnly || n.isAccessible())
                .collect(Collectors.toList());
    }

    /**
     * Finds the IDs of all nodes — used to build the destination list
     * injected into Gemini's intent-parsing prompt.
     */
    public List<String> getAllNodeIds() {
        return List.copyOf(graph.keySet());
    }
}
