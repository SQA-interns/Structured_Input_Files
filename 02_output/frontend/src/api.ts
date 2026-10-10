/** REST client for the backend under /api (AR-01, contract docs/02_contracts/openapi.yaml). */

export type RegistrationType = "EXTERNAL" | "STUDENT";
export type OptionCategory = "WORKSHOP" | "EVENT" | "MEAL" | "OTHER";

export interface ConferenceOption {
  id: string;
  name: string;
  category: OptionCategory;
  audience: RegistrationType[];
}

export interface Consent {
  id: string;
  text: string;
  mandatory: boolean;
}

export interface FormConfig {
  conferenceName: string;
  options: ConferenceOption[];
  consents: Consent[];
  recaptcha: { siteKey: string; testMode: boolean };
}

export interface RegistrationRequest {
  type: RegistrationType;
  firstName: string;
  lastName: string;
  email: string;
  organization?: string;
  studyInstitution?: string;
  studyProgramme?: string;
  studentId?: string;
  optionIds: string[];
  consentIds: string[];
  captchaToken: string;
}

export interface RegistrationAccepted {
  reference: string;
  submittedAt: string;
  type: RegistrationType;
  firstName: string;
  lastName: string;
  options: { id: string; name: string; category: OptionCategory }[];
}

export interface FieldError {
  field: string;
  code: string;
  message: string;
}

export type SubmitResult =
  | { kind: "accepted"; registration: RegistrationAccepted }
  | { kind: "rejected"; status: number; fieldErrors: FieldError[] }
  | { kind: "failed"; status: number };

export async function fetchFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new Error(`form configuration unavailable (${response.status})`);
  }
  return (await response.json()) as FormConfig;
}

/** Never throws: network and server failures become a "failed" result. */
export async function submitRegistration(body: RegistrationRequest): Promise<SubmitResult> {
  let response: Response;
  try {
    response = await fetch("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(body),
    });
  } catch {
    return { kind: "failed", status: 0 };
  }
  if (response.status === 201) {
    return { kind: "accepted", registration: (await response.json()) as RegistrationAccepted };
  }
  if (response.status === 400 || response.status === 409) {
    const fieldErrors = await readFieldErrors(response);
    if (fieldErrors.length > 0) {
      return { kind: "rejected", status: response.status, fieldErrors };
    }
  }
  return { kind: "failed", status: response.status };
}

async function readFieldErrors(response: Response): Promise<FieldError[]> {
  try {
    const body = (await response.json()) as { fieldErrors?: FieldError[] };
    return Array.isArray(body.fieldErrors) ? body.fieldErrors : [];
  } catch {
    return [];
  }
}
