/**
 * LiveViewMap — Immersive Stadium AR View
 *
 * Attempts to load Google Maps Street View at MetLife Stadium.
 * Falls back gracefully to a rich CSS-animated AR overlay when no
 * API key is available or when the Maps script fails to load.
 *
 * The fallback renders a stadium-themed AR HUD with:
 *   - Animated radar sweep
 *   - Live-style crowd density beacons
 *   - Wayfinding arrows for key destinations
 *   - Pulsing "Live" indicator and GPS coordinates
 *
 * Uses only vanilla CSS (no Tailwind) aligned with the design system.
 * Fully accessible: all decorative elements have aria-hidden="true".
 */

"use client";

import React, { useEffect, useRef, useState, useCallback } from "react";

declare global {
  interface Window {
    google: {
      maps: {
        StreetViewPanorama: new (
          el: HTMLElement,
          opts: object
        ) => { setVisible: (v: boolean) => void };
      };
    };
    __gmapsInitialized?: boolean;
    initStadiumStreetView?: () => void;
  }
}

// ── MetLife Stadium street-view position ─────────────────────────────────────
const METLIFE_LAT = 40.8135;
const METLIFE_LNG = -74.0745;

// ── AR beacon definitions (shown in fallback) ────────────────────────────────
interface ARBeacon {
  id: string;
  label: string;
  icon: string;
  angle: number;   // degrees from north (0-360)
  distance: string;
  crowd: "low" | "medium" | "high";
  color: string;
}

const BEACONS: ARBeacon[] = [
  { id: "gate_a",   label: "Gate A (East)",    icon: "🚪", angle: 90,  distance: "120m", crowd: "low",    color: "#43a047" },
  { id: "gate_b",   label: "Gate B (North)",   icon: "🚪", angle: 5,   distance: "280m", crowd: "medium", color: "#fb8c00" },
  { id: "gate_c",   label: "Gate C (West)",    icon: "🚪", angle: 270, distance: "200m", crowd: "high",   color: "#e53935" },
  { id: "gate_d",   label: "Gate D (South)",   icon: "🚪", angle: 180, distance: "340m", crowd: "low",    color: "#43a047" },
  { id: "restroom", label: "Restrooms",        icon: "🚻", angle: 40,  distance: "60m",  crowd: "low",    color: "#1a4db3" },
  { id: "food",     label: "Food Court",       icon: "🍔", angle: 135, distance: "90m",  crowd: "medium", color: "#fb8c00" },
  { id: "store",    label: "Merchandise",      icon: "🛍️", angle: 220, distance: "150m", crowd: "low",    color: "#1a4db3" },
  { id: "medical",  label: "First Aid",        icon: "⛑️", angle: 315, distance: "110m", crowd: "low",    color: "#e53935" },
];

const CROWD_LABEL: Record<string, string> = {
  low: "Clear", medium: "Busy", high: "Crowded",
};

interface LiveViewMapProps {
  apiKey?: string;
  lat?: number;
  lng?: number;
}

