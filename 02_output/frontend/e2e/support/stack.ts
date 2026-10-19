import { execFileSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

/** 02_output, where docker-compose.yml lives. */
const OUTPUT_DIR = resolve(dirname(fileURLToPath(import.meta.url)), "..", "..", "..");
const ENV_FILE = resolve(OUTPUT_DIR, "..", ".env");
export const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";

export interface ConferenceOption {
  id: string;
  name: string;
  category: "WORKSHOP" | "EVENT" | "MEAL" | "OTHER";
  active: boolean;
  availableTo?: ("EXTERNAL" | "STUDENT")[];
}

/** The shipped configuration the stack runs with (config/conference.json). */
export function conferenceConfig(): {
  conferenceName: string;
  consents: { id: string; text: string; mandatory: boolean }[];
  options: ConferenceOption[];
} {
  return JSON.parse(readFileSync(resolve(OUTPUT_DIR, "config", "conference.json"), "utf8"));
}

/** Active options available to the type (default both, OQ-01). */
export function optionsFor(type: "EXTERNAL" | "STUDENT"): ConferenceOption[] {
  return conferenceConfig().options.filter(
    (o) => o.active && (o.availableTo ?? ["EXTERNAL", "STUDENT"]).includes(type),
  );
}

/** The first option of every category available to the type. */
export function onePerCategory(type: "EXTERNAL" | "STUDENT"): ConferenceOption[] {
  const result: ConferenceOption[] = [];
  for (const category of ["WORKSHOP", "EVENT", "MEAL", "OTHER"] as const) {
    const option = optionsFor(type).find((o) => o.category === category);
    if (option) result.push(option);
  }
  return result;
}

export function uniqueEmail(name: string): string {
  return `${name}.${randomUUID().slice(0, 8)}@example.com`;
}

export function organizer() {
  const username = process.env.ORGANIZER_USERNAME ?? "";
  const password = process.env.ORGANIZER_PASSWORD ?? "";
  const emails = (process.env.ORGANIZER_EMAILS ?? "")
    .split(",")
    .map((e) => e.trim())
    .filter((e) => e.length > 0);
  if (!username || !password || emails.length === 0) {
    throw new Error("export ORGANIZER_USERNAME, ORGANIZER_PASSWORD and ORGANIZER_EMAILS from .env");
  }
  return { username, password, emails };
}

export function basicAuth(username: string, password: string): string {
  return "Basic " + Buffer.from(`${username}:${password}`, "utf8").toString("base64");
}

function compose(args: string[]): string {
  return execFileSync("docker", ["compose", "--env-file", ENV_FILE, ...args], {
    cwd: OUTPUT_DIR,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

/** Runs one query in the stack's PostgreSQL and returns the rows as parsed JSON objects. */
export function sqlJson<T>(query: string): T[] {
  const output = compose([
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
    "-At",
    "-c",
    `SELECT row_to_json(q) FROM (${query}) q`,
  ]);
  return output
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line) => JSON.parse(line) as T);
}

export interface RegistrationRow {
  id: string;
  type: string;
  first_name: string;
  last_name: string;
  email: string;
  organization: string | null;
  study_institution: string | null;
  study_programme: string | null;
  student_id: string | null;
  registered_at: string;
}

export function registrationsByEmail(email: string): RegistrationRow[] {
  return sqlJson<RegistrationRow>(
    `SELECT * FROM registration WHERE email = '${email.replace(/'/g, "''")}'`,
  );
}

export function optionIdsOf(id: string): string[] {
  return sqlJson<{ option_id: string }>(
    `SELECT option_id FROM registration_option WHERE registration_id = '${id}'`,
  ).map((r) => r.option_id);
}

export function registrationCount(): number {
  const exists = sqlJson<{ e: boolean }>(
    "SELECT to_regclass('public.registration') IS NOT NULL AS e",
  )[0].e;
  return exists ? sqlJson<{ n: number }>("SELECT COUNT(*) AS n FROM registration")[0].n : 0;
}

/** Raw bytes of the JSON copy inside the backend container. */
export function jsonCopy(id: string): Buffer {
  return execFileSync(
    "docker",
    [
      "compose",
      "--env-file",
      ENV_FILE,
      "exec",
      "-T",
      "backend",
      "cat",
      `/data/registrations/${id}.json`,
    ],
    { cwd: OUTPUT_DIR, stdio: ["ignore", "pipe", "pipe"] },
  );
}

export interface MailpitMessage {
  ID: string;
  To: { Address: string }[];
  Subject: string;
  Text: string;
  HTML: string;
  Attachments: { PartID: string; FileName: string; ContentType: string }[];
}

export async function clearMailpit(): Promise<void> {
  await fetch(`${MAILPIT_URL}/api/v1/messages`, { method: "DELETE" });
}

async function allMessages(): Promise<MailpitMessage[]> {
  const list = (await (await fetch(`${MAILPIT_URL}/api/v1/messages?limit=1000`)).json()) as {
    messages: { ID: string }[];
  };
  const result: MailpitMessage[] = [];
  for (const summary of list.messages) {
    result.push(
      (await (await fetch(`${MAILPIT_URL}/api/v1/message/${summary.ID}`)).json()) as MailpitMessage,
    );
  }
  return result;
}

/** Waits up to 20 seconds for a message to the address whose text contains the marker. */
export async function awaitMessage(address: string, marker: string): Promise<MailpitMessage> {
  const deadline = Date.now() + 20_000;
  while (Date.now() < deadline) {
    const found = (await allMessages()).find(
      (m) =>
        m.To.some((t) => t.Address.toLowerCase() === address.toLowerCase()) &&
        m.Text.includes(marker),
    );
    if (found) return found;
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`no email to ${address} containing ${marker}`);
}

export async function attachment(message: MailpitMessage, fileName: string): Promise<Buffer> {
  const part = message.Attachments.find((a) => a.FileName === fileName);
  if (!part) throw new Error(`no attachment ${fileName}`);
  const response = await fetch(`${MAILPIT_URL}/api/v1/message/${message.ID}/part/${part.PartID}`);
  return Buffer.from(await response.arrayBuffer());
}
