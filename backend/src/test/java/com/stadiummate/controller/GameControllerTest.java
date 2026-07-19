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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GameControllerTest {

    private MockMvc mockMvc;
    private GamificationService gamificationService;

    @BeforeEach
    public void setup() {
        gamificationService = Mockito.mock(GamificationService.class);
        GameController controller = new GameController(gamificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testGetItems() throws Exception {
        when(gamificationService.getAvailableItems("node1")).thenReturn(List.of("scarf", "hat"));

        mockMvc.perform(get("/api/game/items/node1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]").value("scarf"))
                .andExpect(jsonPath("$.items[1]").value("hat"));
    }

    @Test
    public void testCollectItem() throws Exception {
        when(gamificationService.collectItem("user1", "node1", "scarf")).thenReturn("Success");
        when(gamificationService.getScore("user1")).thenReturn(10);

        mockMvc.perform(post("/api/game/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"user1\", \"nodeId\": \"node1\", \"itemType\": \"scarf\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.newScore").value("10"));
    }

    @Test
    public void testCollectItemInvalid() throws Exception {
        mockMvc.perform(post("/api/game/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"user1\", \"nodeId\": \"\", \"itemType\": \"scarf\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetScore() throws Exception {
        when(gamificationService.getScore("user1")).thenReturn(50);

        mockMvc.perform(get("/api/game/score/user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(50));
    }
}
