package com.investinator;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.investinator.domain.Json.*;
import com.investinator.domain.Json;
import com.investinator.domain.Contracts.*;
import com.investinator.persistence.*;
import com.investinator.workflows.WorkflowService;
import com.investinator.api.ApiFailure;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PersistenceWorkflowTest {
    @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer(org.testcontainers.utility.DockerImageName.parse("postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e").asCompatibleSubstituteFor("postgres"));
    @DynamicPropertySource static void properties(DynamicPropertyRegistry props) {
        props.add("investinator.persistence.enabled",()->true);
        props.add("investinator.persistence.url",postgres::getJdbcUrl);
        props.add("investinator.persistence.username",postgres::getUsername);
        props.add("investinator.persistence.password",postgres::getPassword);
        props.add("investinator.demo-mode",()->true);
    }
    @Autowired MockMvc mvc;
    @Autowired WorkflowService service;
    @Autowired Store store;
    @BeforeEach void reset() {
        store.jdbc.execute("truncate table execution_records,request_keys,alerts,telemetry,approvals,audit_events,replay_runs,messages,conversations,evaluations,incidents,entities,relationships,knowledge_documents,ai_usage cascade");
        service.seed("demo");
    }
    Map<String,Object> postJson(String path,Object body,int status,String key) throws Exception {
        var req=post(path).contentType("application/json");if(body!=null)req.content(encode(body));if(key!=null)req.header("Idempotency-Key",key);
        var response=mvc.perform(req).andExpect(status().is(status)).andReturn().getResponse();
        return Json.map(Json.decode(response.getContentAsString()));
    }
    @Test void replayApprovalVerificationAndAuditAreTransactionalAndTenantScoped() throws Exception {
        var replay=postJson("/api/v1/replay/runs",obj("scenario","kafka_disk_pressure"),200,"replay-one");String id=replay.get("incident_id").toString();
        assertEquals(5,((List<?>)replay.get("steps")).size());
        assertEquals(replay,postJson("/api/v1/replay/runs",obj("scenario","kafka_disk_pressure"),200,"replay-one"));
        postJson("/api/v1/replay/runs",obj("scenario","spark_executor_oom"),409,"replay-one");
        postJson("/api/v1/replay/runs",obj("scenario","spark_executor_oom"),409,null);
        postJson("/api/v1/incidents/"+id+"/actions/ACT-DEMO/execute",null,403,null);
        assertEquals(false,postJson("/api/v1/incidents/"+id+"/verify",obj("checks",obj("passed",true)),200,null).get("passed"));
        assertEquals(5,store.all(Table.ALERTS,"demo").size());
        assertTrue(service.incident("demo",id).affected_entities.contains("revenue-dashboard"));
        postJson("/api/v1/incidents/"+id+"/actions/ACT-DEMO/approval",obj("decision","approve","decided_by","forged-admin","reason","token=hidden"),200,null);
        postJson("/api/v1/incidents/"+id+"/actions/ACT-DEMO/approval",obj("decision","approve","decided_by","forged-admin"),409,null);
        var approval=store.all(Table.APPROVALS,"demo").getFirst();assertEquals("demo.senior_sre",approval.get("actor"));assertEquals("[REDACTED]",approval.get("reason"));
        var result=postJson("/api/v1/incidents/"+id+"/actions/ACT-DEMO/execute",null,200,"execute-one");
        assertEquals(true,map(result.get("verification")).get("passed"));
        assertEquals(result,postJson("/api/v1/incidents/"+id+"/actions/ACT-DEMO/execute",null,200,"execute-one"));
        postJson("/api/v1/incidents/"+id+"/actions/ACT-DEMO/execute",null,403,null);
        assertEquals(Lifecycle.resolved,service.incident("demo",id).status);
        assertEquals(1.,((Number)store.all(Table.EVALUATIONS,"demo").getFirst().get("score")).doubleValue());
        assertTrue(store.all(Table.AUDIT_EVENTS,"demo").stream().anyMatch(r->r.get("event").equals("action_executed")));
        service.seed("other");assertEquals(404,assertThrows(ApiFailure.class,()->service.incident("other",id)).status);
        assertTrue(store.all(Table.APPROVALS,"other").isEmpty());assertTrue(store.all(Table.AUDIT_EVENTS,"other").isEmpty());
        mvc.perform(get("/api/v1/incidents/"+id+"/postmortem")).andExpect(status().isOk());
    }
    @Test void allReplayScenariosAndRejectionPreserveExpectedState() throws Exception {
        for(String scenario:List.of("spark_executor_oom","data_freshness_failure")) {
            String id=postJson("/api/v1/replay/runs",obj("scenario",scenario),200,null).get("incident_id").toString();
            assertEquals(false,postJson("/api/v1/incidents/"+id+"/verify",null,200,null).get("passed"));
            postJson("/api/v1/incidents/"+id+"/actions/ACT-DEMO/approval",obj("decision","reject","decided_by","tester"),200,null);
            assertEquals(Lifecycle.closed_unresolved,service.incident("demo",id).status);
        }
    }
    @Test void negativeContractsCannotForgeTenantApprovalOrVerification() throws Exception {
        String id=postJson("/api/v1/incidents/analyze",obj("description","Kafka lag high","tenant_id","other"),200,null).get("incident_id").toString();
        assertEquals("demo",service.incident("demo",id).tenant_id);
        postJson("/api/v1/incidents/"+id+"/verify",obj("checks",obj("healthy",true)),409,null);
        postJson("/api/v1/incidents/"+id+"/actions/ACT-004/execute",null,403,null);
        postJson("/api/v1/incidents/analyze",obj("description",""),422,null);
        postJson("/api/v1/telemetry/metrics",obj("name","x","entity_id","x","value",2,"observed_at","not-a-time"),422,null);
        postJson("/api/v1/replay/runs",obj("scenario","unknown"),404,null);
        mvc.perform(get("/api/v1/telemetry/logs?limit=201")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/not-an-endpoint")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/telemetry/logs?limit=nope")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/incidents/missing")).andExpect(status().isNotFound());
        postJson("/api/v1/investigator/messages",obj("message","question","conversation_id","foreign"),404,null);
    }
    @Test void conversationHistoryAndFailedCommandsRollback() throws Exception {
        int before=store.all(Table.CONVERSATIONS,"demo").size();
        postJson("/api/v1/investigator/messages",obj("message","question","incident_id","missing"),404,null);
        assertEquals(before,store.all(Table.CONVERSATIONS,"demo").size());
        var first=postJson("/api/v1/investigator/messages",obj("message","password=secret contact jane@example.com"),200,"chat-one");
        String id=first.get("conversation_id").toString();
        postJson("/api/v1/investigator/messages",obj("message","follow up","conversation_id",id),200,null);
        mvc.perform(get("/api/v1/investigator/conversations/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.messages.length()").value(4));
        assertFalse(store.all(Table.MESSAGES,"demo").stream().anyMatch(r->r.get("content").toString().contains("jane@example.com")));
    }
    @Test void concurrentRetriesDoNotDuplicateReplayOrAlertWrites() throws Exception {
        var pool=Executors.newFixedThreadPool(4);
        try {
            var tasks=new ArrayList<Callable<Object>>();
            for(int i=0;i<4;i++)tasks.add(()->store.command("demo","/test-replay","same","hash",()->service.replay("demo","kafka_disk_pressure")));
            var results=pool.invokeAll(tasks);String first=encode(results.getFirst().get());
            for(var result:results)assertEquals(first,encode(result.get()));
            assertEquals(1,store.all(Table.REPLAY_RUNS,"demo").size());assertEquals(5,store.all(Table.ALERTS,"demo").size());
        }finally{pool.shutdownNow();}
    }
    @Test void unknownProductionActionsAndMigrationOwnershipAreNotPermitted() {
        assertEquals(2,store.jdbc.queryForObject("select count(*) from flyway_schema_history where type='SQL' and success",Integer.class));
        assertEquals(0,store.jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name='incidents'",Integer.class));
        assertThrows(ApiFailure.class,()->store.write("demo",()->{service.analyze("demo",new IncidentRequest("Kafka",null,null,"production","demo"));throw new ApiFailure(409,"rollback");}));
        assertTrue(store.all(Table.INCIDENTS,"demo").isEmpty());
    }

    @Test void pythonAndJavaContractsMatchForSeededReadsAndWorkflowTraces() throws Exception {
        String fixture;
        try(var in=getClass().getResourceAsStream("/contracts/python-main.json")) {
            fixture=new String(java.util.Objects.requireNonNull(in).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
        String incident="",conversation="";int count=0;
        for(var step:Json.rows(Json.decode(fixture))) {
            String path=step.get("path").toString().replace("{incident}",incident).replace("{conversation}",conversation);
            var req=step.get("method").equals("GET")?get(path):post(path);
            if(step.get("body")!=null)req.contentType("application/json").content(encode(step.get("body")));
            var response=mvc.perform(req).andReturn().getResponse();
            assertEquals(((Number)step.get("status")).intValue(),response.getStatus(),"HTTP "+path+": "+response.getContentAsString());
            Object actual=step.get("response") instanceof String?response.getContentAsString():Json.decode(response.getContentAsString());
            assertEquals(normalize(step.get("response"),""),normalize(actual,""),"Contract step "+(++count)+" "+path);
            if(actual instanceof Map<?,?> result&&response.getStatus()==200) {
                if(result.get("incident_id")!=null)incident=result.get("incident_id").toString();
                if(result.get("conversation_id")!=null)conversation=result.get("conversation_id").toString();
            }
        }
        assertTrue(count>=70);
    }
    private Object normalize(Object value,String key) {
        if(value==null)return null;
        if(value instanceof Map<?,?> row) {
            var normalized=new TreeMap<String,Object>();
            row.forEach((k,v)->normalized.put(k.toString(),normalize(v,k.toString())));return normalized;
        }
        if(value instanceof List<?> list) {
            var normalized=new ArrayList<Object>();for(var item:list)normalized.add(normalize(item,key));
            // Unordered collections have no stable ordering contract. Timelines and history do.
            if(!Set.of("timeline","messages","evidence").contains(key))normalized.sort(Comparator.comparing(Json::encode));
            return normalized;
        }
        if(value instanceof Number number)return new java.math.BigDecimal(number.toString()).stripTrailingZeros();
        if(value instanceof String text) {
            if(Set.of("at","created_at","updated_at","observed_at").contains(key)) {
                try { java.time.OffsetDateTime.parse(text); }
                catch(java.time.format.DateTimeParseException noZone) { java.time.LocalDateTime.parse(text); }
                return "<timestamp>";
            }
            return text.replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}","<uuid>")
                .replaceAll("INC-[A-F0-9]{8}","INC-<generated>")
                .replaceAll("METRIC-[0-9a-f]{8}","METRIC-<generated>");
        }
        return value;
    }
}
