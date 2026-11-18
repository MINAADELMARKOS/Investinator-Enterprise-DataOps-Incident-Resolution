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
    static class Exporter implements io.opentelemetry.sdk.trace.export.SpanExporter {
        final java.util.List<io.opentelemetry.sdk.trace.data.SpanData> spans=new java.util.ArrayList<>();
        public io.opentelemetry.sdk.common.CompletableResultCode export(java.util.Collection<io.opentelemetry.sdk.trace.data.SpanData> data){spans.addAll(data);return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess();}
        public io.opentelemetry.sdk.common.CompletableResultCode flush(){return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess();}
        public io.opentelemetry.sdk.common.CompletableResultCode shutdown(){return io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess();}
    }
    @Test void actualSdkSpanEndsWithoutCapturingQuestionOrExceptionSecrets(){
        var exporter=new Exporter();
        try(var provider=io.opentelemetry.sdk.trace.SdkTracerProvider.builder().addSpanProcessor(io.opentelemetry.sdk.trace.export.SimpleSpanProcessor.create(exporter)).build()){
            var metrics=new InvestigationMetrics(new SimpleMeterRegistry(),provider.get("fixture"));
            metrics.traceTurn(()->{assertTrue(io.opentelemetry.api.trace.Span.current().getSpanContext().isValid());return result("NOT_CONFIGURED",0,0,0);});
            assertThrows(IllegalStateException.class,()->metrics.traceTurn(()->{throw new IllegalStateException("PRIVATE_EXCEPTION");}));
            assertEquals(2,exporter.spans.size());assertTrue(exporter.spans.stream().allMatch(s->s.hasEnded()));
            assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR,exporter.spans.get(1).getStatus().getStatusCode());
            assertFalse(exporter.spans.toString().contains("PRIVATE_EXCEPTION"));assertFalse(exporter.spans.toString().contains("private-model"));
        }
    }
}
