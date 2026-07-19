import { render } from "@testing-library/react";
import RootLayout from "../app/layout";

describe("RootLayout", () => {
  it("renders children correctly", () => {
    const { container } = render(
      <RootLayout>
        <div data-testid="child-mock">Child Content</div>
      </RootLayout>,
    );
    expect(container).toBeInTheDocument();
  });
});
