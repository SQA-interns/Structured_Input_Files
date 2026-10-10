import { fireEvent, render, screen, within } from "@testing-library/react";
import { vi } from "vitest";
import App from "../../src/App";

/** A backend double at the network boundary, shaped by docs/02_contracts/openapi.yaml. */
export interface RecordedRequest {
  url: string;
  method: string;
  body: unknown;
}

export const formConfig = {
  conferenceName: "Testna konferenca 2026",
  options: [
    {
      id: "ws-ai",
      name: "Delavnica: umetna inteligenca",
      category: "WORKSHOP",
      audience: ["EXTERNAL", "STUDENT"],
    },
    { id: "ev-gala", name: "Gala večerja", category: "EVENT", audience: ["EXTERNAL"] },
    { id: "ev-student-night", name: "Študentski večer", category: "EVENT", audience: ["STUDENT"] },
    { id: "meal-lunch", name: "Kosilo", category: "MEAL", audience: ["EXTERNAL", "STUDENT"] },
    {
      id: "other-tour",
      name: "Ogled Ljubljane",
      category: "OTHER",
      audience: ["EXTERNAL", "STUDENT"],
    },
  ],
  consents: [
    {
      id: "data-processing",
      text: "Strinjam se z obdelavo osebnih podatkov za organizacijo konference.",
      mandatory: true,
    },
    { id: "photos", text: "Strinjam se s fotografiranjem na dogodku.", mandatory: false },
  ],
  recaptcha: { siteKey: "", testMode: true },
};

export type Reply = { status: number; body: unknown } | "network-error";

export function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** Installs a fetch double; returns the recorded requests. */
export function mockBackend(
  options: { config?: unknown; registration?: Reply } = {},
): RecordedRequest[] {
  const requests: RecordedRequest[] = [];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
    const method = (init?.method ?? "GET").toUpperCase();
    const body = typeof init?.body === "string" ? JSON.parse(init.body) : undefined;
    requests.push({ url, method, body });
    if (url.endsWith("/api/form-config") && method === "GET") {
      return jsonResponse(200, options.config ?? formConfig);
    }
    if (url.endsWith("/api/registrations") && method === "POST") {
      const reply = options.registration ?? { status: 500, body: {} };
      if (reply === "network-error") {
        throw new TypeError("Failed to fetch");
      }
      return jsonResponse(reply.status, reply.body);
    }
    return jsonResponse(404, {
      status: 404,
      error: "NOT_FOUND",
      message: "Not found",
      fieldErrors: [],
    });
  });
  vi.stubGlobal("fetch", fetchMock);
  return requests;
}

export function registrationPosts(requests: RecordedRequest[]): RecordedRequest[] {
  return requests.filter((r) => r.method === "POST" && r.url.endsWith("/api/registrations"));
}

export function renderApp() {
  return render(<App />);
}

/** Chooses the registration type in the radio group named "Registration type". */
export async function chooseType(label: "External participant" | "Student") {
  const group = await screen.findByRole("radiogroup", { name: "Registration type" });
  fireEvent.click(within(group).getByRole("radio", { name: label }));
}

export function fill(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

export function check(label: string) {
  fireEvent.click(screen.getByRole("checkbox", { name: label }));
}

export function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

export function errorOf(fieldId: string): HTMLElement | null {
  return document.getElementById(`${fieldId}-error`);
}

export function fillValidExternal() {
  fill("First name", "Ana");
  fill("Last name", "Novak");
  fill("Email", "ana.novak@example.si");
  fill("Organization / institution", "Institut Jožef Stefan");
  check("Delavnica: umetna inteligenca");
  check("Kosilo");
  check(formConfig.consents[0].text);
  check("I am not a robot (test mode)");
}

export const accepted = {
  reference: "3f2b8c1e-6a4d-4e2b-9f1a-0c5d7e8f9a10",
  submittedAt: "2026-10-11T08:00:00.000Z",
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  options: [
    { id: "ws-ai", name: "Delavnica: umetna inteligenca", category: "WORKSHOP" },
    { id: "meal-lunch", name: "Kosilo", category: "MEAL" },
  ],
};
