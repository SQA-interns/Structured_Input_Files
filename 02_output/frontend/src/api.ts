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
export async function submitRegistration(_body: Record<string, unknown>): Promise<SubmitResult> {
  throw new Error("not implemented");
}
