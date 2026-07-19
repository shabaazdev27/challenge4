package com.stadiummate.controller;

import com.stadiummate.service.CrowdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for crowd-state management.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /api/crowd}           — current zone congestion levels</li>
 *   <li>{@code POST /api/crowd/simulate}  — set a zone's congestion (demo use)</li>
 *   <li>{@code POST /api/crowd/reset}     — reset all zones to baseline</li>
 * </ul>
 *
 * <p>The simulate endpoint is the key "operational intelligence" hook for the
 * demo — calling it with a high level triggers live rerouting on the next
 * fan query, demonstrating the crowd-aware navigation in real time.
 */
@RestController
@RequestMapping("/api/crowd")
public class CrowdController {

    private static final Logger log = LoggerFactory.getLogger(CrowdController.class);

    private final CrowdService crowdService;

    public CrowdController(CrowdService crowdService) {
        this.crowdService = crowdService;
    }

    // ─── GET /api/crowd ───────────────────────────────────────────────────────

    /**
     * Returns the current congestion level for every zone.
     * The frontend polls this on the map view to update the heat overlay.
     *
     * @return map of zoneId → congestion level [0.0, 1.0]
     */
    @GetMapping
    public ResponseEntity<Map<String, Double>> getCrowdState() {
        crowdService.refreshCache();
        return ResponseEntity.ok(crowdService.getAllCongestionLevels());
    }

    // ─── POST /api/crowd/simulate ─────────────────────────────────────────────

    /**
     * Sets the congestion level for a specific zone — writes to Firestore
     * so the next {@code /api/chat} call picks it up automatically.
     *
     * <p>Demo usage: {@code POST /api/crowd/simulate}
     * <pre>{"zoneId": "ZONE_CONC_NE", "level": 0.9}</pre>
     *
     * @param body JSON body with {@code zoneId} (String) and {@code level} (double)
     * @return acknowledgement message
     */
    @PostMapping("/simulate")
    public ResponseEntity<String> simulate(@RequestBody Map<String, Object> body) {
        String zoneId = (String) body.get("zoneId");
        Object levelObj = body.get("level");

        if (zoneId == null || levelObj == null) {
            return ResponseEntity.badRequest()
                    .body("Required fields: zoneId (string), level (number 0.0-1.0)");
        }

        double level;
        try {
            level = ((Number) levelObj).doubleValue();
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("'level' must be a number");
        }

        crowdService.setManualCongestion(zoneId, level);
        log.info("Crowd simulation: {} = {}", zoneId, level);

        String message = level >= CrowdService.WARNING_THRESHOLD
                ? String.format("Zone %s set to %.0f%% congestion — rerouting will activate.", zoneId, level * 100)
                : String.format("Zone %s set to %.0f%% congestion — normal traffic.", zoneId, level * 100);

        return ResponseEntity.ok(message);
    }

    // ─── POST /api/crowd/reset ────────────────────────────────────────────────

    /**
     * Resets all zones to a low baseline (0.1) for clean demo restarts.
     *
     * @return acknowledgement message
     */
    @PostMapping("/reset")
    public ResponseEntity<String> reset() {
        crowdService.resetAll();
        return ResponseEntity.ok("All zones reset to baseline congestion (10%).");
    }
}
