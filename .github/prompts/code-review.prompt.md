---
description: "Use when: reviewing Java Spring Boot code for correctness, security (OWASP 2025), performance, SonarQube rules, and Java 17+/Spring Boot 3.2+ standards — produces structured findings with severity, SonarQube rule IDs, and fix recommendations. Can fix selected issues after user confirmation."
agent: "code-review"
tools: [read, edit, search, terminal]
argument-hint: "File path or class name to review — e.g., 'ProductServiceImpl' or 'src/main/java/.../controller/OrderController.java'"
---

# Code Review

Perform a thorough **code review** of the specified Java Spring Boot code. Present findings interactively and fix selected issues after user confirmation.

## Review Checklist

### Correctness
- [ ] **S2259**: Null safety — all `Optional` values handled, no raw `.get()` calls
- [ ] Exception handling — custom exceptions, no generic `RuntimeException`
- [ ] **S2095**: Resource cleanup — streams, connections closed (try-with-resources)
- [ ] Transaction boundaries — `@Transactional` on service methods, `readOnly = true` for reads
- [ ] **S4973**: String comparison with `.equals()`, not `==`
- [ ] Concurrent safety — thread-safe singletons, no shared mutable state

### Security (OWASP Top 10 2025)
- [ ] **S3649**: No SQL injection — parameterized queries only
- [ ] **S2068/S6437**: No hardcoded secrets — use `@Value("${...}")`
- [ ] **S5131**: No XSS — sanitize user-controlled output
- [ ] **S5135**: No unsafe deserialization — use Jackson with explicit types
- [ ] **S4790**: No weak hashing (MD5/SHA-1) — use SHA-256 or BCrypt
- [ ] **S2245**: No `java.util.Random` for security — use `SecureRandom`
- [ ] **S4684**: No JPA entities exposed in API responses — use DTOs
- [ ] Input validation — `@Valid` on all request bodies
- [ ] No sensitive data in logs — no passwords, tokens, PII

### Performance
- [ ] No N+1 queries — `@EntityGraph` or `JOIN FETCH` for associations
- [ ] Pagination — all list endpoints use `Pageable`, no unbounded `findAll()`
- [ ] `FetchType.LAZY` default for all collections
- [ ] No unnecessary `findById` before `save` — use `getReferenceById`

### Java 17+ / Spring Boot 3.2+ / Hibernate 6
- [ ] Records for request/response DTOs
- [ ] `jakarta.*` imports (not `javax.*`)
- [ ] `ProblemDetail` for error responses (RFC 7807)
- [ ] Constructor injection only (no `@Autowired` fields)
- [ ] `SecurityFilterChain` (not `WebSecurityConfigurerAdapter`)
- [ ] Oracle `@SequenceGenerator` for IDs (not IDENTITY/AUTO)
- [ ] `ddl-auto=validate` (never `update`/`create` in production)

### Readability & SonarQube Code Smells
- [ ] **S3776**: Cognitive complexity ≤ 15 — extract methods, use guard clauses
- [ ] **S1192**: No duplicated string literals > 3 times — extract constant or enum
- [ ] **S106**: No `System.out.println` — use SLF4J
- [ ] **S1481**: No unused local variables
- [ ] **S1128**: No unused imports
- [ ] **S1874**: No deprecated API usage

### API Design
- [ ] RESTful URL patterns — `/api/v1/<resources>`, plural nouns
- [ ] Correct HTTP status codes — 201 for POST, 204 for DELETE
- [ ] `ResponseEntity<T>` for explicit status control
- [ ] `Location` header on 201 Created responses

## Output Format

Present findings in a numbered table:

```
| # | Severity | Rule | File:Line | Issue | Auto-fixable? |
|---|----------|------|-----------|-------|---------------|
| 1 | CRITICAL | S2259 | OrderService.java:45 | NPE risk | ✅ Yes |

**Which findings should I fix?** ("all", "1,2,3", "all Critical", "none — just review")
```

Severity levels: `CRITICAL` | `HIGH` | `MAJOR` | `MINOR` | `SUGGESTION`

After user confirms, apply fixes and verify with `mvn compile` and `mvn test`.
