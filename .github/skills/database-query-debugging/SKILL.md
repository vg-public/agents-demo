---
name: database-query-debugging
description: "Guide for debugging and optimizing JPA/Hibernate queries with Oracle Database, diagnosing N+1 problems, fixing migration failures, resolving connection pool issues, and tuning Oracle SQL. Use when facing database-related issues in Spring Boot applications."
---

# Database Query Debugging — JPA/Hibernate + Oracle Database

This skill guides the AI through diagnosing and fixing database-related issues in Spring Boot applications using Spring Data JPA (Hibernate 6) with Oracle Database.

## When to Use

- JPA/Hibernate queries returning unexpected results
- N+1 query problems degrading performance
- `LazyInitializationException` or proxy initialization errors
- Oracle-specific query performance issues
- HikariCP connection pool exhaustion
- Oracle sequence or constraint violations
- Slow queries needing Oracle execution plan analysis

## Debugging Workflow

### 1. Enable SQL Logging

Add these properties for full Hibernate SQL visibility:

```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: false  # Use logging config below instead (formatted output)
    properties:
      hibernate:
        format_sql: true
        generate_statistics: true

logging:
  level:
    org.hibernate.SQL: DEBUG                              # Log SQL statements
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # Log bind parameters
    org.hibernate.stat: DEBUG                             # Log session statistics
    com.zaxxer.hikari: DEBUG                              # Log connection pool activity
```

### Hibernate Statistics Example Output
```
Session Metrics {
    23 queries executed
    1 second query execution time
    4 JDBC batches (50 statements each)
    0 L2C puts/hits/misses
    3 flushes, 12 entities loaded, 2 collections loaded
}
```

### 2. Common JPA/Hibernate Issues and Fixes

#### N+1 Query Problem

**Symptom**: One query loads a list of entities, then N additional queries load each entity's association.

**Detection**: Enable `hibernate.generate_statistics=true` and count queries per request.

**Example — The Problem**:
```java
// BAD: Triggers N+1 when accessing category.getName() in the mapper
List<Product> products = productRepository.findAll();
products.forEach(p -> log.info(p.getCategory().getName()));  // N additional queries!
```

**Fix 1 — JOIN FETCH in JPQL**:
```java
@Query("SELECT p FROM Product p JOIN FETCH p.category")
List<Product> findAllWithCategory();
```

**Fix 2 — @EntityGraph**:
```java
@EntityGraph(attributePaths = {"category"})
List<Product> findAll();

// Multiple associations
@EntityGraph(attributePaths = {"category", "images"})
Optional<Product> findById(Long id);
```

**Fix 3 — Batch Fetching (Hibernate tuning)**:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 25
```

#### LazyInitializationException

**Symptom**: `LazyInitializationException: could not initialize proxy - no Session`

**Cause**: Accessing a lazy-loaded association after the Hibernate session is closed (common when `spring.jpa.open-in-view=false`).

**Fix 1** (Recommended): Use `JOIN FETCH` or `@EntityGraph` in the repository query:
```java
@Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
Optional<Product> findByIdWithCategory(@Param("id") Long id);
```

**Fix 2**: Ensure the service method is `@Transactional` and access the association inside the transaction:
```java
@Transactional(readOnly = true)
public ProductResponse getById(Long id) {
    Product product = productRepository.findById(id).orElseThrow(...);
    // Access lazy association inside the transaction
    String categoryName = product.getCategory().getName();
    return new ProductResponse(..., categoryName, ...);
}
```

**Anti-pattern — DO NOT use**: `spring.jpa.open-in-view=true` (keeps session open in controller, causes hidden N+1 queries and transaction leaks).

#### OptimisticLockException

**Symptom**: `OptimisticLockException` or `StaleObjectStateException`

**Cause**: Two transactions try to update the same row — the `@Version` field detects the conflict.

**Fix**: Catch `OptimisticLockingFailureException` in the exception handler and return 409 Conflict:
```java
@ExceptionHandler(OptimisticLockingFailureException.class)
public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "The resource was modified by another user. Please refresh and retry.");
    problem.setTitle("Concurrent Modification");
    return problem;
}
```

#### Incorrect Query Results

- **Wrong JOIN type**: `INNER JOIN` excludes non-matching rows — use `LEFT JOIN FETCH` if the association is optional
- **Missing `DISTINCT`**: `JOIN FETCH` on collections can duplicate parent rows — add `DISTINCT`:
  ```java
  @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.category.id = :catId")
  List<Product> findByCategoryWithImages(@Param("catId") Long catId);
  ```
- **NULL handling**: `= NULL` doesn't work in JPQL — use `IS NULL`:
  ```java
  @Query("SELECT p FROM Product p WHERE p.description IS NULL")
  ```
- **Enum comparison**: Ensure `@Enumerated(EnumType.STRING)` matches the database column values

### 3. Oracle-Specific Query Debugging

#### Viewing Oracle Execution Plans

```sql
-- Generate execution plan
EXPLAIN PLAN FOR
SELECT * FROM PRODUCTS WHERE CATEGORY_ID = 42 AND STATUS = 'ACTIVE';

-- View the plan
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY());

