package com.investinator.security;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"investinator.demo-mode=false","investinator.persistence.enabled=false","METRICS_TOKEN=fixture-metrics","HEALTH_API_TOKEN=fixture-health","CONNECTOR_API_TOKEN=fixture-connector","REMEDIATION_APPROVE_TOKEN=fixture-approve"})
@AutoConfigureMockMvc
class EnterpriseIngressTest {
    @Autowired MockMvc mvc;
    @Test void nonDemoBlocksPlatformAndBusinessDataEvenWithDemoCapabilities()throws Exception {
        for(String route:new String[]{"/api/v1/platform/health","/api/v1/incidents","/api/v1/audit","/api/v1/investigator/conversations","/api/v1/connectors","/api/v1/health/integrations","/api/v1/knowledge/search","/api/v1/monitor/status"})
            mvc.perform(get(route).header("Authorization","Bearer fixture-connector").header("X-Tenant-ID","other")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/remediation/plans/any/approval").header("Authorization","Bearer fixture-approve").contentType("application/json").content("{\"decision\":\"APPROVE\"}")).andExpect(status().isForbidden());
    }
    @Test void probesAreMinimalAndMetricsNeverPublic()throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk()).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/prometheus").header("Authorization","Bearer fixture-metrics")).andExpect(status().isOk());
    }
}
