---
description: "Use when: generating Javadoc comments for service methods, controller endpoints, or entity classes — writes clear documentation following Oracle Java conventions."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Class or file to document — e.g., 'ProductServiceImpl' or 'all service classes'"
---

# Generate Javadoc

Add **Javadoc comments** to public methods and classes following Oracle Java conventions.

## Rules

### Class-Level Javadoc
```java
/**
 * Service implementation for managing {@link Product} entities.
 *
 * <p>Handles CRUD operations, validation, and business rules for products.
 * Uses {@link ProductRepository} for persistence and {@link ProductMapper}
 * for entity-DTO conversions.</p>
 *
 * @author API Team
 * @since 1.0
 * @see ProductService
 * @see ProductRepository
 */
```

### Method-Level Javadoc
```java
/**
 * Creates a new product from the given request data.
 *
 * <p>Validates that the SKU is unique before persisting. The product
 * is created with {@link ProductStatus#ACTIVE} status by default.</p>
 *
 * @param request the product creation request containing required fields
 * @return the created product response with generated ID and audit timestamps
 * @throws DuplicateResourceException if a product with the same SKU already exists
 * @throws IllegalArgumentException if the request contains invalid data
 */
```

### Controller Endpoint Javadoc
```java
/**
 * Retrieves a paginated list of products with optional filtering.
 *
 * @param status  filter by product status (optional)
 * @param search  search term for product name (optional, case-insensitive)
 * @param pageable pagination and sort parameters (default: page=0, size=20)
 * @return paginated list of product responses
 */
```

### Entity Field Javadoc
```java
/**
 * Stock Keeping Unit — unique business identifier for this product.
 * Format: uppercase alphanumeric with hyphens (e.g., {@code ELEC-001}).
 */
@Column(name = "SKU", nullable = false, unique = true, length = 50)
private String sku;
```

## What to Document

- All `public` methods in service interfaces and implementations
- All REST controller endpoints
- Entity classes and non-obvious fields
- Complex business logic methods
- Custom exceptions

## What NOT to Document

- Getters, setters, and constructors (unless non-trivial)
- Private methods (unless complex algorithms)
- Self-explanatory one-liner methods
- Test classes

## Style

- First sentence is a **summary** ending with a period — this appears in IDE tooltips
- Use `{@link ClassName}` for cross-references
- Use `{@code value}` for inline code
- Use `<p>` for paragraph breaks in longer descriptions
- Every `@param` and `@return` should be a complete phrase
- List all checked exceptions with `@throws`
