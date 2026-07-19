"use client";

import React, { useEffect, useRef, memo } from "react";
import { RouteResult } from "@/lib/api";

// ─── Node data (matches metlife-graph.json exactly) ───────────────────────────

interface NodeDef {
  nodeId: string;
  type: string;
  name: string;
  floor: number;
  svgX: number;
  svgY: number;
  zone: string;
  accessible: boolean;
}

const NODES: NodeDef[] = [
  { nodeId: "GATE_A", type: "gate", name: "Gate A", floor: 1, svgX: 640, svgY: 295, zone: "ZONE_GATE_A", accessible: true },
  { nodeId: "CONC_MAIN_NE", type: "concourse", name: "NE Concourse", floor: 1, svgX: 570, svgY: 160, zone: "ZONE_CONC_NE", accessible: true },
  { nodeId: "GATE_B", type: "gate", name: "Gate B", floor: 1, svgX: 400, svgY: 105, zone: "ZONE_GATE_B", accessible: true },
  { nodeId: "CONC_MAIN_NW", type: "concourse", name: "NW Concourse", floor: 1, svgX: 230, svgY: 160, zone: "ZONE_CONC_NW", accessible: true },
  { nodeId: "GATE_C", type: "gate", name: "Gate C", floor: 1, svgX: 160, svgY: 295, zone: "ZONE_GATE_C", accessible: true },
  { nodeId: "CONC_MAIN_SW", type: "concourse", name: "SW Concourse", floor: 1, svgX: 230, svgY: 430, zone: "ZONE_CONC_SW", accessible: true },
  { nodeId: "GATE_D", type: "gate", name: "Gate D", floor: 1, svgX: 400, svgY: 485, zone: "ZONE_GATE_D", accessible: true },
  { nodeId: "CONC_MAIN_SE", type: "concourse", name: "SE Concourse", floor: 1, svgX: 570, svgY: 430, zone: "ZONE_CONC_SE", accessible: true },
  { nodeId: "REST_N_ACC", type: "restroom", name: "Restroom N ♿", floor: 1, svgX: 400, svgY: 130, zone: "ZONE_GATE_B", accessible: true },
  { nodeId: "REST_S_ACC", type: "restroom", name: "Restroom S ♿", floor: 1, svgX: 400, svgY: 460, zone: "ZONE_GATE_D", accessible: true },
  { nodeId: "REST_E", type: "restroom", name: "Restroom E", floor: 1, svgX: 625, svgY: 220, zone: "ZONE_GATE_A", accessible: false },
  { nodeId: "REST_W", type: "restroom", name: "Restroom W", floor: 1, svgX: 185, svgY: 370, zone: "ZONE_GATE_C", accessible: false },
  { nodeId: "FOOD_N", type: "food", name: "Food N", floor: 1, svgX: 330, svgY: 120, zone: "ZONE_GATE_B", accessible: true },
  { nodeId: "FOOD_S", type: "food", name: "Food S", floor: 1, svgX: 470, svgY: 465, zone: "ZONE_GATE_D", accessible: true },
  { nodeId: "FOOD_E", type: "food", name: "Food E", floor: 1, svgX: 615, svgY: 375, zone: "ZONE_GATE_A", accessible: true },
  { nodeId: "FOOD_W", type: "food", name: "Food W", floor: 1, svgX: 185, svgY: 220, zone: "ZONE_GATE_C", accessible: true },
  { nodeId: "ELEV_E", type: "elevator", name: "Elevator E", floor: 1, svgX: 592, svgY: 295, zone: "ZONE_GATE_A", accessible: true },
  { nodeId: "ELEV_W", type: "elevator", name: "Elevator W", floor: 1, svgX: 210, svgY: 295, zone: "ZONE_CONC_NW", accessible: true },
  { nodeId: "RAMP_NE", type: "ramp", name: "Ramp NE", floor: 1, svgX: 530, svgY: 185, zone: "ZONE_CONC_NE", accessible: false },
  { nodeId: "RAMP_SW", type: "ramp", name: "Ramp SW", floor: 1, svgX: 265, svgY: 405, zone: "ZONE_CONC_SW", accessible: false },
  { nodeId: "FIRSTAID", type: "firstaid", name: "First Aid", floor: 1, svgX: 480, svgY: 295, zone: "ZONE_GATE_A", accessible: true },
  { nodeId: "MERCH_MAIN", type: "merchandise", name: "Merchandise", floor: 1, svgX: 490, svgY: 135, zone: "ZONE_CONC_NE", accessible: true },
  { nodeId: "TEAM_ENTRY", type: "team_entry", name: "VIP Entry", floor: 1, svgX: 600, svgY: 450, zone: "ZONE_CONC_SE", accessible: true },
  { nodeId: "CONC_UPPER_NE", type: "concourse", name: "Upper NE", floor: 2, svgX: 520, svgY: 200, zone: "ZONE_UPPER", accessible: true },
  { nodeId: "CONC_UPPER_NW", type: "concourse", name: "Upper NW", floor: 2, svgX: 280, svgY: 200, zone: "ZONE_UPPER", accessible: true },
  { nodeId: "CONC_UPPER_SW", type: "concourse", name: "Upper SW", floor: 2, svgX: 280, svgY: 390, zone: "ZONE_UPPER", accessible: true },
  { nodeId: "CONC_UPPER_SE", type: "concourse", name: "Upper SE", floor: 2, svgX: 520, svgY: 390, zone: "ZONE_UPPER", accessible: true },
  { nodeId: "SEC_114", type: "section", name: "Sec 114", floor: 1, svgX: 440, svgY: 430, zone: "ZONE_GATE_D", accessible: true },
  { nodeId: "SEC_120", type: "section", name: "Sec 120", floor: 1, svgX: 592, svgY: 340, zone: "ZONE_GATE_A", accessible: true },
  { nodeId: "SEC_214", type: "section", name: "Sec 214", floor: 2, svgX: 460, svgY: 395, zone: "ZONE_UPPER", accessible: true },
  { nodeId: "SEC_320", type: "section", name: "Sec 320", floor: 2, svgX: 515, svgY: 210, zone: "ZONE_UPPER", accessible: true },
];

