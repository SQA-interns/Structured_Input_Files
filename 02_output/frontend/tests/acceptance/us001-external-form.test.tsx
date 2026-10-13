import { screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { chooseType, jsonResponse, renderPage, stubBackend } from "./support";

const STUDENT_LABELS = ["Study institution", "Study programme", "Student ID"];

describe("US-001 external participant form", () => {
  it("AC-001-08 shows exactly the external fields, active options grouped by category and unchecked consents", async () => {
    stubBackend(() => jsonResponse(500, { message: "unused" }));
    await renderPage();

    chooseType("External participant");

    for (const label of [
      "First name",
      "Last name",
      "Email",
      "Organization / institution",
    ]) {
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    }
    for (const label of STUDENT_LABELS) {
      expect(screen.queryByLabelText(label)).not.toBeInTheDocument();
    }
    expect(screen.getAllByRole("textbox")).toHaveLength(4);

    const groups: Record<string, string[]> = {
      Workshops: ["Workshop A", "Workshop for externals"],
      Events: ["Evening event"],
      Meals: ["Lunch"],
      "Other activities": ["City tour"],
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
      screen.queryByRole("checkbox", { name: "Student meetup" }),
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
