import { renderHook, act } from "@testing-library/react";
import { useChat } from "../hooks/useChat";
import * as api from "../lib/api";

jest.mock("../lib/api", () => ({
  ...jest.requireActual("../lib/api"),
  sendChatMessage: jest.fn(),
}));

describe("useChat", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("initializes with default values", () => {
    const { result } = renderHook(() => useChat());
    expect(result.current.isLoading).toBe(false);
    expect(result.current.messages).toEqual([]);
    expect(result.current.currentRoute).toBeNull();
    expect(result.current.crowdState).toEqual({});
  });

  it("sends a message and handles response successfully", async () => {
    const mockResponse = {
      narration: "Here is your route.",
      language: "en",
      route: {
        steps: [],
        totalDistance: 100,
        estimatedMinutes: 2,
        nodePath: ["GATE_A", "REST_E"],
        rerouted: false,
      },
      congestionWarning: null,
      crowdState: { GATE_A: 0.2 },
      sessionId: "new-session-id",
    };

    (api.sendChatMessage as jest.Mock).mockResolvedValue(mockResponse);

    const { result } = renderHook(() => useChat());

    let promise;
    act(() => {
      promise = result.current.sendMessage("Where is restroom?", "GATE_A");
    });

    expect(result.current.isLoading).toBe(true);
    expect(result.current.messages.length).toBe(1);
    expect(result.current.messages[0].content).toBe("Where is restroom?");

    await act(async () => {
      await promise;
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.messages.length).toBe(2);
    expect(result.current.messages[1].content).toBe("Here is your route.");
    expect(result.current.currentRoute).toEqual(mockResponse.route);
    expect(result.current.crowdState).toEqual(mockResponse.crowdState);
    expect(result.current.currentLanguage).toBe("en");
  });

  it("gracefully handles sending message errors", async () => {
    (api.sendChatMessage as jest.Mock).mockRejectedValue(new Error("Connection error"));

    const { result } = renderHook(() => useChat());

    let promise;
    act(() => {
      promise = result.current.sendMessage("Hello", "GATE_A");
    });

    await act(async () => {
      await promise;
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.messages.length).toBe(2);
    expect(result.current.messages[1].content).toContain("having trouble connecting");
  });

  it("does not send message if text is blank", async () => {
    const { result } = renderHook(() => useChat());

    await act(async () => {
      await result.current.sendMessage("   ", "GATE_A");
    });

    expect(api.sendChatMessage).not.toHaveBeenCalled();
    expect(result.current.messages.length).toBe(0);
  });

  it("clears chat history successfully", () => {
    const { result } = renderHook(() => useChat());

    act(() => {
      result.current.clearChat();
    });

    expect(result.current.messages).toEqual([]);
    expect(result.current.currentRoute).toBeNull();
    expect(result.current.crowdState).toEqual({});
  });
});
