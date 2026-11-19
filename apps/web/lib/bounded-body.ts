export class BodyLimitError extends Error {}
export class BodyEncodingError extends Error {}
/** Count actual bytes before concatenation; Content-Length is not an authority. */
export async function boundedText(request: Request, limit: number): Promise<string> {
  if (!request.body) return "";
  const reader = request.body.getReader(); const chunks: Uint8Array[] = []; let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read(); if (done) break;
      size += value.byteLength;
      if (size > limit) { await reader.cancel(); throw new BodyLimitError("Request too large"); }
      chunks.push(value);
    }
  } finally { reader.releaseLock(); }
  const bytes = new Uint8Array(size); let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
  try { return new TextDecoder("utf-8", { fatal: true }).decode(bytes); } catch { throw new BodyEncodingError("Invalid UTF-8 body"); }
}
