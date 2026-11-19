package com.investinator.api;
import com.investinator.chat.ConversationReadService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@RestController @RequestMapping("/api/v1/investigator/conversations") @ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class ConversationController {
    private final ConversationReadService service;private final String tenant;
    public ConversationController(ConversationReadService service,@Value("${investinator.demo-tenant:demo}")String tenant){this.service=service;this.tenant=tenant;}
    @GetMapping public Object list(){return service.list(tenant);}
    @GetMapping("/{id}/evidence/{evidence}") public Object inspect(@PathVariable String id,@PathVariable String evidence){return service.inspect(tenant,id,evidence);}
}
