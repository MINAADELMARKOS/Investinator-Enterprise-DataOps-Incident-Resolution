package com.investinator.api;
import com.investinator.knowledge.*;
import static com.investinator.domain.Json.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
@RestController @RequestMapping("/api/v1/knowledge")
@ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class KnowledgeController {
    private final KnowledgeService service;private final String tenant,principal;
    public KnowledgeController(KnowledgeService service,@Value("${investinator.demo-tenant:demo}") String tenant,@Value("${KNOWLEDGE_READER_PRINCIPAL:}") String principal){this.service=service;this.tenant=tenant;this.principal=principal.isBlank()?tenant+".senior_sre":principal;if(!this.principal.startsWith(tenant+"."))throw new IllegalArgumentException("Knowledge principal must be tenant bound");}
    public record Review(String decision,String reason) {}
    @PostMapping("/revisions") public Object ingest(@RequestBody KnowledgeDocument document){return service.ingest(tenant,tenant+".knowledge_ingestor",document);}
    @GetMapping("/revisions/{id}/{revision}") public Object draft(@PathVariable String id,@PathVariable int revision){return service.draft(tenant,id,revision);}
    @PostMapping("/revisions/{id}/{revision}/review") public Object review(@PathVariable String id,@PathVariable int revision,@RequestBody Review review){return service.review(tenant,tenant+".knowledge_reviewer",id,revision,review.decision(),review.reason());}
    @PostMapping("/revisions/{id}/{revision}/publish") public Object publish(@PathVariable String id,@PathVariable int revision){return service.publish(tenant,tenant+".knowledge_publisher",id,revision);}
    @GetMapping("/search") public Object search(@RequestParam String q,@RequestParam String incident_id,HttpServletRequest request){if(!Set.of("q","incident_id").containsAll(request.getParameterMap().keySet()))throw new ApiFailure(422,"Knowledge search scope is bound on the server");return service.search(tenant,principal,q,service.scope(tenant,incident_id));}
    @GetMapping("/documents/{id}/{revision}") public Object readable(@PathVariable String id,@PathVariable int revision){return service.readableDocument(tenant,principal,id,revision);}
    @PostMapping("/feedback") public Object feedback(@RequestBody KnowledgeService.Outcome input){return obj("id",service.feedback(tenant,tenant+".operator",input),"review_state","PENDING_REVIEW","automatic_knowledge_update",false);}
    @GetMapping("/feedback/{incident}") public Object outcomes(@PathVariable String incident){return service.outcomes(tenant,incident);}
    @PostMapping("/feedback/{id}/review") public Object outcomeReview(@PathVariable String id,@RequestBody Review review){return service.reviewOutcome(tenant,tenant+".knowledge_reviewer",id,review.decision(),review.reason());}
}
