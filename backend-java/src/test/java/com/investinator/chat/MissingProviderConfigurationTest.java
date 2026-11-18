package com.investinator.chat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import com.openai.client.OpenAIClient;
@SpringBootTest(properties={"investinator.openai.enabled=true","investinator.openai.model=fixture-model","OPENAI_API_KEY="})
class MissingProviderConfigurationTest {
    @Autowired ApplicationContext context;
    @Test void absentOptionalKeyNeverBlocksCoreStartupOrCreatesProviderClient(){assertTrue(context.getBeansOfType(OpenAIClient.class).isEmpty());}
}
