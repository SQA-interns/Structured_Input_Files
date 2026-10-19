import type { FormConfig } from "./types";

/** GET /api/form-config (AR-01: relative /api URLs only). */
export async function fetchFormConfig(): Promise<FormConfig> {
  throw new Error("not implemented");
}

/** openapi.json RegistrationResponse. */
export interface AcceptedRegistration {
  id: string;
  type: string;
  firstName: string;
  lastName: string;
  options: { id: string; name: string; category: string }[];
  registeredAt: string;
}
