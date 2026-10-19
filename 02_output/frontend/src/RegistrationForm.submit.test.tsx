import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "./RegistrationForm";
import { testConfig } from "./testConfig";

const accepted = {
  id: "id-1",
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  options: [{ id: "ws-a", name: "Workshop A", category: "WORKSHOP" }],
  registeredAt: "2026-10-19T12:00:00.000Z",
};

function backend(status: number, body: unknown) {
  const fetchMock = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function sentBody(fetchMock: ReturnType<typeof backend>) {
  const init = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
  return JSON.parse(init[1].body as string);
}

function textbox(name: string) {
  return screen.getByRole("textbox", { name }) as HTMLInputElement;
}

function fillExternal(onAccepted = vi.fn()) {
  render(<RegistrationForm config={testConfig} onAccepted={onAccepted} />);
  fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
  fireEvent.change(textbox("First name"), { target: { value: " Ana " } });
  fireEvent.change(textbox("Last name"), { target: { value: "Novak" } });
  fireEvent.change(textbox("Email"), { target: { value: "ana@example.com" } });
  fireEvent.change(textbox("Organization / institution"), { target: { value: "IJS" } });
  fireEvent.click(screen.getByRole("checkbox", { name: "Workshop A" }));
  fireEvent.click(screen.getByRole("checkbox", { name: "I agree to the processing." }));
  fireEvent.click(screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }));
  return onAccepted;
}

function register() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

describe("RegistrationForm submission", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-004-01 a valid form sends the type's fields and reports the accepted registration", async () => {
    const fetchMock = backend(201, accepted);
    const onAccepted = fillExternal();

    register();

    await waitFor(() => expect(onAccepted).toHaveBeenCalledWith(accepted));
    expect(sentBody(fetchMock)).toEqual({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.com",
      organization: "IJS",
      optionIds: ["ws-a"],
      consentIds: ["privacy"],
      recaptchaToken: "test-pass",
    });
  });

  it("AC-004-01 a second click while sending sends nothing more", async () => {
    const fetchMock = backend(201, accepted);
    const onAccepted = fillExternal();

    register();
    register();

    await waitFor(() => expect(onAccepted).toHaveBeenCalled());
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("AC-004-03 errors are shown next to the fields and nothing is sent", async () => {
    const fetchMock = backend(201, accepted);
    fillExternal();
    fireEvent.change(textbox("First name"), { target: { value: " " } });
    fireEvent.change(textbox("Email"), { target: { value: "ana@" } });
    fireEvent.click(screen.getByRole("checkbox", { name: "I agree to the processing." }));

    register();

    expect(textbox("First name")).toHaveAttribute("aria-invalid", "true");
    expect(textbox("First name")).toHaveAccessibleDescription("First name is required.");
    expect(textbox("First name").nextElementSibling).toHaveAttribute("id", "error-firstName");
    expect(textbox("Email")).toHaveAccessibleDescription("Please enter a valid email address.");
    expect(
      screen.getByRole("checkbox", { name: "I agree to the processing." }),
    ).toHaveAccessibleDescription("This consent is required.");
    expect(textbox("Last name")).not.toHaveAttribute("aria-invalid");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("AC-004-03 fixed errors disappear on the next submit", async () => {
    backend(201, accepted);
    fillExternal();
    fireEvent.change(textbox("First name"), { target: { value: "" } });
    register();
    expect(textbox("First name")).toHaveAttribute("aria-invalid", "true");

    fireEvent.change(textbox("First name"), { target: { value: "Ana" } });
    register();

    await waitFor(() => expect(textbox("First name")).not.toHaveAttribute("aria-invalid"));
    expect(document.getElementById("error-firstName")).toBeNull();
  });

  it("AC-004-02 backend field errors are shown next to their fields and values remain", async () => {
    backend(400, {
      message: "x",
      fieldErrors: [
        { field: "organization", message: "Too long" },
        { field: "optionIds", message: "Option not available" },
        { field: "consents.privacy", message: "Consent missing" },
        { field: "recaptchaToken", message: "Robot check failed" },
      ],
    });
    const onAccepted = fillExternal();

    register();

    await waitFor(() =>
      expect(textbox("Organization / institution")).toHaveAccessibleDescription("Too long"),
    );
    const options = document.querySelector('[aria-describedby~="error-optionIds"]');
    expect(options).not.toBeNull();
    expect(options).toHaveAccessibleDescription("Option not available");
    expect(
      screen.getByRole("checkbox", { name: "I agree to the processing." }),
    ).toHaveAccessibleDescription("Consent missing");
    expect(document.getElementById("error-recaptchaToken")).toHaveTextContent("Robot check failed");
    expect(textbox("First name")).toHaveValue(" Ana ");
    expect(onAccepted).not.toHaveBeenCalled();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("AC-004-04 an error for an unknown field shows the general message", async () => {
    backend(400, { message: "x", fieldErrors: [{ field: "somethingElse", message: "Nope" }] });
    fillExternal();

    register();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Registration could not be completed. Please try again later.",
    );
  });

  it("AC-004-04 a failed request shows the general message and keeps the form", async () => {
    backend(500, { message: "java.lang.NullPointerException", fieldErrors: [] });
    const onAccepted = fillExternal();

    register();

    const alert = await screen.findByRole("alert");
    expect(alert.textContent).toBe("Registration could not be completed. Please try again later.");
    expect(onAccepted).not.toHaveBeenCalled();
    expect(textbox("Last name")).toHaveValue("Novak");
    expect(screen.getByRole("button", { name: "Register" })).toBeEnabled();
  });
});
