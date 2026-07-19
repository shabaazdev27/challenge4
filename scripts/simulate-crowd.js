#!/usr/bin/env node
/**
 * scripts/simulate-crowd.js
 *
 * Demo crowd simulator — randomly mutates 1–2 zone congestion levels in
 * Firestore every 30 seconds to demonstrate real-time crowd-aware rerouting.
 *
 * ⚡ KEY DEMO MOMENT:
 *   When a zone spikes above 0.6, the next /api/chat request will route
 *   around it and Gemini narrates "I've rerouted you due to congestion".
 *
 * Usage (run in a separate terminal during the demo):
 *   GCP_PROJECT_ID=your-project-id node scripts/simulate-crowd.js
 *
 * Manual override (spike a specific zone for demo):
 *   SPIKE_ZONE=ZONE_CONC_NE SPIKE_LEVEL=0.9 node scripts/simulate-crowd.js --once
 */

"use strict";

const admin = require("firebase-admin");

// ─── Configuration ────────────────────────────────────────────────────────────

const PROJECT_ID = process.env.GCP_PROJECT_ID;
if (!PROJECT_ID) {
  console.error("❌ GCP_PROJECT_ID environment variable is required.");
  process.exit(1);
}

const INTERVAL_MS = 30_000; // 30 seconds
const SPIKE_ZONE = process.env.SPIKE_ZONE;
const SPIKE_LEVEL = SPIKE_ZONE ? parseFloat(process.env.SPIKE_LEVEL ?? "0.9") : null;
const ONCE = process.argv.includes("--once");

const ZONES = [
  "ZONE_GATE_A", "ZONE_GATE_B", "ZONE_GATE_C", "ZONE_GATE_D",
  "ZONE_CONC_NE", "ZONE_CONC_NW", "ZONE_CONC_SW", "ZONE_CONC_SE",
  "ZONE_UPPER",
];

// Baseline distribution — naturally higher congestion at main gates
const BASELINES = {
  ZONE_GATE_A: 0.2, ZONE_GATE_B: 0.3, ZONE_GATE_C: 0.15,
  ZONE_GATE_D: 0.25, ZONE_CONC_NE: 0.15, ZONE_CONC_NW: 0.1,
  ZONE_CONC_SW: 0.1, ZONE_CONC_SE: 0.2, ZONE_UPPER: 0.15,
};

// ─── Initialise Firebase ──────────────────────────────────────────────────────

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: PROJECT_ID,
});

const db = admin.firestore();

// ─── Helpers ──────────────────────────────────────────────────────────────────

function randBetween(min, max) {
  return min + Math.random() * (max - min);
}

function formatLevel(level) {
  return `${Math.round(level * 100)}%`;
}

async function updateZone(zoneId, level) {
  const ref = db.collection("crowd_state").doc(zoneId);
  await ref.set({
    congestionLevel: level,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  const bar = "█".repeat(Math.round(level * 20)) + "░".repeat(20 - Math.round(level * 20));
  const status = level >= 0.7 ? "🔴 HIGH" : level >= 0.4 ? "🟡 MED" : "🟢 LOW";
  console.log(`  ${zoneId.padEnd(18)} [${bar}] ${formatLevel(level)} ${status}`);
}

async function tick() {
  const ts = new Date().toLocaleTimeString();
  console.log(`\n⏱  ${ts} — Crowd state update`);

  if (SPIKE_ZONE && SPIKE_LEVEL !== null) {
    // Manual spike mode
    await updateZone(SPIKE_ZONE, SPIKE_LEVEL);
    // Also gradually lower all other zones toward baseline
    for (const zone of ZONES) {
      if (zone !== SPIKE_ZONE) {
        const baseline = BASELINES[zone] ?? 0.15;
        await updateZone(zone, baseline + randBetween(-0.05, 0.05));
      }
    }
  } else {
    // Random simulation mode — pick 1–2 zones to spike
    const shuffled = [...ZONES].sort(() => Math.random() - 0.5);
    const spikeCount = Math.random() < 0.3 ? 2 : 1;

    for (const zone of ZONES) {
      const baseline = BASELINES[zone] ?? 0.15;
      let level;
      if (shuffled.indexOf(zone) < spikeCount) {
        // Spike this zone
        level = randBetween(0.65, 0.92);
      } else {
        // Normal variance around baseline
        level = Math.max(0.05, baseline + randBetween(-0.1, 0.15));
      }
      await updateZone(zone, parseFloat(level.toFixed(2)));
    }
  }
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  console.log(`\n🏟️  StadiumMate Crowd Simulator — Project: ${PROJECT_ID}`);
  if (SPIKE_ZONE) {
    console.log(`⚡ Manual spike: ${SPIKE_ZONE} → ${formatLevel(SPIKE_LEVEL ?? 0)}`);
  } else {
    console.log(`🔄 Auto simulation every ${INTERVAL_MS / 1000}s`);
  }
  console.log("Press Ctrl+C to stop.\n");

  await tick();

  if (!ONCE) {
    setInterval(tick, INTERVAL_MS);
  } else {
    process.exit(0);
  }
}

main().catch((err) => {
  console.error("Simulator error:", err);
  process.exit(1);
});
