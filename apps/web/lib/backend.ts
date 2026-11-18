import "server-only";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { backendKind, permission, type Role } from "./gateway-policy";
import { readSession } from "./session-codec";
export const sessionCookie = "investinator_operator";
export class GatewayError extends Error { constructor(public status: number, message: string) { super(message); } }
export function selectedBackend() { return backendKind(process.env.INVESTINATOR_BACKEND); }
export async function currentRole(): Promise<Role | null> {
  if (process.env.WEB_DEMO_ENABLED !== "true") return null;
  return readSession((await cookies()).get(sessionCookie)?.value, process.env.UI_SESSION_SECRET ?? "");
}
export async function publicContext() { return { backend: selectedBackend(), role: await currentRole(), simulated: true }; }
export async function requireJavaSession() { if (selectedBackend() === "java" && !(await currentRole())) redirect("/operator/login"); }
export async function backendRequest(path: string, options: { method?: string; body?: string; key?: string; role?: Role | null; query?: URLSearchParams } = {}) {
  const kind = selectedBackend(), method = options.method ?? "GET";
  const rule = permission(method, path);
  if (!rule) throw new GatewayError(403, "Operation is not exposed by this console");
  const query = options.query ?? new URLSearchParams();
  for (const key of query.keys()) if (!rule.query.includes(key)) throw new GatewayError(422, "Unsupported query scope");
  const publicHealth = method === "GET" && path === "health";
  const role = options.role === undefined ? await currentRole() : options.role;
  if (kind === "java" && !publicHealth && (!role || !rule.roles.includes(role))) throw new GatewayError(role ? 403 : 401, "Operator session or role required");
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (kind === "java" && rule.token) {
    const credential = process.env[rule.token];
    if (!credential) throw new GatewayError(503, "Backend capability is not configured");
    headers.Authorization = "Bearer " + credential;
  }
  if (options.key) { if (!/^[A-Za-z0-9._:-]{1,128}$/.test(options.key)) throw new GatewayError(422, "Invalid request identity"); headers["Idempotency-Key"] = options.key; }
  if ((options.body?.length ?? 0) > 120000) throw new GatewayError(413, "Request too large");
  const base = kind === "java" ? process.env.JAVA_API_INTERNAL_URL ?? "http://127.0.0.1:8081" : process.env.API_INTERNAL_URL ?? "http://127.0.0.1:8000";
  const url = new URL("/api/v1/" + path.split("/").map(encodeURIComponent).join("/"), base);
  if (!["http:", "https:"].includes(url.protocol) || url.username || url.password) throw new GatewayError(503, "Backend configuration unavailable");
  url.search = query.toString();
  return fetch(url, { method, headers, body: method === "GET" ? undefined : options.body, cache: "no-store", redirect: "error", signal: AbortSignal.timeout(path === "investigator/messages" ? 25000 : 12000) });
}

export function sameOrigin(origin: string | null, internalOrigin: string) {
  const expected = process.env.UI_PUBLIC_ORIGIN ?? internalOrigin;
  try { const url = new URL(expected); return ["http:", "https:"].includes(url.protocol) && url.origin === expected && origin === expected; } catch { return false; }
}
