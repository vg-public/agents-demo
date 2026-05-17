package com.epam.agents.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that converts application exceptions to RFC 7807
 * {@link ProblemDetail} responses.
 *
 * <p>
 * Error messages deliberately omit stack traces and PII.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERROR_CODE_PROPERTY = "errorCode";
    private static final String TIMESTAMP_PROPERTY = "timestamp";

    /**
     * Handles {@link ResourceNotFoundException} — returns HTTP 404.
     *
     * @param ex
     *            the exception
     * @return a ProblemDetail with status 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setProperty(ERROR_CODE_PROPERTY, ex.getErrorCode());
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    /**
     * Handles {@link DuplicateResourceException} — returns HTTP 409.
     *
     * @param ex
     *            the exception
     * @return a ProblemDetail with status 409
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Resource");
        problem.setProperty(ERROR_CODE_PROPERTY, ex.getErrorCode());
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    /**
     * Handles bean-validation failures from {@code @Valid} on request bodies — returns HTTP 400.
     *
     * @param ex
     *            the validation exception
     * @return a ProblemDetail with per-field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setProperty(ERROR_CODE_PROPERTY, "VALIDATION_ERROR");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    /**
     * Handles constraint-violation exceptions — returns HTTP 400.
     *
     * @param ex
     *            the constraint violation
     * @return a ProblemDetail with per-constraint error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Constraint Violation");
        problem.setProperty(ERROR_CODE_PROPERTY, "CONSTRAINT_VIOLATION");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());

        Map<String, String> violations = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> violations.put(v.getPropertyPath().toString(), v.getMessage()));
        problem.setProperty("violations", violations);
        return problem;
    }

    /**
     * Handles database constraint violations — returns HTTP 409.
     *
     * @param ex
     *            the data integrity exception
     * @return a ProblemDetail with status 409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A data integrity constraint was violated");
        problem.setTitle("Data Integrity Violation");
        problem.setProperty(ERROR_CODE_PROPERTY, "DATA_INTEGRITY_VIOLATION");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    /**
     * Catch-all handler for unexpected exceptions — returns HTTP 500.
     *
     * @param ex
     *            the unexpected exception
     * @return a generic ProblemDetail with status 500
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setProperty(ERROR_CODE_PROPERTY, "INTERNAL_ERROR");
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }
}
