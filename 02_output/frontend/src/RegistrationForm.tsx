import { useState } from "react";
import type { AcceptedRegistration } from "./api";
import { groupByCategory, optionsFor } from "./options";
import type { FormConfig, RegistrationType } from "./types";

export interface RegistrationFormProps {
  config: FormConfig;
  onAccepted: (registration: AcceptedRegistration) => void;
}

interface FieldDefinition {
  name: string;
  label: string;
  inputType?: "email";
}

const FIELDS: Record<RegistrationType, FieldDefinition[]> = {
  EXTERNAL: [
    { name: "firstName", label: "First name" },
    { name: "lastName", label: "Last name" },
    { name: "email", label: "Email", inputType: "email" },
    { name: "organization", label: "Organization / institution" },
  ],
  STUDENT: [],
};

const TEST_TOKEN = "test-pass";

function toggle(values: string[], value: string): string[] {
  return values.includes(value) ? values.filter((v) => v !== value) : [...values, value];
}

/** The registration form (ui.md). */
export function RegistrationForm({ config }: RegistrationFormProps) {
  const [type, setType] = useState<RegistrationType | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [optionIds, setOptionIds] = useState<string[]>([]);
  const [consentIds, setConsentIds] = useState<string[]>([]);
  const [captchaToken, setCaptchaToken] = useState("");

  return (
    <form noValidate>
      <fieldset>
        <legend>Registration type</legend>
        <label>
          <input
            type="radio"
            name="type"
            checked={type === "EXTERNAL"}
            onChange={() => setType("EXTERNAL")}
          />
          External participant
        </label>
        <label>
          <input
            type="radio"
            name="type"
            checked={type === "STUDENT"}
            onChange={() => setType("STUDENT")}
          />
          Student
        </label>
      </fieldset>
      {type && (
        <>
          {FIELDS[type].map((field) => (
            <div key={field.name}>
              <label htmlFor={field.name}>{field.label}</label>
              <input
                id={field.name}
                name={field.name}
                type={field.inputType ?? "text"}
                value={values[field.name] ?? ""}
                onChange={(e) => setValues({ ...values, [field.name]: e.target.value })}
              />
            </div>
          ))}
          {groupByCategory(optionsFor(config.options, type)).map((group) => (
            <fieldset key={group.category}>
              <legend>{group.legend}</legend>
              {group.options.map((option) => (
                <label key={option.id}>
                  <input
                    type="checkbox"
                    checked={optionIds.includes(option.id)}
                    onChange={() => setOptionIds(toggle(optionIds, option.id))}
                  />
                  {option.name}
                </label>
              ))}
            </fieldset>
          ))}
          {config.consents.map((consent) => (
            <div key={consent.id}>
              <label>
                <input
                  type="checkbox"
                  checked={consentIds.includes(consent.id)}
                  onChange={() => setConsentIds(toggle(consentIds, consent.id))}
                />
                {consent.text}
              </label>
            </div>
          ))}
          {config.recaptcha.testMode && (
            <label>
              <input
                type="checkbox"
                checked={captchaToken === TEST_TOKEN}
                onChange={(e) => setCaptchaToken(e.target.checked ? TEST_TOKEN : "")}
              />
              I am not a robot (test mode)
            </label>
          )}
          <button type="submit">Register</button>
        </>
      )}
    </form>
  );
}
