---
description: "Use when: checking Maven dependencies for known vulnerabilities, outdated versions, or unnecessary dependencies — generates upgrade recommendations."
agent: "agent"
tools: [read, search]
argument-hint: "Say 'Check pom.xml for vulnerabilities' or 'Upgrade Spring Boot dependencies'"
---

# Check Dependencies

Analyze **Maven dependencies** in `pom.xml` for security vulnerabilities, outdated versions, and unnecessary dependencies.

## Instructions

1. Read `pom.xml` to list all dependencies and their versions
2. Check for known issues per the categories below

## Dependency Audit Checklist

### Security Vulnerabilities (CVEs)

Check these commonly-vulnerable dependencies:

| Dependency | Risk | Action |
|-----------|------|--------|
| `log4j-core` < 2.17.1 | CVE-2021-44228 (Log4Shell) | Upgrade or remove (Spring Boot uses Logback) |
| `jackson-databind` | Deserialization RCE | Ensure managed by Spring Boot BOM |
| `snakeyaml` | Constructor injection | Ensure latest via Spring Boot BOM |
| `commons-text` < 1.10 | CVE-2022-42889 | Upgrade |
| `h2` in prod scope | Database exposure | Change to `test` scope only |

### Version Currency

| Category | Check |
|----------|-------|
| Spring Boot | Is it the latest patch of current LTS? |
| Java version | Is `maven.compiler.source` set to 17 or 21? |
| Oracle JDBC | Is `ojdbc11` used (not `ojdbc8`)? |
| JUnit | Is JUnit 5 used (not JUnit 4)? |

### Dependency Hygiene

| Issue | Fix |
|-------|-----|
| Version hardcoded instead of using Spring Boot BOM | Remove `<version>` — let `spring-boot-starter-parent` manage it |
| Unused dependency | Remove from `pom.xml` |
| Test dependency in `compile` scope | Change to `<scope>test</scope>` |
| Duplicate dependency (different versions) | Use `<dependencyManagement>` to enforce one version |
| Snapshot dependency in release | Replace with stable version |

### Recommended Spring Boot Starters

```xml
<!-- Core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Oracle -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- API Docs -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Output Format

```markdown
## Dependency Audit Report

### 🔴 Critical (fix immediately)
- `dependency:version` — CVE description + upgrade target

### 🟡 Warning (fix soon)
- `dependency:version` — outdated, upgrade to X.Y.Z

### 🟢 Info (nice to have)
- Suggestion or cleanup recommendation

### Recommended pom.xml Changes
(code block with specific XML changes)
```

## Rules

- Don't recommend upgrading to a version that breaks Spring Boot compatibility
- Prefer Spring Boot's managed versions over explicit versions
- Flag any dependency with scope `compile` that should be `test` or `runtime`
- Check for dependencies that duplicate what Spring Boot starters already provide
