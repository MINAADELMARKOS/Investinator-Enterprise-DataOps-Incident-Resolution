package com.investinator.domain;
import java.util.*;
import jakarta.validation.constraints.*;
public final class Contracts {
    private Contracts() {}
    public enum Technology { HDFS, Spark, Kafka, Kudu, NiFi, API, Data, Unknown }
    public enum Severity { P1, P2, P3, P4 }
    public enum Lifecycle { investigating, resolved, closed_unresolved }
    public enum ActionStatus { proposed, approved, rejected, executed }
    public record Evidence(String id,String source,String kind,String snippet,double score,String observed_at) {}
    public static class RecoveryAction {
        public String id,action,rationale,risk="low";
        public boolean dangerous,requires_approval;
        public ActionStatus status=ActionStatus.proposed;
        public RecoveryAction() {}
        public RecoveryAction(String id,String action,String rationale,String risk,boolean approval) {
            this.id=id;this.action=action;this.rationale=rationale;this.risk=risk;this.requires_approval=approval;this.dangerous=approval;
        }
    }
    public static class Incident {
        public String incident_id,environment="demo",tenant_id="demo",engineering_report,executive_summary;
        public Lifecycle status=Lifecycle.investigating;
        public Technology technology=Technology.Unknown;
        public Severity severity=Severity.P3;
        public double confidence;
        public boolean approval_required;
        public List<String> likely_root_causes=new ArrayList<>(),affected_entities=new ArrayList<>();
        public List<Evidence> evidence=new ArrayList<>();
        public List<RecoveryAction> recovery_actions=new ArrayList<>();
        public List<Map<String,Object>> timeline=new ArrayList<>();
    }
    public record IncidentRequest(@NotNull @Size(min=1,max=10000) String description,
        @Size(max=100000) String logs, Map<String,Object> metrics,
        @Size(max=100) String environment, @Size(max=100) String tenant_id) {
        public IncidentRequest {
            description=description==null?null:description.strip();logs=logs==null?"":logs.strip();
            metrics=metrics==null?new LinkedHashMap<>():new LinkedHashMap<>(metrics);
            environment=environment==null?"demo":environment.strip();tenant_id=tenant_id==null?"demo":tenant_id.strip();
        }
    }
    public record MetricInput(@NotNull @Size(min=1,max=200) String entity_id,
        @NotNull @Size(min=1,max=150) String name,@NotNull Double value,
        @Size(max=30) String unit,String observed_at,Map<String,String> labels) {
        public MetricInput {unit=unit==null?"":unit;labels=labels==null?Map.of():labels;}
    }
    public record ApprovalDecision(@NotNull @Pattern(regexp="approve|reject") String decision,
        @NotNull @Size(min=1,max=200) String decided_by,@Size(max=2000) String reason) {
        public ApprovalDecision {reason=reason==null?"":reason;}
    }
    public record VerificationRequest(@NotNull Map<String,Object> checks,@Size(max=200) String verified_by) {
        public VerificationRequest {verified_by=verified_by==null?"system":verified_by;}
    }
    public record ReplayStart(@NotNull String scenario) {}
    public record ChatMessage(String conversation_id,String incident_id,
        @Pattern(regexp="ask|investigate|fix") String mode,@NotNull @Size(min=1,max=4000) String message) {
        public ChatMessage {mode=mode==null?"investigate":mode;}
    }
    public record Observation(String tenant_id,String source_mode,String observed_at,String entity_id,Map<String,Object> values) {}
    public record Hypothesis(String title,List<String> evidence_ids,List<String> contradictions) {}
    public record RootCause(String confirmed_by,String confirmed_at,Hypothesis hypothesis) {}
    public record RemediationPlan(String action_id,String target,String environment,boolean simulation_only) {}
    public record Approval(String tenant_id,String incident_id,String action_id,String actor,String decision,String reason) {}
    public record Verification(String incident_id,boolean passed,Map<String,Object> checks) {}
    public record Alert(String id,String incident_id,String rule_id,String entity_id,String severity,String observed_at,Map<String,Object> payload) {}
    public record Entity(String id,String name,String type,int criticality) {}
    public record Relationship(String from,String to,String relationship) {}
    public record Knowledge(String id,String title,String technology,String source,String content) {}
    public record Conversation(String id,String incident_id,List<Map<String,Object>> messages) {}
    public record AIUsage(String model,String task,int latency_ms,int input_tokens,int output_tokens,String status) {}
    public record Audit(String id,String event,String actor,String incident_id,String created_at,Map<String,Object> details) {}
    public record Evaluation(String id,String replay_id,double score,String created_at,Map<String,Object> payload) {}
}
