import { useEffect, useRef } from "react";

interface Grecaptcha {
  render(
    element: HTMLElement,
    options: { sitekey: string; callback: (token: string) => void; "expired-callback": () => void },
  ): number;
  reset(widgetId?: number): void;
}

declare global {
  interface Window {
    grecaptcha?: Grecaptcha;
    onRecaptchaLoad?: () => void;
  }
}

const SCRIPT = "https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit";

/** Google reCAPTCHA v2 widget with the configured site key (SR-01, AR-07). */
export function RecaptchaWidget({
  siteKey,
  onToken,
  resetSignal,
}: {
  siteKey: string;
  onToken: (token: string) => void;
  resetSignal: number;
}) {
  const container = useRef<HTMLDivElement>(null);
  const widgetId = useRef<number | null>(null);

  useEffect(() => {
    function renderWidget() {
      if (!container.current || widgetId.current !== null || !window.grecaptcha) {
        return;
      }
      widgetId.current = window.grecaptcha.render(container.current, {
        sitekey: siteKey,
        callback: onToken,
        "expired-callback": () => onToken(""),
      });
    }
    if (window.grecaptcha) {
      renderWidget();
      return;
    }
    window.onRecaptchaLoad = renderWidget;
    if (!document.querySelector("script[data-recaptcha]")) {
      const script = document.createElement("script");
      script.src = SCRIPT;
      script.async = true;
      script.defer = true;
      script.dataset.recaptcha = "true";
      document.head.appendChild(script);
    }
  }, [siteKey, onToken]);

  useEffect(() => {
    if (resetSignal > 0 && widgetId.current !== null) {
      window.grecaptcha?.reset(widgetId.current);
    }
  }, [resetSignal]);

  return <div ref={container} />;
}
