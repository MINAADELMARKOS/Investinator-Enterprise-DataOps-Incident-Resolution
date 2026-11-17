package com.investinator.api;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ProblemDetail invalid(Exception ignored) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed"); }
    @ExceptionHandler(Exception.class)
    ProblemDetail failure(Exception ignored) { return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Request failed"); }
}
