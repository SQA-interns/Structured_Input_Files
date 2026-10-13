import type {
  FormConsent,
  OptionCategory,
  RegistrationType,
  TextField,
} from "./types";

/** Client rules mirroring the backend (spec §4 step 3, `ui-contract.md`, NFR-03). */
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const MESSAGES = {
  required: "This field is required.",
  email: "Enter a valid email address.",
  consent: "This consent is required.",
  captcha: "Please confirm that you are not a robot.",
} as const;

export const FIELDS: Record<
  RegistrationType,
  { name: TextField; label: string }[]
> = {
  EXTERNAL: [
    { name: "firstName", label: "First name" },
    { name: "lastName", label: "Last name" },
    { name: "email", label: "Email" },
    { name: "organization", label: "Organization / institution" },
  ],
  STUDENT: [
    { name: "firstName", label: "First name" },
    { name: "lastName", label: "Last name" },
    { name: "email", label: "Email" },
    { name: "studyInstitution", label: "Study institution" },
    { name: "studyProgramme", label: "Study programme" },
    { name: "studentId", label: "Student ID" },
  ],
};

export const CATEGORIES: { category: OptionCategory; legend: string }[] = [
  { category: "WORKSHOP", legend: "Workshops" },
  { category: "EVENT", legend: "Events" },
  { category: "MEAL", legend: "Meals" },
  { category: "OTHER", legend: "Other activities" },
];

export interface FormState {
  type: RegistrationType;
  values: Partial<Record<TextField, string>>;
  consents: Record<string, boolean>;
  token: string | null;
}

/** Field name → message for every rule the state violates; empty when valid. */
export function validate(
  state: FormState,
  consents: FormConsent[],
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const { name } of FIELDS[state.type]) {
    const value = (state.values[name] ?? "").trim();
    if (value === "") {
      errors[name] = MESSAGES.required;
    } else if (name === "email" && !EMAIL_PATTERN.test(value)) {
      errors[name] = MESSAGES.email;
    }
  }
  for (const consent of consents) {
    if (consent.mandatory && !state.consents[consent.id]) {
      errors[`consents.${consent.id}`] = MESSAGES.consent;
    }
  }
  if (!state.token) {
    errors.recaptchaToken = MESSAGES.captcha;
  }
  return errors;
}

/** The id of the error element of a field (`ui-contract.md`, "Field errors"). */
export function errorId(field: string): string {
  return `error-${field}`;
}
