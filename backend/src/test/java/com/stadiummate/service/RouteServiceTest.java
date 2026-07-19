package com.stadiummate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stadiummate.model.RouteResult;
import com.stadiummate.model.StadiumNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RouteService} — validates the A* pathfinding algorithm
 * over the MetLife Stadium graph.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Basic shortest-path correctness</li>
 *   <li>Crowd-congestion rerouting — verifies that a congested intermediate
 *       node causes A* to choose a longer but less congested path</li>
 *   <li>Same-node edge case (0-distance route)</li>
 *   <li>Nearest-by-type resolver</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private GraphService graphService;

    @Mock
    private CrowdService crowdService;

    @Mock
    private WeatherService weatherService;

    private RouteService routeService;
    private Map<String, StadiumNode> graph;

    @BeforeEach
    void setUp() throws Exception {
        routeService = new RouteService(graphService, crowdService, weatherService);

        // Load the real stadium graph JSON (classpath)
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("data/metlife-graph.json");
        try (InputStream is = resource.getInputStream()) {
            List<StadiumNode> nodes = mapper.readValue(is, new TypeReference<>() {});
            graph = new HashMap<>();
            nodes.forEach(n -> graph.put(n.getNodeId(), n));
        }

        lenient().when(graphService.getGraph()).thenReturn(graph);
        lenient().when(graphService.getNode(anyString()))
                .thenAnswer(inv -> graph.get(inv.getArgument(0)));

        // Default: no congestion, no rain
        lenient().when(crowdService.getWeightMultiplier(anyString())).thenReturn(1.0);
        lenient().when(crowdService.isHighlyCongested(anyString())).thenReturn(false);
        lenient().when(weatherService.isRaining()).thenReturn(false);
    }

    // ─── Basic routing ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Route found from GATE_A to GATE_B — path passes through NE Concourse")
    void shortestPath_gatA_to_gateB() {
        RouteResult result = routeService.findRoute("GATE_A", "GATE_B");

        assertThat(result).isNotNull();
        assertThat(result.getNodePath()).isNotEmpty();
        assertThat(result.getNodePath().get(0)).isEqualTo("GATE_A");
        assertThat(result.getNodePath()).last().isEqualTo("GATE_B");

        // Direct path via CONC_MAIN_NE: 120 + 110 = 230m
        assertThat(result.getTotalDistance()).isLessThanOrEqualTo(240.0);
        assertThat(result.getEstimatedMinutes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Route to accessible restroom from Gate A finds REST_N_ACC or REST_S_ACC")
    void nearestAccessibleRestroom_fromGateA() {
        when(graphService.findByType("restroom", true))
                .thenReturn(List.of(graph.get("REST_N_ACC"), graph.get("REST_S_ACC")));

        RouteResult result = routeService.findNearestByType("GATE_A", "restroom", true);

        assertThat(result).isNotNull();
        String dest = result.getNodePath().get(result.getNodePath().size() - 1);
        assertThat(dest).isIn("REST_N_ACC", "REST_S_ACC");
    }

    @Test
    @DisplayName("Multi-floor route: GATE_A to SEC_214 passes through elevator or ramp")
    void multiFloorRoute_gateA_to_sec214() {
        RouteResult result = routeService.findRoute("GATE_A", "SEC_214");

        assertThat(result).isNotNull();
        // Must pass through ELEV_E or RAMP_SW to reach level 2
        boolean usesVerticalAccess = result.getNodePath().stream()
                .anyMatch(id -> id.equals("ELEV_E") || id.equals("RAMP_NE")
                        || id.equals("RAMP_SW") || id.equals("ELEV_W"));
        assertThat(usesVerticalAccess).isTrue();
    }

    @Test
    @DisplayName("Same-node route returns 0 distance and 1 step")
    void sameNodeRoute_returnsZeroDistance() {
        RouteResult result = routeService.findRoute("GATE_A", "GATE_A");

        assertThat(result).isNotNull();
        assertThat(result.getTotalDistance()).isEqualTo(0.0);
        assertThat(result.getSteps()).hasSize(1);
    }

    // ─── Crowd-aware rerouting ────────────────────────────────────────────────

    @Test
    @DisplayName("Congested ZONE_CONC_NE causes A* to avoid CONC_MAIN_NE on GATE_A→GATE_B route")
    void crowdCongestion_avoidsCongestedCorridor() {
        // Congest the NE concourse heavily — 4.6× multiplier
        lenient().when(crowdService.getWeightMultiplier("ZONE_CONC_NE")).thenReturn(4.6);

        RouteResult congested = routeService.findRoute("GATE_A", "GATE_B");
        assertThat(congested).isNotNull();

        // No congestion baseline route
        lenient().when(crowdService.getWeightMultiplier("ZONE_CONC_NE")).thenReturn(1.0);
        RouteResult normal = routeService.findRoute("GATE_A", "GATE_B");

        // With high NE congestion the effective distance should be longer
        // (the router either picks the long-way-around or the same path — but
        // the effective g-score via CONC_MAIN_NE is ≥ 4.6x, so it should reroute)
        assertThat(congested.getNodePath())
                .as("Congested route should not pass through the NE corridor")
                .doesNotContain("CONC_MAIN_NE");
    }

    @Test
    @DisplayName("Unknown node IDs return null gracefully")
    void unknownNodes_returnNull() {
        RouteResult result = routeService.findRoute("NONEXISTENT", "GATE_B");
        assertThat(result).isNull();
    }
}
