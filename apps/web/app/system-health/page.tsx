import Link from "next/link";
import { apiGet } from "@/lib/api";
import { selectedBackend } from "@/lib/backend";
import { PageHeading, Panel } from "@/components/ui";
import SystemHealth from "@/components/system-health";
import type { RecordValue } from "@/lib/operator-contracts";
export default async function Health() {
  if (selectedBackend() !== "java") return <><PageHeading eyebrow="Platform / Diagnostics" title="System Health" /><div className="callout">Native diagnostic reports require the explicitly selected Java backend. Python remains the default; no service is marked healthy from missing Java data. <Link href="/integrations">View Python integration inventory</Link></div></>;
  const report = await apiGet<RecordValue>("/health/integrations?refresh=false");
  return <><PageHeading eyebrow="Platform / Diagnostics" title="System Health" description="Read-only integration evidence, semantic capabilities and freshness." /><Panel title="Diagnostic observations" detail="Missing is UNKNOWN"><SystemHealth initial={report} /></Panel></>;
}
