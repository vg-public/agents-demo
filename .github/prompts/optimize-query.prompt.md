---
description: "Use when: a JPA query is slow, detecting N+1 problems, or optimizing database access — analyzes query performance and suggests fixes."
agent: "agent"
tools: [read, search]
argument-hint: "Entity or repository class name, or describe the slow operation — e.g., 'OrderRepository.findAllByCustomerId is slow'"
---

# Optimize JPA Query Performance

Analyze and optimize **JPA/Hibernate query performance** in a Spring Boot application with Oracle Database.

## Diagnostic Steps

### 1. Read the Code

- Read the entity class to understand relationships and fetch types
- Read the repository method being called
- Read the service method to understand the transaction boundary
- Look for related entities that might cause N+1 queries

### 2. Identify Common Performance Issues

| Issue | Detection | Fix |
|-------|-----------|-----|
| **N+1 queries** | `@OneToMany` / `@ManyToOne` without explicit fetch | `@EntityGraph` or `JOIN FETCH` in `@Query` |
| **Loading unnecessary data** | `SELECT *` when only a few fields needed | DTO projection with `new com.example.dto.Response(...)` |
| **No pagination** | `findAll()` returning entire table | Use `Pageable` parameter |
| **Missing index** | Slow WHERE/ORDER BY on non-indexed column | Create Oracle index via DDL script |
| **Cartesian product** | Multiple `JOIN FETCH` on collections | Use two-query approach or `@BatchSize` |
| **Eager loading everywhere** | `FetchType.EAGER` on collections | Change to `FetchType.LAZY` |
| **Large IN clause** | `findAllById()` with hundreds of IDs | Batch into chunks of 1000 (Oracle limit) |

### 3. Recommended Fixes

**N+1 — EntityGraph approach:**
```java
@EntityGraph(attributePaths = {"category", "tags"})
Page<Product> findByStatus(ProductStatus status, Pageable pageable);
```

**N+1 — JPQL JOIN FETCH:**
```java
@Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.status = :status")
List<Product> findByStatusWithCategory(@Param("status") ProductStatus status);
```

**DTO projection (avoid loading full entity):**
```java
@Query("SELECT new com.example.dto.ProductSummary(p.id, p.name, p.price) FROM Product p WHERE p.status = :status")
Page<ProductSummary> findSummaries(@Param("status") ProductStatus status, Pageable pageable);
```

**Batch fetching:**
```java
@BatchSize(size = 20)
@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
private List<OrderItem> items;
```

**Two-query pattern for paginated collection fetch:**
```java
// Query 1: Get IDs with pagination
Page<Long> ids = productRepository.findIdsByStatus(status, pageable);

// Query 2: Fetch full entities with joins for those IDs only
List<Product> products = productRepository.findAllWithDetailsByIdIn(ids.getContent());
```

### 4. Oracle Index Optimization

If a query is slow due to missing indexes, suggest an Oracle DDL script:
```sql
-- Simple index
CREATE INDEX IDX_PRODUCTS_STATUS ON PRODUCTS (STATUS);

-- Composite index (column order matters — most selective first)
CREATE INDEX IDX_ORDERS_CUSTOMER_DATE ON ORDERS (CUSTOMER_ID, ORDER_DATE);

-- Function-based index for case-insensitive search
CREATE INDEX IDX_PRODUCTS_NAME_UPPER ON PRODUCTS (UPPER(PRODUCT_NAME));
```

## Output

1. **Diagnosis** — what's causing the slowdown
2. **Code fix** — optimized query/entity code
3. **Migration** (if index needed) — Oracle DDL script
4. **Verification** — how to confirm the fix (SQL logging, explain plan)
