import type { FormConfig } from "./types";
import type { AcceptedRegistration } from "./api";

export interface RegistrationFormProps {
  config: FormConfig;
  onAccepted: (registration: AcceptedRegistration) => void;
}

/** The registration form (ui.md). */
export function RegistrationForm(_props: RegistrationFormProps): never {
  throw new Error("not implemented");
}
