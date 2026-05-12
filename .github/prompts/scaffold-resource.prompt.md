---
description: "Use when: scaffolding a complete new REST API resource from scratch — generates entity, repository, service, DTOs, mapper, controller, and tests in one pass."
agent: "agent"
tools: [read, edit, search, terminal]
argument-hint: "Resource name and fields — e.g., 'Product with fields: sku (String, unique), name (String), price (BigDecimal), status (enum: ACTIVE/INACTIVE)'"
---

# Scaffold New REST API Resource

You are scaffolding a **complete new Spring Boot REST API resource** for a Java 17+ / Spring Boot 3.2+ / Oracle Database project.

## Input Required

The user will provide:
- **Resource name** (e.g., Product, Order, Customer)
- **Fields** with types, constraints, and relationships

## What to Generate (in this exact order)

### 1. Entity — `src/main/java/.../entity/<Resource>.java`

```java
@Entity
@Table(name = "<TABLE_NAME>")
@EntityListeners(AuditingEntityListener.class)
public class <Resource> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "<resource>_seq")
    @SequenceGenerator(name = "<resource>_seq", sequenceName = "<TABLE>_SEQ", allocationSize = 1)
    private Long id;

    // Map fields with @Column annotations
    // Use @Version for optimistic locking
    // Use @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy for audit
}
```

### 2. Repository — `src/main/java/.../repository/<Resource>Repository.java`

- Extend `JpaRepository<Resource, Long>` and `JpaSpecificationExecutor<Resource>`
- Add derived query methods for unique fields
- Add `existsBy<Field>()` for duplicate checks

### 3. Service Interface — `src/main/java/.../service/<Resource>Service.java`

- Define CRUD methods: `create`, `getById`, `list` (paginated), `update`, `delete`
- Return response DTOs, accept request DTOs

### 4. Service Implementation — `src/main/java/.../service/impl/<Resource>ServiceImpl.java`

- Use constructor injection
- `@Transactional` for writes, `@Transactional(readOnly = true)` for reads
- Throw `ResourceNotFoundException` for missing entities
- Throw `DuplicateResourceException` for unique constraint violations
- Use mapper for entity ↔ DTO conversions

### 5. Request DTOs — `src/main/java/.../dto/request/Create<Resource>Request.java` and `Update<Resource>Request.java`

- Use Java **records**
- Add Jakarta validation: `@NotBlank`, `@NotNull`, `@Size`, `@DecimalMin`, `@Digits`, `@Pattern`
- Update DTO fields should be optional (no `@NotNull`)

### 6. Response DTO — `src/main/java/.../dto/response/<Resource>Response.java`

- Use Java **record**
- Include `id`, all business fields, and audit timestamps

### 7. MapStruct Mapper — `src/main/java/.../mapper/<Resource>Mapper.java`

```java
@Mapper(componentModel = "spring")
public interface <Resource>Mapper {
    <Resource> toEntity(Create<Resource>Request request);
    <Resource>Response toResponse(<Resource> entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(Update<Resource>Request request, @MappingTarget <Resource> entity);
}
```

### 8. Controller — `src/main/java/.../controller/<Resource>Controller.java`

- `@RestController` with `@RequestMapping("/api/v1/<resources>")`
- `@Tag` for OpenAPI documentation
- Return `ResponseEntity<T>` with proper HTTP status codes
- `@Valid` on all `@RequestBody` parameters
- POST returns 201 with Location header
- DELETE returns 204

### 9. Exception — `src/main/java/.../exception/<Resource>NotFoundException.java`

- Extend `ResourceNotFoundException` if it exists, otherwise create it

### 10. Tests — Service unit test + Controller MockMvc test

## Rules

- Detect the project's base package from existing source files
- Follow existing code conventions found in the project
- Use constructor injection exclusively (no `@Autowired`)
- All Oracle table/column names in UPPER_SNAKE_CASE
- Java field names in camelCase
