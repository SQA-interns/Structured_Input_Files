import { expect, test } from "@playwright/test";
import {
  awaitMessages,
  confirmConsentAndCaptcha,
  fillFields,
  literal,
  sql,
  unique,
} from "./support";

test("E2E-02 student registers (AC-002-01, AC-002-07, AC-004-01)", async ({
  page,
}) => {
  const run = unique();
  const firstName = "Luka";
  const lastName = `Student${run}`;
  const email = `luka.${run}@e2e.example`;

  await page.goto("/");
  await page.getByRole("radio", { name: "Student" }).check();

  // Only the student fields and the options available to students (shipped configuration)
  await expect(
    page.getByLabel("Organization / institution", { exact: true }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("checkbox", { name: "Workshop: Industry partnerships" }),
  ).toHaveCount(0);
  await expect(page.getByRole("checkbox", { name: "Gala dinner" })).toHaveCount(
    0,
  );
  await expect(
    page.getByRole("checkbox", { name: "Student mentoring session" }),
  ).toBeVisible();

  await fillFields(page, {
    "First name": firstName,
    "Last name": lastName,
    Email: email,
    "Study institution": "Univerza v Mariboru",
    "Study programme": "Informatika",
    "Student ID": `S${run}`,
  });
  await page
    .getByRole("checkbox", { name: "Workshop: Cloud-native architecture" })
    .check();
  await page
    .getByRole("checkbox", { name: "Student mentoring session" })
    .check();
  await confirmConsentAndCaptcha(page);
  await page.getByRole("button", { name: "Register" }).click();

  await expect(
    page.getByRole("heading", { level: 2, name: "Registration received" }),
  ).toBeVisible();
  await expect(
    page.getByText(`Thank you, ${firstName} ${lastName}.`),
  ).toBeVisible();
  const selected = page.getByRole("list", { name: "Selected options" });
  await expect(
    selected.getByText("Student mentoring session", { exact: true }),
  ).toBeVisible();

  const rows = sql(
    `SELECT type, study_institution, study_programme, student_id, organization FROM registration WHERE email = ${literal(email)}`,
  );
  expect(rows).toEqual([
    ["STUDENT", "Univerza v Mariboru", "Informatika", `S${run}`, ""],
  ]);

  const messages = await awaitMessages(lastName, 2);
  expect(
    messages.filter((m) => m.To.some((t) => t.Address === email)),
  ).toHaveLength(1);
  expect(
    messages.filter((m) => m.Subject.startsWith("New registration")).length,
  ).toBeGreaterThanOrEqual(1);
});
