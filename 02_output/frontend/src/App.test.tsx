import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App, LOAD_ERROR } from "./App";
import { Confirmation } from "./Confirmation";
import { GENERAL_ERROR, RegistrationForm } from "./RegistrationForm";
import type { FormConfig } from "./types";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

const config: FormConfig = {
  conferenceName: "Konferenca",
  recaptcha: { siteKey: "", testMode: true },
  options: [
    { id: "ws", name: "Workshop", category: "WORKSHOP", availableTo: ["EXTERNAL", "STUDENT"] },
    { id: "gala", name: "Gala", category: "EVENT", availableTo: ["EXTERNAL"] },
  ],
  consents: [{ id: "privacy", text: "Privacy", mandatory: true }],
};

function stub(status: number, body: unknown) {
  const fetchMock = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function fillExternal() {
  fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
  fireEvent.change(screen.getByRole("textbox", { name: "First name" }), {
    target: { value: " Ana " },
  });
  fireEvent.change(screen.getByRole("textbox", { name: "Last name" }), {
    target: { value: "Novak" },
  });
  fireEvent.change(screen.getByRole("textbox", { name: "Email" }), {
    target: { value: "ana@example.org" },
  });
  fireEvent.change(screen.getByRole("textbox", { name: "Organization / institution" }), {
    target: { value: "Org" },
  });
}

describe("App", () => {
  it("shows a loading text, then the conference title", async () => {
    stub(200, config);
    render(<App />);

    expect(screen.getByText("Loading…")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Conference registration");
    expect(
      await screen.findByRole("heading", { level: 1, name: "Konferenca registration" }),
    ).toBeInTheDocument();
  });

  it("shows a general message when the configuration cannot be loaded", async () => {
    stub(500, {});
    render(<App />);

    expect(await screen.findByRole("alert")).toHaveTextContent(LOAD_ERROR);
    expect(screen.queryByRole("radio")).toBeNull();
  });

  it("ignores a late answer after unmounting", async () => {
    let resolve: (r: Response) => void = () => undefined;
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise<Response>((r) => (resolve = r))),
    );
    const { unmount } = render(<App />);
    unmount();

    resolve(new Response(JSON.stringify(config), { status: 200 }));
    await Promise.resolve();
  });

  it("ignores a late failure after unmounting", async () => {
    let reject: (e: Error) => void = () => undefined;
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise<Response>((_, r) => (reject = r))),
    );
    const { unmount } = render(<App />);
    unmount();

    reject(new Error("gone"));
    await Promise.resolve();
  });
});

describe("Confirmation", () => {
  it("says when no options were selected", () => {
    render(
      <Confirmation
        registration={{
          id: "1",
          type: "STUDENT",
          firstName: "Luka",
          lastName: "Kranjc",
          options: [],
          registeredAt: "2026-10-12T10:00:00Z",
        }}
      />,
    );

    expect(screen.getByText("No options selected.")).toBeInTheDocument();
    expect(screen.queryByRole("list")).toBeNull();
  });
});

