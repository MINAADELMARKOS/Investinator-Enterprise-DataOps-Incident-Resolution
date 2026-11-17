package com.investinator.health;
public interface HealthProbe {
    String service();
    String capability();
    default boolean requiredForReadiness(){return false;}
    HealthCheckResult check() throws Exception;
}
