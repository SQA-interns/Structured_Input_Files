import { expect, test } from "@playwright/test";
import {
  firstSheetRowCount,
  organizerAuthorization,
  sql,
  unique,
  unzip,
  workbookStrings,
} from "./support";

const XLSX =
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

test("E2E-05 organizer exports registrations (AC-008-01, AC-008-02)", async ({
  request,
}) => {
  // At least one stored registration, submitted through the public API of the stack.
  const run = unique();
  const email = `export.${run}@e2e.example`;
  const created = await request.post("/api/registrations", {
    data: {
      type: "EXTERNAL",
      firstName: "Izvoz",
      lastName: `Organizator${run}`,
      email,
      organization: "Zavod Primer",
      optionIds: ["ws-cloud"],
      consents: { privacy: true },
      recaptchaToken: "test-pass",
    },
  });
  expect(created.status(), "registration accepted").toBe(201);

  // With organizer credentials: a valid workbook with one row per registration
  const exported = await request.get("/api/registrations/export", {
    headers: { Authorization: organizerAuthorization() },
  });
  expect(exported.status()).toBe(200);
  expect(exported.headers()["content-type"]).toContain(XLSX);
  const workbook = unzip(await exported.body());
  expect(workbook.has("xl/workbook.xml")).toBe(true);
  const registrations = Number(sql("SELECT count(*) FROM registration")[0][0]);
  expect(firstSheetRowCount(workbook)).toBe(registrations + 1);
  expect(workbookStrings(workbook)).toContain(email);

  // Without credentials: refused, no data
  const refused = await request.get("/api/registrations/export");
  expect(refused.status()).toBe(401);
  const body = await refused.body();
  expect(body.subarray(0, 2).toString("latin1")).not.toBe("PK");
  expect(body.toString("utf8")).not.toContain(email);
});
