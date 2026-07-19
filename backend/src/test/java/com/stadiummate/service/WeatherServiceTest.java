package com.stadiummate.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WeatherServiceTest {

    @Test
    public void testWeatherService() {
        WeatherService weatherService = new WeatherService();
        assertFalse(weatherService.isRaining(), "Default should be not raining");
        
        weatherService.setRaining(true);
        assertTrue(weatherService.isRaining(), "Should be raining after set to true");
        
        weatherService.setRaining(false);
        assertFalse(weatherService.isRaining(), "Should not be raining after set to false");
    }
}
