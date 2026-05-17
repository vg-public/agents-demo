# Copilot Workspace Instructions

## Project Overview

This workspace is a **Java Spring Boot REST API** project for building production-grade database CRUD APIs using **Spring Data JPA** with **Oracle Database**. Custom Copilot agents assist with generating project artifacts, implementing features, and maintaining code quality.

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 17+ (LTS) |
| Framework | Spring Boot | 3.2+ |
| Data Access | Spring Data JPA (Hibernate 6) | Latest |
| Database | Oracle Database | 19c / 21c / 23ai |
| Build Tool | Maven | 3.9+ |
| API Docs | SpringDoc OpenAPI (Swagger) | 2.x |
| Validation | Jakarta Bean Validation | 3.x |
| Mapping | MapStruct | 1.5+ |
| Testing | JUnit 5 + Mockito + AssertJ | Latest |
| Logging | SLF4J + Logback | Default |
| Containerization | Docker + Docker Compose | Latest |

## Project Structure

```
src/
├── main/
│   ├── java/com/<group>/<artifact>/
│   │   ├── config/                  # Configuration classes (@Configuration)
│   │   ├── controller/              # REST controllers (@RestController)
│   │   ├── dto/
│   │   │   ├── request/             # Request DTOs with validation
│   │   │   └── response/            # Response DTOs
│   │   ├── entity/                  # JPA entities (@Entity)
│   │   ├── exception/               # Custom exceptions + @RestControllerAdvice
│   │   ├── mapper/                  # MapStruct mappers (@Mapper)
│   │   ├── repository/              # Spring Data JPA repositories
│   │   ├── service/                 # Service interfaces
│   │   │   └── impl/               # Service implementations (@Service)
│   │   └── util/                    # Utility classes
│   └── resources/
│       ├── application.yml          # Main configuration
│       ├── application-dev.yml      # Dev profile
│       └── application-prod.yml     # Production profile
├── test/
│   └── java/com/<group>/<artifact>/
│       ├── controller/              # MockMvc integration tests
│       ├── repository/              # @DataJpaTest tests
│       ├── service/                 # Unit tests with Mockito
│       └── integration/             # Full integration tests
└── pom.xml
```

## Coding Standards

### General
- Use **Java 17+ features**: records, sealed classes, pattern matching, text blocks, enhanced switch
- Follow **Oracle Java naming conventions**: camelCase methods/fields, PascalCase classes, UPPER_SNAKE constants
- Use **constructor injection** exclusively (no field injection with `@Autowired`)
- Use **Lombok** sparingly — prefer records for DTOs, explicit constructors for services
- All public methods must have **Javadoc** with `@param`, `@return`, `@throws`

### Controller Layer
- Prefix all endpoints with `/api/v1/<resource>`
- Return `ResponseEntity<T>` for explicit HTTP status control
- Use `@Valid` on all `@RequestBody` parameters
- Keep controllers thin — delegation only, no business logic

### Service Layer
- Define service interfaces + implementation classes
- Use `@Transactional(readOnly = true)` for read operations
- Use `@Transactional` for write operations
- Throw custom business exceptions (never generic RuntimeException)

### Repository Layer
- Extend `JpaRepository<Entity, Long>` for full CRUD
- Use derived query methods for simple lookups
- Use `@Query` with JPQL for complex queries
- Use `Pageable` for all list operations

