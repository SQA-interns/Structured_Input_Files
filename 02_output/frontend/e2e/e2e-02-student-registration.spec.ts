import { expect, test } from "@playwright/test";
import {
  OPTION_GROUPS,
  PRIVACY_CONSENT,
  addressedTo,
  awaitMessages,
  jsonCopy,
  organizerEmails,
  sql,
  submitAndCapture,
  uniqueEmail,
} from "./support/stack";

type Option = { id: string; name: string; category: string; availableTo: string[] };

test("E2E-02 student registers (AC-002-01, AC-002-07, AC-004-01)", async ({ page, request }) => {
  const email = uniqueEmail("e2e-student");
  const configResponse = await request.get("/api/form-config");
  expect(configResponse.status()).toBe(200);
  const config = (await configResponse.json()) as { options: Option[] };
  const studentOptions = config.options.filter((o) => o.availableTo.includes("STUDENT"));
  const externalOnly = config.options.filter((o) => !o.availableTo.includes("STUDENT"));

  await page.goto("/");
  await page.getByRole("radio", { name: "Student" }).check();

  // Only the student fields are shown.
  await expect(page.getByRole("textbox")).toHaveCount(6);
  for (const name of [
    "First name",
    "Last name",
    "Email",
    "Study institution",
    "Study programme",
    "Student ID",
  ]) {
    await expect(page.getByRole("textbox", { name })).toBeVisible();
  }
  await expect(page.getByRole("textbox", { name: "Organization / institution" })).toHaveCount(0);

  // Only the options available to students are shown.
  let shown = 0;
  for (const group of OPTION_GROUPS) {
    shown += await page.getByRole("group", { name: group }).getByRole("checkbox").count();
  }
  expect(shown).toBe(studentOptions.length);
  for (const option of externalOnly) {
    await expect(page.getByRole("checkbox", { name: option.name, exact: true })).toHaveCount(0);
  }

  await page.getByRole("textbox", { name: "First name" }).fill("Luka");
  await page.getByRole("textbox", { name: "Last name" }).fill("Kranjc");
  await page.getByRole("textbox", { name: "Email" }).fill(email);
  await page.getByRole("textbox", { name: "Study institution" }).fill("Univerza E2E");
  await page.getByRole("textbox", { name: "Study programme" }).fill("Informatika");
  await page.getByRole("textbox", { name: "Student ID" }).fill("63209999");
  await page.getByRole("checkbox", { name: studentOptions[0].name, exact: true }).check();
  await page.getByRole("checkbox", { name: PRIVACY_CONSENT }).check();
  await page.getByRole("checkbox", { name: "I am not a robot (test mode)" }).check();

  const { status, body } = await submitAndCapture(page);
  expect(status).toBe(201);
  const id = (body as { id: string }).id;

  await expect(page.getByRole("heading", { name: "Registration confirmed" })).toBeVisible();
  await expect(page.locator("body")).toContainText("Luka Kranjc");
  await expect(page.locator("body")).toContainText(studentOptions[0].name);

  // Stored in the database and as JSON copy.
  expect(
    sql(
      `SELECT type, study_institution, study_programme, student_id, organization IS NULL FROM registration WHERE id = '${id}'`,
    ),
  ).toEqual([["STUDENT", "Univerza E2E", "Informatika", "63209999", "t"]]);
  expect(jsonCopy(id)).not.toBeNull();

  // Both emails arrive.
  expect(await awaitMessages((m) => addressedTo(m, email))).toHaveLength(1);
  const organizers = organizerEmails();
  const notifications = await awaitMessages(
    (m) => m.Subject.includes(id) && organizers.some((o) => addressedTo(m, o)),
    organizers.length,
  );
  expect(notifications.length).toBeGreaterThanOrEqual(organizers.length);
});
