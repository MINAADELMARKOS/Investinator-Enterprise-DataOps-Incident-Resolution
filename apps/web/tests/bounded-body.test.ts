import { test } from "node:test";
import assert from "node:assert/strict";
import { boundedText, BodyLimitError, BodyEncodingError } from "../lib/bounded-body.ts";
function request(body: ReadableStream<Uint8Array>) { return new Request("http://localhost", { method: "POST", body, duplex: "half" } as RequestInit); }
test("chunked request exceeding byte limit cancels upstream without trusting headers", async () => {
  let cancelled = false;
  const stream = new ReadableStream<Uint8Array>({ start(c) { c.enqueue(new Uint8Array(4)); c.enqueue(new Uint8Array(4)); }, cancel() { cancelled = true; } });
  await assert.rejects(boundedText(request(stream), 7), BodyLimitError); assert.equal(cancelled, true);
});
test("split UTF-8 respects byte bounds and malformed bytes fail", async () => {
  const bytes = new TextEncoder().encode("€");
  const stream = new ReadableStream<Uint8Array>({ start(c) { c.enqueue(bytes.slice(0,1)); c.enqueue(bytes.slice(1)); c.close(); } });
  assert.equal(await boundedText(request(stream), 3), "€");
  await assert.rejects(boundedText(new Request("http://localhost", { method:"POST",body:bytes }), 2), BodyLimitError);
  await assert.rejects(boundedText(new Request("http://localhost", {method:"POST",body:new Uint8Array([255])}), 3), BodyEncodingError);
});
