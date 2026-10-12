import type { RegistrationType, TextField } from "./types";

/** Client-side checks before submission (AC-004-03, NFR-03); the backend checks again (SB-01). */

export const REQUIRED = "This field is required.";
export const INVALID_EMAIL = "Enter a valid email address.";

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export interface FieldDefinition {
  name: TextField;
  label: string;
}

/** The fixed fields of each registration type (BR-01), in display order. */
export const FIELDS: Record<RegistrationType, FieldDefinition[]> = {
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

/** Field errors of the fields of the type; an empty object means the fields are valid. */
export function validateFields(
  type: RegistrationType,
  values: Partial<Record<TextField, string>>,
): Partial<Record<TextField, string>> {
  const errors: Partial<Record<TextField, string>> = {};
  for (const field of FIELDS[type]) {
    const value = (values[field.name] ?? "").trim();
    if (value === "") {
      errors[field.name] = REQUIRED;
    } else if (field.name === "email" && !EMAIL.test(value)) {
      errors[field.name] = INVALID_EMAIL;
    }
  }
  return errors;
}
