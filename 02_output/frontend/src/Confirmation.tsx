import type { AcceptedRegistration } from "./api";

/** The in-application confirmation (AC-004-01, BR-06). */
export function Confirmation({ registration }: { registration: AcceptedRegistration }) {
  return (
    <section role="status">
      <h2>Registration received</h2>
      <p>
        Thank you, {registration.firstName} {registration.lastName}. Your registration was received
        and a confirmation email is on its way.
      </p>
      {registration.options.length > 0 ? (
        <ul>
          {registration.options.map((option) => (
            <li key={option.id}>{option.name}</li>
          ))}
        </ul>
      ) : (
        <p>No options selected.</p>
      )}
    </section>
  );
}
