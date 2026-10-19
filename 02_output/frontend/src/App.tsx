import { useEffect, useState } from "react";
import { fetchFormConfig } from "./api";
import { RegistrationForm } from "./RegistrationForm";
import type { FormConfig } from "./types";

type LoadState = { kind: "loading" } | { kind: "failed" } | { kind: "ready"; config: FormConfig };

export function App() {
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  useEffect(() => {
    fetchFormConfig()
      .then((config) => setState({ kind: "ready", config }))
      .catch(() => setState({ kind: "failed" }));
  }, []);

  if (state.kind === "loading") {
    return <main>Loading…</main>;
  }
  if (state.kind === "failed") {
    return (
      <main>
        <p role="alert">The registration form could not be loaded. Please try again later.</p>
      </main>
    );
  }
  return (
    <main>
      <h1>{state.config.conferenceName}</h1>
      <RegistrationForm config={state.config} onAccepted={() => undefined} />
    </main>
  );
}
