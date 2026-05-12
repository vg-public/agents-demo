---
name: fortify-remediation
description: "Guide for triaging and fixing Fortify SAST (Static Application Security Testing) findings in Java Spring Boot codebases. Use when Fortify scan results need remediation or a Fortify security gate is failing."
---

# Fortify SAST Remediation — Java Spring Boot

This skill guides the AI to triage, prioritize, and fix security findings reported by Micro Focus (OpenText) Fortify Static Code Analyzer in Java Spring Boot applications.

## When to Use

- Fortify SAST scan has reported vulnerabilities that need fixing
- Preparing a codebase to pass a Fortify security gate
- Reviewing Fortify findings to determine true positives vs. false positives
- Remediating critical/high Fortify findings before release

## Triage Workflow

### 1. Prioritize by Severity and Category
- **Critical**: Fix immediately — exploitable with high impact (RCE, SQL injection, hardcoded credentials).
- **High**: Fix before release — significant risk (XSS, path traversal, insecure crypto).
- **Medium**: Plan to fix — moderate risk (information exposure, weak validation).
- **Low**: Assess and fix as time permits — minimal direct risk.

### 2. Identify False Positives
Not every Fortify finding is a real vulnerability. Assess each finding:
- Does user-controlled input actually reach the flagged sink?
- Is there sanitization or validation between the source and sink that Fortify missed?
- Is the flagged code in test code, dead code, or a build artifact?
- Is the data from a trusted internal source (not user-controlled)?

If confirmed false positive, suppress with documented justification — never suppress without explanation.

### 3. Fix True Positives
Apply the remediation patterns below based on the Fortify category.

---

## Common Fortify Categories and Fixes

### SQL Injection (CWE-89)
**Fortify Category**: `SQL Injection`, `SQL Injection: Hibernate`, `SQL Injection: MyBatis`

```java
// VULNERABLE — Fortify flags string concatenation in SQL
String query = "SELECT * FROM users WHERE name = '" + userName + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(query);

// FIXED — Use PreparedStatement with parameterized query
String query = "SELECT * FROM users WHERE name = ?";
PreparedStatement pstmt = conn.prepareStatement(query);
pstmt.setString(1, userName);
ResultSet rs = pstmt.executeQuery();
```

**JPA/Hibernate** — Use named parameters in `@Query`:
```java
// VULNERABLE
@Query("SELECT u FROM User u WHERE u.name = '" + name + "'")

// FIXED
@Query("SELECT u FROM User u WHERE u.name = :name")
User findByName(@Param("name") String name);
```

**MyBatis** — Use `#{}` (parameterized) instead of `${}` (string interpolation):
```xml
<!-- VULNERABLE -->
<select id="findUser">
  SELECT * FROM users WHERE name = '${name}'
</select>

<!-- FIXED -->
<select id="findUser">
  SELECT * FROM users WHERE name = #{name}
</select>
```

### Cross-Site Scripting — XSS (CWE-79)
**Fortify Category**: `Cross-Site Scripting: Reflected`, `Cross-Site Scripting: Stored`

- **Thymeleaf**: Use `th:text` (escaped) not `th:utext` (unescaped).
- **JSP**: Use `<c:out>` or JSTL escaping.
- **REST API**: Return JSON with `Content-Type: application/json` — browsers won't render HTML.
- **Spring Boot**: Set response content type headers correctly.

### Path Traversal (CWE-22)
**Fortify Category**: `Path Manipulation`

```java
// VULNERABLE
File file = new File("/uploads/" + userFilename);

// FIXED — Canonicalize and validate the path
Path basePath = Paths.get("/uploads").toRealPath();
Path resolvedPath = basePath.resolve(userFilename).normalize().toRealPath();
if (!resolvedPath.startsWith(basePath)) {
    throw new SecurityException("Path traversal attempt detected");
}
```

### Hardcoded Credentials (CWE-798)
**Fortify Category**: `Password in Configuration File`, `Hardcoded Password`

```java
// VULNERABLE
private static final String DB_PASSWORD = "SuperSecret123!";

// FIXED — Load from environment variable
@Value("${db.password}")
private String dbPassword;

// Or use Spring Boot externalized config
// application.yml: db.password: ${DB_PASSWORD}
```

### Insecure Randomness (CWE-330)
**Fortify Category**: `Insecure Randomness`

```java
// VULNERABLE — predictable random
Random rand = new Random();
String token = String.valueOf(rand.nextInt());

// FIXED — cryptographically secure random
SecureRandom secureRand = new SecureRandom();
byte[] tokenBytes = new byte[32];
secureRand.nextBytes(tokenBytes);
String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
```

### Null Dereference (CWE-476)
**Fortify Category**: `Null Dereference`

```java
// VULNERABLE
User user = userRepo.findById(id);  // may return null
String name = user.getName();       // NPE if null

// FIXED — use Optional
User user = userRepo.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
String name = user.getName();
```

### Log Forging / Log Injection (CWE-117)
**Fortify Category**: `Log Forging`

```java
// VULNERABLE — user input can inject newlines to fake log entries
logger.info("User login: " + username);

// FIXED — strip CRLF characters and use parameterized logging
String sanitized = username.replaceAll("[\\r\\n\\t]", "_");
logger.info("User login: {}", sanitized);
```

### Privacy Violation (CWE-359)
**Fortify Category**: `Privacy Violation`

```java
// VULNERABLE
logger.info("User registered: email={}, password={}", email, password);

// FIXED — never log passwords; mask sensitive data
logger.info("User registered: email={}", maskEmail(email));
```

### XML External Entity — XXE (CWE-611)
**Fortify Category**: `XML External Entity Injection`

```java
// FIXED — Disable external entities and DTDs
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
dbf.setXIncludeAware(false);
dbf.setExpandEntityReferences(false);
```

### Missing HSTS / Security Headers
**Fortify Category**: `Insecure Transport: Missing HSTS Header`

```java
// Spring Boot SecurityFilterChain
http.headers(h -> h
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)
        .preload(true))
    .contentTypeOptions(Customizer.withDefaults())
    .frameOptions(fo -> fo.deny()));
```

### Open Redirect (CWE-601)
**Fortify Category**: `Open Redirect`

```java
// VULNERABLE
response.sendRedirect(request.getParameter("redirectUrl"));

// FIXED — validate against allowlist
String redirectUrl = request.getParameter("redirectUrl");
if (ALLOWED_REDIRECT_URLS.contains(redirectUrl)) {
    response.sendRedirect(redirectUrl);
} else {
    response.sendRedirect("/default");
}
```

### Unreleased Resource (CWE-404)
**Fortify Category**: `Unreleased Resource`

```java
// VULNERABLE — resource not closed on exception
Connection conn = dataSource.getConnection();
PreparedStatement ps = conn.prepareStatement(sql);
ResultSet rs = ps.executeQuery();

// FIXED — try-with-resources
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    // process results
}
```

---

## Suppression Guidelines

When a finding is a confirmed false positive, suppress it with a documented reason:

- **Fortify Audit Workbench**: Mark as "Not an Issue" with a comment explaining why.
- **In-code suppression**: Add a comment near the flagged line:
  ```java
  // Fortify Suppression: [Category] — False positive: [reason]. Reviewed by [name] on [date].
  ```
