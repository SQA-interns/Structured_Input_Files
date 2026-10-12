import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Recaptcha, SCRIPT_URL, TEST_MODE_TOKEN } from "./Recaptcha";

type Win = Window & { grecaptcha?: unknown; onRecaptchaLoaded?: () => void };

afterEach(() => {
  cleanup();
  delete (window as Win).grecaptcha;
  delete (window as Win).onRecaptchaLoaded;
  document.head.querySelectorAll("script").forEach((s) => s.remove());
});

function fakeGrecaptcha() {
  let callbacks: { callback: (t: string) => void; "expired-callback": () => void } | undefined;
  const api = {
    render: vi.fn((_container: HTMLElement, parameters: typeof callbacks & { sitekey: string }) => {
      callbacks = parameters;
      return 7;
    }),
    reset: vi.fn(),
    solve: (token: string) => callbacks?.callback(token),
    expire: () => callbacks?.["expired-callback"](),
  };
  return api;
}

describe("Recaptcha in test mode", () => {
  it("toggles the test token with a labelled checkbox", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Recaptcha siteKey="" testMode token={null} onToken={onToken} resetKey={0} />,
    );

    fireEvent.click(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }));
    expect(onToken).toHaveBeenLastCalledWith(TEST_MODE_TOKEN);

    rerender(
      <Recaptcha siteKey="" testMode token={TEST_MODE_TOKEN} onToken={onToken} resetKey={1} />,
    );
    const box = screen.getByRole("checkbox");
    expect(box).toBeChecked();
    fireEvent.click(box);
    expect(onToken).toHaveBeenLastCalledWith(null);
    expect(document.head.querySelector("script")).toBeNull();
  });

  it("shows its error next to the checkbox", () => {
    render(
      <Recaptcha siteKey="" testMode token={null} onToken={vi.fn()} resetKey={0} error="Bad" />,
    );

    expect(document.getElementById("recaptchaToken-error")).toHaveTextContent("Bad");
    expect(screen.getByRole("checkbox")).toHaveAttribute(
      "aria-describedby",
      "recaptchaToken-error",
    );
  });
});

describe("Recaptcha in production mode", () => {
  it("renders the widget when the library is present and forwards tokens", () => {
    const api = fakeGrecaptcha();
    (window as Win).grecaptcha = api;
    const onToken = vi.fn();

    render(
      <Recaptcha siteKey="site" testMode={false} token={null} onToken={onToken} resetKey={0} />,
    );

    expect(api.render).toHaveBeenCalledTimes(1);
    expect(api.render.mock.calls[0][1]).toMatchObject({ sitekey: "site" });
    api.solve("google-token");
    expect(onToken).toHaveBeenLastCalledWith("google-token");
    api.expire();
    expect(onToken).toHaveBeenLastCalledWith(null);
    expect(screen.queryByRole("checkbox")).toBeNull();
  });

  it("loads the script once and renders when it has loaded", () => {
    const onToken = vi.fn();
    const first = render(
      <Recaptcha siteKey="site" testMode={false} token={null} onToken={onToken} resetKey={0} />,
    );
    render(
      <Recaptcha siteKey="site" testMode={false} token={null} onToken={onToken} resetKey={0} />,
    );

    const scripts = document.head.querySelectorAll("script");
    expect(scripts).toHaveLength(1);
    expect(scripts[0].src).toBe(SCRIPT_URL + "onRecaptchaLoaded");
    expect(scripts[0].async).toBe(true);
    expect(scripts[0].defer).toBe(true);

    const api = fakeGrecaptcha();
    (window as Win).grecaptcha = api;
    act(() => (window as Win).onRecaptchaLoaded?.());
    expect(api.render).toHaveBeenCalledTimes(1);
    act(() => (window as Win).onRecaptchaLoaded?.());
    expect(api.render).toHaveBeenCalledTimes(1);
    first.unmount();
  });

  it("resets the widget and the token when the reset key changes", () => {
    const api = fakeGrecaptcha();
    (window as Win).grecaptcha = api;
    const onToken = vi.fn();
    const { rerender } = render(
      <Recaptcha siteKey="site" testMode={false} token="t" onToken={onToken} resetKey={0} />,
    );
    expect(api.reset).not.toHaveBeenCalled();

    rerender(
      <Recaptcha siteKey="site" testMode={false} token="t" onToken={onToken} resetKey={1} />,
    );

    expect(api.reset).toHaveBeenCalledWith(7);
    expect(onToken).toHaveBeenLastCalledWith(null);
  });

  it("does not reset before the widget exists", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Recaptcha siteKey="site" testMode={false} token={null} onToken={onToken} resetKey={0} />,
    );
    const api = fakeGrecaptcha();
    (window as Win).grecaptcha = api;

    rerender(
      <Recaptcha siteKey="site" testMode={false} token={null} onToken={onToken} resetKey={1} />,
    );

    expect(api.reset).not.toHaveBeenCalled();
    expect(onToken).not.toHaveBeenCalled();
  });
});
