export type RecordValue = Record<string, unknown>;
export const record = (value: unknown): RecordValue => typeof value === "object" && value !== null && !Array.isArray(value) ? value as RecordValue : {};
export const list = (value: unknown): RecordValue[] => Array.isArray(value) ? value.map(record) : [];
const text = (value: unknown, fallback = "UNKNOWN") => typeof value === "string" ? value : fallback;
export type ToolTrace = { name: string; status: string; source_mode: string; partial: boolean | null };
export type ChatTurn = { role: "user" | "assistant"; content: string; evidence_ids: string[]; response_mode: string; provider_status: string; provider_source: string; source_mode: string; model: string | null; tools: ToolTrace[]; analysis: RecordValue; usage: RecordValue; created_at?: string };
export function normalizeTurn(input: unknown): ChatTurn {
  const row = record(input), envelope = record(row.payload), payload = { ...envelope, ...record(envelope.turn_metadata) };
  const mode = text(row.response_mode ?? payload.response_mode ?? payload.mode, "unknown");
  const tools = list(row.tool_trace ?? row.tool_calls ?? payload.tool_trace ?? payload.tool_calls).map(t => ({ name: text(t.tool ?? t.name), status: text(t.status), source_mode: text(t.source_mode, "NOT_TESTED"), partial: typeof t.partial === "boolean" ? t.partial : null }));
  const ids = row.evidence_ids ?? payload.evidence_ids;
  return { role: row.role === "user" ? "user" : "assistant", content: text(row.content ?? row.answer, ""), evidence_ids: Array.isArray(ids) ? ids.filter((s): s is string => typeof s === "string") : [], response_mode: mode, provider_status: text(row.provider_status ?? payload.provider_status, "NOT_TESTED"), provider_source: text(row.provider_source ?? payload.provider_source, "NOT_TESTED"), source_mode: text(row.source_mode ?? payload.source_mode, "NOT_TESTED"), model: typeof (row.model ?? payload.model) === "string" ? String(row.model ?? payload.model) : null, tools, analysis: record(row.analysis ?? payload.analysis), usage: normalizeUsage(row.usage ?? payload.usage), created_at: typeof row.created_at === "string" ? row.created_at : undefined };
}
export function normalizeUsage(input: unknown): RecordValue {
  const usage = record(input);
  if (Object.keys(usage).length === 0) return {};
  const count = (value: unknown) => typeof value === "number" && Number.isFinite(value) && value >= 0 ? value : null;
  return { input_tokens: count(usage.input_tokens), output_tokens: count(usage.output_tokens), latency_ms: count(usage.latency_ms), cost: count(usage.cost) };
}
export function providerLabel(turn: ChatTurn): string {
  if (turn.provider_source === "MOCKED_PROVIDER" || turn.provider_status === "MOCKED_PROVIDER" || turn.response_mode === "MOCKED_PROVIDER") return "MOCKED provider fixture";
  if (isModelTurn(turn)) return turn.provider_status === "OK" ? "GPT response" : "Provider response · status unverified";
  return turn.response_mode === "deterministic-fallback" ? "Deterministic fallback" : "Provider not recorded";
}
export function isModelTurn(turn: ChatTurn) { return turn.response_mode === "gpt" || turn.response_mode === "openai"; }
export function healthChecks(value: unknown) {
  const root = record(value);
  if (!Array.isArray(root.checks) || typeof root.checked_at !== "string") throw new Error("Diagnostic contract unavailable");
  const statuses = ["HEALTHY", "DEGRADED", "UNHEALTHY", "UNKNOWN", "NOT_CONFIGURED", "NOT_TESTED"];
  return list(root.checks).map(row => ({ ...row, status: statuses.includes(String(row.status)) ? String(row.status) : "UNKNOWN" }));
}
export function ageSeconds(value: unknown, now = Date.now()): number | null {
  if (typeof value !== "string") return null;
  const time = Date.parse(value);
  return Number.isFinite(time) && time <= now + 30000 ? Math.max(0, Math.floor((now - time) / 1000)) : null;
}
