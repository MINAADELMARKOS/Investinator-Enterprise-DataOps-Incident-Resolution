package com.investinator.evidence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.opensearch.client.opensearch.OpenSearchClient;
@SpringBootTest(properties={"investinator.evidence.enabled=true","investinator.evidence.url=http://127.0.0.1:9",
    "investinator.evidence.username=test-reader","investinator.evidence.password=test-only-not-a-real-secret"})
class EvidenceEnabledContextTest {
    @Autowired OpenSearchClient client;
    @Autowired EvidenceSearch search;
    @Test void enabledSdkBeanIsFullyIntrospectableWithoutRemoteCalls(){assertNotNull(client);assertNotNull(search);}
}
