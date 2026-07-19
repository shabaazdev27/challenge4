package com.stadiummate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTests {

    @Test
    public void testObjectMapper() {
        AppConfig config = new AppConfig();
        ObjectMapper mapper = config.objectMapper();
        assertNotNull(mapper);
    }

    @Test
    public void testGeminiWebClient() {
        AppConfig config = new AppConfig();
        ReflectionTestUtils.setField(config, "geminiBaseUrl", "https://generativelanguage.googleapis.com/v1beta");
        WebClient client = config.geminiWebClient();
        assertNotNull(client);
    }

    @Test
    public void testCorsFilter() {
        CorsConfig config = new CorsConfig();
        assertNotNull(config.corsFilter());
    }
}
