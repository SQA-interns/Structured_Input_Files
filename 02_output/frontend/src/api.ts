// Client of the backend REST API (docs/02_contracts/openapi.json); relative URLs only (AR-01).

export type RegistrationType = "EXTERNAL" | "STUDENT";
export type Category = "WORKSHOP" | "EVENT" | "MEAL" | "OTHER";

export interface ConferenceOption {
  id: string;
  name: string;
  category: Category;
  availableTo: RegistrationType[];
}

export interface Consent {
  id: string;
  text: string;
  mandatory: boolean;
}

export interface FormConfig {
  conferenceName: string;
  captcha: { mode: "test" | "recaptcha"; siteKey: string };
  consents: Consent[];
  options: ConferenceOption[];
}

export interface FieldError {
  field: string;
  message: string;
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

export interface RegistrationResponse {
  id: string;
  type: RegistrationType;
  firstName: string;
  lastName: string;
  options: { id: string; name: string; category: Category }[];
}

export type SubmitResult =
  | { kind: "accepted"; registration: RegistrationResponse }
  | { kind: "rejected"; fieldErrors: FieldError[] }
  | { kind: "rate-limited" }
  | { kind: "failed" };

export async function fetchFormConfig(): Promise<FormConfig> {
  const response = await fetch("/api/form-config", { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new Error("form configuration unavailable");
  }
  return (await response.json()) as FormConfig;
}

export async function submitRegistration(body: RegistrationRequest): Promise<SubmitResult> {
  let response: Response;
  try {
    response = await fetch("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(body),
    });
  } catch {
    return { kind: "failed" };
  }
  if (response.status === 201) {
    return { kind: "accepted", registration: (await response.json()) as RegistrationResponse };
  }
  if (response.status === 429) {
    return { kind: "rate-limited" };
  }
  if (response.status === 400) {
    try {
      const error = (await response.json()) as { fieldErrors?: FieldError[] };
      return { kind: "rejected", fieldErrors: error.fieldErrors ?? [] };
    } catch {
      return { kind: "failed" };
    }
  }
  return { kind: "failed" };
}
