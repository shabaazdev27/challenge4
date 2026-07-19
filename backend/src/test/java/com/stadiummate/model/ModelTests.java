package com.stadiummate.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ModelTests {

    @Test
    public void testStadiumNode() {
        StadiumNode node1 = new StadiumNode();
        node1.setNodeId("123");
        node1.setType("gate");
        node1.setName("Gate A");
        node1.setAccessible(true);
        node1.setZone("ZONE_A");
        node1.setSvgX(100.0);
        node1.setSvgY(200.0);
        node1.setEdges(List.of(new StadiumEdge()));
        node1.setSpawnPoints(List.of("item1"));

        assertEquals("123", node1.getNodeId());
        assertEquals("gate", node1.getType());
        assertEquals("Gate A", node1.getName());
        assertTrue(node1.isAccessible());
        assertEquals("ZONE_A", node1.getZone());
        assertEquals(100.0, node1.getSvgX());
        assertEquals(200.0, node1.getSvgY());
        assertFalse(node1.getEdges().isEmpty());
        assertEquals(List.of("item1"), node1.getSpawnPoints());

        StadiumNode node2 = StadiumNode.builder()
            .nodeId("123")
            .type("gate")
            .name("Gate A")
            .accessible(true)
            .zone("ZONE_A")
            .svgX(100.0)
            .svgY(200.0)
            .edges(node1.getEdges())
            .spawnPoints(List.of("item1"))
            .build();

        assertEquals(node1, node2);
        assertEquals(node1.hashCode(), node2.hashCode());
        assertNotNull(node1.toString());
    }

    @Test
    public void testStadiumEdge() {
        StadiumEdge edge1 = StadiumEdge.builder()
            .to("target")
            .baseWeight(1.0)
            .type("walkway")
            .isAccessible(true)
            .isCovered(true)
            .build();
            
        assertEquals("target", edge1.getTo());
        assertEquals(1.0, edge1.getBaseWeight());
        assertEquals("walkway", edge1.getType());
        assertTrue(edge1.isAccessible());
        assertTrue(edge1.isCovered());

        StadiumEdge edge2 = new StadiumEdge();
        edge2.setTo("target");
        edge2.setBaseWeight(1.0);
        edge2.setType("walkway");
        edge2.setAccessible(true);
        edge2.setCovered(true);

        assertEquals(edge1, edge2);
        assertEquals(edge1.hashCode(), edge2.hashCode());
        assertNotNull(edge1.toString());
    }

    @Test
    public void testChatRequest() {
        ChatRequest req1 = new ChatRequest();
        req1.setSessionId("sess1");
        req1.setMessage("hello");
        req1.setCurrentLocation("loc1");
        
        assertEquals("sess1", req1.getSessionId());
        assertEquals("hello", req1.getMessage());
        assertEquals("loc1", req1.getCurrentLocation());

        ChatRequest req2 = ChatRequest.builder()
            .sessionId("sess1")
            .message("hello")
            .currentLocation("loc1")
            .build();

        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        assertNotNull(req1.toString());
    }

    @Test
    public void testChatResponse() {
        ChatResponse res1 = ChatResponse.builder()
            .narration("go left")
            .language("en")
            .route(new RouteResult())
            .congestionWarning("crowded")
            .crowdState(Map.of("Z1", 0.5))
            .sessionId("sess123")
            .build();

        assertEquals("go left", res1.getNarration());
        assertEquals("en", res1.getLanguage());
        assertNotNull(res1.getRoute());
        assertEquals("crowded", res1.getCongestionWarning());
        assertEquals(Map.of("Z1", 0.5), res1.getCrowdState());
        assertEquals("sess123", res1.getSessionId());

        ChatResponse res2 = new ChatResponse();
        res2.setNarration("go left");
        res2.setLanguage("en");
        res2.setRoute(res1.getRoute());
        res2.setCongestionWarning("crowded");
        res2.setCrowdState(res1.getCrowdState());
        res2.setSessionId("sess123");

        assertEquals(res1, res2);
        assertEquals(res1.hashCode(), res2.hashCode());
        assertNotNull(res1.toString());
    }

    @Test
    public void testIntentResult() {
        IntentResult int1 = IntentResult.builder()
            .destinationType("restroom")
            .destinationId("REST_E")
            .sourceNodeId("GATE_A")
            .language("en")
            .urgency("normal")
            .build();

        assertEquals("restroom", int1.getDestinationType());
        assertEquals("REST_E", int1.getDestinationId());
        assertEquals("GATE_A", int1.getSourceNodeId());
        assertEquals("en", int1.getLanguage());
        assertEquals("normal", int1.getUrgency());

        IntentResult int2 = new IntentResult();
        int2.setDestinationType("restroom");
        int2.setDestinationId("REST_E");
        int2.setSourceNodeId("GATE_A");
        int2.setLanguage("en");
        int2.setUrgency("normal");

        assertEquals(int1, int2);
        assertEquals(int1.hashCode(), int2.hashCode());
        assertNotNull(int1.toString());
    }

    @Test
    public void testRouteResult() {
        RouteResult rr1 = RouteResult.builder()
            .steps(List.of(new RouteStep()))
            .totalDistance(150.0)
            .estimatedMinutes(2)
            .nodePath(List.of("N1", "N2"))
            .rerouted(true)
            .build();

        assertFalse(rr1.getSteps().isEmpty());
        assertEquals(150.0, rr1.getTotalDistance());
        assertEquals(2, rr1.getEstimatedMinutes());
        assertEquals(List.of("N1", "N2"), rr1.getNodePath());
        assertTrue(rr1.isRerouted());

        RouteResult rr2 = new RouteResult();
        rr2.setSteps(rr1.getSteps());
        rr2.setTotalDistance(150.0);
        rr2.setEstimatedMinutes(2);
        rr2.setNodePath(rr1.getNodePath());
        rr2.setRerouted(true);

        assertEquals(rr1, rr2);
        assertEquals(rr1.hashCode(), rr2.hashCode());
        assertNotNull(rr1.toString());
    }

    @Test
    public void testRouteStep() {
        RouteStep step1 = RouteStep.builder()
            .nodeId("node1")
            .nodeName("Gate")
            .instruction("go")
            .edgeType("walk")
            .distanceFromPrevious(10.0)
            .build();

        assertEquals("node1", step1.getNodeId());
        assertEquals("Gate", step1.getNodeName());
        assertEquals("go", step1.getInstruction());
        assertEquals("walk", step1.getEdgeType());
        assertEquals(10.0, step1.getDistanceFromPrevious());

        RouteStep step2 = new RouteStep();
        step2.setNodeId("node1");
        step2.setNodeName("Gate");
        step2.setInstruction("go");
        step2.setEdgeType("walk");
        step2.setDistanceFromPrevious(10.0);

        assertEquals(step1, step2);
        assertEquals(step1.hashCode(), step2.hashCode());
        assertNotNull(step1.toString());
    }

    @Test
    public void testCrowdSimulateRequest() {
        CrowdSimulateRequest req1 = CrowdSimulateRequest.builder()
            .zoneId("ZONE_A")
            .level(0.7)
            .build();

        assertEquals("ZONE_A", req1.getZoneId());
        assertEquals(0.7, req1.getLevel());

        CrowdSimulateRequest req2 = new CrowdSimulateRequest();
        req2.setZoneId("ZONE_A");
        req2.setLevel(0.7);

        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        assertNotNull(req1.toString());
    }

    @Test
    public void testGameCollectRequest() {
        GameCollectRequest req1 = GameCollectRequest.builder()
            .userId("u1")
            .nodeId("n1")
            .itemType("hat")
            .build();

        assertEquals("u1", req1.getUserId());
        assertEquals("n1", req1.getNodeId());
        assertEquals("hat", req1.getItemType());

        GameCollectRequest req2 = new GameCollectRequest();
        req2.setUserId("u1");
        req2.setNodeId("n1");
        req2.setItemType("hat");

        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        assertNotNull(req1.toString());
    }

    @Test
    public void testWeatherSimulateRequest() {
        WeatherSimulateRequest req1 = WeatherSimulateRequest.builder()
            .isRaining(true)
            .build();

        assertTrue(req1.getIsRaining());

        WeatherSimulateRequest req2 = new WeatherSimulateRequest();
        req2.setIsRaining(true);

        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        assertNotNull(req1.toString());
    }
}
