package com.investinator.observability;
import com.investinator.chat.ChatGateway;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
/** Fixed low-cardinality dimensions. Never records prompts, identifiers, models or credentials. */
@Component
public class InvestigationMetrics {
    private final MeterRegistry registry;
    public InvestigationMetrics(MeterRegistry registry){this.registry=registry;}
    public void record(ChatGateway.Result result,long elapsedMillis){
        String status=result.provider_status();
        String outcome=status.equals("OK")?"ok":status.equals("NOT_CONFIGURED")?"not_configured":status.contains("BUDGET")?"budget":status.equals("RATE_LIMITED")?"provider_rate_limited":status.equals("SATURATED")?"saturated":status.contains("POLICY")||status.contains("CITATION")?"policy_denied":"unavailable";
        Timer.builder("investinator.chat.turn").tag("response",result.response_mode().equals("gpt")?"gpt":"fallback").tag("outcome",outcome).serviceLevelObjectives(java.time.Duration.ofSeconds(1),java.time.Duration.ofSeconds(8),java.time.Duration.ofSeconds(20)).register(registry).record(Math.max(0,elapsedMillis),TimeUnit.MILLISECONDS);
        registry.counter("investinator.provider.requests").increment(result.requests());
        if(result.input_tokens()+result.output_tokens()>0){registry.counter("investinator.provider.tokens","direction","input").increment(result.input_tokens());registry.counter("investinator.provider.tokens","direction","output").increment(result.output_tokens());}
        else if(result.requests()>0)registry.counter("investinator.provider.usage.unknown").increment();
    }
}
