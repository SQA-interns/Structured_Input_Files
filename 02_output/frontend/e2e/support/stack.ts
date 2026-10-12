import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { inflateRawSync } from "node:zlib";
import { expect, type Page } from "@playwright/test";

/** Helpers that observe the running local stack (docker compose in 02_output/). */

const here = path.dirname(fileURLToPath(import.meta.url));
export const OUTPUT_DIR = path.resolve(here, "..", "..", "..");
export const REPO_ROOT = path.resolve(OUTPUT_DIR, "..");
const ENV_FILE = path.join(REPO_ROOT, ".env");
export const MAILPIT_URL = process.env.MAILPIT_URL ?? "http://127.0.0.1:8025";

/** A setting from the environment, else from the repository .env file (never printed). */
export function setting(name: string): string {
  const fromEnv = process.env[name];
  if (fromEnv !== undefined && fromEnv !== "") {
    return fromEnv;
  }
  if (existsSync(ENV_FILE)) {
    for (const line of readFileSync(ENV_FILE, "utf8").split(/\r?\n/)) {
      const match = /^([A-Z_][A-Z0-9_]*)=(.*)$/.exec(line);
      if (match && match[1] === name) {
        return match[2].trim().replace(/^["']|["']$/g, "");
      }
    }
  }
  throw new Error(`Setting ${name} is not available`);
}

export function organizerEmails(): string[] {
  return setting("ORGANIZER_EMAILS")
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}

export function organizerAuthorization(): string {
  const raw = `${setting("ORGANIZER_USERNAME")}:${setting("ORGANIZER_PASSWORD")}`;
  return `Basic ${Buffer.from(raw, "utf8").toString("base64")}`;
}

function compose(args: string[]): { status: number | null; stdout: Buffer } {
  const result = spawnSync(
    "docker",
    [
      "compose",
      "--project-directory",
      OUTPUT_DIR,
      "-f",
      path.join(OUTPUT_DIR, "docker-compose.yml"),
      "--env-file",
      ENV_FILE,
      ...args,
    ],
    { maxBuffer: 32 * 1024 * 1024 },
  );
  return { status: result.status, stdout: result.stdout ?? Buffer.alloc(0) };
}

/** Runs a query in the PostgreSQL container; rows of tab-separated columns. */
export function sql(query: string): string[][] {
  const result = compose([
    "exec",
    "-T",
    "postgres",
    "psql",
    "-U",
    "registration",
    "-d",
    "registration",
    "-At",
    "-F",
    "\t",
    "-c",
    query,
  ]);
  if (result.status !== 0) {
    throw new Error(`psql failed with status ${result.status}`);
  }
  return result.stdout
    .toString("utf8")
    .split("\n")
    .filter((line) => line.length > 0)
    .map((line) => line.split("\t"));
}

/** The bytes of the JSON copy of a registration, or null when it does not exist. */
export function jsonCopy(id: string): Buffer | null {
  if (!/^[0-9a-f-]{36}$/.test(id)) {
    throw new Error("not a registration id");
  }
  const result = compose(["exec", "-T", "backend", "cat", `/data/json-copies/${id}.json`]);
  return result.status === 0 ? result.stdout : null;
}

// ---- Mailpit -------------------------------------------------------------------------------

export type MailMessage = {
  ID: string;
  Subject: string;
  To: { Address: string }[];
  Cc: { Address: string }[] | null;
  Bcc: { Address: string }[] | null;
  Text: string;
  HTML: string;
  Attachments: { PartID: string; FileName: string; ContentType: string }[];
};

async function mailpitJson<T>(pathname: string): Promise<T> {
  const response = await fetch(`${MAILPIT_URL}${pathname}`);
  expect(response.status, `Mailpit ${pathname}`).toBe(200);
  return (await response.json()) as T;
}

async function allMessages(): Promise<MailMessage[]> {
  const list = await mailpitJson<{ messages: { ID: string }[] }>("/api/v1/messages?limit=1000");
  return Promise.all(list.messages.map((m) => mailpitJson<MailMessage>(`/api/v1/message/${m.ID}`)));
}

/** Waits up to 15 s for messages matching the predicate and returns them. */
export async function awaitMessages(
  predicate: (m: MailMessage) => boolean,
  atLeast = 1,
): Promise<MailMessage[]> {
  const deadline = Date.now() + 15_000;
  let found: MailMessage[] = [];
  while (Date.now() < deadline) {
    found = (await allMessages()).filter(predicate);
    if (found.length >= atLeast) {
      return found;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  return found;
}

export function addressedTo(message: MailMessage, address: string): boolean {
  const all = [...message.To, ...(message.Cc ?? []), ...(message.Bcc ?? [])];
  return all.some((a) => a.Address.toLowerCase() === address.toLowerCase());
}

export async function attachmentBytes(message: MailMessage, partId: string): Promise<Buffer> {
  const response = await fetch(`${MAILPIT_URL}/api/v1/message/${message.ID}/part/${partId}`);
  expect(response.status).toBe(200);
  return Buffer.from(await response.arrayBuffer());
}

// ---- Registration form ---------------------------------------------------------------------

export const PRIVACY_CONSENT = /I agree that my personal data is processed/;
export const OPTION_GROUPS = ["Workshops", "Events", "Meals", "Other activities"];

export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.org`;
}

/** Clicks Register and returns the HTTP status and body of the registration request. */
export async function submitAndCapture(page: Page): Promise<{ status: number; body: unknown }> {
  const responsePromise = page.waitForResponse(
    (r) => r.url().endsWith("/api/registrations") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Register" }).click();
  const response = await responsePromise;
  return { status: response.status(), body: await response.json() };
}

// ---- xlsx reading (zip + SpreadsheetML), enough to read cell text ----------------------------

function unzip(buffer: Buffer): Map<string, Buffer> {
  const files = new Map<string, Buffer>();
  let eocd = buffer.length - 22;
  while (eocd >= 0 && buffer.readUInt32LE(eocd) !== 0x06054b50) {
    eocd--;
  }
  if (eocd < 0) {
    throw new Error("not a zip file");
  }
  const entries = buffer.readUInt16LE(eocd + 10);
  let offset = buffer.readUInt32LE(eocd + 16);
  for (let i = 0; i < entries; i++) {
    if (buffer.readUInt32LE(offset) !== 0x02014b50) {
      throw new Error("bad central directory");
    }
    const method = buffer.readUInt16LE(offset + 10);
    const compressedSize = buffer.readUInt32LE(offset + 20);
    const nameLength = buffer.readUInt16LE(offset + 28);
    const extraLength = buffer.readUInt16LE(offset + 30);
    const commentLength = buffer.readUInt16LE(offset + 32);
    const localOffset = buffer.readUInt32LE(offset + 42);
    const name = buffer.toString("utf8", offset + 46, offset + 46 + nameLength);
    const localNameLength = buffer.readUInt16LE(localOffset + 26);
    const localExtraLength = buffer.readUInt16LE(localOffset + 28);
    const start = localOffset + 30 + localNameLength + localExtraLength;
    const data = buffer.subarray(start, start + compressedSize);
    files.set(name, method === 8 ? inflateRawSync(data) : Buffer.from(data));
    offset += 46 + nameLength + extraLength + commentLength;
  }
  return files;
}

function decodeXml(text: string): string {
  return text
    .replace(/&#x([0-9a-fA-F]+);/g, (_, h: string) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&#(\d+);/g, (_, d: string) => String.fromCodePoint(parseInt(d, 10)))
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&");
}

function columnIndex(ref: string): number {
  const letters = /^[A-Z]+/.exec(ref)?.[0] ?? "A";
  return [...letters].reduce((acc, ch) => acc * 26 + (ch.charCodeAt(0) - 64), 0) - 1;
}

/** The cell texts of the first worksheet, row by row. */
export function readWorkbookRows(buffer: Buffer): string[][] {
  const files = unzip(buffer);
  const shared: string[] = [];
  const sharedXml = files.get("xl/sharedStrings.xml")?.toString("utf8") ?? "";
  for (const si of sharedXml.match(/<si>[\s\S]*?<\/si>/g) ?? []) {
    const parts = [...si.matchAll(/<t(?:\s[^>]*)?>([\s\S]*?)<\/t>/g)].map((m) => decodeXml(m[1]));
    shared.push(parts.join(""));
  }
  const sheetXml = files.get("xl/worksheets/sheet1.xml")?.toString("utf8");
  if (!sheetXml) {
    throw new Error("workbook has no first sheet");
  }
  const rows: string[][] = [];
  for (const row of sheetXml.match(/<row[\s\S]*?<\/row>/g) ?? []) {
    const cells: string[] = [];
    for (const cell of row.matchAll(/<c\s([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const attributes = cell[1];
      const content = cell[2] ?? "";
      const ref = /r="([A-Z]+\d+)"/.exec(attributes)?.[1] ?? "A1";
      const type = /t="([^"]+)"/.exec(attributes)?.[1];
      const value = /<v>([\s\S]*?)<\/v>/.exec(content)?.[1];
      let text = "";
      if (type === "s" && value !== undefined) {
        text = shared[Number(value)] ?? "";
      } else if (type === "inlineStr") {
        text = [...content.matchAll(/<t(?:\s[^>]*)?>([\s\S]*?)<\/t>/g)]
          .map((m) => decodeXml(m[1]))
          .join("");
      } else if (value !== undefined) {
        text = decodeXml(value);
      }
      cells[columnIndex(ref)] = text;
    }
    rows.push(Array.from(cells, (c) => c ?? ""));
  }
  return rows;
}
