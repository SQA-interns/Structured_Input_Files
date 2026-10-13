import type {
  Consent,
  FieldErrors,
  RegistrationType,
  TextField,
} from "./types";

// The same field rules as the API (spec 4.2), checked before sending (NFR-03).

export const MESSAGES = {
  required: "This field is required.",
  email: "Enter a valid email address.",
  type: "Choose a registration type.",
  consent: "This consent is required.",
  captcha: "Confirm that you are not a robot.",
  tooLong: (max: number) => `Use at most ${max} characters.`,
};

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const MAX_LENGTH: Record<TextField, number> = {
  firstName: 100,
  lastName: 100,
  email: 254,
  organization: 200,
  studyInstitution: 200,
  studyProgramme: 200,
  studentId: 50,
};

export const FIELDS: Record<RegistrationType, TextField[]> = {
  EXTERNAL: ["firstName", "lastName", "email", "organization"],
  STUDENT: [
    "firstName",
    "lastName",
    "email",
    "studyInstitution",
    "studyProgramme",
    "studentId",
  ],
};

export interface FormInput {
  type: RegistrationType | null;
  values: Partial<Record<TextField, string>>;
  consents: string[];
  recaptchaToken: string;
}

export function validate(input: FormInput, consents: Consent[]): FieldErrors {
  const errors: FieldErrors = {};
  if (input.type === null) {
    errors.type = MESSAGES.type;
  } else {
    for (const field of FIELDS[input.type]) {
      const value = (input.values[field] ?? "").trim();
      if (value === "") {
        errors[field] = MESSAGES.required;
      } else if (value.length > MAX_LENGTH[field]) {
        errors[field] = MESSAGES.tooLong(MAX_LENGTH[field]);
      } else if (field === "email" && !EMAIL.test(value)) {
        errors[field] = MESSAGES.email;
      }
    }
  }
  for (const consent of consents) {
    if (consent.mandatory && !input.consents.includes(consent.id)) {
      errors[`consents.${consent.id}`] = MESSAGES.consent;
    }
  }
  if (input.recaptchaToken === "") {
    errors.recaptchaToken = MESSAGES.captcha;
  }
  return errors;
}
