import { useState, type FormEvent } from "react";
import { submitRegistration } from "../api";
import type {
  FormConfig,
  RegistrationAccepted,
  RegistrationRequest,
  RegistrationType,
  TextField,
} from "../types";
import { FIELDS, errorId, validate } from "../validation";
import { Captcha } from "./Captcha";
import { FieldErrorText } from "./FieldErrorText";
import { OptionGroups } from "./OptionGroups";

const GENERAL_ERROR =
  "Registration could not be completed. Please try again later.";

/** Error fields shown next to a field; any other backend error is a general error. */
function isDisplayable(field: string, type: RegistrationType): boolean {
  return (
    FIELDS[type].some((f) => f.name === field) ||
    field === "options" ||
    field === "recaptchaToken" ||
    field.startsWith("consents.")
  );
}

interface RegistrationFormProps {
  config: FormConfig;
  onAccepted: (registration: RegistrationAccepted) => void;
}

/** The registration form of `ui-contract.md` (US-001, US-002, US-004, NFR-03). */
export function RegistrationForm({
  config,
  onAccepted,
}: RegistrationFormProps) {
  const [type, setType] = useState<RegistrationType | null>(null);
  const [values, setValues] = useState<Partial<Record<TextField, string>>>({});
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [consents, setConsents] = useState<Record<string, boolean>>({});
  const [token, setToken] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [captchaResets, setCaptchaResets] = useState(0);

  function chooseType(next: RegistrationType) {
    setType(next);
    setErrors({});
    setSelected(
      (current) =>
        new Set(
          config.options
            .filter((o) => current.has(o.id) && o.availableTo.includes(next))
            .map((o) => o.id),
        ),
    );
  }

  function toggleOption(id: string) {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  function buildRequest(current: RegistrationType): RegistrationRequest {
    const request: RegistrationRequest = {
      type: current,
      optionIds: config.options
        .filter((o) => selected.has(o.id) && o.availableTo.includes(current))
        .map((o) => o.id),
      consents: Object.fromEntries(
        config.consents.map((c) => [c.id, consents[c.id] === true]),
      ),
      recaptchaToken: token,
    };
    for (const { name } of FIELDS[current]) {
      request[name] = (values[name] ?? "").trim();
    }
    return request;
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!type || submitting) return;
    setGeneralError(false);
    const clientErrors = validate(
      { type, values, consents, token },
      config.consents,
    );
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) return;

    setSubmitting(true);
    const result = await submitRegistration(buildRequest(type));
    setSubmitting(false);
    if (result.kind === "accepted") {
      onAccepted(result.registration);
      return;
    }
    if (result.kind === "rejected") {
      const fieldErrors: Record<string, string> = {};
      for (const error of result.errors) {
        if (isDisplayable(error.field, type)) {
          fieldErrors[error.field] ??= error.message;
        } else {
          setGeneralError(true);
        }
      }
      setErrors(fieldErrors);
      if (config.captcha.mode === "recaptcha") {
        setCaptchaResets((n) => n + 1);
      }
      return;
    }
    setGeneralError(true);
  }

  const described = (field: string) =>
    errors[field] ? errorId(field) : undefined;

  return (
    <form noValidate onSubmit={submit}>
      <fieldset role="radiogroup">
        <legend>Registration type</legend>
        {(
          [
            ["EXTERNAL", "External participant"],
            ["STUDENT", "Student"],
          ] as const
        ).map(([value, label]) => (
          <label key={value} className="checkbox">
            <input
              type="radio"
              name="type"
              value={value}
              checked={type === value}
              onChange={() => chooseType(value)}
            />
            {label}
          </label>
        ))}
      </fieldset>

      {type && (
        <>
          {FIELDS[type].map(({ name, label }) => (
            <div key={name} className="field">
              <label htmlFor={`field-${name}`}>{label}</label>
              <input
                id={`field-${name}`}
                name={name}
                type={name === "email" ? "email" : "text"}
                value={values[name] ?? ""}
                aria-invalid={errors[name] ? true : undefined}
                aria-describedby={described(name)}
                onChange={(event) =>
                  setValues((v) => ({ ...v, [name]: event.target.value }))
                }
              />
              <FieldErrorText field={name} message={errors[name]} />
            </div>
          ))}

          <OptionGroups
            options={config.options}
            type={type}
            selected={selected}
            onToggle={toggleOption}
            error={errors.options}
          />

          <fieldset>
            <legend>Consents</legend>
            {config.consents.map((consent) => {
              const field = `consents.${consent.id}`;
              return (
                <div key={consent.id} className="field">
                  <label className="checkbox">
                    <input
                      type="checkbox"
                      checked={consents[consent.id] === true}
                      aria-invalid={errors[field] ? true : undefined}
                      aria-describedby={described(field)}
                      onChange={(event) =>
                        setConsents((c) => ({
                          ...c,
                          [consent.id]: event.target.checked,
                        }))
                      }
                    />
                    {consent.text}
                  </label>
                  <FieldErrorText field={field} message={errors[field]} />
                </div>
              );
            })}
          </fieldset>

          <div className="field">
            <Captcha
              mode={config.captcha.mode}
              siteKey={config.captcha.siteKey}
              token={token}
              onToken={setToken}
              resetCount={captchaResets}
              describedBy={described("recaptchaToken")}
            />
            <FieldErrorText
              field="recaptchaToken"
              message={errors.recaptchaToken}
            />
          </div>

          {generalError && (
            <p role="alert" className="general-error">
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
