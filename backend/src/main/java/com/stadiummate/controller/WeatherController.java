package com.stadiummate.controller;

import com.stadiummate.model.WeatherSimulateRequest;
import com.stadiummate.service.WeatherService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Weather data.
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Boolean>> getWeather() {
        return ResponseEntity.ok(Map.of("isRaining", weatherService.isRaining()));
    }

    @PostMapping("/simulate")
    public ResponseEntity<Map<String, String>> simulateWeather(@Valid @RequestBody WeatherSimulateRequest req) {
        boolean isRaining = req.getIsRaining();
        weatherService.setRaining(isRaining);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Weather updated"));
    }
}
