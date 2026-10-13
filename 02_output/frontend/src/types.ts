// Shapes of the REST contract (docs/02_contracts/openapi.json).

export type RegistrationType = "EXTERNAL" | "STUDENT";
export type OptionCategory = "WORKSHOP" | "EVENT" | "MEAL" | "OTHER";

export interface ConferenceOption {
  id: string;
  name: string;
  category: OptionCategory;
  availableTo: RegistrationType[];
}

export interface Consent {
  id: string;
  text: string;
  mandatory: boolean;
}

export interface FormConfig {
  conferenceName: string;
  recaptchaSiteKey: string;
  recaptchaTestMode: boolean;
  consents: Consent[];
  options: ConferenceOption[];
}

export type TextField =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export type FieldErrors = Record<string, string>;

export interface RegistrationPayload {
  type: RegistrationType;
  firstName: string;
  lastName: string;
  email: string;
  organization?: string;
  studyInstitution?: string;
  studyProgramme?: string;
  studentId?: string;
  optionIds: string[];
  consents: string[];
  recaptchaToken: string;
}

export type SubmitResult =
  | { kind: "created"; id: string }
  | { kind: "rejected"; fieldErrors: FieldErrors }
  | { kind: "failed" };
