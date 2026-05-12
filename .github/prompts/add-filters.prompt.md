---
description: "Use when: adding search, filter, or sort capabilities to a list API endpoint — generates JPA Specifications, service updates, and controller query parameters."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Describe the filters — e.g., 'Add status, date range, and name search filters to the Product list endpoint'"
---

# Add Filters to List Endpoint

Add **search, filter, and sort capabilities** to an existing paginated list endpoint using Spring Data JPA Specifications.

## Instructions

1. Read the existing entity, repository, service, and controller
2. Understand what filter parameters the user wants

## What to Generate

### 1. Specification Class — `src/main/java/.../repository/specification/<Resource>Specifications.java`

```java
public final class ProductSpecifications {

    private ProductSpecifications() {} // utility class

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> nameLike(String search) {
        return (root, query, cb) ->
            search == null ? null : cb.like(cb.upper(root.get("name")), "%" + search.toUpperCase() + "%");
    }

    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("price"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.lessThanOrEqualTo(root.get("price"), max);
        };
    }

    public static Specification<Product> createdAfter(LocalDate from) {
        return (root, query, cb) ->
            from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }

    public static Specification<Product> createdBefore(LocalDate to) {
        return (root, query, cb) ->
            to == null ? null : cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
    }
}
```

### 2. Update Repository

Ensure the repository extends `JpaSpecificationExecutor<Entity>`:
```java
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> { ... }
```

### 3. Update Service

```java
@Transactional(readOnly = true)
public Page<ProductResponse> list(ProductStatus status, String search,
        BigDecimal minPrice, BigDecimal maxPrice,
        LocalDate fromDate, LocalDate toDate, Pageable pageable) {

    Specification<Product> spec = Specification
        .where(ProductSpecifications.hasStatus(status))
        .and(ProductSpecifications.nameLike(search))
        .and(ProductSpecifications.priceBetween(minPrice, maxPrice))
        .and(ProductSpecifications.createdAfter(fromDate))
        .and(ProductSpecifications.createdBefore(toDate));

    return productRepository.findAll(spec, pageable).map(productMapper::toResponse);
}
```

### 4. Update Controller

```java
@GetMapping
public ResponseEntity<Page<ProductResponse>> list(
        @RequestParam(required = false) ProductStatus status,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        Pageable pageable) {
    return ResponseEntity.ok(productService.list(status, search, minPrice, maxPrice, fromDate, toDate, pageable));
}
```

## Performance Considerations

- For text search filters, suggest a function-based Oracle index:
  ```sql
  CREATE INDEX IDX_<TABLE>_<COL>_UPPER ON <TABLE> (UPPER(<COLUMN>));
  ```
- For date range filters, suggest an index on the date column
- For enum/status filters, suggest an index if cardinality is low
