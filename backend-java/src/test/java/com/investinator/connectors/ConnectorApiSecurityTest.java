package com.investinator.connectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"CONNECTOR_API_TOKEN=test-only-diagnostic-reader","investinator.demo-tenant=tenant-a","CONNECTOR_ALLOWED_SERVICES=spark,hive","investinator.health.timeout-ms=100","investinator.health.retries=0"})
@AutoConfigureMockMvc
class ConnectorApiSecurityTest {
    @Autowired MockMvc mvc;
    @Test void scopeIsRequiredCallerCannotChooseUrlSqlTenantOrCommandAndWritesAreDenied()throws Exception {
        mvc.perform(get("/api/v1/connectors")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/connectors").header("Authorization","Bearer wrong")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/connectors").header("Authorization","Bearer test-only-diagnostic-reader")).andExpect(status().isOk()).andExpect(jsonPath("$.scope").value("diagnostics.read"));
        for(String key:java.util.List.of("url","sql","command","tenant_id"))mvc.perform(get("/api/v1/connectors/spark/health").param(key,"untrusted").header("Authorization","Bearer test-only-diagnostic-reader")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/connectors/spark/health").header("Authorization","Bearer test-only-diagnostic-reader")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NOT_CONFIGURED"));
        mvc.perform(get("/api/v1/connectors/spark/logs").header("Authorization","Bearer test-only-diagnostic-reader")).andExpect(status().isOk()).andExpect(jsonPath("$.error_type").value("UNSUPPORTED_CAPABILITY"));
        mvc.perform(get("/api/v1/connectors/kafka/health").header("Authorization","Bearer test-only-diagnostic-reader")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/connectors/spark/health").header("Authorization","Bearer test-only-diagnostic-reader")).andExpect(status().isForbidden());
    }
    @Test void certificationInventoryIsAuthenticatedTenantBoundAndCannotEnableIngestion()throws Exception{
        mvc.perform(get("/api/v1/connectors/certification")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/connectors/certification").param("tenant_id","other").header("Authorization","Bearer test-only-diagnostic-reader")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/connectors/certification").header("Authorization","Bearer test-only-diagnostic-reader")).andExpect(status().isOk()).andExpect(jsonPath("$.tenant_id").value("tenant-a")).andExpect(jsonPath("$.production_certificate").value("BLOCKED")).andExpect(jsonPath("$.native_ingestion_authorized").value(false));
        mvc.perform(get("/api/v1/connectors").header("Authorization","Bearer test-only-diagnostic-reader")).andExpect(status().isOk()).andExpect(jsonPath("$.tenant").value("tenant-a"));
    }
}
