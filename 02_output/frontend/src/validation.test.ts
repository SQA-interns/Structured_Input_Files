import { describe, expect, it } from "vitest";
import {
  CAPTCHA_REQUIRED,
  CONSENT_REQUIRED,
  INVALID_EMAIL,
  REQUIRED,
  validate,
} from "./validation";

const consents = [
  { id: "privacy", text: "P", mandatory: true },
  { id: "photo", text: "F", mandatory: false },
];

const external = {
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@example.si",
  organization: "IJS",
};

describe("validate", () => {
  it("accepts a complete external form", () => {
    expect(validate("EXTERNAL", external, consents, new Set(["privacy"]), "t")).toEqual({});
  });

  it("requires every field of the type after trimming, and only those", () => {
    const errors = validate(
      "STUDENT",
      { firstName: "  ", organization: "" },
      consents,
      new Set(),
      "",
    );
    expect(errors).toEqual({
      firstName: REQUIRED,
      lastName: REQUIRED,
      email: REQUIRED,
      studyInstitution: REQUIRED,
      studyProgramme: REQUIRED,
      studentId: REQUIRED,
      "consents.privacy": CONSENT_REQUIRED,
      captchaToken: CAPTCHA_REQUIRED,
    });
  });

  it.each(["ana", "ana@x", "a b@x.si", "ana@x .si", "@x.si"])("rejects email %s", (email) => {
    expect(
      validate("EXTERNAL", { ...external, email }, consents, new Set(["privacy"]), "t"),
    ).toEqual({ email: INVALID_EMAIL });
  });

  it("accepts a trimmed valid email and ignores optional consents", () => {
    expect(
      validate(
        "EXTERNAL",
        { ...external, email: " ana@x.si " },
        consents,
        new Set(["privacy"]),
        "t",
      ),
    ).toEqual({});
  });
});
