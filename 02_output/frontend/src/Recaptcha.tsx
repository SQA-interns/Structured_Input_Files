import { useEffect, useRef } from "react";

/** Token produced by the test-mode checkbox (docs/02_contracts/recaptcha.json). */
export const TEST_MODE_TOKEN = "test-pass";
export const SCRIPT_URL = "https://www.google.com/recaptcha/api.js?render=explicit&onload=";
const ONLOAD = "onRecaptchaLoaded";

interface Grecaptcha {
  render: (
    container: HTMLElement,
    parameters: {
      sitekey: string;
      callback: (token: string) => void;
      "expired-callback": () => void;
    },
  ) => number;
  reset: (widgetId?: number) => void;
}

type RecaptchaWindow = Window & { grecaptcha?: Grecaptcha; [ONLOAD]?: () => void };

interface Props {
  siteKey: string;
  testMode: boolean;
  token: string | null;
  onToken: (token: string | null) => void;
  /** Changes whenever the widget must be reset (a token is valid for one submission). */
  resetKey: number;
  error?: string;
}

/** The reCAPTCHA v2 widget, or a labelled checkbox in test mode (SR-01, SR-02). */
export function Recaptcha({ siteKey, testMode, token, onToken, resetKey, error }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const widget = useRef<number | null>(null);
  const describedBy = error ? "recaptchaToken-error" : undefined;

  useEffect(() => {
    if (testMode) {
      return;
    }
    const win = window as RecaptchaWindow;
    const render = () => {
      if (win.grecaptcha && container.current && widget.current === null) {
        widget.current = win.grecaptcha.render(container.current, {
          sitekey: siteKey,
          callback: (value) => onToken(value),
          "expired-callback": () => onToken(null),
        });
      }
    };
    if (win.grecaptcha) {
      render();
      return;
    }
    win[ONLOAD] = render;
    if (!document.querySelector(`script[src^="${SCRIPT_URL}"]`)) {
      const script = document.createElement("script");
      script.src = SCRIPT_URL + ONLOAD;
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);
    }
  }, [siteKey, testMode, onToken]);

  useEffect(() => {
    const win = window as RecaptchaWindow;
    if (!testMode && resetKey > 0 && win.grecaptcha && widget.current !== null) {
      win.grecaptcha.reset(widget.current);
      onToken(null);
    }
  }, [resetKey, testMode, onToken]);

  return (
    <div className="recaptcha">
      {testMode ? (
        <label>
          <input
            type="checkbox"
            checked={token === TEST_MODE_TOKEN}
            onChange={(event) => onToken(event.target.checked ? TEST_MODE_TOKEN : null)}
            aria-describedby={describedBy}
          />
          I am not a robot (test mode)
        </label>
      ) : (
        <div ref={container} aria-describedby={describedBy} />
      )}
      {error && (
        <p id="recaptchaToken-error" role="alert" className="error">
          {error}
        </p>
      )}
    </div>
  );
}
