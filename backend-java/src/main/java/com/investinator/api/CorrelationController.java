package com.investinator.api;
import com.investinator.correlation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
@RestController
@ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
@RequestMapping("/api/v1/dependencies")
public class CorrelationController {
    private final CorrelationService service;private final String tenant;
    public CorrelationController(CorrelationService service,@Value("${investinator.demo-tenant:demo}") String tenant){this.service=service;this.tenant=tenant;}
    public record Fixture(DependencyGraph graph,List<CorrelationEngine.Observation> observations) {}
    @PostMapping("/fixtures") public Map<String,Object> register(@RequestBody Fixture input){if(input.graph()==null||input.observations()==null||input.observations().size()>1000)throw new ApiFailure(422,"Invalid fixture bounds");service.registerSimulation(tenant,input.graph(),input.observations());return Map.of("status","registered","source_mode","SIMULATED");}
    @GetMapping("/graphs/{id}") public Map<String,Object> graph(@PathVariable String id,@RequestParam(required=false) Integer revision,@RequestParam(required=false) String at){var graph=service.graph(tenant,id,revision);return Map.of("graph",graph,"active_edges",graph.at(at==null?Instant.now():Instant.parse(at)));}
    @GetMapping("/incidents/{id}/graph") public DependencyGraph bound(@PathVariable String id){return service.boundGraph(tenant,id);}
    @GetMapping("/incidents/{id}/investigation") public CorrelationEngine.Investigation investigate(@PathVariable String id,@RequestParam String graph,@RequestParam(required=false) Integer revision){return service.investigate(tenant,id,graph,revision);}
}
