import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { testConfig } from "./testConfig";

describe("App", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-001-08 shows the form once the configuration is loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify(testConfig), { status: 200 })),
    );

    render(<App />);

    expect(await screen.findByRole("radio", { name: "External participant" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Konferenca");
  });

  it("AC-004-04 shows a general error when the configuration cannot be loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("Failed to fetch");
      }),
    );

    render(<App />);

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("The registration form could not be loaded.");
    expect(alert).not.toHaveTextContent("Failed to fetch");
  });
});
