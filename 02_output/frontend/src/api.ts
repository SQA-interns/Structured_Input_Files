import type { FieldError, FormConfig } from "./types";

/** GET /api/form-config (AR-01: relative /api URLs only). */
export async function fetchFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new Error(`form configuration answered ${response.status}`);
  }
  return (await response.json()) as FormConfig;
}

/** openapi.json RegistrationResponse. */
export interface AcceptedRegistration {
  id: string;
  type: string;
  firstName: string;
  lastName: string;
  options: { id: string; name: string; category: string }[];
  registeredAt: string;
}

export type SubmitResult =
  | { kind: "accepted"; registration: AcceptedRegistration }
  | { kind: "rejected"; fieldErrors: FieldError[] }
  | { kind: "failed" };

/** POST /api/registrations; never throws, every failure is a result. */
export async function submitRegistration(body: Record<string, unknown>): Promise<SubmitResult> {
  try {
    const response = await fetch("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(body),
    });
    const answer = await response.json();
    if (response.status === 201) {
      return { kind: "accepted", registration: answer as AcceptedRegistration };
    }
    const fieldErrors = (answer as { fieldErrors?: FieldError[] }).fieldErrors;
    if (response.status === 400 && Array.isArray(fieldErrors) && fieldErrors.length > 0) {
      return { kind: "rejected", fieldErrors };
    }
    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}
