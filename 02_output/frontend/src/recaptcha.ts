// Google reCAPTCHA v2 widget, loaded only in "recaptcha" mode (contract recaptcha.md, AR-07).

interface Grecaptcha {
  render(container: HTMLElement, params: { sitekey: string }): number;
  getResponse(widgetId: number): string;
  reset(widgetId: number): void;
}

declare global {
  interface Window {
    grecaptcha?: Grecaptcha;
    onRecaptchaLoad?: () => void;
  }
}

const SCRIPT = "https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit";

let loading: Promise<Grecaptcha> | undefined;

function load(): Promise<Grecaptcha> {
  loading ??= new Promise((resolve) => {
    window.onRecaptchaLoad = () => resolve(window.grecaptcha as Grecaptcha);
    const script = document.createElement("script");
    script.src = SCRIPT;
    script.async = true;
    document.head.appendChild(script);
  });
  return loading;
}

export interface CaptchaWidget {
  token(): string;
  reset(): void;
}

export async function renderRecaptcha(
  container: HTMLElement,
  siteKey: string,
): Promise<CaptchaWidget> {
  const grecaptcha = await load();
  const id = grecaptcha.render(container, { sitekey: siteKey });
  return {
    token: () => grecaptcha.getResponse(id),
    reset: () => grecaptcha.reset(id),
  };
}
