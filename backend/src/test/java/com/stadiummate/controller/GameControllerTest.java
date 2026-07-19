package com.stadiummate.controller;

import com.stadiummate.service.GamificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for GameController.
 */
public class GameControllerTest {

    private MockMvc mockMvc;
    private GamificationService gamificationService;

    @BeforeEach
    public void setup() {
        gamificationService = Mockito.mock(GamificationService.class);
        GameController controller = new GameController(gamificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * Test fetching available items for a node.
     */
    @Test
    public void testGetItems() throws Exception {
        when(gamificationService.getAvailableItems("GATE_A")).thenReturn(List.of("scarf"));
        mockMvc.perform(get("/api/game/items/GATE_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]").value("scarf"));
    }

    /**
     * Test collecting an item.
     */
    @Test
    public void testCollectItem() throws Exception {
        when(gamificationService.collectItem("fan1", "GATE_A", "scarf")).thenReturn("Collected");
        when(gamificationService.getScore("fan1")).thenReturn(10);
        
        mockMvc.perform(post("/api/game/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"fan1\", \"nodeId\": \"GATE_A\", \"itemType\": \"scarf\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
    
    /**
     * Test fetching score.
     */
    @Test
    public void testGetScore() throws Exception {
        when(gamificationService.getScore("fan1")).thenReturn(50);
        mockMvc.perform(get("/api/game/score/fan1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(50));
    }
}
