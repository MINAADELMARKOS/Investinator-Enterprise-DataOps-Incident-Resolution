package com.investinator.config;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class ConnectorConfiguration {
    // No network call or paid probe on startup. Model and credentials remain backend environment settings.
    @Bean(destroyMethod = "close")
    @org.springframework.context.annotation.Conditional(com.investinator.model.ProviderConfigured.class)
    OpenAIClient openAIClient(OpenAiProperties properties,org.springframework.core.env.Environment env) { return com.investinator.model.ProviderClients.create(env); }
}
