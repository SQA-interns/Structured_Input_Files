import type { FieldErrorItem, FormConfig, RegistrationCreated, RegistrationRequest } from "./types";

/** The frontend talks to the backend only through /api (AR-01). */

export async function loadFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new Error("form configuration unavailable");
  }
  return (await response.json()) as FormConfig;
}

export type SubmitResult =
  | { kind: "created"; registration: RegistrationCreated }
  | { kind: "rejected"; fieldErrors: FieldErrorItem[] }
  | { kind: "failed" };

/** Sends a registration; any outcome other than 201 or 400 with field errors is "failed". */
export async function submitRegistration(request: RegistrationRequest): Promise<SubmitResult> {
  let response: Response;
  try {
    response = await fetch("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(request),
    });
  } catch {
    return { kind: "failed" };
  }
  try {
    if (response.status === 201) {
      return { kind: "created", registration: (await response.json()) as RegistrationCreated };
    }
    if (response.status === 400) {
      const body = (await response.json()) as { fieldErrors?: FieldErrorItem[] };
      if (Array.isArray(body.fieldErrors) && body.fieldErrors.length > 0) {
        return { kind: "rejected", fieldErrors: body.fieldErrors };
      }
    }
  } catch {
    return { kind: "failed" };
  }
  return { kind: "failed" };
}
