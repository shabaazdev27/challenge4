package com.stadiummate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.stadiummate.model.StadiumNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GraphServiceTest {

    private Firestore firestore;
    private ObjectMapper mapper;

    @BeforeEach
    public void setup() {
        firestore = mock(Firestore.class);
        mapper = new ObjectMapper();
    }

    @Test
    public void testLoadGraphLocal() {
        GraphService service = new GraphService(firestore, mapper, "local");
        service.loadGraph();

        assertNotNull(service.getGraph());
        assertNotNull(service.getNode("GATE_A"));
        assertFalse(service.getAllNodeIds().isEmpty());
        assertFalse(service.findByType("restroom", false).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLoadGraphFirestoreSuccess() throws Exception {
        CollectionReference collectionRef = mock(CollectionReference.class);
        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);

        when(firestore.collection("stadium_graph")).thenReturn(collectionRef);
        when(collectionRef.get()).thenReturn(future);
        when(future.get()).thenReturn(snapshot);
        when(snapshot.getDocuments()).thenReturn(List.of(doc));

        Map<String, Object> mockData = Map.of(
            "nodeId", "MOCK_NODE",
            "type", "gate",
            "name", "Mock Gate",
            "accessible", true,
            "zone", "ZONE_MOCK",
            "svgX", 10.0,
            "svgY", 20.0
        );
        when(doc.getData()).thenReturn(mockData);

        GraphService service = new GraphService(firestore, mapper, "firestore");
        service.loadGraph();

        assertNotNull(service.getGraph());
        assertEquals("Mock Gate", service.getNode("MOCK_NODE").getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLoadGraphFirestoreExceptionFallback() throws Exception {
        CollectionReference collectionRef = mock(CollectionReference.class);
        when(firestore.collection("stadium_graph")).thenReturn(collectionRef);
        when(collectionRef.get()).thenThrow(new RuntimeException("Firestore not initialized"));

        GraphService service = new GraphService(firestore, mapper, "firestore");
        // Should catch the exception and fallback to local JSON load
        assertDoesNotThrow(() -> service.loadGraph());
        assertNotNull(service.getNode("GATE_A")); // Local node should be loaded
    }
}
