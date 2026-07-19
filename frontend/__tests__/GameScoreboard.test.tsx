import { render, screen, fireEvent, act } from "@testing-library/react";
import GameScoreboard from "../components/GameScoreboard";

describe("GameScoreboard", () => {
  let mockFetch: jest.Mock;

  beforeEach(() => {
    mockFetch = jest.fn();
    global.fetch = mockFetch;
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("renders user rank, score, and expands leaderboard", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ score: 1500 }),
    });

    render(<GameScoreboard userId="demo_fan_123" />);
    expect(screen.getByText(/Fan Arena/i)).toBeInTheDocument();

    // Verify initial score pill contains 1,240 before fetch updates
    expect(screen.getByText("1,240")).toBeInTheDocument();

    // Resolve initial mount fetch
    await act(async () => {
      jest.advanceTimersByTime(100);
    });

    // Expand leaderboard
    const toggleBtn = screen.getByRole("button", { name: /Show full leaderboard/i });
    expect(toggleBtn).toBeInTheDocument();
    
    act(() => {
      fireEvent.click(toggleBtn);
    });

    // Check that top fans are rendered
    expect(screen.getByText("⚡ AlphaFan")).toBeInTheDocument();
    expect(screen.getByText("🔥 GoalKing")).toBeInTheDocument();

    // Collapse leaderboard
    act(() => {
      fireEvent.click(toggleBtn);
    });
  });

  it("handles fetch errors gracefully without breaking rendering", async () => {
    mockFetch.mockRejectedValueOnce(new Error("Network Error"));

    render(<GameScoreboard userId="demo_fan_123" />);
    
    await act(async () => {
      jest.advanceTimersByTime(100);
    });

    expect(screen.getByText(/Fan Arena/i)).toBeInTheDocument();
  });
});
