import { render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../App";
import { acceptedExternal, installFakeBackend, PRIVACY_TEXT } from "./support/fakeBackend";
import { chooseType, fillValidExternal, submit, textbox, type, check } from "./support/form";

const GENERAL_ERROR = "Registration could not be completed. Please try again later.";

describe("US-004 registration confirmation and errors (ui.md)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-004-01 accepted registration shows a confirmation with name and options and hides the form", async () => {
    const backend = installFakeBackend(acceptedExternal);
    render(<App />);
    await fillValidExternal();

    submit();

    const confirmation = await screen.findByRole("status");
    expect(
      within(confirmation).getByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
    expect(confirmation).toHaveTextContent("Ana");
    expect(confirmation).toHaveTextContent("Novak");
    const items = within(confirmation)
      .getAllByRole("listitem")
      .map((item) => item.textContent?.trim());
    expect(items).toEqual(expect.arrayContaining(["Workshop: AI in practice", "Lunch"]));
    expect(screen.queryByRole("button", { name: "Register" })).not.toBeInTheDocument();
    expect(screen.queryAllByRole("textbox")).toHaveLength(0);

    const sent = backend.registrationRequests();
    expect(sent).toHaveLength(1);
    expect(sent[0].body).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.com",
      organization: "Institut Jozef Stefan",
      consentIds: ["privacy"],
      recaptchaToken: "test-pass",
    });
    expect((sent[0].body as { optionIds: string[] }).optionIds).toEqual(
      expect.arrayContaining(["ws-ai", "meal-lunch"]),
    );
  });

  it("AC-004-02 backend field errors are shown next to their fields and values remain", async () => {
    installFakeBackend({
      kind: "json",
      status: 400,
      body: {
        message: "Validation failed",
        fieldErrors: [
          { field: "email", message: "Email address is not accepted" },
          { field: "optionIds", message: "Option is not available" },
        ],
      },
    });
    render(<App />);
    await fillValidExternal();

    submit();

    await waitFor(() =>
      expect(textbox("Email")).toHaveAccessibleDescription("Email address is not accepted"),
    );
    expect(textbox("Email")).toHaveAttribute("aria-invalid", "true");
    expect(document.getElementById("error-email")).toHaveTextContent(
      "Email address is not accepted",
    );
    expect(document.getElementById("error-optionIds")).toHaveTextContent("Option is not available");
    expect(document.querySelector('[aria-describedby~="error-optionIds"]')).not.toBeNull();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(textbox("First name")).toHaveValue("Ana");
    expect(textbox("Email")).toHaveValue("ana.novak@example.com");
    expect(textbox("Organization / institution")).toHaveValue("Institut Jozef Stefan");
    expect(screen.getByRole("checkbox", { name: PRIVACY_TEXT })).toBeChecked();
  });

  it("AC-004-03 empty required field and invalid email are shown without sending a request", async () => {
    const backend = installFakeBackend(acceptedExternal);
    render(<App />);
    await chooseType("External participant");
    type("Last name", "Novak");
    type("Email", "ana.novak@");
    type("Organization / institution", "Institut Jozef Stefan");
    check(PRIVACY_TEXT);
    check("I am not a robot (test mode)");

    submit();

    await waitFor(() => expect(textbox("First name")).toHaveAttribute("aria-invalid", "true"));
    expect(document.getElementById("error-firstName")?.textContent?.trim()).not.toBe("");
    expect(textbox("First name")).toHaveAttribute("aria-describedby", "error-firstName");
    expect(textbox("Email")).toHaveAttribute("aria-invalid", "true");
    expect(document.getElementById("error-email")?.textContent?.trim()).not.toBe("");
    expect(textbox("Email")).toHaveAttribute("aria-describedby", "error-email");
    expect(backend.registrationRequests()).toHaveLength(0);
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("AC-004-04 backend failure shows a general error without technical details", async () => {
    installFakeBackend({
      kind: "json",
      status: 500,
      body: { message: "java.sql.SQLException: connection refused", fieldErrors: [] },
    });
    render(<App />);
    await fillValidExternal();

    submit();

    const alert = await screen.findByRole("alert");
    expect(alert.textContent?.trim()).toBe(GENERAL_ERROR);
    expect(screen.queryByText(/SQLException/)).not.toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(textbox("First name")).toHaveValue("Ana");
  });

  it("AC-004-04 unreachable backend shows a general error without technical details", async () => {
    installFakeBackend({ kind: "network-error" });
    render(<App />);
    await fillValidExternal();

    submit();

    const alert = await screen.findByRole("alert");
    expect(alert.textContent?.trim()).toBe(GENERAL_ERROR);
    expect(screen.queryByText(/Failed to fetch/)).not.toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(textbox("Email")).toHaveValue("ana.novak@example.com");
  });
});
