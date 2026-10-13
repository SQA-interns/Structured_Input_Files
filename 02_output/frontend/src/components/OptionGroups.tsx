import type { FormOption, RegistrationType } from "../types";
import { CATEGORIES, errorId } from "../validation";
import { FieldErrorText } from "./FieldErrorText";

interface OptionGroupsProps {
  options: FormOption[];
  type: RegistrationType;
  selected: Set<string>;
  onToggle: (id: string) => void;
  error?: string;
}

/** Active options available to the type, one group per category (BR-04, OQ-01). */
export function OptionGroups(props: OptionGroupsProps) {
  const { options, type, selected, onToggle, error } = props;
  const available = options.filter((o) => o.availableTo.includes(type));
  return (
    <>
      <div
        data-testid="options"
        role="group"
        aria-label="Options"
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId("options") : undefined}
      >
        {CATEGORIES.map(({ category, legend }) => {
          const inCategory = available.filter((o) => o.category === category);
          if (inCategory.length === 0) return null;
          return (
            <fieldset key={category}>
              <legend>{legend}</legend>
              {inCategory.map((option) => (
                <label key={option.id} className="checkbox">
                  <input
                    type="checkbox"
                    checked={selected.has(option.id)}
                    onChange={() => onToggle(option.id)}
                  />
                  {option.name}
                </label>
              ))}
            </fieldset>
          );
        })}
      </div>
      <FieldErrorText field="options" message={error} />
    </>
  );
}
