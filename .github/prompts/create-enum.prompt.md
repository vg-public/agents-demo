---
description: "Use when: converting a String field to an enum type, adding a new enum, or refactoring existing enum usage — updates entity, DTOs, migration, and validation."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Describe the enum — e.g., 'Convert Product.status from String to enum with values: ACTIVE, INACTIVE, DISCONTINUED'"
---

# Create or Convert to Enum

Create a **Java enum** and update all layers when converting a String field to an enum type, or adding a new enum field.

## What to Generate

### 1. Enum Class — `src/main/java/.../entity/<EnumName>.java` or `.../enums/<EnumName>.java`

```java
/**
 * Represents the lifecycle status of a product.
 */
public enum ProductStatus {
    ACTIVE,
    INACTIVE,
    DISCONTINUED;
}
```

For enums with display values:
```java
public enum OrderStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### 2. Update Entity

```java
// BEFORE (String)
@Column(name = "STATUS", length = 20)
private String status;

// AFTER (Enum)
@Enumerated(EnumType.STRING)
@Column(name = "STATUS", nullable = false, length = 20)
private ProductStatus status = ProductStatus.ACTIVE;
```

### 3. Update DTOs

```java
// Create request — enum validates automatically (rejects invalid values)
public record CreateProductRequest(
    // ...
    ProductStatus status   // null = use entity default
) {}

// Response
public record ProductResponse(
    // ...
    ProductStatus status
) {}
```

### 4. Oracle CHECK Constraint (if needed)

```sql
-- add_status_check_constraint.sql
ALTER TABLE PRODUCTS ADD CONSTRAINT CK_PRODUCTS_STATUS
    CHECK (STATUS IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED'));
```

### 5. Update Repository Queries

```java
// Derived queries work naturally with enums
Page<Product> findByStatus(ProductStatus status, Pageable pageable);

// Specification
public static Specification<Product> hasStatus(ProductStatus status) {
    return (root, query, cb) ->
        status == null ? null : cb.equal(root.get("status"), status);
}
```

## Rules

- Always use `@Enumerated(EnumType.STRING)` — never `ORDINAL` (fragile, breaks on reorder)
- Set a default value in the entity field declaration
- Oracle stores the enum as `VARCHAR2` — the CHECK constraint is optional but recommended
- Jackson serializes/deserializes enum names automatically — no custom converter needed
- Invalid enum values in JSON requests automatically return 400 Bad Request
