import { test } from "node:test";
import assert from "node:assert/strict";
import { backendKind, permission } from "../lib/gateway-policy.ts";
import { readSession, signSession, sameSecret, secureCookie } from "../lib/session-codec.ts";
import { ageSeconds, healthChecks, isModelTurn, normalizeTurn } from "../lib/operator-contracts.ts";
const secret = "fixture-only-session-signing-secret-32-characters";
test("Python remains default and unknown backend cannot silently route", () => {
  assert.equal(backendKind(undefined), "python"); assert.equal(backendKind("java"), "java"); assert.throws(() => backendKind("http://outside"));
});
test("write roles are disjoint; native administrative routes are never exposed", () => {
  assert.deepEqual(permission("POST", "remediation/plans/owned/approval")?.roles, ["approver"]);
  assert.deepEqual(permission("POST", "remediation/plans/owned/execute")?.roles, ["executor"]);
  assert.deepEqual(permission("POST", "monitor/resume")?.roles, ["operator"]);
  for (const path of ["dependencies/fixtures", "knowledge/revisions", "restart/kafka", "remediation/execute-shell", "incidents/id/actions/DELETE/execute"]) assert.equal(permission("POST", path), null);
});
test("query scope cannot choose tenant, endpoint or arbitrary index", () => {
  assert.deepEqual(permission("GET", "health/integrations")?.query, ["refresh", "service"]);
  for (const path of ["../health", "dependencies/../health", "incidents/%2e%2e", "incidents/https://elsewhere"]) assert.equal(permission("GET", path), null);
  assert.equal(permission("GET", "remediation/plans/p")!.query.includes("tenant_id"), false);
});
test("signed role cannot be changed or replayed beyond expiry", () => {
  const now = 1000000; const token = signSession("viewer", secret, now);
  assert.equal(readSession(token, secret, now), "viewer"); assert.equal(readSession(token, secret, now + 901000), null); assert.equal(readSession(token, secret + "wrong", now), null);
  const parts = token.split("."); const value = JSON.parse(Buffer.from(parts[0], "base64url").toString()); value.role = "approver"; parts[0] = Buffer.from(JSON.stringify(value)).toString("base64url");
  assert.equal(readSession(parts.join("."), secret, now), null); assert.equal(readSession(token + ".extra", secret, now), null);
});
test("short, absent and malformed credentials fail closed", () => {
  assert.equal(sameSecret("short", "short"), false); assert.equal(readSession(undefined, secret), null); assert.equal(readSession("garbage", secret), null); assert.throws(() => signSession("planner", "short"));
});
test("Java and Python provider names normalize without inventing provenance", () => {
  const java = normalizeTurn({ answer: "read result", response_mode: "gpt", provider_status: "OK", tool_trace: [{ tool: "get_evidence", status: "OK" }], source_mode: "SIMULATED" });
  const python = normalizeTurn({ answer: "read result", response_mode: "openai", tool_calls: [{ name: "search_logs", status: "UNAVAILABLE" }] });
  assert.ok(isModelTurn(java)); assert.ok(isModelTurn(python)); assert.equal(java.source_mode, "SIMULATED"); assert.equal(python.source_mode, "NOT_TESTED"); assert.equal(java.tools[0].name, "get_evidence"); assert.equal(python.tools[0].status, "UNAVAILABLE");
});
test("fallback and old history do not fabricate a live provider or measured cost", () => {
  const missing = normalizeTurn({ answer: "Select incident", response_mode: "deterministic-fallback", provider_status: "NOT_CONFIGURED" });
  assert.equal(isModelTurn(missing), false); assert.equal(missing.provider_status, "NOT_CONFIGURED"); assert.deepEqual(missing.usage, {});
  const old = normalizeTurn({ role: "assistant", content: "past advice", payload: { mode: "deterministic-fallback", evidence_ids: ["EV-1"] } }); assert.deepEqual(old.evidence_ids, ["EV-1"]);
});
test("malformed and future health cannot become a green default", () => {
  assert.throws(() => healthChecks({ score: 100 })); assert.equal(healthChecks({ checked_at: "time", checks: [{ status: "SUPER_HEALTHY" }] })[0].status, "UNKNOWN");
  assert.equal(ageSeconds("bad time"), null); assert.equal(ageSeconds(undefined), null); assert.equal(ageSeconds(new Date(2000000).toISOString(), 1000000), null); assert.equal(ageSeconds(new Date(900000).toISOString(), 1000000), 100);
});

test("configured HTTPS origin requires secure cookies behind TLS termination", () => { assert.equal(secureCookie("https://console.example"), true); assert.equal(secureCookie("http://127.0.0.1:13000"), false); assert.equal(secureCookie("invalid"), false); });
