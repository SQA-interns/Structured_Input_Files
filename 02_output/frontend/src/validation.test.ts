import { describe, expect, it } from "vitest";
import {
  FIELDS,
  MESSAGES,
  hasControlCharacter,
  isValidEmail,
  validate,
  type FormValues,
} from "./validation";

const consents = [
  { id: "data", text: "Data", mandatory: true },
  { id: "photos", text: "Photos", mandatory: false },
];

function values(overrides: Partial<FormValues["text"]> = {}, extra: Partial<FormValues> = {}) {
  return {
    text: {
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.si",
      organization: "IJS",
      studyInstitution: "UM",
      studyProgramme: "RIT",
      studentId: "E1",
      ...overrides,
    },
    consentIds: ["data"],
    captchaToken: "t",
    ...extra,
  } satisfies FormValues;
}

describe("validation", () => {
  it("defines the fixed fields per type", () => {
    expect(FIELDS.EXTERNAL.map((f) => f.id)).toEqual([
      "firstName",
      "lastName",
      "email",
      "organization",
    ]);
    expect(FIELDS.STUDENT.map((f) => f.id)).toEqual([
      "firstName",
      "lastName",
      "email",
      "studyInstitution",
      "studyProgramme",
      "studentId",
    ]);
  });

  it("accepts a valid form of each type", () => {
    expect(validate(values(), "EXTERNAL", consents)).toEqual({});
    expect(validate(values({ organization: "" }), "STUDENT", consents)).toEqual({});
  });

  it("reports required, too long and control characters per field", () => {
    const errors = validate(
      values({ firstName: "  ", lastName: "x".repeat(201), organization: "a\u007fb" }),
      "EXTERNAL",
      consents,
    );
    expect(errors).toEqual({
      firstName: MESSAGES.REQUIRED,
      lastName: MESSAGES.TOO_LONG,
      organization: MESSAGES.INVALID_CHARACTERS,
    });
  });

  it("counts code points and allows exactly the limits", () => {
    expect(validate(values({ firstName: "😀".repeat(200) }), "EXTERNAL", consents)).toEqual({});
    const longEmail = `${"a".repeat(64)}@${"b".repeat(186)}.si`;
    expect(longEmail).toHaveLength(254);
    expect(validate(values({ email: longEmail }), "EXTERNAL", consents)).toEqual({});
    expect(validate(values({ email: `${longEmail}x` }), "EXTERNAL", consents)).toEqual({
      email: MESSAGES.TOO_LONG,
    });
  });

  it("checks the email format after trimming", () => {
    expect(validate(values({ email: " ana@example.si " }), "EXTERNAL", consents)).toEqual({});
    expect(validate(values({ email: "ana@example" }), "EXTERNAL", consents)).toEqual({
      email: MESSAGES.INVALID_EMAIL,
    });
  });

  it("requires mandatory consents and the captcha token", () => {
    expect(
      validate(values({}, { consentIds: ["photos"], captchaToken: null }), "EXTERNAL", consents),
    ).toEqual({
      "consents.data": MESSAGES.CONSENT_REQUIRED,
      captchaToken: MESSAGES.CAPTCHA_REQUIRED,
    });
  });

  it.each([
    ["ana@example.si", true],
    ["o'brien+x@sub.example.co", true],
    ["a".repeat(64) + "@x.si", true],
    ["a".repeat(65) + "@x.si", false],
    ["ana", false],
    ["@example.si", false],
    ["ana@", false],
    ["a@b@example.si", false],
    ["ana..n@example.si", false],
    ["ana@-x.si", false],
    ["ana@x-.si", false],
    ["ana@x.s", false],
    ["ana@x.s1", false],
  ])("isValidEmail(%s) is %s", (email, expected) => {
    expect(isValidEmail(email)).toBe(expected);
  });

  it.each([
    ["plain Čšž", false],
    ["tab\there", true],
    ["\u001f", true],
    [" ", false],
    ["~", false],
    ["\u007f", true],
    ["\u009f", true],
    [" ", false],
  ])("hasControlCharacter(%j) is %s", (text, expected) => {
    expect(hasControlCharacter(text)).toBe(expected);
  });
});
