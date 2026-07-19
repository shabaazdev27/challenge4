package com.stadiummate.controller;

import com.stadiummate.service.CrowdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CrowdController.
 */
public class CrowdControllerTest {

    private MockMvc mockMvc;
    private CrowdService crowdService;

    @BeforeEach
    public void setup() {
        crowdService = Mockito.mock(CrowdService.class);
        CrowdController controller = new CrowdController(crowdService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * Test retrieving crowd state.
     */
    @Test
    public void testGetCrowdState() throws Exception {
        when(crowdService.getAllCongestionLevels()).thenReturn(Map.of("ZONE_A", 0.8));
        mockMvc.perform(get("/api/crowd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ZONE_A").value(0.8));
        verify(crowdService).refreshCache();
    }

    /**
     * Test simulating crowd.
     */
    @Test
    public void testSimulate() throws Exception {
        mockMvc.perform(post("/api/crowd/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"zoneId\": \"ZONE_A\", \"level\": 0.9}"))
                .andExpect(status().isOk());
        
        verify(crowdService).setManualCongestion("ZONE_A", 0.9);
    }
    
    /**
     * Test resetting crowd.
     */
    @Test
    public void testReset() throws Exception {
        mockMvc.perform(post("/api/crowd/reset"))
                .andExpect(status().isOk());
                
        verify(crowdService).resetAll();
    }
}
