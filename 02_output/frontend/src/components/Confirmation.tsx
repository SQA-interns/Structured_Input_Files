import type { RegistrationAccepted } from "../api";

interface Props {
  registration: RegistrationAccepted;
}

/** Shown only after the backend accepted the registration (US-004, BR-06). */
export default function Confirmation({ registration }: Props) {
  return (
    <section role="status" className="confirmation">
      <h2>Registration received</h2>
      <p>Reference: {registration.reference}</p>
      <p>
        {registration.firstName} {registration.lastName}
      </p>
      {registration.options.length === 0 ? (
        <p>No activities selected</p>
      ) : (
        <ul>
          {registration.options.map((option) => (
            <li key={option.id}>{option.name}</li>
          ))}
        </ul>
      )}
    </section>
  );
}
