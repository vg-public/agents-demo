---
name: code-review
description: "Guide for performing thorough code reviews of Java Spring Boot applications focusing on correctness, security, performance, readability, and best practices. Use when reviewing pull requests or code changes."
---

# Code Review — Java Spring Boot

This skill guides the AI to perform structured, high-quality code reviews for Java Spring Boot REST API applications.

## When to Use

- Reviewing a pull request or merge request
- Checking code quality before committing
- Auditing existing code for issues
- Reviewing someone else's implementation for feedback

## Review Checklist

### 1. Correctness

- Does the code do what the story/ticket describes?
- Are all acceptance criteria addressed?
- Are edge cases handled (null, empty, boundary values, concurrent access)?
- Are error paths handled gracefully with custom exceptions?
- Is `@Transactional` used correctly (readOnly for reads, writable for writes)?
- Are JPA entity mappings correct (FetchType, cascade, orphanRemoval)?
- Does the `@Query` JPQL match the intended logic?

### 2. Security (OWASP Top 10)

- **Injection**: Are all `@Query` parameterized with `@Param`? No string concatenation in queries?
- **Broken Authentication**: Are endpoints protected with `@PreAuthorize`? Are JWTs validated?
- **Sensitive Data Exposure**: Are passwords/tokens absent from logs and `application.yml`?
- **Broken Access Control**: Can users access resources they shouldn't (IDOR)?
- **Mass Assignment**: Are only allowed fields accepted in request DTOs?
- **Dependencies**: Are there known CVEs in `pom.xml` dependencies?

### 3. Performance

- Are there N+1 query issues (missing `JOIN FETCH` or `@EntityGraph`)?
- Are list endpoints paginated with `Pageable`?
- Is `FetchType.EAGER` used on collections (should be LAZY)?
- Is `spring.jpa.open-in-view` set to `false`?
- Are database indexes needed for new query patterns?
- Are batch sizes configured for bulk operations?

### 4. Readability & Maintainability

- Are variable and method names descriptive and consistent?
- Are methods under ~50 lines? No deep nesting (>3 levels)?
- Is code duplication avoided?
- Does the code follow the layered architecture (controller → service → repository)?
- Are magic numbers or strings extracted into constants or enums?
- Is Javadoc present on public API methods?

### 5. Spring Boot Best Practices

- Constructor injection only (no `@Autowired` on fields)?
- Service interfaces with `impl/` classes?
- DTOs as Java `record` types with Bean Validation?
- MapStruct for entity ↔ DTO mapping?
- RFC 7807 `ProblemDetail` for error responses?
- Oracle sequences for ID generation (not auto-increment)?
- Schema managed externally with `ddl-auto=validate` (not `update`)?
- `@Transactional(readOnly = true)` for read operations?

### 6. Testing

- Are new features covered by tests?
- Do tests cover both happy path and error cases?
- Are test assertions specific (AssertJ `assertThat`)?
- Are mocks used appropriately (mock repos, not the code under test)?
- Are `@Nested` classes used for grouping related tests?

### 7. API Design

- Are HTTP methods and status codes used correctly?
- Is the API consistent with existing endpoints (`/api/v1/<resource>`)?
- Is `@Valid` present on all `@RequestBody` parameters?
- Is backward compatibility maintained?
- Does `POST` return 201 with `Location` header?
- Does `DELETE` return 204 No Content?

## Review Communication Style

- Be specific: point to the exact line and explain the concern.
- Suggest a fix, not just the problem.
- Distinguish between: `[BLOCKER]`, `[SUGGESTION]`, `[NIT]`, `[QUESTION]`.
- Acknowledge what's done well — positive feedback helps.

## Review Output Format

```markdown
## Code Review Summary

**PR**: <title>
**Overall**: Approve / Request Changes / Comment

### Blockers
- [file:line] Description of the issue. Suggested fix: ...

### Suggestions
- [file:line] Description. Consider: ...

### Nits
- [file:line] Minor style/naming suggestion.

### Positive Notes
- Good test coverage for edge cases.
- Clean separation of concerns in the service layer.
```
