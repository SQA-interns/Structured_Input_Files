import { cleanup, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  check,
  chooseType,
  errorOf,
  fill,
  formConfig,
  mockBackend,
  registrationPosts,
  renderApp,
  submit,
} from "./support";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  document.head.querySelectorAll("script").forEach((s) => s.remove());
});

function textboxLabels(): string[] {
  const form = screen.getByRole("form", { name: "Registration form" });
  return within(form)
    .getAllByRole("textbox")
    .map((el) => (el as HTMLInputElement).labels?.[0]?.textContent?.trim() ?? "");
}

function optionLabels(): string[] {
  return formConfig.options
    .map((o) => o.name)
    .filter((name) => screen.queryByRole("checkbox", { name }) !== null);
}

describe("US-002 student form", () => {
  it("AC-002-01 shows exactly the student fields, student options and unchecked consents", async () => {
    mockBackend();
    renderApp();

    await chooseType("Student");

    expect(textboxLabels()).toEqual([
      "First name",
      "Last name",
      "Email",
      "Study institution",
      "Study programme",
      "Student ID",
    ]);
    for (const consent of formConfig.consents) {
      expect(screen.getByRole("checkbox", { name: consent.text })).not.toBeChecked();
    }
  });

  it("AC-002-04 does not offer options whose audience excludes students", async () => {
    mockBackend();
    renderApp();

    await chooseType("Student");

    expect(optionLabels()).toEqual([
      "Delavnica: umetna inteligenca",
      "Študentski večer",
      "Kosilo",
      "Ogled Ljubljane",
    ]);
    expect(screen.queryByRole("checkbox", { name: "Gala večerja" })).toBeNull();
  });

  it("AC-002-03 requires the student fields before submission", async () => {
    const requests = mockBackend();
    renderApp();
    await chooseType("Student");
    fill("First name", "Luka");
    fill("Last name", "Kranjc");
    fill("Email", "luka@example.si");
    check(formConfig.consents[0].text);
    check("I am not a robot (test mode)");

    submit();

    for (const field of ["studyInstitution", "studyProgramme", "studentId"]) {
      expect(errorOf(field)).toHaveTextContent("This field is required.");
    }
    expect(registrationPosts(requests)).toHaveLength(0);
  });
});
