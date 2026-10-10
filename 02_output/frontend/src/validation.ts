import type { Consent, RegistrationType } from "./api";

/** Client-side checks mirroring the backend rules (NFR-03, spec section 4). */

export const MAX_TEXT = 200;
export const MAX_EMAIL = 254;

export const MESSAGES = {
  REQUIRED: "This field is required.",
  TOO_LONG: "This value is too long.",
  INVALID_CHARACTERS: "This value contains characters that are not allowed.",
  INVALID_EMAIL: "Enter a valid email address.",
  CONSENT_REQUIRED: "This consent is required.",
  CAPTCHA_REQUIRED: "Please confirm that you are not a robot.",
} as const;

export type TextField =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export interface FieldDefinition {
  id: TextField;
  label: string;
  type: "text" | "email";
}

const COMMON: FieldDefinition[] = [
  { id: "firstName", label: "First name", type: "text" },
  { id: "lastName", label: "Last name", type: "text" },
  { id: "email", label: "Email", type: "email" },
];

/** The fixed fields of each registration type (BR-01). */
export const FIELDS: Record<RegistrationType, FieldDefinition[]> = {
  EXTERNAL: [...COMMON, { id: "organization", label: "Organization / institution", type: "text" }],
  STUDENT: [
    ...COMMON,
    { id: "studyInstitution", label: "Study institution", type: "text" },
    { id: "studyProgramme", label: "Study programme", type: "text" },
    { id: "studentId", label: "Student ID", type: "text" },
  ],
};

export interface FormValues {
  text: Record<TextField, string>;
  consentIds: string[];
  captchaToken: string | null;
}

const ATOM = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+";
const LOCAL_PART = new RegExp(`^${ATOM}(\\.${ATOM})*$`);
const DOMAIN = /^([A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z]{2,}$/;

export function isValidEmail(email: string): boolean {
  const at = email.indexOf("@");
  if (at <= 0 || at !== email.lastIndexOf("@") || at === email.length - 1) {
    return false;
  }
  const local = email.slice(0, at);
  return local.length <= 64 && LOCAL_PART.test(local) && DOMAIN.test(email.slice(at + 1));
}

/** True when the text contains a control character (Unicode category Cc). */
export function hasControlCharacter(value: string): boolean {
  for (const char of value) {
    const code = char.codePointAt(0) ?? 0;
    if (code <= 0x1f || (code >= 0x7f && code <= 0x9f)) {
      return true;
    }
  }
  return false;
}

function checkText(value: string, max: number): string | null {
  const trimmed = value.trim();
  if (trimmed === "") {
    return MESSAGES.REQUIRED;
  }
  if ([...trimmed].length > max) {
    return MESSAGES.TOO_LONG;
  }
  if (hasControlCharacter(trimmed)) {
    return MESSAGES.INVALID_CHARACTERS;
  }
  return null;
}

/** Field id to message for every problem found; empty when the form may be sent. */
export function validate(
  values: FormValues,
  type: RegistrationType,
  consents: Consent[],
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const field of FIELDS[type]) {
    const value = values.text[field.id];
    const problem = checkText(value, field.type === "email" ? MAX_EMAIL : MAX_TEXT);
    if (problem) {
      errors[field.id] = problem;
    } else if (field.type === "email" && !isValidEmail(value.trim())) {
      errors[field.id] = MESSAGES.INVALID_EMAIL;
    }
  }
  for (const consent of consents) {
    if (consent.mandatory && !values.consentIds.includes(consent.id)) {
      errors[`consents.${consent.id}`] = MESSAGES.CONSENT_REQUIRED;
    }
  }
  if (!values.captchaToken) {
    errors.captchaToken = MESSAGES.CAPTCHA_REQUIRED;
  }
  return errors;
}
