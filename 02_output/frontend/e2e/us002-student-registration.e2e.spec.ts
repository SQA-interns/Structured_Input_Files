import { expect, test } from "@playwright/test";
import { register, uniqueEmail } from "./support";

test("AC-002-03 a student registers through the form and the registration is accepted", async ({
  page,
}) => {
  const email = uniqueEmail("e2e-student");

  const response = await register(
    page,
    "Student",
    {
      "First name": "Luka",
      "Last name": "Kranjc",
      Email: email,
      "Study institution": "Univerza v Ljubljani",
      "Study programme": "Computer Science",
      "Student ID": "63210001",
    },
    ["Workshop: Test automation", "Career fair"],
  );

  expect(response.status).toBe(201);
  await expect(
    page.getByRole("heading", { name: "Registration received" }),
  ).toBeVisible();
});
