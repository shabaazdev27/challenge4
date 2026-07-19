import { render, screen } from "@testing-library/react";
import CrowdIndicator from "../components/CrowdIndicator";

describe("CrowdIndicator", () => {
  it("renders correctly", () => {
    render(
      <CrowdIndicator crowdState={{ GATE_A: 0.5 }} showControls={false} />,
    );
    expect(screen.getByText(/Crowd Level/i)).toBeInTheDocument();
  });
});
