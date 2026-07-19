package com.stadiummate.controller;

import com.stadiummate.service.GamificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Gamification and scoring.
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    private final GamificationService gamificationService;

    public GameController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/items/{nodeId}")
    public ResponseEntity<Map<String, List<String>>> getItems(@PathVariable String nodeId) {
        return ResponseEntity.ok(Map.of("items", gamificationService.getAvailableItems(nodeId)));
    }

    @PostMapping("/collect")
    public ResponseEntity<Map<String, String>> collectItem(@RequestBody Map<String, String> payload) {
        String userId = payload.getOrDefault("userId", "anonymous");
        String nodeId = payload.get("nodeId");
        String itemType = payload.get("itemType");

        if (nodeId == null || itemType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing nodeId or itemType"));
        }

        String message = gamificationService.collectItem(userId, nodeId, itemType);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", message,
            "newScore", String.valueOf(gamificationService.getScore(userId))
        ));
    }
    
    @GetMapping("/score/{userId}")
    public ResponseEntity<Map<String, Integer>> getScore(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("score", gamificationService.getScore(userId)));
    }
}
