import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchFormConfig, submitRegistration } from "./api";
import { testConfig } from "./testConfig";

function stub(status: number, body: unknown) {
  const fetchMock = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function headersOf(fetchMock: ReturnType<typeof stub>) {
  const call = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
  return new Headers(call[1].headers);
}

describe("api requests (phase 5 survivors)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-001-08 the form configuration is requested as JSON", async () => {
    const fetchMock = stub(200, testConfig);

    await fetchFormConfig();

    expect(headersOf(fetchMock).get("Accept")).toBe("application/json");
  });

  it("AC-001-08 a failed configuration request names the status", async () => {
    stub(503, {});

    await expect(fetchFormConfig()).rejects.toThrow("form configuration answered 503");
  });

  it("AC-004-01 the registration is sent and answered as JSON", async () => {
    const fetchMock = stub(201, {});

    await submitRegistration({});

    expect(headersOf(fetchMock).get("Accept")).toBe("application/json");
  });

  it("AC-004-04 field errors with another status than 400 are a failure", async () => {
    stub(422, { message: "x", fieldErrors: [{ field: "email", message: "Bad" }] });

    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });
  });

  it("AC-004-04 a 400 whose field errors are not a list is a failure", async () => {
    stub(400, { message: "x", fieldErrors: "email" });

    await expect(submitRegistration({})).resolves.toEqual({ kind: "failed" });
  });
});
