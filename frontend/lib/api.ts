/**
 * StadiumMate API client.
 *
 * All backend calls go through this module so the base URL is configured
 * in exactly one place (NEXT_PUBLIC_API_URL env var).
 */

const BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

// ─── Types (mirroring the Java models) ───────────────────────────────────────

export interface RouteStep {
  nodeId: string;
  nodeName: string;
  instruction: string;
  edgeType: "walk" | "elevator" | "ramp";
  distanceFromPrevious: number;
}

export interface RouteResult {
  steps: RouteStep[];
  totalDistance: number;
  estimatedMinutes: number;
  nodePath: string[];
  rerouted: boolean;
}

export interface ChatResponse {
  narration: string;
  language: string;
  route: RouteResult | null;
  congestionWarning: string | null;
  crowdState: Record<string, number>;
  sessionId: string;
}

export interface ChatRequest {
  message: string;
  sessionId: string;
  currentLocation: string;
}

export interface CrowdState {
  [zoneId: string]: number; // 0.0–1.0
}

// ─── Chat endpoint ────────────────────────────────────────────────────────────

/**
 * Sends a fan message to the backend and returns the AI response + route.
 */
export async function sendChatMessage(
  request: ChatRequest
): Promise<ChatResponse> {
  const res = await fetch(`${BASE_URL}/api/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!res.ok) {
    const errorText = await res.text().catch(() => "Unknown error");
    throw new Error(`Chat API error ${res.status}: ${errorText}`);
  }

  return res.json();
}

// ─── Crowd state endpoint ─────────────────────────────────────────────────────

/**
 * Fetches current zone congestion levels for the map heat overlay.
 */
export async function fetchCrowdState(): Promise<CrowdState> {
  const res = await fetch(`${BASE_URL}/api/crowd`, {
    next: { revalidate: 0 }, // Always fresh
  });
  if (!res.ok) return {};
  return res.json();
}

/**
 * Simulates crowd congestion on a specific zone — used in demo mode.
 *
 * @param zoneId   e.g. "ZONE_CONC_NE"
 * @param level    0.0 (clear) – 1.0 (fully blocked)
 */
export async function simulateCrowd(
  zoneId: string,
  level: number
): Promise<string> {
  const res = await fetch(`${BASE_URL}/api/crowd/simulate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ zoneId, level }),
  });
  return res.text();
}

/**
 * Resets all crowd zones to baseline (0.1) — useful between demo runs.
 */
export async function resetCrowd(): Promise<void> {
  await fetch(`${BASE_URL}/api/crowd/reset`, { method: "POST" });
}
