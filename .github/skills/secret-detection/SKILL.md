---
name: secret-detection
description: "Guide for detecting, removing, and preventing hardcoded secrets (API keys, passwords, tokens, certificates) in Java Spring Boot source code and Git history. Use when secrets are accidentally committed or when setting up secret scanning."
---

# Secret Detection & Remediation — Java Spring Boot

This skill guides the AI to detect, remove, and prevent secrets from leaking into Java Spring Boot source code and Git history.

## When to Use

- A secret (API key, password, token) has been accidentally committed
- Setting up pre-commit hooks or CI checks for secret detection
- Reviewing code for hardcoded credentials
- Rotating compromised secrets after a leak

## Immediate Response When a Secret Is Found

### 1. Revoke and Rotate FIRST

Before cleaning up the code:
1. **Revoke the exposed credential** — deactivate the key, reset the password.
2. **Issue a new credential** and update it in the secrets vault.
3. **Check for unauthorized usage** — review audit logs.
4. **Notify your security team** — follow incident response process.

> A secret in Git history is permanently exposed even if you delete the file.

### 2. Remove from Code

```java
// BEFORE — secret in code
private static final String API_KEY = "sk-abc123def456";
private static final String DB_PASSWORD = "P@ssw0rd!";

// AFTER — loaded from Spring externalized config
@Value("${api.key}")
private String apiKey;

@Value("${spring.datasource.password}")
private String dbPassword;
```

```yaml
# application.yml — reference environment variables
spring:
  datasource:
    password: ${DB_PASSWORD}
api:
  key: ${API_KEY}
```

```yaml
# docker-compose.yml
environment:
  DB_PASSWORD: ${DB_PASSWORD}  # loaded from .env file (not committed)
```

### 3. Clean Git History (if the secret was pushed)

**Option A — git-filter-repo (recommended)**:
```bash
# Remove a file containing secrets from all history
git filter-repo --invert-paths --path src/main/resources/credentials.properties

# Or replace specific strings in all history
echo 'sk-abc123def456==>***REDACTED***' > replacements.txt
git filter-repo --replace-text replacements.txt
```

**Option B — BFG Repo-Cleaner**:
```bash
echo 'sk-abc123def456' > passwords.txt
bfg --replace-text passwords.txt
git reflog expire --expire=now --all
git gc --prune=now --aggressive
git push --force-with-lease
```

### 4. Verify Removal
```bash
git log -p --all -S 'sk-abc123def456' --diff-filter=ACDMR
# If nothing is returned, the secret is gone
```

---

## Common Secret Patterns to Detect

| Secret Type | Pattern / Regex | Example |
|------------|-----------------|---------|
| AWS Access Key | `AKIA[0-9A-Z]{16}` | `AKIAIOSFODNN7EXAMPLE` |
| GitHub PAT | `ghp_[A-Za-z0-9]{36}` | `ghp_ABCDef1234...` |
| JDBC URL with password | `jdbc:.*password=` | `jdbc:oracle:thin:@//host?password=secret` |
| Oracle Wallet password | `javax.net.ssl.keyStorePassword` | hardcoded in properties |
| Private Key (PEM) | `-----BEGIN (RSA\|EC) PRIVATE KEY-----` | PEM block |
| Generic password in config | `password\s*[:=]\s*["'][^"']{8,}` | `password: "MyS3cret!"` |
| Generic API key | `api[_-]?key\s*[:=]\s*["'][A-Za-z0-9]{16,}` | `api_key = "abc123..."` |
| Spring datasource password | `spring.datasource.password=` | hardcoded value |
| Bearer token | `Bearer\s+[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+` | `Bearer eyJhbGci...` |

---

## Prevention — Pre-Commit Hooks

### GitLeaks (recommended)
```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/gitleaks/gitleaks
    rev: v8.21.2
    hooks:
      - id: gitleaks
```

### GitLeaks Custom Config
```toml
# .gitleaks.toml
title = "Custom Gitleaks Config"

[allowlist]
  description = "Allowed patterns"
  paths = [
    '''src/test/.*''',
    '''.*\.md''',
  ]

[[rules]]
  id = "oracle-wallet-password"
  description = "Oracle wallet password"
  regex = '''javax\.net\.ssl\.(keyStore|trustStore)Password\s*=\s*["']?[^\s"']+'''
  secretGroup = 0
```

### GitLeaks in CI (GitHub Actions)
```yaml
- name: Gitleaks Secret Scan
  uses: gitleaks/gitleaks-action@v2
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## Spring Boot Secret Management Best Practices

1. **Use Spring profiles** for environment-specific config:
   ```yaml
   # application-prod.yml — no secrets, only references
   spring:
     datasource:
       url: ${ORACLE_JDBC_URL}
       username: ${ORACLE_USERNAME}
       password: ${ORACLE_PASSWORD}
   ```

2. **Use Spring Cloud Vault** for production:
   ```yaml
   spring:
     cloud:
       vault:
         uri: https://vault.example.com
         authentication: TOKEN
         token: ${VAULT_TOKEN}
   ```

3. **Never log secrets** — mask sensitive fields:
   ```java
   // BAD
   log.info("Connecting with password: {}", password);

   // GOOD
   log.info("Connecting to database: {}", jdbcUrl.replaceAll("password=.*", "password=***"));
   ```

4. **Add to .gitignore**:
   ```gitignore
   application-local.yml
   application-secret.yml
   *.p12
   *.jks
   *.pem
   .env
   ```
