package com.stadiummate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Spring configuration — registers application-scoped beans:
 * <ul>
 *   <li>{@link ObjectMapper}   — configured Jackson JSON mapper</li>
 *   <li>{@link Firestore}      — Firebase Admin Firestore client</li>
 *   <li>{@link WebClient}      — pre-configured HTTP client for Gemini API</li>
 * </ul>
 */
@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("${stadiummate.firebase.project-id}")
    private String projectId;

    @Value("${stadiummate.firebase.credentials-file:}")
    private String credentialsFile;

    @Value("${stadiummate.gemini.base-url}")
    private String geminiBaseUrl;

    // ─── Jackson ─────────────────────────────────────────────────────────────

    /**
     * Shared Jackson {@link ObjectMapper} with Java-time support enabled.
     * Injected into services for parsing Gemini JSON responses.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    // ─── Firebase / Firestore ─────────────────────────────────────────────────

    /**
     * Initialises Firebase Admin and returns a Firestore client.
     *
     * <p>Authentication priority:
     * <ol>
     *   <li>Service account JSON file (path in {@code GOOGLE_APPLICATION_CREDENTIALS})</li>
     *   <li>Application Default Credentials — automatic when running on Cloud Run</li>
     * </ol>
     */
    @Bean
    public Firestore firestore() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            GoogleCredentials credentials = loadCredentials();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase initialised for project: {}", projectId);
        }
        return FirestoreClient.getFirestore();
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (credentialsFile != null && !credentialsFile.isBlank()) {
            log.info("Loading Firebase credentials from: {}", credentialsFile);
            try (InputStream is = new FileInputStream(credentialsFile)) {
                return GoogleCredentials.fromStream(is);
            }
        }
        log.info("Using Application Default Credentials for Firebase");
        return GoogleCredentials.getApplicationDefault();
    }

    // ─── WebClient (Gemini REST API) ──────────────────────────────────────────

    /**
     * Pre-configured reactive {@link WebClient} for calling the Gemini REST API.
     * The base URL is set here; each call appends the model and method path.
     */
    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(geminiBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
