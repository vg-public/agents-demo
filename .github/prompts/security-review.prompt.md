---
description: "Use when: reviewing code for security vulnerabilities aligned with OWASP Top 10 — checks injection, auth, data exposure, misconfig, and provides remediation."
agent: "agent"
tools: [read, search]
argument-hint: "File or class to review — e.g., 'AuthController' or 'all controllers and services'"
---

# Security Code Review

Perform a **security-focused code review** of the specified Java Spring Boot code, aligned with **OWASP Top 10 (2025)** and **CWE Top 25**.

## Checklist

### A01 — Broken Access Control
- [ ] Endpoints enforce proper authorization (`@PreAuthorize`, `@Secured`)
- [ ] ID-based lookups don't allow accessing other users' data (IDOR)
- [ ] Admin-only operations are restricted
- [ ] CORS is configured restrictively (not `allowedOrigins("*")`)

### A02 — Cryptographic Failures
- [ ] No secrets hardcoded (passwords, API keys, tokens)
- [ ] Sensitive data not logged (passwords, SSN, credit cards)
- [ ] Passwords hashed with BCrypt/SCrypt (not MD5/SHA-1)
- [ ] TLS enforced for external communication

### A03 — Injection
- [ ] No SQL string concatenation — use parameterized queries (`@Query` with `:param`)
- [ ] No `nativeQuery` with unsanitized input
- [ ] JPQL/HQL uses parameter binding
- [ ] Log messages don't include unsanitized user input (log injection)
- [ ] No `Runtime.exec()` or `ProcessBuilder` with user input

### A04 — Insecure Design
- [ ] Business logic validated server-side (not just client)
- [ ] Rate limiting on sensitive endpoints (login, password reset)
- [ ] Proper error handling — no stack traces in responses

### A05 — Security Misconfiguration
- [ ] `spring.jpa.hibernate.ddl-auto` set to `validate` (not `create` or `update`)
- [ ] `server.error.include-stacktrace: never`
- [ ] Debug mode disabled in production
- [ ] Default credentials not used
- [ ] Actuator endpoints secured

### A06 — Vulnerable Components
- [ ] Dependencies are up to date (check `pom.xml`)
- [ ] No known CVEs in transitive dependencies
- [ ] Spring Boot version is current LTS

### A07 — Authentication Failures
- [ ] Session management properly configured
- [ ] JWT tokens validated correctly (signature, expiry, issuer)
- [ ] Password policy enforced

### A08 — Data Integrity Failures
- [ ] Deserialization is safe — no `ObjectInputStream` on untrusted data
- [ ] `@JsonIgnoreProperties(ignoreUnknown = true)` on DTOs
- [ ] Input validated with Jakarta Bean Validation before processing

### A09 — Logging & Monitoring Failures
- [ ] Security events logged (login, access denied, data changes)
- [ ] No sensitive data in logs
- [ ] Log injection prevented (sanitize user input in log messages)

### A10 — SSRF
- [ ] No user-controlled URLs in `RestTemplate`/`WebClient` calls
- [ ] URL allowlists for external service calls

## Output Format

For each finding:
```
### [SEVERITY] Category — Finding Title
- **File**: ClassName.java:lineNumber
- **CWE**: CWE-XXX
- **Issue**: Description of the vulnerability
- **Risk**: What could happen if exploited
- **Fix**: Code snippet showing the remediation
```

Severity levels: `CRITICAL` | `HIGH` | `MEDIUM` | `LOW` | `INFO`

## Rules

- Report findings with specific line numbers and code snippets
- Provide a fix for every finding — never just flag without remediation
- Prioritize findings by exploitability and impact
- Do NOT generate false positives — only report genuine risks
- End with a summary table: total findings by severity
