package com.investinator.model;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
public class ProviderConfigured implements Condition {
    public boolean matches(ConditionContext context,AnnotatedTypeMetadata metadata){var env=context.getEnvironment();return env.getProperty("investinator.openai.enabled",Boolean.class,false)&&!env.getProperty("OPENAI_API_KEY","").isBlank()&&!env.getProperty("investinator.openai.model","").isBlank();}
}
