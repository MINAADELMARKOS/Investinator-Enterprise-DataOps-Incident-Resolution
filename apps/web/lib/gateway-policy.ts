export const roles = ["viewer", "operator", "planner", "approver", "executor", "verifier"] as const;
export type Role = typeof roles[number];
export type BackendKind = "python" | "java";
export type Permission = { roles: readonly Role[]; token?: string; query: readonly string[] };
const readers = roles;
const coreReads = /^(health|platform\/health|incidents(?:\/[A-Za-z0-9_.:-]+(?:\/postmortem)?)?|topology|telemetry\/(metrics|logs|events|traces)|replay\/(runs|scenarios)|investigator\/conversations(?:\/[A-Za-z0-9_.:-]+(?:\/evidence\/[A-Za-z0-9_.:-]+)?)?|integrations|integrations\/kafka\/health|runbooks|knowledge|approvals|audit|ai\/usage|evaluations|about)$/;
export function permission(method: string, path: string): Permission | null {
  if (path.split("/").some(p => !/^[A-Za-z0-9_.:-]{1,150}$/.test(p) || p === "." || p === "..")) return null;
  if (method === "GET") {
    if (coreReads.test(path)) return { roles: readers, query: /^investigator\/conversations\/[A-Za-z0-9_.:-]+$/.test(path) ? ["details"] : [] };
    if (path === "health/integrations") return { roles: readers, token: "HEALTH_API_TOKEN", query: ["refresh", "service"] };
    if (/^dependencies\/graphs\/[A-Za-z0-9_.:-]+$/.test(path)) return { roles: readers, token: "INVESTIGATION_API_TOKEN", query: ["revision", "at"] };
    if (/^dependencies\/incidents\/[A-Za-z0-9_.:-]+\/(investigation|graph)$/.test(path)) return { roles: readers, token: "INVESTIGATION_API_TOKEN", query: ["graph", "revision"] };
    if (/^connectors(?:\/[A-Za-z0-9_.:-]+\/health)?$/.test(path)) return { roles: readers, token: "CONNECTOR_API_TOKEN", query: [] };
    if (/^monitor\/(status|rules|incidents\/[A-Za-z0-9_.:-]+)$/.test(path)) return { roles: readers, token: "MONITOR_API_TOKEN", query: [] };
    if (path === "remediation/plans") return { roles: readers, token: "REMEDIATION_PLAN_TOKEN", query: ["incident_id"] };
    if (/^remediation\/(registry|plans\/[A-Za-z0-9_.:-]+(?:\/postmortem)?)$/.test(path)) return { roles: readers, token: "REMEDIATION_PLAN_TOKEN", query: [] };
    if (/^knowledge\/feedback\/[A-Za-z0-9_.:-]+$/.test(path)) return { roles: readers, token: "KNOWLEDGE_READ_TOKEN", query: [] };
    if (/^knowledge\/documents\/[A-Za-z0-9_.:-]+\/[0-9]+$/.test(path)) return { roles: readers, token: "KNOWLEDGE_READ_TOKEN", query: [] };
  }
  if (method === "POST") {
    if (path === "investigator/messages") return { roles: readers, query: [] };
    if (path === "incidents/analyze" || path === "replay/runs" || /^incidents\/[A-Za-z0-9_.:-]+\/(verify|actions\/ACT-DEMO\/(approval|execute))$/.test(path)) return { roles: ["operator"], query: [] };
    if (/^monitor\/(pause|resume|settings)$/.test(path)) return { roles: ["operator"], token: "MONITOR_API_TOKEN", query: [] };
    if (/^monitor\/incidents\/[A-Za-z0-9_.:-]+\/transition$/.test(path)) return { roles: ["operator"], token: "MONITOR_API_TOKEN", query: [] };
    if (/^remediation\/(plans|dry-run)$/.test(path)) return { roles: ["planner"], token: "REMEDIATION_PLAN_TOKEN", query: [] };
    if (/^remediation\/plans\/[A-Za-z0-9_.:-]+\/approval$/.test(path)) return { roles: ["approver"], token: "REMEDIATION_APPROVE_TOKEN", query: [] };
    if (/^remediation\/plans\/[A-Za-z0-9_.:-]+\/(execute|recover)$/.test(path)) return { roles: ["executor"], token: "REMEDIATION_EXECUTE_TOKEN", query: [] };
    if (/^remediation\/plans\/[A-Za-z0-9_.:-]+\/(sample|verify)$/.test(path)) return { roles: ["verifier"], token: "REMEDIATION_VERIFY_TOKEN", query: [] };
  }
  return null;
}
export function backendKind(value: string | undefined): BackendKind {
  if (!value || value === "python") return "python";
  if (value === "java") return "java";
  throw new Error("Invalid backend selection");
}
