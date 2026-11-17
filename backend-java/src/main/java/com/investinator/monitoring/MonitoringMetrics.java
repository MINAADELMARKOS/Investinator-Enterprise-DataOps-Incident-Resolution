package com.investinator.monitoring;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.core.instrument.*;
@Component @ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class MonitoringMetrics {
    public MonitoringMetrics(MonitoringService service,MeterRegistry registry,@Value("${investinator.demo-tenant:demo}")String tenant){
        for(String state:java.util.List.of("QUEUED","PROCESSED","UNKNOWN","COOLDOWN","HYSTERESIS","STALE_OR_FUTURE","INCIDENT_BUDGET"))Gauge.builder("investinator.monitor.events",service,s->{try{return s.db.jdbc.queryForObject("select count(*) from monitor_events where tenant_id=? and state=?",Double.class,tenant,state);}catch(Exception e){return Double.NaN;}}).tag("state",state).register(registry);
        Gauge.builder("investinator.monitor.active",service,s->{try{return s.db.jdbc.queryForObject("select count(*) from monitor_incidents where tenant_id=? and active",Double.class,tenant);}catch(Exception e){return Double.NaN;}}).register(registry);
        Gauge.builder("investinator.monitor.health.age.seconds",service,s->{try{Double value=s.db.jdbc.queryForObject("select extract(epoch from (now()-max(observed_at))) from monitor_health_samples where tenant_id=?",Double.class,tenant);return value==null?Double.NaN:value;}catch(Exception e){return Double.NaN;}}).register(registry);
    }
}
