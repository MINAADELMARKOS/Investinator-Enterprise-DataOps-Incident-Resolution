package com.investinator.workflows;

import com.investinator.api.ApiFailure;
import com.investinator.domain.Contracts.*;
import com.investinator.domain.Json;
import com.investinator.persistence.*;
import com.investinator.agent.DeterministicInvestigator;
import com.investinator.security.Redaction;
import static com.investinator.domain.Json.*;
import java.time.*;
import java.util.*;
import java.util.function.DoublePredicate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class WorkflowService {
    public final Store db;
    public WorkflowService(Store db) {this.db=db;}
    public void seed(String tenant) {
        db.write(tenant,()->{
            if(!db.all(Table.ENTITIES,tenant).isEmpty())return null;
            for(var n:DemoSeed.tuples("nodes"))db.insert(Table.ENTITIES,tenant,obj("id",tenant+":"+n.get(0),"kind",n.get(1),"name",n.get(2),"criticality",n.get(3),"payload",obj("demo",true,"status","healthy")));
            for(var e:DemoSeed.tuples("edges"))db.insert(Table.RELATIONSHIPS,tenant,obj("source_id",tenant+":"+e.get(0),"target_id",tenant+":"+e.get(1),"kind",e.get(2)));
            for(var r:DemoSeed.tuples("runbooks")) {
                String source=r.get(1).toString();String title=source.substring(source.lastIndexOf('/')+1).replace(".md","").replace('_',' ');
                title=java.util.regex.Pattern.compile("[A-Za-z]+").matcher(title).replaceAll(match -> match.group().substring(0,1).toUpperCase(Locale.ROOT)+match.group().substring(1).toLowerCase(Locale.ROOT));
                db.insert(Table.KNOWLEDGE_DOCUMENTS,tenant,obj("title",title,"technology",r.get(0),"source",source,"content",r.get(2)));
            }
            for(var m:DemoSeed.tuples("metrics"))telemetry(tenant,m.get(0).toString(),"metric",m.get(1).toString(),((Number)m.get(2)).doubleValue(),now(),obj("unit",m.get(3),"mode","simulated","value",m.get(2)));
            return null;
        });
    }
    public void audit(String tenant,String event,String incident,String actor,Map<String,Object> details) {
        db.insert(Table.AUDIT_EVENTS,tenant,obj("incident_id",incident,"actor",actor,"event",event,"payload",details));
    }
    public Incident incident(String tenant,String id) {
        var rows=db.select(Table.INCIDENTS,tenant,"id=?",id);
        if(rows.isEmpty())throw new ApiFailure(404,"Incident not found");
        return Json.decode(encode(rows.getFirst().get("payload")),Incident.class);
    }
    public List<Incident> incidents(String tenant) {return db.all(Table.INCIDENTS,tenant).stream().limit(50).map(r->Json.decode(encode(r.get("payload")),Incident.class)).toList();}
    public void save(String tenant,Incident incident) {
        var values=obj("environment",incident.environment,"status",incident.status,"severity",incident.severity,"technology",incident.technology,"payload",incident,"updated_at",now());
        if(db.select(Table.INCIDENTS,tenant,"id=?",incident.incident_id).isEmpty()) {
            values.put("id",incident.incident_id);db.insert(Table.INCIDENTS,tenant,values);
        }else db.update(Table.INCIDENTS,tenant,incident.incident_id,values);
    }
    public Incident analyze(String tenant,IncidentRequest request) {
        var result=DeterministicInvestigator.analyze(request,tenant);save(tenant,result);
        audit(tenant,"investigation_created",result.incident_id,"demo.senior_sre",Map.of());return result;
    }
    private String telemetry(String tenant,String entity,String type,String name,Double numeric,String observed,Map<String,Object> payload) {
        String id=id();db.insert(Table.TELEMETRY,tenant,obj("id",id,"entity_id",tenant+":"+entity,"signal_type",type,"name",name,"numeric_value",numeric,"observed_at",observed,"payload",payload));return id;
    }
    public List<Map<String,Object>> latest(String tenant) {
        var seen=new HashSet<String>();var out=new ArrayList<Map<String,Object>>();
        for(var row:db.select(Table.TELEMETRY,tenant,"signal_type=?","metric").stream().limit(500).toList()) {
            String key=row.get("entity_id")+":"+row.get("name");if(!seen.add(key))continue;
            var payload=map(row.get("payload"));
            out.add(obj("id",row.get("id"),"entity_id",external(tenant,row.get("entity_id")),"name",row.get("name"),"value",row.get("numeric_value"),"unit",payload.getOrDefault("unit",""),"observed_at",row.get("observed_at"),"mode",payload.getOrDefault("mode","external")));
        }
        return out;
    }
    private String external(String tenant,Object id) {String value=id.toString(),prefix=tenant+":";return value.startsWith(prefix)?value.substring(prefix.length()):value;}
    public Map<String,Object> topology(String tenant) {
        return obj("nodes",db.all(Table.ENTITIES,tenant).stream().map(r->obj("id",external(tenant,r.get("id")),"name",r.get("name"),"type",r.get("kind"),"criticality",r.get("criticality"))).toList(),
            "edges",db.all(Table.RELATIONSHIPS,tenant).stream().map(r->obj("from",external(tenant,r.get("source_id")),"to",external(tenant,r.get("target_id")),"relationship",r.get("kind"))).toList(),"mode","simulated-demo");
    }
    public List<Map<String,Object>> blast(String tenant,String root) {
        var graph=new HashMap<String,List<String>>();for(var e:db.all(Table.RELATIONSHIPS,tenant))graph.computeIfAbsent(e.get("source_id").toString(),k->new ArrayList<>()).add(e.get("target_id").toString());
        var visited=new HashSet<String>();var queue=new ArrayDeque<String>();queue.add(tenant+":"+root);visited.add(tenant+":"+root);
        while(!queue.isEmpty())for(var child:graph.getOrDefault(queue.remove(),List.of()))if(visited.add(child))queue.add(child);
        return db.all(Table.ENTITIES,tenant).stream().filter(r->visited.contains(r.get("id"))).map(r->obj("id",external(tenant,r.get("id")),"name",r.get("name"),"kind",r.get("kind"),"criticality",r.get("criticality"))).toList();
    }
    private boolean connected(String tenant,String first,String second) {
        var graph=new HashMap<String,List<String>>();for(var e:db.all(Table.RELATIONSHIPS,tenant)) {
            String a=e.get("source_id").toString(),b=e.get("target_id").toString();graph.computeIfAbsent(a,k->new ArrayList<>()).add(b);graph.computeIfAbsent(b,k->new ArrayList<>()).add(a);
        }
        var visited=new HashSet<String>();var queue=new ArrayDeque<String>();queue.add(first);visited.add(first);
        while(!queue.isEmpty()){String current=queue.remove();if(current.equals(second))return true;for(String n:graph.getOrDefault(current,List.of()))if(visited.add(n))queue.add(n);}return false;
    }
    public Map<String,Object> ingest(String tenant,MetricInput metric,String mode) {
        if(!Double.isFinite(metric.value()))throw new ApiFailure(422,"Metric must be finite");
        String observed=metric.observed_at()==null?now():parseTime(metric.observed_at());
        String id=telemetry(tenant,metric.entity_id(),"metric",metric.name(),metric.value(),observed,obj("unit",metric.unit(),"mode",mode,"value",metric.value(),"labels",metric.labels()));
        var rules=DemoSeed.mapping("rules");if(!rules.containsKey(metric.name()))return obj("metric_id",id,"alert",null,"incident_id",null);
        var rule=(List<?>)rules.get(metric.name());double threshold=((Number)rule.get(0)).doubleValue();boolean breach=rule.get(1).equals("above")?metric.value()>threshold:metric.value()<threshold;
        if(!breach)return obj("metric_id",id,"alert",null,"incident_id",null);
        Severity severity=Severity.valueOf(rule.get(2).toString());String message=rule.get(3).toString();Incident item=null;
        for(var candidate:db.select(Table.INCIDENTS,tenant,"status=? and created_at>=?","investigating",java.sql.Timestamp.from(Instant.parse(observed).minusSeconds(1800))).stream().limit(25).toList()) {
            if(db.select(Table.ALERTS,tenant,"incident_id=?",candidate.get("id")).stream().anyMatch(a->connected(tenant,a.get("entity_id").toString(),tenant+":"+metric.entity_id()))) {item=incident(tenant,candidate.get("id").toString());break;}
        }
        if(item==null) {
            String description,logs="";
            if(metric.name().startsWith("kafka_")){description="Kafka broker disk pressure with consumer lag and order pipeline delay";if(metric.name().contains("disk"))logs="DiskErrorException broker-02";}
            else if(metric.name().startsWith("spark_")){description="Spark executor memory pressure and order stream job delay";if(metric.name().contains("memory"))logs="OutOfMemoryError order_stream_job";}
            else description="fact_orders dataset freshness SLA violation while infrastructure is healthy";
            item=DeterministicInvestigator.analyze(new IncidentRequest(description,logs,obj(metric.name(),metric.value()),"demo",tenant),tenant);
            item.incident_id="INC-"+id().replace("-","").substring(0,8).toUpperCase(Locale.ROOT);item.severity=severity;item.affected_entities=new ArrayList<>(List.of(metric.entity_id()));
            item.recovery_actions.add(new RecoveryAction("ACT-DEMO","Apply allowlisted ReplayLab recovery to simulated telemetry","Restore the simulated signal to its known healthy baseline and verify every symptom","medium",true));item.approval_required=true;
        }else {
            if(!item.affected_entities.contains(metric.entity_id()))item.affected_entities.add(metric.entity_id());
            if(severity.ordinal()<item.severity.ordinal())item.severity=severity;
        }
        item.timeline.add(obj("at",observed,"event",message,"kind","alert"));save(tenant,item);
        String alertId=id();db.insert(Table.ALERTS,tenant,obj("id",alertId,"incident_id",item.incident_id,"rule_id",metric.name(),"entity_id",tenant+":"+metric.entity_id(),"severity",severity,"observed_at",observed,"payload",obj("message",message,"observed_value",metric.value(),"threshold",threshold,"signal_id",id)));
        item.evidence.add(new Evidence("METRIC-"+id.substring(0,8),"telemetry/"+id,"metric",metric.name()+" = "+metric.value()+" "+metric.unit(),1,observed));
        for(var affected:blast(tenant,metric.entity_id()))if(!item.affected_entities.contains(affected.get("id")))item.affected_entities.add(affected.get("id").toString());
        item.engineering_report="Technology="+item.technology+"; evidence_count="+item.evidence.size()+"; affected_entities="+item.affected_entities.size()+"; approval_required=True.";save(tenant,item);
        audit(tenant,"alert_detected",item.incident_id,"system",obj("rule_id",metric.name(),"entity_id",metric.entity_id(),"alert_id",alertId));
        return obj("metric_id",id,"alert",obj("id",alertId,"rule_id",metric.name(),"severity",severity,"message",message),"incident_id",item.incident_id);
    }
    private String parseTime(String value) {try{return OffsetDateTime.parse(value).toInstant().toString();}catch(Exception e){throw new ApiFailure(422,"Invalid observation timestamp");}}
    private List<MetricInput> sequence(String scenario,boolean recovery) {
        var out=new ArrayList<MetricInput>();
        switch(scenario) {
            case "kafka_disk_pressure" -> {
                out.add(metric("broker-02","kafka_disk_percent",recovery?63:97.8,"%"));out.add(metric("orders.events","kafka_isr_percent",recovery?100:78,"%"));
                out.add(metric("orders-consumer","kafka_consumer_lag",recovery?8300:2400000,"records"));out.add(metric("order_stream_job","spark_input_delay_seconds",recovery?4:141,"s"));out.add(metric("fact_orders","dataset_freshness_minutes",recovery?3:37,"min"));
            }
            case "spark_executor_oom" -> {out.add(metric("order_stream_job","spark_executor_memory_percent",recovery?58:98,"%"));out.add(metric("order_stream_job","spark_gc_percent",recovery?7:44,"%"));out.add(metric("order_stream_job","spark_input_delay_seconds",recovery?4:192,"s"));}
            case "data_freshness_failure" -> out.add(metric("fact_orders","dataset_freshness_minutes",recovery?3:49,"min"));
            default -> throw new ApiFailure(404,"Unknown ReplayLab scenario");
        }
        return out;
    }
    private MetricInput metric(String entity,String name,double value,String unit){return new MetricInput(entity,name,value,unit,null,Map.of());}
    public Map<String,Object> replay(String tenant,String scenario) {
        var seq=sequence(scenario,false);
        if(!db.select(Table.REPLAY_RUNS,tenant,"status=?","awaiting_approval").isEmpty())throw new ApiFailure(409,"Resolve or reject the active ReplayLab run before starting another");
        String run=id(),incident=null;var steps=new ArrayList<Map<String,Object>>();
        for(var metric:seq){var detected=ingest(tenant,metric,"simulated");if(detected.get("incident_id")!=null)incident=detected.get("incident_id").toString();steps.add(obj("metric",metric.name(),"value",metric.value(),"alert",detected.get("alert")));}
        String message=switch(scenario){case "kafka_disk_pressure"->"DiskErrorException broker-02: log directory unavailable";case "spark_executor_oom"->"OutOfMemoryError order_stream_job executor lost";default->"fact_orders did not receive expected records";};
        telemetry(tenant,seq.getFirst().entity_id(),"log","fault_log",null,now(),obj("message",message,"mode","simulated"));
        telemetry(tenant,seq.getFirst().entity_id(),"event","fault_injected",null,now(),obj("message","ReplayLab started "+scenario,"mode","simulated"));
        String truth=DemoSeed.scenarios().stream().filter(s->s.get("id").equals(scenario)).findFirst().orElseThrow().get("expected_root_cause").toString();
        db.insert(Table.REPLAY_RUNS,tenant,obj("id",run,"scenario",scenario,"status","awaiting_approval","incident_id",incident,"payload",obj("mode","simulated","steps",steps,"ground_truth",truth)));
        audit(tenant,"replay_started",incident,"system",obj("scenario",scenario,"run_id",run));
        return obj("id",run,"scenario",scenario,"status","awaiting_approval","incident_id",incident,"mode","simulated","steps",steps);
    }
    public Map<String,Object> approval(String tenant,String incidentId,String actionId,ApprovalDecision decision) {
        var item=incident(tenant,incidentId);var action=action(item,actionId);
        if(!action.requires_approval||action.status!=ActionStatus.proposed)throw new ApiFailure(409,"Action is not awaiting approval");
        action.status=decision.decision().equals("approve")?ActionStatus.approved:ActionStatus.rejected;
        if(action.status==ActionStatus.rejected&&actionId.equals("ACT-DEMO")) {
            for(var replay:db.select(Table.REPLAY_RUNS,tenant,"incident_id=?",incidentId))db.update(Table.REPLAY_RUNS,tenant,replay.get("id").toString(),obj("status","rejected"));
            item.status=Lifecycle.closed_unresolved;
        }
        db.insert(Table.APPROVALS,tenant,obj("incident_id",incidentId,"action_id",actionId,"decision",decision.decision(),"actor","demo.senior_sre","reason",Redaction.redact(decision.reason())));
        item.timeline.add(obj("at",now(),"event","Action "+action.status+" by demo.senior_sre","kind","approval"));save(tenant,item);
        audit(tenant,"approval_decision",incidentId,"demo.senior_sre",obj("action_id",actionId,"decision",decision.decision()));
        return obj("incident_id",incidentId,"action_id",actionId,"status",action.status,"execution","requires_separate_request","mode","simulated-demo");
    }
    private RecoveryAction action(Incident incident,String id){return incident.recovery_actions.stream().filter(a->a.id.equals(id)).findFirst().orElseThrow(()->new ApiFailure(404,"Action not found"));}
    public Map<String,Object> verify(String tenant,String incidentId) {
        var replay=db.select(Table.REPLAY_RUNS,tenant,"incident_id=?",incidentId);
        var item=incident(tenant,incidentId);
        if(replay.isEmpty())throw new ApiFailure(409,"No verification adapter is configured for this incident");
        return verify(tenant,item,replay.getFirst().get("scenario").toString());
    }
    private Map<String,Object> verify(String tenant,Incident item,String scenario) {
        var values=new HashMap<String,Double>();for(var metric:latest(tenant))values.put(metric.get("name").toString(),((Number)metric.get("value")).doubleValue());
        var predicates=new LinkedHashMap<String,DoublePredicate>();
        if(scenario.equals("kafka_disk_pressure")){predicates.put("kafka_disk_percent",v->v<80);predicates.put("kafka_isr_percent",v->v>=100);predicates.put("kafka_consumer_lag",v->v<100000);predicates.put("spark_input_delay_seconds",v->v<30);predicates.put("dataset_freshness_minutes",v->v<=5);}
        if(scenario.equals("spark_executor_oom")){predicates.put("spark_executor_memory_percent",v->v<80);predicates.put("spark_gc_percent",v->v<15);predicates.put("spark_input_delay_seconds",v->v<30);}
        if(scenario.equals("data_freshness_failure"))predicates.put("dataset_freshness_minutes",v->v<=5);
        var checks=new LinkedHashMap<String,Object>();predicates.forEach((k,p)->checks.put(k,obj("observed",values.get(k),"passed",values.containsKey(k)&&p.test(values.get(k)))));
        boolean passed=!checks.isEmpty()&&checks.values().stream().allMatch(c->Boolean.TRUE.equals(map(c).get("passed")));
        item.status=passed?Lifecycle.resolved:Lifecycle.investigating;item.timeline.add(obj("at",now(),"event",passed?"Recovery verified":"Recovery verification failed","kind","verification"));save(tenant,item);
        audit(tenant,"verification",item.incident_id,"system",obj("checks",checks,"passed",passed));
        return obj("incident_id",item.incident_id,"passed",passed,"checks",checks,"status",item.status);
    }
    public Map<String,Object> execute(String tenant,String incidentId,String actionId) {
        var item=incident(tenant,incidentId);
        if(!actionId.equals("ACT-DEMO"))throw new ApiFailure(403,"Only the allowlisted ReplayLab recovery can execute");
        var action=item.recovery_actions.stream().filter(a->a.id.equals(actionId)).findFirst().orElse(null);
        if(action==null||action.status!=ActionStatus.approved||db.select(Table.APPROVALS,tenant,"incident_id=? and action_id=? and decision=?",incidentId,actionId,"approve").isEmpty())throw new ApiFailure(403,"Approved action is required");
        var runs=db.select(Table.REPLAY_RUNS,tenant,"incident_id=?",incidentId);if(runs.isEmpty())throw new ApiFailure(409,"Only ReplayLab incidents can use demo recovery");
        var replay=runs.getFirst();String scenario=replay.get("scenario").toString();
        db.insert(Table.EXECUTION_RECORDS,tenant,obj("incident_id",incidentId,"action_id",actionId));
        item.timeline.add(obj("at",now(),"event","Allowlisted demo recovery executed","kind","action"));
        for(var metric:sequence(scenario,true))ingest(tenant,metric,"simulated");
        telemetry(tenant,sequence(scenario,true).getFirst().entity_id(),"event","demo_recovery",null,now(),obj("message","Allowlisted simulated recovery executed for "+scenario,"mode","simulated"));
        var verification=verify(tenant,item,scenario);boolean passed=Boolean.TRUE.equals(verification.get("passed"));
        var payload=new LinkedHashMap<>(map(replay.get("payload")));payload.put("recovery",verification);
        db.update(Table.REPLAY_RUNS,tenant,replay.get("id").toString(),obj("status",passed?"completed":"failed_verification","payload",payload));
        db.insert(Table.EVALUATIONS,tenant,obj("replay_id",replay.get("id"),"score",passed?1.:.5,"payload",obj("detection",true,"correlation",true,"approval_required",true,"verification",passed,"mode","simulated")));
        audit(tenant,"demo_recovery_executed",incidentId,"system",obj("scenario",scenario,"verification_passed",passed));
        action.status=ActionStatus.executed;save(tenant,item);audit(tenant,"action_executed",incidentId,"demo.senior_sre",obj("action_id",actionId,"mode","simulated"));
        return obj("run_id",replay.get("id"),"action","demo_recovery","mode","simulated","verification",verification);
    }
}
