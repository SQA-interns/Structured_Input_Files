import { fireEvent, screen } from "@testing-library/react";
import { PRIVACY_TEXT } from "./fakeBackend";

/** Waits for the form configuration and chooses a registration type (ui.md). */
export async function chooseType(type: "External participant" | "Student") {
  const radio = await screen.findByRole("radio", { name: type });
  fireEvent.click(radio);
}

export function textbox(name: string): HTMLInputElement {
  return screen.getByRole("textbox", { name }) as HTMLInputElement;
}

export function type(name: string, value: string) {
  fireEvent.change(textbox(name), { target: { value } });
}

export function check(name: string) {
  fireEvent.click(screen.getByRole("checkbox", { name }));
}

/** Fills a valid external submission: fields, two options, the mandatory consent, captcha. */
export async function fillValidExternal() {
  await chooseType("External participant");
  type("First name", "Ana");
  type("Last name", "Novak");
  type("Email", "ana.novak@example.com");
  type("Organization / institution", "Institut Jozef Stefan");
  check("Workshop: AI in practice");
  check("Lunch");
  check(PRIVACY_TEXT);
  check("I am not a robot (test mode)");
}

export function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}
