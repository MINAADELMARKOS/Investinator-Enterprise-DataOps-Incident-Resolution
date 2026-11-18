import { apiGet, type Incident } from "@/lib/api";
import { selectedBackend } from "@/lib/backend";
import InvestigatorChat from "@/components/investigator-chat";
import { PageHeading, Unavailable } from "@/components/ui";

export default async function Investigator({searchParams}:{searchParams:Promise<{incident?:string}>}) {
  const [incidents, params]=await Promise.all([apiGet<Incident[]>("/incidents"),searchParams]);
  if (!incidents) return <><PageHeading eyebrow="Workspace / AI" title="Investigator" /><Unavailable /></>;
  return <><PageHeading eyebrow="Workspace / AI" title="Investigator" description="Ask, investigate, and plan recovery with incident evidence in view." />
    <div className="callout block-gap"><strong>AI availability is explicit.</strong> Without an OpenAI key and configured model, the Investigator uses a deterministic evidence summary.</div>
    <InvestigatorChat backend={selectedBackend()} incidents={incidents} defaultIncident={params.incident} />
  </>;
}
