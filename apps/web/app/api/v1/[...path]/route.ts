import { NextRequest, NextResponse } from "next/server";
import { backendRequest, GatewayError, selectedBackend, sameOrigin } from "@/lib/backend";
import { boundedText, BodyLimitError, BodyEncodingError } from "@/lib/bounded-body";
export const runtime = "nodejs";
async function forward(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  try {
    if (request.method !== "GET" && selectedBackend() === "java" && !sameOrigin(request.headers.get("origin"), request.nextUrl.origin))
      return NextResponse.json({ detail: "Same-origin request required" }, { status: 403 });
    if (Number(request.headers.get("content-length") ?? 0) > 120000) return NextResponse.json({ detail: "Request too large" }, { status: 413 });
    const { path } = await context.params;
    const response = await backendRequest(path.join("/"), { method: request.method,
      query: request.nextUrl.searchParams, key: request.headers.get("idempotency-key") ?? undefined,
      body: request.method === "GET" ? undefined : await boundedText(request, 120000) });
    const headers = { "Content-Type": response.headers.get("content-type") ?? "application/json", "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" };
    return new NextResponse(await response.text(), { status: response.status, headers });
  } catch (cause) {
    return NextResponse.json({ detail: cause instanceof GatewayError ? cause.message : "InvestiNator API unavailable or timed out; no success assumed" }, { status: cause instanceof BodyLimitError ? 413 : cause instanceof BodyEncodingError ? 422 : cause instanceof GatewayError ? cause.status : 503 });
  }
}
export const GET = forward;
export const POST = forward;
