import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { stubBackend } from "./support";
import { PHOTO, PRIVACY, fillValidExternal, renderLoaded, type } from "./ui";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("US-004 confirmation", () => {
  it("AC-004-01: shows the confirmation only after the backend accepted the registration", async () => {
    const backend = stubBackend({
      status: 201,
      body: {
        id: "3f2b8c1e-9a4d-4e7b-8f00-1a2b3c4d5e6f",
        type: "EXTERNAL",
        firstName: "Ana",
        lastName: "Novak",
        options: [{ id: "ws-ai", name: "Workshop: AI v praksi", category: "WORKSHOP" }],
      },
    });
    await renderLoaded();
    fillValidExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(
      await screen.findByRole("heading", { level: 2, name: "Registration received" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Thank you, Ana Novak\. Your registration has been received\./),
    ).toBeInTheDocument();
    expect(screen.getByText("Workshop: AI v praksi")).toBeInTheDocument();
    expect(
      screen.getByText(/Registration ID: 3f2b8c1e-9a4d-4e7b-8f00-1a2b3c4d5e6f/),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Register" })).not.toBeInTheDocument();

    const sent = backend.registrationRequests();
    expect(sent).toHaveLength(1);
    expect(sent[0].method).toBe("POST");
    expect(sent[0].body).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.test",
      organization: "Institut Jožef Stefan",
      optionIds: ["ws-ai"],
      consentIds: ["privacy"],
      captchaToken: "TEST-PASS",
    });
  });

  it("AC-004-02: shows server field errors next to the fields, keeps values, no confirmation", async () => {
    stubBackend({
      status: 400,
      body: {
        message: "Validation failed",
        fieldErrors: [
          { field: "email", message: "Email address is not accepted." },
          { field: "organization", message: "Organization is too long." },
        ],
      },
    });
    await renderLoaded();
    fillValidExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    const email = screen.getByLabelText("Email");
    await waitFor(() =>
      expect(email).toHaveAccessibleDescription("Email address is not accepted."),
    );
    expect(email).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText("Organization / institution")).toHaveAccessibleDescription(
      "Organization is too long.",
    );
    expect(screen.getByLabelText("First name")).toHaveValue("Ana");
    expect(email).toHaveValue("ana.novak@example.test");
    expect(
      screen.queryByRole("heading", { name: "Registration received" }),
    ).not.toBeInTheDocument();
  });

  it("AC-004-03: validates required fields, email and consent before sending anything", async () => {
    const backend = stubBackend({ status: 201 });
    await renderLoaded();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findAllByText("This field is required.")).toHaveLength(4);
    expect(screen.getByLabelText("First name")).toHaveAccessibleDescription(
      "This field is required.",
    );
    expect(screen.getByLabelText("First name")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByRole("checkbox", { name: PRIVACY })).toHaveAccessibleDescription(
      "This consent is required.",
    );
    expect(screen.getByRole("checkbox", { name: PHOTO })).not.toHaveAttribute(
      "aria-invalid",
      "true",
    );

    fillValidExternal();
    type("Email", "ana.novak@example");
    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() =>
      expect(screen.getByLabelText("Email")).toHaveAccessibleDescription(
        "Enter a valid email address.",
      ),
    );
    expect(backend.registrationRequests()).toHaveLength(0);
  });

  it("AC-004-04: a network failure shows a general error and no confirmation", async () => {
    stubBackend("network-error");
    await renderLoaded();
    fillValidExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Registration could not be completed. Please try again later.",
    );
    expect(
      screen.queryByRole("heading", { name: "Registration received" }),
    ).not.toBeInTheDocument();
  });

  it("AC-004-04: a server error shows a general error without technical details", async () => {
    stubBackend({ status: 500, body: { message: "Internal error", fieldErrors: [] } });
    await renderLoaded();
    fillValidExternal();

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Registration could not be completed. Please try again later.");
    expect(alert).not.toHaveTextContent("500");
    expect(
      screen.queryByRole("heading", { name: "Registration received" }),
    ).not.toBeInTheDocument();
  });
});
