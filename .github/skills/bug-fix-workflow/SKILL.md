---
name: bug-fix-workflow
description: "Systematic approach to diagnosing, fixing, and verifying bugs in Java Spring Boot applications. Use when a bug report is provided or when something is not working as expected."
---

# Bug Fix Workflow

This skill guides the AI through a disciplined, systematic approach to fixing bugs in Java Spring Boot REST API applications.

## When to Use

- A bug report or defect ticket is provided
- Something is not working as expected and the cause is unknown
- A test is failing and needs investigation
- A production issue needs root-cause analysis

## Workflow

### 1. Understand the Bug

- Read the bug report fully. Identify:
  - **Expected behavior**: What should happen?
  - **Actual behavior**: What is happening instead?
  - **Steps to reproduce**: How to trigger the bug.
  - **Environment**: JDK version, Spring Boot version, Oracle DB version.
  - **Frequency**: Always, intermittent, or one-time?
  - **Severity**: Blocker, critical, major, minor, trivial.
- If the report is incomplete, ask clarifying questions before investigating.

### 2. Reproduce the Bug

- Follow the steps to reproduce exactly.
- Use `curl` or Postman to hit the endpoint and observe the response.
- Check server logs for stack traces.
- If you cannot reproduce, check:
  - Environment differences (DB state, config profile)
  - Data/state dependencies
  - Timing or race condition issues
  - Feature flags or `application.yml` differences

### 3. Locate the Root Cause

Use a **binary search** strategy to narrow down:

1. **Check error logs**: Stack traces, Hibernate SQL logs, validation errors.
2. **Trace the flow**: Follow the execution from controller → service → repository → database.
3. **Identify the layer**: Is the bug in the controller, service, repository, entity, or database?
4. **Narrow to the file**: Use `grep_search` to find the relevant code.
5. **Narrow to the line**: Enable `org.hibernate.SQL=DEBUG` or add `log.debug()` statements.

Common root causes in Spring Boot:

| Root Cause | Symptom | Fix |
|-----------|---------|-----|
| `NullPointerException` | Missing null check or `Optional` not unwrapped | Use `Optional.orElseThrow()` |
| `LazyInitializationException` | Lazy collection accessed outside `@Transactional` | Use `JOIN FETCH` or `@EntityGraph` |
| Missing `@Valid` | Validation not triggering on request body | Add `@Valid` annotation |
| Missing `@Transactional` | Changes not persisted, or read inconsistency | Add `@Transactional` to service method |
| Wrong `@Query` JPQL | Incorrect results or `QuerySyntaxException` | Fix JPQL syntax, verify entity/field names |
| Oracle sequence mismatch | `DataIntegrityViolationException` on insert | Align `allocationSize` with Oracle `INCREMENT BY` |
| Circular JSON serialization | Stack overflow on entity serialization | Use DTOs, never return entities from controllers |
| `DataIntegrityViolationException` | Unique constraint or FK violation | Check for duplicates, verify FK references |
| `OptimisticLockException` | Concurrent update on `@Version` entity | Catch and return 409 Conflict |

### 4. Write a Failing Test

- Before fixing, write a test that reproduces the bug and currently fails.
- This ensures the fix is verifiable and prevents regression.
- The test should assert the **expected** behavior described in the bug report.

```java
@Test
@DisplayName("should return 404 when product ID does not exist - regression for BUG-123")
void shouldReturn404ForNonExistentProduct() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
}
```

### 5. Apply the Fix

- Make the **smallest possible change** that fixes the bug.
- Do NOT refactor surrounding code in the same change — fix only the bug.
- Do NOT add unrelated improvements — those belong in separate commits.
- Ensure the fix doesn't break other functionality by running `mvn test`.

### 6. Verify the Fix

- Run the failing test — it should now pass.
- Run the full test suite — no regressions: `mvn test`
- Manually verify via `curl` using the original reproduction steps.
- Check edge cases around the fix (boundary values, null inputs).

### 7. Document the Fix

- Write a clear commit message:
  ```
  fix: <short description of what was broken>

  Root cause: <one-line explanation of why it broke>
  Fix: <one-line explanation of what was changed>
  Fixes #<issue-number>
  ```

## Debugging Techniques

| Technique | When to Use |
|-----------|-------------|
| `log.debug()` | Quick value inspection in service/repository |
| Breakpoints (VS Code / IntelliJ) | Step through complex logic |
| Hibernate SQL logging | Verify generated SQL and parameters |
| Actuator `/health` | Check app and DB connectivity |
| `curl -v` | Inspect full request/response headers |
| Git blame / git log | Finding when and why a line was changed |
| `git bisect` | Finding which commit introduced the bug |
| Oracle `EXPLAIN PLAN` | Verify query execution plan |

## Anti-Patterns to Avoid

- Do NOT guess and apply random fixes without understanding the root cause.
- Do NOT fix symptoms instead of root causes.
- Do NOT skip writing a regression test.
- Do NOT bundle unrelated changes with the bug fix.
- Do NOT ignore similar code paths that may have the same bug.
