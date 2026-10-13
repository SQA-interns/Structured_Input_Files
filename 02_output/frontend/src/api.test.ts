import { describe, expect, it, vi } from "vitest";
import { fetchFormConfig, submitRegistration } from "./api";
import type { RegistrationPayload } from "./types";

const payload: RegistrationPayload = {
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  email: "a@b.co",
  organization: "Acme",
  optionIds: ["ws"],
  consents: ["privacy"],
  recaptchaToken: "tok",
};

function answer(status: number, body: unknown, raw = false) {
  const fetchMock = vi.fn(
    async () =>
      new Response(raw ? (body as string) : JSON.stringify(body), { status }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("fetchFormConfig", () => {
  it("reads the form configuration as JSON", async () => {
    const fetchMock = answer(200, { conferenceName: "K" });
    await expect(fetchFormConfig()).resolves.toEqual({ conferenceName: "K" });
    expect(fetchMock).toHaveBeenCalledWith("/api/form-config", {
      headers: { Accept: "application/json" },
    });
  });

  it("fails when the backend does not answer 2xx", async () => {
    answer(503, {});
    await expect(fetchFormConfig()).rejects.toThrow(
      "form configuration unavailable (503)",
    );
  });
});

describe("submitRegistration", () => {
  it("posts the payload as JSON and returns the new id on 201", async () => {
    const fetchMock = answer(201, { id: "abc", registeredAt: "t" });

    await expect(submitRegistration(payload)).resolves.toEqual({
      kind: "created",
      id: "abc",
    });

    expect(fetchMock).toHaveBeenCalledWith("/api/registrations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(payload),
    });
  });

  it("returns the field errors of a 400 answer, keeping only text messages", async () => {
    answer(400, { message: "m", fieldErrors: { email: "Bad", other: 5 } });
    await expect(submitRegistration(payload)).resolves.toEqual({
      kind: "rejected",
      fieldErrors: { email: "Bad" },
    });
  });

  it.each([
    ["no field errors", { message: "m", fieldErrors: {} }],
    ["field errors not an object", { message: "m", fieldErrors: "x" }],
    ["field errors null", { message: "m", fieldErrors: null }],
    ["no body object", "plain"],
    ["null body", null],
  ])("treats a 400 with %s as a failure", async (_name, body) => {
    answer(400, body);
    await expect(submitRegistration(payload)).resolves.toEqual({
      kind: "failed",
    });
  });

  it("treats a 400 that is not JSON as a failure", async () => {
    answer(400, "<html>", true);
    await expect(submitRegistration(payload)).resolves.toEqual({
      kind: "failed",
    });
  });

  it.each([500, 413, 429, 401, 200])(
    "treats status %i as a failure",
    async (status) => {
      answer(status, { id: "x", fieldErrors: { email: "e" } });
      await expect(submitRegistration(payload)).resolves.toEqual({
        kind: "failed",
      });
    },
  );

  it("treats a network error as a failure", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("Failed to fetch");
      }),
    );
    await expect(submitRegistration(payload)).resolves.toEqual({
      kind: "failed",
    });
  });
});
