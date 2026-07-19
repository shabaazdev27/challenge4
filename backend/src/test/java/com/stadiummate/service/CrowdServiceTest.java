package com.stadiummate.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CrowdServiceTest {

    private Firestore firestore;
    private CrowdService crowdService;

    @BeforeEach
    public void setup() {
        firestore = mock(Firestore.class);
        crowdService = new CrowdService(firestore);
    }

    @Test
    public void testGetCongestionLevel() {
        assertEquals(0.0, crowdService.getCongestionLevel("ZONE_UNKNOWN"));
    }

    @Test
    public void testSetManualCongestion() throws Exception {
        CollectionReference collectionRef = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<WriteResult> future = mock(ApiFuture.class);

        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(anyString())).thenReturn(docRef);
        when(docRef.set(any())).thenReturn(future);

        // Mock addListener to immediately execute the runnable
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(future).addListener(any(Runnable.class), any(Executor.class));

        crowdService.setManualCongestion("ZONE_A", 0.9);

        assertEquals(0.9, crowdService.getCongestionLevel("ZONE_A"));
        assertTrue(crowdService.isHighlyCongested("ZONE_A"));
        assertEquals(4.6, crowdService.getWeightMultiplier("ZONE_A")); // 1.0 + 4.0 * 0.9
    }

    @Test
    public void testResetAll() {
        CollectionReference collectionRef = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        ApiFuture<WriteResult> future = mock(ApiFuture.class);

        when(firestore.collection(anyString())).thenReturn(collectionRef);
        when(collectionRef.document(anyString())).thenReturn(docRef);
        when(docRef.set(any())).thenReturn(future);

        crowdService.setManualCongestion("ZONE_A", 0.8);
        crowdService.resetAll();
        assertEquals(0.1, crowdService.getCongestionLevel("ZONE_A"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRefreshCacheSuccess() throws Exception {
        CollectionReference collectionRef = mock(CollectionReference.class);
        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);

        when(firestore.collection("crowd_state")).thenReturn(collectionRef);
        when(collectionRef.get()).thenReturn(future);
        when(future.get()).thenReturn(snapshot);
        when(snapshot.getDocuments()).thenReturn(List.of(doc));
        when(doc.getId()).thenReturn("ZONE_A");
        when(doc.get("congestionLevel")).thenReturn(0.85);

        crowdService.refreshCache();

        assertEquals(0.85, crowdService.getCongestionLevel("ZONE_A"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRefreshCacheFailure() throws Exception {
        CollectionReference collectionRef = mock(CollectionReference.class);
        when(firestore.collection("crowd_state")).thenReturn(collectionRef);
        when(collectionRef.get()).thenThrow(new RuntimeException("Firestore error"));

        // Should degrade gracefully and log warning without crashing
        assertDoesNotThrow(() -> crowdService.refreshCache());
    }

    @Test
    public void testGetAllCongestionLevels() {
        assertNotNull(crowdService.getAllCongestionLevels());
    }
}
