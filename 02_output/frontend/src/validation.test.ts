import { describe, expect, it } from "vitest";
import { MESSAGES, validate } from "./validation";

const external = {
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@x.si",
  organization: "Acme",
};

describe("validate", () => {
  it("accepts a complete external form", () => {
    expect(validate("EXTERNAL", external, true, "tok")).toEqual({});
  });

  it("requires every field of the chosen type, ignoring surrounding whitespace", () => {
    expect(
      validate("STUDENT", { ...external, studentId: "  " }, true, "tok"),
    ).toEqual({
      studyInstitution: MESSAGES.required,
      studyProgramme: MESSAGES.required,
      studentId: MESSAGES.required,
    });
  });

  it("does not require fields of the other type", () => {
    expect(
      validate("EXTERNAL", { ...external, studentId: "" }, true, "tok"),
    ).toEqual({});
  });

  it.each(["ana", "ana@x", "@x.si", "a b@x.si", "ana@x.si trailing"])(
    "rejects the email %s",
    (email) => {
      expect(validate("EXTERNAL", { ...external, email }, true, "tok")).toEqual(
        { email: MESSAGES.email },
      );
    },
  );

  it("accepts an email with surrounding spaces", () => {
    expect(
      validate("EXTERNAL", { ...external, email: " ana@x.si " }, true, "tok"),
    ).toEqual({});
  });

  it("requires consent and the anti-automation token", () => {
    expect(validate("EXTERNAL", external, false, "")).toEqual({
      consentGiven: MESSAGES.consent,
      recaptchaToken: MESSAGES.robot,
    });
  });
});
