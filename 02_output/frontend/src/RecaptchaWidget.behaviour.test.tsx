import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { StrictMode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RecaptchaWidget } from "./RecaptchaWidget";
import { RegistrationForm } from "./RegistrationForm";
import { testConfig } from "./testConfig";

type Options = { sitekey: string; callback: (t: string) => void; "expired-callback": () => void };

const productionConfig = { ...testConfig, recaptcha: { siteKey: "site-key-1", testMode: false } };

function stubGrecaptcha() {
  const grecaptcha = {
    render: vi.fn((_element: HTMLElement, _options: Options) => 3),
    reset: vi.fn(),
  };
  vi.stubGlobal("grecaptcha", grecaptcha);
  return grecaptcha;
}

describe("RecaptchaWidget behaviour (phase 5 survivors)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    delete window.onRecaptchaLoad;
    document.head.querySelectorAll("script[data-recaptcha]").forEach((s) => s.remove());
  });

  it("SR-01 the widget is rendered once, also when effects run twice", () => {
    const grecaptcha = stubGrecaptcha();

    render(
      <StrictMode>
        <RecaptchaWidget siteKey="k" onToken={vi.fn()} resetSignal={0} />
      </StrictMode>,
    );

    expect(grecaptcha.render).toHaveBeenCalledTimes(1);
    expect(grecaptcha.reset).not.toHaveBeenCalled();
  });

  it("SR-01 the script is added once, async and deferred, and renders when loaded", () => {
    render(
      <StrictMode>
        <RecaptchaWidget siteKey="k" onToken={vi.fn()} resetSignal={0} />
      </StrictMode>,
    );

    const scripts = document.head.querySelectorAll("script[data-recaptcha]");
    expect(scripts).toHaveLength(1);
    const script = scripts[0] as HTMLScriptElement;
    expect(script.async).toBe(true);
    expect(script.defer).toBe(true);
    expect(script.getAttribute("data-recaptcha")).toBe("true");

    const grecaptcha = stubGrecaptcha();
    act(() => window.onRecaptchaLoad?.());
    expect(grecaptcha.render).toHaveBeenCalledTimes(1);
    expect(grecaptcha.render.mock.calls[0][1].sitekey).toBe("k");
  });

  it("SR-01 the widget is reset only when the reset signal increases", () => {
    const grecaptcha = stubGrecaptcha();
    const { rerender } = render(<RecaptchaWidget siteKey="k" onToken={vi.fn()} resetSignal={0} />);
    expect(grecaptcha.reset).not.toHaveBeenCalled();

    rerender(<RecaptchaWidget siteKey="k" onToken={vi.fn()} resetSignal={1} />);

    expect(grecaptcha.reset).toHaveBeenCalledTimes(1);
    expect(grecaptcha.reset).toHaveBeenCalledWith(3);
  });

  it("SR-01 an expired token is not sent and a failed attempt needs a new token", async () => {
    const grecaptcha = stubGrecaptcha();
    const fetchMock = vi.fn(async () => new Response("{}", { status: 500 }));
    vi.stubGlobal("fetch", fetchMock);
    render(<RegistrationForm config={productionConfig} onAccepted={vi.fn()} />);
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
    const options = grecaptcha.render.mock.calls[0][1];
    act(() => options.callback("token-1"));
    act(() => options["expired-callback"]());

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const first = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(JSON.parse(first[1].body as string).recaptchaToken).toBe("");

    await screen.findByRole("alert");
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    const second = fetchMock.mock.calls[1] as unknown as [string, RequestInit];
    expect(JSON.parse(second[1].body as string).recaptchaToken).toBe("");
  });
});
