import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import {
  STUDENT_FIELDS,
  chooseType,
  formConfig,
  optionsInGroup,
  stubBackend,
  textboxNames,
} from "./support";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("US-002 student form", () => {
  it("AC-002-07 shows exactly the student fields and only student options, grouped, consents unchecked", async () => {
    stubBackend();
    render(<App />);

    await chooseType("Student");

    expect(textboxNames()).toEqual(STUDENT_FIELDS);
    expect(screen.queryByRole("textbox", { name: "Organization / institution" })).toBeNull();

    expect(optionsInGroup("Workshops")).toEqual(["Workshop: Software testing"]);
    expect(optionsInGroup("Events")).toEqual(["Opening reception"]);
    expect(optionsInGroup("Meals")).toEqual(["Lunch"]);
    expect(optionsInGroup("Other activities")).toEqual(["City tour"]);
    expect(screen.queryByRole("checkbox", { name: "Workshop: Secure coding" })).toBeNull();
    expect(screen.queryByRole("checkbox", { name: "Conference dinner" })).toBeNull();

    for (const name of ["Workshop: Software testing", "Opening reception", "Lunch", "City tour"]) {
      expect(screen.getByRole("checkbox", { name })).not.toBeChecked();
    }
    for (const consent of formConfig.consents) {
      expect(screen.getByRole("checkbox", { name: consent.text })).not.toBeChecked();
    }
  });

  it("AC-002-07 a group without student options is not shown", async () => {
    stubBackend();
    formConfig.options.push({
      id: "oth-gala",
      name: "Gala evening",
      category: "OTHER",
      availableTo: ["EXTERNAL"],
    });
    const tour = formConfig.options.findIndex((o) => o.id === "oth-tour");
    const [removed] = formConfig.options.splice(tour, 1);
    try {
      render(<App />);
      await chooseType("Student");

      expect(screen.getByRole("group", { name: "Workshops" })).toBeInTheDocument();
      expect(screen.queryByRole("group", { name: "Other activities" })).toBeNull();
      expect(screen.queryByRole("checkbox", { name: "Gala evening" })).toBeNull();
    } finally {
      formConfig.options.pop();
      formConfig.options.splice(tour, 0, removed);
    }
  });
});
