package com.investinator.api;
import static com.investinator.domain.Json.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.investinator.domain.Json;
import com.investinator.domain.Contracts.*;
import com.investinator.persistence.*;
import com.investinator.workflows.WorkflowService;
import com.investinator.chat.*;
import com.investinator.correlation.*;
import com.investinator.knowledge.*;
import com.investinator.monitoring.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.junit.jupiter.api.*;
import java.util.*;
import java.time.*;
@SpringBootTest(properties={"MONITOR_API_TOKEN=fixture-ui-monitor","investinator.monitor.enabled=true","investinator.monitor.tick-ms=3600000","investinator.monitor.health-ms=3600000"})
@AutoConfigureMockMvc @Testcontainers
class OperatorUiContractTest {
    @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer(org.testcontainers.utility.DockerImageName.parse("postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e").asCompatibleSubstituteFor("postgres"));
    @DynamicPropertySource static void props(DynamicPropertyRegistry p){p.add("investinator.persistence.enabled",()->true);p.add("investinator.persistence.url",postgres::getJdbcUrl);p.add("investinator.persistence.username",postgres::getUsername);p.add("investinator.persistence.password",postgres::getPassword);}
    @Autowired MockMvc mvc;@Autowired Store db;@Autowired WorkflowService workflows;@Autowired ReadTools tools;@Autowired CorrelationService graphs;@Autowired KnowledgeService knowledge;@Autowired MonitoringService monitor;
    @Test void detailHistoryAndEvidenceAreScopedWithoutExposingPrivateContinuation()throws Exception{
        var incident=db.write("demo",()->workflows.analyze("demo",new IncidentRequest("Kafka UI evidence contract "+UUID.randomUUID(),null,null,null,null)));
        var reply=map(Json.decode(mvc.perform(post("/api/v1/investigator/messages").contentType("application/json").content(encode(obj("incident_id",incident.incident_id,"message","Explain without fabricating a provider response")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()));String id=reply.get("conversation_id").toString();assertNull(reply.get("usage"));assertEquals("NOT_CONFIGURED",reply.get("provider_status"));
        mvc.perform(get("/api/v1/investigator/conversations/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.messages[1].turn_metadata").doesNotExist()).andExpect(jsonPath("$.messages[1].provider_status").doesNotExist());
        mvc.perform(get("/api/v1/investigator/conversations/"+id).param("details","true")).andExpect(status().isOk()).andExpect(jsonPath("$.messages[1].provider_status").value("NOT_CONFIGURED")).andExpect(jsonPath("$.messages[1].usage").isEmpty());
        mvc.perform(get("/api/v1/investigator/conversations/"+id+"/evidence/"+incident.evidence.getFirst().id())).andExpect(status().isOk()).andExpect(jsonPath("$.snippet").exists());mvc.perform(get("/api/v1/investigator/conversations/"+id+"/evidence/invented")).andExpect(status().isNotFound());
        db.write("other",()->{db.insert(Table.CONVERSATIONS,"other",obj("id","foreign-ui-conversation"));return null;});mvc.perform(get("/api/v1/investigator/conversations/foreign-ui-conversation/evidence/anything")).andExpect(status().isNotFound());var listed=mvc.perform(get("/api/v1/investigator/conversations")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(listed.contains("foreign-ui-conversation"));assertFalse(listed.contains("ciphertext"));
    }
    @Test void historicalInspectorRechecksKnowledgeRevocationAndReadableChunk()throws Exception{
        var incident=db.write("demo",()->workflows.analyze("demo",new IncidentRequest("Kafka readable UI source "+UUID.randomUUID(),null,null,null,null)));String conversation=UUID.randomUUID().toString();db.write("demo",()->{db.insert(Table.CONVERSATIONS,"demo",obj("id",conversation,"incident_id",incident.incident_id));return null;});
        String now=Instant.now().minusSeconds(10).toString();var graph=new DependencyGraph("ui-inspection",1,"SIMULATED","KRAFT","Kafka 4.3",List.of(new DependencyGraph.Node("broker",DependencyGraph.NodeType.BROKER,"Broker")),List.of());graphs.registerSimulation("demo",graph,List.of(new CorrelationEngine.Observation("ui-observation","demo",incident.incident_id,"broker",CorrelationEngine.Signal.BROKER_ERROR,CorrelationEngine.State.ABNORMAL,now,"SIMULATED",0.5,"fixture")));
        var document=new KnowledgeDocument("ui-readable-runbook",1,KnowledgeDocument.Kind.RUNBOOK,"UI source fixture","Kafka","4.3","KRAFT","repo:eval/knowledge_review_cases.json","fixture-reviewer",now,List.of("demo.senior_sre"),"Historical quorum guidance, not proof of causation",null,null,null,null,"SIMULATED");knowledge.ingest("demo","demo.fixture_writer",document);knowledge.review("demo","demo.fixture_reviewer",document.document_id(),1,"APPROVE","SIMULATED role-boundary test, not independent human quality review");var chunk=document.chunks("demo").getFirst();String evidence=chunk.get("chunk_id").toString();var context=new ReadTools.Context("demo",incident.incident_id,"SIMULATED");var citation=new InvestigationAnswer.Citation(evidence,"knowledge:"+document.document_id()+"@1#0",now,"HISTORICAL_ADVICE");tools.capture(context,citation,obj("kind","knowledge","document_id",document.document_id(),"revision",1));
        String route="/api/v1/investigator/conversations/"+conversation+"/evidence/"+evidence;mvc.perform(get(route)).andExpect(status().isOk()).andExpect(jsonPath("$.historical_advice").value(true)).andExpect(jsonPath("$.snippet").value(document.content()));knowledge.review("demo","demo.fixture_reviewer",document.document_id(),1,"REVOKE","Revoked before browser inspects stale citation");mvc.perform(get(route)).andExpect(status().isNotFound());
    }
    @Test void boundedSettingsPauseRulesAndSuppressNotificationsWithoutErasingIncidents()throws Exception{
        mvc.perform(post("/api/v1/monitor/settings").contentType("application/json").content(encode(obj("cadence_seconds",5,"suppress_seconds",10,"disabled_rules",List.of())))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/monitor/settings").header("Authorization","Bearer fixture-ui-monitor").contentType("application/json").content(encode(obj("cadence_seconds",0,"suppress_seconds",4000,"disabled_rules",List.of())))).andExpect(status().isUnprocessableEntity());
        monitor.control("demo",false);monitor.configure("demo",new MonitoringService.Settings(15,60,List.of("network")));String graphId="ui-settings";var graph=new DependencyGraph(graphId,1,"SIMULATED","KRAFT","Kafka 4.3",List.of(new DependencyGraph.Node("network",DependencyGraph.NodeType.NETWORK,"Network")),List.of());graphs.registerSimulation("demo",graph,List.of());Instant base=Instant.now().minusSeconds(30);
        for(int i=0;i<2;i++)monitor.accept("demo",new MonitoringContracts.SignalReceived("disabled-ui-"+i,"demo","demo","ui-fixture",graphId,1,"network","network",base.plusSeconds(i).toString(),"SIMULATED",Map.of("network_loss_percent",10.0),Set.of("network_loss_percent"),"fixture",null));monitor.tick("demo");assertEquals(2,db.jdbc.queryForObject("select count(*) from monitor_events where state='RULE_DISABLED'",Integer.class));
        monitor.configure("demo",new MonitoringService.Settings(15,60,List.of()));for(int i=0;i<2;i++)monitor.accept("demo",new MonitoringContracts.SignalReceived("suppressed-ui-"+i,"demo","demo","ui-fixture",graphId,1,"network","network",base.plusSeconds(5+i).toString(),"SIMULATED",Map.of("network_loss_percent",10.0),Set.of("network_loss_percent"),"fixture",null));monitor.tick("demo");assertEquals(1,db.jdbc.queryForObject("select count(*) from monitor_incidents",Integer.class));assertEquals(1,db.jdbc.queryForObject("select count(*) from monitor_outbox where kind='NOTIFY' and state='SUPPRESSED'",Integer.class));assertEquals(0,db.jdbc.queryForObject("select count(*) from monitor_deliveries",Integer.class));monitor.configure("demo",new MonitoringService.Settings(15,0,List.of()));monitor.dispatch("demo");assertEquals(1,db.jdbc.queryForObject("select count(*) from monitor_deliveries",Integer.class));assertTrue(monitor.claimScheduledTick("demo"));assertFalse(monitor.claimScheduledTick("demo"));
    }
}
