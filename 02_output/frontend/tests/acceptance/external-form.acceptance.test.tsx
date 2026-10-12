import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "../../src/App";
import {
  PHOTOS_TEXT,
  PRIVACY_TEXT,
  created,
  installFakeApi,
} from "./support/fakeApi";
import { checkboxNames, chooseType, group, textboxNames } from "./support/form";

describe("US-001 external participant form", () => {
  it("AC-001-08 shows exactly the external fields, grouped active options and unchecked consents", async () => {
    installFakeApi(() => created());
    render(<App />);

    await chooseType("External participant");

    expect(textboxNames()).toEqual(
      ["Email", "First name", "Last name", "Organization / institution"].sort(),
    );
    expect(screen.queryByLabelText("Study institution")).toBeNull();
    expect(screen.queryByLabelText("Study programme")).toBeNull();
    expect(screen.queryByLabelText("Student ID")).toBeNull();

    expect(checkboxNames(group("Workshops"))).toEqual([
      "Test automation workshop",
      "Secure coding workshop",
    ]);
    expect(checkboxNames(group("Events"))).toEqual(["Opening reception"]);
    expect(checkboxNames(group("Meals"))).toEqual(["Lunch"]);
    expect(checkboxNames(group("Other activities"))).toEqual(["City tour"]);
    for (const name of ["Workshops", "Events", "Meals", "Other activities"]) {
      for (const box of within(group(name)).getAllByRole("checkbox")) {
        expect(box).not.toBeChecked();
      }
    }

    const privacy = screen.getByRole("checkbox", { name: PRIVACY_TEXT });
    const photos = screen.getByRole("checkbox", { name: PHOTOS_TEXT });
    expect(privacy).not.toBeChecked();
    expect(photos).not.toBeChecked();
  });

  it("AC-001-08 lists the option groups in the order workshops, events, meals, other activities", async () => {
    installFakeApi(() => created());
    render(<App />);

    await chooseType("External participant");

    const legends = screen
      .getAllByRole("group")
      .map((g) => g.querySelector("legend")?.textContent?.trim())
      .filter((t) =>
        ["Workshops", "Events", "Meals", "Other activities"].includes(t ?? ""),
      );
    expect(legends).toEqual([
      "Workshops",
      "Events",
      "Meals",
      "Other activities",
    ]);
  });
});
