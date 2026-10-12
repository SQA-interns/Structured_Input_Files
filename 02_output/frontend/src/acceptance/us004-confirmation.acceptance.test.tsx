import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "../App";
import { installFakeBackend } from "./fakeBackend";
import { fillValidExternal, submit, waitForForm } from "./formHelpers";

const FAILURE_MESSAGE =
  "Your registration could not be sent. Please try again.";

describe("US-004 registration confirmation", () => {
  it("AC-004-01 shows the confirmation with the selected options after the backend accepts", async () => {
    installFakeBackend();
    render(<App />);
    await waitForForm();
    fillValidExternal();

    submit();

    expect(
      await screen.findByRole("heading", { name: "Registration received" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Thank you, Ana Novak. A confirmation has been sent to ana.novak@example.si.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole("listitem")).toHaveTextContent(
      "Workshop A: Testing",
    );
    expect(
      screen.queryByRole("button", { name: "Register" }),
    ).not.toBeInTheDocument();
  });

  it("AC-004-02 shows no confirmation but an error and keeps the values when the backend rejects", async () => {
    const backend = installFakeBackend();
    backend.reply({
      status: 400,
      body: {
        status: 400,
        title: "Bad Request",
        fieldErrors: [{ field: "optionIds", message: "Unknown option." }],
      },
    });
    render(<App />);
    await waitForForm();
    fillValidExternal();

    submit();

    expect(await screen.findByText("Unknown option.")).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Registration received" }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Last name" })).toHaveValue(
      "Novak",
    );
  });

  it("AC-004-02 shows no confirmation but an error and keeps the values on a server error", async () => {
    const backend = installFakeBackend();
    backend.reply({
      status: 500,
      body: { status: 500, title: "Internal Server Error" },
    });
    render(<App />);
    await waitForForm();
    fillValidExternal();

    submit();

    expect(await screen.findByText(FAILURE_MESSAGE)).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Registration received" }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Email" })).toHaveValue(
      "ana.novak@example.si",
    );
  });

  it("AC-004-02 shows no confirmation but an error and keeps the values when the backend cannot be reached", async () => {
    const backend = installFakeBackend();
    backend.reply("network-error");
    render(<App />);
    await waitForForm();
    fillValidExternal();

    submit();

    expect(await screen.findByText(FAILURE_MESSAGE)).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Registration received" }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "First name" })).toHaveValue(
      "Ana",
    );
  });

  it("AC-004-03 shows no confirmation and blocks a second submission while the backend has not answered", async () => {
    const backend = installFakeBackend();
    backend.reply("pending");
    render(<App />);
    await waitForForm();
    fillValidExternal();

    submit();

    const sending = await screen.findByRole("button", { name: "Sending…" });
    expect(sending).toBeDisabled();
    sending.click();
    expect(backend.registrations).toHaveLength(1);
    expect(
      screen.queryByRole("heading", { name: "Registration received" }),
    ).not.toBeInTheDocument();
  });
});
