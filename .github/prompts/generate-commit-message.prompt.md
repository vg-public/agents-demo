---
description: "Use when: generating a conventional Git commit message from staged changes — creates structured commit messages following Conventional Commits format."
agent: "agent"
tools: [read, search, terminal]
argument-hint: "Describe the changes or say 'generate from staged files'"
---

# Generate Commit Message

Generate a **Conventional Commits** formatted commit message from the current code changes.

## Instructions

1. Identify the changed files and understand what was modified
2. Determine the commit type and scope
3. Write a clear, concise commit message

## Commit Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

## Types

| Type | When to Use | Example |
|------|------------|---------|
| `feat` | New feature | `feat(product): add bulk import endpoint` |
| `fix` | Bug fix | `fix(order): correct total calculation with discounts` |
| `refactor` | Code restructuring (no behavior change) | `refactor(service): extract validation to shared method` |
| `test` | Adding or fixing tests | `test(product): add service tests for edge cases` |
| `docs` | Documentation only | `docs(api): update OpenAPI spec for order endpoints` |
| `chore` | Build, CI, tooling | `chore(deps): upgrade Spring Boot to 3.2.5` |
| `perf` | Performance improvement | `perf(query): add index for order date range lookups` |
| `style` | Formatting (no logic change) | `style: apply consistent import ordering` |
| `ci` | CI/CD pipeline changes | `ci: add SonarQube quality gate to pipeline` |
| `build` | Build system changes | `build: configure Oracle JDBC driver in pom.xml` |

## Scope

Use the primary domain entity or module:
- `product`, `order`, `customer`, `category` — entity-level
- `auth`, `config`, `migration`, `docker` — infrastructure-level
- `api`, `service`, `repo` — layer-level (when change spans entities)

## Examples

**Single feature:**
```
feat(product): add search by category and price range

- Add JPA Specification for dynamic filtering
- Add query parameters to GET /api/v1/products
- Create function-based index for case-insensitive name search
```

**Bug fix:**
```
fix(order): prevent duplicate order creation on retry

The order service was not checking for idempotency key,
causing duplicate orders when clients retried on timeout.

- Add idempotency_key column to ORDERS table
- Check for existing order before persisting
- Return existing order on duplicate key

Fixes: JIRA-1234
```

**Multiple related changes:**
```
feat(product): add product lifecycle management

- Add ProductStatus enum (ACTIVE, INACTIVE, DISCONTINUED)
- Add PATCH /api/v1/products/{id}/status endpoint
- Add Oracle CHECK constraint for status column
- Add service and controller tests

Breaking change: GET /api/v1/products now returns status field
```

## Rules

- **Subject line**: max 72 characters, imperative mood ("add" not "added")
- **Body**: explain *what* and *why*, not *how* (the code shows how)
- **Scope**: lowercase, singular noun
- **Breaking changes**: add `BREAKING CHANGE:` in footer or `!` after type
- One logical change per commit — don't mix unrelated changes
