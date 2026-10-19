import { useRef, useState, type FormEvent } from "react";
import { submitRegistration, type AcceptedRegistration } from "./api";
import { groupByCategory, optionsFor } from "./options";
import { RecaptchaWidget } from "./RecaptchaWidget";
import type { FormConfig, RegistrationType } from "./types";
import { validateForm } from "./validation";

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
  STUDENT: [
    { name: "firstName", label: "First name" },
    { name: "lastName", label: "Last name" },
    { name: "email", label: "Email", inputType: "email" },
    { name: "studyInstitution", label: "Study institution" },
    { name: "studyProgramme", label: "Study programme" },
    { name: "studentId", label: "Student ID" },
  ],
};

const TEST_TOKEN = "test-pass";
const GENERAL_ERROR = "Registration could not be completed. Please try again later.";

function toggle(values: string[], value: string): string[] {
  return values.includes(value) ? values.filter((v) => v !== value) : [...values, value];
}

function errorProps(errors: Record<string, string>, field: string) {
  return errors[field]
    ? { "aria-invalid": true as const, "aria-describedby": `error-${field}` }
    : {};
}

function ErrorText({ errors, field }: { errors: Record<string, string>; field: string }) {
  return errors[field] ? (
    <p id={`error-${field}`} className="field-error">
      {errors[field]}
    </p>
  ) : null;
}

/** The registration form (ui.md). */
export function RegistrationForm({ config, onAccepted }: RegistrationFormProps) {
  const [type, setType] = useState<RegistrationType | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [optionIds, setOptionIds] = useState<string[]>([]);
  const [consentIds, setConsentIds] = useState<string[]>([]);
  const [captchaToken, setCaptchaToken] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState(false);
  const [sending, setSending] = useState(false);
  const [captchaReset, setCaptchaReset] = useState(0);
  const inFlight = useRef(false);

  function chooseType(next: RegistrationType) {
    const available = optionsFor(config.options, next).map((option) => option.id);
    setOptionIds(optionIds.filter((id) => available.includes(id)));
    setErrors({});
    setType(next);
  }

  function knownFields(current: RegistrationType): string[] {
    return [
      ...FIELDS[current].map((field) => field.name),
      "optionIds",
      "recaptchaToken",
      ...config.consents.map((consent) => `consents.${consent.id}`),
    ];
  }

  function resetCaptcha() {
    if (!config.recaptcha.testMode) {
      setCaptchaToken("");
      setCaptchaReset((n) => n + 1);
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!type || inFlight.current) {
      return;
    }
    setGeneralError(false);
    const clientErrors = validateForm(type, values, consentIds, config.consents);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) {
      return;
    }
    const body: Record<string, unknown> = { type };
    for (const field of FIELDS[type]) {
      body[field.name] = (values[field.name] ?? "").trim();
    }
    body.optionIds = optionIds;
    body.consentIds = consentIds;
    body.recaptchaToken = captchaToken;
    inFlight.current = true;
    setSending(true);
    const result = await submitRegistration(body);
    inFlight.current = false;
    setSending(false);
    if (result.kind === "accepted") {
      onAccepted(result.registration);
      return;
    }
    resetCaptcha();
    if (result.kind === "rejected") {
      const known = knownFields(type);
      const fieldErrors: Record<string, string> = {};
      for (const error of result.fieldErrors) {
        if (known.includes(error.field)) {
          fieldErrors[error.field] = error.message;
        }
      }
      setErrors(fieldErrors);
      setGeneralError(result.fieldErrors.some((error) => !known.includes(error.field)));
      return;
    }
    setGeneralError(true);
  }

  return (
    <form noValidate onSubmit={submit}>
      <fieldset>
        <legend>Registration type</legend>
        <label>
          <input
            type="radio"
            name="type"
            checked={type === "EXTERNAL"}
            onChange={() => chooseType("EXTERNAL")}
          />
          External participant
        </label>
        <label>
          <input
            type="radio"
            name="type"
            checked={type === "STUDENT"}
            onChange={() => chooseType("STUDENT")}
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
                {...errorProps(errors, field.name)}
              />
              <ErrorText errors={errors} field={field.name} />
            </div>
          ))}
          <div className="options" {...errorProps(errors, "optionIds")}>
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
          </div>
          <ErrorText errors={errors} field="optionIds" />
          {config.consents.map((consent) => (
            <div key={consent.id}>
              <label>
                <input
                  type="checkbox"
                  checked={consentIds.includes(consent.id)}
                  onChange={() => setConsentIds(toggle(consentIds, consent.id))}
                  {...errorProps(errors, `consents.${consent.id}`)}
                />
                {consent.text}
              </label>
              <ErrorText errors={errors} field={`consents.${consent.id}`} />
            </div>
          ))}
          <div>
            {config.recaptcha.testMode ? (
              <label>
                <input
                  type="checkbox"
                  checked={captchaToken === TEST_TOKEN}
                  onChange={(e) => setCaptchaToken(e.target.checked ? TEST_TOKEN : "")}
                />
                I am not a robot (test mode)
              </label>
            ) : (
              <RecaptchaWidget
                siteKey={config.recaptcha.siteKey}
                onToken={setCaptchaToken}
                resetSignal={captchaReset}
              />
            )}
            <ErrorText errors={errors} field="recaptchaToken" />
          </div>
          {generalError && <p role="alert">{GENERAL_ERROR}</p>}
          <button type="submit" disabled={sending}>
            Register
          </button>
        </>
      )}
    </form>
  );
}
