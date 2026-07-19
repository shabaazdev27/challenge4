"use client";

import React, { useEffect, useState } from "react";
import { fetchCrowdState, simulateCrowd, resetCrowd } from "@/lib/api";

interface CrowdIndicatorProps {
  crowdState: Record<string, number>;
  /** When true, shows the simulation control panel (demo / organiser view) */
  showControls?: boolean;
}

const ZONE_LABELS: Record<string, string> = {
  ZONE_GATE_A: "Gate A (East)",
  ZONE_GATE_B: "Gate B (North)",
  ZONE_GATE_C: "Gate C (West)",
  ZONE_GATE_D: "Gate D (South)",
  ZONE_CONC_NE: "NE Concourse",
  ZONE_CONC_NW: "NW Concourse",
  ZONE_CONC_SW: "SW Concourse",
  ZONE_CONC_SE: "SE Concourse",
  ZONE_UPPER: "Upper Level",
};

function crowdColor(level: number): string {
  if (level >= 0.7) return "#e53935";
  if (level >= 0.4) return "#fb8c00";
  return "#43a047";
}

function crowdLabel(level: number): string {
  if (level >= 0.7) return "High";
  if (level >= 0.4) return "Medium";
  return "Low";
}

/**
 * Crowd congestion indicator panel.
 *
 * Displays a real-time bar graph of zone congestion levels and,
 * when `showControls` is true, exposes the demo simulation controls
 * so a presenter can spike congestion live during the demo.
 */
export default function CrowdIndicator({
  crowdState,
  showControls = false,
}: CrowdIndicatorProps) {
  const [simZone, setSimZone] = useState("ZONE_CONC_NE");
  const [simLevel, setSimLevel] = useState(0.9);
  const [simFeedback, setSimFeedback] = useState("");
  const [isSimulating, setIsSimulating] = useState(false);

  const zones = Object.keys(ZONE_LABELS);

  const handleSimulate = async () => {
    setIsSimulating(true);
    try {
      const msg = await simulateCrowd(simZone, simLevel);
      setSimFeedback(msg);
    } catch {
      setSimFeedback("Simulation failed — check backend connection.");
    } finally {
      setIsSimulating(false);
    }
  };

  const handleReset = async () => {
    setIsSimulating(true);
    try {
      await resetCrowd();
      setSimFeedback("All zones reset to baseline.");
    } catch {
      setSimFeedback("Reset failed.");
    } finally {
      setIsSimulating(false);
    }
  };

  return (
    <div
      style={{
        padding: "12px 16px",
        borderTop: "1px solid rgba(255,255,255,0.06)",
      }}
    >
      {/* Title */}
      <div
        style={{
          fontSize: "0.72rem",
          fontWeight: 600,
          textTransform: "uppercase",
          letterSpacing: "0.08em",
          color: "var(--text-muted)",
          marginBottom: 10,
          display: "flex",
          alignItems: "center",
          gap: 6,
        }}
      >
        <span aria-hidden="true">📊</span>
        Live Crowd Density
      </div>

      {/* Zone bars */}
      <div
        className="crowd-panel"
        role="list"
        aria-label="Zone congestion levels"
      >
        {zones.map((zoneId) => {
          const level = crowdState[zoneId] ?? 0;
          const color = crowdColor(level);
          return (
            <div
              key={zoneId}
              className="crowd-zone-row"
              role="listitem"
              aria-label={`${ZONE_LABELS[zoneId]}: ${crowdLabel(level)} congestion at ${Math.round(level * 100)}%`}
            >
              <span
                style={{
                  minWidth: 100,
                  fontSize: "0.72rem",
                  color: "var(--text-secondary)",
                }}
              >
                {ZONE_LABELS[zoneId]}
              </span>
              <div className="crowd-bar-track" aria-hidden="true">
                <div
                  className="crowd-bar-fill"
                  style={{ width: `${level * 100}%`, background: color }}
                />
              </div>
              <span
                style={{
                  minWidth: 30,
                  fontSize: "0.7rem",
                  color,
                  fontWeight: 600,
                }}
                aria-hidden="true"
              >
                {Math.round(level * 100)}%
              </span>
            </div>
          );
        })}
      </div>

      {/* ── Demo simulation controls (organiser view) ──────────────── */}
      {showControls && (
        <div
          style={{
            marginTop: 14,
            padding: "12px",
            background: "rgba(255,215,0,0.04)",
            border: "1px solid rgba(255,215,0,0.15)",
            borderRadius: 10,
          }}
          aria-label="Demo simulation controls"
        >
          <div
            style={{
              fontSize: "0.7rem",
              fontWeight: 700,
              textTransform: "uppercase",
              letterSpacing: "0.08em",
              color: "var(--color-accent)",
              marginBottom: 10,
            }}
          >
            ⚡ Demo Controls
          </div>

          <label
            htmlFor="sim-zone"
            style={{
              fontSize: "0.72rem",
              color: "var(--text-muted)",
              display: "block",
              marginBottom: 4,
            }}
          >
            Zone
          </label>
          <select
            id="sim-zone"
            value={simZone}
            onChange={(e) => setSimZone(e.target.value)}
            style={{
              width: "100%",
              background: "var(--bg-input)",
              border: "1px solid var(--border-subtle)",
              color: "var(--text-primary)",
              borderRadius: 6,
              padding: "4px 8px",
              fontSize: "0.78rem",
              marginBottom: 8,
            }}
            aria-label="Select zone to simulate congestion"
          >
            {zones.map((z) => (
              <option key={z} value={z}>
                {ZONE_LABELS[z]}
              </option>
            ))}
          </select>

          <label
            htmlFor="sim-level"
            style={{
              fontSize: "0.72rem",
              color: "var(--text-muted)",
              display: "block",
              marginBottom: 4,
            }}
          >
            Congestion: {Math.round(simLevel * 100)}%
          </label>
          <input
            id="sim-level"
            type="range"
            min={0}
            max={1}
            step={0.05}
            value={simLevel}
            onChange={(e) => setSimLevel(Number(e.target.value))}
            style={{
              width: "100%",
              marginBottom: 10,
              accentColor: "var(--color-accent)",
            }}
            aria-label={`Congestion level: ${Math.round(simLevel * 100)}%`}
          />

          <div style={{ display: "flex", gap: 8 }}>
            <button
              onClick={handleSimulate}
              disabled={isSimulating}
              style={{
                flex: 1,
                padding: "6px 0",
                background: "linear-gradient(135deg, #e53935, #c62828)",
                border: "none",
                borderRadius: 6,
                color: "white",
                fontSize: "0.78rem",
                fontWeight: 600,
                cursor: "pointer",
                opacity: isSimulating ? 0.5 : 1,
              }}
              aria-label="Apply simulated congestion"
            >
              🚨 Spike Crowd
            </button>
            <button
              onClick={handleReset}
              disabled={isSimulating}
              style={{
                flex: 1,
                padding: "6px 0",
                background: "var(--bg-card)",
                border: "1px solid var(--border-subtle)",
                borderRadius: 6,
                color: "var(--text-secondary)",
                fontSize: "0.78rem",
                cursor: "pointer",
                opacity: isSimulating ? 0.5 : 1,
              }}
              aria-label="Reset all zones to baseline"
            >
              ↺ Reset All
            </button>
          </div>

          {simFeedback && (
            <p
              style={{
                marginTop: 8,
                fontSize: "0.72rem",
                color: "var(--text-muted)",
              }}
              role="status"
              aria-live="polite"
            >
              {simFeedback}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
