import { vi } from "vitest";

/** Form configuration as served by GET /api/form-config (openapi.json FormConfig). */
export const formConfig = {
  conferenceName: "Konferenca 2026",
  captcha: { mode: "test", siteKey: "" },
  consents: [
    {
      id: "privacy",
      text: "I agree that my personal data is processed for organizing the conference.",
      mandatory: true,
    },
    { id: "photo", text: "I agree to be photographed at the conference.", mandatory: false },
  ],
  options: [
    {
      id: "ws-ai",
      name: "Workshop: AI v praksi",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ws-cloud",
      name: "Workshop: Cloud security",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL"],
    },
    {
      id: "ev-dinner",
      name: "Gala večerja",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "meal-lunch", name: "Kosilo", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "other-tour", name: "Ogled Ljubljane", category: "OTHER", availableTo: ["STUDENT"] },
  ],
};

export type Reply = { status: number; body?: unknown } | "network-error";

export interface RecordedRequest {
  url: string;
  method: string;
  body: unknown;
}

function urlOf(input: unknown): string {
  if (typeof input === "string") return input;
  if (input instanceof URL) return input.toString();
  return (input as { url: string }).url;
}

/**
 * Stubs global fetch: GET /api/form-config answers the fixture, POST /api/registrations answers
 * the given reply. Returns the recorded requests.
 */
export function stubBackend(registrationReply: Reply, config: unknown = formConfig) {
  const requests: RecordedRequest[] = [];
  const fetchMock = vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = urlOf(input);
    const method = (init?.method ?? "GET").toUpperCase();
    const body = typeof init?.body === "string" ? JSON.parse(init.body) : undefined;
    requests.push({ url, method, body });
    if (url.endsWith("/api/form-config")) {
      return new Response(JSON.stringify(config), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }
    if (url.endsWith("/api/registrations")) {
      if (registrationReply === "network-error") throw new TypeError("Failed to fetch");
      return new Response(
        registrationReply.body === undefined ? null : JSON.stringify(registrationReply.body),
        {
          status: registrationReply.status,
          headers: { "Content-Type": "application/json" },
        },
      );
    }
    return new Response(null, { status: 404 });
  });
  vi.stubGlobal("fetch", fetchMock);
  return {
    requests,
    registrationRequests: () => requests.filter((r) => r.url.endsWith("/api/registrations")),
  };
}
