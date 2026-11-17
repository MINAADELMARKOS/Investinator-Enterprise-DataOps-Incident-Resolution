package com.investinator.config;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@Validated
@ConfigurationProperties("investinator.openai")
public record OpenAiProperties(boolean enabled, String model, @Min(1) @Max(10) int maxRequests) {
    @AssertTrue(message = "An enabled OpenAI connector requires a backend model setting")
    public boolean isConfigurationValid() { return !enabled || (model != null && !model.isBlank()); }
}
