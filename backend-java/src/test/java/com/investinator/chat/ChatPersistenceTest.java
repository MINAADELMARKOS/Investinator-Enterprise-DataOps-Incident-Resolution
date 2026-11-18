package com.investinator.chat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.investinator.domain.Json.*;
import com.investinator.domain.Json;
import com.investinator.domain.Contracts.*;
import com.investinator.persistence.*;
import com.investinator.workflows.WorkflowService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.*;
@SpringBootTest(properties={"investinator.openai.enabled=true","investinator.openai.model=fixture-model","INVESTINATOR_AI_ALLOWED_SERVICES=kafka","OPENAI_API_KEY=fixture-key-not-real","INVESTINATOR_OPENAI_TEST_ENDPOINT=true","INVESTINATOR_CHAT_STATE_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="})
@AutoConfigureMockMvc @Testcontainers
class ChatPersistenceTest {
    static final WireMockServer provider=new WireMockServer(0);static {provider.start();}
    @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer(org.testcontainers.utility.DockerImageName.parse("postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e").asCompatibleSubstituteFor("postgres"));
    @Container static final org.testcontainers.kafka.KafkaContainer kafka=new org.testcontainers.kafka.KafkaContainer(org.testcontainers.utility.DockerImageName.parse("apache/kafka:4.3.1@sha256:77e3df9054047a88b520d0cc46e16696d3b22022e1d580aeccd2632df6532837").asCompatibleSubstituteFor("apache/kafka"));
    @DynamicPropertySource static void props(DynamicPropertyRegistry p){p.add("investinator.persistence.enabled",()->true);p.add("investinator.persistence.url",postgres::getJdbcUrl);p.add("investinator.persistence.username",postgres::getUsername);p.add("investinator.persistence.password",postgres::getPassword);p.add("KAFKA_BOOTSTRAP_SERVERS",kafka::getBootstrapServers);p.add("INVESTINATOR_OPENAI_BASE_URL",()->provider.baseUrl()+"/v1");}
    @Autowired MockMvc mvc;@Autowired Store store;@Autowired WorkflowService workflows;@Autowired ReadTools tools;
    @AfterAll static void stop(){provider.stop();}
    @BeforeEach void clean(){provider.resetAll();}
    Map<String,Object> chat(Map<String,Object> input,String key)throws Exception{var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/investigator/messages").contentType("application/json").content(encode(input));if(key!=null)request.header("Idempotency-Key",key);return map(Json.decode(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()));}
    @Test void officialSdkBeanPersistsRedactedHistoryEncryptedContinuationAndIdempotentFollowups()throws Exception {
        var answer=ChatGatewayTest.answer("General architecture advice");answer.put("source_mode","NOT_TESTED");provider.stubFor(post(urlEqualTo("/v1/responses")).willReturn(okJson(encode(ChatGatewayTest.response(List.of(ChatGatewayTest.message(answer)))))));
        var first=chat(obj("message","What is a metadata quorum? password=do-not-send"),"sdk-first");assertEquals("gpt",first.get("response_mode"));assertEquals(first,chat(obj("message","What is a metadata quorum? password=do-not-send"),"sdk-first"));assertEquals(1,provider.getAllServeEvents().size());String id=first.get("conversation_id").toString();var follow=chat(obj("conversation_id",id,"message","How does that affect failover?"),null);assertEquals("gpt",follow.get("response_mode"));assertTrue(provider.getAllServeEvents().stream().anyMatch(e->e.getRequest().getBodyAsString().contains("metadata quorum")&&e.getRequest().getBodyAsString().contains("affect failover")));assertTrue(provider.getAllServeEvents().stream().noneMatch(e->e.getRequest().getBodyAsString().contains("do-not-send")));
        var saved=store.jdbc.queryForObject("select ciphertext from provider_conversation_state where tenant_id='demo' and conversation_id=?",String.class,id);assertFalse(saved.contains("metadata quorum"));mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/investigator/conversations/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.messages.length()").value(4));
        int calls=provider.getAllServeEvents().size();store.write("other",()->{store.insert(Table.CONVERSATIONS,"other",obj("id","foreign-conversation"));return null;});mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/investigator/messages").contentType("application/json").content(encode(obj("conversation_id","foreign-conversation","message","read it")))).andExpect(status().isNotFound());assertEquals(calls,provider.getAllServeEvents().size());
    }
    private static Map<String,Object> mixedAnswer(){var value=ChatGatewayTest.answer("Read completed; causal attribution remains unverified");value.put("source_mode","MIXED");return value;}
    @Test void functionLoopExecutesActualReadOnlyKafkaAdminAgainstRealKraftBroker()throws Exception {
        var incident=store.write("demo",()->workflows.analyze("demo",new IncidentRequest("Inspect Kafka metadata without changing it",null,null,null,null)));
        provider.stubFor(post(urlEqualTo("/v1/responses")).inScenario("native-kafka").whenScenarioStateIs("Started").willReturn(okJson(encode(ChatGatewayTest.response(List.of(ChatGatewayTest.call("connector_health","{\"service\":\"kafka\"}","native_kafka")))))).willSetStateTo("complete"));
        provider.stubFor(post(urlEqualTo("/v1/responses")).inScenario("native-kafka").whenScenarioStateIs("complete").willReturn(okJson(encode(ChatGatewayTest.response(List.of(ChatGatewayTest.message(mixedAnswer())))))));
        var result=chat(obj("message","Read current Kafka metadata","incident_id",incident.incident_id),null);assertEquals("gpt",result.get("response_mode"));
        var body=provider.getAllServeEvents().stream().map(e->e.getRequest().getBodyAsString()).filter(s->s.contains("function_call_output")).findFirst().orElseThrow();var input=Json.rows(map(Json.decode(body)).get("input"));var output=input.stream().filter(i->"function_call_output".equals(i.get("type"))).findFirst().orElseThrow();var tool=map(Json.decode(output.get("output").toString()));var observed=map(tool.get("untrusted_observations"));var health=map(observed.get("health"));assertEquals("HEALTHY",health.get("status"));assertEquals("KRAFT",map(health.get("details")).get("metadata_mode"));assertNotNull(map(health.get("details")).get("cluster_id"));assertEquals(1,store.jdbc.queryForObject("select count(*) from citation_evidence where tenant_id='demo' and incident_id=?",Integer.class,incident.incident_id));
    }
    @Test void toolsRejectCrossTenantArgumentsNoIncidentAndUnsupportedApproval() {
        var incident=store.write("demo",()->workflows.analyze("demo",new IncidentRequest("Kafka disk pressure",null,null,null,null)));var context=new ReadTools.Context("demo",incident.incident_id,"SIMULATED");tools.initialize(context);
        assertEquals("DENIED",tools.execute("get_evidence",obj("tenant_id","other"),context).get("status"));assertEquals("DENIED",tools.execute("approve_action",Map.of(),context).get("status"));assertEquals("NOT_TESTED",tools.execute("get_evidence",Map.of(),new ReadTools.Context("demo",null,"NOT_TESTED")).get("status"));assertEquals(0,store.all(Table.APPROVALS,"demo").size());
        var foreign=new ReadTools.Context("other",incident.incident_id,"SIMULATED");assertThrows(Exception.class,()->tools.initialize(foreign));
    }
}
