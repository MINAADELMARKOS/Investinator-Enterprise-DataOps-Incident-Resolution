package com.investinator.api;
import com.investinator.health.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class IntegrationHealthController {
    private final HealthAggregator health;
    public IntegrationHealthController(HealthAggregator health){this.health=health;}
    @GetMapping("/api/v1/health/integrations")
    public HealthAggregator.Report report(@RequestParam(defaultValue="true") boolean refresh,@RequestParam(required=false) String service) {
        var report=refresh?health.refresh():health.cached();
        if(service==null)return report;
        var checks=report.checks().stream().filter(c->c.service().equals(service)).toList();
        if(checks.isEmpty())throw new ApiFailure(404,"Unknown diagnostic service");
        return new HealthAggregator.Report(report.report_id(),report.checked_at(),report.delivery_mode(),checks.stream().allMatch(c->c.status()==HealthCheckResult.Status.HEALTHY),report.partial(),checks);
    }
}
