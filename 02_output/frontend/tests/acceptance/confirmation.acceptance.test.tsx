import { render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "../../src/App";
import {
  PRIVACY_TEXT,
  created,
  installFakeApi,
  jsonResponse,
} from "./support/fakeApi";
import { chooseType, fillValidExternal, submit, type } from "./support/form";

const GENERAL_ERROR =
  "The registration could not be completed. Please try again later.";

describe("US-004 registration confirmation", () => {
  it("AC-004-01 shows the confirmation with name and selected options and hides the form", async () => {
    const api = installFakeApi(() => created());
    render(<App />);
    await fillValidExternal();

    submit();

    const status = await screen.findByRole("status");
    expect(
      within(status).getByRole("heading", { name: "Registration received" }),
    ).toBeVisible();
    expect(status).toHaveTextContent("Thank you, Ana Novak.");
    const list = within(status).getByRole("list", { name: "Selected options" });
    expect(
      within(list)
        .getAllByRole("listitem")
        .map((item) => item.textContent?.trim()),
    ).toEqual(["Secure coding workshop", "Lunch"]);
    expect(
      screen.queryByRole("form", { name: "Registration form" }),
    ).toBeNull();

    expect(api.registrations).toHaveLength(1);
    expect(api.registrations[0]).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.com",
      organization: "Acme Testing Ltd",
      consents: ["privacy"],
      recaptchaToken: "test-mode-token",
    });
    expect(
      (api.registrations[0] as { optionIds: string[] }).optionIds.sort(),
    ).toEqual(["meal-lunch", "ws-security"]);
  });

  it("AC-004-02 shows backend field errors next to their fields and keeps the entered values", async () => {
    installFakeApi(() =>
      jsonResponse(400, {
        message: "Please correct the marked fields.",
        fieldErrors: {
          email: "The backend rejected this email address.",
          optionIds: "Select only available options, each at most once.",
          "consents.privacy": "This consent is required.",
        },
      }),
    );
    render(<App />);
    await fillValidExternal();

    submit();

    const email = screen.getByLabelText("Email", { selector: "input" });
    await waitFor(() =>
      expect(email).toHaveAccessibleDescription(
        "The backend rejected this email address.",
      ),
    );
    expect(email).toHaveAttribute("aria-invalid", "true");
    expect(
      screen.getByRole("checkbox", { name: "Secure coding workshop" }),
    ).toHaveAccessibleDescription(
      "Select only available options, each at most once.",
    );
    expect(
      screen.getByRole("checkbox", { name: PRIVACY_TEXT }),
    ).toHaveAccessibleDescription("This consent is required.");
    expect(screen.queryByRole("status")).toBeNull();
    expect(
      screen.getByRole("form", { name: "Registration form" }),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("First name", { selector: "input" }),
    ).toHaveValue("Ana");
    expect(email).toHaveValue("ana.novak@example.com");
    expect(
      screen.getByLabelText("Organization / institution", {
        selector: "input",
      }),
    ).toHaveValue("Acme Testing Ltd");
    expect(
      screen.getByRole("checkbox", { name: "Secure coding workshop" }),
    ).toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Lunch" })).toBeChecked();
  });

  it("AC-004-03 shows client-side field errors and sends no request", async () => {
    const api = installFakeApi(() => created());
    render(<App />);
    await fillValidExternal();
    type("First name", "   ");
    type("Email", "not-an-email");

    submit();

    const firstName = screen.getByLabelText("First name", {
      selector: "input",
    });
    await waitFor(() =>
      expect(firstName).toHaveAccessibleDescription("This field is required."),
    );
    expect(
      screen.getByLabelText("Email", { selector: "input" }),
    ).toHaveAccessibleDescription("Enter a valid email address.");
    expect(api.registrations).toHaveLength(0);
    expect(screen.queryByRole("status")).toBeNull();
  });

  it("AC-004-03 an empty student field is reported without a request", async () => {
    const api = installFakeApi(() => created());
    render(<App />);
    await chooseType("Student");
    type("First name", "Bor");
    type("Last name", "Kranjc");
    type("Email", "bor@example.com");
    type("Study institution", "University of Maribor");
    type("Study programme", "");
    type("Student ID", "63210001");

    submit();

    await waitFor(() =>
      expect(
        screen.getByLabelText("Study programme", { selector: "input" }),
      ).toHaveAccessibleDescription("This field is required."),
    );
    expect(api.registrations).toHaveLength(0);
  });

  it("AC-004-04 a backend failure shows only a general message without technical details", async () => {
    installFakeApi(() =>
      jsonResponse(500, {
        message:
          "org.postgresql.util.PSQLException: connection refused at db:5432",
        fieldErrors: {},
      }),
    );
    render(<App />);
    await fillValidExternal();

    submit();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(GENERAL_ERROR);
    expect(screen.queryByText(/PSQLException|5432|org\.postgresql/)).toBeNull();
    expect(screen.queryByRole("status")).toBeNull();
    expect(
      screen.getByLabelText("First name", { selector: "input" }),
    ).toHaveValue("Ana");
  });

  it("AC-004-04 an unreachable backend shows only a general message", async () => {
    installFakeApi(() => "network-error");
    render(<App />);
    await fillValidExternal();

    submit();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(GENERAL_ERROR);
    expect(screen.queryByText(/Failed to fetch|TypeError/)).toBeNull();
    expect(screen.queryByRole("status")).toBeNull();
  });
});
