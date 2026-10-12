import { fireEvent, screen } from "@testing-library/react";
import { CONSENT_TEXT } from "./fakeBackend";

export async function waitForForm() {
  return screen.findByRole("button", { name: "Register" });
}

export function type(label: string, value: string) {
  fireEvent.change(screen.getByRole("textbox", { name: label }), {
    target: { value },
  });
}

export function check(name: string) {
  const box = screen.getByRole("checkbox", { name });
  if (!(box as HTMLInputElement).checked) fireEvent.click(box);
}

export function chooseStudent() {
  fireEvent.click(screen.getByRole("radio", { name: "Student" }));
}

export function fillValidExternal() {
  type("First name", "Ana");
  type("Last name", "Novak");
  type("Email", "ana.novak@example.si");
  type("Organization / institution", "Acme d.o.o.");
  check("Workshop A: Testing");
  check(CONSENT_TEXT);
  check("I am not a robot (test mode)");
}

export function fillValidStudent() {
  chooseStudent();
  type("First name", "Luka");
  type("Last name", "Kranjc");
  type("Email", "luka.kranjc@example.si");
  type("Study institution", "University of Ljubljana");
  type("Study programme", "Computer Science");
  type("Student ID", "63210001");
  check("Career fair");
  check(CONSENT_TEXT);
  check("I am not a robot (test mode)");
}

export function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

/** Asserts the visible text fields are exactly these, by accessible name. */
export function expectExactlyTextFields(labels: string[]) {
  for (const label of labels) screen.getByRole("textbox", { name: label });
  if (screen.getAllByRole("textbox").length !== labels.length) {
    throw new Error(
      `expected exactly ${labels.length} text fields, found ${screen.getAllByRole("textbox").length}`,
    );
  }
}
