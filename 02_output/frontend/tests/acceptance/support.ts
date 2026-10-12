import { fireEvent, screen, within } from "@testing-library/react";
import { vi } from "vitest";

/** A form configuration as the backend returns it (docs/02_contracts/openapi.json FormConfig). */
export const formConfig = {
  conferenceName: "Test Conference 2026",
  recaptcha: { siteKey: "", testMode: true },
  options: [
    {
      id: "ws-testing",
      name: "Workshop: Software testing",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    {
      id: "ws-security",
      name: "Workshop: Secure coding",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL"],
    },
    {
      id: "ev-opening",
      name: "Opening reception",
      category: "EVENT",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "meal-lunch", name: "Lunch", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "meal-dinner", name: "Conference dinner", category: "MEAL", availableTo: ["EXTERNAL"] },
    { id: "oth-tour", name: "City tour", category: "OTHER", availableTo: ["EXTERNAL", "STUDENT"] },
  ],
  consents: [
    {
      id: "privacy",
      text: "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers.",
      mandatory: true,
    },
    { id: "photo", text: "I agree to be photographed at the conference.", mandatory: false },
  ],
};

export const EXTERNAL_FIELDS = ["First name", "Last name", "Email", "Organization / institution"];
export const STUDENT_FIELDS = [
  "First name",
  "Last name",
  "Email",
  "Study institution",
  "Study programme",
  "Student ID",
];
export const GROUPS = ["Workshops", "Events", "Meals", "Other activities"];
export const GENERAL_ERROR = "The registration could not be completed. Please try again later.";

type Reply = { status: number; body?: unknown } | Error;

/**
 * Stubs window.fetch: GET /api/form-config answers formConfig, POST /api/registrations answers
 * the given reply. Returns the mock so tests can inspect the calls.
 */
export function stubBackend(registrationReply: Reply = { status: 201, body: {} }) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
    const method = (init?.method ?? "GET").toUpperCase();
    if (url.endsWith("/api/form-config") && method === "GET") {
      return jsonResponse(200, formConfig);
    }
    if (url.endsWith("/api/registrations") && method === "POST") {
      if (registrationReply instanceof Error) {
        throw registrationReply;
      }
      return jsonResponse(registrationReply.status, registrationReply.body ?? {});
    }
    return jsonResponse(404, { message: "Not found" });
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** POST calls to /api/registrations recorded by the stub. */
export function registrationCalls(fetchMock: ReturnType<typeof stubBackend>) {
  return fetchMock.mock.calls.filter(([input, init]) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
    return url.endsWith("/api/registrations") && (init?.method ?? "GET").toUpperCase() === "POST";
  });
}

export async function chooseType(name: "External participant" | "Student") {
  fireEvent.click(await screen.findByRole("radio", { name }));
}

export function textbox(name: string): HTMLInputElement {
  return screen.getByRole("textbox", { name }) as HTMLInputElement;
}

export function fill(name: string, value: string) {
  fireEvent.change(textbox(name), { target: { value } });
}

export function check(name: string) {
  fireEvent.click(screen.getByRole("checkbox", { name }));
}

export function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

/** Fills a complete valid external form (options Workshop: Software testing and Lunch). */
export async function fillValidExternal() {
  await chooseType("External participant");
  fill("First name", "Ana");
  fill("Last name", "Novak");
  fill("Email", "ana.novak@example.org");
  fill("Organization / institution", "Institut Example");
  check("Workshop: Software testing");
  check("Lunch");
  check(formConfig.consents[0].text);
  check("I am not a robot (test mode)");
}

/** Accessible names of all text boxes on the page, in document order. */
export function textboxNames(): string[] {
  return screen
    .queryAllByRole("textbox")
    .map((el) => el.getAttribute("aria-label") ?? labelOf(el as HTMLInputElement));
}

function labelOf(el: HTMLInputElement): string {
  const label = el.labels?.[0];
  return label ? (label.textContent ?? "").trim() : "";
}

/** Names of the option checkboxes inside the group with the given legend. */
export function optionsInGroup(group: string): string[] {
  const element = screen.getByRole("group", { name: group });
  return within(element)
    .getAllByRole("checkbox")
    .map((el) => labelOf(el as HTMLInputElement));
}

/** The error element of a field (docs/02_contracts/ui-registration-form.json fieldError). */
export function fieldError(field: string): HTMLElement | null {
  return document.getElementById(`${field}-error`);
}
