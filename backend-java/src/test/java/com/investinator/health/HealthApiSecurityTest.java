package com.investinator.health;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"investinator.health.token=test-only-health-token","investinator.health.timeout-ms=100","investinator.health.retries=0"})
@AutoConfigureMockMvc
class HealthApiSecurityTest {
    @Autowired MockMvc mvc;
    @Test void privateReportRequiresScopedTokenAndReportsNoPaidCalls() throws Exception {
        mvc.perform(get("/api/v1/health/integrations")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/health/integrations").header("Authorization","Bearer wrong")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/health/integrations").param("service","gpt").header("Authorization","Bearer test-only-health-token"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.checks[0].details.paid_call_made").value(false));
        mvc.perform(get("/api/v1/health/integrations").param("service","unknown").header("Authorization","Bearer test-only-health-token")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
