import { expect, test } from "@playwright/test";
import { organizerCredentials } from "./support";

test.describe("US-008 export, end to end", () => {
  test("AC-008-02 export without organizer credentials is refused", async ({ request }) => {
    const response = await request.get("/api/export/registrations.xlsx");

    expect(response.status()).toBe(401);
    expect(response.headers()["content-type"] ?? "").not.toContain("spreadsheetml");
  });

  test("AC-008-01 export with organizer credentials returns a workbook", async ({ request }) => {
    const { username, password } = organizerCredentials();

    const response = await request.get("/api/export/registrations.xlsx", {
      headers: {
        Authorization: `Basic ${Buffer.from(`${username}:${password}`).toString("base64")}`,
      },
    });

    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toContain(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    );
    const body = await response.body();
    expect(body.subarray(0, 2).toString("latin1")).toBe("PK");
  });
});
