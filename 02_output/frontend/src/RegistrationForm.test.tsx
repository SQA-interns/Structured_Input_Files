import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "./RegistrationForm";
import type { FormConfig } from "./types";

const config: FormConfig = {
  conferenceName: "K",
  recaptchaSiteKey: "",
  recaptchaTestMode: true,
  consents: [
    { id: "privacy", text: "I agree", mandatory: true },
    { id: "photos", text: "Photos ok", mandatory: false },
  ],
  options: [
    {
      id: "ws",
      name: "Workshop",
      category: "WORKSHOP",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
    { id: "ext", name: "Dinner", category: "EVENT", availableTo: ["EXTERNAL"] },
    {
      id: "ml",
      name: "Lunch",
      category: "MEAL",
      availableTo: ["EXTERNAL", "STUDENT"],
    },
  ],
};

let posts: Record<string, unknown>[] = [];

function backend(...answers: Response[]) {
  posts = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (_url: string, init?: RequestInit) => {
      posts.push(JSON.parse(init!.body as string));
      return answers.shift() ?? new Response("{}", { status: 503 });
    }),
  );
}

const created = () =>
  new Response(JSON.stringify({ id: "x", registeredAt: "t" }), { status: 201 });
const rejected = (fieldErrors: Record<string, string>) =>
  new Response(JSON.stringify({ message: "m", fieldErrors }), { status: 400 });
const input = (label: string) =>
  screen.getByLabelText(label, { selector: "input" });
const box = (name: string) => screen.getByRole("checkbox", { name });
const type = (label: string, value: string) =>
  fireEvent.change(input(label), { target: { value } });
const register = () =>
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
const settle = () => act(async () => {});

function fillExternal(withCaptcha = true) {
  fireEvent.click(screen.getByRole("radio", { name: "External participant" }));
  type("First name", "Ana");
  type("Last name", "Novak");
  type("Email", "ana@example.com");
  type("Organization / institution", "Acme");
  fireEvent.click(box("I agree"));
  if (withCaptcha) {
    fireEvent.click(box("I am not a robot (test mode)"));
  }
}

afterEach(() => {
  delete window.grecaptcha;
});

describe("RegistrationForm", () => {
  it("starts empty: no type, no alert, empty fields and an unticked reCAPTCHA", () => {
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    expect(
      screen.getByRole("radio", { name: "External participant" }),
    ).not.toBeChecked();
    expect(screen.queryByRole("alert")).toBeNull();

    fireEvent.click(
      screen.getByRole("radio", { name: "External participant" }),
    );

    expect(
      screen.getByRole("radio", { name: "External participant" }),
    ).toBeChecked();
    expect(screen.getByRole("radio", { name: "Student" })).not.toBeChecked();
    for (const label of [
      "First name",
      "Last name",
      "Email",
      "Organization / institution",
    ]) {
      expect(input(label)).toHaveValue("");
    }
    expect(box("I am not a robot (test mode)")).not.toBeChecked();
  });

  it("requires the reCAPTCHA before sending", async () => {
    backend(created());
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    fillExternal(false);

    register();
    await settle();

    expect(box("I am not a robot (test mode)")).toHaveAccessibleDescription(
      "Confirm that you are not a robot.",
    );
    expect(posts).toHaveLength(0);
  });

  it("prevents the browser's own form submission", () => {
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    expect(
      fireEvent.submit(screen.getByRole("form", { name: "Registration form" })),
    ).toBe(false);
  });

  it("unticking one of two options or consents keeps the other", async () => {
    backend(created());
    const onRegistered = vi.fn();
    render(<RegistrationForm config={config} onRegistered={onRegistered} />);
    fillExternal();
    fireEvent.click(box("Workshop"));
    fireEvent.click(box("Lunch"));
    fireEvent.click(box("Workshop"));
    fireEvent.click(box("Photos ok"));
    fireEvent.click(box("Photos ok"));

    register();
    await settle();

    expect(posts[0]).toMatchObject({
      optionIds: ["ml"],
      consents: ["privacy"],
    });
    expect(onRegistered).toHaveBeenCalledWith({
      firstName: "Ana",
      lastName: "Novak",
      optionNames: ["Lunch"],
    });
  });

  it("switching type drops only the options the new type cannot have", () => {
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    fireEvent.click(
      screen.getByRole("radio", { name: "External participant" }),
    );
    fireEvent.click(box("Workshop"));
    fireEvent.click(box("Dinner"));

    fireEvent.click(screen.getByRole("radio", { name: "Student" }));
    expect(box("Workshop")).toBeChecked();
    fireEvent.click(
      screen.getByRole("radio", { name: "External participant" }),
    );

    expect(box("Workshop")).toBeChecked();
    expect(box("Dinner")).not.toBeChecked();
  });

  it("hides a category without options for the chosen type", () => {
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    fireEvent.click(screen.getByRole("radio", { name: "Student" }));

    expect(screen.queryByRole("group", { name: "Events" })).toBeNull();
    expect(
      within(screen.getByRole("group", { name: "Meals" })).getAllByRole(
        "checkbox",
      ),
    ).toHaveLength(1);
  });

  it("clears the token after a failure, so a new reCAPTCHA is needed", async () => {
    backend(rejected({ email: "Bad email" }));
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    fillExternal();
    register();
    await settle();
    expect(input("Email")).toHaveAccessibleDescription("Bad email");

    register();
    await settle();

    expect(posts).toHaveLength(1);
    expect(box("I am not a robot (test mode)")).toHaveAccessibleDescription(
      "Confirm that you are not a robot.",
    );
  });

  it("a general failure clears earlier field errors", async () => {
    backend(
      rejected({ email: "Bad email" }),
      new Response("{}", { status: 500 }),
    );
    render(<RegistrationForm config={config} onRegistered={vi.fn()} />);
    fillExternal();
    register();
    await settle();
    fireEvent.click(box("I am not a robot (test mode)"));

    register();
    await settle();

    expect(posts).toHaveLength(2);
    expect(screen.getByRole("alert")).toBeInTheDocument();
    expect(input("Email")).not.toHaveAttribute("aria-invalid");
    expect(screen.queryByText("Bad email")).toBeNull();
  });

  it("resets the Google widget once after each failed submission", async () => {
    const reset = vi.fn();
    let tokenCallback: (token: string) => void = () => {};
    window.grecaptcha = {
      ready: (callback) => callback(),
      render: (_element, options) => {
        tokenCallback = options.callback;
        return 3;
      },
      reset,
    };
    backend(
      new Response("{}", { status: 500 }),
      new Response("{}", { status: 500 }),
    );
    render(
      <RegistrationForm
        config={{ ...config, recaptchaTestMode: false, recaptchaSiteKey: "k" }}
        onRegistered={vi.fn()}
      />,
    );
    fillExternal(false);
    await settle();
    act(() => tokenCallback("google-token"));

    register();
    await settle();
    expect(posts[0]).toMatchObject({ recaptchaToken: "google-token" });
    expect(reset).toHaveBeenCalledTimes(1);
    expect(reset).toHaveBeenCalledWith(3);

    act(() => tokenCallback("second-token"));
    register();
    await settle();
    expect(reset).toHaveBeenCalledTimes(2);
  });
});
