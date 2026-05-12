---
name: performance-optimization
description: "Guide for profiling and optimizing Java Spring Boot API performance, including JPA/Hibernate queries, Oracle Database tuning, HikariCP connection pools, Spring caching, and batch operations. Use when diagnosing slow endpoints or high resource usage."
---

# Performance Optimization — Java Spring Boot + Oracle

This skill guides the AI through identifying and resolving performance bottlenecks in Java Spring Boot REST API applications with Spring Data JPA and Oracle Database.

## When to Use

- API response times exceed acceptable thresholds (>500ms for CRUD operations)
- High memory consumption or memory leaks
- CPU spikes under load
- Database queries are slow
- Connection pool exhaustion
- N+1 query problems

## General Approach

1. **Measure first**: Never optimize without profiling. Identify the actual bottleneck.
2. **Set a target**: Define what "fast enough" means (e.g., 200ms API response, <500 queries per request).
3. **Optimize the bottleneck**: Fix the single biggest contributor first.
4. **Measure again**: Verify the optimization helped and didn't introduce regressions.
5. **Repeat**: Move to the next bottleneck if needed.

---

## Measurement Tools

| Tool | Purpose | How to Enable |
|------|---------|---------------|
| Hibernate Statistics | Query count, time, cache hits | `hibernate.generate_statistics=true` |
| SQL Logging | See generated SQL + bind params | `org.hibernate.SQL=DEBUG` |
| Spring Boot Actuator | Endpoint timing, JVM metrics | Add `spring-boot-starter-actuator` |
| Micrometer + Prometheus | Production monitoring | Add `micrometer-registry-prometheus` |
| Oracle EXPLAIN PLAN | Query execution plan analysis | `EXPLAIN PLAN FOR <query>` |
| JVisualVM / JFR | CPU profiling, heap analysis | Connect to running JVM |
| HikariCP Metrics | Connection pool utilization | `spring.datasource.hikari` logging |

---

## JPA/Hibernate Performance

### N+1 Database Queries (Most Common Issue)

**Detection**: Enable `hibernate.generate_statistics=true` and count queries per request.

**Fix 1 — JOIN FETCH**:
```java
@Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.status = :status")
List<Product> findByStatusWithCategory(@Param("status") ProductStatus status);
```

**Fix 2 — @EntityGraph**:
```java
@EntityGraph(attributePaths = {"category", "images"})
Optional<Product> findById(Long id);
```

**Fix 3 — Batch Fetch Size** (global fallback):
```yaml
spring.jpa.properties.hibernate.default_batch_fetch_size: 25
```

### DTO Projections (Skip Entity Overhead)

When you only need a few columns:
```java
// Interface projection
public interface ProductSummaryProjection {
    Long getId();
    String getName();
    BigDecimal getPrice();
}

@Query("SELECT p.id AS id, p.name AS name, p.price AS price FROM Product p WHERE p.status = 'ACTIVE'")
List<ProductSummaryProjection> findActiveProductSummaries();

// Constructor projection
@Query("SELECT new com.example.dto.response.ProductSummary(p.id, p.name, p.price) FROM Product p")
Page<ProductSummary> findAllSummaries(Pageable pageable);
```

### Pagination with Collections (Oracle Optimization)

**Problem**: `JOIN FETCH` with `Pageable` causes Hibernate to fetch all rows then paginate in memory.

**Fix — Two-query approach**:
```java
// Step 1: Paginate IDs only
@Query("SELECT p.id FROM Product p WHERE p.status = :status")
Page<Long> findIdsByStatus(@Param("status") ProductStatus status, Pageable pageable);

// Step 2: Fetch full entities by IDs
@EntityGraph(attributePaths = {"category", "images"})
@Query("SELECT p FROM Product p WHERE p.id IN :ids")
List<Product> findByIdInWithDetails(@Param("ids") List<Long> ids);
```

### Batch Insert/Update

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
          order_inserts: true
          order_updates: true
```

```java
@Transactional
public void bulkInsert(List<CreateProductRequest> requests) {
    List<Product> products = requests.stream()
            .map(productMapper::toEntity)
            .toList();
    productRepository.saveAll(products);  // Batched by Hibernate
}
```

### Second-Level Cache (Ehcache / Caffeine)

```java
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Category extends BaseEntity { ... }
```

```yaml
spring.jpa.properties.hibernate:
  cache.use_second_level_cache: true
  cache.use_query_cache: true
  cache.region.factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
```

---

## Oracle Database Performance

### Index Optimization

```sql
-- Check existing indexes
SELECT index_name, column_name, column_position
FROM user_ind_columns WHERE table_name = 'PRODUCTS'
ORDER BY index_name, column_position;

-- Composite index for common filter pattern
CREATE INDEX IDX_PRODUCTS_CAT_STATUS ON PRODUCTS(CATEGORY_ID, STATUS);

-- Function-based index for case-insensitive search
CREATE INDEX IDX_PRODUCTS_NAME_UPPER ON PRODUCTS(UPPER(NAME));

-- Covering index (includes all columns in SELECT)
CREATE INDEX IDX_PRODUCTS_SUMMARY ON PRODUCTS(STATUS, ID, NAME, PRICE);
```

### Query Plan Analysis

```sql
EXPLAIN PLAN FOR
SELECT * FROM PRODUCTS WHERE CATEGORY_ID = 42 AND STATUS = 'ACTIVE';

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY());
```

Look for:
- `TABLE ACCESS FULL` on large tables → needs an index
- `NESTED LOOPS` with many iterations → consider `HASH JOIN`
- High `Bytes` estimate → too many rows being processed

### Gather Statistics

```sql
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS(
        ownname => 'APP_SCHEMA',
        tabname => 'PRODUCTS',
        estimate_percent => DBMS_STATS.AUTO_SAMPLE_SIZE,
        cascade => TRUE  -- includes indexes
    );
END;
/
```

---

## HikariCP Connection Pool

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20          # Formula: (CPU cores * 2) + spindle count
      minimum-idle: 5
      connection-timeout: 30000      # 30 sec
      idle-timeout: 300000           # 5 min
      max-lifetime: 600000           # 10 min
      leak-detection-threshold: 60000  # 60 sec
      pool-name: AppHikariPool
      connection-test-query: SELECT 1 FROM DUAL
```

---

## Spring Caching

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());
        return cacheManager;
    }
}
```

```java
@Cacheable(value = "categories", key = "#id")
public CategoryResponse getCategoryById(Long id) { ... }

@CacheEvict(value = "categories", key = "#id")
public CategoryResponse updateCategory(Long id, ...) { ... }

@CacheEvict(value = "categories", allEntries = true)
public void deleteCategory(Long id) { ... }
```

---

## API Layer Optimizations

- **Response compression**: `server.compression.enabled=true`
- **Async operations**: Use `@Async` for fire-and-forget (email, audit logging)
- **HTTP caching**: Return `ETag` and `Cache-Control` headers for idempotent GET endpoints
- **Parallel service calls**: Use `CompletableFuture.allOf()` for independent data fetching

```java
@Async
public CompletableFuture<Void> sendNotification(Long productId) { ... }
```

---

## Anti-Patterns to Avoid

- Do NOT optimize without measuring — premature optimization wastes time
- Do NOT add indexes on every column — they have write/storage cost
- Do NOT cache mutable data without an invalidation strategy
- Do NOT use `FetchType.EAGER` on collections — causes N+1 on every query
- Do NOT hold database connections during external HTTP calls
- Do NOT set `spring.jpa.open-in-view=true` — causes hidden N+1 queries
- Do NOT return unbounded results from any endpoint — always use `Pageable`
