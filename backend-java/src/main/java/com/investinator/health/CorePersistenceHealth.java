package com.investinator.health;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component("corePersistence")
public class CorePersistenceHealth implements HealthIndicator {
    private final ObjectProvider<JdbcTemplate> jdbc;
    private final boolean enabled;
    public CorePersistenceHealth(ObjectProvider<JdbcTemplate> jdbc,@Value("${investinator.persistence.enabled:false}") boolean enabled){this.jdbc=jdbc;this.enabled=enabled;}
    @Override public Health health() {
        if(!enabled)return Health.up().withDetail("persistence","NOT_CONFIGURED").build();
        try {
            Integer one=jdbc.getObject().queryForObject("select 1",Integer.class);
            Integer versions=jdbc.getObject().queryForObject("select count(*) from flyway_schema_history where success=true",Integer.class);
            return Integer.valueOf(1).equals(one)&&versions!=null&&versions>=2?Health.up().build():Health.down().build();
        }catch(Exception ignored){return Health.down().build();}
    }
}
