---
name: security-code-review
description: "Guide for performing security-focused code reviews of Java Spring Boot applications aligned with OWASP Top 10 2025, SANS CWE Top 25, and secure coding standards. Use when reviewing code for security vulnerabilities or hardening applications."
---

# Security Code Review — Java Spring Boot

This skill guides the AI to identify and remediate security vulnerabilities during code reviews of Java Spring Boot REST API applications, covering OWASP Top 10 (2025), SANS CWE Top 25, and modern secure coding practices.

## When to Use

- Reviewing code changes for security vulnerabilities
- Hardening existing code before a security audit
- Assessing security posture of a new feature
- Responding to Fortify/SonarQube SAST findings
- Ensuring compliance with security policies

## OWASP Top 10 (2025) Checklist

### A01 — Broken Access Control
- Verify authorization checks on every endpoint (`@PreAuthorize`, `@Secured`).
- Ensure users cannot access or modify resources belonging to other users (IDOR).
- Verify CORS is configured with explicit allowed origins — never `*` in production.
- Confirm JWT tokens are validated on every request.
- Check for privilege escalation — can a regular user call admin endpoints?

**What to look for:**
```java
// BAD — no authorization check, any authenticated user can access any order
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable Long id) { return orderRepo.findById(id).orElseThrow(); }

// GOOD — verify the order belongs to the requesting user
@GetMapping("/orders/{id}")
@PreAuthorize("hasRole('USER')")
public Order getOrder(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
    Order order = orderRepo.findById(id).orElseThrow();
    if (!order.getUserId().equals(user.getId())) throw new ForbiddenException();
    return order;
}
```

### A02 — Cryptographic Failures
- Passwords must be hashed with `BCryptPasswordEncoder` — never MD5 or SHA-1.
- Sensitive data at rest encrypted (AES-256-GCM).
- TLS 1.2+ enforced for all connections.
- No secrets in source code, `application.yml`, or logs — use environment variables or vault.
- Use `SecureRandom` for random values — never `Math.random()` or `java.util.Random`.

### A03 — Injection
- **SQL Injection**: All `@Query` must use `@Param` bindings. No string concatenation in queries.
- **Command Injection**: Never pass user input to `Runtime.getRuntime().exec()` or `ProcessBuilder`.
- **LDAP Injection**: Escape special characters in LDAP search filters.
- **Log Injection**: Sanitize user input before logging — prevent CRLF injection in log entries.

**Grep patterns to search for:**
```
# SQL injection risks
String.format.*SELECT|"SELECT.*" \+|query\(.*\+
# Command injection
Runtime.getRuntime\(\).exec|ProcessBuilder
# Unsafe deserialization
ObjectInputStream|readObject\(\)
```

### A04 — Insecure Design
- Rate limiting on authentication and password reset endpoints.
- Business logic flaws: negative quantities, price manipulation, race conditions.
- Multi-step workflows validate state at each step — don't trust client flow.
- Resource consumption limits (file upload size, pagination caps, query limits).

### A05 — Security Misconfiguration
- `spring.profiles.active=prod` in production — never dev/debug.
- `spring.jpa.show-sql=false` in production.
- Error responses must not expose stack traces (use `GlobalExceptionHandler`).
- Security headers present: `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`.
- Default credentials changed in all environments.
- `spring.jpa.hibernate.ddl-auto=validate` in production (never `update` or `create`).

### A06 — Vulnerable and Outdated Components
- Check for known CVEs in `pom.xml` dependencies.
- No end-of-life frameworks or libraries.
- Cross-reference with Mend/Snyk scan results.

### A07 — Identification and Authentication Failures
- Password policy enforced (minimum length 12+, no common passwords).
- Account lockout or rate limiting after failed login attempts.
- Session tokens regenerated after login.
- JWTs have reasonable expiration (access: 15-60min, refresh: 7-30 days).
- Logout invalidates the token server-side.

### A08 — Software and Data Integrity Failures
- No deserialization of untrusted data — use allowlists for deserialized types.
- Verify CI/CD pipeline integrity.
- Signed artifacts for deployment.

### A09 — Security Logging and Monitoring Failures
- Authentication events logged (login, logout, failure).
- Authorization failures logged.
- Logs must NOT contain passwords, tokens, credit card numbers, or PII.
- Log injection prevented — use SLF4J parameterized logging:
```java
// GOOD — parameterized logging prevents log injection
log.info("User {} logged in", username);

// BAD — string concatenation allows log injection
log.info("User " + username + " logged in");
```

### A10 — Server-Side Request Forgery (SSRF)
- Validate and allowlist URLs when the server fetches user-supplied URLs.
- Block requests to internal networks (169.254.x.x, 10.x.x.x, 127.0.0.1).
- Disable HTTP redirects when fetching user-supplied URLs.

## Spring Boot Security Checklist

| Check | How to Verify |
|-------|--------------|
| CSRF enabled for browser clients | `SecurityFilterChain` config includes CSRF |
| CORS restricted to allowed origins | `WebMvcConfigurer.addCorsMappings()` or `@CrossOrigin` |
| `open-in-view=false` | `application.yml` |
| Endpoints secured by default | `anyRequest().authenticated()` in `SecurityFilterChain` |
| Actuator endpoints restricted | `management.endpoints.web.exposure.include` limited |
| Method-level security | `@EnableMethodSecurity` + `@PreAuthorize` |
| Input validation | `@Valid` on all `@RequestBody` parameters |
| Error details hidden | `ProblemDetail` responses without stack traces |

## Review Output Format

```markdown
## Security Review: <PR/Feature Title>

**Risk Level**: Critical / High / Medium / Low

### Findings

#### [CRITICAL] SQL Injection in UserService
- **File**: src/main/java/.../UserService.java:42
- **CWE**: CWE-89 (SQL Injection)
- **Issue**: User input concatenated into query string.
- **Fix**: Use `@Query` with `@Param` binding.

#### [HIGH] Missing Authorization on Admin Endpoint
- **File**: src/main/java/.../AdminController.java:15
- **CWE**: CWE-862 (Missing Authorization)
- **Issue**: Endpoint accessible to any authenticated user.
- **Fix**: Add `@PreAuthorize("hasRole('ADMIN')")`.

### Passed Checks
- Input validation present on all request DTOs
- Passwords hashed with bcrypt
- No secrets in codebase
```
