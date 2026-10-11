import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, screen, within } from "@testing-library/react";
import { stubBackend } from "./support";
import { PRIVACY, renderLoaded } from "./ui";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("US-001 external participant form", () => {
  it("AC-001-08: shows the external fields, active options by category and unchecked consents", async () => {
    stubBackend({ status: 201 });
    await renderLoaded();

    const typeGroup = screen.getByRole("radiogroup", { name: "Registration type" });
    expect(within(typeGroup).getByRole("radio", { name: "External participant" })).toBeChecked();
    expect(within(typeGroup).getByRole("radio", { name: "Student" })).not.toBeChecked();

    for (const label of ["First name", "Last name", "Email", "Organization / institution"]) {
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    }
    for (const label of ["Study institution", "Study programme", "Student ID"]) {
      expect(screen.queryByLabelText(label)).not.toBeInTheDocument();
    }

    const workshops = screen.getByRole("group", { name: "Workshops" });
    expect(
      within(workshops).getByRole("checkbox", { name: "Workshop: AI v praksi" }),
    ).toBeInTheDocument();
    expect(
      within(workshops).getByRole("checkbox", { name: "Workshop: Cloud security" }),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole("group", { name: "Events" })).getByRole("checkbox", {
        name: "Gala večerja",
      }),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole("group", { name: "Meals" })).getByRole("checkbox", {
        name: "Kosilo",
      }),
    ).toBeInTheDocument();
    expect(screen.queryByRole("group", { name: "Other activities" })).not.toBeInTheDocument();
    expect(screen.queryByRole("checkbox", { name: "Ogled Ljubljane" })).not.toBeInTheDocument();

    const consents = screen.getByRole("group", { name: "Consents" });
    expect(within(consents).getByRole("checkbox", { name: PRIVACY })).toHaveAttribute(
      "aria-required",
      "true",
    );
    for (const box of screen.getAllByRole("checkbox")) {
      expect(box).not.toBeChecked();
    }
  });
});
