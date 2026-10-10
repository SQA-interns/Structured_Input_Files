import { expect, test } from "@playwright/test";
import {
  awaitMessages,
  fillCommon,
  formConfig,
  message,
  organizerCredentials,
  uniqueEmail,
  workbookText,
} from "./support";

test.describe("US-001 external participant, end to end", () => {
  test("AC-001-02 AC-001-06 AC-004-01 AC-006-01 AC-007-01 AC-008-01 Slovenian names survive form, emails and export", async ({
    page,
    request,
  }) => {
    const config = await formConfig(request);
    const email = uniqueEmail("e2e-external");
    const person = { firstName: "Čedomir Šime", lastName: "Žagar-Čuk", email };
    const organization = "Fakulteta za računalništvo – ŠŽČ";

    await page.goto("/");
    await page
      .getByRole("radiogroup", { name: "Registration type" })
      .getByRole("radio", { name: "External participant" })
      .check();
    const options = await fillCommon(page, config, "EXTERNAL", person);
    await page.getByLabel("Organization / institution").fill(organization);
    await page.getByRole("button", { name: "Register" }).click();

    const status = page.getByRole("status");
    await expect(status.getByRole("heading", { name: "Registration received" })).toBeVisible();
    await expect(status).toContainText("Čedomir Šime Žagar-Čuk");
    for (const option of options) {
      await expect(status).toContainText(option);
    }
    const reference = ((await status.textContent()) ?? "").match(
      /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/,
    )?.[0];
    expect(reference).toBeDefined();

    const [participantMail] = await awaitMessages(request, `to:"${email}"`, 1);
    expect((await message(request, participantMail.ID)).Text).toContain("Čedomir Šime");

    const organizerMails = (await awaitMessages(request, `subject:"${reference}"`, 1)).filter(
      (m) => !m.To.some((to) => to.Address === email),
    );
    expect(organizerMails.length).toBeGreaterThanOrEqual(1);
    const organizerMail = await message(request, organizerMails[0].ID);
    expect(organizerMail.Text).toContain("Žagar-Čuk");
    expect(organizerMail.Text).toContain(organization);
    expect(organizerMail.Attachments.map((a) => a.FileName)).toEqual([
      `registration-${reference}.json`,
    ]);

    const { username, password } = organizerCredentials();
    const exported = await request.get("/api/export/registrations.xlsx", {
      headers: {
        Authorization: `Basic ${Buffer.from(`${username}:${password}`).toString("base64")}`,
      },
    });
    expect(exported.status()).toBe(200);
    const text = workbookText(await exported.body());
    expect(text).toContain(reference as string);
    expect(text).toContain("Čedomir Šime");
    expect(text).toContain("Žagar-Čuk");
    expect(text).toContain(organization);
  });

  test("AC-001-12 shows field errors and sends nothing when the form is incomplete", async ({
    page,
  }) => {
    let posted = false;
    page.on("request", (r) => {
      if (r.method() === "POST" && r.url().endsWith("/api/registrations")) posted = true;
    });

    await page.goto("/");
    await page
      .getByRole("radiogroup", { name: "Registration type" })
      .getByRole("radio", { name: "External participant" })
      .check();
    await page.getByLabel("Email").fill("not-an-email");
    await page.getByRole("button", { name: "Register" }).click();

    await expect(page.locator("[id='firstName-error']")).toHaveText("This field is required.");
    await expect(page.locator("[id='email-error']")).toHaveText("Enter a valid email address.");
    expect(posted).toBe(false);
  });
});
