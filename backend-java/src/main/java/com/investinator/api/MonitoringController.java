package com.investinator.api;
import com.investinator.monitoring.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import static com.investinator.domain.Json.*;
@RestController @RequestMapping("/api/v1/monitor") @ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class MonitoringController {
    private final MonitoringService service;private final DetectionRules rules;private final String tenant;
    public MonitoringController(MonitoringService s,DetectionRules r,@Value("${investinator.demo-tenant:demo}")String t){service=s;rules=r;tenant=t;}
    @GetMapping("/status") public Object status(){return service.status(tenant);}
    @GetMapping("/rules") public Object rules(){return rules.document();}
    @PostMapping("/settings") public Object settings(@RequestBody MonitoringService.Settings input){return service.configure(tenant,input);}
    @PostMapping("/pause") public Object pause(){return service.control(tenant,true);}
    @PostMapping("/resume") public Object resume(){return service.control(tenant,false);}
    @PostMapping("/events") public Object event(@RequestBody MonitoringContracts.SignalReceived e){return service.accept(tenant,e);}
    @PostMapping("/dry-run") public Object dry(@RequestBody MonitoringContracts.SignalReceived e){if(!tenant.equals(e.tenant_id())||!"SIMULATED".equals(e.source_mode()))throw new ApiFailure(403,"Fixture scope denied");return service.dryRun(e);}
    @PostMapping("/tick") public Object tick(){return obj("processed",service.tick(tenant));}
    @GetMapping("/incidents/{id}") public Object incident(@PathVariable String id){return service.incident(tenant,id);}
    public record Transition(MonitoringContracts.Lifecycle target){}
    @PostMapping("/incidents/{id}/transition") public Object transition(@PathVariable String id,@RequestBody Transition input){if(input.target()==MonitoringContracts.Lifecycle.RESOLVED||input.target()==MonitoringContracts.Lifecycle.VERIFYING)throw new ApiFailure(403,"Independent verification boundary required");return service.transition(tenant,id,input.target(),false);}
}
