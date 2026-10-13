import { useEffect, useState } from "react";
import { fetchFormConfig } from "./api";
import { Confirmation } from "./Confirmation";
import { RegistrationForm, type Registered } from "./RegistrationForm";
import type { FormConfig } from "./types";

export const CONFIG_ERROR =
  "The registration form is currently unavailable. Please try again later.";

type State =
  | { kind: "loading" }
  | { kind: "unavailable" }
  | { kind: "form"; config: FormConfig }
  | { kind: "registered"; config: FormConfig; registered: Registered };

export default function App() {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let active = true;
    fetchFormConfig()
      .then((config) => active && setState({ kind: "form", config }))
      .catch(() => active && setState({ kind: "unavailable" }));
    return () => {
      active = false;
    };
  }, []);

  if (state.kind === "loading") {
    return <main>Loading…</main>;
  }
  if (state.kind === "unavailable") {
    return (
      <main>
        <p role="alert">{CONFIG_ERROR}</p>
      </main>
    );
  }
  return (
    <main>
      <h1>{state.config.conferenceName}</h1>
      {state.kind === "registered" ? (
        <Confirmation registered={state.registered} />
      ) : (
        <RegistrationForm
          config={state.config}
          onRegistered={(registered) =>
            setState({ kind: "registered", config: state.config, registered })
          }
        />
      )}
    </main>
  );
}
