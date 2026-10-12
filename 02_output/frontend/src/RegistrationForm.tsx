import { useState, type FormEvent } from "react";
import { submitRegistration } from "./api";
import { Recaptcha } from "./Recaptcha";
import {
  CATEGORIES,
  type Confirmation,
  type ConferenceOption,
  type RegistrationType,
  type Settings,
} from "./types";
import { FIELDS, validate, type TextField } from "./validation";

const FAILURE = "Your registration could not be sent. Please try again.";
const FORM_LEVEL_FIELDS = ["optionIds", "recaptchaToken", "body", "type"];

interface Props {
  settings: Settings;
  options: ConferenceOption[];
  onAccepted: (confirmation: Confirmation) => void;
}

/** The external participant and student forms (contract `ui-form.md`). */
export function RegistrationForm({ settings, options, onAccepted }: Props) {
  const [type, setType] = useState<RegistrationType>("EXTERNAL");
  const [values, setValues] = useState<Partial<Record<TextField, string>>>({});
  const [selected, setSelected] = useState<string[]>([]);
  const [consentGiven, setConsentGiven] = useState(false);
  const [token, setToken] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formErrors, setFormErrors] = useState<string[]>([]);
  const [failed, setFailed] = useState(false);
  const [sending, setSending] = useState(false);
  const [captchaReset, setCaptchaReset] = useState(0);

  const available = options.filter((o) => o.registrationTypes.includes(type));

  function chooseType(next: RegistrationType) {
    setType(next);
    setSelected((current) =>
      current.filter((id) =>
        options.find((o) => o.id === id)?.registrationTypes.includes(next),
      ),
    );
    setConsentGiven(false);
    setErrors({});
    setFormErrors([]);
  }

  function toggle(id: string, checked: boolean) {
    setSelected((current) =>
      checked ? [...current, id] : current.filter((x) => x !== id),
    );
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (sending) return;
    setFailed(false);
    setFormErrors([]);
    const clientErrors = validate(type, values, consentGiven, token);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) return;

    const request = {
      type,
      ...Object.fromEntries(
        FIELDS[type].map((f) => [f.name, (values[f.name] ?? "").trim()]),
      ),
      optionIds: selected,
      consentGiven,
      recaptchaToken: token,
    } as Parameters<typeof submitRegistration>[0];
    setSending(true);
    const result = await submitRegistration(request);
    setSending(false);
    if (result.kind === "accepted") {
      onAccepted(result.confirmation);
      return;
    }
    if (!settings.recaptcha.testMode) setCaptchaReset((n) => n + 1);
    if (result.kind === "rejected") {
      const byField: Record<string, string> = {};
      const general: string[] = [];
      for (const e of result.fieldErrors) {
        if (FORM_LEVEL_FIELDS.includes(e.field)) general.push(e.message);
        else byField[e.field] = e.message;
      }
      setErrors(byField);
      setFormErrors(general);
    } else {
      setFailed(true);
    }
  }

  const errorId = (field: string) =>
    errors[field] ? `${field}-error` : undefined;

  return (
    <form noValidate onSubmit={onSubmit}>
      <fieldset>
        <legend>Registration type</legend>
        <label className="check">
          <input
            type="radio"
            name="registrationType"
            checked={type === "EXTERNAL"}
            onChange={() => chooseType("EXTERNAL")}
          />
          External participant
        </label>
        <label className="check">
          <input
            type="radio"
            name="registrationType"
            checked={type === "STUDENT"}
            onChange={() => chooseType("STUDENT")}
          />
          Student
        </label>
      </fieldset>

      {FIELDS[type].map((field) => (
        <div className="field" key={field.name}>
          <label htmlFor={field.name}>{field.label}</label>
          <input
            id={field.name}
            name={field.name}
            type={field.type ?? "text"}
            value={values[field.name] ?? ""}
            aria-invalid={errors[field.name] ? true : undefined}
            aria-describedby={errorId(field.name)}
            onChange={(e) =>
              setValues((v) => ({ ...v, [field.name]: e.target.value }))
            }
          />
          {errors[field.name] && (
            <p id={`${field.name}-error`} role="alert" className="error">
              {errors[field.name]}
            </p>
          )}
        </div>
      ))}

      {CATEGORIES.map(({ category, heading }) => {
        const inCategory = available.filter((o) => o.category === category);
        if (inCategory.length === 0) return null;
        return (
          <fieldset key={category}>
            <legend>
              <h2>{heading}</h2>
            </legend>
            {inCategory.map((option) => (
              <label className="check" key={option.id}>
                <input
                  type="checkbox"
                  checked={selected.includes(option.id)}
                  onChange={(e) => toggle(option.id, e.target.checked)}
                />
                {option.name}
              </label>
            ))}
          </fieldset>
        );
      })}

      <div className="field">
        <label className="check">
          <input
            type="checkbox"
            checked={consentGiven}
            aria-describedby={errorId("consentGiven")}
            onChange={(e) => setConsentGiven(e.target.checked)}
          />
          {settings.consent.text}
        </label>
        {errors.consentGiven && (
          <p id="consentGiven-error" role="alert" className="error">
            {errors.consentGiven}
          </p>
        )}
      </div>

      <div className="field">
        <Recaptcha
          testMode={settings.recaptcha.testMode}
          siteKey={settings.recaptcha.siteKey}
          token={token}
          onToken={setToken}
          resetKey={captchaReset}
          describedBy={errorId("recaptchaToken")}
        />
        {errors.recaptchaToken && (
          <p id="recaptchaToken-error" role="alert" className="error">
            {errors.recaptchaToken}
          </p>
        )}
      </div>

      {formErrors.map((message) => (
        <p key={message} role="alert" className="error">
          {message}
        </p>
      ))}
      {failed && (
        <p role="alert" className="error">
          {FAILURE}
        </p>
      )}

      <button type="submit" disabled={sending}>
        {sending ? "Sending…" : "Register"}
      </button>
    </form>
  );
}
