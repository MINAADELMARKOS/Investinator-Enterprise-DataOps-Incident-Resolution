package com.investinator.persistence;
import java.util.*;
public enum Table {
AI_USAGE("ai_usage", "id,tenant_id,incident_id,task,model,latency_ms,input_tokens,output_tokens,status,request_count,created_at"),
APPROVALS("approvals", "id,tenant_id,incident_id,action_id,decision,actor,reason,created_at"),
AUDIT_EVENTS("audit_events", "id,tenant_id,incident_id,actor,event,payload,created_at"),
CONVERSATIONS("conversations", "id,tenant_id,incident_id,created_at"),
ENTITIES("entities", "id,tenant_id,kind,name,criticality,payload"),
EVALUATIONS("evaluations", "id,tenant_id,replay_id,score,payload,created_at"),
INCIDENTS("incidents", "id,tenant_id,environment,status,severity,technology,payload,created_at,updated_at"),
KNOWLEDGE_DOCUMENTS("knowledge_documents", "id,tenant_id,title,technology,content,source,created_at"),
RELATIONSHIPS("relationships", "id,tenant_id,source_id,target_id,kind"),
REPLAY_RUNS("replay_runs", "id,tenant_id,scenario,incident_id,status,payload,created_at"),
TELEMETRY("telemetry", "id,tenant_id,entity_id,signal_type,name,numeric_value,observed_at,payload"),
ALERTS("alerts", "id,tenant_id,incident_id,rule_id,entity_id,severity,observed_at,payload"),
MESSAGES("messages", "id,conversation_id,tenant_id,role,content,payload,created_at"),
REQUEST_KEYS("request_keys", "tenant_id,route,key,request_hash,response,created_at"),
EXECUTION_RECORDS("execution_records", "tenant_id,incident_id,action_id,completed_at");
    public final String sql;
    public final Set<String> columns;
    Table(String sql,String columns) {this.sql=sql;this.columns=Set.of(columns.split(","));}
}
