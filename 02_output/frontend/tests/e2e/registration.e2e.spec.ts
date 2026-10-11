import { expect, test } from "@playwright/test";
import { API_URL, awaitMail, organizerAuthHeader, zipText } from "./support";

const unique = () => Math.random().toString(36).slice(2, 10);

test("AC-004-01 / NFR-01: a student with Slovenian characters registers end to end", async ({
  page,
  request,
}) => {
  const u = unique();
  const firstName = "Žiga";
  const lastName = `Šuštaršič-Čeh ${u}`;
  const email = `ziga.${u}@example.test`;
  const institution = "Univerza v Ljubljani, Fakulteta za računalništvo in informatiko";

  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Conference registration");
  await page.getByRole("radio", { name: "Student" }).check();
  await page.getByLabel("First name").fill(firstName);
  await page.getByLabel("Last name").fill(lastName);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Study institution").fill(institution);
  await page.getByLabel("Study programme").fill("Računalništvo in matematika");
  await page.getByLabel("Student ID").fill(`6320${u.slice(0, 4)}`);
  const firstOption = page.getByRole("group", { name: "Workshops" }).getByRole("checkbox").first();
  const optionName = (
    (await firstOption.evaluate((el) => (el as HTMLInputElement).labels?.[0]?.textContent)) ?? ""
  ).trim();
  await firstOption.check();
  for (const consent of await page
    .getByRole("group", { name: "Consents" })
    .getByRole("checkbox")
    .all()) {
    await consent.check();
  }
  await page.getByRole("button", { name: "Register" }).click();

  await expect(
    page.getByRole("heading", { level: 2, name: "Registration received" }),
  ).toBeVisible();
  await expect(
    page.getByText(`Thank you, ${firstName} ${lastName}.`, { exact: false }),
  ).toBeVisible();
  const idText = await page.getByText(/Registration ID: /).textContent();
  const id = (idText ?? "").replace(/.*Registration ID:\s*/, "").trim();
  expect(id).toMatch(/^[0-9a-f-]{36}$/);

  const participantMail = await awaitMail(request, email, lastName);
  expect(participantMail.Text).toContain(`${firstName} ${lastName}`);
  if (optionName) expect(participantMail.Text).toContain(optionName);

  const organizer = (process.env.ORGANIZER_EMAILS ?? "").split(",")[0].trim();
  const organizerMail = await awaitMail(request, organizer, lastName);
  expect(organizerMail.Text).toContain(institution);
  expect((organizerMail.Attachments ?? []).map((a) => a.FileName)).toContain(
    `registration-${id}.json`,
  );

  const exported = await request.get(`${API_URL}/api/export`, {
    headers: { Authorization: organizerAuthHeader() },
  });
  expect(exported.status()).toBe(200);
  const workbookText = zipText(await exported.body());
  expect(workbookText).toContain(lastName);
  expect(workbookText).toContain(institution);
});

test("AC-001-01 / AC-004-01: an external participant registers end to end", async ({
  page,
  request,
}) => {
  const u = unique();
  const lastName = `Novak ${u}`;
  const email = `ana.${u}@example.test`;

  await page.goto("/");
  await expect(page.getByRole("radio", { name: "External participant" })).toBeChecked();
  await page.getByLabel("First name").fill("Ana");
  await page.getByLabel("Last name").fill(lastName);
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Organization / institution").fill("Institut Jožef Stefan");
  for (const consent of await page
    .getByRole("group", { name: "Consents" })
    .getByRole("checkbox")
    .all()) {
    await consent.check();
  }
  await page.getByRole("button", { name: "Register" }).click();

  await expect(
    page.getByRole("heading", { level: 2, name: "Registration received" }),
  ).toBeVisible();
  const participantMail = await awaitMail(request, email, lastName);
  expect(participantMail.Subject).toContain("Registration confirmation");

  const refused = await request.get(`${API_URL}/api/export`);
  expect(refused.status()).toBe(401);
});
