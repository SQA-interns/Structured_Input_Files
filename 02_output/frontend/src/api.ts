import type {
  FieldError,
  FormConfig,
  RegistrationAccepted,
  RegistrationRequest,
} from "./types";

/** Only relative `/api` URLs (AR-01). */
export const FORM_CONFIG_URL = "/api/form-config";
export const REGISTRATIONS_URL = "/api/registrations";

export type SubmitResult =
  | { kind: "accepted"; registration: RegistrationAccepted }
  | { kind: "rejected"; errors: FieldError[] }
  | { kind: "failed" };

/** Loads the form configuration; throws when it is unavailable. */
export async function loadFormConfig(): Promise<FormConfig> {
  const response = await fetch(FORM_CONFIG_URL);
  if (!response.ok) {
    throw new Error(`form configuration answered ${response.status}`);
  }
  const config = (await response.json()) as FormConfig;
  if (!Array.isArray(config?.options) || !Array.isArray(config?.consents)) {
    throw new Error("form configuration is incomplete");
  }
  return config;
}

function isFieldError(value: unknown): value is FieldError {
  const candidate = value as FieldError;
  return (
    typeof candidate?.field === "string" &&
    typeof candidate?.message === "string"
  );
}

function isAccepted(value: unknown): value is RegistrationAccepted {
  const candidate = value as RegistrationAccepted;
  return (
    typeof candidate?.id === "string" &&
    typeof candidate?.firstName === "string" &&
    typeof candidate?.lastName === "string" &&
    Array.isArray(candidate?.options)
  );
}

/** Submits a registration; never throws (AC-004-04: failures become `failed`). */
export async function submitRegistration(
  request: RegistrationRequest,
): Promise<SubmitResult> {
  try {
    const response = await fetch(REGISTRATIONS_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
    });
    if (response.status === 201) {
      const body: unknown = await response.json();
      return isAccepted(body)
        ? { kind: "accepted", registration: body }
        : { kind: "failed" };
    }
    if (response.status === 400) {
      const body = (await response.json()) as { errors?: unknown };
      const errors = Array.isArray(body?.errors)
        ? body.errors.filter(isFieldError)
        : [];
      return errors.length > 0
        ? { kind: "rejected", errors }
        : { kind: "failed" };
    }
    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}
