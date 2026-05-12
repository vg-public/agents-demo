---
description: "Use when: setting up global exception handling, customizing error responses, or adding new custom exceptions — generates @RestControllerAdvice and exception hierarchy."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Describe what to do — e.g., 'Set up global exception handler' or 'Add custom InventoryException'"
---

# Exception Handling Setup

Generate or update the **global exception handler** and **custom exception hierarchy** for a Spring Boot REST API using RFC 7807 ProblemDetail.

## Exception Hierarchy

```
BusinessException (abstract, base)
├── ResourceNotFoundException     — 404
├── DuplicateResourceException    — 409
├── InvalidOperationException     — 422
└── (custom domain exceptions)    — 4xx
```

### Base Exception — `exception/BusinessException.java`

```java
/**
 * Base exception for all business-rule violations.
 */
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;

    protected BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

### ResourceNotFoundException

```java
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super("%s not found with id: %d".formatted(resourceName, id), "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, String field, String value) {
        super("%s not found with %s: %s".formatted(resourceName, field, value), "RESOURCE_NOT_FOUND");
    }
}
```

### DuplicateResourceException

```java
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String resourceName, String field, String value) {
        super("%s already exists with %s: %s".formatted(resourceName, field, value), "DUPLICATE_RESOURCE");
    }
}
```

## Global Exception Handler — `exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleConflict(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Resource");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                (a, b) -> a));

        log.warn("Validation failed: {}", fieldErrors);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setProperty("fieldErrors", fieldErrors);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        // Never expose stack traces or internal messages
        return problem;
    }
}
```

## Rules

- **Never** expose stack traces, SQL errors, or class names in API responses
- Always log the full exception server-side (`log.error` for 500s, `log.warn` for 4xx)
- Use `ProblemDetail` (RFC 7807) — not custom error DTOs
- Keep the handler in `exception/` package alongside the exception classes
- Every custom exception must extend `BusinessException`
