import { expect, type APIRequestContext } from "@playwright/test";
import { inflateRawSync } from "node:zlib";

// Local stack endpoints (docker-compose.yml); organizer credentials come from the environment.
export const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";
export const API_URL = process.env.E2E_API_URL ?? "http://127.0.0.1:8080";

export function organizerAuthHeader(): string {
  const user = process.env.ORGANIZER_USERNAME ?? "";
  const password = process.env.ORGANIZER_PASSWORD ?? "";
  return "Basic " + Buffer.from(`${user}:${password}`, "utf8").toString("base64");
}

interface MailSummary {
  ID: string;
  To: { Address: string }[] | null;
}

export interface MailMessage {
  ID: string;
  Subject: string;
  Text: string;
  To: { Address: string }[] | null;
  Attachments: { PartID: string; FileName: string; ContentType: string }[] | null;
}

/** Waits for a Mailpit message to the address whose text contains the marker. */
export async function awaitMail(
  request: APIRequestContext,
  address: string,
  marker: string,
): Promise<MailMessage> {
  let found: MailMessage | undefined;
  await expect
    .poll(
      async () => {
        const list = await (await request.get(`${MAILPIT_URL}/api/v1/messages?limit=1000`)).json();
        for (const summary of list.messages as MailSummary[]) {
          if (!(summary.To ?? []).some((t) => t.Address.toLowerCase() === address.toLowerCase())) {
            continue;
          }
          const message = (await (
            await request.get(`${MAILPIT_URL}/api/v1/message/${summary.ID}`)
          ).json()) as MailMessage;
          if (message.Text.includes(marker)) {
            found = message;
            return true;
          }
        }
        return false;
      },
      { timeout: 30_000, intervals: [500] },
    )
    .toBe(true);
  return found as MailMessage;
}

/** Text of every entry of a ZIP archive (an .xlsx workbook), read via the central directory. */
export function zipText(zip: Buffer): string {
  let end = zip.length - 22;
  while (end >= 0 && zip.readUInt32LE(end) !== 0x06054b50) end--;
  if (end < 0) throw new Error("not a ZIP archive");
  const entries = zip.readUInt16LE(end + 10);
  let p = zip.readUInt32LE(end + 16);
  let text = "";
  for (let i = 0; i < entries; i++) {
    if (zip.readUInt32LE(p) !== 0x02014b50) throw new Error("bad central directory");
    const method = zip.readUInt16LE(p + 10);
    const compressedSize = zip.readUInt32LE(p + 20);
    const nameLength = zip.readUInt16LE(p + 28);
    const extraLength = zip.readUInt16LE(p + 30);
    const commentLength = zip.readUInt16LE(p + 32);
    const localHeader = zip.readUInt32LE(p + 42);
    const dataStart =
      localHeader + 30 + zip.readUInt16LE(localHeader + 26) + zip.readUInt16LE(localHeader + 28);
    const data = zip.subarray(dataStart, dataStart + compressedSize);
    text += (method === 8 ? inflateRawSync(data) : data).toString("utf8");
    p += 46 + nameLength + extraLength + commentLength;
  }
  return text;
}
