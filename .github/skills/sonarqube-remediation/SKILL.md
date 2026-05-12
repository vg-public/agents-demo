---
name: sonarqube-remediation
description: "Guide for triaging and fixing SonarQube / SonarCloud code quality and security findings in Java Spring Boot codebases including bugs, vulnerabilities, code smells, security hotspots, and coverage gaps. Use when SonarQube quality gate fails or findings need remediation."
---

# SonarQube / SonarCloud Remediation — Java Spring Boot

This skill guides the AI to triage and fix findings reported by SonarQube or SonarCloud in Java Spring Boot applications.

## When to Use

- SonarQube quality gate is failing and blocking a merge
- Security hotspots or vulnerabilities need review
- Code smell or bug findings need remediation
- Reducing technical debt tracked by SonarQube
- Improving code coverage to meet SonarQube thresholds

## Understanding SonarQube Findings

### Issue Types

| Type | Meaning | Priority |
|------|---------|----------|
| **Bug** | Code that is demonstrably wrong or will produce incorrect results at runtime | Fix immediately |
| **Vulnerability** | Code that can be exploited by an attacker | Fix immediately |
| **Security Hotspot** | Security-sensitive code that needs manual review | Review and resolve |
| **Code Smell** | Maintainability issue that makes code harder to understand or change | Fix during maintenance |

### Severity Levels

| Severity | Impact | Action |
|----------|--------|--------|
| **Blocker** | App crashes, data corruption, or critical security flaw | Must fix before merge |
| **Critical** | High-impact bug or security vulnerability | Must fix before release |
| **Major** | Significant quality or moderate security concern | Fix in current sprint |
| **Minor** | Minor quality improvement | Fix when touching the file |
| **Info** | Cosmetic or stylistic suggestion | Optional |

---

## Common Bug Findings and Fixes

### Null Pointer Dereference (S2259)
```java
// FLAGGED — possible null dereference
String name = user.getName().toUpperCase(); // NPE if getName() returns null

// FIXED — null-safe with Optional
String name = Optional.ofNullable(user.getName())
    .map(String::toUpperCase)
    .orElse("");
```

### Equals and HashCode Contract (S1206)
```java
// FLAGGED — overrides equals() but not hashCode()
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Product other)) return false;
    return Objects.equals(id, other.id);
}

// FIXED — override both consistently
@Override
public int hashCode() {
    return Objects.hash(id);
}
```

### Resource Leak (S2095)
```java
// FLAGGED — stream not closed in all paths
InputStream is = new FileInputStream(file);
String content = new String(is.readAllBytes());

// FIXED — try-with-resources
try (InputStream is = new FileInputStream(file)) {
    String content = new String(is.readAllBytes());
}
```

### Identical Branches in Conditional (S1871)
```java
// FLAGGED — both branches do the same thing
if (condition) {
    result = calculateTotal(order);
} else {
    result = calculateTotal(order);
}

// FIXED — remove the dead branch
result = calculateTotal(order);
```

### Collection Emptiness Check (S1155)
```java
// FLAGGED
if (list.size() == 0) { ... }

// FIXED
if (list.isEmpty()) { ... }
```

### String Comparison with == (S4973)
```java
// FLAGGED
if (status == "ACTIVE") { ... }

// FIXED
if ("ACTIVE".equals(status)) { ... }
```

### Unused Variables / Imports (S1481, S1128)
```java
// FLAGGED — unused import and variable
import java.util.stream.Collectors;  // unused
int unusedCount = list.size();       // value never read

// FIXED — remove unused code
```

---

## Common Vulnerability Findings and Fixes

### Weak Cryptography (S4426, S5542)
```java
// FLAGGED — MD5 is cryptographically broken
MessageDigest md = MessageDigest.getInstance("MD5");

// FIXED — use SHA-256
MessageDigest md = MessageDigest.getInstance("SHA-256");

// FLAGGED — DES is broken
Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");

// FIXED — use AES-256-GCM
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
```

### Untrusted Deserialization (S5135)
```java
// FLAGGED — deserializing untrusted data
ObjectInputStream ois = new ObjectInputStream(untrustedInput);
Object obj = ois.readObject(); // RCE risk

// FIXED — use Jackson with explicit type binding
ObjectMapper mapper = new ObjectMapper();
MyDto dto = mapper.readValue(input, MyDto.class);
```

### CSRF Protection Disabled (S4502)
```java
// FLAGGED — CSRF disabled for browser-facing app
http.csrf(csrf -> csrf.disable());

// FIXED — enable CSRF for browser-facing apps
http.csrf(Customizer.withDefaults());

// Exception: stateless REST APIs using JWT/Bearer tokens
http.csrf(csrf -> csrf.disable()); // Stateless API — auth via Bearer token, no cookies
```

### SQL Injection (S3649)
```java
// FLAGGED — string concatenation in SQL
String query = "SELECT * FROM users WHERE name = '" + name + "'";

// FIXED — parameterized query
@Query("SELECT u FROM User u WHERE u.name = :name")
User findByName(@Param("name") String name);
```

### Hardcoded Credentials (S6437)
```java
// FLAGGED
private static final String DB_PASSWORD = "admin123";

// FIXED
@Value("${db.password}")
private String dbPassword;
```

---

## Security Hotspot Review

For each hotspot:
1. **Read the SonarQube explanation** — click "See Rule".
2. **Assess the actual risk** in your context.
3. **Resolve** as **Fixed**, **Safe** (with comment), or **Acknowledged** (with ticket).

### Common Security Hotspot Categories

| Hotspot Rule | What to Check |
|-------------|---------------|
| Using HTTP instead of HTTPS | Is this internal-only or does it handle sensitive data? |
| Disabling CSRF protection | Stateless API with Bearer auth (safe) or browser app (fix)? |
| Cookies without Secure/HttpOnly/SameSite | Are cookies used for auth or session management? |
| Logging sensitive data | Does the logged data contain PII, passwords, or tokens? |
| Regex susceptible to ReDoS | Is the regex applied to user input? |
| Using pseudorandom generators | Is the random value used for security (tokens, keys)? |
| Permissive CORS policy | Is `Access-Control-Allow-Origin: *` used with authenticated endpoints? |

---

## Improving Code Coverage

### Strategy
1. **Focus on "Coverage on New Code"** — SonarQube gates on new code, not overall.
2. **Focus on business logic** — don't waste effort on getters, setters, config.
3. **Write meaningful assertions** — verify behavior, not just line execution.

### JaCoCo Configuration — Exclude from Coverage
```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/dto/**</exclude>
            <exclude>**/entity/**</exclude>
            <exclude>**/config/**</exclude>
            <exclude>**/exception/**</exclude>
            <exclude>**/*Application.class</exclude>
            <exclude>**/generated/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

Use `@Generated` annotation to exclude classes:
```java
@Generated  // Excluded from JaCoCo coverage
public class AppConfig { ... }
```

### Maven Commands
```bash
# Run tests with coverage
mvn clean verify

# Generate coverage report
mvn jacoco:report

# Check coverage thresholds
mvn jacoco:check
```

---

## Quality Gate Configuration

### Recommended Thresholds

| Metric | Condition | Threshold |
|--------|-----------|-----------|
| Coverage on new code | >= | 80% |
| Duplicated lines on new code | <= | 3% |
| Maintainability rating on new code | = | A |
| Reliability rating on new code | = | A |
| Security rating on new code | = | A |
| Security hotspots reviewed | = | 100% |
