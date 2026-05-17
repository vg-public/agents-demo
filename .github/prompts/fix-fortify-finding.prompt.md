---
description: "Use when: fixing a single Fortify SAST finding — paste the category, CWE, file, and line number to get a targeted minimal fix."
agent: "fortify-fix"
tools: [read, edit, search, terminal]
argument-hint: "Paste the Fortify finding — e.g., 'SQL Injection (CWE-89) in OrderRepository.java line 45' or 'Path Manipulation in FileService.java:78'"
---

# Fix Fortify SAST Finding

Remediate a **single Fortify SAST finding** with the smallest code change that eliminates the vulnerability. All fixes align with **OWASP Top 10 (2025)**, **CWE Top 25 (2024)**, and **NIST SP 800-53 Rev 5**.

## Instructions

1. **Parse** the finding: extract **Category**, **CWE**, **Severity**, **File**, **Line**, **Source** (tainted input), **Sink** (dangerous operation)
2. **Map** to OWASP Top 10 2025 category (e.g., CWE-89 → A03:2025 Injection)
3. **Read** the affected code and trace the source-to-sink dataflow — confirm whether user-controlled input reaches the sink
4. **Triage**: determine True Positive or False Positive
   - If the code is in `src/test/`, generated code, or uses data from internal constants/enums → likely False Positive
   - If user input reaches the sink without sanitization → True Positive
5. If **True Positive** — apply the minimal category-specific fix pattern:
   - **Prefer framework controls**: Spring Security config, `@Valid`, `@PreAuthorize`, JPA `@Param`
   - **Then library sanitizers**: OWASP Java Encoder, `HtmlUtils.htmlEscape()`, `StringEscapeUtils`
   - **Then inline fix**: parameterized query, try-with-resources, null check
6. If **False Positive** — provide suppression comment with OWASP reference, CWE, specific justification, and reviewer name
7. **Verify**: `mvn compile` passes, `mvn test` passes, no new Fortify/SonarQube findings introduced
8. If the fix is security-critical, **add a unit test** that proves malicious input is rejected or safely handled

## Priority

- **Fix the vulnerability** — do not refactor surrounding code
- **Minimal change** — smallest edit that eliminates the finding
- **Preserve behavior** — all non-vulnerable code paths unchanged
- **Defense in depth** — prefer multiple layers (validation + parameterization + output encoding) over one
- **No prohibited patterns** — never introduce `Runtime.exec(String)`, `ObjectInputStream`, `String.format()` in SQL, `Class.forName(userInput)`, `new Random()` for security, `MD5`/`SHA-1` for security, or `AES/ECB`

## Concrete Examples

### SQL Injection (CWE-89 → A03:2025)
```java
// ❌ @Query("SELECT u FROM User u WHERE u.name = '" + name + "'")
// ✅ @Query("SELECT u FROM User u WHERE u.name = :name")
//    User findByName(@Param("name") String name);
```

### Log Forging (CWE-117 → A03:2025)
```java
// ❌ logger.info("Login: " + username);
// ✅ logger.info("Login: {}", username.replaceAll("[\\r\\n\\t]", "_"));
```

### Path Traversal (CWE-22 → A01:2025)
```java
// ❌ Path file = Paths.get("/uploads/" + userFilename);
// ✅ Path base = Paths.get("/uploads").toRealPath();
//    Path resolved = base.resolve(userFilename).normalize().toRealPath();
//    if (!resolved.startsWith(base)) throw new SecurityException("Path traversal blocked");
```

## Output

Report the result:
```
Finding:    [Category] (CWE-XXX) — [Severity]
OWASP:     A0X:2025 — [Category Name]
File:      [path]:[line]
Verdict:   True Positive | False Positive
Action:    Fixed | Suppressed
Change:    [1-sentence description]
Test:      [Added | Existing coverage sufficient]
```
