package com.stadiummate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single location node in the MetLife Stadium graph.
 *
 * <p>Nodes are persisted in Firestore (collection: {@code stadium_graph})
 * and loaded into an in-memory map at startup. The {@code svgX}/{@code svgY}
 * coordinates map directly to the SVG viewport used by the frontend map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StadiumNode {

    /** Unique identifier — e.g. {@code GATE_A}, {@code REST_N_ACC}. */
    @JsonProperty("nodeId")
    private String nodeId;

    /**
     * Functional type — drives both routing logic and frontend icon selection.
     * Values: gate | concourse | restroom | food | elevator | ramp |
     *         firstaid | merchandise | section | team_entry
     */
    @JsonProperty("type")
    private String type;

    /** Human-readable display name shown in chat narrations and route steps. */
    @JsonProperty("name")
    private String name;

    /** Building floor (1 = main concourse, 2 = upper concourse). */
    @JsonProperty("floor")
    private int floor;

    /**
     * SVG x-coordinate for this node in the 800 × 600 stadium map viewport.
     * Used by the heuristic function and frontend rendering.
     */
    @JsonProperty("svgX")
    private double svgX;

    /**
     * SVG y-coordinate for this node in the 800 × 600 stadium map viewport.
     */
    @JsonProperty("svgY")
    private double svgY;

    /**
     * Crowd zone this node belongs to.
     * The {@link com.stadiummate.service.CrowdService} provides congestion
     * multipliers keyed by zone, which are applied to edges by the route engine.
     */
    @JsonProperty("zone")
    private String zone;

    /** Whether this node is accessible for wheelchair users. */
    @JsonProperty("accessible")
    private boolean accessible;

    /**
     * Search keywords — used by the intent parser to resolve natural-language
     * references (e.g. "bathroom" → restroom nodes).
     */
    @JsonProperty("keywords")
    private List<String> keywords;

    /** Outgoing edges from this node to adjacent nodes. */
    @JsonProperty("edges")
    private List<StadiumEdge> edges;

    /** Gamification items spawned at this node (e.g. mascot). */
    @JsonProperty("spawnPoints")
    private List<String> spawnPoints;
}
