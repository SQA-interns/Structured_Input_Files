import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "../App";
import { CONSENT_TEXT, installFakeBackend } from "./fakeBackend";
import {
  chooseStudent,
  expectExactlyTextFields,
  fillValidStudent,
  submit,
  type,
  waitForForm,
} from "./formHelpers";

describe("US-002 student form", () => {
  it("AC-002-01 shows exactly the student fields and only the active options open to students", async () => {
    installFakeBackend();
    render(<App />);
    await waitForForm();

    chooseStudent();

    expectExactlyTextFields([
      "First name",
      "Last name",
      "Email",
      "Study institution",
      "Study programme",
      "Student ID",
    ]);
    expect(
      screen.queryByRole("textbox", { name: "Organization / institution" }),
    ).not.toBeInTheDocument();
    expect(
      within(screen.getByRole("group", { name: "Workshops" })).getByRole(
        "checkbox",
        { name: "Workshop A: Testing" },
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("checkbox", { name: "Workshop for professionals" }),
    ).not.toBeInTheDocument();
    expect(
      within(screen.getByRole("group", { name: "Other activities" })).getByRole(
        "checkbox",
        { name: "Career fair" },
      ),
    ).toBeInTheDocument();
  });

  it("AC-002-02 shows the mandatory consent unchecked, also after switching from the external form", async () => {
    installFakeBackend();
    render(<App />);
    await waitForForm();
    fireEvent.click(screen.getByRole("checkbox", { name: CONSENT_TEXT }));

    chooseStudent();

    expect(
      screen.getByRole("checkbox", { name: CONSENT_TEXT }),
    ).not.toBeChecked();
  });

  it("AC-002-09 an empty student field blocks the submission and shows the error next to the field", async () => {
    const backend = installFakeBackend();
    render(<App />);
    await waitForForm();
    fillValidStudent();
    type("Student ID", "");

    submit();

    expect(
      await screen.findByRole("textbox", { name: "Student ID" }),
    ).toHaveAccessibleDescription("This field is required.");
    expect(backend.registrations).toHaveLength(0);
  });
});
