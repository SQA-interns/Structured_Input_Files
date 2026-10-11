import type { RegistrationResponse } from "./api";

/** Shown only after the backend accepted the registration (BR-06, AC-004-01). */
export function Confirmation({ registration }: { registration: RegistrationResponse }) {
  return (
    <section className="confirmation">
      <h2>Registration received</h2>
      <p>
        Thank you, {registration.firstName} {registration.lastName}. Your registration has been
        received.
      </p>
      {registration.options.length === 0 ? (
        <p>No options selected.</p>
      ) : (
        <ul>
          {registration.options.map((o) => (
            <li key={o.id}>{o.name}</li>
          ))}
        </ul>
      )}
      <p>Registration ID: {registration.id}</p>
    </section>
  );
}
