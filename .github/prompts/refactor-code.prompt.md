---
description: "Use when: refactoring Java code to improve readability, reduce duplication, or simplify complex methods — applies clean code principles without changing behavior."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Class or method to refactor — e.g., 'Simplify OrderServiceImpl.processOrder()' or 'Reduce duplication in ProductController'"
---

# Refactor Code

Refactor the specified Java Spring Boot code to improve **readability, maintainability, and adherence to clean code principles** — without changing external behavior.

## Instructions

1. Read the target code and understand its current behavior
2. Identify refactoring opportunities from the checklist below
3. Apply refactorings one at a time, explaining each change
4. Verify the refactored code preserves the same behavior

## Refactoring Checklist

### Method-Level
| Smell | Refactoring | Example |
|-------|------------|---------|
| Long method (>20 lines) | Extract method | Break into named private methods |
| Deep nesting (>3 levels) | Early return / guard clauses | `if (x == null) return;` instead of nested else |
| Multiple return types | Use Optional | `Optional<Product>` instead of null checks |
| Flag arguments | Split into separate methods | `create()` and `createDraft()` instead of `create(boolean isDraft)` |
| Magic numbers/strings | Extract constants | `private static final int MAX_RETRIES = 3;` |

### Class-Level
| Smell | Refactoring | Example |
|-------|------------|---------|
| God class (>300 lines) | Extract class / delegate | Split service by responsibility |
| Feature envy | Move method | Move logic to the class that owns the data |
| Duplicate code | Extract shared method or utility | Common validation → utility method |
| Primitive obsession | Introduce value object | `Money` record instead of `BigDecimal price` |
| Long parameter list (>4) | Introduce parameter object | `SearchCriteria` record |

### Spring Boot–Specific
| Smell | Refactoring | Example |
|-------|------------|---------|
| Business logic in controller | Move to service | Controllers should only delegate |
| Manual mapping code | Use MapStruct | Replace manual `new Response(entity.getX(), ...)` |
| Hardcoded config values | Externalize to `application.yml` | `@Value("${app.max-page-size}")` |
| String-based queries | Use JPA Specifications | Type-safe dynamic queries |
| Catching generic Exception | Catch specific exceptions | `catch (DataIntegrityViolationException e)` |

## Refactoring Patterns

### Before: Deep nesting
```java
public ProductResponse getProduct(Long id) {
    Product product = productRepository.findById(id).orElse(null);
    if (product != null) {
        if (product.getStatus() == ProductStatus.ACTIVE) {
            return productMapper.toResponse(product);
        } else {
            throw new InvalidOperationException("Product is not active");
        }
    } else {
        throw new ResourceNotFoundException("Product", id);
    }
}
```

### After: Guard clauses + Optional
```java
public ProductResponse getProduct(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product", id));

    if (product.getStatus() != ProductStatus.ACTIVE) {
        throw new InvalidOperationException("Product is not active");
    }

    return productMapper.toResponse(product);
}
```

### Before: Long parameter list
```java
public Page<Product> search(String name, String sku, ProductStatus status,
        BigDecimal minPrice, BigDecimal maxPrice, LocalDate from, LocalDate to,
        int page, int size, String sortBy) { ... }
```

### After: Parameter object
```java
public record ProductSearchCriteria(
    String name, String sku, ProductStatus status,
    BigDecimal minPrice, BigDecimal maxPrice,
    LocalDate from, LocalDate to
) {}

public Page<Product> search(ProductSearchCriteria criteria, Pageable pageable) { ... }
```

## Rules

- **Never change external behavior** — same inputs must produce same outputs
- Refactor in small, verifiable steps
- If tests exist, run them after each change
- If tests don't exist, suggest writing them first
- Preserve all existing Javadoc and update if method signatures change
- Use Java 17+ features where they simplify: records, pattern matching, enhanced switch, text blocks
