import { vi } from "vitest";

/** Form configuration as GET /api/form-config returns it (openapi.json FormConfig). */
export const formConfig = {
  conferenceName: "Konferenca Test 2026",
  options: [
    {
      id: "ws-ai",
      name: "Workshop: AI in practice",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ev-dinner",
      name: "Conference dinner",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "meal-lunch", name: "Lunch", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "oth-tour", name: "City tour", category: "OTHER", availableTo: ["EXTERNAL"] },
    { id: "oth-mentoring", name: "Student mentoring", category: "OTHER", availableTo: ["STUDENT"] },
  ],
  consents: [
    {
      id: "privacy",
      text: "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers.",
      mandatory: true,
    },
    { id: "photos", text: "I agree to appear in conference photos.", mandatory: false },
  ],
  recaptcha: { siteKey: "", testMode: true },
};

export const PRIVACY_TEXT = formConfig.consents[0].text;
export const PHOTOS_TEXT = formConfig.consents[1].text;

export type RegistrationReply =
  { kind: "json"; status: number; body: unknown } | { kind: "network-error" };

export interface RecordedRequest {
  url: string;
  method: string;
  body: unknown;
}

/**
 * Replaces fetch with a test double of the backend contract: GET /api/form-config answers the
 * configuration above, POST /api/registrations answers the given reply. Every call is recorded.
 */
export function installFakeBackend(registrationReply: RegistrationReply) {
  const requests: RecordedRequest[] = [];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url =
      typeof input === "string" ? input : input instanceof URL ? input.toString() : input.url;
    const method = (
      init?.method ?? (typeof input === "object" && "method" in input ? input.method : "GET")
    ).toUpperCase();
    const rawBody = init?.body;
    let body: unknown = undefined;
    if (typeof rawBody === "string") {
      try {
        body = JSON.parse(rawBody);
      } catch {
        body = rawBody;
      }
    }
    requests.push({ url, method, body });
    const path = new URL(url, "http://localhost").pathname;
    if (path === "/api/form-config" && method === "GET") {
      return jsonResponse(200, formConfig);
    }
    if (path === "/api/registrations" && method === "POST") {
      if (registrationReply.kind === "network-error") {
        throw new TypeError("Failed to fetch");
      }
      return jsonResponse(registrationReply.status, registrationReply.body);
    }
    return jsonResponse(404, { message: "Not found", fieldErrors: [] });
  });
  vi.stubGlobal("fetch", fetchMock);
  return {
    requests,
    registrationRequests: () =>
      requests.filter(
        (r) =>
          r.method === "POST" &&
          new URL(r.url, "http://localhost").pathname === "/api/registrations",
      ),
  };
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

export const acceptedExternal = {
  kind: "json" as const,
  status: 201,
  body: {
    id: "7d1c6a52-3f0e-4b8e-9d55-2a4f0c9b1e11",
    type: "EXTERNAL",
    firstName: "Ana",
    lastName: "Novak",
    options: [
      { id: "ws-ai", name: "Workshop: AI in practice", category: "WORKSHOP" },
      { id: "meal-lunch", name: "Lunch", category: "MEAL" },
    ],
    registeredAt: "2026-10-19T12:00:00.123Z",
  },
};
