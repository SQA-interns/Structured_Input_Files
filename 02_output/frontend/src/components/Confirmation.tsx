import type { RegistrationAccepted } from "../types";

/** Shown only after the backend accepted the registration (BR-06, AC-004-01). */
export function Confirmation({
  registration,
}: {
  registration: RegistrationAccepted;
}) {
  return (
    <section>
      <h2>Registration received</h2>
      <p>
        Thank you, {registration.firstName} {registration.lastName}.
      </p>
      {registration.options.length > 0 ? (
        <ul aria-label="Selected options">
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
