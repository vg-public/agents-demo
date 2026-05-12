---
description: "Use when: a Spring Boot application throws an error or stack trace — diagnoses the root cause and provides a fix with explanation."
agent: "agent"
tools: [read, edit, search, terminal]
argument-hint: "Paste the error message or stack trace"
---

# Diagnose and Fix Error

You are diagnosing a **runtime error or stack trace** in a Java Spring Boot application.

## Diagnostic Process

### Step 1 — Parse the Stack Trace

- Identify the **exception type** (e.g., `NullPointerException`, `LazyInitializationException`, `ConstraintViolationException`)
- Find the **root cause** — look for `Caused by:` at the bottom of the trace
- Locate the **first line of application code** (not framework code) in the stack trace

### Step 2 — Read the Failing Code

Open and read the source file at the line number referenced in the stack trace.

### Step 3 — Identify Common Patterns

| Exception | Likely Cause | Typical Fix |
|-----------|-------------|-------------|
| `LazyInitializationException` | Accessing lazy collection outside transaction | Add `@Transactional` or use `@EntityGraph` / `JOIN FETCH` |
| `NullPointerException` | Missing null check on Optional or findById result | Use `.orElseThrow()` with meaningful exception |
| `DataIntegrityViolationException` | Unique constraint violation or FK error | Check for duplicates before save, add proper error handling |
| `ConstraintViolationException` | Jakarta validation failed | Fix the request data or adjust validation annotations |
| `HttpMessageNotReadableException` | Malformed JSON or wrong request body | Fix request format, add `@JsonFormat` for dates |
| `MethodArgumentNotValidException` | `@Valid` validation failed | Fix field validations in DTO |
| `OptimisticLockException` | Concurrent modification conflict | Retry the operation or notify user |
| `SQLSyntaxErrorException` | Bad JPQL/SQL or DDL script error | Fix query syntax |
| `BeanCreationException` | Missing bean or circular dependency | Check `@Service`, `@Repository`, `@Component` annotations |
| `HttpRequestMethodNotSupportedException` | Wrong HTTP method used | Check controller `@GetMapping` / `@PostMapping` etc. |

### Step 4 — Apply the Fix

1. Make the minimal code change to fix the root cause
2. Explain **why** the error happened
3. Explain **what** the fix does
4. Suggest a **test** to prevent regression

### Step 5 — Verify

Run `mvn compile` to confirm the fix compiles, and suggest a test command.

## Rules

- Always fix the **root cause**, not just the symptom
- Never catch and swallow exceptions silently
- Never use generic `catch (Exception e)` unless re-throwing a specific custom exception
