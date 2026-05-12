---
name: cicd-pipeline-security
description: "Guide for securing CI/CD pipelines for Java Spring Boot projects, fixing build/deploy failures, hardening GitHub Actions workflows, and implementing security quality gates. Use when fixing pipeline issues or securing the software delivery process."
---

# CI/CD Pipeline Security — Java Spring Boot

This skill guides the AI through securing CI/CD pipelines and diagnosing common build, test, and deployment failures for Java Spring Boot applications.

## When to Use

- CI/CD pipeline is failing and needs diagnosis
- Hardening pipeline security (secrets management, permissions, supply chain)
- Adding security scanning stages (SAST, SCA, DAST, container scanning)
- Fixing flaky or broken build/test/deploy stages
- Implementing quality gates to block insecure code from merging

---

## Pipeline Security Hardening

### 1. Secrets Management

**Never** commit secrets to the repository. Use the platform's secrets store:

**GitHub Actions**:
```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    env:
      DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
      ORACLE_WALLET_PASSWORD: ${{ secrets.ORACLE_WALLET_PASSWORD }}
    steps:
      - name: Deploy
        run: ./deploy.sh
```

**Jenkins**:
```groovy
pipeline {
    environment {
        DB_PASSWORD = credentials('db-password-credential-id')
    }
}
```

**Rules**:
- Store all credentials, tokens, and API keys in the CI platform's secrets vault.
- Rotate secrets regularly (at least quarterly).
- Use OIDC for cloud provider authentication instead of long-lived credentials.
- Never echo or print secrets in build logs.
- Use short-lived tokens where possible.

### 2. Least-Privilege Permissions

```yaml
permissions:
  contents: read
  pull-requests: write
  packages: write
  security-events: write
```

- Pin actions to full commit SHAs:
```yaml
# BAD — tag can be hijacked
- uses: actions/checkout@v4

# GOOD — pinned to immutable commit SHA
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2
```

### 3. Supply Chain Security

- **Pin action versions by SHA** to prevent supply chain attacks.
- **Verify checksums** of downloaded build tools.
- **Use lock files** (`pom.xml` with pinned versions) for reproducible builds.
- Enable **Dependabot** or **Renovate** for automated dependency updates.
- Use **Step Security Harden Runner**:
```yaml
- uses: step-security/harden-runner@v2
  with:
    egress-policy: audit
```

---

## Recommended CI Pipeline for Java Spring Boot

```yaml
name: Java Spring Boot CI Pipeline

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main]

permissions:
  contents: read
  security-events: write

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Build and Test
        run: mvn clean verify -B
      - name: Upload JaCoCo Report
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: target/site/jacoco/

  checkstyle:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Checkstyle
        run: mvn checkstyle:check -B

  sast:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: java
      - name: Autobuild
        uses: github/codeql-action/autobuild@v3
      - name: CodeQL Analysis
        uses: github/codeql-action/analyze@v3

  sca:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: OWASP Dependency Check
        run: mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 -B

  secret-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
        with:
          fetch-depth: 0
      - name: Gitleaks
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  container-scan:
    runs-on: ubuntu-latest
    needs: [build-and-test]
    steps:
      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Build JAR
        run: mvn package -DskipTests -B
      - name: Build Docker image
        run: docker build -t myapp:${{ github.sha }} .
      - name: Trivy scan
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'myapp:${{ github.sha }}'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'

  quality-gate:
    needs: [build-and-test, checkstyle, sast, sca, secret-scan]
    runs-on: ubuntu-latest
    steps:
      - name: All checks passed
        run: echo "Quality gate passed — safe to merge"
```

### SonarQube Integration
```yaml
  sonar:
    runs-on: ubuntu-latest
    needs: [build-and-test]
    steps:
      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
        with:
          fetch-depth: 0
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: SonarQube Scan
        run: mvn sonar:sonar -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} -Dsonar.token=${{ secrets.SONAR_TOKEN }} -B
```

---

## Common CI/CD Failure Diagnosis

### Build Failures

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Maven build OOM | Insufficient heap for compilation | Set `MAVEN_OPTS: -Xmx1024m` in pipeline env |
| Compilation error only in CI | Different JDK version than local | Pin JDK version in pipeline config |
| Docker build fails on `COPY` | File not in build context or `.dockerignore` excludes it | Fix path or `.dockerignore` |
| Gradle build fails — dependency resolution | Repo not accessible from CI runner | Configure mirror/proxy, check auth |
| `mvn dependency:resolve` fails | Private repo credentials missing | Add Maven `settings.xml` with credentials |

### Test Failures in CI

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Tests pass locally, fail in CI | Environment difference (timezone, locale) | Use UTC timezone, fix locale-dependent tests |
| Flaky tests (intermittent pass/fail) | Race conditions, shared state | Isolate tests, use `@DirtiesContext` |
| Coverage threshold not met | New code without tests | Add tests for uncovered code paths |
| Integration test fails | Database or service unavailable | Use Testcontainers or H2 in-memory |
| `ENOMEM` or OOM killed | Test process exceeds memory limit | Increase runner memory or split test suites |

### Security Scan Failures

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Fortify scan timeout | Large codebase, insufficient memory | Increase scan memory, exclude test/generated code |
| OWASP Dependency-Check CVSS > 7 | Vulnerable JAR dependency | Update in `pom.xml` |
| Mend policy violation | New dependency with known CVE | Upgrade or replace the dependency |
| Trivy finds critical in base image | Outdated Docker base image | Update base image version |
