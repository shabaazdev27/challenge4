import { render } from "@testing-library/react";
import RootLayout from "../app/layout";

describe("RootLayout", () => {
  let originalError: typeof console.error;

  beforeAll(() => {
    originalError = console.error;
    console.error = jest.fn();
  });

  afterAll(() => {
    console.error = originalError;
  });

  it("renders children correctly", () => {
    const { container } = render(
      <RootLayout>
        <div data-testid="child-mock">Child Content</div>
      </RootLayout>,
      { container: document.documentElement }
    );
    expect(container).toBeInTheDocument();
  });
});
