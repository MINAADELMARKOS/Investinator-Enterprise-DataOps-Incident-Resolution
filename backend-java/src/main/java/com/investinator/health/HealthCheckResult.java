package com.investinator.health;
import java.time.Instant;
import java.util.*;
public record HealthCheckResult(String check_id,String service,String capability,Status status,
    SourceMode source_mode,String evidence_source,String checked_at,long duration_ms,String diagnosis,
    String next_safe_action,String error_type,boolean required_for_readiness,Map<String,Object> details) {
    public enum Status { HEALTHY,DEGRADED,UNHEALTHY,UNKNOWN,NOT_CONFIGURED,NOT_TESTED }
    public enum SourceMode { LIVE,SIMULATED,CACHED,NOT_CONFIGURED,NOT_TESTED }
    public HealthCheckResult {error_type=error_type==null?"":error_type;details=Collections.unmodifiableMap(new LinkedHashMap<>(details));}
    public static HealthCheckResult of(String service,String capability,Status status,SourceMode mode,String source,String diagnosis,String next,String error,boolean required,Map<String,Object> details) {
        return new HealthCheckResult(UUID.randomUUID().toString(),service,capability,status,mode,source,Instant.now().toString(),0,diagnosis,next,error,required,details);
    }
    public HealthCheckResult timed(long duration,int attempts) {
        var values=new LinkedHashMap<>(details);values.put("attempts",attempts);
        return new HealthCheckResult(check_id,service,capability,status,source_mode,evidence_source,checked_at,duration,diagnosis,next_safe_action,error_type,required_for_readiness,values);
    }
    public boolean failed(){return Set.of(Status.DEGRADED,Status.UNHEALTHY,Status.UNKNOWN).contains(status);}
}
