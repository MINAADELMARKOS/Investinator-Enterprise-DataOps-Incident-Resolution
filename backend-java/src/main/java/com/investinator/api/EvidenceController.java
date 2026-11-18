package com.investinator.api;
import com.investinator.evidence.EvidenceSearch;
import static com.investinator.domain.Json.*;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {
    private final ObjectProvider<EvidenceSearch> search;
    private final String tenant;
    public EvidenceController(ObjectProvider<EvidenceSearch> search,@Value("${investinator.demo-tenant:demo}") String tenant) {this.search=search;this.tenant=tenant;if(!tenant.matches("[A-Za-z0-9_-]{1,100}"))throw new IllegalArgumentException("Invalid evidence tenant binding");}
    @GetMapping("/search") public ResponseEntity<?> query(@RequestParam(defaultValue="") String q,@RequestParam(required=false) String service,
        @RequestParam(required=false) String from,@RequestParam(required=false) String to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        Set<String> allowed=Set.of("q","service","from","to","page","size");
        if(!allowed.containsAll(request.getParameterMap().keySet()))throw new ApiFailure(422,"Unsupported search parameter");
        var engine=search.getIfAvailable();if(engine==null)return result(obj("status","NOT_CONFIGURED","retrieval_mode","NOT_TESTED","items",List.of()));
        return result(engine.search(tenant,q,service,from,to,page,size));
    }
    @GetMapping("/health") public ResponseEntity<?> health(){var engine=search.getIfAvailable();return result(engine==null?obj("status","NOT_CONFIGURED","retrieval_mode","NOT_TESTED"):engine.health(tenant));}
    private ResponseEntity<?> result(Map<String,Object> value){String state=value.get("status").toString();return ResponseEntity.status(Set.of("HEALTHY","DEGRADED").contains(state)?200:state.equals("RATE_LIMITED")?429:503).body(value);}
}
