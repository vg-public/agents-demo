---
description: "Use when: adding a new field to an existing entity — updates entity, migration, DTOs, mapper, and tests across all layers."
agent: "agent"
tools: [read, edit, search, terminal]
argument-hint: "Entity name and new field — e.g., 'Add discountPercent (BigDecimal, 0-100, default 0) to Product'"
---

# Add Field to Existing Entity

You are adding a **new field to an existing entity** in a Java Spring Boot project with Oracle Database. This requires changes across multiple layers.

## Step-by-Step

### 1. Find All Affected Files

Search the codebase to locate:
- Entity class
- Create request DTO
- Update request DTO
- Response DTO
- MapStruct mapper
- Service implementation (if field has business logic implications)
- Existing tests

### 2. Update Entity

Add the field with proper JPA annotations:
```java
@Column(name = "<COLUMN_NAME>", nullable = false, precision = 5, scale = 2)
private BigDecimal fieldName = BigDecimal.ZERO;
```

### 3. Update DTOs

- **CreateRequest**: Add field with validation annotations if required on creation
- **UpdateRequest**: Add field without `@NotNull` (partial update)
- **Response**: Add field to expose in API responses

### 4. Verify Mapper

MapStruct will auto-map fields with matching names. If the field name differs between DTO and entity, add `@Mapping`.

### 5. Update Tests

- Add the new field to test data builders/fixtures
- Add assertions for the new field in service and controller tests

### 6. Compile and Verify

Run `mvn compile` to ensure MapStruct generates correctly and no compilation errors exist.

## Rules

- Use Oracle data types in SQL scripts (`VARCHAR2`, `NUMBER`, `TIMESTAMP WITH TIME ZONE`)
- If the field has a default, make the column `NOT NULL` with `DEFAULT`
- If the field is nullable, don't add `NOT NULL` constraint
