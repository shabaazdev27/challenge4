package com.stadiummate.controller;

import com.stadiummate.model.*;
import com.stadiummate.service.CrowdService;
import com.stadiummate.service.GeminiService;
import com.stadiummate.service.GraphService;
import com.stadiummate.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ChatController.
 */
public class ChatControllerTest {

    private MockMvc mockMvc;
    private GeminiService geminiService;
    private RouteService routeService;
    private GraphService graphService;
    private CrowdService crowdService;

    @BeforeEach
    public void setup() {
        geminiService = Mockito.mock(GeminiService.class);
        routeService = Mockito.mock(RouteService.class);
        graphService = Mockito.mock(GraphService.class);
        crowdService = Mockito.mock(CrowdService.class);

        ChatController controller = new ChatController(geminiService, routeService, graphService, crowdService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * Test chat endpoint health.
     */
    @Test
    public void testHealth() throws Exception {
        when(graphService.getAllNodeIds()).thenReturn(List.of("node1", "node2"));
        mockMvc.perform(get("/api/chat/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("StadiumMate OK - 2 nodes loaded"));
    }

    /**
     * Test chat API basic request processing.
     */
    @Test
    public void testChat() throws Exception {
        IntentResult intent = IntentResult.builder().destinationType("restroom").destinationId("REST_E").language("en").build();
        when(geminiService.parseIntent(anyString())).thenReturn(intent);
        when(graphService.getNode("REST_E")).thenReturn(new StadiumNode());
        when(routeService.findRoute(anyString(), anyString())).thenReturn(RouteResult.builder().nodePath(List.of("GATE_A", "REST_E")).build());
        when(geminiService.buildNarration(anyString(), anyString(), any(), any(), any())).thenReturn("Go left to the restroom.");
        
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Where is the restroom?\", \"currentLocation\": \"GATE_A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.narration").value("Go left to the restroom."))
                .andExpect(jsonPath("$.language").value("en"));
                
        verify(crowdService).refreshCache();
    }

    @Test
    public void testChatUnknownDestination() throws Exception {
        IntentResult intent = IntentResult.builder().destinationType("unknown").language("en").build();
        when(geminiService.parseIntent(anyString())).thenReturn(intent);
        when(geminiService.buildNarration(anyString(), anyString(), any(), any(), any())).thenReturn("Sorry, I don't know that place.");

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"where is the moon?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.narration").value("Sorry, I don't know that place."))
                .andExpect(jsonPath("$.route").doesNotExist());
    }

    @Test
    public void testChatAccessibleRestroom() throws Exception {
        IntentResult intent = IntentResult.builder().destinationType("restroom_accessible").language("en").build();
        when(geminiService.parseIntent(anyString())).thenReturn(intent);
        when(routeService.findNearestByType(anyString(), eq("restroom"), eq(true)))
                .thenReturn(RouteResult.builder().nodePath(List.of("GATE_A", "REST_N_ACC")).build());
        when(geminiService.buildNarration(anyString(), anyString(), any(), any(), any())).thenReturn("Accessible toilet is near.");

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"accessible restroom\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.narration").value("Accessible toilet is near."));
    }

    @Test
    public void testChatWithCrowdWarning() throws Exception {
        IntentResult intent = IntentResult.builder().destinationType("restroom").destinationId("REST_E").language("en").build();
        when(geminiService.parseIntent(anyString())).thenReturn(intent);
        
        StadiumNode sourceNode = new StadiumNode();
        sourceNode.setNodeId("GATE_A");
        sourceNode.setZone("ZONE_GATE_A");
        StadiumNode destNode = new StadiumNode();
        destNode.setNodeId("REST_E");
        destNode.setZone("ZONE_CONC_NE");

        when(graphService.getNode("GATE_A")).thenReturn(sourceNode);
        when(graphService.getNode("REST_E")).thenReturn(destNode);

        when(routeService.findRoute(anyString(), anyString()))
                .thenReturn(RouteResult.builder().nodePath(List.of("GATE_A", "REST_E")).rerouted(true).build());
        
        when(crowdService.isHighlyCongested("ZONE_CONC_NE")).thenReturn(true);
        when(geminiService.buildNarration(anyString(), anyString(), any(), any(), any())).thenReturn("Avoid NE zone.");

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"restroom\", \"currentLocation\": \"GATE_A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.congestionWarning").value("High congestion detected in: ZONE_CONC_NE. Route adjusted for your comfort."));
    }

    @Test
    public void testChatValidationErrors() throws Exception {
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"\", \"currentLocation\": \"GATE_A\"}"))
                .andExpect(status().isBadRequest());
    }
}