// ── Fallback AR HUD component ─────────────────────────────────────────────────
function ARFallbackView() {
  const [compass, setCompass] = useState(0);
  const [radarAngle, setRadarAngle] = useState(0);
  const [activeBeacon, setActiveBeacon] = useState<string | null>(null);
  const [time, setTime] = useState(new Date());
  const animRef = useRef<number>(0);
  const startRef = useRef<number>(Date.now());

  // Animate radar sweep
  useEffect(() => {
    let frame: number;
    const animate = () => {
      const elapsed = (Date.now() - startRef.current) / 1000;
      setRadarAngle((elapsed * 60) % 360); // 60°/s
      setCompass((elapsed * 8) % 360);     // gentle compass drift
      frame = requestAnimationFrame(animate);
    };
    frame = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(frame);
  }, []);

  // Live clock
  useEffect(() => {
    const t = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(t);
  }, []);

  const selected = BEACONS.find((b) => b.id === activeBeacon);

  return (
    <div className="ar-view" aria-label="Augmented reality stadium navigation view">
      {/* ── Simulated camera background ─────────────────────────────── */}
      <div className="ar-bg" aria-hidden="true">
        <div className="ar-bg-grid" />
        <div className="ar-bg-vignette" />
      </div>

      {/* ── Top HUD bar ─────────────────────────────────────────────── */}
      <div className="ar-hud-top" aria-label="AR navigation HUD">
        <div className="ar-hud-top-left">
          <span className="ar-live-badge" role="status" aria-label="Live AR mode active">
            <span className="ar-live-dot" aria-hidden="true" />
            LIVE AR
          </span>
          <span className="ar-location-label" aria-label="Location: MetLife Stadium">
            📍 MetLife Stadium
          </span>
        </div>
        <div className="ar-hud-top-center">
          <span className="ar-coords" aria-label={`GPS: ${METLIFE_LAT}, ${METLIFE_LNG}`} aria-hidden="true">
            {METLIFE_LAT.toFixed(4)}°N · {Math.abs(METLIFE_LNG).toFixed(4)}°W
          </span>
        </div>
        <div className="ar-hud-top-right">
          <time className="ar-clock" aria-label={`Current time: ${time.toLocaleTimeString()}`}>
            {time.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })}
          </time>
        </div>
      </div>

      {/* ── Crosshair ───────────────────────────────────────────────── */}
      <div className="ar-crosshair" aria-hidden="true">
        <div className="ar-crosshair-h" />
        <div className="ar-crosshair-v" />
        <div className="ar-crosshair-corner ar-crosshair-corner--tl" />
        <div className="ar-crosshair-corner ar-crosshair-corner--tr" />
        <div className="ar-crosshair-corner ar-crosshair-corner--bl" />
        <div className="ar-crosshair-corner ar-crosshair-corner--br" />
      </div>

      {/* ── Beacon overlays ─────────────────────────────────────────── */}
      <div className="ar-beacons" role="list" aria-label="Navigation beacons">
        {BEACONS.map((beacon) => {
          // Map angle to horizontal position (0° = right/east, 90° = bottom/south)
          const normalised = ((beacon.angle + 90) % 360) / 360;
          const x = normalised * 100;
          const y = 25 + Math.sin((beacon.angle * Math.PI) / 180) * 15;
          const isActive = activeBeacon === beacon.id;

          return (
            <button
              key={beacon.id}
              className={`ar-beacon ${isActive ? "ar-beacon--active" : ""}`}
              style={{ left: `${x}%`, top: `${y}%`, borderColor: beacon.color }}
              onClick={() => setActiveBeacon(isActive ? null : beacon.id)}
              aria-label={`${beacon.label}: ${beacon.distance} away, ${CROWD_LABEL[beacon.crowd]} crowd`}
              aria-pressed={isActive}
              role="listitem"
            >
              <span className="ar-beacon-icon" aria-hidden="true">{beacon.icon}</span>
              <div className="ar-beacon-label" style={{ color: beacon.color }}>
                {beacon.label}
              </div>
              <div className="ar-beacon-meta" aria-hidden="true">
                <span>{beacon.distance}</span>
                <span
                  className="ar-beacon-crowd"
                  style={{ color: beacon.color }}
                >
                  ● {CROWD_LABEL[beacon.crowd]}
                </span>
              </div>
              <div
                className="ar-beacon-pulse"
                style={{ borderColor: beacon.color }}
                aria-hidden="true"
              />
            </button>
          );
        })}
      </div>

      {/* ── Bottom panel: Radar + Compass + Info ────────────────────── */}
      <div className="ar-hud-bottom" aria-label="Navigation instruments">
        {/* Radar */}
        <div className="ar-radar" aria-label="Radar overview of nearby points of interest" aria-hidden="true">
          <div className="ar-radar-circle ar-radar-circle--outer" />
          <div className="ar-radar-circle ar-radar-circle--mid" />
          <div className="ar-radar-circle ar-radar-circle--inner" />
          <div className="ar-radar-crossline ar-radar-crossline--h" />
          <div className="ar-radar-crossline ar-radar-crossline--v" />

          {/* Rotating sweep */}
          <div
            className="ar-radar-sweep"
            style={{ transform: `rotate(${radarAngle}deg)` }}
          />

          {/* Beacon blips */}
          {BEACONS.map((beacon) => {
            const rad = (beacon.angle * Math.PI) / 180;
            const r = 36; // % from center
            const x = 50 + Math.sin(rad) * r;
            const y = 50 - Math.cos(rad) * r;
            return (
              <div
                key={beacon.id}
                className="ar-radar-blip"
                style={{ left: `${x}%`, top: `${y}%`, background: beacon.color }}
              />
            );
          })}

          {/* You marker */}
          <div className="ar-radar-you">📍</div>
        </div>

        {/* Compass */}
        <div className="ar-compass" aria-label={`Compass heading: ${Math.round(compass)}°`}>
          <div
            className="ar-compass-rose"
            style={{ transform: `rotate(${-compass}deg)` }}
            aria-hidden="true"
          >
            <span className="ar-compass-n">N</span>
            <span className="ar-compass-s">S</span>
            <span className="ar-compass-e">E</span>
            <span className="ar-compass-w">W</span>
          </div>
          <div className="ar-compass-needle" aria-hidden="true" />
          <div className="ar-compass-heading" aria-hidden="true">
            {Math.round(compass)}°
          </div>
        </div>

        {/* Selected beacon info */}
        {selected ? (
          <div className="ar-info-card" aria-live="polite" aria-label={`Selected: ${selected.label}`}>
            <div className="ar-info-icon" aria-hidden="true">{selected.icon}</div>
            <div>
              <div className="ar-info-name">{selected.label}</div>
              <div className="ar-info-distance" style={{ color: selected.color }}>
                {selected.distance} away · {CROWD_LABEL[selected.crowd]}
              </div>
              <div className="ar-info-hint">Tap beacon to navigate →</div>
            </div>
          </div>
        ) : (
          <div className="ar-info-card ar-info-card--hint" aria-label="Tap any beacon to select a destination">
            <div className="ar-info-icon" aria-hidden="true">🎯</div>
            <div>
              <div className="ar-info-name">AR Navigation Active</div>
              <div className="ar-info-hint">Tap any beacon to select a destination</div>
              <div className="ar-info-hint" style={{ marginTop: 2 }}>
                🏟️ FIFA World Cup 2026 · MetLife Stadium
              </div>
            </div>
          </div>
        )}
      </div>

      {/* ── Scan line effect ─────────────────────────────────────────── */}
      <div className="ar-scanline" aria-hidden="true" />

      {/* ── Corner decorations ──────────────────────────────────────── */}
      <div className="ar-corner ar-corner--tl" aria-hidden="true" />
      <div className="ar-corner ar-corner--tr" aria-hidden="true" />
      <div className="ar-corner ar-corner--bl" aria-hidden="true" />
      <div className="ar-corner ar-corner--br" aria-hidden="true" />
    </div>
  );
}

