import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "../App";
import { CONSENT_TEXT, installFakeBackend } from "./fakeBackend";
import {
  expectExactlyTextFields,
  fillValidExternal,
  submit,
  type,
  waitForForm,
} from "./formHelpers";

describe("US-001 external participant form", () => {
  it("AC-001-01 shows exactly the external fields and the active options open to external participants, grouped by category", async () => {
    installFakeBackend();
    render(<App />);
    await waitForForm();

    expect(
      screen.getByRole("radio", { name: "External participant" }),
    ).toBeChecked();
    expectExactlyTextFields([
      "First name",
      "Last name",
      "Email",
      "Organization / institution",
    ]);
    for (const heading of ["Workshops", "Events", "Meals"]) {
      expect(
        screen.getByRole("heading", { name: heading }),
      ).toBeInTheDocument();
    }
    expect(
      screen.queryByRole("heading", { name: "Other activities" }),
    ).not.toBeInTheDocument();
    const workshops = screen.getByRole("group", { name: "Workshops" });
    expect(
      within(workshops).getByRole("checkbox", { name: "Workshop A: Testing" }),
    ).toBeInTheDocument();
    expect(
      within(workshops).getByRole("checkbox", {
        name: "Workshop for professionals",
      }),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole("group", { name: "Events" })).getByRole(
        "checkbox",
        { name: "Conference dinner" },
      ),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole("group", { name: "Meals" })).getByRole(
        "checkbox",
        { name: "Lunch" },
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("checkbox", { name: "Career fair" }),
    ).not.toBeInTheDocument();
  });

  it("AC-001-02 shows the mandatory consent with its configured wording, unchecked", async () => {
    installFakeBackend();
    render(<App />);
    await waitForForm();

    expect(
      screen.getByRole("checkbox", { name: CONSENT_TEXT }),
    ).not.toBeChecked();
  });

  it("AC-001-15 an empty required field blocks the submission and shows the error next to the field", async () => {
    const backend = installFakeBackend();
    render(<App />);
    await waitForForm();
    fillValidExternal();
    type("First name", "   ");

    submit();

    expect(
      await screen.findByRole("textbox", { name: "First name" }),
    ).toHaveAccessibleDescription("This field is required.");
    expect(backend.registrations).toHaveLength(0);
  });

  it("AC-001-15 an invalid email blocks the submission and shows the error next to the field", async () => {
    const backend = installFakeBackend();
    render(<App />);
    await waitForForm();
    fillValidExternal();
    type("Email", "ana.novak@example");

    submit();

    expect(
      await screen.findByRole("textbox", { name: "Email" }),
    ).toHaveAccessibleDescription("Enter a valid email address.");
    expect(backend.registrations).toHaveLength(0);
  });

  it("AC-001-16 shows the backend's field errors next to their fields and keeps the entered values", async () => {
    const backend = installFakeBackend();
    backend.reply({
      status: 409,
      body: {
        status: 409,
        title: "Conflict",
        fieldErrors: [
          {
            field: "email",
            message: "This email address is already registered.",
          },
        ],
      },
    });
    render(<App />);
    await waitForForm();
    fillValidExternal();

    submit();

    const email = await screen.findByRole("textbox", { name: "Email" });
    await screen.findByText("This email address is already registered.");
    expect(email).toHaveAccessibleDescription(
      "This email address is already registered.",
    );
    expect(email).toHaveValue("ana.novak@example.si");
    expect(screen.getByRole("textbox", { name: "First name" })).toHaveValue(
      "Ana",
    );
    expect(
      screen.queryByRole("heading", { name: "Registration received" }),
    ).not.toBeInTheDocument();
    expect(backend.registrations).toHaveLength(1);
  });
});
