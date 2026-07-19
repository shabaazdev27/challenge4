# StadiumMate — Implementation Plan
## GenAI Multilingual Navigation & Crowd-Aware Concierge (FIFA World Cup 2026)

A fan-facing PWA that combines Gemini AI for multilingual intent parsing & narration with a pure Java A* pathfinding engine over a stadium graph, crowd-aware dynamic rerouting via Firestore, and Web Speech API voice I/O — all deployed on GCP.

---

## User Review Required

> [!IMPORTANT]
> **GCP Project Setup**: This plan assumes you will provide a GCP Project ID. The backend uses **Vertex AI (Gemini 2.0 Flash)**, **Firestore**, and **Cloud Run**. You'll need to enable these APIs and create a service account with the appropriate roles before running.

> [!IMPORTANT]
> **Target Stack Decision**: Per your plan, the frontend is **Next.js PWA** and the backend is **Java Spring Boot on Cloud Run**. Given the 3–5 day hackathon timeline and the solo build constraint, I'll implement the full stack locally-runnable first, then containerize for Cloud Run.

> [!WARNING]
> **Stadium Graph**: MetLife Stadium data will be a ~35-node hand-crafted graph (traced from public seating charts). This is accurate enough for demo purposes and demonstrates the routing mechanism clearly.

---

## Open Questions

> [!IMPORTANT]
> 1. **GCP Project ID**: Please provide your GCP Project ID so I can pre-configure the backend and Docker files correctly.
> 2. **Gemini API Key vs. Service Account**: Do you want to use a plain Gemini API key (faster to set up) or a full GCP Service Account with Vertex AI? The plan defaults to **Vertex AI with Application Default Credentials** for production alignment.
> 3. **Deployment target**: Should I configure the Next.js frontend for Cloud Run as well, or will you serve it via Vercel/Netlify for the demo?

---

## Proposed Changes

### Project Structure

```
challenge4/
├── frontend/                    # Next.js 14 PWA
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx            # Main chat + map view
│   │   └── globals.css
│   ├── components/
│   │   ├── ChatInterface.tsx   # Chat panel + voice controls
│   │   ├── StadiumMap.tsx      # SVG floor plan + animated route
│   │   ├── RoutePanel.tsx      # Step-by-step turn list
│   │   └── CrowdIndicator.tsx  # Zone congestion badges
│   ├── hooks/
│   │   ├── useSpeech.ts        # Web Speech API (STT + TTS)
│   │   └── useChat.ts          # Chat state + backend calls
│   ├── lib/
│   │   └── api.ts              # Backend API client
│   ├── public/
│   │   ├── manifest.json       # PWA manifest
│   │   └── stadium-map.svg     # MetLife floor plan SVG
│   ├── next.config.js          # PWA config (next-pwa)
│   └── package.json
│
├── backend/                     # Java Spring Boot
│   ├── src/main/java/com/stadiummate/
│   │   ├── StadiumMateApplication.java
│   │   ├── controller/
│   │   │   ├── ChatController.java
│   │   │   └── CrowdController.java
│   │   ├── service/
│   │   │   ├── GeminiService.java      # Vertex AI calls
│   │   │   ├── RouteService.java       # A* pathfinding
│   │   │   ├── CrowdService.java       # Firestore crowd feed
│   │   │   └── GraphService.java       # Firestore graph loader
│   │   ├── model/
│   │   │   ├── StadiumNode.java
│   │   │   ├── ChatRequest.java
│   │   │   ├── ChatResponse.java
│   │   │   ├── IntentResult.java
│   │   │   └── RouteResult.java
│   │   └── config/
│   │       ├── FirebaseConfig.java
│   │       └── CorsConfig.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── data/
│   │       ├── metlife-graph.json      # Stadium node/edge data
│   │       └── seed-crowd.json         # Initial crowd state
│   ├── Dockerfile
│   └── pom.xml
│
├── scripts/
│   ├── seed-firestore.js               # Node.js seed script
│   └── simulate-crowd.js              # Crowd state mutator (demo)
│
├── infrastructure/
│   ├── cloudbuild.yaml                 # CI/CD config
│   └── firestore.rules
│
└── README.md
```

---

### Component 1: MetLife Stadium Graph Data

#### [NEW] `backend/src/main/resources/data/metlife-graph.json`
- ~35 nodes: Gates (A1-A4, B1-B4, C1-C4, D1-D4), Concourses (100/200/300 level), Restrooms (accessible + standard), Concessions, Elevators, First Aid, Team entrances
- Edges with `baseWeight` (distance in meters) + `type` (walk/ramp/elevator)
- Designed so routes visibly differ when crowd weights are applied

