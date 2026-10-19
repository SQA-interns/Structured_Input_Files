import { expect, type Page } from "@playwright/test";
import { conferenceConfig, type ConferenceOption } from "./stack";

/** Opens the page and chooses the registration type (ui.md). */
export async function openForm(page: Page, type: "External participant" | "Student") {
  await page.goto("/");
  await page.getByRole("radio", { name: type, exact: true }).check();
}

export async function fill(page: Page, fields: Record<string, string>) {
  for (const [name, value] of Object.entries(fields)) {
    await page.getByRole("textbox", { name, exact: true }).fill(value);
  }
}

export async function selectOptions(page: Page, options: ConferenceOption[]) {
  for (const option of options) {
    await page.getByRole("checkbox", { name: option.name, exact: true }).check();
  }
}

/** Gives every mandatory consent and passes the test-mode reCAPTCHA. */
export async function consentAndCaptcha(page: Page) {
  for (const consent of conferenceConfig().consents.filter((c) => c.mandatory)) {
    await page.getByRole("checkbox", { name: consent.text, exact: true }).check();
  }
  await page.getByRole("checkbox", { name: "I am not a robot (test mode)", exact: true }).check();
}

export async function register(page: Page) {
  await page.getByRole("button", { name: "Register", exact: true }).click();
}

export async function expectConfirmation(page: Page, texts: string[]) {
  const confirmation = page.getByRole("status");
  await expect(confirmation.getByRole("heading", { name: "Registration received" })).toBeVisible();
  for (const text of texts) {
    await expect(confirmation).toContainText(text);
  }
  await expect(page.getByRole("button", { name: "Register", exact: true })).toHaveCount(0);
}
