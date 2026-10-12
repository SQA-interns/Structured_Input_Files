import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "../../src/App";
import {
  PHOTOS_TEXT,
  PRIVACY_TEXT,
  created,
  installFakeApi,
} from "./support/fakeApi";
import { checkboxNames, chooseType, group, textboxNames } from "./support/form";

describe("US-002 student form", () => {
  it("AC-002-07 shows exactly the student fields, only student options by category, unchecked consents", async () => {
    installFakeApi(() => created());
    render(<App />);

    await chooseType("Student");

    expect(textboxNames()).toEqual(
      [
        "Email",
        "First name",
        "Last name",
        "Student ID",
        "Study institution",
        "Study programme",
      ].sort(),
    );
    expect(screen.queryByLabelText("Organization / institution")).toBeNull();

    expect(checkboxNames(group("Workshops"))).toEqual([
      "Test automation workshop",
    ]);
    expect(
      screen.queryByRole("checkbox", { name: "Secure coding workshop" }),
    ).toBeNull();
    expect(checkboxNames(group("Events"))).toEqual(["Opening reception"]);
    expect(checkboxNames(group("Meals"))).toEqual(["Lunch"]);
    expect(checkboxNames(group("Other activities"))).toEqual(["City tour"]);

    expect(
      screen.getByRole("checkbox", { name: PRIVACY_TEXT }),
    ).not.toBeChecked();
    expect(
      screen.getByRole("checkbox", { name: PHOTOS_TEXT }),
    ).not.toBeChecked();
  });

  it("AC-002-07 switching from external to student removes the external-only option and field", async () => {
    installFakeApi(() => created());
    render(<App />);

    await chooseType("External participant");
    expect(
      screen.getByRole("checkbox", { name: "Secure coding workshop" }),
    ).toBeInTheDocument();
    await chooseType("Student");

    expect(
      screen.queryByRole("checkbox", { name: "Secure coding workshop" }),
    ).toBeNull();
    expect(screen.queryByLabelText("Organization / institution")).toBeNull();
    expect(screen.getByLabelText("Student ID")).toBeInTheDocument();
  });
});
