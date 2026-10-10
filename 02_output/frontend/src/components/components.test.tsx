import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "../App";
import Captcha, { TEST_MODE_TOKEN } from "./Captcha";
import Confirmation from "./Confirmation";
import RegistrationForm from "./RegistrationForm";
import type { FormConfig } from "../api";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  delete window.grecaptcha;
  delete window.konferencaRecaptchaLoaded;
  document.head.querySelectorAll("script").forEach((s) => s.remove());
});

const config: FormConfig = {
  conferenceName: "Konf",
  options: [{ id: "o", name: "Option", category: "OTHER", audience: ["EXTERNAL"] }],
  consents: [{ id: "data", text: "Data", mandatory: true }],
  recaptcha: { siteKey: "", testMode: true },
};

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status });
}

describe("Captcha", () => {
  it("emits the test token when ticked and null when unticked", () => {
    const onToken = vi.fn();
    render(<Captcha testMode siteKey="" resetKey={0} onToken={onToken} />);
    const box = screen.getByRole("checkbox", { name: "I am not a robot (test mode)" });
    fireEvent.click(box);
    fireEvent.click(box);
    expect(onToken.mock.calls).toEqual([[TEST_MODE_TOKEN], [null]]);
    expect(document.querySelector("script")).toBeNull();
  });

  it("renders the Google widget once loaded, resets it and reports expiry", () => {
    const render$ = vi.fn(() => 7);
    const reset = vi.fn();
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha testMode={false} siteKey="key" resetKey={0} onToken={onToken} />,
    );
    const script = document.head.querySelector("script");
    expect(script?.src).toBe(
      "https://www.google.com/recaptcha/api.js?onload=konferencaRecaptchaLoaded&render=explicit",
    );
    window.grecaptcha = { render: render$, reset };
    act(() => window.konferencaRecaptchaLoaded?.());
    const options = (render$.mock.calls[0] as unknown as [HTMLElement, Record<string, unknown>])[1];
    expect(options.sitekey).toBe("key");
    (options.callback as (t: string) => void)("tok");
    (options["expired-callback"] as () => void)();
    rerender(<Captcha testMode={false} siteKey="key" resetKey={1} onToken={onToken} />);
    expect(reset).toHaveBeenCalledWith(7);
    expect(onToken.mock.calls).toEqual([["tok"], [null], [null]]);
  });

  it("renders immediately when the library is already present and adds no second script", () => {
    const render$ = vi.fn(() => 1);
    window.grecaptcha = { render: render$, reset: vi.fn() };
    render(<Captcha testMode={false} siteKey="k" resetKey={0} error="E" onToken={vi.fn()} />);
    expect(render$).toHaveBeenCalledTimes(1);
    expect(document.head.querySelector("script")).toBeNull();
    expect(screen.getByRole("alert")).toHaveTextContent("E");
  });
});

describe("Confirmation", () => {
  it("says when no activity was selected", () => {
    render(
      <Confirmation
        registration={{
          reference: "r",
          submittedAt: "",
          type: "STUDENT",
          firstName: "L",
          lastName: "K",
          options: [],
        }}
      />,
    );
    expect(screen.getByRole("status")).toHaveTextContent("No activities selected");
  });
});

describe("RegistrationForm", () => {
  function fillValid() {
    fireEvent.change(screen.getByLabelText("First name"), { target: { value: "A" } });
    fireEvent.change(screen.getByLabelText("Last name"), { target: { value: "B" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "a@example.si" } });
    fireEvent.change(screen.getByLabelText("Organization / institution"), {
      target: { value: "O" },
    });
    fireEvent.click(screen.getByRole("checkbox", { name: "Data" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }));
  }

  it("shows a general error for field errors it cannot place", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        json(400, {
          fieldErrors: [{ field: "consentIds", code: "UNKNOWN_CONSENT", message: "m" }],
        }),
      ),
    );
    render(<RegistrationForm config={config} type="EXTERNAL" onAccepted={vi.fn()} />);
    fillValid();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await waitFor(() =>
      expect(document.getElementById("form-error")).toHaveTextContent(
        "Your registration could not be processed.",
      ),
    );
  });

  it("explains rate limiting and allows deselecting options", async () => {
    const fetchMock = vi.fn(async () => json(429, {}));
    vi.stubGlobal("fetch", fetchMock);
    render(<RegistrationForm config={config} type="EXTERNAL" onAccepted={vi.fn()} />);
    fillValid();
    const option = screen.getByRole("checkbox", { name: "Option" });
    fireEvent.click(option);
    fireEvent.click(option);
    expect(option).not.toBeChecked();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await waitFor(() =>
      expect(document.getElementById("form-error")).toHaveTextContent("Too many attempts."),
    );
    const sent = JSON.parse(
      (fetchMock.mock.calls[0] as unknown as [string, RequestInit])[1].body as string,
    );
    expect(sent.optionIds).toEqual([]);
    expect(sent).not.toHaveProperty("studentId");
  });

  it("marks invalid inputs", () => {
    render(<RegistrationForm config={config} type="EXTERNAL" onAccepted={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(screen.getByLabelText("First name")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByRole("checkbox", { name: "Data" })).toHaveAttribute(
      "aria-describedby",
      "consents.data-error",
    );
  });
});

describe("App", () => {
  it("shows a load error when the configuration is unavailable", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => json(500, {})),
    );
    render(<App />);
    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The registration form could not be loaded.",
    );
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Conference registration");
  });

  it("titles the page with the conference name", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => json(200, config)),
    );
    render(<App />);
    expect(await screen.findByRole("heading", { level: 1 })).toHaveTextContent(
      "Konf – registration",
    );
  });
});
