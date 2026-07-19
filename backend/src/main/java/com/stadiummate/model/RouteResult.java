package com.stadiummate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The full result of an A* routing computation.
 *
 * <p>Included in every {@link ChatResponse} when a route is found.
 * {@code nodePath} is the ordered list of node IDs — the frontend uses this
 * to draw the animated route overlay on the SVG stadium map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResult {

    /** Ordered list of turn-by-turn steps from source to destination. */
    @JsonProperty("steps")
    private List<RouteStep> steps;

    /**
     * Total actual walking distance in metres (un-weighted by crowd).
     * Used for the distance badge in the UI.
     */
    @JsonProperty("totalDistance")
    private double totalDistance;

    /**
     * Estimated walking time in minutes, calculated at 80 m/min.
     * Minimum value is 1 minute.
     */
    @JsonProperty("estimatedMinutes")
    private int estimatedMinutes;

    /**
     * Ordered list of node IDs representing the computed path.
     * The frontend iterates this to render the animated route line on the
     * SVG map by joining the corresponding SVG (x, y) coordinates.
     */
    @JsonProperty("nodePath")
    private List<String> nodePath;

    /**
     * Whether any high-congestion zone was detected and avoided during routing.
     * When true, the response will also contain a {@code congestionWarning}.
     */
    @JsonProperty("rerouted")
    private boolean rerouted;
}
