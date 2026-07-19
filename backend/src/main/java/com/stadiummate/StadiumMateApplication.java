package com.stadiummate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StadiumMate — GenAI multilingual navigation & crowd-aware concierge
 * for FIFA World Cup 2026.
 *
 * <p>Architecture summary:
 * <ul>
 *   <li>Gemini 2.0 Flash — intent parsing (JSON mode) and localised narration</li>
 *   <li>A* pathfinding — pure Java, crowd-weight-aware routing over the stadium graph</li>
 *   <li>Firestore — stadium graph storage, live crowd density state</li>
 *   <li>Cloud Run — zero-scale-to-zero hosting</li>
 * </ul>
 */
@SpringBootApplication
public class StadiumMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(StadiumMateApplication.class, args);
    }
}
