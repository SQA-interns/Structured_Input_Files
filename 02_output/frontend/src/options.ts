import type { ConferenceOption, OptionCategory, RegistrationType } from "./types";

export interface OptionGroup {
  category: OptionCategory;
  legend: string;
  options: ConferenceOption[];
}

const GROUPS: { category: OptionCategory; legend: string }[] = [
  { category: "WORKSHOP", legend: "Workshops" },
  { category: "EVENT", legend: "Events" },
  { category: "MEAL", legend: "Meals" },
  { category: "OTHER", legend: "Other activities" },
];

/** Active options available to the type (OQ-01). */
export function optionsFor(
  options: ConferenceOption[],
  type: RegistrationType,
): ConferenceOption[] {
  return options.filter((option) => option.availableTo.includes(type));
}

/** Groups in the order of ui.md, without empty groups. */
export function groupByCategory(options: ConferenceOption[]): OptionGroup[] {
  return GROUPS.map((group) => ({
    ...group,
    options: options.filter((option) => option.category === group.category),
  })).filter((group) => group.options.length > 0);
}
