import type { RegistrationCreated } from "./types";

/** Shown only after the backend accepted the registration (BR-06, AC-004-01). */
export function Confirmation({ registration }: { registration: RegistrationCreated }) {
  return (
    <section className="confirmation" aria-live="polite">
      <h2>Registration confirmed</h2>
      <p>
        Thank you, {registration.firstName} {registration.lastName}. Your registration has been
        received and a confirmation email is on its way.
      </p>
      {registration.options.length > 0 ? (
        <>
          <p>Selected options:</p>
          <ul>
            {registration.options.map((option) => (
              <li key={option.id}>{option.name}</li>
            ))}
          </ul>
        </>
      ) : (
        <p>No options selected.</p>
      )}
    </section>
  );
}
