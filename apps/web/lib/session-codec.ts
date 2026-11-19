import { createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import { roles, type Role } from "./gateway-policy.ts";
export function sameSecret(expected: string, supplied: string): boolean {
  const a = Buffer.from(expected), b = Buffer.from(supplied);
  return a.length >= 24 && a.length === b.length && timingSafeEqual(a, b);
}
export function signSession(role: Role, secret: string, now = Date.now()): string {
  if (secret.length < 32 || !roles.includes(role)) throw new Error("Session unavailable");
  const payload = Buffer.from(JSON.stringify({ role, expires: Math.floor(now / 1000) + 900, nonce: randomBytes(18).toString("base64url") })).toString("base64url");
  return payload + "." + createHmac("sha256", secret).update(payload).digest("base64url");
}
export function readSession(value: string | undefined, secret: string, now = Date.now()): Role | null {
  try {
    if (!value || value.length > 600 || secret.length < 32) return null;
    const [payload, signature, extra] = value.split(".");
    if (extra !== undefined || !sameSecret(createHmac("sha256", secret).update(payload).digest("base64url"), signature)) return null;
    const claims = JSON.parse(Buffer.from(payload, "base64url").toString());
    const time = Math.floor(now / 1000);
    return roles.includes(claims.role) && claims.expires > time && claims.expires <= time + 900 && /^[A-Za-z0-9_-]{24}$/.test(claims.nonce) ? claims.role : null;
  } catch { return null; }
}

/** Deployment-owned public origin, never caller-forwarded protocol headers. */
export function secureCookie(origin: string): boolean {
  try { return new URL(origin).protocol === "https:"; } catch { return false; }
}
