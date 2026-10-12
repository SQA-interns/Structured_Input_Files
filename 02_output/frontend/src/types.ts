/** Shapes of the REST API (contract `openapi.yaml`). */
export type RegistrationType = "EXTERNAL" | "STUDENT";
export type Category = "WORKSHOP" | "EVENT" | "MEAL" | "OTHER";

export interface ConferenceOption {
  id: string;
  name: string;
  category: Category;
  registrationTypes: RegistrationType[];
}

export interface Settings {
  conferenceName: string;
  consent: { id: string; text: string };
  recaptcha: { testMode: boolean; siteKey: string };
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
  consentGiven: boolean;
  recaptchaToken: string;
}

export interface Confirmation {
  id: string;
  type: RegistrationType;
  firstName: string;
  lastName: string;
  email: string;
  selectedOptions: { id: string; name: string; category: Category }[];
}

export interface FieldError {
  field: string;
  message: string;
}

export const CATEGORIES: { category: Category; heading: string }[] = [
  { category: "WORKSHOP", heading: "Workshops" },
  { category: "EVENT", heading: "Events" },
  { category: "MEAL", heading: "Meals" },
  { category: "OTHER", heading: "Other activities" },
];
