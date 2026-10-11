import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { FormConfig } from "./api";
import { RegistrationForm } from "./RegistrationForm";

vi.mock("./recaptcha", () => ({
  renderRecaptcha: vi.fn(async () => ({ token: () => "widget-token", reset: vi.fn() })),
}));

const config: FormConfig = {
  conferenceName: "K",
  captcha: { mode: "recaptcha", siteKey: "site" },
  consents: [{ id: "privacy", text: "Privacy", mandatory: true }],
  options: [
    { id: "a", name: "Ext only", category: "EVENT", availableTo: ["EXTERNAL"] },
    { id: "b", name: "Both", category: "MEAL", availableTo: ["EXTERNAL", "STUDENT"] },
  ],
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

function fill() {
  for (const [label, value] of [
    ["First name", "Ana"],
    ["Last name", "Novak"],
    ["Email", "ana@x.si"],
    ["Organization / institution", "IJS"],
  ]) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
  }
  fireEvent.click(screen.getByRole("checkbox", { name: "Privacy" }));
}

function stubPost(status: number, body: unknown) {
  const fetchMock = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("RegistrationForm", () => {
  it("renders the reCAPTCHA container and sends the widget token", async () => {
    const fetchMock = stubPost(201, { id: "1", firstName: "Ana", lastName: "Novak", options: [] });
    const onAccepted = vi.fn();
    const { container } = render(<RegistrationForm config={config} onAccepted={onAccepted} />);
    expect(container.querySelector("#recaptcha")).not.toBeNull();
    expect(screen.queryByText("Test mode: captcha disabled")).not.toBeInTheDocument();
    fill();
    fireEvent.click(screen.getByRole("checkbox", { name: "Ext only" }));

    await waitFor(() => {
      fireEvent.click(screen.getByRole("button", { name: "Register" }));
      expect(onAccepted).toHaveBeenCalled();
    });
    const init = (fetchMock.mock.calls[0] as unknown as [string, RequestInit])[1];
    expect(JSON.parse(init.body as string)).toMatchObject({
      captchaToken: "widget-token",
      optionIds: ["a"],
    });
  });

  it("unchecks options the new type cannot select", () => {
    render(<RegistrationForm config={config} onAccepted={vi.fn()} />);
    fireEvent.click(screen.getByRole("checkbox", { name: "Ext only" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Both" }));

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    fireEvent.click(screen.getByRole("radio", { name: "External participant" }));

    expect(screen.getByRole("checkbox", { name: "Ext only" })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Both" })).toBeChecked();
  });

  it("maps option, captcha and consent errors and sends unknown fields to the general error", async () => {
    stubPost(400, {
      fieldErrors: [
        { field: "optionIds", message: "Option gone." },
        { field: "captchaToken", message: "Robot?" },
        { field: "consents.privacy", message: "Consent missing." },
        { field: "type", message: "Bad type." },
      ],
    });
    render(<RegistrationForm config={config} onAccepted={vi.fn()} />);
    fill();
    await waitFor(() => expect(screen.getByRole("button", { name: "Register" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByText("Option gone.")).toHaveAttribute("id", "options-error");
    expect(screen.getByText("Robot?")).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Privacy" })).toHaveAccessibleDescription(
      "Consent missing.",
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Registration could not be completed.");
  });

  it("shows the rate-limit message on 429", async () => {
    stubPost(429, {});
    render(<RegistrationForm config={config} onAccepted={vi.fn()} />);
    fill();
    await waitFor(() => expect(screen.getByRole("button", { name: "Register" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Too many attempts. Please wait a minute and try again.",
    );
  });
});
