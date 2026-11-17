package com.investinator.connectors;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import com.investinator.health.HealthCheckResult;
import java.util.*;
class RestDiagnosticConnectorTest {
    WireMockServer server;
    @BeforeEach void start(){server=new WireMockServer(0);server.start();}
    @AfterEach void stop(){server.stop();}
    ConnectorConfig config(String service){String p="CONNECTOR_"+service.toUpperCase(Locale.ROOT).replace('-','_')+"_";return new ConnectorConfig(new MockEnvironment().withProperty(p+"URL",server.baseUrl()).withProperty(p+"TOKEN","fixture-secret-do-not-return").withProperty(p+"LOCAL_FIXTURE","true"),service);}
    void reply(String path,String json){server.stubFor(get(urlEqualTo(path)).withHeader("Authorization",equalTo("Bearer fixture-secret-do-not-return")).willReturn(okJson(json)));}
    void fixtures(String service){switch(service){
        case "spark" -> {reply("/api/v1/version","{\"spark\":\"4.0.0\"}");reply("/api/v1/applications?limit=100","[{\"id\":\"app-1\"}]");reply("/api/v1/applications/app-1/executors","[{\"isActive\":true,\"memoryUsed\":20,\"maxMemory\":100}]");reply("/api/v1/applications/app-1/stages?limit=100","[{\"status\":\"COMPLETE\"}]");}
        case "yarn" -> {reply("/ws/v1/cluster/info","{\"clusterInfo\":{\"hadoopVersion\":\"3.4.2\",\"haState\":\"ACTIVE\"}}");reply("/ws/v1/cluster/metrics","{\"clusterMetrics\":{\"appsPending\":0,\"availableMB\":1024,\"containersPending\":0}}");reply("/ws/v1/cluster/apps?states=ACCEPTED,RUNNING,FAILED&limit=100","{\"apps\":{\"app\":[]}}");reply("/ws/v1/cluster/scheduler","{\"scheduler\":{\"schedulerInfo\":{\"type\":\"capacityScheduler\"}}}");}
        case "hdfs" -> {reply("/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo","{\"beans\":[{\"Version\":\"3.4.2, rfixture\",\"NumberOfMissingBlocks\":0,\"NumberOfMissingBlocksWithReplicationFactorOne\":0,\"LiveNodes\":\"{\\\"dn1\\\":{}}\",\"DeadNodes\":\"{}\",\"Safemode\":\"\"}]}");reply("/jmx?qry=Hadoop:service=NameNode,name=NameNodeStatus","{\"beans\":[{\"State\":\"active\"}]}");reply("/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem","{\"beans\":[{\"UnderReplicatedBlocks\":0,\"CorruptBlocks\":0}]}");}
        case "nifi" -> {reply("/nifi-api/flow/about","{\"about\":{\"version\":\"2.0.0\"}}");reply("/nifi-api/system-diagnostics","{\"systemDiagnostics\":{\"aggregateSnapshot\":{\"uptime\":\"1h\",\"availableProcessors\":4}}}");reply("/nifi-api/flow/process-groups/root/status?recursive=false","{\"processGroupStatus\":{\"aggregateSnapshot\":{\"flowFilesQueued\":0,\"bytesQueued\":0,\"activeThreadCount\":1}}}");reply("/nifi-api/flow/bulletin-board?limit=100","{\"bulletinBoard\":{\"bulletins\":[]}}");}
        case "airflow" -> {reply("/api/v1/version","{\"version\":\"2.10.4\"}");reply("/api/v1/health","{\"metadatabase\":{\"status\":\"healthy\"},\"scheduler\":{\"status\":\"healthy\"}}");reply("/api/v1/dags?limit=100&only_active=true","{\"dags\":[]}");}
        case "cloudera-manager" -> {server.stubFor(get(urlEqualTo("/api/version")).withHeader("Authorization",equalTo("Bearer fixture-secret-do-not-return")).willReturn(ok("v50")));reply("/api/v50/cm/version","{\"version\":\"7.13.0\"}");reply("/api/v50/clusters?view=summary","{\"items\":[]}");}
        case "atlas" -> {reply("/api/atlas/admin/version","{\"Version\":\"2.4.0\"}");reply("/api/atlas/admin/status","{\"Status\":\"ACTIVE\"}");}
        case "impala" -> {reply("/?json","{\"version\":\"impalad version 4.4.0\"}");reply("/backends?json","{\"num_known_backends\":1,\"num_active_backends\":1}");}
    }}
    @Test void everySupportedRestAdapterUsesAuthenticatedGetOnlyAndBoundsReturnedFacts(){
        for(String service:List.of("spark","yarn","hdfs","nifi","airflow","cloudera-manager","atlas","impala")){
            server.resetAll();fixtures(service);var result=new RestDiagnosticConnector(config(service)).check();assertEquals(HealthCheckResult.Status.HEALTHY,result.status(),service+":"+result.error_type());assertFalse(result.toString().contains("fixture-secret-do-not-return"));
            assertTrue(server.getAllServeEvents().stream().allMatch(e->e.getRequest().getMethod().getName().equals("GET")&&e.getRequest().getHeader("Authorization").startsWith("Bearer ")));assertEquals("SIMULATED",result.details().get("data_source_mode"));
        }
    }
    @Test void authorizationVersionMalformedAndServerFailuresNeverLookHealthy(){
        for(String service:List.of("spark","yarn","hdfs","nifi","airflow","cloudera-manager","atlas","impala")) {
            server.resetAll();server.stubFor(get(anyUrl()).willReturn(forbidden().withBody("DO-NOT-ECHO-provider-secret")));var denied=new RestDiagnosticConnector(config(service)).check();assertEquals(HealthCheckResult.Status.UNHEALTHY,denied.status(),service);assertEquals("AUTHORIZATION",denied.error_type());assertFalse(denied.toString().contains("DO-NOT-ECHO"));
            server.resetAll();server.stubFor(get(anyUrl()).willReturn(okJson("{\"version\":\"999.0\",\"spark\":\"999.0\",\"Version\":\"999.0\",\"clusterInfo\":{\"hadoopVersion\":\"999.0\"},\"about\":{\"version\":\"999.0\"},\"beans\":[{\"Version\":\"999.0\"}]}")));assertNotEquals(HealthCheckResult.Status.HEALTHY,new RestDiagnosticConnector(config(service)).check().status(),service);
            server.resetAll();server.stubFor(get(anyUrl()).willReturn(ok("malformed-json")));assertEquals(HealthCheckResult.Status.UNHEALTHY,new RestDiagnosticConnector(config(service)).check().status(),service);
        }
    }
    @Test void configuredSemanticFailuresAreDegraded(){
        fixtures("yarn");reply("/ws/v1/cluster/metrics","{\"clusterMetrics\":{\"appsPending\":50,\"availableMB\":0,\"containersPending\":900}}");assertEquals(HealthCheckResult.Status.DEGRADED,new RestDiagnosticConnector(config("yarn")).check().status());
        server.resetAll();fixtures("spark");reply("/api/v1/applications/app-1/stages?limit=100","[{\"status\":\"FAILED\"}]");assertEquals(HealthCheckResult.Status.DEGRADED,new RestDiagnosticConnector(config("spark")).check().status());
        server.resetAll();fixtures("airflow");reply("/api/v1/health","{\"metadatabase\":{\"status\":\"healthy\"},\"scheduler\":{\"status\":\"unhealthy\"}}");assertEquals(HealthCheckResult.Status.DEGRADED,new RestDiagnosticConnector(config("airflow")).check().status());
    }
    @Test void missingConfigurationUnsupportedRangerAndUnsafeEndpointDoNotMakeNetworkCalls(){
        assertEquals(HealthCheckResult.Status.NOT_CONFIGURED,new RestDiagnosticConnector(new ConnectorConfig(new MockEnvironment(),"spark")).check().status());
        assertEquals(HealthCheckResult.Status.NOT_TESTED,new RestDiagnosticConnector(config("ranger")).check().status());assertTrue(server.getAllServeEvents().isEmpty());
        var env=new MockEnvironment().withProperty("CONNECTOR_SPARK_URL","http://remote.invalid").withProperty("CONNECTOR_SPARK_TOKEN","test-only");assertEquals("IllegalArgumentException",new RestDiagnosticConnector(new ConnectorConfig(env,"spark")).check().error_type());
    }
}
