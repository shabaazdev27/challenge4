import { render } from "@testing-library/react";
import StadiumMap from "../components/StadiumMap";

describe("StadiumMap", () => {
  it("renders correctly", () => {
    const { container } = render(<StadiumMap route={null} crowdState={{}} />);
    expect(container).toBeInTheDocument();
  });
});
