package com.investinator.api;
import com.investinator.connectors.*;
import com.investinator.health.*;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import static com.investinator.health.HealthCheckResult.*;
@RestController
@RequestMapping("/api/v1/connectors")
public class ConnectorController {
    private final ConnectorRegistry registry;private final HealthAggregator health;
    public ConnectorController(ConnectorRegistry registry,HealthAggregator health){this.registry=registry;this.health=health;}
    private void fixedParameters(HttpServletRequest request){if(!request.getParameterMap().isEmpty())throw new ApiFailure(422,"Connector parameters are fixed by backend configuration");}
    @GetMapping public Map<String,Object> list(HttpServletRequest request){fixedParameters(request);return Map.of("scope","diagnostics.read","tenant","demo","connectors",registry.descriptors());}
    @GetMapping("/{service}/{capability}") public HealthCheckResult inspect(@PathVariable String service,@PathVariable String capability,HttpServletRequest request) {
        fixedParameters(request);DiagnosticConnector connector;
        try{connector=registry.find(service);}catch(SecurityException e){throw new ApiFailure(403,"Service outside diagnostics scope");}catch(NoSuchElementException e){throw new ApiFailure(404,"Unknown connector");}
        DiagnosticConnector.Capability selected;try{selected=DiagnosticConnector.Capability.valueOf(capability);}catch(IllegalArgumentException e){throw new ApiFailure(404,"Unknown diagnostic capability");}
        if(!connector.descriptor().supported().contains(selected))return of(service,capability,Status.NOT_TESTED,SourceMode.NOT_TESTED,"connector capability declaration","This adapter does not support this capability","Use only supported read-only capabilities","UNSUPPORTED_CAPABILITY",false,Map.of());
        if(selected==DiagnosticConnector.Capability.config)return of(service,capability,Status.NOT_TESTED,SourceMode.NOT_TESTED,"safe connector descriptor","Configuration capability describes supported protocol/authentication; no secrets returned","Run health to verify configured integration","",false,Map.of("descriptor",connector.descriptor()));
        return health.refresh().checks().stream().filter(c->c.service().equals(service)).findFirst().orElseThrow(()->new ApiFailure(503,"Diagnostic result unavailable"));
    }
}
