package com.investinator.correlation;
import static com.investinator.domain.Json.*;
import com.investinator.domain.Json;
import com.investinator.persistence.Store;
import com.investinator.workflows.WorkflowService;
import com.investinator.api.ApiFailure;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import java.util.*;
import java.time.*;
@Service
@ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class CorrelationService {
    private final Store db;private final WorkflowService workflows;private final CorrelationEngine engine;
    public CorrelationService(Store db,WorkflowService workflows,Environment env){this.db=db;this.workflows=workflows;engine=new CorrelationEngine(Duration.ofSeconds(env.getProperty("investinator.correlation.skew-seconds",Long.class,30L)),Duration.ofSeconds(env.getProperty("investinator.correlation.window-seconds",Long.class,14400L)));}
    public DependencyGraph graph(String tenant,String id,Integer revision){var values=revision==null?db.jdbc.queryForList("select payload::text from dependency_graphs where tenant_id=? and graph_id=? order by revision desc limit 1",String.class,tenant,id):db.jdbc.queryForList("select payload::text from dependency_graphs where tenant_id=? and graph_id=? and revision=?",String.class,tenant,id,revision);if(values.isEmpty())throw new ApiFailure(404,"Graph not found");return Json.decode(values.getFirst(),DependencyGraph.class);}
    public void registerSimulation(String tenant,DependencyGraph graph,List<CorrelationEngine.Observation> observations){registerSimulation(tenant,graph,observations,"demo.senior_sre");}
    public void registerMonitoringSimulation(String tenant,DependencyGraph graph,List<CorrelationEngine.Observation> observations){registerSimulation(tenant,graph,observations,"system.monitor");}
    private void registerSimulation(String tenant,DependencyGraph graph,List<CorrelationEngine.Observation> observations,String actor){
        if(!graph.source_mode().equals("SIMULATED")||observations.stream().anyMatch(o->!o.source_mode().equals("SIMULATED")))throw new ApiFailure(422,"Public fixture ingestion must explicitly be SIMULATED");
        db.write(tenant,()->{var existing=db.jdbc.queryForList("select payload::text from dependency_graphs where tenant_id=? and graph_id=? and revision=?",String.class,tenant,graph.graph_id(),graph.revision());if(!existing.isEmpty()&&!Json.decode(existing.getFirst(),DependencyGraph.class).equals(graph))throw new ApiFailure(409,"Graph revision is immutable");
            if(existing.isEmpty())db.jdbc.update("insert into dependency_graphs(tenant_id,graph_id,revision,payload) values(?,?,?,?::jsonb)",tenant,graph.graph_id(),graph.revision(),encode(graph));
            for(var o:observations){if(!tenant.equals(o.tenant_id()))throw new ApiFailure(403,"Cross-tenant observation denied");workflows.incident(tenant,o.incident_id());var binding=db.jdbc.queryForList("select graph_id,revision from incident_graph_bindings where tenant_id=? and incident_id=?",tenant,o.incident_id());if(!binding.isEmpty()&&(!binding.getFirst().get("graph_id").equals(graph.graph_id())||((Number)binding.getFirst().get("revision")).intValue()>graph.revision()))throw new ApiFailure(409,"Incident topology scope cannot be changed or downgraded implicitly");db.jdbc.update("insert into incident_graph_bindings(tenant_id,incident_id,graph_id,revision) values(?,?,?,?) on conflict(tenant_id,incident_id) do update set revision=excluded.revision",tenant,o.incident_id(),graph.graph_id(),graph.revision());var previous=db.jdbc.queryForList("select payload::text from correlation_observations where tenant_id=? and incident_id=? and evidence_id=?",String.class,tenant,o.incident_id(),o.id());if(!previous.isEmpty()&&!Json.decode(previous.getFirst(),CorrelationEngine.Observation.class).equals(o))throw new ApiFailure(409,"Evidence is immutable");if(previous.isEmpty())db.jdbc.update("insert into correlation_observations(tenant_id,incident_id,evidence_id,payload) values(?,?,?,?::jsonb)",tenant,o.incident_id(),o.id(),encode(o));}
            workflows.audit(tenant,"dependency_fixture_registered",null,actor,obj("graph_id",graph.graph_id(),"revision",graph.revision(),"source_mode","SIMULATED"));return null;});
    }
    public DependencyGraph boundGraph(String tenant,String incident){workflows.incident(tenant,incident);var rows=db.jdbc.queryForList("select graph_id,revision from incident_graph_bindings where tenant_id=? and incident_id=?",tenant,incident);if(rows.isEmpty())throw new ApiFailure(404,"No verified graph is bound to this incident");return graph(tenant,rows.getFirst().get("graph_id").toString(),((Number)rows.getFirst().get("revision")).intValue());}
    public CorrelationEngine.Investigation investigate(String tenant,String incident,String graph,Integer revision){workflows.incident(tenant,incident);var bound=boundGraph(tenant,incident);if(!bound.graph_id().equals(graph)||revision!=null&&revision>bound.revision())throw new ApiFailure(422,"Graph is outside this incident's verified scope");if(revision==null)revision=bound.revision();var observations=db.jdbc.queryForList("select payload::text from correlation_observations where tenant_id=? and incident_id=? order by evidence_id limit 1001",String.class,tenant,incident).stream().map(v->Json.decode(v,CorrelationEngine.Observation.class)).toList();return engine.investigate(tenant,incident,graph(tenant,graph,revision),observations);}
    public List<CorrelationEngine.Observation> observations(String tenant,String incident){workflows.incident(tenant,incident);return db.jdbc.queryForList("select payload::text from correlation_observations where tenant_id=? and incident_id=? order by evidence_id limit 1000",String.class,tenant,incident).stream().map(v->Json.decode(v,CorrelationEngine.Observation.class)).toList();}
}
