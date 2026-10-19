import { render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../App";
import {
  acceptedExternal,
  installFakeBackend,
  PHOTOS_TEXT,
  PRIVACY_TEXT,
} from "./support/fakeBackend";
import { chooseType } from "./support/form";

const CATEGORY_LEGENDS = ["Workshops", "Events", "Meals", "Other activities"];

describe("US-002 student form (ui.md)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-002-07 shows exactly the student fields, only student options by category and unchecked consents", async () => {
    installFakeBackend(acceptedExternal);
    render(<App />);

    await chooseType("Student");

    const expectedFields = [
      "First name",
      "Last name",
      "Email",
      "Study institution",
      "Study programme",
      "Student ID",
    ];
    expect(screen.getAllByRole("textbox")).toHaveLength(expectedFields.length);
    for (const name of expectedFields) {
      expect(screen.getByRole("textbox", { name })).toBeInTheDocument();
    }
    expect(
      screen.queryByRole("textbox", { name: "Organization / institution" }),
    ).not.toBeInTheDocument();

    const legends = screen
      .getAllByRole("group")
      .map((group) => group.querySelector("legend")?.textContent?.trim() ?? "")
      .filter((legend) => CATEGORY_LEGENDS.includes(legend));
    expect(legends).toEqual(CATEGORY_LEGENDS);
    const workshops = screen.getByRole("group", { name: "Workshops" });
    const events = screen.getByRole("group", { name: "Events" });
    const meals = screen.getByRole("group", { name: "Meals" });
    const other = screen.getByRole("group", { name: "Other activities" });
    expect(
      within(workshops).getByRole("checkbox", { name: "Workshop: AI in practice" }),
    ).not.toBeChecked();
    expect(within(events).getByRole("checkbox", { name: "Conference dinner" })).not.toBeChecked();
    expect(within(meals).getByRole("checkbox", { name: "Lunch" })).not.toBeChecked();
    expect(within(other).getByRole("checkbox", { name: "Student mentoring" })).not.toBeChecked();
    expect(screen.queryByRole("checkbox", { name: "City tour" })).not.toBeInTheDocument();

    expect(screen.getByRole("checkbox", { name: PRIVACY_TEXT })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: PHOTOS_TEXT })).not.toBeChecked();
  });
});
