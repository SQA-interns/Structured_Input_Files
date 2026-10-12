import { describe, expect, it } from "vitest";
import { availableOptions } from "./options";
import type { ConferenceOption } from "./types";
import { FIELDS, INVALID_EMAIL, REQUIRED, validateFields } from "./validation";

describe("validateFields", () => {
  it("accepts complete external values", () => {
    expect(
      validateFields("EXTERNAL", {
        firstName: "Ana",
        lastName: "Novak",
        email: "ana@example.org",
        organization: "Org",
      }),
    ).toEqual({});
  });

  it("requires every field of the type and ignores the other type", () => {
    expect(validateFields("STUDENT", { firstName: " ", organization: "" })).toEqual({
      firstName: REQUIRED,
      lastName: REQUIRED,
      email: REQUIRED,
      studyInstitution: REQUIRED,
      studyProgramme: REQUIRED,
      studentId: REQUIRED,
    });
  });

  it.each(["ana", "ana@", "@example.org", "ana@example", "a b@example.org", "a@@b.org"])(
    "rejects the email %s",
    (email) => {
      expect(
        validateFields("EXTERNAL", {
          firstName: "A",
          lastName: "B",
          email,
          organization: "O",
        }),
      ).toEqual({ email: INVALID_EMAIL });
    },
  );

  it("accepts an email with surrounding spaces", () => {
    expect(
      validateFields("EXTERNAL", {
        firstName: "A",
        lastName: "B",
        email: " a@b.org ",
        organization: "O",
      }),
    ).toEqual({});
  });

  it("lists the fixed fields in display order", () => {
    expect(FIELDS.EXTERNAL.map((f) => f.label)).toEqual([
      "First name",
      "Last name",
      "Email",
      "Organization / institution",
    ]);
    expect(FIELDS.STUDENT.map((f) => f.name)).toEqual([
      "firstName",
      "lastName",
      "email",
      "studyInstitution",
      "studyProgramme",
      "studentId",
    ]);
  });
});

describe("availableOptions", () => {
  it("keeps only options available to the type", () => {
    const options: ConferenceOption[] = [
      { id: "a", name: "A", category: "MEAL", availableTo: ["EXTERNAL"] },
      { id: "b", name: "B", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
    ];

    expect(availableOptions(options, "STUDENT").map((o) => o.id)).toEqual(["b"]);
    expect(availableOptions(options, "EXTERNAL").map((o) => o.id)).toEqual(["a", "b"]);
  });
});
