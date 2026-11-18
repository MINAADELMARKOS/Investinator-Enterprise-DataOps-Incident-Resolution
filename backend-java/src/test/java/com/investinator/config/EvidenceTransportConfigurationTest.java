package com.investinator.config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EvidenceTransportConfigurationTest {
    @Test void officialTransportConstructsAndClosesWithoutNetworkCalls() throws Exception {
        try(var transport=new EvidenceConfiguration().evidenceTransport("http://127.0.0.1:9","test-reader","test-only-not-a-real-secret",true)) {
            assertNotNull(new org.opensearch.client.opensearch.OpenSearchClient(transport));
        }
    }
    @Test void nonLocalPlaintextUserinfoAndEnterpriseModeAreRejected() {
        var config=new EvidenceConfiguration();
        assertThrows(IllegalArgumentException.class,()->config.evidenceTransport("http://remote.example.com:9200","reader","test",true));
        assertThrows(IllegalArgumentException.class,()->config.evidenceTransport("http://user:password@127.0.0.1:9200","reader","test",true));
        assertThrows(IllegalArgumentException.class,()->config.evidenceTransport("https://remote.example.com:9200","reader","test",false));
    }
}
