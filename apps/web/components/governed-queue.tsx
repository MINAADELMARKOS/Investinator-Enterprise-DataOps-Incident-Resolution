"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { list, type RecordValue } from "@/lib/operator-contracts";
export default function GovernedQueue() { const [items, setItems] = useState<RecordValue[]>([]), [error, setError] = useState(""); useEffect(() => { fetch("/api/v1/remediation/plans", { cache: "no-store" }).then(async r => { if (!r.ok) throw new Error("Governed queue unavailable"); setItems(list((await r.json()).items)); }).catch(e => setError(e.message)); }, []); return <div className="panel-body">{error && <p role="alert">{error}</p>}{!items.length && !error && <p>No governed plans. Legacy proposals remain a separate demo workflow.</p>}{items.map(i => <div className="action-item" key={String(i.id)}><strong>{String(i.id)} · {String(i.state)}</strong><p><Link className="table-link" href={`/incidents/${encodeURIComponent(String(i.incident_id))}`}>Inspect immutable target, hash and verification →</Link></p></div>)}</div>; }
