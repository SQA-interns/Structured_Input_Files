import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "./RegistrationForm";
import type { ConferenceOption, Settings } from "./types";

const settings: Settings = {
  conferenceName: "C",
  consent: { id: "c", text: "I agree." },
  recaptcha: { testMode: true, siteKey: "" },
};
const options: ConferenceOption[] = [
  {
    id: "both",
    name: "Both",
    category: "WORKSHOP",
    registrationTypes: ["EXTERNAL", "STUDENT"],
  },
  {
    id: "ext",
    name: "External only",
    category: "WORKSHOP",
    registrationTypes: ["EXTERNAL"],
  },
];

describe("RegistrationForm", () => {
  it("drops selections not open to the new type and sends only the remaining ones", async () => {
    const fetchMock = vi.fn(async () => new Response("{}", { status: 500 }));
    vi.stubGlobal("fetch", fetchMock);
    render(
      <RegistrationForm
        settings={settings}
        options={options}
        onAccepted={() => {}}
      />,
    );
    fireEvent.click(screen.getByRole("checkbox", { name: "Both" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "External only" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "External only" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "External only" }));

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    for (const [label, value] of [
      ["First name", "L"],
      ["Last name", "K"],
      ["Email", "l@k.si"],
      ["Study institution", "U"],
      ["Study programme", "P"],
      ["Student ID", "1"],
    ]) {
      fireEvent.change(screen.getByRole("textbox", { name: label }), {
        target: { value },
      });
    }
    fireEvent.click(screen.getByRole("checkbox", { name: "I agree." }));
    fireEvent.click(
      screen.getByRole("checkbox", { name: "I am not a robot (test mode)" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await screen.findByText(
      "Your registration could not be sent. Please try again.",
    );
    const body = JSON.parse(
      String(
        (fetchMock.mock.calls[0] as unknown as [string, RequestInit])[1].body,
      ),
    );
    expect(body.optionIds).toEqual(["both"]);
    expect(body.type).toBe("STUDENT");
    expect(body).not.toHaveProperty("organization");
    vi.unstubAllGlobals();
  });
});
