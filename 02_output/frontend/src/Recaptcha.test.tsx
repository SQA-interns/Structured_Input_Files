import { act, fireEvent, render, screen } from "@testing-library/react";
import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
  type Mock,
} from "vitest";
import { Recaptcha, TEST_MODE_TOKEN } from "./Recaptcha";

describe("test-mode stand-in", () => {
  it("emits the test token when checked and clears it when unchecked", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Recaptcha
        testMode
        siteKey=""
        token=""
        resetCount={0}
        invalid={false}
        onToken={onToken}
      />,
    );
    const box = screen.getByRole("checkbox", {
      name: "I am not a robot (test mode)",
    });
    expect(box).not.toBeChecked();
    expect(box).not.toHaveAttribute("aria-invalid");
    expect(box).not.toHaveAttribute("aria-describedby");

    fireEvent.click(box);
    expect(onToken).toHaveBeenLastCalledWith(TEST_MODE_TOKEN);

    rerender(
      <Recaptcha
        testMode
        siteKey=""
        token={TEST_MODE_TOKEN}
        resetCount={0}
        invalid
        onToken={onToken}
      />,
    );
    expect(box).toBeChecked();
    expect(box).toHaveAttribute("aria-invalid", "true");
    expect(box).toHaveAttribute("aria-describedby", "recaptchaToken-error");
    fireEvent.click(box);
    expect(onToken).toHaveBeenLastCalledWith("");
    expect(TEST_MODE_TOKEN).toBe("test-mode-token");
  });
});

describe("Google widget", () => {
  type RenderOptions = {
    sitekey: string;
    callback: (token: string) => void;
    "expired-callback": () => void;
  };
  let renderWidget: Mock<
    (element: HTMLElement, options: RenderOptions) => number
  >;
  let reset: Mock<(widget: number) => void>;

  beforeEach(() => {
    renderWidget = vi.fn<
      (element: HTMLElement, options: RenderOptions) => number
    >(() => 7);
    reset = vi.fn<(widget: number) => void>();
  });

  afterEach(() => {
    delete window.grecaptcha;
    vi.useRealTimers();
  });

  const install = () => {
    window.grecaptcha = {
      ready: (callback) => callback(),
      render: renderWidget,
      reset,
    };
  };

  it("loads the script once, renders with the site key and forwards tokens", async () => {
    install();
    const onToken = vi.fn();
    const { rerender, container } = render(
      <Recaptcha
        testMode={false}
        siteKey="site-1"
        token=""
        resetCount={0}
        invalid
        onToken={onToken}
      />,
    );
    await act(async () => {});

    const scripts = document.head.querySelectorAll(
      'script[src="https://www.google.com/recaptcha/api.js?render=explicit"]',
    );
    expect(scripts).toHaveLength(1);
    expect((scripts[0] as HTMLScriptElement).async).toBe(true);
    expect((scripts[0] as HTMLScriptElement).defer).toBe(true);
    expect(renderWidget).toHaveBeenCalledTimes(1);
    const [element, options] = renderWidget.mock.calls[0];
    expect(element).toBe(container.firstChild);
    expect(element).toHaveAttribute("aria-describedby", "recaptchaToken-error");
    expect(options.sitekey).toBe("site-1");
    options.callback("google-token");
    expect(onToken).toHaveBeenLastCalledWith("google-token");
    options["expired-callback"]();
    expect(onToken).toHaveBeenLastCalledWith("");
    expect(reset).not.toHaveBeenCalled();

    rerender(
      <Recaptcha
        testMode={false}
        siteKey="site-1"
        token=""
        resetCount={1}
        invalid={false}
        onToken={onToken}
      />,
    );
    expect(reset).toHaveBeenCalledWith(7);
    expect(container.firstChild).not.toHaveAttribute("aria-describedby");

    render(
      <Recaptcha
        testMode={false}
        siteKey="site-2"
        token=""
        resetCount={0}
        invalid={false}
        onToken={onToken}
      />,
    );
    await act(async () => {});
    expect(
      document.head.querySelectorAll('script[src*="recaptcha/api.js"]'),
    ).toHaveLength(1);
  });

  it("waits for the script before rendering", async () => {
    vi.useFakeTimers();
    render(
      <Recaptcha
        testMode={false}
        siteKey="k"
        token=""
        resetCount={0}
        invalid={false}
        onToken={vi.fn()}
      />,
    );
    await act(async () => {
      await vi.advanceTimersByTimeAsync(250);
    });
    expect(renderWidget).not.toHaveBeenCalled();

    install();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(100);
    });
    expect(renderWidget).toHaveBeenCalledTimes(1);
  });

  it("does not render after unmount and does not reset before rendering", async () => {
    vi.useFakeTimers();
    const { unmount, rerender } = render(
      <Recaptcha
        testMode={false}
        siteKey="k"
        token=""
        resetCount={0}
        invalid={false}
        onToken={vi.fn()}
      />,
    );
    rerender(
      <Recaptcha
        testMode={false}
        siteKey="k"
        token=""
        resetCount={1}
        invalid={false}
        onToken={vi.fn()}
      />,
    );
    unmount();
    install();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(200);
    });
    expect(renderWidget).not.toHaveBeenCalled();
    expect(reset).not.toHaveBeenCalled();
  });
});
