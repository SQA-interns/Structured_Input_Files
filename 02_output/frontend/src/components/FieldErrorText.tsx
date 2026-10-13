import { errorId } from "../validation";

/** An error rendered directly after its field or group. */
export function FieldErrorText({
  field,
  message,
}: {
  field: string;
  message?: string;
}) {
  if (!message) return null;
  return (
    <p id={errorId(field)} data-testid={errorId(field)} className="field-error">
      {message}
    </p>
  );
}
