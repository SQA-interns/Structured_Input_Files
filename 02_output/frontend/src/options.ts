import type { ConferenceOption, RegistrationType } from "./types";

/** Options available to the registration type (BR-04, OQ-01). */
export function availableOptions(
  options: ConferenceOption[],
  type: RegistrationType,
): ConferenceOption[] {
  return options.filter((option) => option.availableTo.includes(type));
}
