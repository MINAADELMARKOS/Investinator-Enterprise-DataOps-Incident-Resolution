package com.investinator.api;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.investinator.remediation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/remediation") @ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class RemediationController {
    private final RemediationPolicy policy;private final String tenant;
    public RemediationController(RemediationPolicy p,@Value("${investinator.demo-tenant:demo}")String tenant){policy=p;this.tenant=tenant;}
    public record Approval(String plan_hash,String decision){}
    public record Review(String plan_hash){}
    @GetMapping("/registry") public Object registry(){return List.of(ActionRegistry.RECOVER,ActionRegistry.RESTORE);}
    @PostMapping("/plans") public Object propose(@RequestBody RemediationPolicy.Proposal input){return policy.propose(tenant,input,true);}
    @PostMapping("/dry-run") public Object dry(@RequestBody RemediationPolicy.Proposal input){return policy.propose(tenant,input,false);}
    @GetMapping("/plans/{id}") public Object get(@PathVariable String id){return policy.get(tenant,id);}
    @GetMapping("/plans/{id}/postmortem") public Object postmortem(@PathVariable String id){return policy.postmortem(tenant,id);}
    @PostMapping("/plans/{id}/approval") public Object approve(@PathVariable String id,@RequestBody Approval body){return policy.approve(tenant,id,body.plan_hash(),body.decision());}
    @PostMapping("/plans/{id}/execute") public Object execute(@PathVariable String id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody Review body){return policy.execute(tenant,id,body.plan_hash(),key);}
    @PostMapping("/plans/{id}/sample") public Object sample(@PathVariable String id){return policy.sample(tenant,id);}
    @PostMapping("/plans/{id}/verify") public Object verify(@PathVariable String id,@RequestBody Review body){return policy.verify(tenant,id,body.plan_hash());}
    @PostMapping("/plans/{id}/recover") public Object recover(@PathVariable String id){return policy.recover(tenant,id);}
}
