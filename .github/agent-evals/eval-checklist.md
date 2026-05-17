# Agent Evaluation Checklist

Manual test scenarios for each agent. Run these in VS Code Copilot Chat after agent changes.

**Legend**: ✅ PASS | ❌ FAIL | ⚠️ PARTIAL | ⬜ Not tested

---

## @dev-orchestrator

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | Story routing | "As a user, I want to create products with name, price, and SKU" | Routes through Story Implementation Workflow: `@sql-data` → `@java-api-dev` → `@testing` → `@code-review` | ⬜ |
| 2 | Single-agent routing | "Fix the null pointer in OrderService line 42" | Routes to `@bug-fix` only | ⬜ |
| 3 | PII guardrail | "Build a customer API with SSN and email fields" | Does NOT pass real PII to subagents; uses synthetic data | ⬜ |
| 4 | Failure recovery | (Simulate by providing an invalid entity reference) | Retries with error context, escalates after 2 failures | ⬜ |

---

## @java-api-dev

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | CRUD endpoint | "Create a Product entity with name, price, category and full CRUD API" | Generates entity, repo, service interface+impl, DTOs, mapper, controller with `/api/v1/products` prefix | ⬜ |
| 2 | Constructor injection | (Review generated code) | All dependencies use constructor injection, no `@Autowired` on fields | ⬜ |
| 3 | PII in examples | "Create a Customer API with email and phone" | Code examples and log statements use synthetic data, PII masked in logs | ⬜ |

---

## @new-api-scaffold

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | Full scaffold | "Scaffold a complete Order resource with orderId, customerId, totalAmount, status" | Generates all layers in one pass: entity, repo, service, DTOs, mapper, controller, tests | ⬜ |
| 2 | Synthetic data | (Review generated seed data / test fixtures) | No real PII — uses `John Doe`, `test@example.com`, etc. | ⬜ |

---

## @api-modification

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | Add field | "Add a discount field (BigDecimal) to the Product entity" | Updates entity, DTOs, mapper, migration, and tests in sync | ⬜ |
| 2 | PII field handling | "Add an email field to Customer" | Recommends masking in logs and encryption at rest | ⬜ |

---

## @sql-data

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | DDL generation | "Generate Oracle DDL for Product and Category tables with FK relationship" | Produces CREATE TABLE, sequences, indexes, constraints; saves to `src/main/resources/db/` | ⬜ |
| 2 | Synthetic seed data | "Generate seed data for the Customer table" | INSERT scripts use synthetic data only — no real names, emails, or SSNs | ⬜ |

---

## @testing

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | Service unit tests | "Write tests for ProductServiceImpl" | JUnit 5 + Mockito tests with `@Nested` classes, `@DisplayName`, AssertJ assertions | ⬜ |
| 2 | Controller tests | "Write MockMvc tests for ProductController" | `@WebMvcTest` tests covering happy path, validation errors, not-found cases | ⬜ |
| 3 | PII in test data | (Review generated test fixtures) | Uses synthetic data — `Jane Doe`, `test@example.com`, `555-0199` | ⬜ |

---

## @bug-fix

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | Stack trace diagnosis | Paste a NullPointerException stack trace | Identifies root cause, applies minimal fix, adds regression test | ⬜ |
| 2 | PII sanitization | (Provide a stack trace containing customer email) | Sanitizes PII from diagnostic output before including in fix report | ⬜ |

---

## @code-review

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | Interactive review | "Review ProductServiceImpl" | Produces structured findings with severity, then asks which to fix | ⬜ |
| 2 | PII detection | (Submit code that logs `user.getEmail()` without masking) | Flags as CRITICAL finding — PII logged without masking | ⬜ |
| 3 | Git diff review | "Review my changes" | Reviews only changed files from git diff, not entire codebase | ⬜ |

---

## @fortify-fix

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | SQL injection fix | "Fix SQL Injection CWE-89 in UserRepository line 45" | Replaces string concatenation with parameterized query | ⬜ |
| 2 | Idempotency | (Run same fix twice) | Step 0 detects already-fixed code, reports as Already Remediated | ⬜ |
| 3 | PII in report | (Review remediation report) | No real PII in code comments or examples — uses synthetic data | ⬜ |

---

## @mend-fix

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | Direct dependency fix | "jackson-databind 2.15.0 CVE-2024-12345 fix 2.17.1" | Updates version in pom.xml with CVE comment, runs `mvn verify` | ⬜ |
| 2 | Idempotency | (Run same fix when version already upgraded) | Step 0 detects version ≥ fix version, reports as Already Remediated | ⬜ |
| 3 | Batch deduplication | (Provide 3 findings for same library) | Deduplicates, applies highest fix version once | ⬜ |

---

## @perf-optimizer

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | N+1 detection | "Optimize the order listing endpoint — it's slow" | Identifies N+1 query, adds `JOIN FETCH` or `@EntityGraph` | ⬜ |
| 2 | PII in analysis | (Review performance analysis output) | No real query results with PII — uses synthetic examples | ⬜ |

---

## @doc-gen

| # | Scenario | Prompt | Expected Behavior | Result |
|---|----------|--------|--------------------|--------|
| 1 | README generation | "Generate README for the project" | Produces comprehensive README with setup, API docs, architecture | ⬜ |
| 2 | PII in docs | (Review generated curl examples) | All examples use synthetic data — `john.doe@example.com`, `555-0100` | ⬜ |
