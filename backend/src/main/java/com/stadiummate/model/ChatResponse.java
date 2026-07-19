package com.stadiummate.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response body returned by {@code POST /api/chat}.
 *
 * <p>Contains the Gemini-generated localised narration, the computed route,
 * an optional crowd warning, and the current zone congestion snapshot — so
 * the frontend can update the map heat overlay in a single round-trip.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    /**
     * Friendly, localised navigation narration produced by Gemini 2.0 Flash.
     * Always in the fan's detected language.
     */
    @JsonProperty("narration")
    private String narration;

    /**
     * BCP-47 / ISO 639-1 language code detected from the fan's message.
     * Examples: {@code "en"}, {@code "es"}, {@code "fr"}, {@code "pt"}.
     */
    @JsonProperty("language")
    private String language;

    /** The computed route — null if no navigable destination was found. */
    @JsonProperty("route")
    private RouteResult route;

    /**
     * Human-readable crowd warning in the fan's language, e.g.
     * "Gate C is currently busy — I've rerouted you via the south concourse."
     * Null when no significant congestion affects the route.
     */
    @JsonProperty("congestionWarning")
    private String congestionWarning;

    /**
     * Current congestion levels for all zones (0.0 – 1.0).
     * Used by the frontend to render the real-time heat overlay on the map.
     */
    @JsonProperty("crowdState")
    private Map<String, Double> crowdState;

    /** Session identifier echoed back to the client for continuity. */
    @JsonProperty("sessionId")
    private String sessionId;
}
