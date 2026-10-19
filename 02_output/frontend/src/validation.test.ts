import { describe, expect, it } from "vitest";
import { testConfig } from "./testConfig";
import { validateForm } from "./validation";

const external = {
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@example.com",
  organization: "IJS",
};

const student = {
  firstName: "Luka",
  lastName: "Kranjc",
  email: "luka@example.com",
  studyInstitution: "UL",
  studyProgramme: "RI",
  studentId: "6321",
};

describe("validateForm", () => {
  it("AC-004-03 a complete external form has no errors", () => {
    expect(validateForm("EXTERNAL", external, ["privacy"], testConfig.consents)).toEqual({});
  });

  it("AC-004-03 a complete student form has no errors", () => {
    expect(validateForm("STUDENT", student, ["privacy"], testConfig.consents)).toEqual({});
  });

  it("AC-004-03 empty or blank required fields are errors", () => {
    const errors = validateForm(
      "EXTERNAL",
      { firstName: "  ", lastName: "", email: "ana@example.com" },
      ["privacy"],
      testConfig.consents,
    );

    expect(Object.keys(errors).sort()).toEqual(["firstName", "lastName", "organization"]);
    expect(errors.firstName).toBe("First name is required.");
  });

  it("AC-004-03 every student field is required", () => {
    const errors = validateForm("STUDENT", {}, ["privacy"], testConfig.consents);

    expect(Object.keys(errors).sort()).toEqual([
      "email",
      "firstName",
      "lastName",
      "studentId",
      "studyInstitution",
      "studyProgramme",
    ]);
  });

  it.each(["ana", "ana@", "@example.com", "ana novak@example.com", "ana@example", "a@b..si"])(
    "AC-004-03 invalid email %s is an error",
    (email) => {
      const errors = validateForm(
        "EXTERNAL",
        { ...external, email },
        ["privacy"],
        testConfig.consents,
      );

      expect(Object.keys(errors)).toEqual(["email"]);
      expect(errors.email).toBe("Please enter a valid email address.");
    },
  );

  it("AC-004-03 surrounding spaces around a valid email are accepted", () => {
    expect(
      validateForm(
        "EXTERNAL",
        { ...external, email: " ana@example.com " },
        ["privacy"],
        testConfig.consents,
      ),
    ).toEqual({});
  });

  it("AC-004-03 a missing mandatory consent is an error on that consent", () => {
    const errors = validateForm("EXTERNAL", external, ["photos"], testConfig.consents);

    expect(errors).toEqual({ "consents.privacy": "This consent is required." });
  });
});
