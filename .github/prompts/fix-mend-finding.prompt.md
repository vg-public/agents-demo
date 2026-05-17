---
description: "Use when: fixing a single Mend SCA finding — paste the CVE, library name, current version, and fix version to get a targeted minimal pom.xml fix."
agent: "mend-fix"
tools: [read, edit, search, terminal]
argument-hint: "Paste the Mend finding — e.g., 'jackson-databind 2.15.0 CVE-2024-12345 fix 2.17.1' or 'snakeyaml 1.33 CVE-2022-1471 fix 2.0 (transitive from spring-boot-starter)'"
---

# Fix Mend SCA Finding

Remediate a **single Mend SCA finding** with the smallest `pom.xml` change that resolves the vulnerability. Aligns with **OWASP A06:2025 — Vulnerable and Outdated Components**, **CWE-1395**, and **NIST SP 800-53 SI-2**.

## Instructions

1. **Parse** the finding: extract **CVE**, **Library** (`groupId:artifactId`), **Current Version**, **Fix Version**, **Severity** (CVSS v3.1/v4.0), **EPSS score**, **Direct or Transitive**, **CISA KEV status**
2. **Read** `pom.xml` to locate the dependency — check `<dependencies>`, `<dependencyManagement>`, and `<properties>` blocks. If not found, run `mvn dependency:tree -Dincludes=<groupId>:<artifactId>` to trace the transitive parent.
3. **Triage**:
   - CISA KEV → **mandatory fix** regardless of CVSS
   - CVSS ≥ 9.0 or EPSS ≥ 0.5 → fix immediately
   - Verify fix version is compatible with Spring Boot `3.2.5` and Java 17
   - Verify fix version itself has no known Critical/High CVEs
   - Test-scope-only → lower priority, note in report
4. **Fix**:
   - **Direct dep with explicit version** → update the `<version>` tag in `pom.xml`
   - **Direct dep with BOM-managed version** → add property override in `<properties>` (e.g., `<snakeyaml.version>2.2</snakeyaml.version>`)
   - **Transitive** → first try upgrading the parent dep; if not possible, add a `<dependencyManagement>` override with inline CVE comment and date
   - **No fix version** → add risk acceptance comment with CVE, CVSS, EPSS, CISA KEV status, rationale, JIRA ticket, and reassessment date
   - **Multiple CVEs on same library** → upgrade to version that fixes all of them at once
5. **Verify**: run `mvn dependency:tree -Dincludes=<groupId>:<artifactId>` to confirm fixed version resolves, then `mvn clean verify` to confirm no regressions
6. **Report** the result

## Priority

- **Fix the CVE** — do not upgrade beyond the minimum safe version
- **Minimal change** — only touch `pom.xml`; do not modify Java source files for SCA issues
- **Preserve scopes** — keep existing `compile` / `runtime` / `test` scopes unchanged
- **No speculative upgrades** — do not bump other unrelated dependencies
- **Pin exact versions** — never use `LATEST`, `RELEASE`, or version ranges
- **Include CVE comments** — every version change must have an inline comment with CVE ID and date

## Concrete Examples

### Direct Dependency Upgrade
```xml
<!-- ❌ jackson-databind 2.15.0 — CVE-2024-12345 (CVSS 9.8) -->
<!-- ✅ Updated to 2.17.1 — CVE-2024-12345 fix (2026-05-16) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.1</version>
</dependency>
```

### BOM Property Override
```xml
<!-- ✅ Override Spring Boot BOM snakeyaml version -->
<properties>
    <snakeyaml.version>2.2</snakeyaml.version><!-- CVE-2022-1471 fix (2026-05-16) -->
</properties>
```

### Transitive Dependency Override
```xml
<!-- ✅ Force commons-compress 1.26 over transitive 1.21 from poi -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-compress</artifactId>
            <version>1.26.0</version><!-- CVE-2024-25710 fix — pulled by poi (2026-05-16) -->
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Risk Acceptance (No Fix Available)
```xml
<!-- RISK ACCEPTED: CVE-2023-9999 (CVSS 5.5, EPSS 0.02) — no fix version as of 2026-05-16.
     OWASP: A06:2025. CISA KEV: No.
     Vulnerable code path (XML parsing) is not reachable in this application.
     Ticket: PROJ-1234. Reassess by: 2026-06-16. -->
```

## Prohibited `pom.xml` Patterns

Never introduce: `<version>LATEST</version>`, `<version>RELEASE</version>`, version ranges `[1.0,2.0)`, `<scope>system</scope>`, HTTP repository URLs, or snapshot versions in release builds.

## Output

Report the result:
```
CVE:       CVE-XXXX-XXXXX
OWASP:     A06:2025 — Vulnerable and Outdated Components
Library:   groupId:artifactId
Before:    [current version]
After:     [fixed version]
Type:      Direct | Transitive (pulled by [parent-dep])
Severity:  Critical | High | Medium | Low (CVSS X.X)
EPSS:      [score] | CISA KEV: [Yes/No]
Action:    Fixed | Risk Accepted | Library Replaced
Change:    [1-sentence description of the pom.xml change]
Verified:  dependency:tree ✅ | mvn clean verify ✅
```
