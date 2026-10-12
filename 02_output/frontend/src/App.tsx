import { useEffect, useState } from "react";
import { fetchOptions, fetchSettings } from "./api";
import { Confirmation } from "./Confirmation";
import { RegistrationForm } from "./RegistrationForm";
import type {
  Confirmation as ConfirmationData,
  ConferenceOption,
  Settings,
} from "./types";

type Load =
  | { state: "loading" }
  | { state: "error" }
  | { state: "ready"; settings: Settings; options: ConferenceOption[] };

export function App() {
  const [load, setLoad] = useState<Load>({ state: "loading" });
  const [confirmation, setConfirmation] = useState<ConfirmationData | null>(
    null,
  );

  useEffect(() => {
    let active = true;
    Promise.all([fetchSettings(), fetchOptions()])
      .then(
        ([settings, options]) =>
          active && setLoad({ state: "ready", settings, options }),
      )
      .catch(() => active && setLoad({ state: "error" }));
    return () => {
      active = false;
    };
  }, []);

  if (load.state === "loading") return <main aria-busy="true">Loading…</main>;
  if (load.state === "error") {
    return (
      <main>
        <p role="alert">
          The registration form could not be loaded. Please try again later.
        </p>
      </main>
    );
  }
  return (
    <main>
      <h1>{load.settings.conferenceName} – Registration</h1>
      {confirmation ? (
        <Confirmation confirmation={confirmation} />
      ) : (
        <RegistrationForm
          settings={load.settings}
          options={load.options}
          onAccepted={setConfirmation}
        />
      )}
    </main>
  );
}
