/** Types of openapi.json used by the frontend. */
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
  options: ConferenceOption[];
  consents: Consent[];
  recaptcha: { siteKey: string; testMode: boolean };
}

export interface FieldError {
  field: string;
  message: string;
}
