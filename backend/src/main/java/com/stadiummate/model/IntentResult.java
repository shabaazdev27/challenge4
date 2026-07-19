package com.stadiummate.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured intent extracted by Gemini 2.0 Flash from the fan's raw message.
 *
 * <p>Gemini is prompted to return this as JSON (responseMimeType:
 * application/json). The backend then uses it to look up the destination
 * node and run A* pathfinding.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntentResult {

    /**
     * Broad category of the destination.
     * One of: gate | restroom_accessible | restroom | food | elevator |
     *         firstaid | merchandise | section | team_entry | unknown
     */
    @JsonProperty("destinationType")
    private String destinationType;

    /**
     * Specific node ID when Gemini can resolve it unambiguously.
     * Example: {@code "SEC_214"}, {@code "GATE_B"}.
     * Null means "find the nearest node of {@code destinationType}".
     */
    @JsonProperty("destinationId")
    private String destinationId;

    /**
     * Source node if the fan mentioned their current location in the message.
     * Null means use the {@code currentLocation} from the request, or default.
     */
    @JsonProperty("sourceNodeId")
    private String sourceNodeId;

    /**
     * ISO 639-1 language code detected from the fan's message.
     * This drives both the reply-builder prompt and the TTS voice selection
     * on the frontend.
     */
    @JsonProperty("language")
    private String language;

    /**
     * Urgency level — {@code "urgent"} when the fan's message implies
     * time-sensitivity (e.g. medical need, gate closing). Adjusts narration tone.
     */
    @JsonProperty("urgency")
    private String urgency;
}
