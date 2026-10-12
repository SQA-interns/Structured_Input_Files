import { execFileSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { inflateRawSync } from "node:zlib";
import { expect, type Page } from "@playwright/test";

// Access to the running local stack (docker compose in 02_output/): database, JSON copies,
// Mailpit and the export, all through their public interfaces.

const COMPOSE_FILE = fileURLToPath(
  new URL("../../../docker-compose.yml", import.meta.url),
);
const ROOT_ENV = fileURLToPath(new URL("../../../../.env", import.meta.url));
export const MAILPIT = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";

function compose(args: string[]): string {
  return execFileSync("docker", ["compose", "-f", COMPOSE_FILE, ...args], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

export const sqlText = (value: string) => `'${value.replaceAll("'", "''")}'`;

/** Runs SQL in the stack's PostgreSQL; rows as arrays of column strings ("|" separated). */
export function sql(statement: string): string[][] {
  const out = compose([
    "exec",
    "-T",
    "-e",
    "PGCLIENTENCODING=UTF8",
    "postgres",
    "psql",
    "-U",
    "registration",
    "-d",
    "registration",
    "-At",
    "-F",
    "|",
    "-c",
    statement,
  ]);
  return out
    .split("\n")
    .map((line) => line.replace(/\r$/, ""))
    .filter((line) => line.length > 0)
    .map((line) => line.split("|"));
}

/** The JSON copy of a registration as raw UTF-8 text, read inside the backend container. */
export function jsonCopy(id: string): string {
  return compose([
    "exec",
    "-T",
    "backend",
    "cat",
    `/data/json-copies/${id}.json`,
  ]);
}

export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
}

interface MailSummary {
  ID: string;
  To: { Address: string }[];
}

export interface MailMessage {
  ID: string;
  To: { Address: string }[];
  Subject: string;
  Text: string;
  HTML: string;
  Attachments: { PartID: string; FileName: string; ContentType: string }[];
}

async function search(query: string): Promise<MailSummary[]> {
  const response = await fetch(
    `${MAILPIT}/api/v1/search?limit=100&query=${encodeURIComponent(query)}`,
  );
  expect(response.status).toBe(200);
  const body = (await response.json()) as { messages: MailSummary[] | null };
  return body.messages ?? [];
}

async function fullMessage(id: string): Promise<MailMessage> {
  return (await (
    await fetch(`${MAILPIT}/api/v1/message/${id}`)
  ).json()) as MailMessage;
}

export interface RegistrationMails {
  participant: MailMessage;
  organizer: MailMessage[];
}

/**
 * Waits for the participant confirmation (addressed to `email`) and at least one organizer
 * notification (another recipient, body mentions `email`).
 */
export async function registrationMails(
  email: string,
): Promise<RegistrationMails> {
  let toParticipant: MailSummary[] = [];
  let toOthers: MailSummary[] = [];
  await expect
    .poll(
      async () => {
        toParticipant = await search(`to:"${email}"`);
        toOthers = (await search(`"${email}"`)).filter(
          (m) => !m.To.some((r) => r.Address === email),
        );
        return toParticipant.length > 0 && toOthers.length > 0;
      },
      { timeout: 15_000 },
    )
    .toBe(true);
  expect(toParticipant).toHaveLength(1);
  return {
    participant: await fullMessage(toParticipant[0].ID),
    organizer: await Promise.all(toOthers.map((m) => fullMessage(m.ID))),
  };
}

export async function mailPart(
  messageId: string,
  partId: string,
): Promise<Buffer> {
  const response = await fetch(
    `${MAILPIT}/api/v1/message/${messageId}/part/${partId}`,
  );
  return Buffer.from(await response.arrayBuffer());
}

function envValue(name: string): string {
  const fromProcess = process.env[name];
  if (fromProcess) {
    return fromProcess;
  }
  if (existsSync(ROOT_ENV)) {
    for (const line of readFileSync(ROOT_ENV, "utf8").split(/\r?\n/)) {
      if (line.startsWith(`${name}=`)) {
        return line
          .slice(name.length + 1)
          .trim()
          .replace(/^["']|["']$/g, "");
      }
    }
  }
  throw new Error(`${name} is not set (environment or repository .env)`);
}

export function organizerAuthorization(): string {
  const token = Buffer.from(
    `${envValue("ORGANIZER_USERNAME")}:${envValue("ORGANIZER_PASSWORD")}`,
    "utf8",
  ).toString("base64");
  return `Basic ${token}`;
}

/** Minimal ZIP reader: every entry of an .xlsx file by name. */
export function unzip(data: Buffer): Map<string, Buffer> {
  let eocd = data.length - 22;
  while (eocd >= 0 && data.readUInt32LE(eocd) !== 0x06054b50) {
    eocd--;
  }
  if (eocd < 0) {
    throw new Error("not a ZIP file");
  }
  const entries = data.readUInt16LE(eocd + 10);
  let offset = data.readUInt32LE(eocd + 16);
  const files = new Map<string, Buffer>();
  for (let i = 0; i < entries; i++) {
    const method = data.readUInt16LE(offset + 10);
    const compressedSize = data.readUInt32LE(offset + 20);
    const nameLength = data.readUInt16LE(offset + 28);
    const extraLength = data.readUInt16LE(offset + 30);
    const commentLength = data.readUInt16LE(offset + 32);
    const localHeader = data.readUInt32LE(offset + 42);
    const name = data.toString("utf8", offset + 46, offset + 46 + nameLength);
    const localNameLength = data.readUInt16LE(localHeader + 26);
    const localExtraLength = data.readUInt16LE(localHeader + 28);
    const start = localHeader + 30 + localNameLength + localExtraLength;
    const raw = data.subarray(start, start + compressedSize);
    files.set(name, method === 8 ? inflateRawSync(raw) : Buffer.from(raw));
    offset += 46 + nameLength + extraLength + commentLength;
  }
  return files;
}

const decodeXml = (s: string) =>
  s
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&quot;", '"')
    .replaceAll("&apos;", "'")
    .replaceAll("&amp;", "&");

export interface Workbook {
  rowCount: number;
  texts: string[];
}

/** Rows of the first sheet and every text cell value (shared and inline strings). */
export function readWorkbook(data: Buffer): Workbook {
  const files = unzip(data);
  const sheet = files.get("xl/worksheets/sheet1.xml")?.toString("utf8");
  if (!sheet || !files.has("xl/workbook.xml")) {
    throw new Error("not an Excel workbook");
  }
  const shared = files.get("xl/sharedStrings.xml")?.toString("utf8") ?? "";
  const textsOf = (xml: string) =>
    [
      ...xml.matchAll(
        /<(?:\w+:)?si>([\s\S]*?)<\/(?:\w+:)?si>|<is>([\s\S]*?)<\/is>/g,
      ),
    ].map((m) =>
      decodeXml(
        [...(m[1] ?? m[2]).matchAll(/<t[^>]*>([\s\S]*?)<\/t>/g)]
          .map((t) => t[1])
          .join(""),
      ),
    );
  return {
    rowCount: (sheet.match(/<row[\s>]/g) ?? []).length,
    texts: [...textsOf(shared), ...textsOf(sheet)],
  };
}

export interface FormConfig {
  consents: { id: string; text: string; mandatory: boolean }[];
  options: {
    id: string;
    name: string;
    category: string;
    availableTo: string[];
  }[];
}

export async function formConfig(page: Page): Promise<FormConfig> {
  const response = await page.request.get("/api/form-config");
  expect(response.status()).toBe(200);
  return (await response.json()) as FormConfig;
}

export const CATEGORY_GROUPS = [
  "Workshops",
  "Events",
  "Meals",
  "Other activities",
] as const;

/** Ticks the first option of every category group; returns the ticked option names. */
export async function tickOnePerCategory(page: Page): Promise<string[]> {
  const names: string[] = [];
  for (const group of CATEGORY_GROUPS) {
    const box = page
      .getByRole("group", { name: group })
      .getByRole("checkbox")
      .first();
    await box.check();
    const label = await box.evaluate(
      (el) =>
        (el as HTMLInputElement).labels?.[0]?.textContent ??
        el.getAttribute("aria-label") ??
        "",
    );
    names.push(label.trim());
  }
  return names;
}

/** Ticks every mandatory consent and the test-mode reCAPTCHA stand-in. */
export async function giveConsentsAndPassCaptcha(
  page: Page,
  config: FormConfig,
): Promise<void> {
  for (const consent of config.consents.filter((c) => c.mandatory)) {
    await page.getByRole("checkbox", { name: consent.text }).check();
  }
  await page
    .getByRole("checkbox", { name: "I am not a robot (test mode)" })
    .check();
}
