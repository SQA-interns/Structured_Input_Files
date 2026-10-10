import { cleanup, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  accepted,
  chooseType,
  fillValidExternal,
  mockBackend,
  registrationPosts,
  renderApp,
  submit,
} from "./support";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("US-004 registration confirmation", () => {
  it("AC-004-01 shows the confirmation only after the backend accepted the registration", async () => {
    const requests = mockBackend({ registration: { status: 201, body: accepted } });
    renderApp();
    await chooseType("External participant");
    fillValidExternal();

    submit();

    const status = await screen.findByRole("status");
    expect(
      within(status).getByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
    expect(status).toHaveTextContent(`Reference: ${accepted.reference}`);
    expect(status).toHaveTextContent("Ana Novak");
    expect(status).toHaveTextContent("Delavnica: umetna inteligenca");
    expect(status).toHaveTextContent("Kosilo");
    expect(screen.queryByRole("form", { name: "Registration form" })).not.toBeInTheDocument();
    expect(registrationPosts(requests)[0].body).toMatchObject({
      type: "EXTERNAL",
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.si",
      organization: "Institut Jožef Stefan",
      optionIds: ["ws-ai", "meal-lunch"],
      consentIds: ["data-processing"],
    });
  });

  it("AC-004-02 keeps the values and shows a general error when the backend fails", async () => {
    mockBackend({
      registration: {
        status: 500,
        body: {
          status: 500,
          error: "INTERNAL_ERROR",
          message: "The registration could not be processed.",
          fieldErrors: [],
        },
      },
    });
    renderApp();
    await chooseType("External participant");
    fillValidExternal();

    submit();

    await waitFor(() =>
      expect(document.getElementById("form-error")).toHaveTextContent(
        "Your registration could not be processed. Please try again later.",
      ),
    );
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.getByLabelText("First name")).toHaveValue("Ana");
    expect(screen.getByLabelText("Organization / institution")).toHaveValue(
      "Institut Jožef Stefan",
    );
  });

  it("AC-004-02 shows a general error without details when the network fails", async () => {
    mockBackend({ registration: "network-error" });
    renderApp();
    await chooseType("External participant");
    fillValidExternal();

    submit();

    await waitFor(() =>
      expect(document.getElementById("form-error")).toHaveTextContent(
        "Your registration could not be processed. Please try again later.",
      ),
    );
    expect(document.body).not.toHaveTextContent("Failed to fetch");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toHaveValue("ana.novak@example.si");
  });
});
