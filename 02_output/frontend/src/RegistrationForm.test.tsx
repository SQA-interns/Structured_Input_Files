import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "./RegistrationForm";
import { testConfig } from "./testConfig";

function renderForm() {
  return render(<RegistrationForm config={testConfig} onAccepted={vi.fn()} />);
}

describe("RegistrationForm external", () => {
  it("AC-001-08 shows no fields before a registration type is chosen", () => {
    renderForm();

    expect(screen.getByRole("radio", { name: "External participant" })).not.toBeChecked();
    expect(screen.getByRole("radio", { name: "Student" })).not.toBeChecked();
    expect(screen.queryAllByRole("textbox")).toHaveLength(0);
    expect(screen.queryByRole("button", { name: "Register" })).not.toBeInTheDocument();
  });

  it("AC-001-08 external participants get their fields, groups and unchecked consents", () => {
    renderForm();

    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));

    expect(screen.getAllByRole("textbox").map((t) => t.getAttribute("name"))).toEqual([
      "firstName",
      "lastName",
      "email",
      "organization",
    ]);
    expect(screen.getByRole("textbox", { name: "Organization / institution" })).toHaveValue("");
    expect(screen.getByRole("group", { name: "Workshops" })).toBeInTheDocument();
    expect(screen.queryByRole("group", { name: "Events" })).not.toBeInTheDocument();
    const other = screen.getByRole("group", { name: "Other activities" });
    expect(within(other).getByRole("checkbox", { name: "Tour" })).not.toBeChecked();
    expect(screen.queryByRole("checkbox", { name: "Mentoring" })).not.toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "I agree to the processing." })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Photos are fine." })).not.toBeChecked();
    expect(
      screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }),
    ).not.toBeChecked();
    expect(screen.getByRole("button", { name: "Register" })).toBeInTheDocument();
  });

  it("AC-001-08 checking an option or consent toggles it", () => {
    renderForm();
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));

    fireEvent.click(screen.getByRole("checkbox", { name: "Tour" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Photos are fine." }));

    expect(screen.getByRole("checkbox", { name: "Tour" })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Photos are fine." })).toBeChecked();

    fireEvent.click(screen.getByRole("checkbox", { name: "Tour" }));
    expect(screen.getByRole("checkbox", { name: "Tour" })).not.toBeChecked();
  });

  it("AC-001-08 typed values are kept in the fields", () => {
    renderForm();
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));

    fireEvent.change(screen.getByRole("textbox", { name: "First name" }), {
      target: { value: "Ana" },
    });

    expect(screen.getByRole("textbox", { name: "First name" })).toHaveValue("Ana");
  });
});
