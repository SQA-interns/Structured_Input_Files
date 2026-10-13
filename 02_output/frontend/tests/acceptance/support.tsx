/**
 * Harness of the frontend acceptance tests: the page is driven only through
 * `docs/02_contracts/ui-contract.md` (roles, labels, test ids) and the backend
 * is replaced by a stub of the global `fetch` that follows `openapi.yaml`.
 */
import { fireEvent, render, screen } from "@testing-library/react";
import { vi } from "vitest";
import { App } from "../../src/App";

export const formConfig = {
  conferenceName: "Acceptance Conference 2026",
  options: [
    {
      id: "ws-a",
      name: "Workshop A",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ws-ext",
      name: "Workshop for externals",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL"],
    },
    {
      id: "ev-a",
      name: "Evening event",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "meal-a",
      name: "Lunch",
      category: "MEAL",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ot-a",
      name: "City tour",
      category: "OTHER",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ot-stu",
      name: "Student meetup",
      category: "OTHER",
      availableTo: ["STUDENT"],
    },
  ],
  consents: [
    {
      id: "privacy",
      text: "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers.",
      mandatory: true,
    },
    {
      id: "photos",
      text: "I agree to appear in conference photos.",
      mandatory: false,
    },
  ],
  captcha: { mode: "test", siteKey: null },
};

export const PRIVACY_TEXT = formConfig.consents[0].text;
export const TEST_CAPTCHA_LABEL = "I am not a robot (test mode)";
export const GENERAL_SUBMIT_ERROR =
  "Registration could not be completed. Please try again later.";

export function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

type RegistrationHandler = (
  body: Record<string, unknown>,
) => Response | Promise<Response>;

/** Stubs the global fetch; returns the mock so tests can inspect the calls. */
export function stubBackend(registration: RegistrationHandler) {
  const fetchMock = vi.fn(
    async (input: RequestInfo | URL, init?: RequestInit) => {
      const url =
        typeof input === "string"
          ? input
          : input instanceof URL
            ? input.href
            : input.url;
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.endsWith("/api/form-config") && method === "GET") {
        return jsonResponse(200, formConfig);
      }
      if (url.endsWith("/api/registrations") && method === "POST") {
        return registration(
          JSON.parse(String(init?.body)) as Record<string, unknown>,
        );
      }
      return jsonResponse(404, { message: "Not found" });
    },
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

/** Calls of the stub that submitted a registration. */
export function registrationCalls(fetchMock: ReturnType<typeof stubBackend>) {
  return fetchMock.mock.calls.filter(([input, init]) => {
    const url = typeof input === "string" ? input : String(input);
    return (
      url.endsWith("/api/registrations") &&
      (init?.method ?? "GET").toUpperCase() === "POST"
    );
  });
}

/** Renders the app and waits until the type choice is shown. */
export async function renderPage() {
  render(<App />);
  await screen.findByRole("radio", { name: "External participant" });
}

export function chooseType(label: "External participant" | "Student") {
  fireEvent.click(screen.getByRole("radio", { name: label }));
}

export function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

export function check(label: string) {
  fireEvent.click(screen.getByRole("checkbox", { name: label }));
}

/** Fills every external field with valid values, selects Workshop A, gives the mandatory consent and the test token. */
export function fillValidExternal() {
  type("First name", "Ana");
  type("Last name", "Novak");
  type("Email", "ana.novak@example.si");
  type("Organization / institution", "Institut Primer");
  check("Workshop A");
  check(PRIVACY_TEXT);
  check(TEST_CAPTCHA_LABEL);
}

export function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}
