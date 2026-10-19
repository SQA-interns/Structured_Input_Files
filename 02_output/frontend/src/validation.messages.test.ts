import { describe, expect, it } from "vitest";
import { testConfig } from "./testConfig";
import { validateForm } from "./validation";

describe("validateForm messages (phase 5 survivors)", () => {
  it("AC-004-03 every external field names itself in its message", () => {
    expect(validateForm("EXTERNAL", {}, ["privacy"], testConfig.consents)).toEqual({
      firstName: "First name is required.",
      lastName: "Last name is required.",
      email: "Email is required.",
      organization: "Organization / institution is required.",
    });
  });

  it("AC-004-03 every student field names itself in its message", () => {
    expect(validateForm("STUDENT", {}, ["privacy"], testConfig.consents)).toEqual({
      firstName: "First name is required.",
      lastName: "Last name is required.",
      email: "Email is required.",
      studyInstitution: "Study institution is required.",
      studyProgramme: "Study programme is required.",
      studentId: "Student ID is required.",
    });
  });

  it("AC-004-03 a blank email is reported as required, not as invalid", () => {
    const errors = validateForm(
      "EXTERNAL",
      { firstName: "A", lastName: "B", email: "   ", organization: "O" },
      ["privacy"],
      testConfig.consents,
    );

    expect(errors).toEqual({ email: "Email is required." });
  });
});
