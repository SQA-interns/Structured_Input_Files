import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "./RegistrationForm";
import { testConfig } from "./testConfig";

function chooseStudent() {
  render(<RegistrationForm config={testConfig} onAccepted={vi.fn()} />);
  fireEvent.click(screen.getByRole("radio", { name: "Student" }));
}

describe("RegistrationForm student", () => {
  it("AC-002-07 students get exactly the student fields", () => {
    chooseStudent();

    expect(screen.getAllByRole("textbox").map((t) => t.getAttribute("name"))).toEqual([
      "firstName",
      "lastName",
      "email",
      "studyInstitution",
      "studyProgramme",
      "studentId",
    ]);
    expect(screen.getByRole("textbox", { name: "Study institution" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Study programme" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Student ID" })).toBeInTheDocument();
    expect(
      screen.queryByRole("textbox", { name: "Organization / institution" }),
    ).not.toBeInTheDocument();
  });

  it("AC-002-07 students see only student options, grouped, unchecked", () => {
    chooseStudent();

    expect(
      within(screen.getByRole("group", { name: "Events" })).getByRole("checkbox", {
        name: "Student party",
      }),
    ).not.toBeChecked();
    const other = screen.getByRole("group", { name: "Other activities" });
    expect(within(other).getByRole("checkbox", { name: "Mentoring" })).not.toBeChecked();
    expect(screen.queryByRole("checkbox", { name: "Tour" })).not.toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "I agree to the processing." })).not.toBeChecked();
  });

  it("AC-002-07 switching the type drops options the new type cannot select", () => {
    render(<RegistrationForm config={testConfig} onAccepted={vi.fn()} />);
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop A" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Tour" }));

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));

    expect(screen.getByRole("checkbox", { name: "Workshop A" })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Tour" })).not.toBeChecked();
  });
});
