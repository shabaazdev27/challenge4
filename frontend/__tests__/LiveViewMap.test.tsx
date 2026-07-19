import { render, screen } from "@testing-library/react";
import LiveViewMap from "../components/LiveViewMap";

describe("LiveViewMap", () => {
  it("renders a map frame", () => {
    render(<LiveViewMap apiKey="fake-key" />);
    expect(screen.getByTestId("stadium-map")).toBeInTheDocument();
  });
});
