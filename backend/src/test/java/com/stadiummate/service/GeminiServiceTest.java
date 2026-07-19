package com.stadiummate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stadiummate.model.IntentResult;
import com.stadiummate.model.RouteResult;
import com.stadiummate.model.RouteStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GeminiService} — validates intent parsing logic
 * and narration building using a mocked {@link WebClient}.
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
    void parseIntent_apiFailure_returnsFallback() {
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("API unavailable")));

        IntentResult result = geminiService.parseIntent("Where is Gate B?");

        // Fallback should not be null and should have a default language
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getDestinationType()).isEqualTo("gate");
        assertThat(result.getDestinationId()).isEqualTo("GATE_B");
    }

    @Test
    @DisplayName("Gemini response wrapped in markdown fences is stripped correctly")
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

    @Test
    @DisplayName("Test buildNarration and fallback options")
    void testBuildNarration() {
        String mockResultText = "Walk forward for 10 meters and turn left.";
        String geminiJson = buildGeminiResponse(mockResultText);

        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(geminiJson));

        RouteResult route = RouteResult.builder()
                .steps(List.of(RouteStep.builder().instruction("Walk 10m").nodeName("Concourse").distanceFromPrevious(10.0).build()))
                .totalDistance(10.0)
                .estimatedMinutes(1)
                .build();

        String narration = geminiService.buildNarration("How to walk?", "en", route, List.of("ZONE1"), "normal");
        assertThat(narration).isEqualTo(mockResultText);
    }

    @Test
    @DisplayName("Test buildNarration fallback when API fails")
    void testBuildNarrationFallback() {
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.empty());

        RouteResult route = RouteResult.builder()
                .steps(List.of(RouteStep.builder().instruction("Walk 10m").nodeName("Concourse").distanceFromPrevious(10.0).build()))
                .totalDistance(10.0)
                .estimatedMinutes(1)
                .build();

        // English route fallback
        String narrationEn = geminiService.buildNarration("How to walk?", "en", route, List.of(), "normal");
        assertThat(narrationEn).contains("Concourse");

        // Spanish route fallback
        String narrationEs = geminiService.buildNarration("¿Cómo caminar?", "es", route, List.of(), "normal");
        assertThat(narrationEs).contains("Concourse");

        // Portuguese route fallback
        String narrationPt = geminiService.buildNarration("Como andar?", "pt", route, List.of(), "normal");
        assertThat(narrationPt).contains("Concourse");

        // French route fallback
        String narrationFr = geminiService.buildNarration("Comment marcher?", "fr", route, List.of(), "normal");
        assertThat(narrationFr).contains("Concourse");

        // No route fallback
        String fallbackEn = geminiService.buildNarration("Hello", "en", null, List.of(), "normal");
        assertThat(fallbackEn).contains("find a route");

        String fallbackEs = geminiService.buildNarration("Hola", "es", null, List.of(), "normal");
        assertThat(fallbackEs).contains("encontrar");

        String fallbackPt = geminiService.buildNarration("Ola", "pt", null, List.of(), "normal");
        assertThat(fallbackPt).contains("encontrar");

        String fallbackFr = geminiService.buildNarration("Salut", "fr", null, List.of(), "normal");
        assertThat(fallbackFr).contains("trouver");
    }

    @Test
    @DisplayName("Test generateGamificationDescription and fallback")
    void testGenerateGamificationDescription() {
        String mockResponse = "Amazing! You found a scarf at Gate A.";
        String geminiJson = buildGeminiResponse(mockResponse);

        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(geminiJson));

        String desc = geminiService.generateGamificationDescription("scarf", "Gate A");
        assertThat(desc).isEqualTo(mockResponse);

        // Fallback case when webClient returns empty
        reset(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.empty());
        String fallbackDesc = geminiService.generateGamificationDescription("scarf", "Gate A");
        assertThat(fallbackDesc).contains("scarf");
    }

    @Test
    @DisplayName("Test fallback intent language detections")
    void testFallbackIntentLanguageDetections() {
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.empty());

        IntentResult resultFr = geminiService.parseIntent("où est la toilette?");
        assertThat(resultFr.getLanguage()).isEqualTo("fr");

        IntentResult resultEs = geminiService.parseIntent("dónde está el baño accesible?");
        assertThat(resultEs.getLanguage()).isEqualTo("es");
        assertThat(resultFr.getDestinationType()).isEqualTo("restroom");
        assertThat(resultEs.getDestinationType()).isEqualTo("restroom_accessible");

        IntentResult resultPt = geminiService.parseIntent("onde fica o banheiro?");
        assertThat(resultPt.getLanguage()).isEqualTo("pt");

        IntentResult resultFood = geminiService.parseIntent("I am hungry for pizza");
        assertThat(resultFood.getDestinationType()).isEqualTo("food");

        IntentResult resultFirstAid = geminiService.parseIntent("I need a doctor or medical assistance");
        assertThat(resultFirstAid.getDestinationType()).isEqualTo("firstaid");

        IntentResult resultMerch = geminiService.parseIntent("where is the official store?");
        assertThat(resultMerch.getDestinationType()).isEqualTo("merchandise");

        IntentResult resultElevator = geminiService.parseIntent("where is the elevator?");
        assertThat(resultElevator.getDestinationType()).isEqualTo("elevator");

        IntentResult resultTeam = geminiService.parseIntent("where is the player team entry?");
        assertThat(resultTeam.getDestinationType()).isEqualTo("team_entry");

        IntentResult resultSection = geminiService.parseIntent("where is section 114 or 120 or 214 or 320?");
        assertThat(resultSection.getDestinationType()).isEqualTo("section");

        IntentResult resultGate = geminiService.parseIntent("where is gate a or gate b or gate c or gate d?");
        assertThat(resultGate.getDestinationType()).isEqualTo("gate");
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
