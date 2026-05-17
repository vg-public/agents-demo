---
name: code-review
description: "Guide for performing thorough code reviews of Java Spring Boot applications with interactive fix capability. Covers correctness, security (OWASP 2025), performance, SonarQube rule catalogue, Java 17+/Spring Boot 3.2+/Hibernate 6 standards. Use when reviewing pull requests, auditing code, or fixing review findings."
---

# Code Review — Java Spring Boot

This skill guides the AI to perform structured, high-quality code reviews for Java Spring Boot REST API applications, with an interactive review-then-fix workflow.

## When to Use

- Reviewing a pull request or merge request
- Checking code quality before committing
- Auditing existing code for issues
- Reviewing someone else's implementation for feedback
- Fixing review findings after user confirmation

## Review-Then-Fix Workflow

1. **Review** — Scan all dimensions, produce structured findings with severity and SonarQube rule IDs.
2. **Present** — Show numbered findings table grouped by severity (Critical → High → Major → Minor).
3. **Confirm** — Ask user which findings to fix ("all", "1,2,3", "all Critical", "none").
4. **Fix** — Apply minimal, targeted fixes for confirmed findings. Verify with `mvn compile` and `mvn test`.

## Review Checklist

### 1. Correctness

- Does the code do what the story/ticket describes?
- Are all acceptance criteria addressed?
- Are edge cases handled (null, empty, boundary values, concurrent access)?
- Are error paths handled gracefully with custom exceptions?
- Is `@Transactional` used correctly (readOnly for reads, writable for writes)?
- Are JPA entity mappings correct (FetchType, cascade, orphanRemoval)?
- Does the `@Query` JPQL match the intended logic?
- **S2259**: Null pointer dereference — use `Optional.orElseThrow()`, never raw `.get()`
- **S2095**: Resource leaks — all `InputStream`, `Connection`, `Reader` in try-with-resources
- **S4973**: String comparison with `==` — use `.equals()`

### 2. Security (OWASP Top 10 2025)

- **A01 Broken Access Control**: `@PreAuthorize` on every endpoint, IDOR checks, CORS not `*`
- **A02 Cryptographic Failures**: No MD5/SHA-1 (S4790), no `new Random()` (S2245), `SecureRandom` required
- **A03 Injection**: No string concatenation in `@Query` (S3649), no `Runtime.exec()` with user input
- **A04 Insecure Design**: Rate limiting, bounded pagination, business validation
- **A05 Security Misconfiguration**: `ddl-auto=validate`, `show-sql=false`, no stack traces in responses
- **A06 Vulnerable Components**: Known CVEs in `pom.xml` dependencies
- **A07 Auth Failures**: Password policy, account lockout, JWT expiration
- **A08 Data Integrity**: No `ObjectInputStream` on untrusted data (S5135)
- **A09 Logging Failures**: No PII/passwords/tokens in logs
- **S2068/S6437**: No hardcoded credentials — use `@Value("${...}")`
- **S5131**: XSS — sanitize user-controlled output
- **S4502**: CSRF disabled — document stateless API exception or enable CSRF
- **S4684**: Never return JPA entities directly — use DTOs

### 3. Performance

- N+1 query patterns — missing `JOIN FETCH` or `@EntityGraph`
- `FetchType.EAGER` on collections — should be LAZY
- Missing pagination — `findAll()` without `Pageable`
- `spring.jpa.open-in-view=true` — should be `false`
- Missing database indexes for commonly queried columns
- Unbounded batch operations — no `@BatchSize` or chunking
- `findById` before `save` — prefer `getReferenceById` for associations

### 4. Java 17+ / Spring Boot 3.2+ / Hibernate 6 Standards

#### Java 17+ Patterns to Enforce
| Pattern | What to Check |
|---------|---------------|
| Records for DTOs | Request/response DTOs should be `record` types |
| Sealed classes | Exception hierarchies should use `sealed` |
| Pattern matching | `instanceof` pattern matching vs cast-after-check |
| Enhanced switch | `->` expressions vs fall-through `case:` |
| Text blocks | `"""` for multi-line strings (JPQL, JSON) |
| `Optional` | Never `.get()` without `.isPresent()`; prefer `.orElseThrow()` |

#### Spring Boot 3.2+ Conventions
| Convention | What to Check |
|-----------|---------------|
| Jakarta namespace | `jakarta.*` not `javax.*` |
| `ProblemDetail` | RFC 7807 responses, not custom error DTOs |
| Constructor injection | No `@Autowired` on fields |
| `SecurityFilterChain` | Not deprecated `WebSecurityConfigurerAdapter` |
| `@HttpExchange` | Declarative HTTP clients (not Feign) |
| `application.yml` | Secrets via `${ENV_VAR}`, not inline |

