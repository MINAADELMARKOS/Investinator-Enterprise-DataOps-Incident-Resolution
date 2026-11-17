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
    @ConditionalOnProperty(name = "investinator.openai.enabled", havingValue = "true")
    OpenAIClient openAIClient(OpenAiProperties properties) { return OpenAIOkHttpClient.fromEnv(); }
}
