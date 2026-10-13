import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import App, { CONFIG_ERROR } from "./App";
import { Confirmation } from "./Confirmation";
import { GENERAL_ERROR } from "./RegistrationForm";
import type { FormConfig } from "./types";

const config: FormConfig = {
  conferenceName: "Konferenca 2026",
  recaptchaSiteKey: "",
  recaptchaTestMode: true,
  consents: [{ id: "privacy", text: "I agree", mandatory: true }],
  options: [
    {
      id: "ws",
      name: "Workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "ext", name: "Dinner", category: "EVENT", availableTo: ["EXTERNAL"] },
  ],
};

type Handler = (
  url: string,
  init?: RequestInit,
) => Response | Promise<Response>;

function backend(onPost: Handler) {
  const calls: { url: string; init?: RequestInit }[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, init?: RequestInit) => {
      calls.push({ url, init });
      return url === "/api/form-config"
        ? new Response(JSON.stringify(config))
        : onPost(url, init);
    }),
  );
  return calls;
}

const posted = (calls: { url: string; init?: RequestInit }[]) =>
  calls
    .filter((c) => c.url === "/api/registrations")
    .map((c) => JSON.parse(c.init!.body as string));

const field = (label: string) =>
  screen.getByLabelText(label, { selector: "input" });
const type = (label: string, value: string) =>
  fireEvent.change(field(label), { target: { value } });

async function fillStudent() {
  fireEvent.click(await screen.findByRole("radio", { name: "Student" }));
  type("First name", " Bor ");
  type("Last name", "Kranjc");
  type("Email", "bor@example.com");
  type("Study institution", "Uni");
  type("Study programme", "CS");
  type("Student ID", "123");
  fireEvent.click(screen.getByRole("checkbox", { name: "I agree" }));
  fireEvent.click(
    screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }),
  );
}

describe("App", () => {
  it("shows loading, then the conference name and the form", async () => {
    backend(() => new Response("{}", { status: 201 }));
    render(<App />);
    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(
      await screen.findByRole("heading", { level: 1, name: "Konferenca 2026" }),
    ).toBeVisible();
    expect(
      screen.getByRole("form", { name: "Registration form" }),
    ).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).toBeNull();
  });

  it("shows only a general message when the configuration cannot be loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("boom", { status: 500 })),
    );
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(CONFIG_ERROR);
    expect(CONFIG_ERROR).toBe(
      "The registration form is currently unavailable. Please try again later.",
    );
    expect(screen.queryByRole("form")).toBeNull();
    expect(screen.queryByText("boom")).toBeNull();
  });

  it("does not update after unmount", async () => {
    let resolve: (r: Response) => void = () => {};
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise<Response>((r) => (resolve = r))),
    );
    const error = vi.spyOn(console, "error");
    const { unmount } = render(<App />);
    unmount();
    resolve(new Response(JSON.stringify(config)));
    await new Promise((r) => setTimeout(r, 0));
    expect(error).not.toHaveBeenCalled();
  });

  it("asks for a registration type before anything else", async () => {
    const calls = backend(() => new Response("{}", { status: 201 }));
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Register" }));
    expect(
      screen.getByRole("group", { name: "Registration type" }),
    ).toHaveAccessibleDescription("Choose a registration type.");
    expect(posted(calls)).toHaveLength(0);
  });

  it("sends only the student fields, trimmed, and drops options of the other type", async () => {
    const calls = backend(
      () =>
        new Response(JSON.stringify({ id: "x", registeredAt: "t" }), {
          status: 201,
        }),
    );
    render(<App />);
    fireEvent.click(
      await screen.findByRole("radio", { name: "External participant" }),
    );
    type("Organization / institution", "Acme");
    fireEvent.click(screen.getByRole("checkbox", { name: "Dinner" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop" }));
    await fillStudent();
    expect(screen.queryByRole("checkbox", { name: "Dinner" })).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Thank you, Bor Kranjc.",
    );
    expect(posted(calls)).toEqual([
      {
        type: "STUDENT",
        firstName: "Bor",
        lastName: "Kranjc",
        email: "bor@example.com",
        studyInstitution: "Uni",
        studyProgramme: "CS",
        studentId: "123",
        optionIds: ["ws"],
        consents: ["privacy"],
        recaptchaToken: "test-mode-token",
      },
    ]);
  });

  it("unticking removes options and consents; the button is disabled while sending", async () => {
    let finish: (r: Response) => void = () => {};
    const calls = backend(() => new Promise<Response>((r) => (finish = r)));
    render(<App />);
    await fillStudent();
    const workshop = screen.getByRole("checkbox", { name: "Workshop" });
    fireEvent.click(workshop);
    fireEvent.click(workshop);
    expect(workshop).not.toBeChecked();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Register" })).toBeDisabled(),
    );
    finish(new Response("{}", { status: 503 }));
    expect(await screen.findByRole("alert")).toHaveTextContent(GENERAL_ERROR);
    expect(screen.getByRole("button", { name: "Register" })).toBeEnabled();
    expect(posted(calls)[0].optionIds).toEqual([]);
    expect(
      screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }),
    ).not.toBeChecked();

    fireEvent.click(screen.getByRole("checkbox", { name: "I agree" }));
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(screen.queryByRole("alert")).toBeNull();
    expect(field("Email")).not.toHaveAttribute("aria-invalid");
    expect(
      screen.getByRole("checkbox", { name: "I agree" }),
    ).toHaveAccessibleDescription("This consent is required.");
  });

  it("shows unknown-consent and type errors from the backend", async () => {
    backend(
      () =>
        new Response(
          JSON.stringify({
            fieldErrors: {
              consents: "Unknown consent.",
              type: "Choose a registration type.",
            },
          }),
          { status: 400 },
        ),
    );
    render(<App />);
    await fillStudent();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(await screen.findByText("Unknown consent.")).toHaveAttribute(
      "id",
      "consents-error",
    );
    expect(
      screen.getByRole("group", { name: "Registration type" }),
    ).toHaveAttribute("aria-invalid", "true");
  });
});

describe("Confirmation", () => {
  it("says when no option was selected", () => {
    render(
      <Confirmation
        registered={{ firstName: "A", lastName: "B", optionNames: [] }}
      />,
    );
    const status = screen.getByRole("status");
    expect(
      within(status).getByRole("heading", { name: "Registration received" }),
    ).toBeVisible();
    expect(status).toHaveTextContent("No options selected.");
    expect(within(status).queryByRole("list")).toBeNull();
  });
});
