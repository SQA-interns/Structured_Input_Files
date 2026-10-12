import { useCallback, useState, type FormEvent } from "react";
import { submitRegistration } from "./api";
import { OptionGroups } from "./OptionGroups";
import { availableOptions } from "./options";
import { Recaptcha } from "./Recaptcha";
import type {
  FormConfig,
  RegistrationCreated,
  RegistrationRequest,
  RegistrationType,
  TextField,
} from "./types";
import { FIELDS, validateFields } from "./validation";

export const GENERAL_ERROR = "The registration could not be completed. Please try again later.";

const TYPES: { value: RegistrationType; label: string }[] = [
  { value: "EXTERNAL", label: "External participant" },
  { value: "STUDENT", label: "Student" },
];

type Errors = Record<string, string>;

interface Props {
  config: FormConfig;
  onCreated: (registration: RegistrationCreated) => void;
}

/** The registration form of docs/02_contracts/ui-registration-form.json (US-001, US-002, US-004). */
export function RegistrationForm({ config, onCreated }: Props) {
  const [type, setType] = useState<RegistrationType | null>(null);
  const [values, setValues] = useState<Partial<Record<TextField, string>>>({});
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [consents, setConsents] = useState<Set<string>>(new Set());
  const [token, setToken] = useState<string | null>(null);
  const [errors, setErrors] = useState<Errors>({});
  const [generalError, setGeneralError] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [resetKey, setResetKey] = useState(0);
  const onToken = useCallback((value: string | null) => setToken(value), []);

  const chooseType = (value: RegistrationType) => {
    setType(value);
    setErrors({});
    setGeneralError(false);
  };

  const toggle = (set: Set<string>, id: string) => {
    const next = new Set(set);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    return next;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (type === null || submitting) {
      return;
    }
    setGeneralError(false);
    const clientErrors = validateFields(type, values);
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors as Errors);
      return;
    }
    setErrors({});
    setSubmitting(true);
    const result = await submitRegistration(buildRequest(type));
    setSubmitting(false);
    if (result.kind === "created") {
      onCreated(result.registration);
      return;
    }
    setResetKey((key) => key + 1);
    if (result.kind === "rejected") {
      const shown = shownFields(type);
      const next: Errors = {};
      let unplaced = false;
      for (const error of result.fieldErrors) {
        if (shown.has(error.field)) {
          next[error.field] = error.message;
        } else {
          unplaced = true;
        }
      }
      setErrors(next);
      setGeneralError(unplaced);
    } else {
      setGeneralError(true);
    }
  };

  const buildRequest = (registrationType: RegistrationType): RegistrationRequest => {
    const request: RegistrationRequest = {
      type: registrationType,
      firstName: "",
      lastName: "",
      email: "",
      optionIds: availableOptions(config.options, registrationType)
        .filter((option) => selected.has(option.id))
        .map((option) => option.id),
      consents: config.consents.filter((c) => consents.has(c.id)).map((c) => c.id),
      recaptchaToken: token ?? "",
    };
    for (const field of FIELDS[registrationType]) {
      request[field.name] = (values[field.name] ?? "").trim();
    }
    return request;
  };

  const shownFields = (registrationType: RegistrationType) =>
    new Set<string>([
      "type",
      ...FIELDS[registrationType].map((field) => field.name),
      "optionIds",
      "consents",
      ...config.consents.map((consent) => `consents.${consent.id}`),
      "recaptchaToken",
    ]);

  return (
    <form noValidate onSubmit={handleSubmit}>
      <div role="radiogroup" aria-labelledby="type-label" className="types">
        <span id="type-label">Registration type</span>
        {TYPES.map((option) => (
          <label key={option.value}>
            <input
              type="radio"
              name="type"
              value={option.value}
              checked={type === option.value}
              onChange={() => chooseType(option.value)}
            />
            {option.label}
          </label>
        ))}
        {errors.type && (
          <p id="type-error" role="alert" className="error">
            {errors.type}
          </p>
        )}
      </div>

      {type !== null && (
        <>
          {FIELDS[type].map((field) => (
            <div className="field" key={field.name}>
              <label htmlFor={`field-${field.name}`}>{field.label}</label>
              <input
                id={`field-${field.name}`}
                name={field.name}
                type={field.name === "email" ? "email" : "text"}
                value={values[field.name] ?? ""}
                onChange={(event) => setValues({ ...values, [field.name]: event.target.value })}
                aria-invalid={errors[field.name] ? "true" : undefined}
                aria-describedby={errors[field.name] ? `${field.name}-error` : undefined}
              />
              {errors[field.name] && (
                <p id={`${field.name}-error`} role="alert" className="error">
                  {errors[field.name]}
                </p>
              )}
            </div>
          ))}

          <OptionGroups
            options={config.options}
            type={type}
            selected={selected}
            onToggle={(id) => setSelected(toggle(selected, id))}
            error={errors.optionIds}
          />

          <fieldset className="consents">
            <legend>Consents</legend>
            {config.consents.map((consent) => {
              const key = `consents.${consent.id}`;
              return (
                <div className="choice" key={consent.id}>
                  <label>
                    <input
                      type="checkbox"
                      checked={consents.has(consent.id)}
                      onChange={() => setConsents(toggle(consents, consent.id))}
                      aria-invalid={errors[key] ? "true" : undefined}
                      aria-describedby={errors[key] ? `${key}-error` : undefined}
                    />
                    {consent.text}
                  </label>
                  {errors[key] && (
                    <p id={`${key}-error`} role="alert" className="error">
                      {errors[key]}
                    </p>
                  )}
                </div>
              );
            })}
            {errors.consents && (
              <p id="consents-error" role="alert" className="error">
                {errors.consents}
              </p>
            )}
          </fieldset>

          <Recaptcha
            siteKey={config.recaptcha.siteKey}
            testMode={config.recaptcha.testMode}
            token={token}
            onToken={onToken}
            resetKey={resetKey}
            error={errors.recaptchaToken}
          />

          {generalError && (
            <p role="alert" className="error general">
              {GENERAL_ERROR}
            </p>
          )}

          <button type="submit" disabled={submitting}>
            Register
          </button>
        </>
      )}
    </form>
  );
}
