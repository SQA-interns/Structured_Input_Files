import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import {
  GENERAL_ERROR,
  chooseType,
  fieldError,
  fill,
  fillValidExternal,
  formConfig,
  registrationCalls,
  stubBackend,
  submit,
  textbox,
} from "./support";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

const created = {
  id: "8f6c2a52-1f3e-4f43-9d0c-5b7c1d2e3f40",
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  options: [
    { id: "ws-testing", name: "Workshop: Software testing", category: "WORKSHOP" },
    { id: "meal-lunch", name: "Lunch", category: "MEAL" },
  ],
  registeredAt: "2026-10-12T10:00:00Z",
};

describe("US-004 registration confirmation", () => {
  it("AC-004-01 accepted registration shows the confirmation with name and options and removes the form", async () => {
    const fetchMock = stubBackend({ status: 201, body: created });
    render(<App />);
    await fillValidExternal();

    submit();

    expect(
      await screen.findByRole("heading", { name: "Registration confirmed" }),
    ).toBeInTheDocument();
    expect(document.body).toHaveTextContent("Ana Novak");
    expect(document.body).toHaveTextContent("Workshop: Software testing");
    expect(document.body).toHaveTextContent("Lunch");
    expect(screen.queryByRole("button", { name: "Register" })).toBeNull();
    expect(screen.queryByRole("textbox", { name: "First name" })).toBeNull();

    const calls = registrationCalls(fetchMock);
    expect(calls).toHaveLength(1);
    const sent = JSON.parse(String(calls[0][1]?.body));
    expect(sent).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.org",
      organization: "Institut Example",
      consents: ["privacy"],
      recaptchaToken: "test-pass",
    });
    expect([...sent.optionIds].sort()).toEqual(["meal-lunch", "ws-testing"]);
  });

  it("AC-004-02 backend field errors are shown next to their fields and the values remain", async () => {
    stubBackend({
      status: 400,
      body: {
        message: "Validation failed",
        fieldErrors: [
          { field: "email", message: "Email address is not accepted." },
          { field: "optionIds", message: "An option is not available." },
          { field: "consents.privacy", message: "This consent is required." },
        ],
      },
    });
    render(<App />);
    await fillValidExternal();

    submit();

    expect(await screen.findByText("Email address is not accepted.")).toBeInTheDocument();
    expect(fieldError("email")).toHaveTextContent("Email address is not accepted.");
    expect(textbox("Email")).toHaveAttribute("aria-invalid", "true");
    expect(textbox("Email").getAttribute("aria-describedby")).toContain("email-error");
    expect(fieldError("optionIds")).toHaveTextContent("An option is not available.");
    expect(fieldError("consents.privacy")).toHaveTextContent("This consent is required.");
    const consent = screen.getByRole("checkbox", { name: formConfig.consents[0].text });
    expect(consent.getAttribute("aria-describedby")).toContain("consents.privacy-error");

    expect(screen.queryByRole("heading", { name: "Registration confirmed" })).toBeNull();
    expect(textbox("First name")).toHaveValue("Ana");
    expect(textbox("Last name")).toHaveValue("Novak");
    expect(textbox("Email")).toHaveValue("ana.novak@example.org");
    expect(textbox("Organization / institution")).toHaveValue("Institut Example");
    expect(screen.getByRole("checkbox", { name: "Workshop: Software testing" })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Lunch" })).toBeChecked();
  });

  it("AC-004-03 empty required field shows a field error and sends no request", async () => {
    const fetchMock = stubBackend();
    render(<App />);
    await fillValidExternal();
    fill("First name", "");
    fill("Last name", "   ");

    submit();

    expect(await screen.findAllByText("This field is required.")).toHaveLength(2);
    expect(fieldError("firstName")).toHaveTextContent("This field is required.");
    expect(fieldError("lastName")).toHaveTextContent("This field is required.");
    expect(textbox("First name")).toHaveAttribute("aria-invalid", "true");
    expect(registrationCalls(fetchMock)).toHaveLength(0);
  });

  it("AC-004-03 invalid email shows a field error and sends no request", async () => {
    const fetchMock = stubBackend();
    render(<App />);
    await fillValidExternal();
    fill("Email", "ana.novak@");

    submit();

    expect(await screen.findByText("Enter a valid email address.")).toBeInTheDocument();
    expect(fieldError("email")).toHaveTextContent("Enter a valid email address.");
    expect(registrationCalls(fetchMock)).toHaveLength(0);
  });

  it("AC-004-03 student form: empty student ID shows a field error and sends no request", async () => {
    const fetchMock = stubBackend();
    render(<App />);
    await chooseType("Student");
    fill("First name", "Luka");
    fill("Last name", "Kranjc");
    fill("Email", "luka@student.example.org");
    fill("Study institution", "Univerza Example");
    fill("Study programme", "Computer Science");

    submit();

    expect(await screen.findByText("This field is required.")).toBeInTheDocument();
    expect(fieldError("studentId")).toHaveTextContent("This field is required.");
    expect(registrationCalls(fetchMock)).toHaveLength(0);
  });

  it("AC-004-04 backend failure shows a general error without technical details", async () => {
    stubBackend({
      status: 500,
      body: { message: "java.sql.SQLException: connection refused at 10.0.0.5" },
    });
    render(<App />);
    await fillValidExternal();

    submit();

    const alert = await screen.findByText(GENERAL_ERROR);
    expect(alert).toBeInTheDocument();
    expect(document.body).not.toHaveTextContent("SQLException");
    expect(document.body).not.toHaveTextContent("500");
    expect(screen.queryByRole("heading", { name: "Registration confirmed" })).toBeNull();
    expect(textbox("First name")).toHaveValue("Ana");
  });

  it("AC-004-04 unreachable backend shows a general error without technical details", async () => {
    stubBackend(new TypeError("Failed to fetch"));
    render(<App />);
    await fillValidExternal();

    submit();

    expect(await screen.findByText(GENERAL_ERROR)).toBeInTheDocument();
    expect(document.body).not.toHaveTextContent("Failed to fetch");
    expect(screen.queryByRole("heading", { name: "Registration confirmed" })).toBeNull();
    expect(screen.getByRole("button", { name: "Register" })).toBeInTheDocument();
  });
});