#### Hibernate 6 Best Practices
| Practice | What to Check |
|----------|---------------|
| `@SequenceGenerator` | Oracle sequences for IDs (not IDENTITY/AUTO) |
| `FetchType.LAZY` | Default for all associations |
| `@NaturalId` | Business keys (SKU, email) |
| `@Version` | Optimistic locking for concurrent entities |
| `@SQLRestriction` | Replaces deprecated `@Where` in Hibernate 6.3+ |
| `ddl-auto=validate` | Never `update`/`create` in production |

### 5. Readability & Maintainability

- Methods exceeding ~50 lines — extract sub-methods
- **S3776**: Cognitive complexity > 15 — use guard clauses, early returns, extract methods
- **S1192**: String literal duplicated > 3 times — extract to `static final` or enum
- **S106**: `System.out.println` — use SLF4J `log.info()` / `log.debug()`
- **S1481**: Unused local variable — remove
- **S1128**: Unused import — remove
- **S1144**: Unused private method — remove
- Deep nesting (>3 levels of if/loop) — flatten with guard clauses
- Code duplication across services — extract shared logic
- Missing Javadoc on public API methods

### 6. Testing

- Are new features covered by tests?
- Do tests cover both happy path and error cases?
- Are test assertions specific (AssertJ `assertThat`)?
- Are mocks used appropriately (mock repos, not the code under test)?
- Are `@Nested` classes used for grouping related tests?
- **S2699**: Test method without assertion — add `assertThat(...)` assertion

### 7. API Design

- Are HTTP methods and status codes used correctly?
- Is the API consistent with existing endpoints (`/api/v1/<resource>`)?
- Is `@Valid` present on all `@RequestBody` parameters?
- Is backward compatibility maintained?
- Does `POST` return 201 with `Location` header?
- Does `DELETE` return 204 No Content?

## SonarQube Quick Reference

### Must-Fix (Blocks Merge)
| Rule | Category | Issue |
|------|----------|-------|
| S2259 | Bug | Null pointer dereference |
| S2095 | Bug | Resource leak (unclosed stream/connection) |
| S3649 | Vulnerability | SQL injection |
| S2068 | Vulnerability | Hardcoded credentials |
| S5131 | Vulnerability | XSS |
| S5135 | Vulnerability | Unsafe deserialization |
| S4790 | Security | Weak hash (MD5/SHA-1) |
| S2245 | Security | `java.util.Random` for security |

### Must-Fix Before Release
| Rule | Category | Issue |
|------|----------|-------|
| S3776 | Code Smell | Cognitive complexity > 15 |
| S1192 | Code Smell | Duplicated string > 3 times |
| S4684 | Security | JPA entity as API response |
| S1874 | Code Smell | Deprecated API usage |
| S1948 | Bug | Non-serializable field in serializable class |

### Fix in Sprint
| Rule | Category | Issue |
|------|----------|-------|
| S106 | Code Smell | `System.out.println` |
| S1481 | Code Smell | Unused variable |
| S1128 | Code Smell | Unused import |
| S4973 | Bug | String `==` comparison |
| S1155 | Code Smell | `list.size() == 0` |
| S2142 | Bug | `InterruptedException` not re-interrupted |

## Review Communication Style

- Be specific: point to the exact file, line, and SonarQube rule ID.
- Suggest a fix with concrete code, not just a description.
- Severity levels: `[CRITICAL]`, `[HIGH]`, `[MAJOR]`, `[MINOR]`, `[SUGGESTION]`.
- Acknowledge what's done well — positive feedback helps.

## Review Output Format

```markdown
## Code Review Summary

**Review Mode**: File-based / Git diff-based (branch: feature/xxx vs main)
**Files Reviewed**: list
**Overall**: Approve / Request Changes / Comment

### Findings

| # | Severity | Rule | File:Line | Issue | Auto-fixable? |
|---|----------|------|-----------|-------|---------------|
| 1 | CRITICAL | S2259 | OrderService.java:45 | NPE risk | ✅ Yes |
| 2 | HIGH | S3776 | PaymentService.java:89 | Complexity 22 | ✅ Yes |

**Which findings should I fix?** ("all", "1,2", "all Critical", "none — just review")

### Positive Notes
- Good test coverage for edge cases.
- Clean separation of concerns in the service layer.
```

## Related Skills

- `#skill:security-code-review` — OWASP Top 10 2025 deep-dive with code examples
- `#skill:sonarqube-remediation` — Full SonarQube fix patterns and quality gate config
- `#skill:performance-optimization` — JPA, HikariCP, caching optimization patterns
