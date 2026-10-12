import type { RegistrationType } from "./types";

export const MESSAGES = {
  required: "This field is required.",
  email: "Enter a valid email address.",
  consent: "Please give your consent.",
  robot: "Please confirm you are not a robot.",
} as const;

export type TextField =
  | "firstName"
  | "lastName"
  | "email"
  | "organization"
  | "studyInstitution"
  | "studyProgramme"
  | "studentId";

export const FIELDS: Record<
  RegistrationType,
  { name: TextField; label: string; type?: string }[]
> = {
  EXTERNAL: [
    { name: "firstName", label: "First name" },
    { name: "lastName", label: "Last name" },
    { name: "email", label: "Email", type: "email" },
    { name: "organization", label: "Organization / institution" },
  ],
  STUDENT: [
    { name: "firstName", label: "First name" },
    { name: "lastName", label: "Last name" },
    { name: "email", label: "Email", type: "email" },
    { name: "studyInstitution", label: "Study institution" },
    { name: "studyProgramme", label: "Study programme" },
    { name: "studentId", label: "Student ID" },
  ],
};

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Client-side checks before submission (NFR-03); the backend stays authoritative (SB-01). */
export function validate(
  type: RegistrationType,
  values: Partial<Record<TextField, string>>,
  consentGiven: boolean,
  token: string,
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const field of FIELDS[type]) {
    const value = (values[field.name] ?? "").trim();
    if (value === "") errors[field.name] = MESSAGES.required;
    else if (field.name === "email" && !EMAIL.test(value))
      errors[field.name] = MESSAGES.email;
  }
  if (!consentGiven) errors.consentGiven = MESSAGES.consent;
  if (token === "") errors.recaptchaToken = MESSAGES.robot;
  return errors;
}
