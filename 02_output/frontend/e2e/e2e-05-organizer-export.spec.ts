import { expect, test } from "@playwright/test";
import { organizerAuthorization, readWorkbookRows, sql } from "./support/stack";

const XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

test("E2E-05 organizer exports registrations (AC-008-01, AC-008-02)", async ({ request }) => {
  // With valid organizer credentials: a valid workbook with one row per registration.
  const exported = await request.get("/api/export", {
    headers: { Authorization: organizerAuthorization() },
  });
  expect(exported.status()).toBe(200);
  const registrations = Number(sql("SELECT count(*) FROM registration")[0][0]);
  const someEmails = sql("SELECT email FROM registration LIMIT 3").map((r) => r[0]);
  expect(exported.headers()["content-type"]).toContain(XLSX);
  expect(exported.headers()["content-disposition"] ?? "").toContain(".xlsx");
  const workbook = Buffer.from(await exported.body());
  expect(workbook.subarray(0, 2).toString("latin1")).toBe("PK");
  const rows = readWorkbookRows(workbook).filter((r) => r.some((cell) => cell !== ""));
  expect(rows[0]).toContain("Registration ID");
  expect(rows.length - 1).toBe(registrations);

  // The same request without credentials is refused and returns no data.
  const refused = await request.get("/api/export");
  expect(refused.status()).toBe(401);
  const refusedBody = Buffer.from(await refused.body());
  expect(refusedBody.subarray(0, 2).toString("latin1")).not.toBe("PK");
  for (const email of someEmails) {
    expect(refusedBody.toString("utf8")).not.toContain(email);
  }
});
