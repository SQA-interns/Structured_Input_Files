import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Captcha, TEST_TOKEN } from "./Captcha";

type RenderParams = {
  sitekey: string;
  callback: (token: string) => void;
  "expired-callback": () => void;
};

function fakeGrecaptcha() {
  const api = {
    params: null as RenderParams | null,
    ready: vi.fn((callback: () => void) => callback()),
    render: vi.fn((container: HTMLElement, params: RenderParams) => {
      api.params = params;
      container.appendChild(document.createElement("iframe"));
      return 7;
    }),
    reset: vi.fn(),
  };
  return api;
}

afterEach(() => {
  delete window.grecaptcha;
  document.head.querySelectorAll("script").forEach((s) => s.remove());
});

describe("Captcha in test mode", () => {
  it("gives the pass token while checked", () => {
    const onToken = vi.fn();
    const { rerender } = render(
      <Captcha
        mode="test"
        siteKey={null}
        token={null}
        onToken={onToken}
        resetCount={0}
      />,
    );
    const box = screen.getByRole("checkbox", {
      name: "I am not a robot (test mode)",
    });
    expect(box).not.toBeChecked();
    expect(box).not.toHaveAttribute("aria-invalid");

    fireEvent.click(box);
    expect(onToken).toHaveBeenLastCalledWith(TEST_TOKEN);

    rerender(
      <Captcha
        mode="test"
        siteKey={null}
        token={TEST_TOKEN}
        onToken={onToken}
        resetCount={0}
        describedBy="error-recaptchaToken"
      />,
    );
    expect(box).toBeChecked();
    expect(box).toHaveAttribute("aria-invalid", "true");
    fireEvent.click(box);
    expect(onToken).toHaveBeenLastCalledWith(null);
  });
});

describe("Captcha with Google reCAPTCHA", () => {
  it("renders the widget with the site key and forwards tokens", async () => {
    const api = fakeGrecaptcha();
    window.grecaptcha = api;
    const onToken = vi.fn();

    const { rerender } = render(
      <Captcha
        mode="recaptcha"
        siteKey="site-key"
        token={null}
        onToken={onToken}
        resetCount={0}
      />,
    );
    await act(async () => {});

    expect(screen.getByTestId("recaptcha")).toBeInTheDocument();
    expect(api.render).toHaveBeenCalledTimes(1);
    expect(api.params?.sitekey).toBe("site-key");
    api.params?.callback("google-token");
    expect(onToken).toHaveBeenLastCalledWith("google-token");
    api.params?.["expired-callback"]();
    expect(onToken).toHaveBeenLastCalledWith(null);

    rerender(
      <Captcha
        mode="recaptcha"
        siteKey="site-key"
        token={null}
        onToken={onToken}
        resetCount={1}
      />,
    );
    expect(api.reset).toHaveBeenCalledWith(7);
  });

  it("loads the script when reCAPTCHA is not present yet", async () => {
    const api = fakeGrecaptcha();
    render(
      <Captcha
        mode="recaptcha"
        siteKey="k"
        token={null}
        onToken={vi.fn()}
        resetCount={0}
      />,
    );

    const script = document.head.querySelector("script");
    expect(script?.src).toBe(
      "https://www.google.com/recaptcha/api.js?render=explicit",
    );
    window.grecaptcha = api;
    await act(async () => {
      script?.dispatchEvent(new Event("load"));
    });
    expect(api.render).toHaveBeenCalledTimes(1);
  });

  it("clears the token when the script cannot load", async () => {
    const onToken = vi.fn();
    render(
      <Captcha
        mode="recaptcha"
        siteKey="k"
        token="old"
        onToken={onToken}
        resetCount={0}
      />,
    );
    const script = document.head.querySelector("script");
    await act(async () => {
      script?.dispatchEvent(new Event("error"));
    });
    expect(onToken).toHaveBeenCalledWith(null);
  });

  it("clears the token when the script loads without the API", async () => {
    const onToken = vi.fn();
    render(
      <Captcha
        mode="recaptcha"
        siteKey="k"
        token="old"
        onToken={onToken}
        resetCount={0}
      />,
    );
    await act(async () => {
      document.head.querySelector("script")?.dispatchEvent(new Event("load"));
    });
    expect(onToken).toHaveBeenCalledWith(null);
  });

  it("does nothing without a site key and does not reset before a widget exists", async () => {
    const api = fakeGrecaptcha();
    window.grecaptcha = api;
    const { rerender } = render(
      <Captcha
        mode="recaptcha"
        siteKey={null}
        token={null}
        onToken={vi.fn()}
        resetCount={0}
      />,
    );
    rerender(
      <Captcha
        mode="recaptcha"
        siteKey={null}
        token={null}
        onToken={vi.fn()}
        resetCount={1}
      />,
    );
    await act(async () => {});
    expect(api.render).not.toHaveBeenCalled();
    expect(api.reset).not.toHaveBeenCalled();
  });

  it("does not render after unmounting", async () => {
    const api = fakeGrecaptcha();
    render(
      <Captcha
        mode="recaptcha"
        siteKey="k"
        token={null}
        onToken={vi.fn()}
        resetCount={0}
      />,
    ).unmount();
    window.grecaptcha = api;
    await act(async () => {
      document.head.querySelector("script")?.dispatchEvent(new Event("load"));
    });
    expect(api.render).not.toHaveBeenCalled();
  });
});
