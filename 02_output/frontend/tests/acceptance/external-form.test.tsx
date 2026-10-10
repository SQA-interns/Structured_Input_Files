import { cleanup, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  accepted,
  chooseType,
  errorOf,
  fill,
  fillValidExternal,
  formConfig,
  mockBackend,
  registrationPosts,
  renderApp,
  submit,
} from "./support";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  document.head.querySelectorAll("script").forEach((s) => s.remove());
});

function textboxLabels(): string[] {
  const form = screen.getByRole("form", { name: "Registration form" });
  return within(form)
    .getAllByRole("textbox")
    .map((el) => (el as HTMLInputElement).labels?.[0]?.textContent?.trim() ?? "");
}

function optionLabels(): string[] {
  return formConfig.options
    .map((o) => o.name)
    .filter((name) => screen.queryByRole("checkbox", { name }) !== null);
}

describe("US-001 external participant form", () => {
  it("AC-001-01 shows exactly the external fields, grouped options and unchecked consents", async () => {
    mockBackend();
    renderApp();

    await chooseType("External participant");

    expect(textboxLabels()).toEqual([
      "First name",
      "Last name",
      "Email",
      "Organization / institution",
    ]);
    expect(optionLabels()).toEqual([
      "Delavnica: umetna inteligenca",
      "Gala večerja",
      "Kosilo",
      "Ogled Ljubljane",
    ]);
    for (const legend of ["Workshops", "Events", "Meals", "Other activities"]) {
      expect(screen.getByRole("group", { name: legend })).toBeInTheDocument();
    }
    for (const consent of formConfig.consents) {
      expect(screen.getByRole("checkbox", { name: consent.text })).not.toBeChecked();
    }
    for (const box of screen.getAllByRole("checkbox")) {
      expect(box).not.toBeChecked();
    }
  });

  it("AC-001-12 validates before submission and shows errors next to the fields", async () => {
    const requests = mockBackend();
    renderApp();
    await chooseType("External participant");

    fill("First name", "   ");
    fill("Email", "ana.novak");
    submit();

    expect(errorOf("firstName")).toHaveTextContent("This field is required.");
    expect(errorOf("lastName")).toHaveTextContent("This field is required.");
    expect(errorOf("email")).toHaveTextContent("Enter a valid email address.");
    expect(errorOf("organization")).toHaveTextContent("This field is required.");
    expect(errorOf("consents.data-processing")).toHaveTextContent("This consent is required.");
    expect(errorOf("captchaToken")).toHaveTextContent("Please confirm that you are not a robot.");
    expect(screen.getByLabelText("First name")).toHaveAttribute(
      "aria-describedby",
      expect.stringContaining("firstName-error"),
    );
    expect(registrationPosts(requests)).toHaveLength(0);
  });

  it("AC-001-12 rejects too long values and control characters before submission", async () => {
    const requests = mockBackend();
    renderApp();
    await chooseType("External participant");
    fillValidExternal();

    fill("First name", "A".repeat(201));
    fill("Last name", "Novak\u0007");
    submit();

    expect(errorOf("firstName")).toHaveTextContent("This value is too long.");
    expect(errorOf("lastName")).toHaveTextContent(
      "This value contains characters that are not allowed.",
    );
    expect(registrationPosts(requests)).toHaveLength(0);
  });

  it("AC-001-12 shows the backend's field errors next to their fields", async () => {
    mockBackend({
      registration: {
        status: 409,
        body: {
          status: 409,
          error: "DUPLICATE_EMAIL",
          message: "The registration was not accepted.",
          fieldErrors: [
            {
              field: "email",
              code: "DUPLICATE_EMAIL",
              message: "A registration with this email already exists.",
            },
          ],
        },
      },
    });
    renderApp();
    await chooseType("External participant");
    fillValidExternal();

    submit();

    await waitFor(() =>
      expect(errorOf("email")).toHaveTextContent("A registration with this email already exists."),
    );
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("AC-001-09 sends the test-mode reCAPTCHA token with the registration", async () => {
    const requests = mockBackend({ registration: { status: 201, body: accepted } });
    renderApp();
    await chooseType("External participant");
    fillValidExternal();

    submit();

    await waitFor(() => expect(registrationPosts(requests)).toHaveLength(1));
    expect(registrationPosts(requests)[0].body).toMatchObject({ captchaToken: "test-pass" });
  });

  it("AC-001-09 loads the Google reCAPTCHA widget with the configured site key outside test mode", async () => {
    mockBackend({
      config: { ...formConfig, recaptcha: { siteKey: "site-key-from-config", testMode: false } },
    });
    renderApp();

    await chooseType("External participant");

    await waitFor(() => {
      const script = Array.from(document.querySelectorAll("script")).find((s) =>
        s.src.startsWith("https://www.google.com/recaptcha/api.js"),
      );
      expect(script).toBeDefined();
    });
    expect(screen.queryByRole("checkbox", { name: "I am not a robot (test mode)" })).toBeNull();
    expect(document.querySelector("[data-sitekey='site-key-from-config']")).not.toBeNull();
  });
});
