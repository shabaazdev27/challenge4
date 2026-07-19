import { render, screen, act } from "@testing-library/react";
import LiveViewMap from "../components/LiveViewMap";

describe("LiveViewMap", () => {
  let mockPanorama: jest.Mock;

  beforeEach(() => {
    mockPanorama = jest.fn();
    // Reset global window properties
    delete (window as any).google;
    delete (window as any).initMap;
  });

  it("renders a map frame and dynamically appends script", () => {
    const { unmount } = render(<LiveViewMap apiKey="fake-key" />);
    expect(screen.getByTestId("stadium-map")).toBeInTheDocument();

    // Verify initMap callback is exposed
    expect(window.initMap).toBeDefined();

    // Define google mock before invoking callback
    window.google = {
      maps: {
        StreetViewPanorama: mockPanorama,
      },
    };

    // Invoke initMap and verify mock is called
    act(() => {
      window.initMap();
    });
    expect(mockPanorama).toHaveBeenCalled();

    unmount();
  });

  it("immediately instantiates StreetViewPanorama when window.google already exists", () => {
    window.google = {
      maps: {
        StreetViewPanorama: mockPanorama,
      },
    };

    render(<LiveViewMap apiKey="fake-key" />);
    expect(mockPanorama).toHaveBeenCalled();
  });
});
