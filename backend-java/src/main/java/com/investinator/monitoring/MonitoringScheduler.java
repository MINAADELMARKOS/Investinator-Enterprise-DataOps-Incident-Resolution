package com.investinator.monitoring;
import static com.investinator.domain.Json.*;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.*;
import com.investinator.health.HealthAggregator;
@Component @EnableScheduling @ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class MonitoringScheduler {
    private final MonitoringService service;private final HealthAggregator health;private final Environment env;
    public MonitoringScheduler(MonitoringService service,HealthAggregator health,Environment env){this.service=service;this.health=health;this.env=env;long tick=env.getProperty("investinator.monitor.tick-ms",Long.class,5000L),sample=env.getProperty("investinator.monitor.health-ms",Long.class,60000L);if(tick<1000||tick>3600000||sample<10000||sample>86400000)throw new IllegalArgumentException("Monitoring cadence outside safe bounds");}
    @Scheduled(fixedDelayString="${investinator.monitor.tick-ms:5000}") public void poll(){if(!service.enabled())return;String tenant=env.getProperty("investinator.demo-tenant","demo");if((Boolean)service.status(tenant).get("paused"))return;try{service.tick(tenant);}catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Monitoring tick failed; durable checkpoint unchanged: {}",e.getClass().getSimpleName());}}
    @Scheduled(fixedDelayString="${investinator.monitor.health-ms:60000}") public void sample(){if(!service.enabled())return;String tenant=env.getProperty("investinator.demo-tenant","demo");if((Boolean)service.status(tenant).get("paused"))return;try{var report=health.refresh();service.db.write(tenant,()->{service.db.jdbc.update("insert into monitor_health_samples(tenant_id,payload) values(?,?::jsonb)",tenant,encode(report));return null;});}catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Health sampling unavailable: {}",e.getClass().getSimpleName());}}
}
