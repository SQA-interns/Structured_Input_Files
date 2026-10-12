import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";
import {
  EXTERNAL_FIELDS,
  GROUPS,
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

describe("US-001 external participant form", () => {
  it("AC-001-08 shows exactly the external fields, grouped active options and unchecked consents", async () => {
    stubBackend();
    render(<App />);

    await chooseType("External participant");

    expect(textboxNames()).toEqual(EXTERNAL_FIELDS);
    expect(screen.queryByRole("textbox", { name: "Study institution" })).toBeNull();
    expect(screen.queryByRole("textbox", { name: "Study programme" })).toBeNull();
    expect(screen.queryByRole("textbox", { name: "Student ID" })).toBeNull();

    for (const group of GROUPS) {
      expect(screen.getByRole("group", { name: group })).toBeInTheDocument();
    }
    expect(optionsInGroup("Workshops")).toEqual([
      "Workshop: Software testing",
      "Workshop: Secure coding",
    ]);
    expect(optionsInGroup("Events")).toEqual(["Opening reception"]);
    expect(optionsInGroup("Meals")).toEqual(["Lunch", "Conference dinner"]);
    expect(optionsInGroup("Other activities")).toEqual(["City tour"]);

    for (const option of formConfig.options) {
      expect(screen.getByRole("checkbox", { name: option.name })).not.toBeChecked();
    }
    for (const consent of formConfig.consents) {
      expect(screen.getByRole("checkbox", { name: consent.text })).not.toBeChecked();
    }
  });
});
