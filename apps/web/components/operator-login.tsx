"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { roles } from "@/lib/gateway-policy";
export default function OperatorLogin() {
  const router = useRouter(); const [role, setRole] = useState("viewer"), [credential, setCredential] = useState(""), [error, setError] = useState(""), [busy, setBusy] = useState(false);
  async function login(event: React.FormEvent) { event.preventDefault(); setBusy(true); setError(""); try { const response = await fetch("/api/operator/session", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ role, credential }) }); if (!response.ok) throw new Error("Login denied or unavailable; verify the selected demo role"); setCredential(""); router.push("/system-health"); router.refresh(); } catch (e) { setError(e instanceof Error ? e.message : "Login unavailable"); } finally { setBusy(false); } }
  return <form onSubmit={login} className="panel-body stack"><p>Use the backend-configured credential for one role. Demo roles do not establish enterprise identity or independent people.</p><label htmlFor="operator-role">Demo role</label><select id="operator-role" className="form-select" value={role} onChange={e => setRole(e.target.value)}>{roles.map(r => <option key={r}>{r}</option>)}</select><label htmlFor="operator-credential">Role credential</label><input id="operator-credential" className="form-input" type="password" autoComplete="off" value={credential} onChange={e => setCredential(e.target.value)} required /><button className="button button-primary" disabled={busy || !credential}>{busy ? "Signing in…" : "Sign in to demo"}</button>{error && <p role="alert" className="error-box">{error}</p>}</form>;
}
