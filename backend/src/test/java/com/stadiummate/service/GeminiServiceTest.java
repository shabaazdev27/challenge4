package com.stadiummate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stadiummate.model.IntentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GeminiService} — validates intent parsing logic
 * and narration building using a mocked {@link WebClient}.
 *
 * <p>The Gemini REST API is fully mocked here — no real API calls are made.
 * Tests verify JSON extraction, fallback behaviour, and language detection.
 */
@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.ResponseSpec responseSpec;

    private GeminiService geminiService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        geminiService = new GeminiService(webClient, mapper, "test-api-key", "gemini-2.0-flash");

        // Wire up the WebClient mock chain
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    // ─── Intent parsing ───────────────────────────────────────────────────────

    @Test
    @DisplayName("English restroom request is parsed correctly")
    @SuppressWarnings("unchecked")
    void parseIntent_englishRestroomRequest() {
        String geminiJson = buildGeminiResponse("""
                {"destinationType":"restroom_accessible","destinationId":null,
                 "sourceNodeId":null,"language":"en","urgency":"normal"}
                """);

        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(geminiJson));

        IntentResult result = geminiService.parseIntent(
                "Where is the nearest accessible restroom to Section 214?");

        assertThat(result.getDestinationType()).isEqualTo("restroom_accessible");
        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getUrgency()).isEqualTo("normal");
    }

    @Test
    @DisplayName("Spanish food court request detects Spanish language")
    @SuppressWarnings("unchecked")
    void parseIntent_spanishFoodRequest() {
        String geminiJson = buildGeminiResponse("""
                {"destinationType":"food","destinationId":null,
                 "sourceNodeId":null,"language":"es","urgency":"normal"}
                """);

        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(geminiJson));

        IntentResult result = geminiService.parseIntent("¿Dónde están los puestos de comida?");

        assertThat(result.getLanguage()).isEqualTo("es");
        assertThat(result.getDestinationType()).isEqualTo("food");
    }

    @Test
    @DisplayName("Section 214 request resolves specific destination ID")
    @SuppressWarnings("unchecked")
    void parseIntent_specificSectionId() {
        String geminiJson = buildGeminiResponse("""
                {"destinationType":"section","destinationId":"SEC_214",
                 "sourceNodeId":"GATE_A","language":"en","urgency":"normal"}
                """);

        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(geminiJson));

        IntentResult result = geminiService.parseIntent(
                "How do I get from Gate A to Section 214?");

        assertThat(result.getDestinationId()).isEqualTo("SEC_214");
        assertThat(result.getSourceNodeId()).isEqualTo("GATE_A");
    }

    @Test
    @DisplayName("Gemini API failure returns fallback intent without throwing")
    @SuppressWarnings("unchecked")
    void parseIntent_apiFailure_returnsFallback() {
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("API unavailable")));

        IntentResult result = geminiService.parseIntent("Where is Gate B?");

        // Fallback should not be null and should have a default language
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isNotNull();
    }

    @Test
    @DisplayName("Gemini response wrapped in markdown fences is stripped correctly")
    @SuppressWarnings("unchecked")
    void parseIntent_stripMarkdownFences() {
        String geminiJson = buildGeminiResponse("""
                ```json
                {"destinationType":"firstaid","destinationId":"FIRSTAID",
                 "sourceNodeId":null,"language":"en","urgency":"urgent"}
                ```
                """);

        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(geminiJson));

        IntentResult result = geminiService.parseIntent("I need first aid urgently!");

        assertThat(result.getDestinationType()).isEqualTo("firstaid");
        assertThat(result.getUrgency()).isEqualTo("urgent");
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Wraps a text payload in a Gemini API response JSON envelope.
     */
    private String buildGeminiResponse(String text) {
        // Escape text for embedding in JSON
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n");
        return """
                {"candidates":[{"content":{"parts":[{"text":"%s"}],"role":"model"}}]}
                """.formatted(escaped.strip());
    }
}
