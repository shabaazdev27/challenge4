package com.stadiummate.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ModelTests {

    @Test
    public void testStadiumNode() {
        StadiumNode node = new StadiumNode();
        node.setNodeId("123");
        node.setType("gate");
        node.setName("Gate A");
        node.setAccessible(true);
        node.setZone("ZONE_A");
        node.setEdges(List.of(new StadiumEdge()));
        
        assertEquals("123", node.getNodeId());
        assertEquals("gate", node.getType());
        assertEquals("Gate A", node.getName());
        assertTrue(node.isAccessible());
        assertEquals("ZONE_A", node.getZone());
        assertFalse(node.getEdges().isEmpty());
    }

    @Test
    public void testStadiumEdge() {
        StadiumEdge edge = StadiumEdge.builder()
            .to("target")
            .baseWeight(1.0)
            .type("walkway")
            .isAccessible(true)
            .isCovered(true)
            .build();
            
        assertEquals("target", edge.getTo());
        assertEquals(1.0, edge.getBaseWeight());
        assertEquals("walkway", edge.getType());
        assertTrue(edge.isAccessible());
        assertTrue(edge.isCovered());
    }

    @Test
    public void testChatRequest() {
        ChatRequest req = new ChatRequest();
        req.setSessionId("sess1");
        req.setMessage("hello");
        req.setCurrentLocation("loc1");
        
        assertEquals("sess1", req.getSessionId());
        assertEquals("hello", req.getMessage());
        assertEquals("loc1", req.getCurrentLocation());
    }
}
