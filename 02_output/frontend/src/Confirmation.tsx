import type { Registered } from "./RegistrationForm";

// Shown only after the backend accepted the registration (BR-06, AC-004-01).

export function Confirmation({ registered }: { registered: Registered }) {
  return (
    <section role="status" aria-labelledby="confirmation-heading">
      <h2 id="confirmation-heading">Registration received</h2>
      <p>
        Thank you, {registered.firstName} {registered.lastName}.
      </p>
      {registered.optionNames.length === 0 ? (
        <p>No options selected.</p>
      ) : (
        <ul aria-label="Selected options">
          {registered.optionNames.map((name) => (
            <li key={name}>{name}</li>
          ))}
        </ul>
      )}
    </section>
  );
}
