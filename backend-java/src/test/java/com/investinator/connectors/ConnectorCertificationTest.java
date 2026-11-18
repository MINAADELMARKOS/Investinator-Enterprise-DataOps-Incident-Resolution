package com.investinator.connectors;
import static org.junit.jupiter.api.Assertions.*;
import static com.investinator.domain.Json.*;
import com.investinator.health.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.util.*;
class ConnectorCertificationTest {
    @Test void allNativeRestAndOptionalTargetsNeedIndependentCertification(){var registry=new ConnectorRegistry(new MockEnvironment(),new HealthSettings(new MockEnvironment()));var result=ConnectorCertification.assess("tenant-a",registry.descriptors(),List.of());assertEquals(14,rows(result.get("rows")).size());for(var row:rows(result.get("rows"))){assertEquals("tenant-a",row.get("tenant_id"));assertEquals("NOT_CERTIFIED",row.get("enterprise_status"));assertEquals(false,row.get("native_ingestion_authorized"));}assertEquals("BLOCKED",result.get("production_certificate"));}
    @Test void mockedHealthyAndMissingCredentialsCannotBecomePrivateCertificate(){var descriptor=new DiagnosticConnector.Descriptor("kafka","native","KRAFT only","SASL_TLS",Set.of(DiagnosticConnector.Capability.health),List.of());for(var status:List.of(HealthCheckResult.Status.HEALTHY,HealthCheckResult.Status.NOT_CONFIGURED,HealthCheckResult.Status.UNKNOWN)){var check=HealthCheckResult.of("kafka","health",status,HealthCheckResult.SourceMode.SIMULATED,"protocol fixture","fixture","read only","",false,Map.of("data_source_mode","SIMULATED","version","4.3"));var row=rows(ConnectorCertification.assess("demo",List.of(descriptor),List.of(check)).get("rows")).getFirst();assertEquals(status==HealthCheckResult.Status.NOT_CONFIGURED?"NOT_CONFIGURED":"NOT_CERTIFIED",row.get("enterprise_status"));assertEquals(false,row.get("native_ingestion_authorized"));}}
}
