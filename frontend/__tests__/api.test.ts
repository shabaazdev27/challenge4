import { sendChatMessage, fetchCrowdState, simulateCrowd, resetCrowd } from "@/lib/api";

// Mock global fetch
global.fetch = jest.fn();

const mockFetch = fetch as jest.MockedFunction<typeof fetch>;

describe("api client", () => {
  beforeEach(() => {
    mockFetch.mockClear();
  });

  // ─── sendChatMessage ─────────────────────────────────────────────────────

  test("sendChatMessage returns parsed ChatResponse on success", async () => {
    const mockResponse = {
      narration: "Head to Gate B — about 3 minutes.",
      language: "en",
      route: {
        steps: [
          { nodeId: "GATE_A", nodeName: "Gate A", instruction: "Start at Gate A", edgeType: "walk", distanceFromPrevious: 0 },
          { nodeId: "GATE_B", nodeName: "Gate B", instruction: "Arrive at Gate B", edgeType: "walk", distanceFromPrevious: 230 },
        ],
        totalDistance: 230,
        estimatedMinutes: 3,
        nodePath: ["GATE_A", "GATE_B"],
        rerouted: false,
      },
      congestionWarning: null,
      crowdState: { ZONE_GATE_A: 0.2, ZONE_GATE_B: 0.3 },
      sessionId: "test-session-id",
    };

    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    } as Response);

    const result = await sendChatMessage({
      message: "How do I get to Gate B?",
      sessionId: "test-session",
      currentLocation: "GATE_A",
    });

    expect(result.narration).toBe("Head to Gate B — about 3 minutes.");
    expect(result.language).toBe("en");
    expect(result.route?.totalDistance).toBe(230);
    expect(result.route?.estimatedMinutes).toBe(3);

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/chat"),
      expect.objectContaining({ method: "POST" })
    );
  });

  test("sendChatMessage throws on non-ok response", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 503,
      text: async () => "Service Unavailable",
    } as Response);

    await expect(
      sendChatMessage({ message: "test", sessionId: "s", currentLocation: "GATE_A" })
    ).rejects.toThrow("Chat API error 503");
  });

  // ─── fetchCrowdState ─────────────────────────────────────────────────────

  test("fetchCrowdState returns crowd levels on success", async () => {
    const mockCrowd = {
      ZONE_GATE_A: 0.2,
      ZONE_CONC_NE: 0.75,
    };

    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockCrowd,
    } as Response);

    const result = await fetchCrowdState();
    expect(result.ZONE_GATE_A).toBe(0.2);
    expect(result.ZONE_CONC_NE).toBe(0.75);
  });

  test("fetchCrowdState returns empty object on failure", async () => {
    mockFetch.mockResolvedValueOnce({ ok: false } as Response);
    const result = await fetchCrowdState();
    expect(result).toEqual({});
  });

  // ─── simulateCrowd and resetCrowd ────────────────────────────────────────

  test("simulateCrowd returns success text on success", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      text: async () => "Simulated",
    } as Response);

    const result = await simulateCrowd("ZONE_GATE_A", 0.85);
    expect(result).toBe("Simulated");
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/crowd/simulate"),
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ zoneId: "ZONE_GATE_A", level: 0.85 }),
      })
    );
  });

  test("resetCrowd sends POST reset request", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
    } as Response);

    await resetCrowd();
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/crowd/reset"),
      expect.objectContaining({ method: "POST" })
    );
  });
});