const NODE_MAP = new Map(NODES.map((n) => [n.nodeId, n]));

// ─── Edges for background rendering ──────────────────────────────────────────

const EDGES: [string, string][] = [
  ["GATE_A", "CONC_MAIN_NE"], ["GATE_A", "CONC_MAIN_SE"],
  ["CONC_MAIN_NE", "GATE_B"], ["GATE_B", "CONC_MAIN_NW"],
  ["CONC_MAIN_NW", "GATE_C"], ["GATE_C", "CONC_MAIN_SW"],
  ["CONC_MAIN_SW", "GATE_D"], ["GATE_D", "CONC_MAIN_SE"],
  ["GATE_A", "REST_E"], ["GATE_A", "FOOD_E"], ["GATE_A", "ELEV_E"],
  ["GATE_B", "REST_N_ACC"], ["GATE_B", "FOOD_N"],
  ["GATE_C", "REST_W"], ["GATE_C", "FOOD_W"],
  ["GATE_D", "REST_S_ACC"], ["GATE_D", "FOOD_S"], ["GATE_D", "SEC_114"],
  ["GATE_A", "SEC_120"], ["GATE_A", "FIRSTAID"],
  ["CONC_MAIN_NE", "MERCH_MAIN"], ["CONC_MAIN_NE", "RAMP_NE"],
  ["CONC_MAIN_SW", "RAMP_SW"], ["CONC_MAIN_NW", "ELEV_W"],
  ["CONC_MAIN_SE", "TEAM_ENTRY"],
  ["ELEV_E", "CONC_UPPER_SE"], ["ELEV_W", "CONC_UPPER_NW"],
  ["RAMP_NE", "CONC_UPPER_NE"], ["RAMP_SW", "CONC_UPPER_SW"],
  ["CONC_UPPER_NE", "CONC_UPPER_NW"], ["CONC_UPPER_NW", "CONC_UPPER_SW"],
  ["CONC_UPPER_SW", "CONC_UPPER_SE"], ["CONC_UPPER_SE", "CONC_UPPER_NE"],
  ["CONC_UPPER_SE", "SEC_214"], ["CONC_UPPER_NE", "SEC_320"],
];

// ─── Type → colour / icon ─────────────────────────────────────────────────────

const TYPE_CONFIG: Record<string, { color: string; icon: string }> = {
  gate:        { color: "#FFD700", icon: "🚪" },
  concourse:   { color: "#78909C", icon: "•"  },
  restroom:    { color: "#4FC3F7", icon: "🚻" },
  food:        { color: "#FF8A65", icon: "🍔" },
  elevator:    { color: "#CE93D8", icon: "🛗" },
  ramp:        { color: "#A5D6A7", icon: "↗"  },
  firstaid:    { color: "#EF5350", icon: "🏥" },
  merchandise: { color: "#FFB74D", icon: "🛍" },
  section:     { color: "#90CAF9", icon: "#"  },
  team_entry:  { color: "#FF7043", icon: "⭐" },
};

