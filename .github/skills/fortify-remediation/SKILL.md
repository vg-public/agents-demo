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

### Command Injection (CWE-78)
**Fortify Category**: `Command Injection`, `OS Command Injection`

```java
// VULNERABLE — user input passed directly to shell
Runtime.getRuntime().exec("ping " + userInput);

// FIXED — use ProcessBuilder with explicit argument list (no shell interpretation)
ProcessBuilder pb = new ProcessBuilder("ping", "-c", "4", validatedHost);
pb.redirectErrorStream(true);
Process process = pb.start();
```

**Key Rule**: Never pass user input to `Runtime.exec(String)` or `ProcessBuilder(String)`. Always use the argument-list form and validate inputs against an allowlist.

### Deserialization of Untrusted Data (CWE-502)
**Fortify Category**: `Unsafe Deserialization`, `Object Deserialization`

```java
// VULNERABLE — deserializing arbitrary classes from untrusted input
ObjectInputStream ois = new ObjectInputStream(inputStream);
Object obj = ois.readObject();

// FIXED — use Jackson with explicit type binding (no polymorphic typing)
ObjectMapper mapper = new ObjectMapper();
mapper.deactivateDefaultTyping();
MyDto dto = mapper.readValue(inputStream, MyDto.class);

// If polymorphic typing is required, use allowlist:
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CreditPayment.class, name = "credit"),
    @JsonSubTypes.Type(value = DebitPayment.class, name = "debit")
})
public abstract class Payment { }
```

**Key Rule**: Never use Java native serialization (`ObjectInputStream`) for untrusted data. Use Jackson with explicit DTO classes.

### Server-Side Request Forgery — SSRF (CWE-918)
**Fortify Category**: `Server-Side Request Forgery`

```java
// VULNERABLE — user-provided URL fetched without validation
URL url = new URL(request.getParameter("url"));
HttpURLConnection conn = (HttpURLConnection) url.openConnection();

// FIXED — validate URL against allowlist and block internal networks
String targetUrl = request.getParameter("url");
URI uri = URI.create(targetUrl);

// Block internal networks
InetAddress address = InetAddress.getByName(uri.getHost());
if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
    throw new SecurityException("SSRF attempt: internal network access blocked");
}

// Validate against allowed domains
if (!ALLOWED_DOMAINS.contains(uri.getHost())) {
    throw new SecurityException("SSRF attempt: domain not in allowlist");
}
```

### Weak Cryptographic Algorithm (CWE-327 / CWE-328)
**Fortify Category**: `Weak Encryption`, `Weak Hash`

```java
// VULNERABLE — weak hash algorithm
MessageDigest md = MessageDigest.getInstance("MD5");    // or SHA-1
byte[] hash = md.digest(data);

// FIXED — use SHA-256 or stronger
MessageDigest md = MessageDigest.getInstance("SHA-256");
byte[] hash = md.digest(data);

// VULNERABLE — weak encryption
Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");

// FIXED — use AES-256-GCM
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
```

**For password hashing** — use BCrypt (Spring Security):
```java
// FIXED — BCrypt for password storage
PasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hashed = encoder.encode(rawPassword);
```

### Cookie Security (CWE-614 / CWE-1004)
**Fortify Category**: `Cookie Security: Missing Secure Flag`, `Cookie Security: Missing HttpOnly Flag`

```java
// VULNERABLE — cookie without security flags
Cookie cookie = new Cookie("sessionId", token);
response.addCookie(cookie);

// FIXED — set Secure, HttpOnly, and SameSite
Cookie cookie = new Cookie("sessionId", token);
cookie.setSecure(true);       // Only sent over HTTPS
cookie.setHttpOnly(true);     // Not accessible via JavaScript
cookie.setPath("/");
cookie.setMaxAge(3600);
response.addCookie(cookie);

// Spring Boot application.yml approach:
// server.servlet.session.cookie.secure: true
// server.servlet.session.cookie.http-only: true
// server.servlet.session.cookie.same-site: strict
```

### Race Condition (CWE-362)
**Fortify Category**: `Race Condition`, `Race Condition: Singleton Member Field`

