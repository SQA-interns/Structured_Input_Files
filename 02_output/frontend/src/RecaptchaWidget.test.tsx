import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "./RegistrationForm";
import { testConfig } from "./testConfig";

type RenderOptions = { sitekey: string; callback: (token: string) => void };

const productionConfig = { ...testConfig, recaptcha: { siteKey: "site-key-1", testMode: false } };

function stubGrecaptcha() {
  const grecaptcha = {
    render: vi.fn((_element: HTMLElement, _options: RenderOptions) => 7),
    reset: vi.fn(),
  };
  vi.stubGlobal("grecaptcha", grecaptcha);
  return grecaptcha;
}

function fillExternal() {
  fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
  for (const [name, value] of [
    ["First name", "Ana"],
    ["Last name", "Novak"],
    ["Email", "ana@example.com"],
    ["Organization / institution", "IJS"],
  ]) {
    fireEvent.change(screen.getByRole("textbox", { name }), { target: { value } });
  }
  fireEvent.click(screen.getByRole("checkbox", { name: "I agree to the processing." }));
}

describe("reCAPTCHA widget (SR-01, AR-07)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.head.querySelectorAll("script[data-recaptcha]").forEach((s) => s.remove());
  });

  it("SR-01 renders the Google widget with the configured site key and sends its token", async () => {
    const grecaptcha = stubGrecaptcha();
    const fetchMock = vi.fn(async () => new Response("{}", { status: 500 }));
    vi.stubGlobal("fetch", fetchMock);
    render(<RegistrationForm config={productionConfig} onAccepted={vi.fn()} />);
    fillExternal();

    await waitFor(() => expect(grecaptcha.render).toHaveBeenCalledTimes(1));
    expect(grecaptcha.render.mock.calls[0][1].sitekey).toBe("site-key-1");
    expect(screen.queryByRole("checkbox", { name: "I am not a robot (test mode)" })).toBeNull();
    act(() => grecaptcha.render.mock.calls[0][1].callback("google-token"));
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    const init = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(JSON.parse(init[1].body as string).recaptchaToken).toBe("google-token");
    await waitFor(() => expect(grecaptcha.reset).toHaveBeenCalledWith(7));
  });

  it("SR-01 loads the reCAPTCHA script once when it is not present", () => {
    render(<RegistrationForm config={productionConfig} onAccepted={vi.fn()} />);
    fillExternal();

    const scripts = document.head.querySelectorAll("script[data-recaptcha]");
    expect(scripts).toHaveLength(1);
    expect(scripts[0].getAttribute("src")).toBe(
      "https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit",
    );
  });
});