#### [NEW] `backend/src/main/resources/data/seed-crowd.json`
- Initial congestion levels (0.0–1.0) for each zone/gate

---

### Component 2: Java Spring Boot Backend

#### [NEW] `backend/src/main/java/com/stadiummate/service/GraphService.java`
- Loads graph from Firestore `stadium_graph` collection (with local JSON fallback for dev)
- Caches graph in-memory, refreshes on Firestore updates

#### [NEW] `backend/src/main/java/com/stadiummate/service/RouteService.java`
- Pure Java A* implementation over the stadium graph
- Applies crowd multipliers: `effectiveWeight = baseWeight * (1 + 2 * congestionLevel)`
- Returns ordered list of `RouteStep` (nodeId, name, instruction, coordinates)

#### [NEW] `backend/src/main/java/com/stadiummate/service/GeminiService.java`
- **Intent parsing**: Structured JSON output via Vertex AI — extracts `{ destinationType, destinationId, sourceNodeId, language, urgency }`
- **Reply builder**: Takes route steps + crowd context, returns a short friendly narration in the fan's language
- Uses `gemini-2.0-flash` with `responseMimeType: "application/json"` for intent parsing

#### [NEW] `backend/src/main/java/com/stadiummate/service/CrowdService.java`
- Reads from Firestore `crowd_state` collection
- Provides `getCongestionLevel(zoneId)` used by RouteService

#### [NEW] `backend/src/main/java/com/stadiummate/controller/ChatController.java`
- `POST /api/chat` — main endpoint: text in → intent parse → route compute → narration build → response out
- `GET /api/crowd` — returns current congestion state (for UI badges)

---

### Component 3: Next.js PWA Frontend

#### [NEW] `frontend/app/page.tsx`
- Split-panel layout: **left** = StadiumMap SVG with animated route path, **right** = chat panel
- Responsive: on mobile, map is collapsible / tab-switched

#### [NEW] `frontend/components/ChatInterface.tsx`
- Message history with user/assistant bubbles
- Mic button → `useSpeech` hook (SpeechRecognition)
- Send button for text input
- Language indicator badge (detected from Gemini response)

#### [NEW] `frontend/components/StadiumMap.tsx`
- SVG-based MetLife floor plan (schematic, not photorealistic)
- Animates the computed route as a dashed path with pulsing waypoints
- Highlights congested zones with color overlay (green→yellow→red)

#### [NEW] `frontend/hooks/useSpeech.ts`
- Wraps `window.SpeechRecognition` for voice input
- Wraps `window.speechSynthesis` for spoken output (TTS)
- Graceful fallback if API not available

#### [NEW] `frontend/public/manifest.json`
- PWA manifest for installability
- `display: standalone`, appropriate icons, theme colors

---

### Component 4: Crowd Simulation

#### [NEW] `scripts/simulate-crowd.js`
- Simple Node.js script that randomly mutates 1–2 zone congestion levels in Firestore every 30 seconds
- Run this in a terminal during the demo to trigger live rerouting

---

### Component 5: Infrastructure & DevOps

#### [NEW] `backend/Dockerfile`
- Multi-stage: Maven build → slim JRE 21 runtime
- Configured for Cloud Run (PORT env var)

#### [NEW] `infrastructure/firestore.rules`
- Read: public (fan sessions are anonymous)
- Write: authenticated only (backend service account)

#### [NEW] `README.md`
- Architecture diagram (Mermaid)
- Setup steps (GCP project setup, seed data, local dev, Cloud Run deploy)
- Demo script (3 scenarios)
- Assumptions documented

---

## Verification Plan

### Automated Tests
- `backend/src/test/java/com/stadiummate/RouteServiceTest.java` — unit tests for A* (verifies shortest path, verifies crowd weight changes route)
- `backend/src/test/java/com/stadiummate/GeminiServiceTest.java` — mocked Vertex AI call, verifies JSON parsing
- Frontend: `npm test` — basic render tests for ChatInterface and StadiumMap components

### Manual Verification (3-scenario demo script)
1. **English text** — type "Where's the nearest accessible restroom to Section 214?" → route appears on map, narrated in English
2. **Spanish voice** — speak "¿Dónde está la salida más cercana?" → Gemini detects Spanish, responds in Spanish, TTS reads it aloud
3. **Crowd reroute** — run `simulate-crowd.js` to spike Gate C congestion, re-ask the same question → different route with "rerouting around Gate C due to congestion" message

### Build Checks
- `mvn test` passes in the backend
- `npm run build` passes with no type errors in the frontend
- Docker image builds locally via `docker build`