// ── Main LiveViewMap component ────────────────────────────────────────────────
export default function LiveViewMap({
  apiKey,
  lat = METLIFE_LAT,
  lng = METLIFE_LNG,
}: LiveViewMapProps) {
  const mapRef = useRef<HTMLDivElement>(null);
  const [mapState, setMapState] = useState<"loading" | "ready" | "fallback">(
    apiKey ? "loading" : "fallback"
  );

  const initStreetView = useCallback(() => {
    if (!mapRef.current || !window.google?.maps) {
      setMapState("fallback");
      return;
    }
    try {
      new window.google.maps.StreetViewPanorama(mapRef.current, {
        position: { lat, lng },
        pov: { heading: 165, pitch: 0 },
        zoom: 1,
        addressControl: false,
        fullscreenControl: false,
        motionTracking: false,
        motionTrackingControl: false,
        showRoadLabels: true,
      });
      setMapState("ready");
    } catch {
      setMapState("fallback");
    }
  }, [lat, lng]);

  useEffect(() => {
    // No API key → go straight to fallback
    if (!apiKey) {
      setMapState("fallback");
      return;
    }

    // Google Maps already loaded (e.g., hot-reload)
    if (window.google?.maps) {
      initStreetView();
      return;
    }

    // Guard against duplicate script injection
    if (window.__gmapsInitialized) return;
    window.__gmapsInitialized = true;

    // Timeout fallback: if script hasn't called back in 8s, show fallback UI
    const timeoutId = setTimeout(() => setMapState("fallback"), 8000);

    window.initStadiumStreetView = () => {
      clearTimeout(timeoutId);
      initStreetView();
    };

    const script = document.createElement("script");
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&callback=initStadiumStreetView&loading=async`;
    script.async = true;
    script.defer = true;
    script.onerror = () => {
      clearTimeout(timeoutId);
      setMapState("fallback");
    };
    document.head.appendChild(script);

    return () => {
      clearTimeout(timeoutId);
    };
  }, [apiKey, initStreetView]);

  // If no API key or script failed → show AR fallback
  if (mapState === "fallback") {
    return <ARFallbackView />;
  }

  return (
    <div style={{ position: "relative", width: "100%", height: "100%" }}>
      {/* Loading state overlay */}
      {mapState === "loading" && (
        <div className="ar-loading" aria-live="polite" aria-label="Loading street view">
          <div className="ar-loading-spinner" aria-hidden="true" />
          <span>Loading Street View…</span>
        </div>
      )}
      {/* Google Maps Street View container */}
      <div
        ref={mapRef}
        style={{ width: "100%", height: "100%" }}
        aria-label="Google Maps Street View of MetLife Stadium"
      />
    </div>
  );
}
