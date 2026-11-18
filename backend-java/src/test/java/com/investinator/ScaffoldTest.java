package com.investinator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import com.openai.client.OpenAIClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"investinator.metrics.token=test-only-metrics", "server.address=127.0.0.1"})
class ScaffoldTest {
    @LocalServerPort int port;
    @Autowired ApplicationContext context;
    final HttpClient http = HttpClient.newHttpClient();
    HttpResponse<String> get(String path, String name, String value) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (name != null) request.header(name, value);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void livenessAndReadinessAreIndependentOfUnconfiguredConnectors() throws Exception {
        for (String path : Set.of("liveness", "readiness")) {
            var response = get("/actuator/health/" + path, null, null);
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("UP"));
            assertFalse(response.body().contains("components"));
        }
        assertTrue(context.getBeansOfType(OpenAIClient.class).isEmpty());
        assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
        assertTrue(context.getBeansOfType(org.flywaydb.core.Flyway.class).isEmpty());
    }
    @Test void platformReportsScaffoldWithoutInventingMeasurements() throws Exception {
        var response = get("/api/v1/platform/health", null, null);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("NOT_IMPLEMENTED"));
        assertTrue(response.body().contains("NOT_TESTED"));
        assertTrue(response.body().contains("\"score\":null"));
    }
    @Test void metricsRequireExplicitSecretAndOtherManagementRoutesAreDenied() throws Exception {
        assertEquals(403, get("/actuator/prometheus", null, null).statusCode());
        assertEquals(403, get("/actuator/prometheus", "Authorization", "Bearer wrong").statusCode());
        var response = get("/actuator/prometheus", "Authorization", "Bearer test-only-metrics");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("jvm_memory"));
        for (String path : Set.of("/actuator/env", "/actuator/health", "/api/v1/incidents"))
            assertEquals(403, get(path, null, null).statusCode());
    }
    @Test void requestIdsAreBoundedAndCannotInjectLogFields() throws Exception {
        assertEquals("safe-123", get("/api/v1/platform/health", "X-Request-ID", "safe-123")
            .headers().firstValue("X-Request-ID").orElseThrow());
        var id = get("/api/v1/platform/health", "X-Request-ID", "unsafe secret value")
            .headers().firstValue("X-Request-ID").orElseThrow();
        assertTrue(id.matches("[a-f0-9-]{36}"));
    }
}
