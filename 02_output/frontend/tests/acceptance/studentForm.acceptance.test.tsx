import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, screen, within } from "@testing-library/react";
import { stubBackend } from "./support";
import { renderLoaded } from "./ui";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("US-002 student form", () => {
  it("AC-002-07: shows the student fields and only options available to students, unchecked", async () => {
    stubBackend({ status: 201 });
    await renderLoaded();

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));

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
    expect(screen.queryByLabelText("Organization / institution")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("checkbox", { name: "Workshop: Cloud security" }),
    ).not.toBeInTheDocument();
    expect(
      within(screen.getByRole("group", { name: "Workshops" })).getByRole("checkbox", {
        name: "Workshop: AI v praksi",
      }),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole("group", { name: "Other activities" })).getByRole("checkbox", {
        name: "Ogled Ljubljane",
      }),
    ).toBeInTheDocument();
    for (const box of screen.getAllByRole("checkbox")) {
      expect(box).not.toBeChecked();
    }
  });
});
