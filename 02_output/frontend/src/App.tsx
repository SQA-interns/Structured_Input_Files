import { useEffect, useState } from "react";
import { loadFormConfig } from "./api";
import { Confirmation } from "./components/Confirmation";
import { RegistrationForm } from "./components/RegistrationForm";
import type { FormConfig, RegistrationAccepted } from "./types";
import "./styles.css";

type ConfigState =
  | { status: "loading" }
  | { status: "failed" }
  | { status: "ready"; config: FormConfig };

/** The registration page (`ui-contract.md`). */
export function App() {
  const [state, setState] = useState<ConfigState>({ status: "loading" });
  const [accepted, setAccepted] = useState<RegistrationAccepted | null>(null);

  useEffect(() => {
    let active = true;
    loadFormConfig()
      .then((config) => {
        if (active) setState({ status: "ready", config });
      })
      .catch(() => {
        if (active) setState({ status: "failed" });
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <main>
      <h1>Conference registration</h1>
      {state.status === "loading" && <p>Loading…</p>}
      {state.status === "failed" && (
        <p role="alert" className="general-error">
          The registration form is currently unavailable. Please try again
          later.
        </p>
      )}
      {state.status === "ready" && (
        <>
          <p className="conference-name">{state.config.conferenceName}</p>
          {accepted ? (
            <Confirmation registration={accepted} />
          ) : (
            <RegistrationForm config={state.config} onAccepted={setAccepted} />
          )}
        </>
      )}
    </main>
  );
}
