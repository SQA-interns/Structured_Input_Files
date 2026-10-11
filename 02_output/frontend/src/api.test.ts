import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchFormConfig, submitRegistration, type RegistrationRequest } from "./api";

const body: RegistrationRequest = {
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@x.si",
  organization: "IJS",
  optionIds: [],
  consentIds: ["privacy"],
  captchaToken: "TEST-PASS",
};

function reply(status: number, json?: unknown, raw?: string) {
  const fetchMock = vi.fn(
    async () => new Response(raw ?? (json === undefined ? null : JSON.stringify(json)), { status }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe("api", () => {
  it("loads the form configuration from the relative API path", async () => {
    const fetchMock = reply(200, { conferenceName: "K" });
    await expect(fetchFormConfig()).resolves.toEqual({ conferenceName: "K" });
    expect(fetchMock).toHaveBeenCalledWith("/api/form-config", expect.anything());
  });

  it("fails when the configuration is unavailable", async () => {
    reply(503);
    await expect(fetchFormConfig()).rejects.toThrow();
  });

  it("posts JSON and maps 201 to accepted", async () => {
    const fetchMock = reply(201, { id: "1", options: [] });
    await expect(submitRegistration(body)).resolves.toEqual({
      kind: "accepted",
      registration: { id: "1", options: [] },
    });
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("/api/registrations");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual(body);
  });

  it("maps 400 field errors, 429 and other failures", async () => {
    reply(400, { fieldErrors: [{ field: "email", message: "bad" }] });
    await expect(submitRegistration(body)).resolves.toEqual({
      kind: "rejected",
      fieldErrors: [{ field: "email", message: "bad" }],
    });
    reply(400, {});
    await expect(submitRegistration(body)).resolves.toEqual({ kind: "rejected", fieldErrors: [] });
    reply(400, undefined, "<html>");
    await expect(submitRegistration(body)).resolves.toEqual({ kind: "failed" });
    reply(429);
    await expect(submitRegistration(body)).resolves.toEqual({ kind: "rate-limited" });
    reply(413);
    await expect(submitRegistration(body)).resolves.toEqual({ kind: "failed" });
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("offline");
      }),
    );
    await expect(submitRegistration(body)).resolves.toEqual({ kind: "failed" });
  });
});
