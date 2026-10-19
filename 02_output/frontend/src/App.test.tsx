import { fireEvent, render, screen } from "@testing-library/react";
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

describe("App confirmation", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-004-01 replaces the form with the confirmation once accepted", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string) =>
        url === "/api/form-config"
          ? new Response(JSON.stringify(testConfig), { status: 200 })
          : new Response(
              JSON.stringify({
                id: "id-1",
                type: "EXTERNAL",
                firstName: "Ana",
                lastName: "Novak",
                options: [],
                registeredAt: "2026-10-19T12:00:00.000Z",
              }),
              { status: 201 },
            ),
      ),
    );
    render(<App />);
    fireEvent.click(await screen.findByRole("radio", { name: "External participant" }));
    for (const [name, value] of [
      ["First name", "Ana"],
      ["Last name", "Novak"],
      ["Email", "ana@example.com"],
      ["Organization / institution", "IJS"],
    ]) {
      fireEvent.change(screen.getByRole("textbox", { name }), { target: { value } });
    }
    fireEvent.click(screen.getByRole("checkbox", { name: "I agree to the processing." }));
    fireEvent.click(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Ana Novak");
    expect(screen.queryByRole("button", { name: "Register" })).not.toBeInTheDocument();
  });
});
