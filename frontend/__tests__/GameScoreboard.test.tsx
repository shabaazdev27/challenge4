import { render, screen } from "@testing-library/react";
import GameScoreboard from "../components/GameScoreboard";

describe("GameScoreboard", () => {
  it("renders correctly", () => {
    render(<GameScoreboard userId="demo_fan_123" />);
    expect(screen.getByText(/Fan Arena/i)).toBeInTheDocument();
  });
});
