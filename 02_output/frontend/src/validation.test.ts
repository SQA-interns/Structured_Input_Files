import { describe, expect, it } from "vitest";
import { EMAIL_PATTERN, MESSAGES, errorId, validate } from "./validation";

const consents = [
  { id: "privacy", text: "P", mandatory: true },
  { id: "photos", text: "F", mandatory: false },
];

describe("validate", () => {
  it("accepts a complete student form", () => {
    expect(
      validate(
        {
          type: "STUDENT",
          values: {
            firstName: "Luka",
            lastName: "K",
            email: " luka@x.si ",
            studyInstitution: "U",
            studyProgramme: "CS",
            studentId: "1",
          },
          consents: { privacy: true },
          token: "t",
        },
        consents,
      ),
    ).toEqual({});
  });

  it("reports every missing student field, the consent and the token", () => {
    expect(
      validate(
        { type: "STUDENT", values: { email: "x" }, consents: {}, token: null },
        consents,
      ),
    ).toEqual({
      firstName: MESSAGES.required,
      lastName: MESSAGES.required,
      email: MESSAGES.email,
      studyInstitution: MESSAGES.required,
      studyProgramme: MESSAGES.required,
      studentId: MESSAGES.required,
      "consents.privacy": MESSAGES.consent,
      recaptchaToken: MESSAGES.captcha,
    });
  });

  it("ignores fields of the other type", () => {
    const errors = validate(
      {
        type: "EXTERNAL",
        values: {
          firstName: "A",
          lastName: "B",
          email: "a@b.si",
          organization: "O",
        },
        consents: { privacy: true },
        token: "t",
      },
      consents,
    );
    expect(errors).toEqual({});
  });
});

describe("email pattern", () => {
  it.each(["a@b.si", "ana.novak@example.co.uk"])("accepts %s", (email) => {
    expect(EMAIL_PATTERN.test(email)).toBe(true);
  });

  it.each(["a@b", "a b@c.si", "@b.si", "a@", "a@@b.si", " a@b.si"])(
    "rejects %s",
    (email) => {
      expect(EMAIL_PATTERN.test(email)).toBe(false);
    },
  );
});

it("builds error element ids", () => {
  expect(errorId("consents.privacy")).toBe("error-consents.privacy");
});
