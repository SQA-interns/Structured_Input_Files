import { afterEach, describe, expect, it, vi } from "vitest";
import { submitRegistration } from "./api";

const accepted = {
  id: "7d1c6a52-3f0e-4b8e-9d55-2a4f0c9b1e11",
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  options: [],
  registeredAt: "2026-10-19T12:00:00.000Z",
};

function reply(status: number, body: unknown) {
  const fetchMock = vi.fn(
    async () =>
      new Response(typeof body === "string" ? body : JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" },
      }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("submitRegistration", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-004-01 posts JSON to /api/registrations and returns the accepted registration", async () => {
    const fetchMock = reply(201, accepted);

    const result = await submitRegistration({ type: "EXTERNAL", firstName: "Ana" });

    expect(result).toEqual({ kind: "accepted", registration: accepted });
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("/api/registrations");
    expect(init.method).toBe("POST");
    expect(new Headers(init.headers).get("Content-Type")).toBe("application/json");
    expect(JSON.parse(init.body as string)).toEqual({ type: "EXTERNAL", firstName: "Ana" });
  });

  it("AC-004-02 a 400 with field errors is a rejection", async () => {
    reply(400, { message: "x", fieldErrors: [{ field: "email", message: "Bad email" }] });

    await expect(submitRegistration({})).resolves.toEqual({
      kind: "rejected",
      fieldErrors: [{ field: "email", message: "Bad email" }],
    });
  });

  it("AC-004-04 a 400 without field errors is a failure", async () => {
    reply(400, { message: "The request could not be read.", fieldErrors: [] });

    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });
  });

  it.each([500, 413, 429, 503])("AC-004-04 status %i is a failure", async (status) => {
    reply(status, { message: "internal", fieldErrors: [] });

    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });
  });

  it("AC-004-04 an unreadable answer is a failure", async () => {
    reply(201, "<html>proxy error</html>");

    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });
  });

  it("AC-004-04 an unreachable backend is a failure", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("Failed to fetch");
      }),
    );

    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });
  });
});
