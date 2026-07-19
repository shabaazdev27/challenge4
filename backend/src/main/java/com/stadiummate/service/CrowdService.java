package com.stadiummate.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and caches real-time crowd congestion state from Firestore.
 *
 * <p>Firestore collection: {@code crowd_state}
 * Each document ID is a zone identifier (e.g. {@code ZONE_GATE_C}) and has
 * a numeric {@code congestionLevel} field in the range [0.0, 1.0].
 *
 * <p>The cache is refreshed on every inbound chat request via
 * {@link #refreshCache()} — cheap because Firestore document reads are fast
 * and the collection has only ~9 documents.
 *
 * <p>For demo purposes, the {@link #setManualCongestion(String, double)} method
 * allows the {@code POST /api/crowd/simulate} endpoint to mutate crowd state
 * directly, simulating the crowd-sensor feed without an external scheduler.
 */
@Service
public class CrowdService {

    private static final Logger log = LoggerFactory.getLogger(CrowdService.class);
    private static final String COLLECTION = "crowd_state";

    /** Congestion multiplier formula — keeps routing responsive. */
    public static final double CONGESTION_MULTIPLIER_FACTOR = 4.0;

    /** Above this threshold a congestion warning is surfaced to the fan. */
    public static final double WARNING_THRESHOLD = 0.6;

    private final Firestore firestore;

    /** In-memory congestion cache: zoneId → congestionLevel (0.0–1.0). */
    private final Map<String, Double> congestionCache = new ConcurrentHashMap<>();

    public CrowdService(Firestore firestore) {
        this.firestore = firestore;
    }

    // ─── Read operations ──────────────────────────────────────────────────────

    /**
     * Returns the congestion level for a zone, defaulting to 0.0 if unknown.
     *
     * @param zoneId zone identifier, e.g. {@code "ZONE_CONC_NE"}
     * @return congestion level in [0.0, 1.0]
     */
    public double getCongestionLevel(String zoneId) {
        return congestionCache.getOrDefault(zoneId, 0.0);
    }

    /**
     * Returns a snapshot of all zone congestion levels.
     * Used by the frontend to render the crowd heat overlay on the map.
     */
    public Map<String, Double> getAllCongestionLevels() {
        return Map.copyOf(congestionCache);
    }

    /**
     * Pulls fresh congestion levels from Firestore into the in-memory cache.
     * Called at the start of each {@code /api/chat} request.
     */
    public void refreshCache() {
        try {
            firestore.collection(COLLECTION).get().get().getDocuments()
                    .forEach(doc -> {
                        Object level = doc.get("congestionLevel");
                        if (level instanceof Number) {
                            congestionCache.put(doc.getId(), ((Number) level).doubleValue());
                        }
                    });
        } catch (Exception e) {
            log.warn("Firestore crowd refresh failed — using cached values: {}", e.getMessage());
        }
    }

    /**
     * Computes the A* edge weight multiplier for a destination zone.
     *
     * <p>Formula: {@code effectiveWeight = baseWeight * (1 + FACTOR * congestion)}
     * At maximum congestion (1.0): effective weight is 5× the base weight.
     *
     * @param zoneId destination zone ID
     * @return weight multiplier ≥ 1.0
     */
    public double getWeightMultiplier(String zoneId) {
        double congestion = getCongestionLevel(zoneId);
        return 1.0 + CONGESTION_MULTIPLIER_FACTOR * congestion;
    }

    /**
     * Returns true if the given zone exceeds the congestion warning threshold.
     */
    public boolean isHighlyCongested(String zoneId) {
        return getCongestionLevel(zoneId) >= WARNING_THRESHOLD;
    }

    // ─── Write operations (demo / simulation) ────────────────────────────────

    /**
     * Directly sets the congestion level for a zone in both Firestore and
     * the local cache. Used by the {@code POST /api/crowd/simulate} endpoint
     * for live demo rerouting demonstrations.
     *
     * @param zoneId the zone to update
     * @param level  congestion level in [0.0, 1.0]
     */
    public void setManualCongestion(String zoneId, double level) {
        double clamped = Math.max(0.0, Math.min(1.0, level));
        congestionCache.put(zoneId, clamped);

        Map<String, Object> data = new HashMap<>();
        data.put("congestionLevel", clamped);

        DocumentReference ref = firestore.collection(COLLECTION).document(zoneId);
        ref.set(data).addListener(
                () -> log.info("Crowd update written to Firestore: {} = {}", zoneId, clamped),
                Runnable::run
        );
    }

    /**
     * Resets all zone congestion levels to a low baseline (0.1).
     * Useful to reset the demo environment between runs.
     */
    public void resetAll() {
        congestionCache.keySet().forEach(zone -> setManualCongestion(zone, 0.1));
        log.info("All crowd congestion levels reset to 0.1");
    }
}
