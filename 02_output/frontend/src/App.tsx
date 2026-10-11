import { useEffect, useState } from "react";
import { fetchFormConfig, type FormConfig, type RegistrationResponse } from "./api";
import { Confirmation } from "./Confirmation";
import { RegistrationForm } from "./RegistrationForm";
import "./App.css";

type Load = { state: "loading" } | { state: "ready"; config: FormConfig } | { state: "error" };

export function App() {
  const [load, setLoad] = useState<Load>({ state: "loading" });
  const [accepted, setAccepted] = useState<RegistrationResponse | null>(null);

  useEffect(() => {
    let active = true;
    fetchFormConfig()
      .then((config) => active && setLoad({ state: "ready", config }))
      .catch(() => active && setLoad({ state: "error" }));
    return () => {
      active = false;
    };
  }, []);

  const title =
    load.state === "ready"
      ? `Conference registration – ${load.config.conferenceName}`
      : "Conference registration";

  return (
    <main className="page">
      <h1>{title}</h1>
      {load.state === "loading" && <p>Loading…</p>}
      {load.state === "error" && (
        <p role="alert" className="alert">
          The registration form is currently unavailable. Please try again later.
        </p>
      )}
      {load.state === "ready" &&
        (accepted ? (
          <Confirmation registration={accepted} />
        ) : (
          <RegistrationForm config={load.config} onAccepted={setAccepted} />
        ))}
    </main>
  );
}
