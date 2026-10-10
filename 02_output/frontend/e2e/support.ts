import { inflateRawSync } from "node:zlib";
import { expect, type APIRequestContext, type Page } from "@playwright/test";

/** End-to-end helpers: the local stack (docker compose) with Mailpit and test-mode reCAPTCHA. */
export const mailpitUrl = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";

export function organizerCredentials(): { username: string; password: string } {
  const username = process.env.E2E_ORGANIZER_USERNAME ?? "";
  const password = process.env.E2E_ORGANIZER_PASSWORD ?? "";
  expect(username, "E2E_ORGANIZER_USERNAME must be set").not.toBe("");
  expect(password, "E2E_ORGANIZER_PASSWORD must be set").not.toBe("");
  return { username, password };
}

export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}${Math.floor(Math.random() * 1e6).toString(36)}@example.si`;
}

interface FormConfig {
  options: { id: string; name: string; category: string; audience: string[] }[];
  consents: { id: string; text: string; mandatory: boolean }[];
}

export async function formConfig(request: APIRequestContext): Promise<FormConfig> {
  const response = await request.get("/api/form-config");
  expect(response.status()).toBe(200);
  return (await response.json()) as FormConfig;
}

/** Fills the common part of the form, picks the first option for the type and the consents. */
export async function fillCommon(
  page: Page,
  config: FormConfig,
  type: "EXTERNAL" | "STUDENT",
  person: { firstName: string; lastName: string; email: string },
): Promise<string[]> {
  await page.getByLabel("First name").fill(person.firstName);
  await page.getByLabel("Last name").fill(person.lastName);
  await page.getByLabel("Email").fill(person.email);
  const chosen = config.options.filter((o) => o.audience.includes(type)).slice(0, 1);
  for (const option of chosen) {
    await page.getByRole("checkbox", { name: option.name, exact: true }).check();
  }
  for (const consent of config.consents.filter((c) => c.mandatory)) {
    await page.getByRole("checkbox", { name: consent.text, exact: true }).check();
  }
  await page.getByRole("checkbox", { name: "I am not a robot (test mode)" }).check();
  return chosen.map((o) => o.name);
}

interface MailSummary {
  ID: string;
  Subject: string;
  To: { Address: string }[];
}

/** Waits for messages that match the Mailpit search query. */
export async function awaitMessages(
  request: APIRequestContext,
  query: string,
  count: number,
): Promise<MailSummary[]> {
  let found: MailSummary[] = [];
  for (let attempt = 0; attempt < 40; attempt++) {
    const response = await request.get(
      `${mailpitUrl}/api/v1/search?limit=50&query=${encodeURIComponent(query)}`,
    );
    expect(response.status()).toBe(200);
    found = ((await response.json()) as { messages: MailSummary[] }).messages;
    if (found.length >= count) {
      return found;
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`expected ${count} message(s) for ${query}, found ${found.length}`);
}

export async function message(
  request: APIRequestContext,
  id: string,
): Promise<{ Text: string; Attachments: { FileName: string; PartID: string }[] }> {
  const response = await request.get(`${mailpitUrl}/api/v1/message/${id}`);
  expect(response.status()).toBe(200);
  return response.json();
}

/** Text of every XML part of an .xlsx workbook (shared strings and sheets). */
export function workbookText(xlsx: Buffer): string {
  const eocd = xlsx.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]));
  expect(eocd, "not a zip file").toBeGreaterThanOrEqual(0);
  const entries = xlsx.readUInt16LE(eocd + 10);
  let offset = xlsx.readUInt32LE(eocd + 16);
  let text = "";
  for (let i = 0; i < entries; i++) {
    const method = xlsx.readUInt16LE(offset + 10);
    const compressedSize = xlsx.readUInt32LE(offset + 20);
    const nameLength = xlsx.readUInt16LE(offset + 28);
    const extraLength = xlsx.readUInt16LE(offset + 30);
    const commentLength = xlsx.readUInt16LE(offset + 32);
    const localHeader = xlsx.readUInt32LE(offset + 42);
    const name = xlsx.toString("utf8", offset + 46, offset + 46 + nameLength);
    if (name.endsWith(".xml") && name.startsWith("xl/")) {
      const start =
        localHeader +
        30 +
        xlsx.readUInt16LE(localHeader + 26) +
        xlsx.readUInt16LE(localHeader + 28);
      const data = xlsx.subarray(start, start + compressedSize);
      text += (method === 8 ? inflateRawSync(data) : data).toString("utf8");
    }
    offset += 46 + nameLength + extraLength + commentLength;
  }
  return text;
}
