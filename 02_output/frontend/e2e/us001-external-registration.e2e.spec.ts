import { expect, test } from "@playwright/test";
import { register, uniqueEmail } from "./support";

test("AC-001-03 an external participant registers through the form and the registration is accepted", async ({
  page,
}) => {
  const email = uniqueEmail("e2e-external");

  const response = await register(
    page,
    "External participant",
    {
      "First name": "Ana",
      "Last name": "Novak",
      Email: email,
      "Organization / institution": "Acme d.o.o.",
    },
    ["Workshop: Test automation", "Lunch, day 1"],
  );

  expect(response.status).toBe(201);
  expect(response.body.id).toMatch(/^[0-9a-f-]{36}$/);
  await expect(
    page.getByRole("heading", { name: "Registration received" }),
  ).toBeVisible();
});
