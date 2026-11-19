package com.investinator.monitoring;
import java.time.*;
import java.util.*;
import com.investinator.security.Redaction;
public final class MonitoringContracts {
    private MonitoringContracts(){}
    public enum Lifecycle {NEW,TRIAGED,INVESTIGATING,MITIGATING,AWAITING_APPROVAL,VERIFYING,RESOLVED,CLOSED_UNRESOLVED}
    public record Impact(Integer datasets,Integer customers,Integer criticality,Double slo_minutes) {
        public Impact{if(datasets!=null&&(datasets<0||datasets>1000000)||customers!=null&&(customers<0||customers>100000000)||criticality!=null&&(criticality<1||criticality>5)||slo_minutes!=null&&(!Double.isFinite(slo_minutes)||slo_minutes<0))throw new IllegalArgumentException("Invalid measured impact");}
    }
    public record SignalReceived(String event_id,String tenant_id,String environment,String cluster_id,String graph_id,int graph_revision,String resource,String rule_id,String observed_at,String source_mode,Map<String,Double> measurements,Set<String> capabilities,String excerpt,Impact impact) {
        public SignalReceived {
            for(String value:Arrays.asList(event_id,tenant_id,environment,cluster_id,graph_id,resource,rule_id))if(value==null||!value.matches("[A-Za-z0-9_.:-]{1,100}"))throw new IllegalArgumentException("Invalid event scope");
            if(graph_revision<1||!Set.of("SIMULATED","LIVE").contains(source_mode))throw new IllegalArgumentException("Invalid source mode");Instant.parse(observed_at);
            measurements=Map.copyOf(measurements);capabilities=Set.copyOf(capabilities);if(measurements.size()>30||capabilities.size()>30||measurements.values().stream().anyMatch(v->!Double.isFinite(v)))throw new IllegalArgumentException("Invalid measured signals");
            excerpt=Redaction.redact(excerpt==null?"":excerpt);if(excerpt.length()>2000)throw new IllegalArgumentException("Bounded excerpt required");
        }
    }
    public record NormalizedObservation(String evidence_id,String scope_key,String source_mode,String observed_at,String signal,String availability){}
    public record DetectionCandidate(String resource,String rule_id,boolean triggered,String reason){}
    public record CorrelationWindow(String graph_id,int revision,String event_time,int seconds){}
    public record IncidentAggregate(String incident_id,String fingerprint,Lifecycle lifecycle,String source_mode){}
    public record InvestigationRequested(String incident_id,String idempotency_key,int max_turns){}
    public record NotificationCandidate(String incident_id,String destination,String idempotency_key){}
}
