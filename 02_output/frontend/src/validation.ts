// Client-side checks before any request (NFR-03, contract ui.md); the backend checks again (SB-01).
import type { Consent, RegistrationType } from "./api";

export type TextField =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export const REQUIRED = "This field is required.";
export const INVALID_EMAIL = "Enter a valid email address.";
export const CONSENT_REQUIRED = "This consent is required.";
export const CAPTCHA_REQUIRED = "Please confirm that you are not a robot.";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const FIELDS: Record<RegistrationType, TextField[]> = {
  EXTERNAL: ["firstName", "lastName", "email", "organization"],
  STUDENT: ["firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId"],
};

/** Error messages keyed by field name, consent key (consents.<id>) or "captchaToken". */
export function validate(
  type: RegistrationType,
  values: Partial<Record<TextField, string>>,
  consents: Consent[],
  givenConsents: ReadonlySet<string>,
  captchaToken: string,
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const field of FIELDS[type]) {
    const value = (values[field] ?? "").trim();
    if (value === "") {
      errors[field] = REQUIRED;
    } else if (field === "email" && !EMAIL.test(value)) {
      errors[field] = INVALID_EMAIL;
    }
  }
  for (const consent of consents) {
    if (consent.mandatory && !givenConsents.has(consent.id)) {
      errors[`consents.${consent.id}`] = CONSENT_REQUIRED;
    }
  }
  if (captchaToken === "") {
    errors.captchaToken = CAPTCHA_REQUIRED;
  }
  return errors;
}
