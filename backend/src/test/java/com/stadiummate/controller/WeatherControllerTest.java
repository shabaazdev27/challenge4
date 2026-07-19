package com.stadiummate.controller;

import com.stadiummate.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for WeatherController.
 */
public class WeatherControllerTest {

    private MockMvc mockMvc;
    private WeatherService weatherService;

    @BeforeEach
    public void setup() {
        weatherService = Mockito.mock(WeatherService.class);
        WeatherController controller = new WeatherController(weatherService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * Test retrieving weather status.
     */
    @Test
    public void testGetWeather() throws Exception {
        when(weatherService.isRaining()).thenReturn(true);
        mockMvc.perform(get("/api/weather"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRaining").value(true));
    }

    /**
     * Test simulating weather conditions.
     */
    @Test
    public void testSimulateWeather() throws Exception {
        mockMvc.perform(post("/api/weather/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isRaining\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
                
        verify(weatherService).setRaining(false);
    }
}
