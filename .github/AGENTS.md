# Agents Registry

This workspace provides **13 custom Copilot agents** organized into four categories — all focused on **Java Spring Boot REST API development** with **Oracle Database**.

---

## Orchestrator

| Agent | Command | Purpose |
|-------|---------|---------|
| Dev Orchestrator | `@dev-orchestrator` | Routes tasks to the right agent. Start here if unsure which agent to use. |

---

## Development Agents

These agents work on **actual project code** in `src/`.

| Agent | Command | Purpose | Stack |
|-------|---------|---------|-------|
| Java API Dev | `@java-api-dev` | Build REST API endpoints, services, entities, DTOs, MapStruct mappers | Java 17+, Spring Boot 3.2+, JPA, Oracle |
| New API Scaffold | `@new-api-scaffold` | Scaffold a complete new resource — entity, repo, service, DTOs, mapper, controller, tests | Java 17+, Spring Boot 3.2+, Oracle |
| API Modification | `@api-modification` | Safely modify existing APIs — add/remove fields, change validation, update endpoints across all layers | Java, Spring Boot, JPA |
| Testing | `@testing` | Write JUnit 5 + Mockito unit tests, MockMvc controller tests, @DataJpaTest repository tests | JUnit 5, Mockito, AssertJ, MockMvc |
| Bug Fix | `@bug-fix` | Diagnose and fix bugs — stack traces, JPA issues, validation errors | Java, Spring Boot, Hibernate |
| Code Review | `@code-review` | Review code for correctness, security, performance, readability (**read-only**) | Java, Spring Boot |
| Perf Optimizer | `@perf-optimizer` | Optimize JPA queries, HikariCP pools, Oracle indexes, caching | Spring Data JPA, Oracle, HikariCP |
| Doc Gen | `@doc-gen` | Generate README, Javadoc, ADRs, API documentation, Mermaid diagrams | Markdown, Javadoc |

---

## Artifact Generation Agents

These agents generate **design artifacts** under the `work/` directory.

| Agent | Command | Purpose | Output Directory |
|-------|---------|---------|-----------------|
| Epic | `@epic` | Generate Agile epics from problem statements | `work/pmo/` |
| Story | `@story` | Generate user stories for each epic | `work/pmo/EPIC-XXX/` |
| SQL Data | `@sql-data` | Generate Oracle SQL schema, sequences, indexes, and seed data | `work/sql/` |
| Test Case | `@test-case` | Generate QA test cases and test plans | `work/qa/` |

---

## Common Workflows

### Build a New API Resource (End-to-End)
`@epic` → `@story` → `@sql-data` → `@new-api-scaffold` → `@testing` → `@code-review` → `@doc-gen`

### Quick Scaffold a New Resource
`@new-api-scaffold` — generates entity, repo, service, DTOs, mapper, controller, and tests in one pass

### Modify an Existing API
`@api-modification` → `@testing` → `@code-review`

### Fix a Bug
`@bug-fix` → `@testing` (regression test) → `@code-review` (verify fix)

### Performance Issue
`@perf-optimizer` → `@testing` (no regressions) → `@code-review` (verify quality)

### New API Endpoint on Existing Resource
`@api-modification` → `@testing` → `@doc-gen`

### Database Schema Change
`@sql-data` → `@api-modification` (update entity + DTOs) → `@testing`

### Code Quality Improvement
`@code-review` → `@bug-fix` → `@perf-optimizer` → `@testing` → `@doc-gen`

---

## Skills

All skills are available in `.github/skills/` and are wired into the relevant agents:

