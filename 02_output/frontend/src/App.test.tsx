import { act, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { Confirmation } from "./components/Confirmation";
import { OptionGroups } from "./components/OptionGroups";

const CONFIG = {
  conferenceName: "Konferenca 2026",
  options: [],
  consents: [],
  captcha: { mode: "test", siteKey: null },
};

describe("App", () => {
  it("shows loading until the configuration arrives, then the conference name", async () => {
    let finish: (r: Response) => void = () => {};
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise<Response>((resolve) => (finish = resolve))),
    );
    render(<App />);
    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();

    await act(async () => finish(new Response(JSON.stringify(CONFIG))));

    expect(screen.queryByText("Loading…")).not.toBeInTheDocument();
    expect(screen.getByText("Konferenca 2026")).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "Student" })).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("shows an unavailable message and no form when the configuration fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("{}", { status: 503 })),
    );
    render(<App />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The registration form is currently unavailable. Please try again later.",
    );
    expect(screen.queryByText("Loading…")).not.toBeInTheDocument();
    expect(screen.queryByRole("radio")).not.toBeInTheDocument();
  });

  it("ignores an answer that arrives after unmounting", async () => {
    let finish: (r: Response) => void = () => {};
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise<Response>((resolve) => (finish = resolve))),
    );
    const errors = vi.spyOn(console, "error");
    render(<App />).unmount();
    await act(async () => finish(new Response("{}", { status: 500 })));
    expect(errors).not.toHaveBeenCalled();
  });
});

describe("Confirmation", () => {
  it("says when no option was selected", () => {
    render(
      <Confirmation
        registration={{
          id: "1",
          type: "STUDENT",
          firstName: "L",
          lastName: "K",
          options: [],
          registeredAt: "x",
        }}
      />,
    );
    expect(screen.getByText("No options selected.")).toBeInTheDocument();
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });
});

describe("OptionGroups", () => {
  it("renders only categories with options and no error state without an error", () => {
    render(
      <OptionGroups
        options={[
          {
            id: "m",
            name: "Lunch",
            category: "MEAL",
            availableTo: ["STUDENT"],
          },
        ]}
        type="STUDENT"
        selected={new Set()}
        onToggle={vi.fn()}
      />,
    );
    expect(screen.getByRole("group", { name: "Meals" })).toBeInTheDocument();
    expect(
      screen.queryByRole("group", { name: "Workshops" }),
    ).not.toBeInTheDocument();
    const group = screen.getByRole("group", { name: "Options" });
    expect(group).not.toHaveAttribute("aria-invalid");
    expect(group).not.toHaveAttribute("aria-describedby");
    expect(screen.queryByTestId("error-options")).not.toBeInTheDocument();
  });
});
