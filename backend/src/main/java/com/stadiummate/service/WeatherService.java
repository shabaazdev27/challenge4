package com.stadiummate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    
    // Default to false (not raining)
    private final AtomicBoolean isRaining = new AtomicBoolean(false);

    public boolean isRaining() {
        return isRaining.get();
    }

    public void setRaining(boolean raining) {
        log.info("Weather condition changed. Raining: {}", raining);
        this.isRaining.set(raining);
    }
}
