/** Types of the REST contract (docs/02_contracts/openapi.json). */

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
  recaptcha: { siteKey: string; testMode: boolean };
  options: ConferenceOption[];
  consents: Consent[];
}

export interface FieldErrorItem {
  field: string;
  message: string;
}

export interface RegistrationCreated {
  id: string;
  type: RegistrationType;
  firstName: string;
  lastName: string;
  options: { id: string; name: string; category: Category }[];
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
  consents: string[];
  recaptchaToken: string;
}
