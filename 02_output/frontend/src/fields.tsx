import type { ChangeEvent } from "react";

/** A labelled text input whose error is its accessible description (contract ui.md). */
export function TextInput(props: {
  name: string;
  label: string;
  type?: "text" | "email";
  value: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  const id = `field-${props.name}`;
  const errorId = `${id}-error`;
  return (
    <div className="field">
      <label htmlFor={id}>{props.label}</label>
      <input
        id={id}
        name={props.name}
        type={props.type ?? "text"}
        value={props.value}
        aria-invalid={props.error ? true : undefined}
        aria-describedby={props.error ? errorId : undefined}
        onChange={(e: ChangeEvent<HTMLInputElement>) => props.onChange(e.target.value)}
      />
      {props.error && (
        <span id={errorId} className="error">
          {props.error}
        </span>
      )}
    </div>
  );
}

/** A labelled checkbox with an optional error as its accessible description. */
export function Checkbox(props: {
  id: string;
  label: string;
  checked: boolean;
  required?: boolean;
  error?: string;
  onChange: (checked: boolean) => void;
}) {
  const errorId = `${props.id}-error`;
  return (
    <div className="checkbox">
      <input
        id={props.id}
        type="checkbox"
        checked={props.checked}
        aria-required={props.required ? true : undefined}
        aria-invalid={props.error ? true : undefined}
        aria-describedby={props.error ? errorId : undefined}
        onChange={(e) => props.onChange(e.target.checked)}
      />
      <label htmlFor={props.id}>{props.label}</label>
      {props.error && (
        <span id={errorId} className="error">
          {props.error}
        </span>
      )}
    </div>
  );
}
