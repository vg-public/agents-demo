---
description: "Use when: fixing SonarQube or SonarCloud findings — bugs, vulnerabilities, code smells, security hotspots, or coverage gaps in Java Spring Boot code."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Paste the SonarQube finding or describe the issue — e.g., 'Fix Blocker bug: null pointer dereference in OrderServiceImpl' or 'Fix all Critical code smells'"
---

# Fix SonarQube Finding

Remediate a **SonarQube / SonarCloud finding** in Java Spring Boot code.

## Instructions

1. Identify the rule ID (e.g., `java:S1948`, `java:S2095`)
2. Read the affected file and understand the context
3. Apply the minimal fix that resolves the finding without breaking behavior
4. Verify the fix addresses the root cause, not just the symptom

## Common Findings & Fixes

### Bugs

| Rule | Issue | Fix |
|------|-------|-----|
| S2259 | Null pointer dereference | Add null check or use `Optional` |
| S2095 | Resources not closed | Use try-with-resources |
| S2583 | Condition always true/false | Remove dead branch or fix logic |
| S2189 | Infinite loop | Add proper termination condition |

### Vulnerabilities

| Rule | Issue | Fix |
|------|-------|-----|
| S3649 | SQL injection | Use parameterized queries |
| S5131 | XSS | Sanitize output / use proper encoding |
| S2068 | Hardcoded credentials | Move to environment variables |
| S5542 | Weak encryption | Use AES-256 / RSA-2048+ |

### Code Smells

| Rule | Issue | Fix |
|------|-------|-----|
| S1192 | Duplicated string literals | Extract to `static final` constant |
| S3776 | Cognitive complexity too high | Extract methods, use early returns |
| S1135 | TODO/FIXME in code | Resolve or create a tracked issue |
| S106 | `System.out.println` used | Replace with SLF4J `log.info()` |
| S1948 | Non-serializable field in serializable class | Make field transient or serializable |
| S1874 | Deprecated API usage | Replace with recommended alternative |
| S4684 | Entity used as request body | Use a DTO instead |

### Security Hotspots

| Rule | Issue | Fix |
|------|-------|-----|
| S4790 | Weak hash function (MD5/SHA-1) | Use SHA-256 or BCrypt |
| S2245 | Pseudorandom number generator | Use `SecureRandom` |
| S5332 | HTTP used (not HTTPS) | Enforce TLS |
| S4834 | Permissions too broad | Apply principle of least privilege |

## Fix Template

```java
// BEFORE — SonarQube rule java:S3776 (Cognitive Complexity)
public String processOrder(Order order) {
    if (order != null) {
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getQuantity() > 0) {
                    if (item.getPrice() != null) {
                        // ... deeply nested logic
                    }
                }
            }
        }
    }
    return null;
}

// AFTER — Reduced complexity with guard clauses
public String processOrder(Order order) {
    if (order == null || order.getItems() == null) {
        return null;
    }

    return order.getItems().stream()
        .filter(item -> item.getQuantity() > 0 && item.getPrice() != null)
        .map(this::processItem)
        .collect(Collectors.joining());
}
```

## Rules

- Fix the **root cause**, not just the SonarQube warning
- Don't suppress findings with `@SuppressWarnings` unless justified and documented
- If suppression is necessary, add a comment: `// Suppressed: <reason why this is a false positive>`
- One fix per finding — don't refactor unrelated code
- Maintain existing test coverage — update tests if the fix changes behavior
- For security findings, always verify the fix with a negative test case
