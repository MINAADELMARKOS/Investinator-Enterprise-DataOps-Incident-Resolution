package com.investinator.api;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    org.springframework.http.ResponseEntity<java.util.Map<String,Object>> notFound(Exception ignored) {
        return org.springframework.http.ResponseEntity.status(404).body(java.util.Map.of("detail","Not Found"));
    }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    org.springframework.http.ResponseEntity<java.util.Map<String,Object>> method(Exception ignored) {
        return org.springframework.http.ResponseEntity.status(405).body(java.util.Map.of("detail","Method Not Allowed"));
    }
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})
    org.springframework.http.ResponseEntity<java.util.Map<String,Object>> parameter(Exception ignored) {
        return org.springframework.http.ResponseEntity.unprocessableEntity().body(java.util.Map.of("detail","Request validation failed"));
    }
    @ExceptionHandler(ApiFailure.class)
    org.springframework.http.ResponseEntity<java.util.Map<String,Object>> domain(ApiFailure failure) {
        return org.springframework.http.ResponseEntity.status(failure.status).body(java.util.Map.of("detail",failure.getMessage()));
    }
    @ExceptionHandler({org.springframework.web.method.annotation.HandlerMethodValidationException.class})
    org.springframework.http.ResponseEntity<java.util.Map<String,Object>> methodValidation(Exception ignored) {
        return org.springframework.http.ResponseEntity.unprocessableEntity().body(java.util.Map.of("detail","Request validation failed"));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ProblemDetail invalid(Exception ignored) { return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Request validation failed"); }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    org.springframework.http.ResponseEntity<java.util.Map<String,Object>> malformed(Exception ignored) {
        return org.springframework.http.ResponseEntity.unprocessableEntity().body(java.util.Map.of("detail","Request validation failed"));
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    org.springframework.http.ResponseEntity<java.util.Map<String,Object>> database(Exception ignored) {
        return org.springframework.http.ResponseEntity.status(503).body(java.util.Map.of("detail","Database operation unavailable"));
    }
    @ExceptionHandler(Exception.class)
    ProblemDetail failure(Exception ignored) { return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Request failed"); }
}
