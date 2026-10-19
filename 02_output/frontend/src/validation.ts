import type { Consent, RegistrationType } from "./types";

const REQUIRED: Record<RegistrationType, [string, string][]> = {
  EXTERNAL: [
    ["firstName", "First name"],
    ["lastName", "Last name"],
    ["email", "Email"],
    ["organization", "Organization / institution"],
  ],
  STUDENT: [
    ["firstName", "First name"],
    ["lastName", "Last name"],
    ["email", "Email"],
    ["studyInstitution", "Study institution"],
    ["studyProgramme", "Study programme"],
    ["studentId", "Student ID"],
  ],
};

const LABEL = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?";
const ATOM = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+";
const EMAIL = new RegExp(`^${ATOM}(?:\\.${ATOM})*@${LABEL}(?:\\.${LABEL})+$`);

/** Field errors found before sending (NFR-03); keys are API field names. */
export function validateForm(
  type: RegistrationType,
  values: Record<string, string>,
  consentIds: string[],
  consents: Consent[],
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const [field, label] of REQUIRED[type]) {
    if ((values[field] ?? "").trim() === "") {
      errors[field] = `${label} is required.`;
    }
  }
  const email = (values.email ?? "").trim();
  if (email !== "" && !EMAIL.test(email)) {
    errors.email = "Please enter a valid email address.";
  }
  for (const consent of consents) {
    if (consent.mandatory && !consentIds.includes(consent.id)) {
      errors[`consents.${consent.id}`] = "This consent is required.";
    }
  }
  return errors;
}
