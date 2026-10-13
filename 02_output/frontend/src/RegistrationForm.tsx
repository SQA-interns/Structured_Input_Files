import { useState, type FormEvent } from "react";
import { submitRegistration } from "./api";
import { Recaptcha } from "./Recaptcha";
import type {
  ConferenceOption,
  FieldErrors,
  FormConfig,
  OptionCategory,
  RegistrationPayload,
  RegistrationType,
  TextField,
} from "./types";
import { FIELDS, validate } from "./validation";

// The registration form (contract ui.md; US-001, US-002, US-004, NFR-03).

export const GENERAL_ERROR =
  "The registration could not be completed. Please try again later.";

const LABELS: Record<TextField, string> = {
  firstName: "First name",
  lastName: "Last name",
  email: "Email",
  organization: "Organization / institution",
  studyInstitution: "Study institution",
  studyProgramme: "Study programme",
  studentId: "Student ID",
};

const GROUPS: { category: OptionCategory; legend: string }[] = [
  { category: "WORKSHOP", legend: "Workshops" },
  { category: "EVENT", legend: "Events" },
  { category: "MEAL", legend: "Meals" },
  { category: "OTHER", legend: "Other activities" },
];

export interface Registered {
  firstName: string;
  lastName: string;
  optionNames: string[];
}

function ErrorText({ field, errors }: { field: string; errors: FieldErrors }) {
  const message = errors[field];
  return message ? (
    <p id={`${field}-error`} className="error">
      {message}
    </p>
  ) : null;
}

function describe(field: string, errors: FieldErrors) {
  return errors[field]
    ? { "aria-invalid": true, "aria-describedby": `${field}-error` }
    : {};
}

export function RegistrationForm({
  config,
  onRegistered,
}: {
  config: FormConfig;
  onRegistered: (registered: Registered) => void;
}) {
  const [type, setType] = useState<RegistrationType | null>(null);
  const [values, setValues] = useState<Partial<Record<TextField, string>>>({});
  const [optionIds, setOptionIds] = useState<string[]>([]);
  const [consents, setConsents] = useState<string[]>([]);
  const [token, setToken] = useState("");
  const [resetCount, setResetCount] = useState(0);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [failed, setFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const available = (option: ConferenceOption) =>
    type !== null && option.availableTo.includes(type);
  const chosenOptions = config.options.filter(
    (o) => available(o) && optionIds.includes(o.id),
  );

  const chooseType = (next: RegistrationType) => {
    setType(next);
    setOptionIds((ids) =>
      ids.filter((id) =>
        config.options.some((o) => o.id === id && o.availableTo.includes(next)),
      ),
    );
  };
  const toggle = (list: string[], id: string, on: boolean) =>
    on ? [...list, id] : list.filter((x) => x !== id);

  const failSubmission = () => {
    setToken("");
    setResetCount((n) => n + 1);
  };

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const clientErrors = validate(
      { type, values, consents, recaptchaToken: token },
      config.consents,
    );
    setFailed(false);
    if (Object.keys(clientErrors).length > 0 || type === null) {
      setErrors(clientErrors);
      return;
    }
    const payload: RegistrationPayload = {
      type,
      ...Object.fromEntries(
        FIELDS[type].map((f) => [f, (values[f] ?? "").trim()]),
      ),
      optionIds: chosenOptions.map((o) => o.id),
      consents,
      recaptchaToken: token,
    } as RegistrationPayload;
    setSubmitting(true);
    const result = await submitRegistration(payload);
    setSubmitting(false);
    if (result.kind === "created") {
      onRegistered({
        firstName: payload.firstName,
        lastName: payload.lastName,
        optionNames: chosenOptions.map((o) => o.name),
      });
      return;
    }
    failSubmission();
    if (result.kind === "rejected") {
      setErrors(result.fieldErrors);
    } else {
      setErrors({});
      setFailed(true);
    }
  };

  return (
    <form aria-label="Registration form" noValidate onSubmit={onSubmit}>
      <fieldset {...describe("type", errors)}>
        <legend>Registration type</legend>
        {(["EXTERNAL", "STUDENT"] as const).map((t) => (
          <div key={t} className="field">
            <input
              id={`type-${t}`}
              type="radio"
              name="type"
              value={t}
              checked={type === t}
              onChange={() => chooseType(t)}
            />
            <label htmlFor={`type-${t}`}>
              {t === "EXTERNAL" ? "External participant" : "Student"}
            </label>
          </div>
        ))}
        <ErrorText field="type" errors={errors} />
      </fieldset>

      {type !== null && (
        <>
          {FIELDS[type].map((field) => (
            <div key={field} className="field">
              <label htmlFor={field}>{LABELS[field]}</label>
              <input
                id={field}
                type="text"
                value={values[field] ?? ""}
                onChange={(e) =>
                  setValues((v) => ({ ...v, [field]: e.target.value }))
                }
                {...describe(field, errors)}
              />
              <ErrorText field={field} errors={errors} />
            </div>
          ))}

          {GROUPS.map(({ category, legend }) => {
            const options = config.options.filter(
              (o) => o.category === category && available(o),
            );
            return options.length === 0 ? null : (
              <fieldset key={category}>
                <legend>{legend}</legend>
                {options.map((option) => (
                  <div key={option.id} className="field">
                    <input
                      id={`option-${option.id}`}
                      type="checkbox"
                      checked={optionIds.includes(option.id)}
                      onChange={(e) =>
                        setOptionIds((ids) =>
                          toggle(ids, option.id, e.target.checked),
                        )
                      }
                      {...describe("optionIds", errors)}
                    />
                    <label htmlFor={`option-${option.id}`}>{option.name}</label>
                  </div>
                ))}
              </fieldset>
            );
          })}
          <ErrorText field="optionIds" errors={errors} />

          {config.consents.map((consent) => (
            <div key={consent.id} className="field">
              <input
                id={`consent-${consent.id}`}
                type="checkbox"
                checked={consents.includes(consent.id)}
                onChange={(e) =>
                  setConsents((ids) =>
                    toggle(ids, consent.id, e.target.checked),
                  )
                }
                {...describe(`consents.${consent.id}`, errors)}
              />
              <label htmlFor={`consent-${consent.id}`}>{consent.text}</label>
              <ErrorText field={`consents.${consent.id}`} errors={errors} />
            </div>
          ))}
          <ErrorText field="consents" errors={errors} />

          <Recaptcha
            testMode={config.recaptchaTestMode}
            siteKey={config.recaptchaSiteKey}
            token={token}
            resetCount={resetCount}
            invalid={Boolean(errors.recaptchaToken)}
            onToken={setToken}
          />
          <ErrorText field="recaptchaToken" errors={errors} />
        </>
      )}

      {failed && <p role="alert">{GENERAL_ERROR}</p>}
      <button type="submit" disabled={submitting}>
        Register
      </button>
    </form>
  );
}
