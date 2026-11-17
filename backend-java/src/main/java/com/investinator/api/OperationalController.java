package com.investinator.api;

import static com.investinator.domain.Json.*;
import com.investinator.domain.Contracts.*;
import com.investinator.domain.Json;
import com.investinator.persistence.*;
import com.investinator.workflows.*;
import com.investinator.agent.DeterministicInvestigator;
import com.investinator.security.Redaction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class OperationalController {
    private final WorkflowService service;
    private final Store db;
    private final boolean demo;
    private final String configuredTenant;
    private final Environment env;
    private final com.investinator.chat.ChatService chats;
    private final org.springframework.beans.factory.ObjectProvider<com.investinator.correlation.CorrelationService> correlations;
    public OperationalController(WorkflowService service,@Value("${investinator.demo-mode:true}") boolean demo,
        @Value("${investinator.demo-tenant:demo}") String tenant,Environment env,org.springframework.beans.factory.ObjectProvider<com.investinator.correlation.CorrelationService> correlations,com.investinator.chat.ChatService chats) {
        this.service=service;this.db=service.db;this.demo=demo;this.configuredTenant=tenant;this.env=env;this.correlations=correlations;this.chats=chats;
        if(!tenant.matches("[A-Za-z0-9_-]{1,100}"))throw new IllegalArgumentException("Invalid demo tenant");
    }
    @Bean ApplicationRunner initializeDemo() {return args->{if(demo)service.seed(configuredTenant);};}
    private String tenant() {if(!demo)throw new ApiFailure(503,"OIDC identity provider is required when demo mode is disabled");return configuredTenant;}
    private Object read(Supplier<Object> work){tenant();return db.read(work);}
    private Object command(HttpServletRequest request,Object payload,Supplier<Object> work) {
        String tenant=tenant();return db.command(tenant,request.getRequestURI(),request.getHeader("Idempotency-Key"),DeterministicInvestigator.digest(encode(payload)),work);
    }
    @GetMapping({"/health","/api/v1/health"}) public Object health(){return obj("status","ok","product","InvestiNator","mode",demo?"simulated-demo":"not_configured");}
    @GetMapping("/api/v1/about") public Object about() {
        return obj("product","InvestiNator","tagline","See the signal. Find the cause. Fix with confidence.","statement","AI-powered DataOps Observability, Investigation & Controlled Remediation Platform",
            "creator",obj("name",env.getProperty("INVESTINATOR_CREATOR_NAME","Mina Adel Markos"),"title",env.getProperty("INVESTINATOR_CREATOR_TITLE","Senior Big Data Engineer"),
            "github_url",env.getProperty("INVESTINATOR_GITHUB_URL","https://github.com/MINAADELMARKOS"),"linkedin_url",env.getProperty("INVESTINATOR_LINKEDIN_URL","https://www.linkedin.com/in/mina-markos-343b8b171"),"portfolio_url",env.getProperty("INVESTINATOR_PORTFOLIO_URL")));
    }
    @GetMapping({"/api/v1/platform/health","/command-center"}) public Object platform(){return read(()->platform(tenant()));}
    private Map<String,Object> platform(String tenant) {
        var values=new HashMap<String,Double>();for(var m:service.latest(tenant))values.put(m.get("name").toString(),((Number)m.get("value")).doubleValue());
        boolean kafka=values.getOrDefault("kafka_disk_percent",0.)>=90||values.getOrDefault("kafka_isr_percent",100.)<95;
        boolean spark=values.getOrDefault("spark_executor_memory_percent",0.)>=90||values.getOrDefault("spark_input_delay_seconds",0.)>60;
        boolean data=values.getOrDefault("dataset_freshness_minutes",0.)>15;
        var services=List.of(obj("name","Kafka","status",kafka?"degraded":"healthy","score",kafka?48:99),obj("name","Spark","status",spark?"degraded":"healthy","score",spark?62:98),obj("name","Data health","status",data?"degraded":"healthy","score",data?39:98));
        var active=service.incidents(tenant).stream().filter(i->i.status!=Lifecycle.resolved).toList();
        return obj("product","InvestiNator","mode","simulated-demo","overall_health",obj("score",(int)Math.rint(services.stream().mapToInt(s->((Number)s.get("score")).intValue()).average().orElseThrow())),"services",services,"active_incidents",active,
            "active_p1",active.stream().filter(i->i.severity==Severity.P1).count(),"active_p2",active.stream().filter(i->i.severity==Severity.P2).count(),"systems_degraded",services.stream().filter(s->s.get("status").equals("degraded")).count(),"datasets_delayed",data?1:0,
            "business_impact",obj("affected_dashboards",data?1:0,"slo_at_risk",data,"mode","simulated"),"operations",obj("mttd_seconds",null,"mtti_seconds",null,"mttr_seconds",null,"alert_compression_ratio",null));
    }
    @GetMapping({"/api/v1/telemetry/metrics","/pulse"}) public Object metrics(){return read(()->obj("items",service.latest(tenant()),"mode","simulated-demo"));}
    @PostMapping("/api/v1/telemetry/metrics") public Object ingest(@Valid @RequestBody MetricInput input,HttpServletRequest request){return command(request,input,()->service.ingest(tenant(),input,"demo-api"));}
    @GetMapping("/api/v1/telemetry/{kind}") public Object telemetry(@PathVariable String kind,@RequestParam(defaultValue="50") @Min(1) @Max(200) int limit) {
        if(!List.of("logs","events","traces").contains(kind))throw new ApiFailure(404,"Unknown telemetry signal");
        return read(()->obj("items",db.select(Table.TELEMETRY,tenant(),"signal_type=?",kind.substring(0,kind.length()-1)).stream().limit(limit).map(r->{var p=obj("id",r.get("id"),"entity_id",external(r.get("entity_id")),"observed_at",r.get("observed_at"));p.putAll(map(r.get("payload")));return p;}).toList(),"mode","simulated-demo"));
    }
    private String external(Object id){String value=id.toString(),prefix=tenant()+":";return value.startsWith(prefix)?value.substring(prefix.length()):value;}
    @GetMapping({"/api/v1/topology","/nerve-map"}) public Object topology(){return read(()->{
        String selected=env.getProperty("INVESTINATOR_TOPOLOGY_GRAPH","");if(selected.isBlank())return service.topology(tenant());
        var graph=correlations.getObject().graph(tenant(),selected,null);
        return obj("nodes",graph.nodes().stream().map(n->obj("id",n.id(),"name",n.name(),"type",n.type().name().toLowerCase(Locale.ROOT),"criticality",1)).toList(),"edges",graph.at(java.time.Instant.now()).stream().map(e->obj("from",e.from(),"to",e.to(),"relationship",e.type().name())).toList(),"mode",graph.source_mode().equals("SIMULATED")?"simulated-demo":"live");
    });}
    @GetMapping("/api/v1/topology/{entity}/blast-radius") public Object blast(@PathVariable String entity){return read(()->obj("root",entity,"downstream",service.blast(tenant(),entity),"mode","simulated-demo"));}
    @GetMapping("/api/v1/alerts") public Object alerts(){return read(()->db.all(Table.ALERTS,tenant()).stream().limit(100).map(r->{var p=select(r,"id","incident_id","rule_id","severity","observed_at");p.put("entity_id",external(r.get("entity_id")));p.putAll(map(r.get("payload")));return p;}).toList());}
    @GetMapping({"/api/v1/incidents","/incidents"}) public Object incidents(){return read(()->service.incidents(tenant()));}
    @GetMapping({"/api/v1/incidents/{id}","/incidents/{id}"}) public Object incident(@PathVariable String id){return read(()->service.incident(tenant(),id));}
    @PostMapping({"/api/v1/incidents/analyze","/incidents/analyze"}) public Object analyze(@Valid @RequestBody IncidentRequest input,HttpServletRequest request){
        if(input.metrics().values().stream().anyMatch(v->!(v instanceof Number||v instanceof String)))throw new ApiFailure(422,"Metric values must be numbers or strings");
        return command(request,input,()->service.analyze(tenant(),input));
    }
    @GetMapping("/api/v1/incidents/{id}/timeline") public Object timeline(@PathVariable String id){return read(()->service.incident(tenant(),id).timeline);}
    @GetMapping("/api/v1/incidents/{id}/evidence") public Object evidence(@PathVariable String id){return read(()->service.incident(tenant(),id).evidence);}
    @PostMapping("/api/v1/replay/runs") public Object replay(@Valid @RequestBody ReplayStart input,HttpServletRequest request){return command(request,input,()->service.replay(tenant(),input.scenario()));}
    @GetMapping("/api/v1/replay/runs") public Object replays(){return read(()->db.all(Table.REPLAY_RUNS,tenant()).stream().limit(50).map(r->withPayload(r,"id","scenario","status","incident_id","created_at")).toList());}
    @GetMapping("/api/v1/replay/scenarios") public Object scenarios(){return DemoSeed.scenarios();}
    @PostMapping({"/api/v1/incidents/{id}/actions/{action}/approval","/incidents/{id}/actions/{action}/approval"})
    public Object approve(@PathVariable String id,@PathVariable String action,@Valid @RequestBody ApprovalDecision input,HttpServletRequest request){return command(request,input,()->service.approval(tenant(),id,action,input));}
    @PostMapping("/api/v1/incidents/{id}/actions/{action}/execute") public Object execute(@PathVariable String id,@PathVariable String action,HttpServletRequest request){return command(request,Map.of(),()->service.execute(tenant(),id,action));}
    @PostMapping({"/api/v1/incidents/{id}/verify","/incidents/{id}/verify"}) public Object verify(@PathVariable String id,@Valid @RequestBody(required=false) VerificationRequest ignored,HttpServletRequest request){return command(request,ignored,()->service.verify(tenant(),id));}
    @GetMapping("/api/v1/incidents/{id}/postmortem") public Object postmortem(@PathVariable String id){return read(()->{
        var item=service.incident(tenant(),id);if(item.status!=Lifecycle.resolved)throw new ApiFailure(409,"Postmortem is available after verified resolution");
        var prevention=switch(item.technology){case Kafka->List.of("Alert on early broker storage pressure","Review Kafka capacity and pipeline freshness SLOs");case Spark->List.of("Review executor memory and GC baselines","Alert on input delay before the freshness SLO is breached");case Data->List.of("Add dataset freshness checks at ingestion","Trace missing records through the upstream pipeline");default->List.of("Review monitoring coverage and escalation paths");};
        return obj("incident_id",id,"mode","generated-draft","editable",false,"summary",item.executive_summary,"impact",item.affected_entities,"timeline",item.timeline,"probable_root_cause",item.likely_root_causes,"evidence_ids",item.evidence.stream().map(Evidence::id).toList(),"verification","Recovery criteria passed","preventive_actions",prevention);
    });}
    @GetMapping({"/api/v1/knowledge","/api/v1/runbooks"}) public Object knowledge(){return read(()->db.all(Table.KNOWLEDGE_DOCUMENTS,tenant()).stream().map(r->select(r,"id","title","technology","source","content")).toList());}
    @GetMapping("/api/v1/approvals") public Object approvals(){return read(()->db.all(Table.APPROVALS,tenant()).stream().limit(50).map(r->select(r,"incident_id","action_id","decision","actor","reason","created_at")).toList());}
    @GetMapping({"/api/v1/audit","/audit"}) public Object audit(){return read(()->db.all(Table.AUDIT_EVENTS,tenant()).stream().limit(100).map(r->{var p=select(r,"id","event","actor","incident_id","created_at");p.put("details",r.get("payload"));return p;}).toList());}
    @GetMapping("/api/v1/evaluations") public Object evaluations(){return read(()->db.all(Table.EVALUATIONS,tenant()).stream().limit(50).map(r->withPayload(r,"id","replay_id","score","created_at")).toList());}
    @GetMapping({"/api/v1/ai/usage","/ai/health"}) public Object usage(){return read(()->{
        var rows=db.all(Table.AI_USAGE,tenant()).stream().limit(100).toList();
        return obj("model_calls",rows.stream().mapToInt(r->((Number)r.get("request_count")).intValue()).sum(),"input_tokens",rows.stream().mapToInt(r->((Number)r.get("input_tokens")).intValue()).sum(),"output_tokens",rows.stream().mapToInt(r->((Number)r.get("output_tokens")).intValue()).sum(),"failures",rows.stream().filter(r->!r.get("status").equals("ok")).count(),"recent",rows.stream().map(r->select(r,"model","task","latency_ms","input_tokens","output_tokens","status")).toList(),"mode",rows.stream().anyMatch(r->r.get("status").equals("ok"))?"gpt":"deterministic-fallback");
    });}
    @PostMapping("/api/v1/incidents/{id}/investigate") public Object investigate(@PathVariable String id,HttpServletRequest request){return command(request,Map.of(),()->fallback(service.incident(tenant(),id)));}
    private Object fallback(Incident item) {
        return obj("mode","deterministic-fallback","available",false,"message","AI investigation temporarily unavailable; evidence and deterministic analysis remain available.","analysis",obj("summary",item.executive_summary,
            "root_causes",item.likely_root_causes.stream().filter(c->!c.equals("insufficient_evidence")).map(c->obj("title",c,"explanation","Deterministic runbook hypothesis","confidence",item.confidence,"evidence_ids",item.evidence.stream().limit(2).map(Evidence::id).toList())).toList(),"next_checks",item.recovery_actions.stream().filter(a->!a.dangerous).map(a->a.action).toList(),"uncertainty","No model call was made; inspect evidence before accepting the hypothesis."));
    }
    @PostMapping("/api/v1/investigator/messages") public Object chat(@Valid @RequestBody ChatMessage input,HttpServletRequest request){return chats.admit(()->command(request,input,()->chats.handle(tenant(),input)));}
    @GetMapping("/api/v1/investigator/conversations/{id}") public Object conversation(@PathVariable String id){return read(()->{
        var row=db.select(Table.CONVERSATIONS,tenant(),"id=?",id);if(row.isEmpty())throw new ApiFailure(404,"Conversation not found");
        var messages=new ArrayList<>(db.select(Table.MESSAGES,tenant(),"conversation_id=?",id));Collections.reverse(messages);
        return obj("id",id,"incident_id",row.getFirst().get("incident_id"),"messages",messages.stream().map(r->withPayload(r,"role","content","created_at")).toList());
    });}
    @GetMapping("/api/v1/integrations/kafka/health") public Object kafkaHealth(){tenant();return com.investinator.connectors.KafkaConnectivity.health(env.getProperty("KAFKA_BOOTSTRAP_SERVERS"));}
    @GetMapping("/api/v1/integrations") public Object integrations() {
        tenant();var kafka=Json.map(kafkaHealth());
        return List.of(obj("name","ReplayLab telemetry","status","connected-demo","capabilities",List.of("metrics","alerts","recovery"),"last_sync",null),
            obj("name","OpenAI","status",chats.configurationStatus().toLowerCase(Locale.ROOT).replace('_','-'),"capabilities",List.of("investigation"),"last_sync",null),
            obj("name","Kafka","status",kafka.get("status"),"capabilities",kafka.get("status").equals("connected")?List.of("read-only health","demo workload"):List.of(),"last_sync",null),
            obj("name","Spark","status","planned","capabilities",List.of(),"last_sync",null),
            obj("name","Prometheus","status","compose-available","capabilities",List.of("API scrape"),"last_sync",null),
            obj("name","OpenTelemetry","status",env.getProperty("OTEL_EXPORTER_OTLP_ENDPOINT")==null?"not-configured":"configured","capabilities",List.of("HTTP traces"),"last_sync",null));
    }
    @GetMapping(value="/metrics",produces=MediaType.TEXT_PLAIN_VALUE) public String metricsText(){tenant();return "# TYPE investinator_incidents_total gauge\ninvestinator_incidents_total "+db.jdbc.queryForObject("select count(*) from incidents where tenant_id=?",Long.class,tenant())+"\n# TYPE investinator_alerts_total gauge\ninvestinator_alerts_total "+db.jdbc.queryForObject("select count(*) from alerts where tenant_id=?",Long.class,tenant())+"\n";}
    private Map<String,Object> select(Map<String,Object> row,String... keys){var out=new LinkedHashMap<String,Object>();for(String k:keys)out.put(k,row.get(k));return out;}
    private Map<String,Object> withPayload(Map<String,Object> row,String... keys){var out=select(row,keys);out.putAll(map(row.get("payload")));return out;}
}
