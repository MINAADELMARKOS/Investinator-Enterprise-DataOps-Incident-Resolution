package com.investinator.agent;
import com.investinator.domain.Contracts.*;
import com.investinator.workflows.DemoSeed;
import com.investinator.security.Redaction;
import static com.investinator.domain.Json.*;
import java.util.*;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
public final class DeterministicInvestigator {
    private DeterministicInvestigator() {}
    public static String digest(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static String py(Object value) {
        if(value instanceof Boolean b)return b?"True":"False";
        return Objects.toString(value,"None");
    }
    private static Set<String> terms(String text) {
        var set=new HashSet<String>();var m=Pattern.compile("[a-z0-9_]+").matcher(text.toLowerCase(Locale.ROOT));
        while(m.find())set.add(m.group());return set;
    }
    public static Incident analyze(IncidentRequest request,String tenant) {
        String description=Redaction.redact(request.description()),logs=Redaction.redact(request.logs());
        var metrics=new LinkedHashMap<String,String>();request.metrics().forEach((k,v)->metrics.put(k,Redaction.redact(py(v))));
        String metricText="{"+String.join(", ",metrics.entrySet().stream().map(e->"'"+e.getKey()+"': '"+e.getValue()+"'").toList())+"}";
        String text=description+"\n"+logs+"\n"+metricText,lower=text.toLowerCase(Locale.ROOT);
        Technology technology=Technology.Unknown;int best=0;
        for(var entry:DemoSeed.mapping("keywords").entrySet()) {
            int score=(int)((List<?>)entry.getValue()).stream().filter(v->lower.contains(v.toString())).count();
            if(score>best){best=score;technology=Technology.valueOf(entry.getKey());}
        }
        var incident=new Incident();incident.technology=technology;incident.tenant_id=tenant;incident.environment=request.environment();
        incident.incident_id="INC-"+digest(tenant+":"+text).substring(0,8).toUpperCase(Locale.ROOT);
        incident.confidence=best>0?Math.min(.99,.55+best*.12):.2;
        var findings=new TreeSet<String>();var matcher=Pattern.compile("([A-Z][A-Za-z0-9]*(?:Exception|Error)|safe mode|backpressure|timeout|outofmemory)",Pattern.CASE_INSENSITIVE).matcher(logs);
        while(matcher.find())findings.add(matcher.group());
        var topFindings=findings.stream().limit(10).toList();
        var terms=terms(text);terms.removeIf(t->t.length()<4);
        int index=0;
        for(var runbook:DemoSeed.tuples("runbooks")) {
            index++;String tech=runbook.get(0).toString(),source=runbook.get(1).toString(),snippet=runbook.get(2).toString();
            var candidate=terms(snippet+" "+source);candidate.retainAll(terms);
            if(technology.name().equals(tech)||(technology==Technology.Unknown&&candidate.size()>=2))
                incident.evidence.add(new Evidence("KB-%03d".formatted(index),source,"runbook",snippet,
                    Math.min(.99,((Number)runbook.get(3)).doubleValue()+candidate.size()*.01),null));
        }
        incident.evidence.sort(Comparator.comparingDouble(Evidence::score).reversed());
        if(incident.evidence.size()>5)incident.evidence=new ArrayList<>(incident.evidence.subList(0,5));
        index=0;String at=now();
        for(var entry:metrics.entrySet())incident.evidence.add(new Evidence("METRIC-%03d".formatted(++index),"request.metrics."+entry.getKey(),"metric","Observed "+entry.getKey()+" = "+entry.getValue(),1,at));
        index=0;for(String finding:topFindings)incident.evidence.add(new Evidence("LOG-%03d".formatted(++index),"request.logs","log","Log signature observed: "+finding,.95,at));
        incident.recovery_actions.add(action("ACT-001","Collect current health status using read-only checks","Establish current blast radius before changing state","low"));
        switch(technology) {
            case Kafka -> {
                incident.recovery_actions.add(action("ACT-002","Check broker disk usage and offline log directories","Validate the storage hypothesis","low"));
                incident.recovery_actions.add(action("ACT-003","Review consumer lag, ISR, and affected topic health","Measure downstream impact","low"));
                incident.recovery_actions.add(action("ACT-004","Restart affected Kafka broker after human approval","Restart may rebalance partitions and affect clients","high"));
            }
            case Spark -> incident.recovery_actions.add(action("ACT-002","Inspect failed stages, executor memory, and GC metrics","Confirm executor pressure before changing job settings","low"));
            case HDFS -> incident.recovery_actions.add(action("ACT-002","Verify NameNode safe mode and under-replicated block counts","Write failures often follow safe mode or replication issues","low"));
            case Unknown -> {}
            default -> incident.recovery_actions.add(action("ACT-002","Follow "+technology+" runbook validation steps","Use technology-specific read-only checks","low"));
        }
        incident.likely_root_causes.add(incident.confidence>=.5&&!incident.evidence.isEmpty()?incident.evidence.getFirst().snippet():"insufficient_evidence");
        incident.approval_required=incident.recovery_actions.stream().anyMatch(a->a.requires_approval);
        boolean critical=List.of("outage","unavailable","production down","critical").stream().anyMatch(lower::contains);
        if(technology==Technology.Kafka)critical|=request.metrics().entrySet().stream().anyMatch(e->e.getKey().toLowerCase(Locale.ROOT).contains("disk")&&e.getValue() instanceof Number n&&n.doubleValue()>=90);
        incident.severity=critical?Severity.P1:List.of("lag","delay","stale","backpressure","error").stream().anyMatch(lower::contains)?Severity.P2:technology==Technology.Unknown?Severity.P4:Severity.P3;
        matcher=Pattern.compile("\\b(?:broker[-_ ]?\\d+|[a-z][a-z0-9_.-]*(?:topic|job|table|dataset))\\b",Pattern.CASE_INSENSITIVE).matcher(text);
        var seen=new HashSet<String>();while(matcher.find())if(seen.add(matcher.group().toLowerCase(Locale.ROOT)))incident.affected_entities.add(matcher.group());
        incident.timeline.add(obj("at",at,"event","Investigation started","kind","system"));
        for(var item:incident.evidence)incident.timeline.add(obj("at",at,"event","Evidence gathered: "+item.id(),"kind","evidence","evidence_id",item.id()));
        incident.timeline.add(obj("at",at,"event","Root cause hypothesis generated","kind","hypothesis"));
        incident.executive_summary=incident.severity+" "+technology+" incident; evidence-backed investigation is "+(incident.evidence.isEmpty()?"inconclusive":"ready for review")+". "+(incident.approval_required?"A high-risk action requires approval.":"Only read-only next steps are proposed.");
        String findingsText="["+String.join(", ",(topFindings.isEmpty()?List.of("none"):topFindings).stream().map(s->"'"+s+"'").toList())+"]";
        incident.engineering_report="Technology="+technology+"; findings="+findingsText+"; evidence_count="+incident.evidence.size()+"; approval_required="+(incident.approval_required?"True":"False")+".";
        return incident;
    }
    private static RecoveryAction action(String id,String text,String reason,String risk) {
        boolean approval=!risk.equals("low")||List.of("restart","delete","drop","truncate","chmod","chown","change config","send message").stream().anyMatch(text.toLowerCase(Locale.ROOT)::contains);
        return new RecoveryAction(id,text,reason,risk,approval);
    }
}
