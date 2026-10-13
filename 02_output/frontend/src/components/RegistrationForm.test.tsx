import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { FormConfig } from "../types";
import { RegistrationForm } from "./RegistrationForm";

const PRIVACY = "Privacy text";
const GENERAL = "Registration could not be completed. Please try again later.";

function config(mode: "test" | "recaptcha" = "test"): FormConfig {
  return {
    conferenceName: "C",
    options: [
      {
        id: "ws",
        name: "Shared workshop",
        category: "WORKSHOP",
        availableTo: ["EXTERNAL", "STUDENT"],
      },
      {
        id: "ext",
        name: "External event",
        category: "EVENT",
        availableTo: ["EXTERNAL"],
      },
    ],
    consents: [
      { id: "privacy", text: PRIVACY, mandatory: true },
      { id: "photos", text: "Photos", mandatory: false },
    ],
    captcha: { mode, siteKey: mode === "test" ? null : "site" },
  };
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status });
}

function stub(...responses: Array<Response | Promise<Response>>) {
  const fetchMock = vi.fn(async () => {
    const next = responses.shift();
    if (!next) throw new Error("unexpected request");
    return next;
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function sentBody(fetchMock: ReturnType<typeof stub>, call = 0) {
  const calls = fetchMock.mock.calls as unknown as Array<[string, RequestInit]>;
  return JSON.parse(String(calls[call][1].body));
}

const type = (label: string, value: string) =>
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
const click = (name: string) =>
  fireEvent.click(screen.getByRole("checkbox", { name }));
const choose = (name: string) =>
  fireEvent.click(screen.getByRole("radio", { name }));

async function submit() {
  await act(async () => {
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
  });
}

function fillExternal() {
  type("First name", "  Ana ");
  type("Last name", "Novak");
  type("Email", "ana@example.si");
  type("Organization / institution", "Org");
  click(PRIVACY);
  click("I am not a robot (test mode)");
}

describe("RegistrationForm", () => {
  it("shows no fields before a type is chosen and typed inputs afterwards", () => {
    render(<RegistrationForm config={config()} onAccepted={vi.fn()} />);
    expect(
      screen.queryByRole("button", { name: "Register" }),
    ).not.toBeInTheDocument();
    choose("External participant");
    expect(
      screen.getByRole("radio", { name: "External participant" }),
    ).toBeChecked();
    expect(screen.getByRole("radio", { name: "Student" })).not.toBeChecked();
    expect(screen.getByLabelText("Email")).toHaveAttribute("type", "email");
    expect(screen.getByLabelText("First name")).toHaveAttribute("type", "text");
    expect(screen.getByLabelText("First name")).toHaveValue("");
    expect(screen.getByLabelText("First name")).not.toHaveAttribute(
      "aria-invalid",
    );
    expect(screen.queryByTestId("error-firstName")).not.toBeInTheDocument();
  });

  it("sends trimmed own-type fields, toggled options and every consent", async () => {
    const fetchMock = stub(
      json(201, { id: "1", firstName: "Ana", lastName: "Novak", options: [] }),
    );
    const onAccepted = vi.fn();
    render(<RegistrationForm config={config()} onAccepted={onAccepted} />);
    choose("Student");
    type("First name", "keep me");
    choose("External participant");
    expect(screen.getByLabelText("First name")).toHaveValue("keep me");
    fillExternal();
    click("Shared workshop");
    click("External event");
    click("External event");

    await submit();

    expect(sentBody(fetchMock)).toEqual({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.si",
      organization: "Org",
      optionIds: ["ws"],
      consents: { privacy: true, photos: false },
      recaptchaToken: "test-pass",
    });
    expect(onAccepted).toHaveBeenCalledWith({
      id: "1",
      firstName: "Ana",
      lastName: "Novak",
      options: [],
    });
  });

  it("drops options the new type cannot select", async () => {
    const fetchMock = stub(json(500, {}));
    render(<RegistrationForm config={config()} onAccepted={vi.fn()} />);
    choose("External participant");
    click("Shared workshop");
    click("External event");
    choose("Student");
    choose("External participant");
    expect(
      screen.getByRole("checkbox", { name: "External event" }),
    ).not.toBeChecked();
    expect(
      screen.getByRole("checkbox", { name: "Shared workshop" }),
    ).toBeChecked();
    fillExternal();

    await submit();

    expect(sentBody(fetchMock).optionIds).toEqual(["ws"]);
  });

  it("marks consent and token errors next to their controls and clears errors on type change", async () => {
    stub();
    render(<RegistrationForm config={config()} onAccepted={vi.fn()} />);
    choose("External participant");

    await submit();

    const consent = screen.getByRole("checkbox", { name: PRIVACY });
    expect(consent).toHaveAttribute("aria-invalid", "true");
    expect(consent).toHaveAccessibleDescription("This consent is required.");
    expect(
      screen.getByRole("checkbox", { name: "Photos" }),
    ).not.toHaveAttribute("aria-invalid");
    expect(screen.getByTestId("error-recaptchaToken")).toHaveTextContent(
      "Please confirm that you are not a robot.",
    );
    choose("Student");
    expect(
      screen.queryByTestId("error-recaptchaToken"),
    ).not.toBeInTheDocument();
  });

  it("shows displayable backend errors, keeps the first message per field and reports others generally", async () => {
    stub(
      json(400, {
        errors: [
          { field: "consents.privacy", message: "Consent!" },
          { field: "recaptchaToken", message: "Token!" },
          { field: "email", message: "First" },
          { field: "email", message: "Second" },
          { field: "type", message: "Choose a registration type." },
        ],
      }),
    );
    render(<RegistrationForm config={config()} onAccepted={vi.fn()} />);
    choose("External participant");
    fillExternal();

    await submit();

    expect(screen.getByTestId("error-consents.privacy")).toHaveTextContent(
      "Consent!",
    );
    expect(screen.getByTestId("error-recaptchaToken")).toHaveTextContent(
      "Token!",
    );
    expect(screen.getByTestId("error-email")).toHaveTextContent("First");
    expect(screen.getByRole("alert")).toHaveTextContent(GENERAL);
  });

  it("treats an error for a field of the other type as general and shows own-type ones", async () => {
    stub(
      json(400, {
        errors: [
          { field: "organization", message: "Not allowed" },
          { field: "studentId", message: "Required" },
        ],
      }),
    );
    render(<RegistrationForm config={config()} onAccepted={vi.fn()} />);
    choose("Student");
    type("First name", "L");
    type("Last name", "K");
    type("Email", "l@x.si");
    type("Study institution", "U");
    type("Study programme", "P");
    type("Student ID", "1");
    click(PRIVACY);
    click("I am not a robot (test mode)");

    await submit();

    expect(screen.getByTestId("error-studentId")).toHaveTextContent("Required");
    expect(screen.queryByTestId("error-organization")).not.toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(GENERAL);
  });

  it("only field errors do not show the general error", async () => {
    stub(json(400, { errors: [{ field: "options", message: "Option bad" }] }));
    render(<RegistrationForm config={config()} onAccepted={vi.fn()} />);
    choose("External participant");
    fillExternal();

    await submit();

    expect(screen.getByTestId("error-options")).toHaveTextContent("Option bad");
    expect(screen.getByRole("group", { name: "Options" })).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("clears the general error when the form is submitted again", async () => {
    stub(json(500, {}));
    render(<RegistrationForm config={config()} onAccepted={vi.fn()} />);
    choose("External participant");
    fillExternal();
    await submit();
    expect(screen.getByRole("alert")).toBeInTheDocument();

    type("Email", "broken");
    await submit();

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("disables the button and sends one request while submitting", async () => {
    let finish: (r: Response) => void = () => {};
    const fetchMock = stub(
      new Promise<Response>((resolve) => (finish = resolve)),
    );
    render(<RegistrationForm config={config()} onAccepted={vi.fn()} />);
    choose("External participant");
    fillExternal();

    await submit();
    expect(screen.getByRole("button", { name: "Register" })).toBeDisabled();
    fireEvent.submit(
      screen.getByRole("button", { name: "Register" }).closest("form")!,
    );
    await act(async () => finish(json(500, {})));

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Register" })).toBeEnabled();
  });

  it("resets the Google widget after a rejected submission", async () => {
    const api = {
      params: null as null | { callback: (t: string) => void },
      ready: (cb: () => void) => cb(),
      render: vi.fn(
        (el: HTMLElement, params: { callback: (t: string) => void }) => {
          api.params = params;
          el.appendChild(document.createElement("iframe"));
          return 3;
        },
      ),
      reset: vi.fn(),
    };
    window.grecaptcha = api as unknown as Window["grecaptcha"];
    const fetchMock = stub(
      json(400, { errors: [{ field: "recaptchaToken", message: "Rejected" }] }),
    );
    try {
      render(
        <RegistrationForm config={config("recaptcha")} onAccepted={vi.fn()} />,
      );
      choose("External participant");
      type("First name", "A");
      type("Last name", "B");
      type("Email", "a@b.si");
      type("Organization / institution", "O");
      click(PRIVACY);
      await act(async () => {});
      act(() => api.params?.callback("google-token"));

      await submit();

      expect(sentBody(fetchMock).recaptchaToken).toBe("google-token");
      expect(api.reset).toHaveBeenCalledWith(3);
      await submit();
      expect(screen.getByTestId("error-recaptchaToken")).toBeInTheDocument();
      expect(fetchMock).toHaveBeenCalledTimes(1);
    } finally {
      delete window.grecaptcha;
    }
  });

  it("does not reset anything in test mode after a rejection", async () => {
    stub(json(400, { errors: [{ field: "options", message: "x" }] }));
    render(<RegistrationForm config={config()} onAccepted={vi.fn()} />);
    choose("External participant");
    fillExternal();

    await submit();

    expect(
      screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }),
    ).toBeChecked();
    expect(
      within(screen.getByTestId("options")).getAllByRole("group"),
    ).toHaveLength(2);
  });
});
