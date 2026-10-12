import { vi } from "vitest";

// A stand-in for the backend that speaks the OpenAPI contract (docs/02_contracts/openapi.json).

export const PRIVACY_TEXT =
  "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers.";
export const PHOTOS_TEXT = "I agree to appear in conference photos.";

export const formConfig = {
  conferenceName: "Acceptance Conference 2026",
  recaptchaSiteKey: "",
  recaptchaTestMode: true,
  consents: [
    { id: "privacy", text: PRIVACY_TEXT, mandatory: true },
    { id: "photos", text: PHOTOS_TEXT, mandatory: false },
  ],
  options: [
    {
      id: "ws-testing",
      name: "Test automation workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ws-security",
      name: "Secure coding workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL"],
    },
    {
      id: "ev-opening",
      name: "Opening reception",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "meal-lunch",
      name: "Lunch",
      category: "MEAL",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "other-tour",
      name: "City tour",
      category: "OTHER",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
  ],
};

export type Responder = (body: unknown) => Response | "network-error";

export interface FakeApi {
  /** Parsed JSON bodies of every POST /api/registrations, in order. */
  registrations: unknown[];
}

export function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

async function readRequest(
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<{ url: string; method: string; body: string | null }> {
  if (input instanceof Request) {
    return {
      url: input.url,
      method: input.method,
      body: input.method === "GET" ? null : await input.clone().text(),
    };
  }
  const url = input instanceof URL ? input.toString() : input;
  const body = typeof init?.body === "string" ? init.body : null;
  return { url, method: (init?.method ?? "GET").toUpperCase(), body };
}

/** Replaces fetch: GET /api/form-config answers formConfig, POST /api/registrations asks respond. */
export function installFakeApi(respond: Responder): FakeApi {
  const api: FakeApi = { registrations: [] };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const request = await readRequest(input, init);
      const path = new URL(request.url, "http://localhost").pathname;
      if (path === "/api/form-config" && request.method === "GET") {
        return jsonResponse(200, formConfig);
      }
      if (path === "/api/registrations" && request.method === "POST") {
        const body: unknown =
          request.body === null ? null : JSON.parse(request.body);
        api.registrations.push(body);
        const answer = respond(body);
        if (answer === "network-error") {
          throw new TypeError("Failed to fetch");
        }
        return answer;
      }
      throw new Error(`Unexpected request ${request.method} ${request.url}`);
    }),
  );
  return api;
}

export const created = () =>
  jsonResponse(201, {
    id: "6f1c1b7e-2f7a-4c55-9d0e-3c1f7c2b9a10",
    registeredAt: "2026-10-13T08:00:00.000Z",
  });
