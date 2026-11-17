package com.investinator;
import com.investinator.config.OpenAiProperties;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConnectorConfigurationTest {
    @Test void disabledConnectorRequiresNoModelButEnabledConfigurationIsValidated() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new OpenAiProperties(false, "", 3)).isEmpty());
            assertTrue(validator.validate(new OpenAiProperties(true, "", 3)).isEmpty());
            assertFalse(validator.validate(new OpenAiProperties(false, "", 0)).isEmpty());
            assertFalse(validator.validate(new OpenAiProperties(false, "", 11)).isEmpty());
        }
    }
}
