import { fireEvent, screen, within } from "@testing-library/react";
import { PRIVACY_TEXT } from "./fakeApi";

// Drives the registration page through its accessible names (docs/02_contracts/ui.md).

export async function chooseType(label: "External participant" | "Student") {
  fireEvent.click(await screen.findByRole("radio", { name: label }));
}

export function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label, { selector: "input" }), {
    target: { value },
  });
}

export function check(name: string) {
  const box = screen.getByRole("checkbox", { name });
  if (!(box as HTMLInputElement).checked) {
    fireEvent.click(box);
  }
}

export function group(name: string): HTMLElement {
  return screen.getByRole("group", { name });
}

export function checkboxNames(container: HTMLElement): string[] {
  return within(container)
    .getAllByRole("checkbox")
    .map((box) => box.getAttribute("aria-label") ?? labelText(box));
}

function labelText(box: HTMLElement): string {
  const input = box as HTMLInputElement;
  const label = input.labels?.[0];
  return (label?.textContent ?? "").trim();
}

export function textboxNames(): string[] {
  return screen
    .getAllByRole("textbox")
    .map((box) => labelText(box))
    .sort();
}

export function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

/** Fills a complete, valid external form (one workshop and one meal). */
export async function fillValidExternal() {
  await chooseType("External participant");
  type("First name", "Ana");
  type("Last name", "Novak");
  type("Email", "ana.novak@example.com");
  type("Organization / institution", "Acme Testing Ltd");
  check("Secure coding workshop");
  check("Lunch");
  check(PRIVACY_TEXT);
  check("I am not a robot (test mode)");
}