describe("RegistrationForm", () => {
  it("shows nothing but the type choice until a type is chosen", () => {
    render(<RegistrationForm config={config} onCreated={vi.fn()} />);

    expect(screen.getByRole("radiogroup", { name: "Registration type" })).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).toBeNull();
    expect(screen.queryByRole("button")).toBeNull();
  });

  it("sends trimmed fields of the type, available selected options and given consents", async () => {
    const fetchMock = stub(201, { id: "1", firstName: "Ana", lastName: "Novak", options: [] });
    const onCreated = vi.fn();
    render(<RegistrationForm config={config} onCreated={onCreated} />);
    fillExternal();
    fireEvent.click(screen.getByRole("checkbox", { name: "Gala" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Workshop" }));
    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Privacy" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Privacy" }));

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await vi.waitFor(() => expect(onCreated).toHaveBeenCalled());
    const body = JSON.parse(
      String(((fetchMock.mock.calls[0] as unknown[])[1] as RequestInit).body),
    );
    expect(body).toEqual({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana@example.org",
      organization: "Org",
      optionIds: ["ws", "gala"],
      consents: [],
      recaptchaToken: "",
    });
  });

  it("does not send options the student type cannot select", async () => {
    const fetchMock = stub(201, { id: "1", firstName: "A", lastName: "B", options: [] });
    render(<RegistrationForm config={config} onCreated={vi.fn()} />);
    fillExternal();
    fireEvent.click(screen.getByRole("checkbox", { name: "Gala" }));
    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    for (const [name, value] of [
      ["First name", "A"],
      ["Last name", "B"],
      ["Email", "a@b.org"],
      ["Study institution", "U"],
      ["Study programme", "P"],
      ["Student ID", "1"],
    ]) {
      fireEvent.change(screen.getByRole("textbox", { name }), { target: { value } });
    }

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalled());
    const body = JSON.parse(
      String(((fetchMock.mock.calls[0] as unknown[])[1] as RequestInit).body),
    );
    expect(body.optionIds).toEqual([]);
    expect(body.organization).toBeUndefined();
    expect(body.studentId).toBe("1");
  });

  it("places type, consent-list and token errors and reports unknown fields generally", async () => {
    stub(400, {
      fieldErrors: [
        { field: "type", message: "Type!" },
        { field: "consents", message: "Unknown consent" },
        { field: "recaptchaToken", message: "Robot?" },
        { field: "studentId", message: "Not for you" },
      ],
    });
    render(<RegistrationForm config={config} onCreated={vi.fn()} />);
    fillExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("Type!")).toHaveAttribute("id", "type-error");
    expect(document.getElementById("consents-error")).toHaveTextContent("Unknown consent");
    expect(document.getElementById("recaptchaToken-error")).toHaveTextContent("Robot?");
    expect(screen.getByText(GENERAL_ERROR)).toBeInTheDocument();
    expect(screen.queryByText("Not for you")).toBeNull();
  });

  it("does not show the general error when every field error is placed", async () => {
    stub(400, { fieldErrors: [{ field: "email", message: "E!" }] });
    render(<RegistrationForm config={config} onCreated={vi.fn()} />);
    fillExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("E!")).toBeInTheDocument();
    expect(screen.queryByText(GENERAL_ERROR)).toBeNull();
  });

  it("clears errors when the type changes", async () => {
    stub(500, {});
    render(<RegistrationForm config={config} onCreated={vi.fn()} />);
    fillExternal();
    fireEvent.change(screen.getByRole("textbox", { name: "Email" }), { target: { value: "x" } });
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(await screen.findByText("Enter a valid email address.")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));

    expect(screen.queryByText("Enter a valid email address.")).toBeNull();
    expect(screen.queryByText(GENERAL_ERROR)).toBeNull();
  });

  it("disables the button while sending and ignores repeated submits", async () => {
    let resolve: (r: Response) => void = () => undefined;
    const fetchMock = vi.fn(() => new Promise<Response>((r) => (resolve = r)));
    vi.stubGlobal("fetch", fetchMock);
    render(<RegistrationForm config={config} onCreated={vi.fn()} />);
    fillExternal();
    const button = screen.getByRole("button", { name: "Register" });

    fireEvent.click(button);
    await vi.waitFor(() => expect(button).toBeDisabled());
    fireEvent.submit(button.closest("form")!);
    expect(fetchMock).toHaveBeenCalledTimes(1);

    resolve(new Response("{}", { status: 500 }));
    expect(await screen.findByText(GENERAL_ERROR)).toBeInTheDocument();
    expect(button).toBeEnabled();
  });

  it("ignores a submit before a type is chosen", () => {
    const fetchMock = stub(201, {});
    const { container } = render(<RegistrationForm config={config} onCreated={vi.fn()} />);

    fireEvent.submit(container.querySelector("form")!);

    expect(fetchMock).not.toHaveBeenCalled();
  });
});
