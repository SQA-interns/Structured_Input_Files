import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Confirmation } from "./Confirmation";

const registration = {
  id: "id-1",
  type: "STUDENT",
  firstName: "Žiga",
  lastName: "Šuštaršič",
  options: [
    { id: "ws-a", name: "Workshop A", category: "WORKSHOP" },
    { id: "meal", name: "Lunch", category: "MEAL" },
  ],
  registeredAt: "2026-10-19T12:00:00.000Z",
};

describe("Confirmation", () => {
  it("AC-004-01 names the participant and lists the selected options", () => {
    render(<Confirmation registration={registration} />);

    const status = screen.getByRole("status");
    expect(
      within(status).getByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
    expect(status).toHaveTextContent("Žiga Šuštaršič");
    expect(
      within(status)
        .getAllByRole("listitem")
        .map((li) => li.textContent),
    ).toEqual(["Workshop A", "Lunch"]);
  });

  it("AC-004-01 says so when no option was selected", () => {
    render(<Confirmation registration={{ ...registration, options: [] }} />);

    expect(screen.getByRole("status")).toHaveTextContent("No options selected.");
    expect(screen.queryAllByRole("listitem")).toHaveLength(0);
  });

  it("AC-004-01 shows markup in names as text", () => {
    render(<Confirmation registration={{ ...registration, firstName: "<b>Ana</b>" }} />);

    expect(screen.getByRole("status")).toHaveTextContent("<b>Ana</b>");
    expect(screen.getByRole("status").querySelector("b")).toBeNull();
  });
});
