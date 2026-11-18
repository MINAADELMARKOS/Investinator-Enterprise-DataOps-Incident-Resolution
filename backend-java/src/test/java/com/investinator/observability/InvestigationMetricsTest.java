package com.investinator.observability;
import com.investinator.chat.ChatGateway;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class InvestigationMetricsTest {
    ChatGateway.Result result(String status,int requests,int input,int output){return new ChatGateway.Result("deterministic-fallback",status,"private-text",null,List.of(),List.of(),requests,input,output,"private-model");}
    @Test void missingUsageIsUnknownAndUnconfiguredTurnIsNotProviderTraffic(){
        var registry=new SimpleMeterRegistry();var metrics=new InvestigationMetrics(registry);
        metrics.record(result("NOT_CONFIGURED",0,0,0),5);assertEquals(0,registry.get("investinator.provider.requests").counter().count());assertNull(registry.find("investinator.provider.tokens").counter());
        metrics.record(result("PROVIDER_UNAVAILABLE",1,0,0),10);assertEquals(1,registry.get("investinator.provider.usage.unknown").counter().count());
        metrics.record(result("RATE_LIMITED",1,20,3),12);assertEquals(20,registry.get("investinator.provider.tokens").tag("direction","input").counter().count());
        assertTrue(registry.getMeters().stream().flatMap(m->m.getId().getTags().stream()).noneMatch(t->t.getValue().contains("private")));
    }
}
