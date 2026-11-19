package com.investinator.monitoring;
import com.investinator.domain.Json;
import com.investinator.correlation.CorrelationEngine.Signal;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class DetectionRules {
    public record Rule(String id,Signal signal,String mode,Map<String,Double> thresholds){}
    public enum Verdict {ABNORMAL,HEALTHY,UNKNOWN,INCOMPATIBLE}
    private final Map<String,Object> document;private final Map<String,Rule> rules=new LinkedHashMap<>();
    public DetectionRules(){try(var in=DetectionRules.class.getResourceAsStream("/incident-detection-rules.yaml")){var yaml=new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new org.yaml.snakeyaml.LoaderOptions()));document=Json.map(yaml.load(in));for(var row:Json.rows(document.get("rules"))){var limits=new LinkedHashMap<String,Double>();Json.map(row.get("thresholds")).forEach((k,v)->limits.put(k,((Number)v).doubleValue()));var rule=new Rule(row.get("id").toString(),Signal.valueOf(row.get("signal").toString()),row.get("mode").toString(),Map.copyOf(limits));rules.put(rule.id(),rule);}}catch(Exception e){throw new IllegalStateException("Invalid bundled detection rules",e);}}
    public Rule rule(String id){var r=rules.get(id);if(r==null)throw new IllegalArgumentException("Unknown rule");return r;}
    public Verdict evaluate(MonitoringContracts.SignalReceived event,String mode){var rule=rule(event.rule_id());if(!rule.mode().equals("ANY")&&!rule.mode().equals(mode))return Verdict.INCOMPATIBLE;if(!event.capabilities().containsAll(rule.thresholds().keySet())||!event.measurements().keySet().containsAll(rule.thresholds().keySet()))return Verdict.UNKNOWN;return rule.thresholds().entrySet().stream().allMatch(e->event.measurements().get(e.getKey())>e.getValue())?Verdict.ABNORMAL:Verdict.HEALTHY;}
    public int hysteresis(){return ((Number)document.get("hysteresis_samples")).intValue();}
    public int windowSeconds(){return ((Number)document.get("window_seconds")).intValue();}
    public int cooldownSeconds(){return ((Number)document.get("cooldown_seconds")).intValue();}
    public Map<String,Object> document(){return document;}
}
