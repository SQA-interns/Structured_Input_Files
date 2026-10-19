import type { ConferenceOption, OptionCategory, RegistrationType } from "./types";

export interface OptionGroup {
  category: OptionCategory;
  legend: string;
  options: ConferenceOption[];
}

/** Active options available to the type (OQ-01). */
export function optionsFor(
  _options: ConferenceOption[],
  _type: RegistrationType,
): ConferenceOption[] {
  throw new Error("not implemented");
}

/** Groups in the order of ui.md, without empty groups. */
export function groupByCategory(_options: ConferenceOption[]): OptionGroup[] {
  throw new Error("not implemented");
}
