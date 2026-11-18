package com.investinator.correlation;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.investinator.domain.Json.*;
import com.investinator.domain.Contracts.IncidentRequest;
import com.investinator.persistence.Store;
import com.investinator.workflows.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.*;
@SpringBootTest(properties={"GRAPH_ADMIN_TOKEN=fixture-admin","INVESTIGATION_API_TOKEN=fixture-read"})
@AutoConfigureMockMvc @Testcontainers
class CorrelationApiTest {
    @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer(org.testcontainers.utility.DockerImageName.parse("postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e").asCompatibleSubstituteFor("postgres"));
    @DynamicPropertySource static void props(DynamicPropertyRegistry p){p.add("investinator.persistence.enabled",()->true);p.add("investinator.persistence.url",postgres::getJdbcUrl);p.add("investinator.persistence.username",postgres::getUsername);p.add("investinator.persistence.password",postgres::getPassword);}
    @Autowired MockMvc mvc;@Autowired WorkflowService workflows;@Autowired Store store;@Autowired CorrelationService service;
    @Test void immutableRevisionsTenantEvidenceAndAuthenticatedJavaQueries()throws Exception {
        var incident=store.write("demo",()->workflows.analyze("demo",new IncidentRequest("Kafka network unstable",null,null,null,null)));
        var observations=CorrelationEngineTest.chain(false).stream().map(o->new CorrelationEngine.Observation(o.id(),"demo",incident.incident_id,o.entity_id(),o.signal(),o.state(),o.observed_at(),o.source_mode(),o.intensity(),o.signature())).toList();var graph=CorrelationEngineTest.graph(false);
        mvc.perform(post("/api/v1/dependencies/fixtures").contentType("application/json").content(encode(obj("graph",graph,"observations",observations)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/dependencies/fixtures").header("Authorization","Bearer fixture-admin").contentType("application/json").content(encode(obj("graph",graph,"observations",observations)))).andExpect(status().isOk());
        service.registerSimulation("demo",graph,observations);assertEquals(1,store.jdbc.queryForObject("select count(*) from dependency_graphs",Integer.class));
        mvc.perform(get("/api/v1/dependencies/graphs/kraft").header("Authorization","Bearer fixture-read")).andExpect(status().isOk()).andExpect(jsonPath("$.graph.revision").value(1));
        mvc.perform(get("/api/v1/dependencies/incidents/"+incident.incident_id+"/investigation").param("graph","kraft").header("Authorization","Bearer fixture-read")).andExpect(status().isOk()).andExpect(jsonPath("$.candidate_causes[0].entity_id").value("network"));
        assertThrows(Exception.class,()->service.graph("other","kraft",null));assertThrows(Exception.class,()->service.investigate("other",incident.incident_id,"kraft",null));
        var revised=new DependencyGraph(graph.graph_id(),2,graph.source_mode(),graph.metadata_mode(),graph.platform_version(),graph.nodes(),graph.edges());service.registerSimulation("demo",revised,List.of());assertEquals(2,service.graph("demo","kraft",null).revision());assertEquals(1,service.graph("demo","kraft",1).revision());assertEquals(1,service.boundGraph("demo",incident.incident_id).revision());
        var foreignGraph=new DependencyGraph("unrelated",1,graph.source_mode(),graph.metadata_mode(),graph.platform_version(),graph.nodes(),graph.edges());service.registerSimulation("demo",foreignGraph,List.of());assertThrows(Exception.class,()->service.investigate("demo",incident.incident_id,"unrelated",null));
        service.registerSimulation("demo",revised,observations);assertEquals(2,service.boundGraph("demo",incident.incident_id).revision());assertThrows(Exception.class,()->service.registerSimulation("demo",graph,observations));
    }
}
