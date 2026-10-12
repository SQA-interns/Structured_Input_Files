import { vi } from "vitest";

/** Options as `GET /api/options` returns them (active only, contract `openapi.yaml`). */
export const OPTIONS = [
  {
    id: "ws-a",
    name: "Workshop A: Testing",
    category: "WORKSHOP",
    registrationTypes: ["EXTERNAL", "STUDENT"],
  },
  {
    id: "ws-ext",
    name: "Workshop for professionals",
    category: "WORKSHOP",
    registrationTypes: ["EXTERNAL"],
  },
  {
    id: "ev-dinner",
    name: "Conference dinner",
    category: "EVENT",
    registrationTypes: ["EXTERNAL", "STUDENT"],
  },
  {
    id: "meal-lunch",
    name: "Lunch",
    category: "MEAL",
    registrationTypes: ["EXTERNAL", "STUDENT"],
  },
  {
    id: "other-fair",
    name: "Career fair",
    category: "OTHER",
    registrationTypes: ["STUDENT"],
  },
];

export const CONSENT_TEXT =
  "I agree to the processing of my personal data for organising the conference.";

export const SETTINGS = {
  conferenceName: "Test Conference 2026",
  consent: { id: "data-processing", text: CONSENT_TEXT },
  recaptcha: { testMode: true, siteKey: "" },
};

type RegistrationReply =
  { status: number; body?: unknown } | "network-error" | "pending";

export interface FakeBackend {
  /** Bodies of every `POST /api/registrations`, parsed. */
  registrations: Record<string, unknown>[];
  reply: (r: RegistrationReply) => void;
}

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type":
        status >= 400 ? "application/problem+json" : "application/json",
    },
  });
}

/** Replaces `fetch` with a backend that serves the contract's responses. */
export function installFakeBackend(): FakeBackend {
  let next: RegistrationReply = { status: 201 };
  const backend: FakeBackend = {
    registrations: [],
    reply: (r) => {
      next = r;
    },
  };
  const fetchMock = vi.fn(
    async (input: RequestInfo | URL, init?: RequestInit) => {
      const url =
        typeof input === "string"
          ? input
          : input instanceof URL
            ? input.href
            : input.url;
      const method = (init?.method ?? "GET").toUpperCase();
      const path = new URL(url, "http://localhost").pathname;
      if (method === "GET" && path === "/api/settings")
        return json(200, SETTINGS);
      if (method === "GET" && path === "/api/options")
        return json(200, OPTIONS);
      if (method === "POST" && path === "/api/registrations") {
        const body = JSON.parse(String(init?.body)) as Record<string, unknown>;
        backend.registrations.push(body);
        if (next === "network-error") throw new TypeError("Failed to fetch");
        if (next === "pending") return new Promise<Response>(() => {});
        if (next.status === 201 && next.body === undefined) {
          const selected = OPTIONS.filter((o) =>
            (body.optionIds as string[]).includes(o.id),
          ).map(({ id, name, category }) => ({ id, name, category }));
          return json(201, {
            id: "6f1c2b8e-3d4a-4c1e-9f0a-2b7d5e8c1a90",
            type: body.type,
            firstName: body.firstName,
            lastName: body.lastName,
            email: body.email,
            selectedOptions: selected,
          });
        }
        return json(
          next.status,
          next.body ?? { status: next.status, title: "Error" },
        );
      }
      return json(404, { status: 404, title: "Not Found" });
    },
  );
  vi.stubGlobal("fetch", fetchMock);
  return backend;
}
