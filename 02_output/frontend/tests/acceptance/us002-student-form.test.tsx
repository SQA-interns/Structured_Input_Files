import { screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { chooseType, jsonResponse, renderPage, stubBackend } from "./support";

describe("US-002 student form", () => {
  it("AC-002-07 shows exactly the student fields, only student options grouped by category and unchecked consents", async () => {
    stubBackend(() => jsonResponse(500, { message: "unused" }));
    await renderPage();

    chooseType("Student");

    for (const label of [
      "First name",
      "Last name",
      "Email",
      "Study institution",
      "Study programme",
      "Student ID",
    ]) {
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    }
    expect(
      screen.queryByLabelText("Organization / institution"),
    ).not.toBeInTheDocument();
    expect(screen.getAllByRole("textbox")).toHaveLength(6);

    const groups: Record<string, string[]> = {
      Workshops: ["Workshop A"],
      Events: ["Evening event"],
      Meals: ["Lunch"],
      "Other activities": ["City tour", "Student meetup"],
    };
    for (const [legend, names] of Object.entries(groups)) {
      const group = screen.getByRole("group", { name: legend });
      expect(within(group).getAllByRole("checkbox")).toHaveLength(names.length);
      for (const name of names) {
        expect(
          within(group).getByRole("checkbox", { name }),
        ).toBeInTheDocument();
      }
    }
    expect(
      screen.queryByRole("checkbox", { name: "Workshop for externals" }),
    ).not.toBeInTheDocument();

    const consents = within(
      screen.getByRole("group", { name: "Consents" }),
    ).getAllByRole("checkbox");
    expect(consents).toHaveLength(2);
    for (const box of consents) {
      expect(box).not.toBeChecked();
    }
  });
});
