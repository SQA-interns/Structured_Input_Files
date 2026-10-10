import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchFormConfig, submitRegistration, type RegistrationRequest } from "./api";

const body: RegistrationRequest = {
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  email: "a@example.si",
  organization: "IJS",
  optionIds: [],
  consentIds: ["data"],
  captchaToken: "t",
};

function respond(status: number, payload: unknown) {
  const fetchMock = vi.fn(
    async () =>
      new Response(typeof payload === "string" ? payload : JSON.stringify(payload), { status }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe("api", () => {
  it("loads the form configuration", async () => {
    const fetchMock = respond(200, { conferenceName: "K" });
    await expect(fetchFormConfig()).resolves.toEqual({ conferenceName: "K" });
    expect(fetchMock).toHaveBeenCalledWith("/api/form-config", expect.anything());
  });

  it("fails when the configuration cannot be loaded", async () => {
    respond(503, {});
    await expect(fetchFormConfig()).rejects.toThrow("503");
  });

  it("posts JSON and returns the accepted registration", async () => {
    const fetchMock = respond(201, { reference: "r" });
    await expect(submitRegistration(body)).resolves.toEqual({
      kind: "accepted",
      registration: { reference: "r" },
    });
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("/api/registrations");
    expect(init.method).toBe("POST");
    expect(new Headers(init.headers).get("Content-Type")).toBe("application/json");
    expect(JSON.parse(init.body as string)).toEqual(body);
  });

  it.each([400, 409])("returns field errors of a %s response", async (status) => {
    const fieldErrors = [{ field: "email", code: "X", message: "m" }];
    respond(status, { fieldErrors });
    await expect(submitRegistration(body)).resolves.toEqual({
      kind: "rejected",
      status,
      fieldErrors,
    });
  });

  it.each([
    [400, { fieldErrors: [] }],
    [400, { message: "no list" }],
    [400, "not json"],
    [429, { fieldErrors: [{ field: "x", code: "y", message: "z" }] }],
    [500, {}],
  ])("treats %s %j as a failure", async (status, payload) => {
    respond(status, payload);
    await expect(submitRegistration(body)).resolves.toEqual({ kind: "failed", status });
  });

  it("turns network errors into a failure", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("offline");
      }),
    );
    await expect(submitRegistration(body)).resolves.toEqual({ kind: "failed", status: 0 });
  });
});
