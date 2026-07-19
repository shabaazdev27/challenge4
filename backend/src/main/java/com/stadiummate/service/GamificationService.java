package com.stadiummate.service;

import com.stadiummate.model.StadiumNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GamificationService {

    private final GraphService graphService;
    private final GeminiService geminiService;
    
    // In-memory store for scores and collected items for demo purposes.
    // In production, this would be tied to Firebase Auth UID in Firestore.
    private final Map<String, Integer> userScores = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userCollectedItems = new ConcurrentHashMap<>();

    public GamificationService(GraphService graphService, GeminiService geminiService) {
        this.graphService = graphService;
        this.geminiService = geminiService;
    }

    public List<String> getAvailableItems(String nodeId) {
        StadiumNode node = graphService.getGraph().get(nodeId);
        if (node != null && node.getSpawnPoints() != null) {
            return node.getSpawnPoints();
        }
        return List.of();
    }

    public String collectItem(String userId, String nodeId, String itemType) {
        StadiumNode node = graphService.getGraph().get(nodeId);
        if (node == null || node.getSpawnPoints() == null || !node.getSpawnPoints().contains(itemType)) {
            return "Item not found at this location.";
        }

        userScores.merge(userId, 10, Integer::sum);
        userCollectedItems.computeIfAbsent(userId, k -> new ArrayList<>()).add(itemType);

        return geminiService.generateGamificationDescription(itemType, node.getName());
    }

    public int getScore(String userId) {
        return userScores.getOrDefault(userId, 0);
    }
}
