import { useEffect, useRef } from "react";

export const TEST_TOKEN = "test-pass";
const SCRIPT_URL = "https://www.google.com/recaptcha/api.js?render=explicit";

interface Grecaptcha {
  ready(callback: () => void): void;
  render(
    container: HTMLElement,
    parameters: {
      sitekey: string;
      callback: (token: string) => void;
      "expired-callback": () => void;
    },
  ): number;
  reset(widgetId?: number): void;
}

declare global {
  interface Window {
    grecaptcha?: Grecaptcha;
  }
}

interface CaptchaProps {
  mode: "test" | "recaptcha";
  siteKey: string | null;
  token: string | null;
  onToken: (token: string | null) => void;
  /** Incremented after a rejected submission to reset the widget. */
  resetCount: number;
  describedBy?: string;
}

function loadScript(): Promise<Grecaptcha> {
  return new Promise((resolve, reject) => {
    if (window.grecaptcha) {
      window.grecaptcha.ready(() => resolve(window.grecaptcha as Grecaptcha));
      return;
    }
    const script = document.createElement("script");
    script.src = SCRIPT_URL;
    script.async = true;
    script.onload = () => {
      const api = window.grecaptcha;
      if (api) {
        api.ready(() => resolve(api));
      } else {
        reject(new Error("reCAPTCHA did not load"));
      }
    };
    script.onerror = () => reject(new Error("reCAPTCHA did not load"));
    document.head.appendChild(script);
  });
}

/** Anti-automation control: Google reCAPTCHA v2, or a checkbox in test mode (SR-01, SR-02). */
export function Captcha(props: CaptchaProps) {
  const { mode, siteKey, token, onToken, resetCount, describedBy } = props;
  const container = useRef<HTMLDivElement>(null);
  const widget = useRef<{ api: Grecaptcha; id: number } | null>(null);
  const tokenHandler = useRef(onToken);
  tokenHandler.current = onToken;

  useEffect(() => {
    if (mode !== "recaptcha" || !siteKey || !container.current) return;
    const element = container.current;
    let cancelled = false;
    loadScript()
      .then((api) => {
        if (cancelled || element.childElementCount > 0) return;
        const id = api.render(element, {
          sitekey: siteKey,
          callback: (value) => tokenHandler.current(value),
          "expired-callback": () => tokenHandler.current(null),
        });
        widget.current = { api, id };
      })
      .catch(() => tokenHandler.current(null));
    return () => {
      cancelled = true;
    };
  }, [mode, siteKey]);

  useEffect(() => {
    if (resetCount > 0 && widget.current) {
      widget.current.api.reset(widget.current.id);
      tokenHandler.current(null);
    }
  }, [resetCount]);

  if (mode === "test") {
    return (
      <label className="checkbox">
        <input
          type="checkbox"
          checked={token === TEST_TOKEN}
          aria-invalid={describedBy ? true : undefined}
          aria-describedby={describedBy}
          onChange={(event) =>
            onToken(event.target.checked ? TEST_TOKEN : null)
          }
        />
        I am not a robot (test mode)
      </label>
    );
  }
  return (
    <div
      ref={container}
      data-testid="recaptcha"
      aria-describedby={describedBy}
    />
  );
}
