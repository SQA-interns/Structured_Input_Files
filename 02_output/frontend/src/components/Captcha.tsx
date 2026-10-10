import { useEffect, useRef, useState } from "react";

/** Token accepted by the backend's deterministic test mode (local and test only, SR-02). */
export const TEST_MODE_TOKEN = "test-pass";
const SCRIPT_URL = "https://www.google.com/recaptcha/api.js";
const ONLOAD = "konferencaRecaptchaLoaded";

interface Grecaptcha {
  render: (
    container: HTMLElement,
    options: {
      sitekey: string;
      callback: (token: string) => void;
      "expired-callback": () => void;
    },
  ) => number;
  reset: (widgetId?: number) => void;
}

declare global {
  interface Window {
    grecaptcha?: Grecaptcha;
    [ONLOAD]?: () => void;
  }
}

interface Props {
  testMode: boolean;
  siteKey: string;
  /** Changes after every rejected submission; the widget then asks for a new token. */
  resetKey: number;
  error?: string;
  onToken: (token: string | null) => void;
}

/** reCAPTCHA v2 widget, or a test-mode checkbox when the backend runs in test mode (SR-01). */
export default function Captcha({ testMode, siteKey, resetKey, error, onToken }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const widgetId = useRef<number | null>(null);
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    if (testMode) {
      return;
    }
    const render = () => {
      if (container.current && window.grecaptcha && widgetId.current === null) {
        widgetId.current = window.grecaptcha.render(container.current, {
          sitekey: siteKey,
          callback: (token) => onToken(token),
          "expired-callback": () => onToken(null),
        });
      }
    };
    if (window.grecaptcha) {
      render();
      return;
    }
    window[ONLOAD] = render;
    if (!document.querySelector(`script[src^="${SCRIPT_URL}"]`)) {
      const script = document.createElement("script");
      script.src = `${SCRIPT_URL}?onload=${ONLOAD}&render=explicit`;
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);
    }
  }, [testMode, siteKey, onToken]);

  useEffect(() => {
    if (!testMode && resetKey > 0 && window.grecaptcha && widgetId.current !== null) {
      window.grecaptcha.reset(widgetId.current);
      onToken(null);
    }
  }, [resetKey, testMode, onToken]);

  const describedBy = error ? "captchaToken-error" : undefined;
  return (
    <div className="captcha">
      {testMode ? (
        <label>
          <input
            type="checkbox"
            checked={checked}
            aria-describedby={describedBy}
            onChange={(event) => {
              setChecked(event.target.checked);
              onToken(event.target.checked ? TEST_MODE_TOKEN : null);
            }}
          />{" "}
          I am not a robot (test mode)
        </label>
      ) : (
        <div ref={container} data-sitekey={siteKey} aria-describedby={describedBy} />
      )}
      {error && (
        <p id="captchaToken-error" role="alert" className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}
