/** Types of the REST API (`docs/02_contracts/openapi.yaml`). */

export type RegistrationType = "EXTERNAL" | "STUDENT";

export type OptionCategory = "WORKSHOP" | "EVENT" | "MEAL" | "OTHER";

export interface FormOption {
  id: string;
  name: string;
  category: OptionCategory;
  availableTo: RegistrationType[];
}

export interface FormConsent {
  id: string;
  text: string;
  mandatory: boolean;
}

export interface FormConfig {
  conferenceName: string;
  options: FormOption[];
  consents: FormConsent[];
  captcha: { mode: "test" | "recaptcha"; siteKey: string | null };
}

export interface FieldError {
  field: string;
  message: string;
}

export interface AcceptedOption {
  id: string;
  name: string;
  category: OptionCategory;
}

export interface RegistrationAccepted {
  id: string;
  type: RegistrationType;
  firstName: string;
  lastName: string;
  options: AcceptedOption[];
  registeredAt: string;
}

export type TextField =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export type RegistrationRequest = Partial<Record<TextField, string>> & {
  type: RegistrationType;
  optionIds: string[];
  consents: Record<string, boolean>;
  recaptchaToken: string | null;
};
