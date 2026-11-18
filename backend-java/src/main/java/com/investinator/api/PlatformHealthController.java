package com.investinator.api;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="false",matchIfMissing=true)
public class PlatformHealthController {
    public record OverallHealth(Integer score) {}
    public record PlatformHealth(String product, String mode, String status, String source_mode,
        OverallHealth overall_health, List<Object> services, List<Object> active_incidents,
        Integer active_p1, Integer active_p2, Integer systems_degraded, Integer datasets_delayed,
        Map<String,Object> business_impact, Map<String,Object> operations) {}
    @GetMapping("/api/v1/platform/health")
    public PlatformHealth health() {
        return new PlatformHealth("InvestiNator", "java-scaffold", "NOT_IMPLEMENTED", "NOT_TESTED",
            new OverallHealth(null), List.of(), List.of(), null, null, null, null, Map.of(), Map.of());
    }
}
