import { useEffect, useState } from "react";
import {
  fetchFormConfig,
  type FormConfig,
  type RegistrationAccepted,
  type RegistrationType,
} from "./api";
import RegistrationForm from "./components/RegistrationForm";
import "./App.css";

const LOAD_ERROR = "The registration form could not be loaded. Please try again later.";

const TYPES: { value: RegistrationType; label: string }[] = [
  { value: "EXTERNAL", label: "External participant" },
  { value: "STUDENT", label: "Student" },
];

export default function App() {
  const [config, setConfig] = useState<FormConfig | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [type, setType] = useState<RegistrationType | null>(null);
  const [accepted, setAccepted] = useState<RegistrationAccepted | null>(null);

  useEffect(() => {
    let active = true;
    fetchFormConfig()
      .then((loaded) => active && setConfig(loaded))
      .catch(() => active && setLoadFailed(true));
    return () => {
      active = false;
    };
  }, []);

  const title = config ? `${config.conferenceName} – registration` : "Conference registration";

  return (
    <main>
      <h1>{title}</h1>
      {loadFailed && (
        <div role="alert" className="form-error">
          {LOAD_ERROR}
        </div>
      )}
      {!config && !loadFailed && <p>Loading…</p>}
      {config && !accepted && (
        <>
          <div role="radiogroup" aria-labelledby="type-label" className="type-choice">
            <span id="type-label">Registration type</span>
            {TYPES.map((t) => (
              <label key={t.value} className="choice">
                <input
                  type="radio"
                  name="type"
                  value={t.value}
                  checked={type === t.value}
                  onChange={() => setType(t.value)}
                />{" "}
                {t.label}
              </label>
            ))}
          </div>
          {type && (
            <RegistrationForm key={type} config={config} type={type} onAccepted={setAccepted} />
          )}
        </>
      )}
    </main>
  );
}
