import { inflateRawSync } from "node:zlib";

/**
 * Minimal reader for the export workbook (excel-export.md): unzips the package and returns the
 * cell texts of the first worksheet, row by row. Throws when the bytes are not a valid workbook.
 */
export function readWorkbookRows(xlsx: Buffer): string[][] {
  const files = unzip(xlsx);
  const workbook = files.get("xl/workbook.xml");
  const sheet = files.get("xl/worksheets/sheet1.xml");
  if (!workbook || !sheet) {
    throw new Error("not an xlsx workbook");
  }
  const shared = sharedStrings(files.get("xl/sharedStrings.xml"));
  const rows: string[][] = [];
  for (const rowMatch of sheet.matchAll(/<row\b[^>]*>([\s\S]*?)<\/row>/g)) {
    const cells: string[] = [];
    for (const cellMatch of rowMatch[1].matchAll(/<c\b([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const attributes = cellMatch[1];
      const content = cellMatch[2] ?? "";
      const column = columnIndex(/\br="([A-Z]+)\d+"/.exec(attributes)?.[1] ?? "");
      while (cells.length < column) cells.push("");
      const kind = /\bt="([^"]+)"/.exec(attributes)?.[1];
      let text = "";
      if (kind === "s") {
        text = shared[Number(/<v>([\s\S]*?)<\/v>/.exec(content)?.[1] ?? "-1")] ?? "";
      } else if (kind === "inlineStr") {
        text = allText(content);
      } else {
        text = decode(/<v>([\s\S]*?)<\/v>/.exec(content)?.[1] ?? "");
      }
      cells[column] = text;
    }
    rows.push(cells);
  }
  return rows;
}

function columnIndex(letters: string): number {
  let index = 0;
  for (const letter of letters) index = index * 26 + (letter.charCodeAt(0) - 64);
  return Math.max(index - 1, 0);
}

function sharedStrings(xml: string | undefined): string[] {
  if (!xml) return [];
  return [...xml.matchAll(/<si>([\s\S]*?)<\/si>/g)].map((m) => allText(m[1]));
}

function allText(xml: string): string {
  return [...xml.matchAll(/<t\b[^>]*>([\s\S]*?)<\/t>/g)].map((m) => decode(m[1])).join("");
}

function decode(text: string): string {
  return text
    .replace(/&#x([0-9a-fA-F]+);/g, (_, hex: string) => String.fromCodePoint(parseInt(hex, 16)))
    .replace(/&#(\d+);/g, (_, dec: string) => String.fromCodePoint(Number(dec)))
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&");
}

/** Reads every entry of a zip archive through its central directory. */
function unzip(zip: Buffer): Map<string, string> {
  if (zip.length < 22 || zip.readUInt32LE(0) !== 0x04034b50) {
    throw new Error("not a zip archive");
  }
  let end = zip.length - 22;
  while (end >= 0 && zip.readUInt32LE(end) !== 0x06054b50) end--;
  if (end < 0) throw new Error("zip end of central directory not found");
  const count = zip.readUInt16LE(end + 10);
  let offset = zip.readUInt32LE(end + 16);
  const files = new Map<string, string>();
  for (let i = 0; i < count; i++) {
    const method = zip.readUInt16LE(offset + 10);
    const compressedSize = zip.readUInt32LE(offset + 20);
    const nameLength = zip.readUInt16LE(offset + 28);
    const extraLength = zip.readUInt16LE(offset + 30);
    const commentLength = zip.readUInt16LE(offset + 32);
    const localHeader = zip.readUInt32LE(offset + 42);
    const name = zip.toString("utf8", offset + 46, offset + 46 + nameLength);
    const localNameLength = zip.readUInt16LE(localHeader + 26);
    const localExtraLength = zip.readUInt16LE(localHeader + 28);
    const dataStart = localHeader + 30 + localNameLength + localExtraLength;
    const data = zip.subarray(dataStart, dataStart + compressedSize);
    const content = method === 0 ? data : inflateRawSync(data);
    files.set(name, content.toString("utf8"));
    offset += 46 + nameLength + extraLength + commentLength;
  }
  return files;
}
