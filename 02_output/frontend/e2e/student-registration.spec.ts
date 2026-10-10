import { expect, test } from "@playwright/test";
import { awaitMessages, fillCommon, formConfig, message, uniqueEmail } from "./support";

test.describe("US-002 student, end to end", () => {
  test("AC-002-01 AC-002-02 AC-006-01 student registers and receives the confirmation email", async ({
    page,
    request,
  }) => {
    const config = await formConfig(request);
    const email = uniqueEmail("e2e-student");

    await page.goto("/");
    await page
      .getByRole("radiogroup", { name: "Registration type" })
      .getByRole("radio", { name: "Student" })
      .check();
    await expect(page.getByLabel("Organization / institution")).toHaveCount(0);
    await fillCommon(page, config, "STUDENT", { firstName: "Luka", lastName: "Kranjc", email });
    await page.getByLabel("Study institution").fill("Univerza v Mariboru");
    await page.getByLabel("Study programme").fill("Računalništvo in informatika");
    await page.getByLabel("Student ID").fill("E1234567");
    await page.getByRole("button", { name: "Register" }).click();

    await expect(page.getByRole("status")).toContainText("Registration received");
    const [mail] = await awaitMessages(request, `to:"${email}"`, 1);
    const text = (await message(request, mail.ID)).Text;
    expect(text).toContain("Luka");
    expect(text).toContain("Student");
  });
});
