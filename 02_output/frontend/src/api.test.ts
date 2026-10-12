import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchOptions, fetchSettings, submitRegistration } from "./api";
import type { RegistrationRequest } from "./types";

const request: RegistrationRequest = {
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@x.si",
  organization: "Acme",
  optionIds: [],
  consentGiven: true,
  recaptchaToken: "t",
};

function respond(status: number, body: unknown, raw = false) {
  const fetchMock = vi.fn(
    async () =>
      new Response(raw ? (body as string) : JSON.stringify(body), { status }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe("api", () => {
  it("posts the registration as JSON and returns the confirmation on 201", async () => {
    const fetchMock = respond(201, { id: "1", selectedOptions: [] });

    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "accepted",
      confirmation: { id: "1", selectedOptions: [] },
    });
    const [url, init] = fetchMock.mock.calls[0] as unknown as [
      string,
      RequestInit,
    ];
    expect(url).toBe("/api/registrations");
    expect(init.method).toBe("POST");
    expect(JSON.parse(String(init.body))).toEqual(request);
  });

  it.each([400, 409])(
    "returns the field errors of a %i problem",
    async (status) => {
      respond(status, { fieldErrors: [{ field: "email", message: "m" }] });

      await expect(submitRegistration(request)).resolves.toEqual({
        kind: "rejected",
        fieldErrors: [{ field: "email", message: "m" }],
      });
    },
  );

  it("treats a 400 without field errors, an unreadable body, other statuses and network errors as failures", async () => {
    respond(400, { title: "Bad Request" });
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "failed",
    });
    respond(400, "<html>", true);
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "failed",
    });
    respond(429, {});
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "failed",
    });
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "failed",
    });
  });

  it("loads settings and options, and fails on an error status", async () => {
    respond(200, [{ id: "a" }]);
    await expect(fetchOptions()).resolves.toEqual([{ id: "a" }]);
    respond(200, { conferenceName: "C" });
    await expect(fetchSettings()).resolves.toEqual({ conferenceName: "C" });
    respond(503, {});
    await expect(fetchSettings()).rejects.toThrow("503");
  });
});
