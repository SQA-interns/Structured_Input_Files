import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import { App } from "./App";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("App", () => {
  it("shows the loading text, then the unavailable alert when the configuration fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(null, { status: 503 })),
    );
    render(<App />);

    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The registration form is currently unavailable. Please try again later.",
    );
    expect(screen.queryByText("Loading…")).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      /^Conference registration$/,
    );
    expect(screen.queryByRole("button", { name: "Register" })).not.toBeInTheDocument();
  });
});
