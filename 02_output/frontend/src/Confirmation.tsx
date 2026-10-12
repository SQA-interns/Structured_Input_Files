import type { Confirmation as ConfirmationData } from "./types";

/** Shown only after the backend accepted the registration (US-004, BR-06). */
export function Confirmation({
  confirmation,
}: {
  confirmation: ConfirmationData;
}) {
  const { firstName, lastName, email, selectedOptions } = confirmation;
  return (
    <section aria-labelledby="confirmation-heading">
      <h2 id="confirmation-heading">Registration received</h2>
      <p>{`Thank you, ${firstName} ${lastName}. A confirmation has been sent to ${email}.`}</p>
      {selectedOptions.length > 0 ? (
        <ul>
          {selectedOptions.map((o) => (
            <li key={o.id}>{o.name}</li>
          ))}
        </ul>
      ) : (
        <p>No optional activities selected.</p>
      )}
    </section>
  );
}
