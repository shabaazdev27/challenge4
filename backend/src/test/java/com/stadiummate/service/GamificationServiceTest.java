package com.stadiummate.service;

import com.stadiummate.model.StadiumNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GamificationServiceTest {

    private GraphService graphService;
    private GeminiService geminiService;
    private GamificationService gamificationService;

    @BeforeEach
    public void setup() {
        graphService = Mockito.mock(GraphService.class);
        geminiService = Mockito.mock(GeminiService.class);
        gamificationService = new GamificationService(graphService, geminiService);
    }

    @Test
    public void testGetAvailableItems() {
        StadiumNode node = new StadiumNode();
        node.setSpawnPoints(List.of("scarf", "hat"));
        when(graphService.getGraph()).thenReturn(Map.of("node1", node));

        List<String> items = gamificationService.getAvailableItems("node1");
        assertEquals(2, items.size());
        assertTrue(items.contains("scarf"));
        
        List<String> noItems = gamificationService.getAvailableItems("unknown_node");
        assertTrue(noItems.isEmpty());
    }

    @Test
    public void testCollectItem() {
        StadiumNode node = new StadiumNode();
        node.setSpawnPoints(List.of("scarf"));
        node.setName("Gate A");
        when(graphService.getGraph()).thenReturn(Map.of("node1", node));
        when(geminiService.generateGamificationDescription("scarf", "Gate A")).thenReturn("You collected a scarf!");

        String result = gamificationService.collectItem("user1", "node1", "scarf");
        assertEquals("You collected a scarf!", result);
        assertEquals(10, gamificationService.getScore("user1"));
        
        String notFound = gamificationService.collectItem("user1", "node1", "hat");
        assertEquals("Item not found at this location.", notFound);
    }
}
