package com.investinator.evidence;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.investinator.security.Redaction;
public record NormalizedEvidence(String evidence_id,String tenant_id,String environment,SourceMode source_mode,
    Service service,Host host,String resource_id,String cluster_id,Log log,Event event,
    @JsonProperty("@timestamp") String timestamp,String ingested_at,String trace_id,String span_id,
    String incident_id,String technology,String operation,Error error,String sensitivity,
    long ingestion_lag_ms,String source_url,String record_pointer,List<String> quality_flags) {
    public enum SourceMode { LIVE,SIMULATED,CACHED,NOT_CONFIGURED,NOT_TESTED }
    public record Service(String name,String version) {}
    public record Host(String name) {}
    public record Log(String level) {}
    public record Event(String original) {}
    public record Error(String signature,String category) {}
    public static NormalizedEvidence normalize(String tenant,String sourceId,Service service,String message,
            Instant observed,Instant ingested,String trace,String span) {
        if(tenant==null||!tenant.matches("[A-Za-z0-9_-]{1,100}"))throw new IllegalArgumentException("Invalid evidence tenant");
        if(sourceId==null||sourceId.isBlank()||sourceId.length()>512)throw new IllegalArgumentException("Invalid source pointer");
        if(service==null||service.name()==null||!service.name().matches("[A-Za-z0-9._-]{1,128}"))throw new IllegalArgumentException("Invalid service");
        if(message==null||message.length()>65536)throw new IllegalArgumentException("Oversized evidence");
        if(trace!=null&&!trace.matches("[a-f0-9]{32}"))throw new IllegalArgumentException("Invalid trace ID");
        if(span!=null&&!span.matches("[a-f0-9]{16}"))throw new IllegalArgumentException("Invalid span ID");
        Objects.requireNonNull(observed);Objects.requireNonNull(ingested);
        long lag=Duration.between(observed,ingested).toMillis();var flags=new ArrayList<String>();
        if(lag < -300000)flags.add("FUTURE_CLOCK_SKEW");if(lag>900000)flags.add("LATE_ARRIVAL");
        String safe=Redaction.redact(message);if(safe.length()>8192){safe=safe.substring(0,8192);flags.add("TRUNCATED");}
        String id;
        try{id="EV-"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((tenant+"\n"+sourceId+"\n"+observed).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
        return new NormalizedEvidence(id,tenant,"local-demo",SourceMode.SIMULATED,service,new Host("fixture-host"),service.name(),"orders-demo",new Log("ERROR"),new Event(safe),observed.toString(),ingested.toString(),trace,span,null,"Kafka","diagnostic-observation",new Error("fixture-failure","dependency"),safe.equals(message)?"INTERNAL":"REDACTED",Math.max(0,lag),null,sourceId,List.copyOf(flags));
    }
    public boolean validFor(String tenant,String hitId) {
        if(!Objects.equals(tenant_id,tenant)||!Objects.equals(evidence_id,hitId)||source_mode==null||event==null||service==null||record_pointer==null)return false;
        try{Instant.parse(timestamp);Instant.parse(ingested_at);}catch(Exception e){return false;}
        return evidence_id!=null&&evidence_id.matches("EV-[a-f0-9]{64}");
    }
}
