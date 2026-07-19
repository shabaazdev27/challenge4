package com.stadiummate.service;

import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CrowdService.
 */
public class CrowdServiceTest {

    private CrowdService crowdService;

    @BeforeEach
    public void setup() {
        // We can mock Firestore but manual setting doesn't actually need a working Firestore 
        // to update the local cache, but it does try to write.
        // It's safer to mock it closely or just let the cache update handle it since we mock Firestore.
        Firestore firestore = Mockito.mock(Firestore.class, Mockito.RETURNS_DEEP_STUBS);
        crowdService = new CrowdService(firestore);
    }

    /**
     * Test initial congestion level is 0.
     */
    @Test
    public void testGetCongestionLevel() {
        assertEquals(0.0, crowdService.getCongestionLevel("ZONE_UNKNOWN"));
    }

    /**
     * Test manual congestion update.
     */
    @Test
    public void testSetManualCongestion() {
        crowdService.setManualCongestion("ZONE_A", 0.9);
        assertEquals(0.9, crowdService.getCongestionLevel("ZONE_A"));
        assertTrue(crowdService.isHighlyCongested("ZONE_A"));
    }

    /**
     * Test weight multiplier.
     */
    @Test
    public void testGetWeightMultiplier() {
        crowdService.setManualCongestion("ZONE_A", 0.5);
        double multiplier = crowdService.getWeightMultiplier("ZONE_A");
        assertEquals(3.0, multiplier); // 1.0 + 4.0 * 0.5
    }

    /**
     * Test reset.
     */
    @Test
    public void testResetAll() {
        crowdService.setManualCongestion("ZONE_A", 0.8);
        crowdService.resetAll();
        assertEquals(0.1, crowdService.getCongestionLevel("ZONE_A"));
    }
}
