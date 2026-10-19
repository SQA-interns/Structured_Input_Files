import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "./RegistrationForm";
import { testConfig } from "./testConfig";

const accepted = {
  id: "id-1",
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  options: [],
  registeredAt: "2026-10-19T12:00:00.000Z",
};

function stubFetch(status: number, body: unknown) {
  const fetchMock = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function bodyOf(fetchMock: ReturnType<typeof stubFetch>, call = 0) {
  const args = fetchMock.mock.calls[call] as unknown as [string, RequestInit];
  return JSON.parse(args[1].body as string);
}

function textbox(name: string) {
  return screen.getByRole("textbox", { name }) as HTMLInputElement;
}

function form() {
  return document.querySelector("form") as HTMLFormElement;
}

function renderExternal() {
  render(<RegistrationForm config={testConfig} onAccepted={vi.fn()} />);
  fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
  fireEvent.change(textbox("First name"), { target: { value: "Ana" } });
  fireEvent.change(textbox("Last name"), { target: { value: "Novak" } });
  fireEvent.change(textbox("Email"), { target: { value: "ana@example.com" } });
  fireEvent.change(textbox("Organization / institution"), { target: { value: "IJS" } });
  fireEvent.click(screen.getByRole("checkbox", { name: "I agree to the processing." }));
}

describe("RegistrationForm behaviour (phase 5 survivors)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-001-08 the chosen type radio is checked and the inputs have their types", () => {
    render(<RegistrationForm config={testConfig} onAccepted={vi.fn()} />);

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    expect(screen.getByRole("radio", { name: "Student" })).toBeChecked();
    expect(screen.getByRole("radio", { name: "External participant" })).not.toBeChecked();
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
    expect(screen.getByRole("radio", { name: "External participant" })).toBeChecked();
    expect(screen.getByRole("radio", { name: "Student" })).not.toBeChecked();

    expect(textbox("Email")).toHaveAttribute("type", "email");
    expect(textbox("First name")).toHaveAttribute("type", "text");
    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    expect(textbox("Email")).toHaveAttribute("type", "email");
  });

  it("AC-001-08 unchecking one option keeps the others", () => {
    render(<RegistrationForm config={testConfig} onAccepted={vi.fn()} />);
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));

    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop A" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Lunch" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop A" }));

    expect(screen.getByRole("checkbox", { name: "Workshop A" })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Lunch" })).toBeChecked();
  });

  it("AC-004-01 nothing is selected or confirmed before the participant acts", async () => {
    const fetchMock = stubFetch(400, { message: "x", fieldErrors: [{ field: "x", message: "y" }] });
    renderExternal();

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(
      screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }),
    ).not.toBeChecked();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(bodyOf(fetchMock).optionIds).toEqual([]);
    expect(bodyOf(fetchMock).recaptchaToken).toBe("");
  });

  it("AC-004-01 the test-mode check box can be unchecked again", async () => {
    const fetchMock = stubFetch(201, accepted);
    renderExternal();
    const robot = screen.getByRole("checkbox", { name: "I am not a robot (test mode)" });

    fireEvent.click(robot);
    expect(robot).toBeChecked();
    fireEvent.click(robot);
    expect(robot).not.toBeChecked();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(bodyOf(fetchMock).recaptchaToken).toBe("");
  });

  it("AC-004-01 the form submit is handled in the page", () => {
    stubFetch(201, accepted);
    renderExternal();

    expect(fireEvent.submit(form())).toBe(false);
  });

  it("AC-004-03 a submit before choosing a type sends nothing", () => {
    const fetchMock = stubFetch(201, accepted);
    render(<RegistrationForm config={testConfig} onAccepted={vi.fn()} />);

    expect(() => fireEvent.submit(form())).not.toThrow();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("AC-004-01 a second submit while the first is pending sends nothing", async () => {
    let finish: (value: Response) => void = () => undefined;
    const fetchMock = vi.fn(() => new Promise<Response>((resolve) => (finish = resolve)));
    vi.stubGlobal("fetch", fetchMock);
    renderExternal();

    fireEvent.submit(form());
    fireEvent.submit(form());

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Register" })).toBeDisabled();
    finish(new Response(JSON.stringify(accepted), { status: 201 }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Register" })).toBeEnabled());
  });

  it("AC-004-04 after a failure the participant can submit again", async () => {
    const fetchMock = stubFetch(500, { message: "x", fieldErrors: [] });
    renderExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await screen.findByRole("alert");
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
  });

  it("AC-004-04 in test mode a failure keeps the robot check", async () => {
    stubFetch(500, { message: "x", fieldErrors: [] });
    renderExternal();
    fireEvent.click(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await screen.findByRole("alert");
    expect(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" })).toBeChecked();
  });

  it("AC-004-03 client errors after a failure replace the general message", async () => {
    stubFetch(500, { message: "x", fieldErrors: [] });
    renderExternal();
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    await screen.findByRole("alert");

    fireEvent.change(textbox("First name"), { target: { value: "" } });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(textbox("First name")).toHaveAttribute("aria-invalid", "true");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("AC-004-02 known and unknown backend errors show both the field and the general message", async () => {
    stubFetch(400, {
      message: "x",
      fieldErrors: [
        { field: "email", message: "Email taken" },
        { field: "type", message: "Bad type" },
      ],
    });
    renderExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(textbox("Email")).toHaveAccessibleDescription("Email taken");
  });

  it("AC-004-03 switching the type clears the shown errors", () => {
    stubFetch(201, accepted);
    render(<RegistrationForm config={testConfig} onAccepted={vi.fn()} />);
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(textbox("First name")).toHaveAttribute("aria-invalid", "true");

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));

    expect(textbox("First name")).not.toHaveAttribute("aria-invalid");
    expect(document.getElementById("error-firstName")).toBeNull();
  });
});
