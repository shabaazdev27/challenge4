package com.stadiummate.controller;

import com.stadiummate.model.GameCollectRequest;
import com.stadiummate.service.GamificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Gamification and scoring.
 */
@RestController
@RequestMapping("/api/game")
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
    public ResponseEntity<Map<String, String>> collectItem(@Valid @RequestBody GameCollectRequest req) {
        String userId = req.getUserId() != null && !req.getUserId().isBlank() ? req.getUserId() : "anonymous";
        String nodeId = req.getNodeId();
        String itemType = req.getItemType();

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
