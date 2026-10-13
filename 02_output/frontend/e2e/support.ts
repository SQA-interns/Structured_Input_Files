/**
 * Helpers of the end-to-end tests: the local compose stack is inspected through
 * `docker compose exec` (PostgreSQL, JSON copies) and the Mailpit API (spec §9).
 */
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { inflateRawSync } from "node:zlib";
import { expect, type Page } from "@playwright/test";

const OUTPUT_DIR = fileURLToPath(new URL("../..", import.meta.url));
const ENV_FILE = fileURLToPath(new URL("../../../.env", import.meta.url));
const MAILPIT = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";

export const CONFERENCE_NAME = "Konferenca 2026";
export const PRIVACY_TEXT =
  "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers.";
export const TEST_CAPTCHA_LABEL = "I am not a robot (test mode)";

/** A suffix that makes names and emails of one test run unique. */
export function unique(): string {
  return (
    Date.now().toString(36) + Math.floor(Math.random() * 1296).toString(36)
  );
}

function compose(args: string[]): string {
  return execFileSync("docker", ["compose", "--env-file", ENV_FILE, ...args], {
    cwd: OUTPUT_DIR,
    encoding: "utf8",
  });
}

/** Rows of a query as arrays of column values (psql unaligned, tab separated). */
export function sql(query: string): string[][] {
  const out = compose([
    "exec",
    "-T",
    "postgres",
    "psql",
    "-U",
    "registration",
    "-d",
    "registration",
    "-v",
    "ON_ERROR_STOP=1",
    "-tA",
    "-F",
    "\t",
    "-c",
    query,
  ]);
  return out
    .split("\n")
    .filter((line) => line.length > 0)
    .map((line) => line.split("\t"));
}

/** Escapes a value for a single-quoted SQL literal. */
export function literal(value: string): string {
  return "'" + value.replace(/'/g, "''") + "'";
}

/** The parsed JSON copy of a registration from the backend's copy directory. */
export function jsonCopy(id: string): Record<string, unknown> {
  if (!/^[0-9a-f-]{36}$/.test(id))
    throw new Error("not a registration id: " + id);
  return JSON.parse(
    compose(["exec", "-T", "backend", "cat", `/data/json-copies/${id}.json`]),
  );
}

export interface MailSummary {
  ID: string;
  Subject: string;
  To: { Address: string }[];
}

export interface MailMessage extends MailSummary {
  Text: string;
  HTML: string;
  Attachments: { PartID: string; FileName: string; ContentType: string }[];
}

async function mailpitJson<T>(path: string): Promise<T> {
  const response = await fetch(MAILPIT + path);
  if (!response.ok) throw new Error(`Mailpit ${path}: ${response.status}`);
  return (await response.json()) as T;
}

/** Waits until Mailpit's search for the term returns at least `count` messages. */
export async function awaitMessages(
  term: string,
  count: number,
): Promise<MailMessage[]> {
  let found: MailSummary[] = [];
  await expect
    .poll(
      async () => {
        const result = await mailpitJson<{ messages: MailSummary[] }>(
          "/api/v1/search?limit=200&query=" + encodeURIComponent(term),
        );
        found = result.messages;
        return found.length;
      },
      { timeout: 30_000 },
    )
    .toBeGreaterThanOrEqual(count);
  return Promise.all(
    found.map((m) => mailpitJson<MailMessage>("/api/v1/message/" + m.ID)),
  );
}

export async function attachment(
  message: MailMessage,
  partId: string,
): Promise<string> {
  const response = await fetch(
    `${MAILPIT}/api/v1/message/${message.ID}/part/${partId}`,
  );
  return await response.text();
}

/** Basic authorization header from ORGANIZER_USERNAME / ORGANIZER_PASSWORD of the environment. */
export function organizerAuthorization(): string {
  const user = process.env.ORGANIZER_USERNAME;
  const password = process.env.ORGANIZER_PASSWORD;
  if (!user || !password) {
    throw new Error(
      "ORGANIZER_USERNAME and ORGANIZER_PASSWORD must be exported from .env",
    );
  }
  return (
    "Basic " + Buffer.from(`${user}:${password}`, "utf8").toString("base64")
  );
}

/** Entries of a ZIP archive (an .xlsx workbook) by name; throws if it is not a ZIP. */
export function unzip(data: Buffer): Map<string, Buffer> {
  let eocd = -1;
  for (let i = data.length - 22; i >= 0; i--) {
    if (data.readUInt32LE(i) === 0x06054b50) {
      eocd = i;
      break;
    }
  }
  if (eocd < 0) throw new Error("not a ZIP archive");
  const entries = new Map<string, Buffer>();
  const count = data.readUInt16LE(eocd + 10);
  let p = data.readUInt32LE(eocd + 16);
  for (let n = 0; n < count; n++) {
    if (data.readUInt32LE(p) !== 0x02014b50)
      throw new Error("bad central directory");
    const method = data.readUInt16LE(p + 10);
    const size = data.readUInt32LE(p + 20);
    const nameLength = data.readUInt16LE(p + 28);
    const extraLength = data.readUInt16LE(p + 30);
    const commentLength = data.readUInt16LE(p + 32);
    const local = data.readUInt32LE(p + 42);
    const name = data.toString("utf8", p + 46, p + 46 + nameLength);
    const start =
      local +
      30 +
      data.readUInt16LE(local + 26) +
      data.readUInt16LE(local + 28);
    const raw = data.subarray(start, start + size);
    entries.set(name, method === 8 ? inflateRawSync(raw) : Buffer.from(raw));
    p += 46 + nameLength + extraLength + commentLength;
  }
  return entries;
}

function decodeXml(text: string): string {
  return text
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&");
}

/** Every text value of the workbook (shared strings and inline strings of all sheets). */
export function workbookStrings(entries: Map<string, Buffer>): string[] {
  const strings: string[] = [];
  for (const [name, content] of entries) {
    if (name === "xl/sharedStrings.xml" || name.startsWith("xl/worksheets/")) {
      for (const match of content
        .toString("utf8")
        .matchAll(/<t(?:\s[^>]*)?>([^<]*)<\/t>/g)) {
        strings.push(decodeXml(match[1]));
      }
    }
  }
  return strings;
}

/** Number of rows in the first worksheet. */
export function firstSheetRowCount(entries: Map<string, Buffer>): number {
  const sheet = entries.get("xl/worksheets/sheet1.xml");
  if (!sheet) throw new Error("workbook has no first sheet");
  return (sheet.toString("utf8").match(/<row[\s>]/g) ?? []).length;
}

/** Fills the fields shared by both forms and the given type-specific fields. */
export async function fillFields(page: Page, fields: Record<string, string>) {
  for (const [label, value] of Object.entries(fields)) {
    await page.getByLabel(label, { exact: true }).fill(value);
  }
}

/** Gives the mandatory consent and the test-mode anti-automation token. */
export async function confirmConsentAndCaptcha(page: Page) {
  await page.getByRole("checkbox", { name: PRIVACY_TEXT }).check();
  await page.getByRole("checkbox", { name: TEST_CAPTCHA_LABEL }).check();
}