### Entity Layer
- Use `@Entity` + `@Table(name = "TABLE_NAME")` with explicit Oracle table names
- Map Oracle sequences with `@SequenceGenerator` + `@GeneratedValue(strategy = SEQUENCE)`
- Include audit fields (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`)
- Use `@PrePersist` and `@PreUpdate` lifecycle callbacks

### Error Handling
- Global `@RestControllerAdvice` with RFC 7807 ProblemDetail responses
- Custom exception hierarchy: `BusinessException` → specific exceptions
- Never expose stack traces or internal details in API responses

### Oracle-Specific
- Use Oracle sequences (not auto-increment) for primary keys
- Use `VARCHAR2`, `NUMBER`, `TIMESTAMP WITH TIME ZONE` in migrations
- Use Oracle-specific hints only when justified with performance evidence
- Configure HikariCP with Oracle UCP properties for connection pooling

## Mandatory Quality Gates

All code generated or modified by agents and prompts **MUST** comply with these non-negotiable quality gates.

### Code Coverage
- **Minimum 80% line and branch coverage** is mandatory for all new and modified code
- Coverage is measured by **JaCoCo** and enforced by the SonarQube quality gate
- The `@testing` agent must **iteratively add and fix tests** until 80% coverage is achieved on the target code
- Coverage excludes: entity classes (getters/setters), DTOs (records), configuration classes, and the main `Application.java`
- Run `mvn test jacoco:report` to generate the coverage report after test changes

### SonarQube — Zero Tolerance for HIGH/CRITICAL
- **Zero CRITICAL** findings allowed — bugs, vulnerabilities, or security hotspots
- **Zero HIGH** (Blocker/Critical severity) findings allowed
- All generated code must proactively avoid these common SonarQube violations:

| Rule | Category | What to Avoid |
|------|----------|---------------|
| S2259 | Bug | Null pointer dereference — always use `Optional` or null checks |
| S2095 | Bug | Unclosed resources — always use try-with-resources |
| S3649 | Vulnerability | SQL injection — never concatenate strings in queries, use `@Param` |
| S2068 | Vulnerability | Hardcoded credentials — use environment variables |
| S4790 | Security Hotspot | Weak hash (MD5/SHA-1) — use SHA-256 or BCrypt |
| S2245 | Security Hotspot | Insecure random — use `SecureRandom` |
| S3776 | Code Smell | High cognitive complexity (>15) — extract methods, use early returns |
| S1192 | Code Smell | Duplicated string literals (>3 times) — extract to `static final` constant |
| S106 | Code Smell | `System.out.println` — use SLF4J `log.info()` / `log.debug()` |
| S4684 | Security | JPA entity used as API request/response — always use DTOs |
| S1948 | Bug | Non-serializable field in serializable class |
| S1874 | Code Smell | Deprecated API usage — use recommended alternatives |
| S2583 | Bug | Condition always true/false — remove dead code |
| S5131 | Vulnerability | XSS — sanitize output |
| S2092 | Security Hotspot | Cookies without `secure` flag |

### Code Safety Guardrail
- **DO NOT modify existing working code** unless explicitly asked or required to fix a bug
- When reviewing or fixing, scope changes to **only the new/changed code**
- Preserve existing tests — add new tests, never delete passing tests
- If a fix requires modifying existing code, explain the impact and get confirmation

## Available Agents

| Agent | Purpose | Output |
|-------|---------|--------|
| `@dev-orchestrator` | Orchestrates tasks to the right agent — start here if unsure | (orchestration) |
| `@java-api-dev` | Build Spring Boot REST API endpoints, services, entities, DTOs | `src/` |
| `@new-api-scaffold` | Scaffold a complete new resource (all layers + tests) | `src/` |
| `@api-modification` | Safely modify existing APIs across all layers | `src/` |
| `@sql-data` | Generate Oracle SQL schema, sequences, and seed data | `src/main/resources/db/` |
| `@testing` | Write JUnit 5 + Mockito unit and integration tests | `src/test/` |
| `@bug-fix` | Diagnose and fix bugs in Java/Spring Boot code | `src/` |
| `@fortify-fix` | Triage and fix Fortify SAST vulnerabilities with minimal code changes | `src/` |
| `@code-review` | Review code for correctness, security (OWASP 2025), SonarQube rules, Java 17+/Spring Boot 3.2+ — interactive review + fix | `src/` |
| `@perf-optimizer` | Optimize JPA queries, connection pools, caching | `src/` |
| `@doc-gen` | Generate README, Javadoc, ADRs, API documentation | `docs/` |

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
