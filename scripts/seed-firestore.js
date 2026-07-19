#!/usr/bin/env node
/**
 * scripts/seed-firestore.js
 *
 * Seeds the Firestore database with:
 *   1. stadium_graph/{nodeId}  — all 31 MetLife Stadium nodes
 *   2. crowd_state/{zoneId}    — initial congestion levels
 *
 * Usage:
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/sa.json \
 *   GCP_PROJECT_ID=your-project-id \
 *   node scripts/seed-firestore.js
 *
 * Or with gcloud ADC (after `gcloud auth application-default login`):
 *   GCP_PROJECT_ID=your-project-id node scripts/seed-firestore.js
 */

"use strict";

const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");

// ─── Configuration ────────────────────────────────────────────────────────────

const PROJECT_ID = process.env.GCP_PROJECT_ID;
if (!PROJECT_ID) {
  console.error("❌ GCP_PROJECT_ID environment variable is required.");
  process.exit(1);
}

// ─── Initialise Firebase Admin ────────────────────────────────────────────────

const credFile = process.env.GOOGLE_APPLICATION_CREDENTIALS;
const appOptions = { projectId: PROJECT_ID };

if (credFile) {
  appOptions.credential = admin.credential.cert(credFile);
  console.log(`🔑 Using service account: ${credFile}`);
} else {
  appOptions.credential = admin.credential.applicationDefault();
  console.log("🔑 Using Application Default Credentials");
}

admin.initializeApp(appOptions);
const db = admin.firestore();

// ─── Load local data ──────────────────────────────────────────────────────────

const graphDataPath = path.join(
  __dirname,
  "../backend/src/main/resources/data/metlife-graph.json"
);
const crowdDataPath = path.join(
  __dirname,
  "../backend/src/main/resources/data/seed-crowd.json"
);

const nodes = JSON.parse(fs.readFileSync(graphDataPath, "utf8"));
const crowdZones = JSON.parse(fs.readFileSync(crowdDataPath, "utf8"));

// ─── Seed functions ───────────────────────────────────────────────────────────

async function seedGraph() {
  const batch = db.batch();
  let count = 0;
  for (const node of nodes) {
    const ref = db.collection("stadium_graph").doc(node.nodeId);
    batch.set(ref, node);
    count++;
  }
  await batch.commit();
  console.log(`✅ stadium_graph: seeded ${count} nodes`);
}

async function seedCrowdState() {
  const batch = db.batch();
  let count = 0;
  for (const [zoneId, data] of Object.entries(crowdZones)) {
    const ref = db.collection("crowd_state").doc(zoneId);
    batch.set(ref, { ...data, updatedAt: admin.firestore.FieldValue.serverTimestamp() });
    count++;
  }
  await batch.commit();
  console.log(`✅ crowd_state: seeded ${count} zones`);
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  console.log(`\n🏟️  Seeding Firestore for project: ${PROJECT_ID}\n`);
  try {
    await seedGraph();
    await seedCrowdState();
    console.log("\n🎉 Seed complete! StadiumMate is ready.\n");
    process.exit(0);
  } catch (err) {
    console.error("❌ Seed failed:", err);
    process.exit(1);
  }
}

main();
