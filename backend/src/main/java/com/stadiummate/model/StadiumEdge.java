package com.stadiummate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A directed edge in the stadium graph connecting two nodes.
 *
 * <p>The {@code baseWeight} is the raw walking distance in metres.
 * The {@link com.stadiummate.service.RouteService} applies a crowd-congestion
 * multiplier to produce the effective weight used by A*.
 *
 * <p>Edge types:
 * <ul>
 *   <li>{@code walk}     — flat concourse, no special equipment</li>
 *   <li>{@code elevator} — accessible lift (weight includes wait time)</li>
 *   <li>{@code ramp}     — inclined walkway to/from upper level</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StadiumEdge {

    /** Target node ID of this directed edge. */
    @JsonProperty("to")
    private String to;

    /**
     * Base traversal weight in metres — the un-adjusted walking distance.
     * Crowd multiplier is applied at route-planning time, not stored here.
     */
    @JsonProperty("baseWeight")
    private double baseWeight;

    /**
     * Edge type: {@code walk}, {@code elevator}, or {@code ramp}.
     * Used by the frontend to render appropriate turn-by-turn icons.
     */
    @JsonProperty("type")
    private String type;

    @Builder.Default
    private boolean isAccessible = true;

    /** Whether this edge path is covered or exposed to weather. */
    @JsonProperty("isCovered")
    @Builder.Default
    private boolean isCovered = true;
}
