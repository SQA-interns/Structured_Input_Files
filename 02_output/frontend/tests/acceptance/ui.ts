import { createElement } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { App } from "../../src/App";
import { formConfig } from "./support";

export const PRIVACY = formConfig.consents[0].text;
export const PHOTO = formConfig.consents[1].text;

/** Renders the app and waits until the form configuration is shown. */
export async function renderLoaded() {
  render(createElement(App));
  await screen.findByRole("heading", { level: 1, name: /Konferenca 2026/ });
}

export function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

export function fillValidExternal() {
  type("First name", "Ana");
  type("Last name", "Novak");
  type("Email", "ana.novak@example.test");
  type("Organization / institution", "Institut Jožef Stefan");
  fireEvent.click(screen.getByRole("checkbox", { name: "Workshop: AI v praksi" }));
  fireEvent.click(screen.getByRole("checkbox", { name: PRIVACY }));
}
