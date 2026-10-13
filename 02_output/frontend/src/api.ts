import type {
  FieldErrors,
  FormConfig,
  RegistrationPayload,
  SubmitResult,
} from "./types";

// The frontend talks to the backend only through /api (AR-01).

export async function fetchFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error(`form configuration unavailable (${response.status})`);
  }
  return (await response.json()) as FormConfig;
}

function fieldErrorsOf(body: unknown): FieldErrors | null {
  if (typeof body !== "object" || body === null) {
    return null;
  }
  const errors = (body as { fieldErrors?: unknown }).fieldErrors;
  if (typeof errors !== "object" || errors === null) {
    return null;
  }
  const entries = Object.entries(errors).filter(
    (entry): entry is [string, string] => typeof entry[1] === "string",
  );
  return entries.length > 0 ? Object.fromEntries(entries) : null;
}

/** Sends a registration; any answer other than 201 or 400 with field errors is a failure. */
export async function submitRegistration(
  payload: RegistrationPayload,
): Promise<SubmitResult> {
  try {
    const response = await fetch("/api/registrations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(payload),
    });
    if (response.status === 201) {
      const body = (await response.json()) as { id: string };
      return { kind: "created", id: body.id };
    }
    if (response.status === 400) {
      const fieldErrors = fieldErrorsOf(
        await response.json().catch(() => null),
      );
      if (fieldErrors) {
        return { kind: "rejected", fieldErrors };
      }
    }
    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}
