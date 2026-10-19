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

/** Legends of the option groups, in page order. */
function optionGroupLegends(): string[] {
  return screen
    .getAllByRole("group")
    .map((group) => group.querySelector("legend")?.textContent?.trim() ?? "")
    .filter((legend) => CATEGORY_LEGENDS.includes(legend));
}

describe("US-001 external participant form (ui.md)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("AC-001-08 shows exactly the external fields, grouped active options and unchecked consents", async () => {
    installFakeBackend(acceptedExternal);
    render(<App />);

    await chooseType("External participant");

    const expectedFields = ["First name", "Last name", "Email", "Organization / institution"];
    expect(screen.getAllByRole("textbox")).toHaveLength(expectedFields.length);
    for (const name of expectedFields) {
      expect(screen.getByRole("textbox", { name })).toBeInTheDocument();
    }
    expect(screen.queryByRole("textbox", { name: "Student ID" })).not.toBeInTheDocument();

    expect(optionGroupLegends()).toEqual(CATEGORY_LEGENDS);
    const workshops = screen.getByRole("group", { name: "Workshops" });
    const events = screen.getByRole("group", { name: "Events" });
    const meals = screen.getByRole("group", { name: "Meals" });
    const other = screen.getByRole("group", { name: "Other activities" });
    expect(
      within(workshops).getByRole("checkbox", { name: "Workshop: AI in practice" }),
    ).not.toBeChecked();
    expect(within(events).getByRole("checkbox", { name: "Conference dinner" })).not.toBeChecked();
    expect(within(meals).getByRole("checkbox", { name: "Lunch" })).not.toBeChecked();
    expect(within(other).getByRole("checkbox", { name: "City tour" })).not.toBeChecked();
    expect(screen.queryByRole("checkbox", { name: "Student mentoring" })).not.toBeInTheDocument();

    expect(screen.getByRole("checkbox", { name: PRIVACY_TEXT })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: PHOTOS_TEXT })).not.toBeChecked();
  });
});
