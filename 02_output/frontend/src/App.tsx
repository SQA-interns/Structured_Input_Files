import { useEffect, useState } from "react";
import { loadFormConfig } from "./api";
import { Confirmation } from "./Confirmation";
import { RegistrationForm } from "./RegistrationForm";
import type { FormConfig, RegistrationCreated } from "./types";
import "./styles.css";

export const LOAD_ERROR = "The registration form could not be loaded. Please try again later.";

type State =
  | { kind: "loading" }
  | { kind: "error" }
  | { kind: "form"; config: FormConfig }
  | { kind: "confirmed"; config: FormConfig; registration: RegistrationCreated };

/** Loading → form → confirmation; a failed load shows a general message (S11.2). */
export function App() {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let active = true;
    loadFormConfig()
      .then((config) => {
        if (active) {
          setState({ kind: "form", config });
        }
      })
      .catch(() => {
        if (active) {
          setState({ kind: "error" });
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const title =
    state.kind === "form" || state.kind === "confirmed"
      ? `${state.config.conferenceName} registration`
      : "Conference registration";

  return (
    <main>
      <h1>{title}</h1>
      {state.kind === "loading" && <p>Loading…</p>}
      {state.kind === "error" && (
        <p role="alert" className="error general">
          {LOAD_ERROR}
        </p>
      )}
      {state.kind === "form" && (
        <RegistrationForm
          config={state.config}
          onCreated={(registration) =>
            setState({ kind: "confirmed", config: state.config, registration })
          }
        />
      )}
      {state.kind === "confirmed" && <Confirmation registration={state.registration} />}
    </main>
  );
}
