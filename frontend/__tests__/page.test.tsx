import { render } from "@testing-library/react";
import HomePage from "../app/page";

// Mock child components to isolate HomePage test
jest.mock("@/components/StadiumMap", () => () => (
  <div data-testid="stadium-map-mock" />
));
jest.mock("@/components/ChatInterface", () => () => (
  <div data-testid="chat-interface-mock" />
));
jest.mock("@/components/CrowdIndicator", () => () => (
  <div data-testid="crowd-indicator-mock" />
));
jest.mock("@/components/GameScoreboard", () => () => (
  <div data-testid="game-scoreboard-mock" />
));
jest.mock("@/components/LiveViewMap", () => () => (
  <div data-testid="live-view-map-mock" />
));

describe("HomePage", () => {
  it("renders correctly", () => {
    const { container } = render(<HomePage />);
    expect(container).toBeInTheDocument();
  });
});
