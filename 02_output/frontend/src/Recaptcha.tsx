import { useEffect, useRef } from "react";

export const TEST_MODE_TOKEN = "test-mode-pass";

interface GoogleRecaptcha {
  render: (container: HTMLElement, options: Record<string, unknown>) => number;
  reset: (widgetId?: number) => void;
}

declare global {
  interface Window {
    grecaptcha?: GoogleRecaptcha;
    onRecaptchaLoad?: () => void;
  }
}

interface Props {
  testMode: boolean;
  siteKey: string;
  token: string;
  onToken: (token: string) => void;
  /** Changes when the widget must be reset (a token is valid for one submission). */
  resetKey: number;
  describedBy?: string;
}

/** Anti-automation control (contract `anti-automation.md`). */
export function Recaptcha({
  testMode,
  siteKey,
  token,
  onToken,
  resetKey,
  describedBy,
}: Props) {
  if (testMode) {
    return (
      <label className="check">
        <input
          type="checkbox"
          checked={token === TEST_MODE_TOKEN}
          aria-describedby={describedBy}
          onChange={(e) => onToken(e.target.checked ? TEST_MODE_TOKEN : "")}
        />
        I am not a robot (test mode)
      </label>
    );
  }
  return (
    <GoogleWidget siteKey={siteKey} onToken={onToken} resetKey={resetKey} />
  );
}

function GoogleWidget({
  siteKey,
  onToken,
  resetKey,
}: Pick<Props, "siteKey" | "onToken" | "resetKey">) {
  const container = useRef<HTMLDivElement>(null);
  const widgetId = useRef<number | undefined>(undefined);
  const callback = useRef(onToken);
  callback.current = onToken;

  useEffect(() => {
    const render = () => {
      if (
        !container.current ||
        !window.grecaptcha ||
        widgetId.current !== undefined
      )
        return;
      widgetId.current = window.grecaptcha.render(container.current, {
        sitekey: siteKey,
        callback: (t: string) => callback.current(t),
        "expired-callback": () => callback.current(""),
        "error-callback": () => callback.current(""),
      });
    };
    if (window.grecaptcha) {
      render();
      return;
    }
    window.onRecaptchaLoad = render;
    if (!document.querySelector("script[data-recaptcha]")) {
      const script = document.createElement("script");
      script.src =
        "https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit";
      script.async = true;
      script.defer = true;
      script.dataset.recaptcha = "true";
      document.head.appendChild(script);
    }
  }, [siteKey]);

  useEffect(() => {
    if (resetKey > 0 && window.grecaptcha && widgetId.current !== undefined) {
      window.grecaptcha.reset(widgetId.current);
      callback.current("");
    }
  }, [resetKey]);

  return <div ref={container} className="recaptcha" />;
}
