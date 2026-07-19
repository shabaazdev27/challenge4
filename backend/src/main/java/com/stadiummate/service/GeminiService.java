package com.stadiummate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stadiummate.model.IntentResult;
import com.stadiummate.model.RouteResult;
import com.stadiummate.model.RouteStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gemini 2.0 Flash integration via the REST API.
 *
 * <p>Two calls are made per chat request:
 * <ol>
 *   <li><strong>Intent parsing</strong> — JSON-mode structured output to extract
 *       destination type, destination ID, detected language, and urgency.</li>
 *   <li><strong>Reply builder</strong> — free-form narration in the fan's
 *       language, given the computed route steps and crowd context.</li>
 * </ol>
 *
 * <p>Using the REST API (not the Java SDK) keeps the dependency tree simple
 * and avoids version conflicts with firebase-admin. The API key is passed
 * as a query parameter; when running on Cloud Run with Vertex AI, swap to
 * Application Default Credentials and the Vertex AI endpoint instead.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;

    public GeminiService(WebClient geminiWebClient,
                         ObjectMapper mapper,
                         @Value("${stadiummate.gemini.api-key:}") String apiKey,
                         @Value("${stadiummate.gemini.model:gemini-2.0-flash}") String model) {
        this.webClient = geminiWebClient;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    // ─── Intent Parsing ───────────────────────────────────────────────────────

    /**
     * Sends the fan's raw message to Gemini with a structured-output (JSON-mode)
     * prompt, and parses the response into an {@link IntentResult}.
     *
     * <p>If the API call fails or returns malformed JSON, a safe fallback
     * {@link IntentResult} is returned so navigation degrades gracefully.
     *
     * @param userMessage the fan's raw text (any language)
     * @return parsed intent, never {@code null}
     */
    public IntentResult parseIntent(String userMessage) {
        String prompt = buildIntentPrompt(userMessage);
        String rawJson = callGemini(prompt, true);

        try {
            if (rawJson == null || rawJson.isBlank()) {
                throw new RuntimeException("Empty response from Gemini");
            }
            // Gemini may wrap the JSON in markdown fences — strip them
            String cleaned = extractJsonFromMarkdown(rawJson);
            return mapper.readValue(cleaned, IntentResult.class);
        } catch (Exception e) {
            log.warn("Intent parse failed — using fallback. Raw: {}", rawJson, e);
            return buildFallbackIntent(userMessage);
        }
    }

    /**
     * Generates a friendly, localised narration for the computed route.
     *
     * @param userMessage      the original fan message (for tone matching)
     * @param language         detected language code (e.g. {@code "es"})
     * @param route            the A*-computed route
     * @param congestionZones  set of highly-congested zone IDs (may be empty)
     * @param urgency          {@code "urgent"} or {@code "normal"}
     * @return narration string in the fan's language, never {@code null}
     */
    public String buildNarration(String userMessage, String language,
                                  RouteResult route,
                                  List<String> congestionZones,
                                  String urgency) {
        String prompt = buildNarrationPrompt(userMessage, language, route,
                congestionZones, urgency);
        String response = callGemini(prompt, false);
        return response != null ? response.strip() : fallbackNarration(language, route);
    }

    public String generateGamificationDescription(String itemType, String nodeName) {
        String prompt = """
                You are a fun gamification engine for StadiumMate at MetLife Stadium.
                A fan just collected a virtual item!
                Item type: %s
                Location: %s
                
                Write a fun, 1-sentence description of what they caught, referencing the location. Keep it short and exciting!
                """.formatted(escapeJson(itemType), escapeJson(nodeName));
        String response = callGemini(prompt, false);
        return response != null ? response.strip() : "You caught a " + itemType + " at " + nodeName + "!";
    }

    // ─── Private — Prompt builders ────────────────────────────────────────────

    private String buildIntentPrompt(String userMessage) {
        return """
                You are a stadium navigation assistant for MetLife Stadium during FIFA World Cup 2026.
                Analyse the fan's message and extract their navigation intent.
                                
                Fan message: "%s"
                                
                Available destination node IDs by type:
                - gate: GATE_A (East Entrance), GATE_B (North Entrance), GATE_C (West Entrance), GATE_D (South Entrance)
                - restroom_accessible: REST_N_ACC (North, ♿), REST_S_ACC (South, ♿)
                - restroom: REST_E (East), REST_W (West), REST_N_ACC (North ♿), REST_S_ACC (South ♿)
                - food: FOOD_N (North Food Court), FOOD_S (South Food Court), FOOD_E (East Food Court), FOOD_W (West Food Court)
                - elevator: ELEV_E (East Elevator), ELEV_W (West Elevator)
                - firstaid: FIRSTAID (First Aid Station)
                - merchandise: MERCH_MAIN (Official FIFA Merchandise Store)
                - section: SEC_114 (Section 114 Lower Bowl), SEC_120 (Section 120 Lower Bowl), SEC_214 (Section 214 Upper Bowl), SEC_320 (Section 320 Club Level)
                - team_entry: TEAM_ENTRY (Team & VIP Entry)
                                
                Detect the language the fan is writing in (ISO 639-1, e.g. en, es, fr, pt, de, ar, zh, ja, ko).
                Return ONLY valid JSON — no markdown, no extra text:
                {
                  "destinationType": "<type from the list above, or 'unknown'>",
                  "destinationId": "<specific nodeId, or null if nearest should be found>",
                  "sourceNodeId": "<node ID if the fan mentioned their current location, or null>",
                  "language": "<ISO 639-1 code>",
                  "urgency": "<normal | urgent>"
                }
                """.formatted(escapeJson(userMessage));
    }

    private String buildNarrationPrompt(String userMessage, String language,
                                         RouteResult route,
                                         List<String> congestionZones,
                                         String urgency) {
        String languageName = resolveLanguageName(language);

        if (route == null) {
            return """
                    You are a warm, helpful stadium guide at MetLife Stadium for FIFA World Cup 2026.
                                    
                    Fan's original message: "%s"
                    Detected language: %s (%s)
                                    
                    We could not find a valid stadium route for this query.
                    Please write a polite 1-2 sentence response ENTIRELY in %s explaining that you are a navigation assistant for MetLife Stadium.
                    State that you can help them find restrooms, food, elevators, gates, first aid, the merchandise store, or seating sections.
                    Do NOT include any English text if the language is not English.
                    Return ONLY the message — no JSON, no markdown.
                    """.formatted(
                    escapeJson(userMessage),
                    language, languageName,
                    languageName);
        }

        String stepsText = route.getSteps().stream()
                .map(s -> "  • " + s.getInstruction()
                        + (s.getDistanceFromPrevious() > 0
                        ? String.format(" (%.0fm)", s.getDistanceFromPrevious())
                        : ""))
                .collect(Collectors.joining("\n"));

        String crowdNote = congestionZones.isEmpty() ? ""
                : "⚠ High-congestion zones detected and avoided: "
                + String.join(", ", congestionZones)
                + ". The route was adjusted for your comfort.";

        String urgencyNote = "urgent".equalsIgnoreCase(urgency)
                ? "The fan seems to be in a hurry — be efficient and reassuring."
                : "";

        return """
                You are a warm, helpful stadium guide at MetLife Stadium for FIFA World Cup 2026.
                                
                Fan's original message: "%s"
                Detected language: %s (%s)
                                
                Computed route:
                %s
                                
                Total distance: %.0f metres | Estimated time: %d minute(s)
                %s
                %s
                                
                Write a friendly 2–3 sentence navigation message ENTIRELY in %s.
                - Mention the key landmarks and turns.
                - %s
                - Do NOT include any English text if the language is not English.
                - Return ONLY the message — no JSON, no markdown.
                """.formatted(
                escapeJson(userMessage),
                language, languageName,
                stepsText,
                route.getTotalDistance(), route.getEstimatedMinutes(),
                crowdNote, urgencyNote,
                languageName,
                congestionZones.isEmpty()
                        ? "Keep it brief and encouraging."
                        : "Mention that you found an alternative route due to congestion.");
    }

    // ─── Private — HTTP call ──────────────────────────────────────────────────

    /**
     * Makes a synchronous call to the Gemini REST API.
     *
     * @param prompt       the user-turn text
     * @param jsonMode     when {@code true}, sets responseMimeType to application/json
     * @return the model's text output, or {@code null} on error
     */
    private String callGemini(String prompt, boolean jsonMode) {
        try {
            Map<String, Object> body = buildRequestBody(prompt, jsonMode);
            String url = "/models/" + model + ":generateContent?key=" + apiKey;

            String rawResponse = webClient.post()
                    .uri(url)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        log.error("Gemini API call failed: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (rawResponse == null) return null;

            // Extract text from candidates[0].content.parts[0].text
            JsonNode root = mapper.readTree(rawResponse);
            JsonNode text = root.at("/candidates/0/content/parts/0/text");
            return text.isMissingNode() ? null : text.asText();

        } catch (Exception e) {
            log.error("Gemini call error: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> buildRequestBody(String prompt, boolean jsonMode) {
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of(
                "role", "user",
                "parts", List.of(part)
        );

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("contents", List.of(content));

        if (jsonMode) {
            body.put("generationConfig", Map.of(
                    "responseMimeType", "application/json",
                    "temperature", 0.1,   // Low temp for deterministic JSON
                    "maxOutputTokens", 256
            ));
        } else {
            body.put("generationConfig", Map.of(
                    "temperature", 0.7,
                    "maxOutputTokens", 512
            ));
        }
        return body;
    }

    // ─── Private — Utilities ──────────────────────────────────────────────────

    private String extractJsonFromMarkdown(String raw) {
        if (raw == null) return "{}";
        // Strip ```json ... ``` fences if Gemini included them
        String cleaned = raw.strip();
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf('\n');
            int end = cleaned.lastIndexOf("```");
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start + 1, end).strip();
            }
        }
        return cleaned;
    }

    private IntentResult buildFallbackIntent(String message) {
        // Best-effort language detection from common words
        String lang = "en";
        String lower = message.toLowerCase();
        if (lower.contains("dónde") || lower.contains("donde") || lower.contains("baño") || lower.contains("salida") || lower.contains("comida")) lang = "es";
        else if (lower.contains("où") || lower.contains("toilette") || lower.contains("sortie")) lang = "fr";
        else if (lower.contains("onde") || lower.contains("banheiro") || lower.contains("saída")) lang = "pt";

        String type = "unknown";
        String destId = null;

        if (lower.contains("accessible restroom") || lower.contains("handicap toilet") || lower.contains("baño accesible") || lower.contains("toilette accessible")) {
            type = "restroom_accessible";
        } else if (lower.contains("restroom") || lower.contains("toilet") || lower.contains("baño") || lower.contains("toilette") || lower.contains("banheiro") || lower.contains("w.c") || lower.contains("wc")) {
            type = "restroom";
        } else if (lower.contains("food") || lower.contains("comida") || lower.contains("hungry") || lower.contains("snack") || lower.contains("pizza") || lower.contains("restaurant")) {
            type = "food";
        } else if (lower.contains("first aid") || lower.contains("medical") || lower.contains("doctor") || lower.contains("enfermería") || lower.contains("secours")) {
            type = "firstaid";
            destId = "FIRSTAID";
        } else if (lower.contains("merch") || lower.contains("store") || lower.contains("shop") || lower.contains("tienda") || lower.contains("boutique")) {
            type = "merchandise";
            destId = "MERCH_MAIN";
        } else if (lower.contains("elevator") || lower.contains("ascensor") || lower.contains("ascenseur") || lower.contains("elevador")) {
            type = "elevator";
        } else if (lower.contains("team") || lower.contains("vip") || lower.contains("player")) {
            type = "team_entry";
            destId = "TEAM_ENTRY";
        } else if (lower.contains("114")) {
            type = "section";
            destId = "SEC_114";
        } else if (lower.contains("120")) {
            type = "section";
            destId = "SEC_120";
        } else if (lower.contains("214")) {
            type = "section";
            destId = "SEC_214";
        } else if (lower.contains("320")) {
            type = "section";
            destId = "SEC_320";
        } else if (lower.contains("gate a") || lower.contains("east entrance") || lower.contains("puerta a") || lower.contains("porte a")) {
            type = "gate";
            destId = "GATE_A";
        } else if (lower.contains("gate b") || lower.contains("north entrance") || lower.contains("puerta b") || lower.contains("porte b")) {
            type = "gate";
            destId = "GATE_B";
        } else if (lower.contains("gate c") || lower.contains("west entrance") || lower.contains("puerta c") || lower.contains("porte c")) {
            type = "gate";
            destId = "GATE_C";
        } else if (lower.contains("gate d") || lower.contains("south entrance") || lower.contains("puerta d") || lower.contains("porte d")) {
            type = "gate";
            destId = "GATE_D";
        }

        return IntentResult.builder()
                .destinationType(type)
                .destinationId(destId)
                .language(lang)
                .urgency(lower.contains("emergency") || lower.contains("urgente") || lower.contains("quick") ? "urgent" : "normal")
                .build();
    }

    private String fallbackNarration(String language, RouteResult route) {
        if (route == null) {
            return switch (language == null ? "en" : language.toLowerCase()) {
                case "es" -> "No pude encontrar una ruta. Por favor, pregunte al personal del estadio.";
                case "fr" -> "Je n'ai pas pu trouver d'itinéraire. Veuillez demander à un membre du personnel du stade.";
                case "pt" -> "Não consegui encontrar uma rota. Por favor, pergunte a um funcionário do estádio.";
                default   -> "I couldn't find a route. Please ask a stadium staff member or try specifying a gate, section, or restroom.";
            };
        }
        String destName = route.getSteps().isEmpty() ? "your destination" : route.getSteps().get(route.getSteps().size() - 1).getNodeName();
        return switch (language == null ? "en" : language.toLowerCase()) {
            case "es" -> "Diríjase a " + destName + " — aproximadamente " + route.getEstimatedMinutes() + " minutos a pie.";
            case "fr" -> "Dirigez-vous vers " + destName + " — environ " + route.getEstimatedMinutes() + " minutes de marche.";
            case "pt" -> "Siga para " + destName + " — cerca de " + route.getEstimatedMinutes() + " minutos de caminhada.";
            default   -> "Head to " + destName + " — about " + route.getEstimatedMinutes() + " minute walk.";
        };
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    private String resolveLanguageName(String code) {
        return switch (code == null ? "en" : code.toLowerCase()) {
            case "es" -> "Spanish";
            case "fr" -> "French";
            case "pt" -> "Portuguese";
            case "de" -> "German";
            case "ar" -> "Arabic";
            case "zh" -> "Chinese (Simplified)";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "it" -> "Italian";
            case "nl" -> "Dutch";
            case "ru" -> "Russian";
            default   -> "English";
        };
    }
}
