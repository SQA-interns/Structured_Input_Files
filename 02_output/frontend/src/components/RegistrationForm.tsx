import { useCallback, useState, type FormEvent } from "react";
import {
  submitRegistration,
  type FormConfig,
  type OptionCategory,
  type RegistrationAccepted,
  type RegistrationRequest,
  type RegistrationType,
} from "../api";
import { FIELDS, validate, type FormValues, type TextField } from "../validation";
import Captcha from "./Captcha";

export const GENERAL_ERROR = "Your registration could not be processed. Please try again later.";
const TOO_MANY_REQUESTS = "Too many attempts. Please wait a few minutes and try again.";

const CATEGORIES: { category: OptionCategory; legend: string }[] = [
  { category: "WORKSHOP", legend: "Workshops" },
  { category: "EVENT", legend: "Events" },
  { category: "MEAL", legend: "Meals" },
  { category: "OTHER", legend: "Other activities" },
];

const EMPTY_TEXT: Record<TextField, string> = {
  firstName: "",
  lastName: "",
  email: "",
  organization: "",
  studyInstitution: "",
  studyProgramme: "",
  studentId: "",
};

interface Props {
  config: FormConfig;
  type: RegistrationType;
  onAccepted: (registration: RegistrationAccepted) => void;
}

/** The registration form of one type (US-001, US-002, NFR-03). */
export default function RegistrationForm({ config, type, onAccepted }: Props) {
  const [text, setText] = useState<Record<TextField, string>>(EMPTY_TEXT);
  const [optionIds, setOptionIds] = useState<string[]>([]);
  const [consentIds, setConsentIds] = useState<string[]>([]);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [captchaReset, setCaptchaReset] = useState(0);
  const onToken = useCallback((token: string | null) => setCaptchaToken(token), []);

  const fields = FIELDS[type];
  const options = config.options.filter((o) => o.audience.includes(type));

  const toggle = (list: string[], id: string, on: boolean) =>
    on ? [...list, id] : list.filter((x) => x !== id);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setGeneralError(null);
    const values: FormValues = { text, consentIds, captchaToken };
    const found = validate(values, type, config.consents);
    setErrors(found);
    if (Object.keys(found).length > 0 || !captchaToken) {
      return;
    }
    const body: RegistrationRequest = {
      type,
      ...Object.fromEntries(fields.map((f) => [f.id, text[f.id]])),
      firstName: text.firstName,
      lastName: text.lastName,
      email: text.email,
      optionIds: options.filter((o) => optionIds.includes(o.id)).map((o) => o.id),
      consentIds: config.consents.filter((c) => consentIds.includes(c.id)).map((c) => c.id),
      captchaToken,
    };
    setSubmitting(true);
    const result = await submitRegistration(body);
    setSubmitting(false);
    if (result.kind === "accepted") {
      onAccepted(result.registration);
      return;
    }
    setCaptchaReset((n) => n + 1);
    if (result.kind === "rejected") {
      const shown = new Set<string>([
        ...fields.map((f) => f.id),
        "optionIds",
        "captchaToken",
        ...config.consents.map((c) => `consents.${c.id}`),
      ]);
      const backendErrors: Record<string, string> = {};
      for (const error of result.fieldErrors) {
        backendErrors[error.field] = error.message;
      }
      setErrors(backendErrors);
      if (result.fieldErrors.some((e) => !shown.has(e.field))) {
        setGeneralError(GENERAL_ERROR);
      }
      return;
    }
    setGeneralError(result.status === 429 ? TOO_MANY_REQUESTS : GENERAL_ERROR);
  }

  const errorText = (id: string) =>
    errors[id] ? (
      <p id={`${id}-error`} role="alert" className="field-error">
        {errors[id]}
      </p>
    ) : null;

  return (
    <form aria-label="Registration form" noValidate onSubmit={handleSubmit}>
      {fields.map((field) => (
        <div className="field" key={field.id}>
          <label htmlFor={field.id}>{field.label}</label>
          <input
            id={field.id}
            name={field.id}
            type={field.type}
            autoComplete={field.type === "email" ? "email" : "off"}
            value={text[field.id]}
            aria-invalid={errors[field.id] ? true : undefined}
            aria-describedby={errors[field.id] ? `${field.id}-error` : undefined}
            onChange={(event) => setText({ ...text, [field.id]: event.target.value })}
          />
          {errorText(field.id)}
        </div>
      ))}

      {CATEGORIES.map(({ category, legend }) => {
        const inCategory = options.filter((o) => o.category === category);
        if (inCategory.length === 0) {
          return null;
        }
        return (
          <fieldset key={category}>
            <legend>{legend}</legend>
            {inCategory.map((option) => (
              <label key={option.id} className="choice">
                <input
                  type="checkbox"
                  value={option.id}
                  checked={optionIds.includes(option.id)}
                  onChange={(event) =>
                    setOptionIds(toggle(optionIds, option.id, event.target.checked))
                  }
                />{" "}
                {option.name}
              </label>
            ))}
          </fieldset>
        );
      })}
      {errorText("optionIds")}

      <fieldset>
        <legend>Consents</legend>
        {config.consents.map((consent) => {
          const id = `consents.${consent.id}`;
          return (
            <div key={consent.id}>
              <label className="choice">
                <input
                  type="checkbox"
                  value={consent.id}
                  checked={consentIds.includes(consent.id)}
                  aria-describedby={errors[id] ? `${id}-error` : undefined}
                  onChange={(event) =>
                    setConsentIds(toggle(consentIds, consent.id, event.target.checked))
                  }
                />{" "}
                {consent.text}
              </label>
              {errorText(id)}
            </div>
          );
        })}
      </fieldset>

      <Captcha
        testMode={config.recaptcha.testMode}
        siteKey={config.recaptcha.siteKey}
        resetKey={captchaReset}
        error={errors.captchaToken}
        onToken={onToken}
      />

      {generalError && (
        <div id="form-error" role="alert" className="form-error">
          {generalError}
        </div>
      )}

      <button type="submit" disabled={submitting}>
        Register
      </button>
    </form>
  );
}
