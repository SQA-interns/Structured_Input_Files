import { expect, test } from "@playwright/test";
import { basicAuth, organizer, registrationCount } from "./support/stack";
import { readWorkbookRows } from "./support/xlsx";

const XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

test("E2E-05 organizer exports registrations (AC-008-01, AC-008-02)", async ({ request }) => {
  const { username, password } = organizer();
  const stored = registrationCount();

  const response = await request.get("/api/registrations/export", {
    headers: { Authorization: basicAuth(username, password) },
  });

  expect(response.status()).toBe(200);
  expect(response.headers()["content-type"]).toContain(XLSX);
  expect(response.headers()["content-disposition"]).toContain("registrations.xlsx");
  const rows = readWorkbookRows(await response.body());
  expect(rows[0][0]).toBe("Registration ID");
  expect(rows).toHaveLength(stored + 1);

  const refused = await request.get("/api/registrations/export");
  expect(refused.status()).toBe(401);
  expect(refused.headers()["content-type"] ?? "").not.toContain(XLSX);
  const refusedBody = await refused.body();
  expect(refusedBody.subarray(0, 2).toString("latin1")).not.toBe("PK");
  expect(refusedBody.toString("utf8")).not.toContain("@example.com");
});