-- Autotrace in SQL*Plus
SET AUTOTRACE ON EXPLAIN STATISTICS
SELECT * FROM PRODUCTS WHERE CATEGORY_ID = 42;
```

#### Common Oracle Performance Fixes

**Missing Index**:
```sql
-- Check if index exists
SELECT index_name, column_name FROM user_ind_columns
WHERE table_name = 'PRODUCTS' ORDER BY index_name, column_position;

-- Create composite index for common filter
CREATE INDEX IDX_PRODUCTS_CAT_STATUS ON PRODUCTS(CATEGORY_ID, STATUS);
```

**Full Table Scan on Large Table**:
```sql
-- Check table statistics are up to date
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS(
        ownname => 'APP_SCHEMA',
        tabname => 'PRODUCTS',
        estimate_percent => DBMS_STATS.AUTO_SAMPLE_SIZE
    );
END;
/
```

**Function-Based Index** (for case-insensitive search):
```sql
-- UPPER(NAME) prevents index usage on NAME column
CREATE INDEX IDX_PRODUCTS_NAME_UPPER ON PRODUCTS(UPPER(NAME));

-- JPQL query that uses it
@Query("SELECT p FROM Product p WHERE UPPER(p.name) LIKE UPPER(CONCAT('%', :keyword, '%'))")
Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);
```

**Oracle Sequence Gaps**:
- Sequence gaps are normal — Hibernate pre-allocates IDs based on `allocationSize`
- Set `allocationSize` in `@SequenceGenerator` to match Oracle sequence `INCREMENT BY`
- Use `allocationSize = 50` for high-throughput tables, `allocationSize = 1` for low-volume tables

### 4. Oracle DDL Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `ORA-00955: name is already used` | Table/sequence already exists | Check existence before creating |
| `ORA-02292: integrity constraint violated - child record found` | FK prevents DROP | Drop child tables first or use `CASCADE CONSTRAINTS` |
| `ORA-01031: insufficient privileges` | Missing Oracle grants | `GRANT CREATE TABLE, CREATE SEQUENCE TO app_user` |
| `ORA-00942: table or view does not exist` | Wrong schema or table name | Verify schema and table names |

**Oracle-safe existence check pattern**:
```sql
-- Check if table exists before creating
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'PRODUCTS';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE PRODUCTS (...)';
    END IF;
END;
/
```

### 5. HikariCP Connection Pool Debugging

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `Connection is not available, request timed out` | Pool exhausted — all connections in use | Increase `maximum-pool-size`, check for connection leaks |
| `Connection leak detected` | Code opened a connection but didn't close it | Ensure `@Transactional` is used, check `try-with-resources` for raw connections |
| `Connection closed` unexpectedly | Oracle idle timeout killed the connection | Set `max-lifetime` < Oracle `idle_time` profile setting |
| Slow connection acquisition | Pool too small for load | Tune `maximum-pool-size = (core_count * 2) + spindle_count` |

**HikariCP Configuration for Oracle**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000          # 5 min
      max-lifetime: 600000          # 10 min (must be < Oracle profile idle_time)
      connection-timeout: 30000     # 30 sec
      leak-detection-threshold: 60000  # 60 sec — log warning if connection held too long
      pool-name: AppHikariPool
      connection-test-query: SELECT 1 FROM DUAL
      data-source-properties:
        oracle.jdbc.defaultRowPrefetch: 50
```

### 6. JPA Pagination with Oracle

**Problem**: Hibernate generates inefficient pagination SQL for Oracle with `JOIN FETCH` on collections.

**Fix**: Use two-query approach:
```java
// Query 1: Get paginated IDs
@Query("SELECT p.id FROM Product p WHERE p.status = :status ORDER BY p.id")
Page<Long> findIdsByStatus(@Param("status") ProductStatus status, Pageable pageable);

// Query 2: Fetch full entities by IDs with eager associations
@EntityGraph(attributePaths = {"category", "images"})
@Query("SELECT p FROM Product p WHERE p.id IN :ids")
List<Product> findByIdInWithDetails(@Param("ids") List<Long> ids);

// Service method combining both
@Transactional(readOnly = true)
public Page<ProductResponse> findByStatus(ProductStatus status, Pageable pageable) {
    Page<Long> idPage = productRepository.findIdsByStatus(status, pageable);
    List<Product> products = productRepository.findByIdInWithDetails(idPage.getContent());
    List<ProductResponse> responses = products.stream()
            .map(productMapper::toResponse)
            .toList();
    return new PageImpl<>(responses, pageable, idPage.getTotalElements());
}
```

## Anti-Patterns to Avoid

- Do NOT concatenate user input into SQL strings — always use parameterized queries (`@Param`)
- Do NOT use `SELECT *` in native queries — select only needed columns
- Do NOT ignore Hibernate statistics — they reveal hidden N+1 problems
- Do NOT set `spring.jpa.hibernate.ddl-auto=update` in production — use `validate`
- Do NOT use `FetchType.EAGER` on collections — use `JOIN FETCH` or `@EntityGraph` when needed
- Do NOT add indexes on every column — they have write/storage cost. Profile first.
- Do NOT cache mutable data without an invalidation strategy
- Do NOT hold database connections during external HTTP calls — release the connection first
