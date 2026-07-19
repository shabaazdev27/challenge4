package com.stadiummate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound request body for the {@code POST /api/chat} endpoint.
 *
 * <p>The {@code currentLocation} field is optional — if omitted the backend
 * defaults the source node to {@code GATE_A} (main east entrance).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * The fan's natural-language message in any language.
     * Gemini will detect the language automatically.
     */
    @NotBlank(message = "Message must not be blank")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    @JsonProperty("message")
    private String message;

    /**
     * Opaque session identifier — generated client-side (UUID v4) and stored
     * in {@code localStorage}. Used to maintain conversation context in Firestore.
     */
    @JsonProperty("sessionId")
    private String sessionId;

    /**
     * Optional node ID representing the fan's current location in the stadium.
     * Example: {@code "GATE_A"}, {@code "SEC_214"}.
     * Defaults to {@code "GATE_A"} when null or empty.
     */
    @JsonProperty("currentLocation")
    private String currentLocation;
}
