package com.investinator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.flywaydb.core.Flyway;
import org.apache.kafka.clients.admin.AdminClientConfig;

class DependencyCompatibilityTest {
    @Test void vendorTypesLoadAndClientConstructionDoesNotRequirePaidNetworkCalls() throws Exception {
        // Construction only: no OpenAI, Kafka, search or database request is made.
        var client = OpenAIOkHttpClient.builder().apiKey("test-only-not-a-real-key").build();
        try { assertNotNull(client.responses()); } finally { client.close(); }
        assertEquals(1, SearchRequest.of(b -> b.index("logs-test").size(1)).size());
        assertNotNull(Flyway.configure().locations("classpath:unused-migrations").load());
        assertNotNull(new AdminClientConfig(java.util.Map.of("bootstrap.servers", "127.0.0.1:9092")));
    }
    @Test void standaloneWireMockIsCompatibleWithTheTestRuntime() throws Exception {
        var server = new WireMockServer(0);
        try {
            server.start();
            server.stubFor(get(urlEqualTo("/fixture")).willReturn(okJson("{\"mode\":\"SIMULATED\"}")));
            var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + server.port() + "/fixture")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("SIMULATED"));
        } finally { server.stop(); }
    }
}
