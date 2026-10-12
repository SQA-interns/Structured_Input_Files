import { expect, test } from "@playwright/test";
import {
  MAILPIT_URL,
  awaitMail,
  organizerCredentials,
  register,
  uniqueEmail,
  workbookStrings,
} from "./support";

test("NFR-01 AC-001-07 AC-006-02 AC-007-02 AC-008-02 Slovenian characters survive form, emails, JSON copy and export", async ({
  page,
  request,
}) => {
  const email = uniqueEmail("e2e-sumniki");
  const firstName = "Žiga Špela";
  const lastName = "Čebašek-Žužek";
  const organization = "Šolski center Škofja Loka (ČŠŽ čšž)";

  const response = await register(
    page,
    "External participant",
    {
      "First name": firstName,
      "Last name": lastName,
      Email: email,
      "Organization / institution": organization,
    },
    ["Workshop: Test automation"],
  );
  expect(response.status).toBe(201);
  await expect(
    page.getByText(`Thank you, ${firstName} ${lastName}.`, { exact: false }),
  ).toBeVisible();

  const participantMail = await awaitMail(
    request,
    email,
    "Registration confirmation",
  );
  expect(participantMail.text).toContain(firstName);
  expect(participantMail.text).toContain(lastName);

  const organizerAddress = (
    await (await request.get(`${MAILPIT_URL}/api/v1/messages?limit=200`)).json()
  ).messages
    .filter((m: { Subject: string }) =>
      m.Subject.startsWith("New registration"),
    )
    .map((m: { To: { Address: string }[] }) => m.To[0].Address)[0] as string;
  const organizerMail = await awaitMail(
    request,
    organizerAddress,
    "New registration",
  );
  const attachment = organizerMail.attachments.find(
    (a) => a.FileName === `registration-${response.body.id}.json`,
  );
  expect(attachment, "JSON copy attached to the organizer email").toBeDefined();
  const copy = await (
    await request.get(
      `${MAILPIT_URL}/api/v1/message/${organizerMail.id}/part/${attachment!.PartID}`,
    )
  ).json();
  expect(copy).toMatchObject({
    id: response.body.id,
    firstName,
    lastName,
    organization,
    email,
  });

  const { username, password } = organizerCredentials();
  const exportResponse = await request.get("/api/organizer/export", {
    headers: { Authorization: `Basic ${btoa(`${username}:${password}`)}` },
  });
  expect(exportResponse.status()).toBe(200);
  const cells = await workbookStrings(
    new Uint8Array(await exportResponse.body()),
  );
  expect(cells).toContain(firstName);
  expect(cells).toContain(lastName);
  expect(cells).toContain(organization);
});

test("AC-008-03 the export is refused without organizer credentials", async ({
  request,
}) => {
  const response = await request.get("/api/organizer/export");

  expect(response.status()).toBe(401);
});
