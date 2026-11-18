export type RecordValue = Record<string, unknown>;
export const record = (value: unknown): RecordValue => typeof value === "object" && value !== null && !Array.isArray(value) ? value as RecordValue : {};
export const list = (value: unknown): RecordValue[] => Array.isArray(value) ? value.map(record) : [];
const text = (value: unknown, fallback = "UNKNOWN") => typeof value === "string" ? value : fallback;
export type ToolTrace = { name: string; status: string };
export type ChatTurn = { role: "user" | "assistant"; content: string; evidence_ids: string[]; response_mode: string; provider_status: string; source_mode: string; model: string | null; tools: ToolTrace[]; analysis: RecordValue; usage: RecordValue; created_at?: string };
export function normalizeTurn(input: unknown): ChatTurn {
  const row = record(input), payload = record(row.payload);
  const mode = text(row.response_mode ?? payload.response_mode ?? payload.mode, "unknown");
  const tools = list(row.tool_trace ?? row.tool_calls ?? payload.tool_trace ?? payload.tool_calls).map(t => ({ name: text(t.tool ?? t.name), status: text(t.status) }));
  const ids = row.evidence_ids ?? payload.evidence_ids;
  return { role: row.role === "user" ? "user" : "assistant", content: text(row.content ?? row.answer, ""), evidence_ids: Array.isArray(ids) ? ids.filter((s): s is string => typeof s === "string") : [], response_mode: mode, provider_status: text(row.provider_status ?? payload.provider_status, mode === "openai" || mode === "gpt" ? "OK" : "NOT_TESTED"), source_mode: text(row.source_mode ?? payload.source_mode, "NOT_TESTED"), model: typeof (row.model ?? payload.model) === "string" ? String(row.model ?? payload.model) : null, tools, analysis: record(row.analysis ?? payload.analysis), usage: record(row.usage ?? payload.usage), created_at: typeof row.created_at === "string" ? row.created_at : undefined };
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
