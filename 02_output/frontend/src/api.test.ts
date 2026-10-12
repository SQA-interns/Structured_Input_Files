import { afterEach, describe, expect, it, vi } from "vitest";
import { loadFormConfig, submitRegistration } from "./api";
import type { RegistrationRequest } from "./types";

const request: RegistrationRequest = {
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@example.org",
  organization: "Org",
  optionIds: [],
  consents: ["privacy"],
  recaptchaToken: "t",
};

function reply(status: number, body: string) {
  const fetchMock = vi.fn(async () => new Response(body, { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("loadFormConfig", () => {
  it("returns the configuration from /api/form-config", async () => {
    const fetchMock = reply(200, JSON.stringify({ conferenceName: "C" }));

    await expect(loadFormConfig()).resolves.toEqual({ conferenceName: "C" });
    expect(fetchMock).toHaveBeenCalledWith("/api/form-config", {
      headers: { Accept: "application/json" },
    });
  });

  it("fails on a non-OK status", async () => {
    reply(503, "{}");

    await expect(loadFormConfig()).rejects.toThrow("form configuration unavailable");
  });
});

describe("submitRegistration", () => {
  it("posts JSON and returns the created registration", async () => {
    const fetchMock = reply(201, JSON.stringify({ id: "x" }));

    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "created",
      registration: { id: "x" },
    });
    expect(fetchMock).toHaveBeenCalledWith("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(request),
    });
  });

  it("returns field errors of a 400 answer", async () => {
    reply(400, JSON.stringify({ message: "m", fieldErrors: [{ field: "email", message: "e" }] }));

    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "rejected",
      fieldErrors: [{ field: "email", message: "e" }],
    });
  });

  it.each([
    [400, JSON.stringify({ message: "malformed" })],
    [400, JSON.stringify({ message: "m", fieldErrors: [] })],
    [400, JSON.stringify({ message: "m", fieldErrors: "x" })],
    [400, "not json"],
    [201, "not json"],
    [500, JSON.stringify({ message: "internal" })],
    [429, ""],
  ])("treats status %s with body %s as failed", async (status, body) => {
    reply(status, body);

    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
  });

  it("treats a network error as failed", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("Failed to fetch");
      }),
    );

    await expect(submitRegistration(request)).resolves.toEqual({ kind: "failed" });
  });
});