| Skill | Used By | Focus |
|-------|---------|-------|
| `java-api-development` | `@java-api-dev`, `@new-api-scaffold`, `@testing` | Spring Boot + Oracle CRUD patterns |
| `database-query-debugging` | `@java-api-dev`, `@bug-fix`, `@perf-optimizer` | JPA/Hibernate + Oracle debugging |
| `unit-testing` | `@testing` | JUnit 5 + Mockito + MockMvc patterns |
| `api-debugging` | `@java-api-dev`, `@bug-fix` | REST API error diagnosis |
| `bug-fix-workflow` | `@bug-fix` | Systematic bug-fixing methodology |
| `code-review` | `@code-review` | Code review checklists |
| `security-code-review` | `@code-review` | OWASP-aligned security review |
| `performance-optimization` | `@perf-optimizer`, `@code-review` | Performance profiling methodology |
| `story-implementation` | `@api-modification`, `@new-api-scaffold` | Story → code workflow |
| `git-workflow` | (general use) | Git branching and commit conventions |
| `fortify-remediation` | (security workflows) | Fortify SAST finding remediation |
| `mend-vulnerability-remediation` | (security workflows) | Maven dependency vulnerability remediation |
| `sonarqube-remediation` | (security workflows) | SonarQube finding remediation |
| `cicd-pipeline-security` | (security workflows) | CI/CD pipeline hardening |
| `secret-detection` | (security workflows) | Secret scanning and prevention |
| `container-security` | (security workflows) | Docker/K8s security for Spring Boot |
| `github-actions-failure-debugging` | (CI/CD workflows) | GitHub Actions troubleshooting |

---

## Prompt Templates

Reusable `.prompt.md` files in `.github/prompts/` for common day-to-day tasks. Invoke from VS Code Chat with `/` or by name.

### Scaffolding & Code Generation

| Prompt | Purpose |
|--------|---------|
| `scaffold-resource` | Scaffold a complete new REST API resource (entity → controller → tests) |
| `add-field` | Add a field across all layers (migration → entity → DTOs → mapper → tests) |
| `add-endpoint` | Add a new endpoint to an existing controller |
| `create-enum` | Create or convert a field to an enum type |
| `generate-mapper` | Generate or update a MapStruct entity ↔ DTO mapper |

### Entity & Data Layer

| Prompt | Purpose |
|--------|---------|
| `add-relationship` | Add JPA relationships (@OneToMany, @ManyToOne, @ManyToMany) |
| `add-auditing` | Set up JPA auditing (createdAt, updatedAt, createdBy, updatedBy) |
| `add-filters` | Add search/filter/sort with JPA Specifications |
| `add-validation` | Add Jakarta Bean Validation and custom validators |
| `add-caching` | Configure Spring Cache with @Cacheable / @CacheEvict |
| `add-pagination` | Add pagination and sorting to a list endpoint |

### Database & SQL

| Prompt | Purpose |
|--------|---------|
| `generate-oracle-sql` | Generate Oracle DDL — tables, sequences, indexes, seed data |

### Testing

| Prompt | Purpose |
|--------|---------|
| `generate-service-tests` | JUnit 5 + Mockito service unit tests |
| `generate-controller-tests` | MockMvc controller integration tests |
| `generate-repository-tests` | @DataJpaTest repository tests |
| `improve-coverage` | Identify coverage gaps and generate missing tests |

### Code Quality & Security

| Prompt | Purpose |
|--------|---------|
| `code-review` | Structured review: correctness, security, performance, best practices |
| `security-review` | OWASP Top 10 aligned security audit |
| `refactor-code` | Clean code refactoring without behavior changes |
| `fix-sonar-finding` | Fix SonarQube bugs, vulnerabilities, and code smells |
| `optimize-query` | JPA/Hibernate query optimization (N+1, projections, indexes) |
| `check-dependencies` | Audit Maven dependencies for CVEs and outdated versions |

### Infrastructure & DevOps

| Prompt | Purpose |
|--------|---------|
| `configure-application` | Generate application.yml with profiles (dev/prod) |
| `docker-setup` | Multi-stage Dockerfile + docker-compose with Oracle DB |
| `exception-handling` | Global @RestControllerAdvice with RFC 7807 ProblemDetail |
| `add-health-checks` | Spring Boot Actuator health indicators + K8s probes |

### Daily Workflow

| Prompt | Purpose |
|--------|---------|
| `fix-error` | Diagnose and fix a runtime error or stack trace |
| `debug-jpa-issue` | Fix LazyInitializationException, N+1, transaction errors |
| `explain-code` | Structured code walkthrough for onboarding |
| `generate-javadoc` | Generate Javadoc comments following Oracle conventions |
| `generate-pr-description` | Generate structured PR description from changes |
| `generate-commit-message` | Conventional Commits formatted commit message |
| `implement-story` | End-to-end user story implementation across all layers |
