import { apiGet } from "@/lib/api";
import { publicContext, requireJavaSession } from "@/lib/backend";
import MonitorConsole from "@/components/monitor-console";
import { PageHeading, Panel } from "@/components/ui";
import type { RecordValue } from "@/lib/operator-contracts";
export default async function Monitoring() {
  const gateway = await publicContext();
  if (gateway.backend !== "java") return <><PageHeading eyebrow="Operations / Monitoring" title="Monitoring" /><p>The continuous Java worker is explicitly opt-in. Python remains the current backend.</p></>;
  await requireJavaSession();
  const [state, rules] = await Promise.all([apiGet<RecordValue>("/monitor/status"), apiGet<RecordValue>("/monitor/rules")]);
  return <><PageHeading eyebrow="Operations / Monitoring" title="Monitoring" description="Bounded, replayable detection with explicit suppression and source provenance." /><Panel title="Demo monitor controls"><MonitorConsole initial={state} rules={rules} role={gateway.role} /></Panel></>;
}