function congestionColor(level: number): string {
  if (level >= 0.6) return "rgba(229, 57, 53, 0.22)";
  if (level >= 0.3) return "rgba(251, 140, 0, 0.18)";
  return "rgba(67, 160, 71, 0.1)";
}

// ─── Zone → approximate SVG circle for overlay ───────────────────────────────

const ZONE_CIRCLES: Record<string, { cx: number; cy: number; r: number }> = {
  ZONE_GATE_A:   { cx: 625, cy: 300, r: 60 },
  ZONE_GATE_B:   { cx: 400, cy: 115, r: 55 },
  ZONE_GATE_C:   { cx: 175, cy: 300, r: 55 },
  ZONE_GATE_D:   { cx: 400, cy: 475, r: 55 },
  ZONE_CONC_NE:  { cx: 545, cy: 170, r: 50 },
  ZONE_CONC_NW:  { cx: 255, cy: 170, r: 45 },
  ZONE_CONC_SW:  { cx: 255, cy: 420, r: 45 },
  ZONE_CONC_SE:  { cx: 575, cy: 440, r: 50 },
  ZONE_UPPER:    { cx: 400, cy: 295, r: 95 },
};

// ─── Props ────────────────────────────────────────────────────────────────────

interface StadiumMapProps {
  route: RouteResult | null;
  crowdState: Record<string, number>;
}

// ─── Component ────────────────────────────────────────────────────────────────

