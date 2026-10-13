import { screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  GENERAL_SUBMIT_ERROR,
  chooseType,
  fillValidExternal,
  jsonResponse,
  registrationCalls,
  renderPage,
  stubBackend,
  submit,
  type,
} from "./support";

const ACCEPTED = {
  id: "0b8f3c1e-2d4a-4b6e-9f00-112233445566",
  type: "EXTERNAL",
  firstName: "Ana",
  lastName: "Novak",
  options: [{ id: "ws-a", name: "Workshop A", category: "WORKSHOP" }],
  registeredAt: "2026-10-13T20:15:03.123Z",
};

function confirmationHeading() {
  return screen.queryByRole("heading", {
    level: 2,
    name: "Registration received",
  });
}

describe("US-004 registration confirmation", () => {
  it("AC-004-01 shows the confirmation with name and selected options and hides the form after acceptance", async () => {
    const fetchMock = stubBackend(() => jsonResponse(201, ACCEPTED));
    await renderPage();
    chooseType("External participant");
    fillValidExternal();

    submit();

    expect(
      await screen.findByRole("heading", {
        level: 2,
        name: "Registration received",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText("Thank you, Ana Novak.")).toBeInTheDocument();
    const list = screen.getByRole("list", { name: "Selected options" });
    expect(
      within(list)
        .getAllByRole("listitem")
        .map((li) => li.textContent?.trim()),
    ).toEqual(["Workshop A"]);
    expect(
      screen.queryByRole("button", { name: "Register" }),
    ).not.toBeInTheDocument();
    expect(screen.queryByLabelText("First name")).not.toBeInTheDocument();

    const calls = registrationCalls(fetchMock);
    expect(calls).toHaveLength(1);
    expect(JSON.parse(String(calls[0][1]?.body))).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.si",
      organization: "Institut Primer",
      optionIds: ["ws-a"],
      consents: { privacy: true },
      recaptchaToken: "test-pass",
    });
  });

  it("AC-004-02 shows backend field errors next to the fields, keeps the values and shows no confirmation", async () => {
    stubBackend(() =>
      jsonResponse(400, {
        message: "Validation failed",
        errors: [
          { field: "email", message: "Enter a valid email address." },
          { field: "options", message: "Option 'ws-a' is not available." },
        ],
      }),
    );
    await renderPage();
    chooseType("External participant");
    fillValidExternal();

    submit();

    const emailError = await screen.findByTestId("error-email");
    expect(emailError).toHaveTextContent("Enter a valid email address.");
    expect(screen.getByLabelText("Email")).toHaveAccessibleDescription(
      "Enter a valid email address.",
    );
    expect(screen.getByLabelText("Email")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    const optionsError = screen.getByTestId("error-options");
    expect(optionsError).toHaveTextContent("Option 'ws-a' is not available.");
    const options = screen.getByTestId("options");
    expect(
      options.contains(optionsError) ||
        options.nextElementSibling === optionsError,
    ).toBe(true);

    expect(confirmationHeading()).not.toBeInTheDocument();
    expect(screen.getByLabelText("First name")).toHaveValue("Ana");
    expect(screen.getByLabelText("Last name")).toHaveValue("Novak");
    expect(screen.getByLabelText("Email")).toHaveValue("ana.novak@example.si");
    expect(screen.getByLabelText("Organization / institution")).toHaveValue(
      "Institut Primer",
    );
    expect(screen.getByRole("checkbox", { name: "Workshop A" })).toBeChecked();
  });

  it("AC-004-03 shows field errors for an empty required field and an invalid email and sends no request", async () => {
    const fetchMock = stubBackend(() => jsonResponse(201, ACCEPTED));
    await renderPage();
    chooseType("External participant");
    fillValidExternal();
    type("First name", "   ");
    type("Email", "not-an-email");

    submit();

    expect(await screen.findByTestId("error-firstName")).toHaveTextContent(
      "This field is required.",
    );
    expect(screen.getByTestId("error-email")).toHaveTextContent(
      "Enter a valid email address.",
    );
    expect(screen.getByLabelText("First name")).toHaveAccessibleDescription(
      "This field is required.",
    );
    expect(registrationCalls(fetchMock)).toHaveLength(0);
    expect(confirmationHeading()).not.toBeInTheDocument();
  });

  it("AC-004-04 shows a general error without technical details when the backend fails", async () => {
    stubBackend(() =>
      jsonResponse(500, {
        message: "java.sql.SQLException: connection refused at db:5432",
      }),
    );
    await renderPage();
    chooseType("External participant");
    fillValidExternal();

    submit();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(GENERAL_SUBMIT_ERROR);
    expect(document.body).not.toHaveTextContent("SQLException");
    expect(confirmationHeading()).not.toBeInTheDocument();
    expect(screen.getByLabelText("First name")).toHaveValue("Ana");
  });

  it("AC-004-04 shows a general error when the backend cannot be reached", async () => {
    stubBackend(() => {
      throw new TypeError("Failed to fetch");
    });
    await renderPage();
    chooseType("External participant");
    fillValidExternal();

    submit();

    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent(GENERAL_SUBMIT_ERROR),
    );
    expect(document.body).not.toHaveTextContent("Failed to fetch");
    expect(confirmationHeading()).not.toBeInTheDocument();
  });
});
