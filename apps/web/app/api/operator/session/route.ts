import { NextRequest, NextResponse } from "next/server";
import { currentRole, selectedBackend, sessionCookie, sameOrigin } from "@/lib/backend";
import { roles, type Role } from "@/lib/gateway-policy";
import { sameSecret, signSession } from "@/lib/session-codec";
export const runtime = "nodejs";
const attempts = new Map<string, { count: number; until: number }>();
export async function GET() { return NextResponse.json({ backend: selectedBackend(), role: await currentRole(), scope: "SIMULATED_DEMO" }, { headers: { "Cache-Control": "no-store" } }); }
export async function POST(request: NextRequest) {
  if (!sameOrigin(request.headers.get("origin"), request.nextUrl.origin) || process.env.WEB_DEMO_ENABLED !== "true") return NextResponse.json({ detail: "Demo session unavailable" }, { status: 403 });
  if (Number(request.headers.get("content-length") ?? 0) > 2048) return NextResponse.json({ detail: "Invalid login" }, { status: 413 });
  let input; try { const text = await request.text(); if (text.length > 2048) throw new Error(); input = JSON.parse(text); } catch { return NextResponse.json({ detail: "Invalid login" }, { status: 422 }); }
  if (!input || typeof input !== "object" || Array.isArray(input)) return NextResponse.json({ detail: "Invalid login" }, { status: 422 });
  if (input.logout === true) { const response = NextResponse.json({ logged_out: true }); response.cookies.set(sessionCookie, "", { httpOnly: true, sameSite: "strict", path: "/", maxAge: 0 }); return response; }
  const role = input.role as Role;
  const time = Date.now();
  const limit = attempts.get("demo") ?? { count: 0, until: time + 60000 };
  if (time > limit.until) { limit.count = 0; limit.until = time + 60000; }
  attempts.set("demo", limit);
  if (++limit.count > 30) return NextResponse.json({ detail: "Login rate limit; retry after one minute" }, { status: 429 });
  const configured = roles.map(r => process.env["UI_" + r.toUpperCase() + "_LOGIN_TOKEN"]).filter((v): v is string => !!v);
  if (new Set(configured).size !== configured.length) return NextResponse.json({ detail: "Role credentials must be distinct" }, { status: 503 });
  const key = process.env["UI_" + String(role).toUpperCase() + "_LOGIN_TOKEN"] ?? "";
  if (!roles.includes(role) || typeof input.credential !== "string" || !sameSecret(key, input.credential) || (process.env.UI_SESSION_SECRET?.length ?? 0) < 32)
    return NextResponse.json({ detail: "Invalid or unconfigured demo credential" }, { status: 403 });
  const response = NextResponse.json({ role, expires_in: 900, scope: "SIMULATED_DEMO" });
  response.cookies.set(sessionCookie, signSession(role, process.env.UI_SESSION_SECRET!), { httpOnly: true, secure: request.nextUrl.protocol === "https:", sameSite: "strict", path: "/", maxAge: 900 });
  return response;
}