const StadiumMap = memo(function StadiumMap({
  route,
  crowdState,
}: StadiumMapProps) {
  const routeLineRef = useRef<SVGPolylineElement>(null);

  // Animate route line when path changes
  useEffect(() => {
    const el = routeLineRef.current;
    if (!el) return;
    const length = el.getTotalLength?.() ?? 1000;
    el.style.strokeDasharray = `${length}`;
    el.style.strokeDashoffset = `${length}`;
    // Trigger reflow
    void el.getBoundingClientRect();
    el.style.transition = "stroke-dashoffset 1.5s ease-out";
    el.style.strokeDashoffset = "0";
  }, [route]);

  // Build route polyline points string
  const routePoints = route?.nodePath
    .map((id) => {
      const n = NODE_MAP.get(id);
      return n ? `${n.svgX},${n.svgY}` : null;
    })
    .filter(Boolean)
    .join(" ");

  const destNodeId = route?.nodePath.at(-1);
  const destNode = destNodeId ? NODE_MAP.get(destNodeId) : null;

  return (
    <div className="map-panel" style={{ position: "relative", overflow: "hidden" }}>
      {/* Floor indicator */}
      <div className="map-panel-header">
        <span className="badge badge--live">● Live</span>
        <span className="badge badge--floor">MetLife Stadium</span>
      </div>

      <svg
        viewBox="0 0 800 600"
        style={{ width: "100%", height: "100%", display: "block" }}
        role="img"
        aria-label="MetLife Stadium interactive map showing navigation route"
      >
        {/* ── Stadium shell ───────────────────────────────────────────── */}
        <ellipse cx="400" cy="295" rx="270" ry="215"
          fill="none" stroke="#1a2540" strokeWidth="24" />
        <ellipse cx="400" cy="295" rx="270" ry="215"
          fill="none" stroke="#243059" strokeWidth="2" />

        {/* Playing field */}
        <ellipse cx="400" cy="295" rx="155" ry="120"
          fill="rgba(34, 85, 34, 0.35)" stroke="rgba(67,160,71,0.4)" strokeWidth="1.5" />
        <text x="400" y="295" textAnchor="middle" dominantBaseline="middle"
          fill="rgba(67,160,71,0.6)" fontSize="11" fontFamily="Inter, sans-serif"
          fontWeight="600">FIFA WORLD CUP 2026</text>
        <text x="400" y="311" textAnchor="middle" dominantBaseline="middle"
          fill="rgba(67,160,71,0.4)" fontSize="9" fontFamily="Inter, sans-serif">
          MetLife Stadium
        </text>

        {/* ── Crowd congestion zone overlays ──────────────────────────── */}
        {Object.entries(crowdState).map(([zoneId, level]) => {
          const circle = ZONE_CIRCLES[zoneId];
          if (!circle || level < 0.15) return null;
          return (
            <circle
              key={zoneId}
              cx={circle.cx} cy={circle.cy} r={circle.r}
              fill={congestionColor(level)}
              style={{ transition: "fill 0.5s ease" }}
              aria-hidden="true"
            />
          );
        })}

        {/* ── Background edges ────────────────────────────────────────── */}
        {EDGES.map(([a, b]) => {
          const na = NODE_MAP.get(a);
          const nb = NODE_MAP.get(b);
          if (!na || !nb) return null;
          const isDifferentFloor = na.floor !== nb.floor;
          return (
            <line key={`${a}-${b}`}
              x1={na.svgX} y1={na.svgY} x2={nb.svgX} y2={nb.svgY}
              stroke={isDifferentFloor ? "rgba(206,147,216,0.25)" : "rgba(255,255,255,0.06)"}
              strokeWidth={isDifferentFloor ? 1.5 : 1}
              strokeDasharray={isDifferentFloor ? "4 3" : "none"}
              aria-hidden="true"
            />
          );
        })}

        {/* ── Animated route line ─────────────────────────────────────── */}
        {routePoints && (
          <>
            {/* Glow layer */}
            <polyline
              points={routePoints}
              fill="none"
              stroke="rgba(255, 215, 0, 0.15)"
              strokeWidth="14"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            />
            {/* Main route line */}
            <polyline
              ref={routeLineRef}
              points={routePoints}
              fill="none"
              stroke="#FFD700"
              strokeWidth="3.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeDasharray="12 6"
              aria-label={`Route to ${destNode?.name ?? "destination"}`}
            />
          </>
        )}

        {/* ── Nodes ───────────────────────────────────────────────────── */}
        {NODES.map((node) => {
          const cfg = TYPE_CONFIG[node.type] ?? TYPE_CONFIG.concourse;
          const isGate = node.type === "gate";
          const isOnRoute = route?.nodePath.includes(node.nodeId);
          const isDest = node.nodeId === destNodeId;
          const r = isGate ? 14 : isDest ? 12 : isOnRoute ? 9 : 6;

          return (
            <g key={node.nodeId} aria-label={node.name}>
              {/* Pulse ring on destination */}
              {isDest && (
                <circle
                  cx={node.svgX} cy={node.svgY} r={r + 6}
                  fill="none"
                  stroke={cfg.color}
                  strokeWidth="2"
                  opacity="0.5"
                  style={{ animation: "node-pulse 1.5s infinite ease-in-out" }}
                />
              )}

              {/* Node circle */}
              <circle
                cx={node.svgX} cy={node.svgY} r={r}
                fill={isOnRoute ? cfg.color : `${cfg.color}44`}
                stroke={cfg.color}
                strokeWidth={isGate ? 2 : 1}
              />

              {/* Gate labels */}
              {isGate && (
                <text
                  x={node.svgX}
                  y={node.svgY + (node.svgY > 295 ? 28 : -22)}
                  textAnchor="middle"
                  fill={cfg.color}
                  fontSize="10"
                  fontFamily="Outfit, Inter, sans-serif"
                  fontWeight="700"
                >
                  {node.name}
                </text>
              )}

              {/* Route node labels */}
              {isOnRoute && !isGate && !isDest && (
                <text
                  x={node.svgX + 10}
                  y={node.svgY - 10}
                  fill="rgba(255,255,255,0.7)"
                  fontSize="8"
                  fontFamily="Inter, sans-serif"
                >
                  {node.name}
                </text>
              )}

              {/* Destination label */}
              {isDest && (
                <text
                  x={node.svgX}
                  y={node.svgY + r + 16}
                  textAnchor="middle"
                  fill={cfg.color}
                  fontSize="11"
                  fontFamily="Outfit, Inter, sans-serif"
                  fontWeight="700"
                >
                  📍 {node.name}
                </text>
              )}
            </g>
          );
        })}
      </svg>

      {/* Legend */}
      <div style={{
        position: "absolute", bottom: 16, left: 16,
        background: "rgba(10,14,26,0.85)",
        backdropFilter: "blur(8px)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 10,
        padding: "8px 12px",
        display: "flex",
        gap: 12,
        fontSize: 11,
        color: "rgba(255,255,255,0.5)",
      }}>
        {[
          { color: "#FFD700", label: "Gate" },
          { color: "#4FC3F7", label: "Restroom" },
          { color: "#FF8A65", label: "Food" },
          { color: "#CE93D8", label: "Elevator" },
          { color: "#EF5350", label: "First Aid" },
        ].map(({ color, label }) => (
          <span key={label} style={{ display: "flex", alignItems: "center", gap: 4 }}>
            <span style={{ width: 8, height: 8, borderRadius: "50%", background: color, display: "inline-block" }} />
            {label}
          </span>
        ))}
      </div>
    </div>
  );
});

export default StadiumMap;
