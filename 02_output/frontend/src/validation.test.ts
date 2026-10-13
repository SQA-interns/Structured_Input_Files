import { describe, expect, it } from "vitest";
import type { Consent } from "./types";
import {
  FIELDS,
  MAX_LENGTH,
  MESSAGES,
  validate,
  type FormInput,
} from "./validation";

const consents: Consent[] = [
  { id: "privacy", text: "P", mandatory: true },
  { id: "photos", text: "F", mandatory: false },
];

const external = (
  values: FormInput["values"],
  extra: Partial<FormInput> = {},
): FormInput => ({
  type: "EXTERNAL",
  values: {
    firstName: "Ana",
    lastName: "Novak",
    email: "ana@example.com",
    organization: "Acme",
    ...values,
  },
  consents: ["privacy"],
  recaptchaToken: "tok",
  ...extra,
});

describe("validate", () => {
  it("accepts a complete external form", () => {
    expect(validate(external({}), consents)).toEqual({});
  });

  it("accepts a complete student form and ignores external fields", () => {
    const input: FormInput = {
      type: "STUDENT",
      values: {
        firstName: "Bor",
        lastName: "K",
        email: "b@c.co",
        studyInstitution: "U",
        studyProgramme: "P",
        studentId: "1",
      },
      consents: ["privacy"],
      recaptchaToken: "tok",
    };
    expect(validate(input, consents)).toEqual({});
    expect(
      validate(
        { ...input, values: { ...input.values, studentId: " " } },
        consents,
      ),
    ).toEqual({
      studentId: MESSAGES.required,
    });
  });

  it("requires a type and checks no field without one", () => {
    expect(
      validate(external({ firstName: "" }, { type: null }), consents),
    ).toEqual({
      type: MESSAGES.type,
    });
  });

  it("reports blank, missing and too long fields", () => {
    expect(
      validate(
        external({
          firstName: "  ",
          lastName: undefined,
          organization: "o".repeat(201),
        }),
        consents,
      ),
    ).toEqual({
      firstName: MESSAGES.required,
      lastName: MESSAGES.required,
      organization: "Use at most 200 characters.",
    });
  });

  it("accepts values at the length limit after trimming", () => {
    expect(
      validate(
        external({
          firstName: ` ${"a".repeat(100)} `,
          organization: "o".repeat(200),
        }),
        consents,
      ),
    ).toEqual({});
    expect(
      validate(external({ firstName: "a".repeat(101) }), consents),
    ).toEqual({
      firstName: MESSAGES.tooLong(100),
    });
  });

  it.each(["plain", "a@b", "@b.co", "a b@c.co", "a@b@c.co", "a@b.", "a@.co "])(
    "rejects the email %s",
    (email) => {
      expect(validate(external({ email }), consents)).toEqual({
        email: MESSAGES.email,
      });
    },
  );

  it.each(["a@b.co", " a@b.co ", "č@š.si"])("accepts the email %s", (email) => {
    expect(validate(external({ email }), consents)).toEqual({});
  });

  it("checks the email length before its format", () => {
    expect(
      validate(external({ email: `${"a".repeat(250)}@b.co` }), consents),
    ).toEqual({
      email: "Use at most 254 characters.",
    });
  });

  it("requires mandatory consents only, and the reCAPTCHA", () => {
    expect(
      validate(
        external({}, { consents: ["photos"], recaptchaToken: "" }),
        consents,
      ),
    ).toEqual({
      "consents.privacy": MESSAGES.consent,
      recaptchaToken: MESSAGES.captcha,
    });
  });

  it("declares the fields and limits of both types", () => {
    expect(FIELDS.EXTERNAL).toEqual([
      "firstName",
      "lastName",
      "email",
      "organization",
    ]);
    expect(FIELDS.STUDENT).toEqual([
      "firstName",
      "lastName",
      "email",
      "studyInstitution",
      "studyProgramme",
      "studentId",
    ]);
    expect(MAX_LENGTH).toEqual({
      firstName: 100,
      lastName: 100,
      email: 254,
      organization: 200,
      studyInstitution: 200,
      studyProgramme: 200,
      studentId: 50,
    });
    expect(MESSAGES.tooLong(7)).toBe("Use at most 7 characters.");
  });

  it("rejects text after an otherwise valid address", () => {
    expect(validate(external({ email: "a@b.co x" }), consents)).toEqual({
      email: MESSAGES.email,
    });
  });

  it("uses the contract wording", () => {
    expect(MESSAGES).toMatchObject({
      required: "This field is required.",
      email: "Enter a valid email address.",
      type: "Choose a registration type.",
      consent: "This consent is required.",
      captcha: "Confirm that you are not a robot.",
    });
  });
});
