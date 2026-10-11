import { useEffect, useRef, useState, type FormEvent } from "react";
import {
  submitRegistration,
  type Category,
  type FormConfig,
  type RegistrationRequest,
  type RegistrationResponse,
  type RegistrationType,
} from "./api";
import { Checkbox, TextInput } from "./fields";
import { renderRecaptcha, type CaptchaWidget } from "./recaptcha";
import { FIELDS, validate, type TextField } from "./validation";

const LABELS: Record<TextField, string> = {
  firstName: "First name",
  lastName: "Last name",
  email: "Email",
  organization: "Organization / institution",
  studyInstitution: "Study institution",
  studyProgramme: "Study programme",
  studentId: "Student ID",
};

const CATEGORIES: { category: Category; legend: string }[] = [
  { category: "WORKSHOP", legend: "Workshops" },
  { category: "EVENT", legend: "Events" },
  { category: "MEAL", legend: "Meals" },
  { category: "OTHER", legend: "Other activities" },
];

const TEST_TOKEN = "TEST-PASS";
const GENERAL_ERROR = "Registration could not be completed. Please try again later.";
const RATE_LIMITED = "Too many attempts. Please wait a minute and try again.";

/** The registration form (US-001, US-002, US-004; contract ui.md). */
export function RegistrationForm(props: {
  config: FormConfig;
  onAccepted: (registration: RegistrationResponse) => void;
}) {
  const { config } = props;
  const [type, setType] = useState<RegistrationType>("EXTERNAL");
  const [values, setValues] = useState<Partial<Record<TextField, string>>>({});
  const [options, setOptions] = useState<Set<string>>(new Set());
  const [consents, setConsents] = useState<Set<string>>(new Set());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [general, setGeneral] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const captchaBox = useRef<HTMLDivElement>(null);
  const widget = useRef<CaptchaWidget | null>(null);
  const testMode = config.captcha.mode === "test";

  useEffect(() => {
    if (!testMode && captchaBox.current) {
      renderRecaptcha(captchaBox.current, config.captcha.siteKey)
        .then((w) => (widget.current = w))
        .catch(() => setGeneral(GENERAL_ERROR));
    }
  }, [testMode, config.captcha.siteKey]);

  const available = config.options.filter((o) => o.availableTo.includes(type));

  function changeType(next: RegistrationType) {
    setType(next);
    const allowed = new Set(
      config.options.filter((o) => o.availableTo.includes(next)).map((o) => o.id),
    );
    setOptions((current) => new Set([...current].filter((id) => allowed.has(id))));
  }

  function toggle(set: Set<string>, id: string, on: boolean) {
    const next = new Set(set);
    if (on) next.add(id);
    else next.delete(id);
    return next;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setGeneral(null);
    const captchaToken = testMode ? TEST_TOKEN : (widget.current?.token() ?? "");
    const clientErrors = validate(type, values, config.consents, consents, captchaToken);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) {
      return;
    }
    const body: RegistrationRequest = {
      type,
      firstName: "",
      lastName: "",
      email: "",
      optionIds: config.options.filter((o) => options.has(o.id)).map((o) => o.id),
      consentIds: config.consents.filter((c) => consents.has(c.id)).map((c) => c.id),
      captchaToken,
    };
    for (const field of FIELDS[type]) {
      body[field] = (values[field] ?? "").trim();
    }
    setSubmitting(true);
    const result = await submitRegistration(body);
    setSubmitting(false);
    if (result.kind === "accepted") {
      props.onAccepted(result.registration);
      return;
    }
    widget.current?.reset();
    if (result.kind === "rejected") {
      const mapped: Record<string, string> = {};
      const known = new Set<string>([...FIELDS[type], "optionIds", "captchaToken"]);
      let unmapped = false;
      for (const e of result.fieldErrors) {
        if (known.has(e.field) || e.field.startsWith("consents.")) mapped[e.field] = e.message;
        else unmapped = true;
      }
      setErrors(mapped);
      if (unmapped || result.fieldErrors.length === 0) setGeneral(GENERAL_ERROR);
    } else {
      setGeneral(result.kind === "rate-limited" ? RATE_LIMITED : GENERAL_ERROR);
    }
  }

  return (
    <form noValidate onSubmit={submit} className="form">
      <div role="radiogroup" aria-labelledby="type-label" className="types">
        <span id="type-label">Registration type</span>
        {(["EXTERNAL", "STUDENT"] as const).map((t) => (
          <label key={t}>
            <input type="radio" name="type" checked={type === t} onChange={() => changeType(t)} />
            {t === "EXTERNAL" ? "External participant" : "Student"}
          </label>
        ))}
      </div>

      {FIELDS[type].map((field) => (
        <TextInput
          key={field}
          name={field}
          label={LABELS[field]}
          type={field === "email" ? "email" : "text"}
          value={values[field] ?? ""}
          error={errors[field]}
          onChange={(v) => setValues((current) => ({ ...current, [field]: v }))}
        />
      ))}

      {CATEGORIES.map(({ category, legend }) => {
        const inCategory = available.filter((o) => o.category === category);
        if (inCategory.length === 0) return null;
        return (
          <fieldset key={category}>
            <legend>{legend}</legend>
            {inCategory.map((o) => (
              <Checkbox
                key={o.id}
                id={`option-${o.id}`}
                label={o.name}
                checked={options.has(o.id)}
                onChange={(on) => setOptions((current) => toggle(current, o.id, on))}
              />
            ))}
          </fieldset>
        );
      })}
      {errors.optionIds && (
        <p id="options-error" className="error">
          {errors.optionIds}
        </p>
      )}

      <fieldset>
        <legend>Consents</legend>
        {config.consents.map((c) => (
          <Checkbox
            key={c.id}
            id={`consent-${c.id}`}
            label={c.text}
            required={c.mandatory}
            checked={consents.has(c.id)}
            error={errors[`consents.${c.id}`]}
            onChange={(on) => setConsents((current) => toggle(current, c.id, on))}
          />
        ))}
      </fieldset>

      {testMode ? (
        <p className="note">Test mode: captcha disabled</p>
      ) : (
        <div id="recaptcha" ref={captchaBox} />
      )}
      {errors.captchaToken && <p className="error">{errors.captchaToken}</p>}

      {general && (
        <p role="alert" className="alert">
          {general}
        </p>
      )}
      <button type="submit" disabled={submitting}>
        Register
      </button>
    </form>
  );
}
