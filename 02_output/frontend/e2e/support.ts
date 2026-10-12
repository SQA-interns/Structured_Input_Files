import { expect, type APIRequestContext, type Page } from "@playwright/test";

declare const process: { env: Record<string, string | undefined> };

/** Mailpit API of the local stack (`environments.md`). */
export const MAILPIT_URL =
  process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";

export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.si`;
}

export function organizerCredentials(): { username: string; password: string } {
  const username = process.env.E2E_ORGANIZER_USERNAME;
  const password = process.env.E2E_ORGANIZER_PASSWORD;
  if (!username || !password) {
    throw new Error(
      "Set E2E_ORGANIZER_USERNAME and E2E_ORGANIZER_PASSWORD from .env to run the end-to-end tests",
    );
  }
  return { username, password };
}

/** Fills and submits the registration form; returns the accepted registration's JSON. */
export async function register(
  page: Page,
  type: "External participant" | "Student",
  fields: Record<string, string>,
  options: string[],
): Promise<{ status: number; body: { id: string } }> {
  await page.goto("/");
  await page.getByRole("radio", { name: type }).check();
  for (const [label, value] of Object.entries(fields)) {
    await page.getByRole("textbox", { name: label, exact: true }).fill(value);
  }
  for (const option of options) {
    await page.getByRole("checkbox", { name: option, exact: true }).check();
  }
  // Consent wording of the local default configuration (`02_contracts/examples/conference-options.example.json`).
  await page.getByRole("checkbox", { name: /personal data/i }).check();
  await page
    .getByRole("checkbox", { name: "I am not a robot (test mode)" })
    .check();
  const responsePromise = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/registrations") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Register" }).click();
  const response = await responsePromise;
  return { status: response.status(), body: await response.json() };
}

export interface MailMessage {
  ID: string;
  Subject: string;
  To: { Address: string }[];
}

/** Waits until Mailpit holds a message to `address` whose subject starts with `subjectPrefix`. */
export async function awaitMail(
  request: APIRequestContext,
  address: string,
  subjectPrefix: string,
): Promise<{
  id: string;
  text: string;
  attachments: { PartID: string; FileName: string }[];
}> {
  let found: MailMessage | undefined;
  await expect
    .poll(
      async () => {
        const res = await request.get(
          `${MAILPIT_URL}/api/v1/search?query=${encodeURIComponent(`to:"${address}"`)}`,
        );
        const list = (await res.json()) as { messages: MailMessage[] };
        found = list.messages.find((m) => m.Subject.startsWith(subjectPrefix));
        return found !== undefined;
      },
      { timeout: 15_000 },
    )
    .toBe(true);
  const message = await (
    await request.get(`${MAILPIT_URL}/api/v1/message/${found!.ID}`)
  ).json();
  return {
    id: found!.ID,
    text: message.Text as string,
    attachments: message.Attachments,
  };
}

/** Text of every cell of an .xlsx workbook (shared and inline strings), read without a library. */
export async function workbookStrings(xlsx: Uint8Array): Promise<string[]> {
  const view = new DataView(xlsx.buffer, xlsx.byteOffset, xlsx.byteLength);
  let eocd = xlsx.length - 22;
  while (eocd >= 0 && view.getUint32(eocd, true) !== 0x06054b50) eocd--;
  if (eocd < 0) throw new Error("not a zip file");
  const entries = view.getUint16(eocd + 10, true);
  let central = view.getUint32(eocd + 16, true);
  const strings: string[] = [];
  for (let i = 0; i < entries; i++) {
    const method = view.getUint16(central + 10, true);
    const compressedSize = view.getUint32(central + 20, true);
    const nameLength = view.getUint16(central + 28, true);
    const extraLength = view.getUint16(central + 30, true);
    const commentLength = view.getUint16(central + 32, true);
    const local = view.getUint32(central + 42, true);
    const name = new TextDecoder().decode(
      xlsx.subarray(central + 46, central + 46 + nameLength),
    );
    central += 46 + nameLength + extraLength + commentLength;
    if (name !== "xl/sharedStrings.xml" && !name.startsWith("xl/worksheets/"))
      continue;
    const start =
      local +
      30 +
      view.getUint16(local + 26, true) +
      view.getUint16(local + 28, true);
    const data = xlsx.slice(start, start + compressedSize);
    const raw =
      method === 0
        ? data
        : new Uint8Array(
            await new Response(
              new Blob([data])
                .stream()
                .pipeThrough(new DecompressionStream("deflate-raw")),
            ).arrayBuffer(),
          );
    const xml = new TextDecoder().decode(raw);
    for (const match of xml.matchAll(/<t[^>]*>([^<]*)<\/t>/g)) {
      strings.push(
        match[1]
          .replace(/&lt;/g, "<")
          .replace(/&gt;/g, ">")
          .replace(/&quot;/g, '"')
          .replace(/&apos;/g, "'")
          .replace(/&amp;/g, "&"),
      );
    }
  }
  return strings;
}
