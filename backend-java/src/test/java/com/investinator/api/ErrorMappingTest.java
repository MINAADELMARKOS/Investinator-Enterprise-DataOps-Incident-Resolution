package com.investinator.api;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.http.ProblemDetail;

class ErrorMappingTest {
    @Test void exceptionResponsesDoNotLeakProviderOrCredentialDetails() throws Exception {
        var handler = new ApiExceptionHandler();
        var detail = handler.failure(new RuntimeException("sensitive-provider-value"));
        assertEquals(500, detail.getStatus());
        assertEquals("Request failed", detail.getDetail());
        assertEquals(400, handler.invalid(
            new ConstraintViolationException("sensitive-input", Set.of())).getStatus());
    }
}
