import type { Consent, RegistrationType } from "./types";

/** Field errors found before sending (NFR-03); keys are API field names. */
export function validateForm(
  _type: RegistrationType,
  _values: Record<string, string>,
  _consentIds: string[],
  _consents: Consent[],
): Record<string, string> {
  throw new Error("not implemented");
}
