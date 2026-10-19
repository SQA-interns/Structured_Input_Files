import { describe, expect, it } from "vitest";
import { groupByCategory, optionsFor } from "./options";
import { testConfig } from "./testConfig";

describe("options", () => {
  it("AC-001-08 externals see only options available to them", () => {
    expect(optionsFor(testConfig.options, "EXTERNAL").map((o) => o.id)).toEqual([
      "ws-a",
      "oth-t",
      "meal-l",
    ]);
  });

  it("AC-002-07 students see only options available to them", () => {
    expect(optionsFor(testConfig.options, "STUDENT").map((o) => o.id)).toEqual([
      "ws-a",
      "meal-l",
      "oth-m",
      "ev-s",
    ]);
  });

  it("AC-001-08 groups follow the category order and skip empty categories", () => {
    const groups = groupByCategory(optionsFor(testConfig.options, "EXTERNAL"));

    expect(groups.map((g) => g.legend)).toEqual(["Workshops", "Meals", "Other activities"]);
    expect(groups.map((g) => g.category)).toEqual(["WORKSHOP", "MEAL", "OTHER"]);
    expect(groups[2].options.map((o) => o.id)).toEqual(["oth-t"]);
  });

  it("AC-002-07 student groups include events", () => {
    const groups = groupByCategory(optionsFor(testConfig.options, "STUDENT"));

    expect(groups.map((g) => g.legend)).toEqual([
      "Workshops",
      "Events",
      "Meals",
      "Other activities",
    ]);
  });
});
