import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Recaptcha, TEST_MODE_TOKEN } from "./Recaptcha";

afterEach(() => {
  delete window.grecaptcha;
  delete window.onRecaptchaLoad;
  document
    .querySelectorAll("script[data-recaptcha]")
    .forEach((s) => s.remove());
});

describe("Recaptcha", () => {
  it("in test mode toggles the test token and loads no script", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Recaptcha testMode siteKey="" token="" onToken={onToken} resetKey={0} />,
    );

    fireEvent.click(
      screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }),
    );
    expect(onToken).toHaveBeenLastCalledWith(TEST_MODE_TOKEN);
    rerender(
      <Recaptcha
        testMode
        siteKey=""
        token={TEST_MODE_TOKEN}
        onToken={onToken}
        resetKey={0}
      />,
    );
    fireEvent.click(screen.getByRole("checkbox"));
    expect(onToken).toHaveBeenLastCalledWith("");
    expect(document.querySelector("script[data-recaptcha]")).toBeNull();
  });

  it("in live mode loads Google's script once and renders the widget with the site key on load", () => {
    const render$ = vi.fn(() => 7);
    const onToken = vi.fn();
    render(
      <Recaptcha
        testMode={false}
        siteKey="site-key"
        token=""
        onToken={onToken}
        resetKey={0}
      />,
    );

    const script = document.querySelector<HTMLScriptElement>(
      "script[data-recaptcha]",
    );
    expect(script?.src).toBe(
      "https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit",
    );
    window.grecaptcha = { render: render$, reset: vi.fn() };
    act(() => window.onRecaptchaLoad?.());

    expect(render$).toHaveBeenCalledTimes(1);
    const options = (
      render$.mock.calls[0] as unknown as [
        HTMLElement,
        Record<string, (t?: string) => void>,
      ]
    )[1];
    expect(options.sitekey).toBe("site-key");
    options.callback("google-token");
    expect(onToken).toHaveBeenLastCalledWith("google-token");
    options["expired-callback"]();
    expect(onToken).toHaveBeenLastCalledWith("");
    options["error-callback"]();
    expect(onToken).toHaveBeenLastCalledWith("");
  });

  it("renders at once when the script is already loaded and resets the widget on request", () => {
    const reset = vi.fn();
    window.grecaptcha = { render: vi.fn(() => 3), reset };
    const onToken = vi.fn();
    const { rerender } = render(
      <Recaptcha
        testMode={false}
        siteKey="k"
        token=""
        onToken={onToken}
        resetKey={0}
      />,
    );
    expect(window.grecaptcha.render).toHaveBeenCalledTimes(1);

    rerender(
      <Recaptcha
        testMode={false}
        siteKey="k"
        token="x"
        onToken={onToken}
        resetKey={1}
      />,
    );

    expect(reset).toHaveBeenCalledWith(3);
    expect(onToken).toHaveBeenLastCalledWith("");
  });
});
