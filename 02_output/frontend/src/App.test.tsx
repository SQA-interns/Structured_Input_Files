import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { App } from "./App";

describe("App", () => {
  it("shows a loading state, then an error when the settings cannot be loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("{}", { status: 503 })),
    );

    render(<App />);

    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The registration form could not be loaded.",
    );
  });

  it("shows the conference name as the page heading", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string) =>
        url === "/api/settings"
          ? new Response(
              JSON.stringify({
                conferenceName: "Konferenca 2026",
                consent: { id: "c", text: "I agree." },
                recaptcha: { testMode: true, siteKey: "" },
              }),
            )
          : new Response("[]"),
      ),
    );

    render(<App />);

    expect(await screen.findByRole("heading", { level: 1 })).toHaveTextContent(
      "Konferenca 2026 – Registration",
    );
  });
});
