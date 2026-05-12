---
description: "Use when: reviewing Java Spring Boot code for correctness, security, performance, and best practices — produces structured findings with severity and fix recommendations."
agent: "agent"
tools: [read, search]
argument-hint: "File path or class name to review — e.g., 'ProductServiceImpl' or 'src/main/java/.../controller/OrderController.java'"
---

# Code Review

Perform a thorough **code review** of the specified Java Spring Boot code.

## Review Checklist

### Correctness
- [ ] Null safety — all `Optional` values handled, no raw `.get()` calls
- [ ] Exception handling — custom exceptions, no generic `RuntimeException`
- [ ] Transaction boundaries — `@Transactional` on service methods, `readOnly = true` for reads
- [ ] Resource cleanup — streams, connections closed (try-with-resources)
- [ ] Concurrent safety — thread-safe singletons, no shared mutable state

### Security (OWASP Top 10)
- [ ] No SQL injection — parameterized queries only, no string concatenation in `@Query`
- [ ] No hardcoded secrets — passwords, API keys, connection strings
- [ ] Input validation — `@Valid` on all request bodies, proper constraints
- [ ] No sensitive data in logs — no passwords, tokens, PII in log statements
- [ ] Proper error responses — no stack traces exposed to clients

### Performance
- [ ] No N+1 queries — check `@ManyToOne`/`@OneToMany` relationships, use `@EntityGraph` or `JOIN FETCH`
- [ ] Pagination — all list endpoints use `Pageable`, no `findAll()` without paging
- [ ] Lazy loading — default `FetchType.LAZY` for collections, `EAGER` only when justified
- [ ] No unnecessary database calls — check for redundant `findById` before save

### Spring Boot Best Practices
- [ ] Constructor injection — no `@Autowired` on fields
- [ ] Thin controllers — no business logic, delegation only
- [ ] Service interface + implementation pattern
- [ ] DTOs — never expose entities directly in API responses
- [ ] MapStruct for mapping — no manual getter/setter chains
- [ ] `@Transactional` scope — on service layer, not controller or repository

### API Design
- [ ] RESTful URL patterns — `/api/v1/<resources>`, plural nouns
- [ ] Correct HTTP status codes — 201 for POST, 204 for DELETE, 404 for not found
- [ ] `ResponseEntity<T>` for explicit status control
- [ ] `Location` header on 201 Created responses
- [ ] RFC 7807 ProblemDetail for error responses

## Output Format

For each finding, report:

```
### [SEVERITY] — Short title

**File**: `path/to/File.java:lineNumber`
**Issue**: What's wrong and why it matters
**Fix**: Specific code change to resolve it
```

Severity levels: `CRITICAL` | `WARNING` | `SUGGESTION`

At the end, provide a **summary** with counts by severity.
