import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchFormConfig } from "./api";
import { testConfig } from "./testConfig";

describe("fetchFormConfig", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-001-08 loads the form configuration from /api/form-config", async () => {
    const fetchMock = vi.fn(async () => new Response(JSON.stringify(testConfig), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchFormConfig()).resolves.toEqual(testConfig);
    expect(fetchMock).toHaveBeenCalledWith("/api/form-config", expect.anything());
  });

  it("AC-001-08 fails when the backend answers with an error", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("{}", { status: 503 })),
    );

    await expect(fetchFormConfig()).rejects.toThrow();
  });
});
