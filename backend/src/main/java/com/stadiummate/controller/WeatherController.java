package com.stadiummate.controller;

import com.stadiummate.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Weather data.
 */
@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "*")
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
    public ResponseEntity<Map<String, String>> simulateWeather(@RequestBody Map<String, Boolean> payload) {
        Boolean isRaining = payload.get("isRaining");
        if (isRaining != null) {
            weatherService.setRaining(isRaining);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Weather updated"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Missing 'isRaining' flag"));
    }
}