```java
// VULNERABLE — shared mutable state in singleton Spring bean
@Service
public class CounterService {
    private int counter = 0;  // Fortify flags: race condition

    public int increment() {
        return ++counter;      // Not thread-safe
    }
}

// FIXED — use AtomicInteger or synchronized access
@Service
public class CounterService {
    private final AtomicInteger counter = new AtomicInteger(0);

    public int increment() {
        return counter.incrementAndGet();
    }
}

// For database operations — use optimistic locking:
@Version
private Long version;
```

### Denial of Service: Regular Expression (CWE-1333)
**Fortify Category**: `Denial of Service: Regular Expression`, `ReDoS`

```java
// VULNERABLE — catastrophic backtracking possible
Pattern pattern = Pattern.compile("(a+)+b");  // ReDoS with input "aaaaaaaaaaaac"

// FIXED — simplify regex to avoid nested quantifiers
Pattern pattern = Pattern.compile("a+b");

// FIXED — add input length validation before regex
if (input.length() > MAX_INPUT_LENGTH) {
    throw new ValidationException("Input too long");
}
Pattern pattern = Pattern.compile("a+b");
Matcher matcher = pattern.matcher(input);
```

**Key Rule**: Avoid nested quantifiers (`(a+)+`, `(a*)*`, `(a|b*)+`). Limit input length before matching.

---

## False Positive Assessment Guide

Use this checklist to assess whether a finding is a true or false positive:

| Category | Common False Positive Scenarios |
|----------|-------------------------------|
| SQL Injection | Input from internal enum/constant, Spring Data derived queries, `@Param`-bound JPQL already in use |
| XSS | REST API returning JSON (Content-Type: application/json), no HTML rendering |
| Path Traversal | Path constructed from config file or database (not user input), Spring Resource loading |
| Log Forging | Value from enum, integer ID, or internal constant (not user-controlled string) |
| Null Dereference | Object guaranteed non-null by prior validation (`@NotNull`, `@Valid`, Optional unwrap) |
| Hardcoded Password | Test credentials in `src/test/`, placeholder values in comments/documentation |
| Insecure Randomness | Non-security context (shuffling UI elements, test data generation) |
| SSRF | URL from internal configuration, not user-supplied |
| Weak Crypto | Non-security use case (checksums for caching, non-sensitive data fingerprints in tests) |

---

## Verification Checklist

After applying a fix, verify it resolves the finding:

| Category | Verification |
|----------|-------------|
| SQL Injection | Confirm no string concatenation in query; all inputs bound via `@Param` or `?` |
| XSS | Confirm output is encoded or Content-Type prevents HTML rendering |
| Path Traversal | Confirm path is canonicalized and validated against base directory |
| Command Injection | Confirm no shell interpretation; arguments are in list form |
| Hardcoded Password | Confirm value comes from `@Value`, environment, or vault at runtime |
| Insecure Randomness | Confirm `SecureRandom` instance used for all security-sensitive operations |
| Log Forging | Confirm CRLF stripped and parameterized logging (`{}`) used |
| XXE | Confirm external entities and DTDs disabled on the XML parser |
| SSRF | Confirm URL validated against allowlist and internal networks blocked |
| Deserialization | Confirm no `ObjectInputStream.readObject()` on untrusted data; Jackson with typed DTOs |
| Weak Crypto | Confirm SHA-256+ for hashing, AES-256-GCM for encryption, BCrypt for passwords |
| Cookie Security | Confirm `Secure`, `HttpOnly`, and `SameSite` flags are set |
| Race Condition | Confirm atomic operations, synchronized blocks, or `@Version` optimistic locking |
| ReDoS | Confirm no nested quantifiers; input length limited before regex matching |
| Unreleased Resource | Confirm try-with-resources wraps all `Closeable`/`AutoCloseable` objects |

---

## Suppression Guidelines

When a finding is a confirmed false positive, suppress it with a documented reason:

- **Fortify Audit Workbench**: Mark as "Not an Issue" with a comment explaining why.
- **In-code suppression**: Add a comment near the flagged line:
  ```java
  // Fortify Suppression: [Category] — False positive: [reason]. Reviewed by [name] on [date].
  ```
