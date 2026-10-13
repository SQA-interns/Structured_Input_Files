import { describe, expect, it, vi } from "vitest";
import { loadFormConfig, submitRegistration } from "./api";
import type { RegistrationRequest } from "./types";

const REQUEST: RegistrationRequest = {
  type: "EXTERNAL",
  firstName: "Ana",
  optionIds: [],
  consents: {},
  recaptchaToken: "t",
};

function respond(status: number, body: unknown) {
  const fetchMock = vi.fn(
    async () =>
      new Response(typeof body === "string" ? body : JSON.stringify(body), {
        status,
      }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("loadFormConfig", () => {
  it("returns the configuration from the relative API URL", async () => {
    const config = { options: [], consents: [], conferenceName: "C" };
    const fetchMock = respond(200, config);
    await expect(loadFormConfig()).resolves.toEqual(config);
    expect(fetchMock).toHaveBeenCalledWith("/api/form-config");
  });

  it("fails on a non-OK answer", async () => {
    respond(503, {});
    await expect(loadFormConfig()).rejects.toThrow("503");
  });

  it("fails on an incomplete configuration", async () => {
    respond(200, { options: [] });
    await expect(loadFormConfig()).rejects.toThrow("incomplete");
    respond(200, { consents: [] });
    await expect(loadFormConfig()).rejects.toThrow("incomplete");
  });
});

describe("submitRegistration", () => {
  it("posts JSON and returns the accepted registration", async () => {
    const accepted = { id: "1", firstName: "A", lastName: "B", options: [] };
    const fetchMock = respond(201, accepted);

    await expect(submitRegistration(REQUEST)).resolves.toEqual({
      kind: "accepted",
      registration: accepted,
    });
    expect(fetchMock).toHaveBeenCalledWith("/api/registrations", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(REQUEST),
    });
  });

  it.each([
    [{ id: 1, firstName: "A", lastName: "B", options: [] }],
    [{ id: "1", lastName: "B", options: [] }],
    [{ id: "1", firstName: "A", options: [] }],
    [{ id: "1", firstName: "A", lastName: "B" }],
    [null],
  ])(
    "treats an accepted answer without the contract shape as failed",
    async (body) => {
      respond(201, body);
      await expect(submitRegistration(REQUEST)).resolves.toEqual({
        kind: "failed",
      });
    },
  );

  it("returns only well-formed field errors of a 400 answer", async () => {
    respond(400, {
      errors: [
        { field: "email", message: "bad" },
        { field: 1, message: "x" },
        { field: "x" },
      ],
    });
    await expect(submitRegistration(REQUEST)).resolves.toEqual({
      kind: "rejected",
      errors: [{ field: "email", message: "bad" }],
    });
  });

  it.each([[{}], [{ errors: [] }], [{ errors: "x" }]])(
    "treats a 400 answer without field errors as failed",
    async (body) => {
      respond(400, body);
      await expect(submitRegistration(REQUEST)).resolves.toEqual({
        kind: "failed",
      });
    },
  );

  it("treats other statuses, invalid JSON and network errors as failed", async () => {
    respond(429, { message: "Too many" });
    await expect(submitRegistration(REQUEST)).resolves.toEqual({
      kind: "failed",
    });
    respond(201, "not json");
    await expect(submitRegistration(REQUEST)).resolves.toEqual({
      kind: "failed",
    });
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => Promise.reject(new TypeError("offline"))),
    );
    await expect(submitRegistration(REQUEST)).resolves.toEqual({
      kind: "failed",
    });
  });
});
