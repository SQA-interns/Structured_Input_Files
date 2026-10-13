import { useEffect, useRef } from "react";

// reCAPTCHA v2 (SR-01): the Google widget, or the test-mode stand-in (spec section 9).

export const TEST_MODE_TOKEN = "test-mode-token";
const SCRIPT_URL = "https://www.google.com/recaptcha/api.js?render=explicit";

interface Grecaptcha {
  ready(callback: () => void): void;
  render(
    element: HTMLElement,
    options: {
      sitekey: string;
      callback: (token: string) => void;
      "expired-callback": () => void;
    },
  ): number;
  reset(widget: number): void;
}

declare global {
  interface Window {
    grecaptcha?: Grecaptcha;
  }
}

let scriptRequested = false;

function loadGrecaptcha(): Promise<Grecaptcha> {
  if (!scriptRequested) {
    scriptRequested = true;
    const script = document.createElement("script");
    script.src = SCRIPT_URL;
    script.async = true;
    script.defer = true;
    document.head.appendChild(script);
  }
  return new Promise((resolve) => {
    const poll = () => {
      const api = window.grecaptcha;
      if (api) {
        api.ready(() => resolve(api));
      } else {
        setTimeout(poll, 100);
      }
    };
    poll();
  });
}

interface Props {
  testMode: boolean;
  siteKey: string;
  token: string;
  /** Changes after every failed submission, which resets the widget. */
  resetCount: number;
  invalid: boolean;
  onToken: (token: string) => void;
}

export function Recaptcha({
  testMode,
  siteKey,
  token,
  resetCount,
  invalid,
  onToken,
}: Props) {
  const describedBy = invalid ? "recaptchaToken-error" : undefined;
  if (testMode) {
    return (
      <div className="field">
        <input
          id="recaptcha-test"
          type="checkbox"
          checked={token === TEST_MODE_TOKEN}
          aria-invalid={invalid || undefined}
          aria-describedby={describedBy}
          onChange={(e) => onToken(e.target.checked ? TEST_MODE_TOKEN : "")}
        />
        <label htmlFor="recaptcha-test">I am not a robot (test mode)</label>
      </div>
    );
  }
  return (
    <GoogleWidget
      siteKey={siteKey}
      resetCount={resetCount}
      describedBy={describedBy}
      onToken={onToken}
    />
  );
}

function GoogleWidget({
  siteKey,
  resetCount,
  describedBy,
  onToken,
}: {
  siteKey: string;
  resetCount: number;
  describedBy?: string;
  onToken: (token: string) => void;
}) {
  const container = useRef<HTMLDivElement>(null);
  const widget = useRef<number | null>(null);
  const callback = useRef(onToken);
  callback.current = onToken;

  useEffect(() => {
    let cancelled = false;
    void loadGrecaptcha().then((api) => {
      if (!cancelled && container.current && widget.current === null) {
        widget.current = api.render(container.current, {
          sitekey: siteKey,
          callback: (token) => callback.current(token),
          "expired-callback": () => callback.current(""),
        });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [siteKey]);

  useEffect(() => {
    if (resetCount > 0 && widget.current !== null && window.grecaptcha) {
      window.grecaptcha.reset(widget.current);
    }
  }, [resetCount]);

  return (
    <div className="field" ref={container} aria-describedby={describedBy} />
  );
}
