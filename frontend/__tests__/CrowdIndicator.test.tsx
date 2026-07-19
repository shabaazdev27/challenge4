import { render, screen, fireEvent, act } from "@testing-library/react";
import CrowdIndicator from "../components/CrowdIndicator";
import * as api from "../lib/api";

jest.mock("../lib/api", () => ({
  ...jest.requireActual("../lib/api"),
  simulateCrowd: jest.fn(),
  resetCrowd: jest.fn(),
}));

describe("CrowdIndicator", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders correctly with default state", () => {
    render(<CrowdIndicator crowdState={{ ZONE_GATE_A: 0.5 }} showControls={false} />);
    expect(screen.getByText(/Live Crowd Density/i)).toBeInTheDocument();
    expect(screen.queryByText(/⚡ Demo Controls/i)).not.toBeInTheDocument();
  });

  it("renders controls and handles spike crowd simulation successfully", async () => {
    (api.simulateCrowd as jest.Mock).mockResolvedValue("Zone NE spiked successfully");

    render(<CrowdIndicator crowdState={{}} showControls={true} />);
    expect(screen.getByText(/⚡ Demo Controls/i)).toBeInTheDocument();

    const spikeBtn = screen.getByRole("button", { name: /Apply simulated congestion/i });
    expect(spikeBtn).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(spikeBtn);
    });

    expect(api.simulateCrowd).toHaveBeenCalledWith("ZONE_CONC_NE", 0.9);
    expect(await screen.findByText("Zone NE spiked successfully")).toBeInTheDocument();
  });

  it("handles reset crowd simulation successfully", async () => {
    (api.resetCrowd as jest.Mock).mockResolvedValue(undefined);

    render(<CrowdIndicator crowdState={{}} showControls={true} />);

    const resetBtn = screen.getByRole("button", { name: /Reset all zones to baseline/i });
    await act(async () => {
      fireEvent.click(resetBtn);
    });

    expect(api.resetCrowd).toHaveBeenCalled();
    expect(await screen.findByText("All zones reset to baseline.")).toBeInTheDocument();
  });

  it("handles simulate error gracefully", async () => {
    (api.simulateCrowd as jest.Mock).mockRejectedValue(new Error("Network Error"));

    render(<CrowdIndicator crowdState={{}} showControls={true} />);

    const spikeBtn = screen.getByRole("button", { name: /Apply simulated congestion/i });
    await act(async () => {
      fireEvent.click(spikeBtn);
    });

    expect(await screen.findByText("Simulation failed — check backend connection.")).toBeInTheDocument();
  });

  it("handles reset error gracefully", async () => {
    (api.resetCrowd as jest.Mock).mockRejectedValue(new Error("Network Error"));

    render(<CrowdIndicator crowdState={{}} showControls={true} />);

    const resetBtn = screen.getByRole("button", { name: /Reset all zones to baseline/i });
    await act(async () => {
      fireEvent.click(resetBtn);
    });

    expect(await screen.findByText("Reset failed.")).toBeInTheDocument();
  });
});
