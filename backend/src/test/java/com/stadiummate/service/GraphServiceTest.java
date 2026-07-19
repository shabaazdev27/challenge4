package com.stadiummate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for GraphService.
 */
public class GraphServiceTest {

    private GraphService graphService;

    @BeforeEach
    public void setup() {
        Firestore firestore = Mockito.mock(Firestore.class);
        ObjectMapper mapper = new ObjectMapper();
        
        // Initialize GraphService using the local JSON file.
        graphService = new GraphService(firestore, mapper, "local");
        graphService.loadGraph();
    }

    /**
     * Test getting the full graph.
     */
    @Test
    public void testGetGraph() {
        assertNotNull(graphService.getGraph());
    }

    /**
     * Test fetching a node that should exist.
     */
    @Test
    public void testGetNode() {
        assertNotNull(graphService.getNode("GATE_A"));
    }
    
    /**
     * Test retrieving nodes by type.
     */
    @Test
    public void testFindByType() {
        assertNotNull(graphService.findByType("restroom", false));
    }
    
    /**
     * Test retrieving all node ids.
     */
    @Test
    public void testGetAllNodeIds() {
        assertNotNull(graphService.getAllNodeIds());
    }
}
