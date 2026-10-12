import type {
  Confirmation,
  ConferenceOption,
  FieldError,
  RegistrationRequest,
  Settings,
} from "./types";

/** Result of a registration submission. */
export type SubmitResult =
  | { kind: "accepted"; confirmation: Confirmation }
  | { kind: "rejected"; fieldErrors: FieldError[] }
  | { kind: "failed" };

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: { Accept: "application/json" },
  });
  if (!response.ok)
    throw new Error(`GET ${path} failed with ${response.status}`);
  return (await response.json()) as T;
}

export function fetchSettings(): Promise<Settings> {
  return getJson<Settings>("/api/settings");
}

export function fetchOptions(): Promise<ConferenceOption[]> {
  return getJson<ConferenceOption[]>("/api/options");
}

export async function submitRegistration(
  request: RegistrationRequest,
): Promise<SubmitResult> {
  let response: Response;
  try {
    response = await fetch("/api/registrations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(request),
    });
  } catch {
    return { kind: "failed" };
  }
  if (response.status === 201) {
    return {
      kind: "accepted",
      confirmation: (await response.json()) as Confirmation,
    };
  }
  if (response.status === 400 || response.status === 409) {
    try {
      const problem = (await response.json()) as { fieldErrors?: FieldError[] };
      if (
        Array.isArray(problem.fieldErrors) &&
        problem.fieldErrors.length > 0
      ) {
        return { kind: "rejected", fieldErrors: problem.fieldErrors };
      }
    } catch {
      // fall through to a generic failure
    }
  }
  return { kind: "failed" };
}
