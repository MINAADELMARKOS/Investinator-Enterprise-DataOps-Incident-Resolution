package com.investinator.evidence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties="investinator.evidence.token=test-only-evidence-credential")
@AutoConfigureMockMvc
class EvidenceApiSecurityTest {
    @Autowired MockMvc mvc;
    @Test void credentialsAreRequiredAndAbsentBackendIsTruthfullyNotConfigured() throws Exception {
        mvc.perform(get("/api/v1/evidence/search")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/evidence/search").header("Authorization","Bearer wrong")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/evidence/search").header("Authorization","Bearer test-only-evidence-credential"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value("NOT_CONFIGURED"));
        mvc.perform(get("/api/v1/evidence/health").header("Authorization","Bearer test-only-evidence-credential"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value("NOT_CONFIGURED"));
    }
    @Test void callerCannotSupplyTenantIndexUrlOrDslEvenWithAValidCredential() throws Exception {
        for(String key:java.util.List.of("tenant_id","index","url","dsl"))
            mvc.perform(get("/api/v1/evidence/search").param(key,"untrusted").header("Authorization","Bearer test-only-evidence-credential"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/evidence/search").contentType("application/json").content("{\"delete\":true}"))
            .andExpect(status().isForbidden());
    }
}
