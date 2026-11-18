package com.investinator.health;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
@Component
public class HealthSettings {
    public final int parallelism,queueSize,timeoutMs,deadlineMs,retries,failureThreshold,cooldownMs,intervalMs,freshnessSeconds,maxTopics,maxGroups,maxPartitions,maxLag;
    public final boolean scheduled;
    public HealthSettings(Environment env) {
        parallelism=value(env,"parallelism",4,1,16);queueSize=value(env,"queue-size",64,1,256);
        timeoutMs=value(env,"timeout-ms",6000,50,30000);deadlineMs=value(env,"deadline-ms",12000,100,60000);
        retries=value(env,"retries",1,0,2);failureThreshold=value(env,"failure-threshold",3,1,20);
        cooldownMs=value(env,"cooldown-ms",10000,10,120000);intervalMs=value(env,"interval-ms",20000,1000,300000);
        freshnessSeconds=value(env,"freshness-seconds",86400,1,604800);maxTopics=value(env,"max-topics",50,1,200);
        maxGroups=value(env,"max-groups",20,1,100);maxPartitions=value(env,"max-partitions",200,1,1000);
        maxLag=value(env,"max-consumer-lag",10000,0,1000000000);scheduled=env.getProperty("investinator.health.scheduled",Boolean.class,false);
    }
    private int value(Environment env,String key,int fallback,int min,int max){int value=env.getProperty("investinator.health."+key,Integer.class,fallback);if(value<min||value>max)throw new IllegalArgumentException("Invalid health setting: "+key);return value;}
}
