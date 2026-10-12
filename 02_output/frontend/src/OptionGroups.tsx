import { availableOptions } from "./options";
import type { Category, ConferenceOption, RegistrationType } from "./types";

const GROUPS: { category: Category; legend: string }[] = [
  { category: "WORKSHOP", legend: "Workshops" },
  { category: "EVENT", legend: "Events" },
  { category: "MEAL", legend: "Meals" },
  { category: "OTHER", legend: "Other activities" },
];

interface Props {
  options: ConferenceOption[];
  type: RegistrationType;
  selected: Set<string>;
  onToggle: (id: string) => void;
  error?: string;
}

export function OptionGroups({ options, type, selected, onToggle, error }: Props) {
  const available = availableOptions(options, type);
  return (
    <div className="options">
      {GROUPS.map(({ category, legend }) => {
        const inGroup = available.filter((option) => option.category === category);
        if (inGroup.length === 0) {
          return null;
        }
        return (
          <fieldset key={category}>
            <legend>{legend}</legend>
            {inGroup.map((option) => (
              <div className="choice" key={option.id}>
                <label>
                  <input
                    type="checkbox"
                    checked={selected.has(option.id)}
                    onChange={() => onToggle(option.id)}
                    aria-describedby={error ? "optionIds-error" : undefined}
                  />
                  {option.name}
                </label>
              </div>
            ))}
          </fieldset>
        );
      })}
      {error && (
        <p id="optionIds-error" role="alert" className="error">
          {error}
        </p>
      )}
    </div>
  );
}
