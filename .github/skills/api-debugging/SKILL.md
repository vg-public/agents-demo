---
name: api-debugging
description: "Guide for diagnosing and fixing Spring Boot REST API issues including 4xx/5xx errors, CORS problems, authentication failures, serialization errors, and slow endpoints. Use when a Java API is not behaving as expected."
---

# API Debugging — Spring Boot REST APIs

This skill guides the AI through systematic debugging of REST API issues in Java Spring Boot applications with Spring Data JPA and Oracle Database.

## When to Use

- API endpoint returning unexpected status codes (4xx, 5xx)
- CORS errors in browser console
- Authentication or authorization failures
- Request/response body serialization issues
- Slow API response times
- API not reachable or timing out

## Debugging Workflow

### 1. Reproduce the Issue

- Identify the exact request: method, URL, headers, query params, body.
- Use `curl`, Postman, or the browser Network tab.
- Note the response: status code, headers, body, response time.

```bash
# Test GET endpoint
curl -v -X GET http://localhost:8080/api/v1/products/1 \
  -H "Authorization: Bearer <token>"

# Test POST with body
curl -v -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"sku": "SKU-001", "name": "Widget", "price": 9.99, "categoryId": 1}'

# Check health
curl -I http://localhost:8080/actuator/health
```

### 2. Check Server Logs

- Look at the application logs for stack traces or error messages.
- Enable DEBUG logging for relevant packages:
```yaml
logging.level:
  com.example: DEBUG
  org.springframework.web: DEBUG
  org.hibernate.SQL: DEBUG
```

### 3. Diagnose by Status Code

| Status Code | Common Causes | Where to Look |
|-------------|---------------|---------------|
| **400 Bad Request** | `@Valid` validation failed, type mismatch in `@RequestBody`, missing required field | Request DTO validation annotations, `MethodArgumentNotValidException` |
| **401 Unauthorized** | Missing or expired JWT, wrong credentials, Spring Security filter chain | `SecurityFilterChain`, token validation, `AuthenticationEntryPoint` |
| **403 Forbidden** | Valid token but insufficient role/authority | `@PreAuthorize`, `SecurityFilterChain`, role configuration |
| **404 Not Found** | Wrong URL, `@RequestMapping` mismatch, resource doesn't exist | Controller `@RequestMapping`, service `orElseThrow()` |
| **405 Method Not Allowed** | Wrong HTTP method (GET vs POST) | Controller method annotations (`@GetMapping` vs `@PostMapping`) |
| **409 Conflict** | Duplicate SKU/name, `OptimisticLockException`, unique constraint violation | `DuplicateResourceException`, `DataIntegrityViolationException` |
| **415 Unsupported Media Type** | Missing `Content-Type: application/json` header | Request headers, `consumes` attribute on mapping |
| **422 Unprocessable Entity** | Business rule violation | `BusinessRuleViolationException`, service-layer validation |
| **500 Internal Server Error** | Unhandled exception, NPE, database error | Stack trace in logs, `GlobalExceptionHandler` coverage |

### 4. Common Spring Boot API Issues

#### CORS Errors
- **Symptom**: Browser shows "Access to fetch at ... has been blocked by CORS policy"
- **Fix**: Configure `WebMvcConfigurer`:
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://yourdomain.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

#### Serialization Issues
| Problem | Cause | Fix |
|---------|-------|-----|
| Response missing fields | Field not in DTO, wrong `@JsonProperty` | Check DTO record components match expected JSON |
| Circular reference | `@ManyToOne`/`@OneToMany` bidirectional | Use DTOs instead of returning entities directly |
| Date format wrong | No Jackson date config | Add `spring.jackson.date-format` or `@JsonFormat` |
| Empty response body | Returning `void` or entity without `@ResponseBody` | Return `ResponseEntity<T>` |
| `HttpMessageNotReadableException` | Malformed JSON, type mismatch | Validate JSON structure matches DTO types |

#### Bean Validation Not Triggering
- **Cause**: Missing `@Valid` on `@RequestBody` parameter
- **Fix**: `public ResponseEntity<T> create(@Valid @RequestBody CreateProductRequest request)`

#### Missing Exception Handler
- **Symptom**: 500 error with stack trace in response
- **Fix**: Add handler in `@RestControllerAdvice`:
```java
@ExceptionHandler(ResourceNotFoundException.class)
public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
}
```

#### Spring Security Blocking Endpoints
- **Symptom**: 401/403 on endpoints that should be public
- **Fix**: Configure `SecurityFilterChain`:
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/public/**", "/actuator/health").permitAll()
            .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
            .anyRequest().authenticated())
        .build();
}
```

### 5. Performance Debugging

- **Slow queries**: Enable `org.hibernate.SQL=DEBUG`, check for N+1 queries
- **Large payloads**: Implement pagination with `Pageable` — never return unbounded lists
- **Missing caching**: Add `@Cacheable` for read-heavy, rarely-changing data
- **Connection pool exhaustion**: Check HikariCP metrics via Actuator

## Anti-Patterns to Avoid

- Do NOT return 200 with an error message in the body — use proper HTTP status codes
- Do NOT swallow exceptions with empty catch blocks — always log or rethrow
- Do NOT expose stack traces to the client — use `GlobalExceptionHandler` with `ProblemDetail`
- Do NOT debug by guessing — always check logs and reproduce first
- Do NOT use `spring.jpa.open-in-view=true` — it masks lazy-loading issues
