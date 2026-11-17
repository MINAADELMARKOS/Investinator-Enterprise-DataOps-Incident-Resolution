# API parity matrix

Generated from main FastAPI routes; Java implementation/parity: NOT_IMPLEMENTED for every row.
OpenAPI snapshot preserves request schemas and declared responses. Many responses are generic dict/list; handler source is authoritative, not a complete typed contract. All aliases below remain in scope.

| Method | Route | Handler / source | Declared response | Java parity |
|---|---|---|---|---|
| GET | `/api/v1/health` | `health` (src/incident_copilot/api.py:112) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/health` | `health` (src/incident_copilot/api.py:112) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/about` | `about` (src/incident_copilot/api.py:119) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/command-center` | `platform_health` (src/incident_copilot/api.py:129) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/platform/health` | `platform_health` (src/incident_copilot/api.py:129) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/pulse` | `metrics` (src/incident_copilot/api.py:155) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/telemetry/metrics` | `metrics` (src/incident_copilot/api.py:155) | `<class 'dict'>` | NOT_IMPLEMENTED |
| POST | `/api/v1/telemetry/metrics` | `post_metric` (src/incident_copilot/api.py:161) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/telemetry/{signal_type}` | `telemetry` (src/incident_copilot/api.py:169) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/nerve-map` | `topology` (src/incident_copilot/api.py:183) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/topology` | `topology` (src/incident_copilot/api.py:183) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/topology/{entity_id}/blast-radius` | `impact` (src/incident_copilot/api.py:196) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/alerts` | `alerts` (src/incident_copilot/api.py:201) | `list[dict]` | NOT_IMPLEMENTED |
| GET | `/incidents` | `incidents` (src/incident_copilot/api.py:210) | `list[incident_copilot.schemas.IncidentResponse]` | NOT_IMPLEMENTED |
| GET | `/api/v1/incidents` | `incidents` (src/incident_copilot/api.py:210) | `list[incident_copilot.schemas.IncidentResponse]` | NOT_IMPLEMENTED |
| POST | `/incidents/analyze` | `analyze_incident` (src/incident_copilot/api.py:216) | `<class 'incident_copilot.schemas.IncidentResponse'>` | NOT_IMPLEMENTED |
| POST | `/api/v1/incidents/analyze` | `analyze_incident` (src/incident_copilot/api.py:216) | `<class 'incident_copilot.schemas.IncidentResponse'>` | NOT_IMPLEMENTED |
| GET | `/incidents/{incident_id}` | `incident` (src/incident_copilot/api.py:228) | `<class 'incident_copilot.schemas.IncidentResponse'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/incidents/{incident_id}` | `incident` (src/incident_copilot/api.py:228) | `<class 'incident_copilot.schemas.IncidentResponse'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/incidents/{incident_id}/timeline` | `incident_timeline` (src/incident_copilot/api.py:235) | `list[dict]` | NOT_IMPLEMENTED |
| GET | `/api/v1/incidents/{incident_id}/evidence` | `incident_evidence` (src/incident_copilot/api.py:241) | `list[dict]` | NOT_IMPLEMENTED |
| POST | `/api/v1/incidents/{incident_id}/investigate` | `investigate_incident` (src/incident_copilot/api.py:247) | `<class 'dict'>` | NOT_IMPLEMENTED |
| POST | `/api/v1/investigator/messages` | `chat_message` (src/incident_copilot/api.py:254) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/investigator/conversations/{conversation_id}` | `conversation_messages` (src/incident_copilot/api.py:294) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/replay/scenarios` | `replay_scenarios` (src/incident_copilot/api.py:310) | `list[dict]` | NOT_IMPLEMENTED |
| POST | `/api/v1/replay/runs` | `replay_start` (src/incident_copilot/api.py:315) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/replay/runs` | `replay_runs` (src/incident_copilot/api.py:325) | `list[dict]` | NOT_IMPLEMENTED |
| POST | `/incidents/{incident_id}/actions/{action_id}/approval` | `decide_approval` (src/incident_copilot/api.py:333) | `<class 'dict'>` | NOT_IMPLEMENTED |
| POST | `/api/v1/incidents/{incident_id}/actions/{action_id}/approval` | `decide_approval` (src/incident_copilot/api.py:333) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/approvals` | `approvals` (src/incident_copilot/api.py:364) | `list[dict]` | NOT_IMPLEMENTED |
| POST | `/api/v1/incidents/{incident_id}/actions/{action_id}/execute` | `execute_action` (src/incident_copilot/api.py:373) | `<class 'dict'>` | NOT_IMPLEMENTED |
| POST | `/incidents/{incident_id}/verify` | `verify_incident` (src/incident_copilot/api.py:401) | `<class 'dict'>` | NOT_IMPLEMENTED |
| POST | `/api/v1/incidents/{incident_id}/verify` | `verify_incident` (src/incident_copilot/api.py:401) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/incidents/{incident_id}/postmortem` | `postmortem` (src/incident_copilot/api.py:414) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/runbooks` | `knowledge` (src/incident_copilot/api.py:433) | `list[dict]` | NOT_IMPLEMENTED |
| GET | `/api/v1/knowledge` | `knowledge` (src/incident_copilot/api.py:433) | `list[dict]` | NOT_IMPLEMENTED |
| GET | `/api/v1/evaluations` | `evaluations` (src/incident_copilot/api.py:441) | `list[dict]` | NOT_IMPLEMENTED |
| GET | `/ai/health` | `ai_usage` (src/incident_copilot/api.py:449) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/ai/usage` | `ai_usage` (src/incident_copilot/api.py:449) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/api/v1/integrations` | `integrations` (src/incident_copilot/api.py:464) | `list[dict]` | NOT_IMPLEMENTED |
| GET | `/api/v1/integrations/kafka/health` | `integration_kafka_health` (src/incident_copilot/api.py:478) | `<class 'dict'>` | NOT_IMPLEMENTED |
| GET | `/audit` | `audit_events` (src/incident_copilot/api.py:483) | `list[dict]` | NOT_IMPLEMENTED |
| GET | `/api/v1/audit` | `audit_events` (src/incident_copilot/api.py:483) | `list[dict]` | NOT_IMPLEMENTED |
| GET | `/metrics` | `prometheus_metrics` (src/incident_copilot/api.py:493) | `<class 'str'>` | NOT_IMPLEMENTED |

## Frontend consumers (exact call-site evidence)

Dynamic template paths below must retain parameter substitution. `/api/v1/[...path]` forwards browser GET/POST to API_INTERNAL_URL; server pages use lib/api.ts.

### apps/web/app/about/page.tsx

```tsx
import { apiGet } from "@/lib/api";
export default async function AboutPage(){const about=await apiGet<About>("/about");if(!about)return <><PageHeading eyebrow="Platform / About" title="About InvestiNator" /><Unavailable /></>;
```

### apps/web/app/administration/page.tsx

```tsx
import { apiGet } from "@/lib/api";
export default async function Administration(){const about=await apiGet<About>("/about");if(!about)return <><PageHeading eyebrow="Platform / Settings" title="Administration" /><Unavailable /></>;
```

### apps/web/app/ai-observability/page.tsx

```tsx
import { apiGet } from "@/lib/api";
export default async function AIObservability(){const [usage,evaluations]=await Promise.all([apiGet<Usage>("/ai/usage"),apiGet<Evaluation[]>("/evaluations")]);if(!usage||!evaluations)return <><PageHeading eyebrow="Platform / AI" title="AI Observability" /><Unavailable /></>;
```

### apps/web/app/api/v1/[...path]/route.ts

```tsx
const response = await fetch(destination, {
```

### apps/web/app/approvals/page.tsx

```tsx
import { apiGet, type Incident } from "@/lib/api";
export default async function Approvals(){const [incidents,decisions]=await Promise.all([apiGet<Incident[]>("/incidents"),apiGet<Approval[]>("/approvals")]);if(!incidents||!decisions)return <><PageHeading eyebrow="Operations / Control" title="Approvals" /><Unavailable /></>;
```

### apps/web/app/explore/page.tsx

```tsx
import { apiGet, type Metric } from "@/lib/api";
apiGet<{items:Metric[]}>("/telemetry/metrics"), apiGet<{items:Signal[]}>("/telemetry/logs"),
apiGet<{items:Signal[]}>("/telemetry/events"), apiGet<{items:Signal[]}>("/telemetry/traces"),
```

### apps/web/app/incidents/[id]/page.tsx

```tsx
import { apiGet, type Incident } from "@/lib/api";
const item = await apiGet<Incident>(`/incidents/${encodeURIComponent(id)}`);
```

### apps/web/app/incidents/[id]/postmortem/page.tsx

```tsx
import { apiGet } from "@/lib/api";
const post = await apiGet<Postmortem>(`/incidents/${encodeURIComponent(id)}/postmortem`);
```

### apps/web/app/incidents/page.tsx

```tsx
import { apiGet, type Incident } from "@/lib/api";
const incidents = await apiGet<Incident[]>("/incidents");
```

### apps/web/app/integrations/page.tsx

```tsx
import { apiGet } from "@/lib/api";
export default async function Integrations(){const items=await apiGet<Integration[]>("/integrations");if(!items)return <><PageHeading eyebrow="Platform / Connectors" title="Integrations" /><Unavailable /></>;
```

### apps/web/app/investigator/page.tsx

```tsx
import { apiGet, type Incident } from "@/lib/api";
const [incidents, params]=await Promise.all([apiGet<Incident[]>("/incidents"),searchParams]);
```

### apps/web/app/knowledge/page.tsx

```tsx
import { apiGet } from "@/lib/api";
export default async function KnowledgePage(){const items=await apiGet<Knowledge[]>("/knowledge");if(!items)return <><PageHeading eyebrow="Operations / Memory" title="Knowledge" /><Unavailable /></>;
```

### apps/web/app/nervemap/page.tsx

```tsx
import { apiGet, type Topology } from "@/lib/api";
const data = await apiGet<Topology>("/topology");
```

### apps/web/app/page.tsx

```tsx
import { apiGet, type Health, type Metric } from "@/lib/api";
const [health, telemetry] = await Promise.all([apiGet<Health>("/platform/health"), apiGet<{ items: Metric[] }>("/telemetry/metrics")]);
```

### apps/web/app/pulse/page.tsx

```tsx
import { apiGet, type Metric } from "@/lib/api";
const data = await apiGet<{items: Metric[]; mode: string}>("/telemetry/metrics");
```

### apps/web/app/replaylab/page.tsx

```tsx
import { apiGet } from "@/lib/api";
const [scenarios,runs]=await Promise.all([apiGet<Scenario[]>("/replay/scenarios"),apiGet<Run[]>("/replay/runs")]);
```

### apps/web/app/runbooks/page.tsx

```tsx
import { apiGet } from "@/lib/api";
export default async function Runbooks(){const items=await apiGet<Knowledge[]>("/runbooks");if(!items)return <><PageHeading eyebrow="Operations / Guidance" title="Runbooks" /><Unavailable /></>;
```

### apps/web/components/incident-actions.tsx

```tsx
const response = await fetch(endpoint, { method: "POST", headers: { "Content-Type": "application/json" },
```

### apps/web/components/investigator-chat.tsx

```tsx
try { const response=await fetch("/api/v1/investigator/messages",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({conversation_id:conversationId,incident_id:incidentId||null,mode,message:value})});
```

### apps/web/components/replay-controls.tsx

```tsx
async function start(id:string){setBusy(id);setError(null);try{const response=await fetch("/api/v1/replay/runs",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({scenario:id})});const data=await response.json();if(!response.ok)throw new Error(data.detail??"Replay failed");router.push(`/incidents/${data.incident_id}`);router.refresh();}catch(cause){setError(cause instanceof Error?cause.message:"Replay failed");}finally{setBusy(null);}}
```

### apps/web/lib/api.ts

```tsx
export async function apiGet<T>(path: string): Promise<T | null> {
const response = await fetch(`${backend}/api/v1${path}`, { cache: "no-store", signal: AbortSignal.timeout(8000) });
```

## Behavioral parity requirements

Preserve 404 for missing/cross-tenant records, 409 for active replay/action state and unavailable verification, 403 for unapproved or non-ACT-DEMO execution, and 503 for protected endpoints outside demo mode. Preserve server tenant override, response keys, simulation labels, X-Request-ID and security headers. Tests in test_platform.py are behavioral evidence. IncidentResponse/Pydantic definitions are in the snapshot. Dictionary response shapes and conditional errors require differential fixtures before each Java cutover; no endpoint is certified migrated.
