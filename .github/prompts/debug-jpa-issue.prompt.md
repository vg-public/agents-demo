---
description: "Use when: debugging JPA/Hibernate issues like LazyInitializationException, N+1 queries, transaction errors, or entity lifecycle problems in Spring Boot."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Paste the Hibernate error or describe the issue — e.g., 'LazyInitializationException on order.getItems()' or 'N+1 queries on product listing'"
---

# Debug JPA / Hibernate Issue

Diagnose and fix a **JPA/Hibernate issue** in a Spring Boot application with Oracle Database.

## Common Issues & Solutions

### LazyInitializationException
**Error**: `could not initialize proxy - no Session`

**Causes & Fixes**:

| Cause | Fix |
|-------|-----|
| Accessing lazy relation outside transaction | Use `@EntityGraph` or `JOIN FETCH` in repository |
| `spring.jpa.open-in-view=true` disabled (correct) | Fetch needed data within the `@Transactional` service method |
| Returning entity directly from controller | Map to DTO in service layer, return DTO from controller |

```java
// FIX 1: @EntityGraph
@EntityGraph(attributePaths = {"items", "items.product"})
Optional<Order> findWithItemsById(Long id);

// FIX 2: JPQL JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") Long id);
```

### N+1 Query Problem
**Symptom**: 1 query for parent + N queries for each child

```java
// PROBLEM: N+1 on listing
Page<Order> orders = orderRepository.findAll(pageable);
// Each order.getCustomer() fires a separate SELECT

// FIX: Use @EntityGraph for known relations
@EntityGraph(attributePaths = {"customer"})
Page<Order> findAll(Pageable pageable);

// FIX: Use DTO projection for read-only
@Query("SELECT new com.app.dto.OrderSummary(o.id, o.total, c.name) " +
       "FROM Order o JOIN o.customer c")
Page<OrderSummary> findOrderSummaries(Pageable pageable);
```

### Transaction Issues
**Error**: `No EntityManager with actual transaction available for current thread`

```java
// PROBLEM: Missing @Transactional
public void updateProduct(Long id, UpdateRequest request) { ... }

// FIX: Add annotation
@Transactional
public void updateProduct(Long id, UpdateRequest request) { ... }
```

**Error**: `Transaction silently rolled back` (self-invocation)
```java
// PROBLEM: Calling @Transactional method from same class
public void process() {
    this.save(item); // @Transactional on save() is IGNORED
}

// FIX: Inject self or extract to separate service
@Autowired
private ProductService self; // proxy-aware reference
```

### Detached Entity
**Error**: `detached entity passed to persist`

```java
// PROBLEM: Passing entity with ID to save()
Product product = new Product();
product.setId(existingId); // detached!
productRepository.save(product); // ERROR

// FIX: Fetch, then modify
Product product = productRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Product", id));
product.setName(request.name());
productRepository.save(product); // merge
```

### Dirty Checking / Unexpected Updates
**Symptom**: UPDATE fired without explicit `save()`

```java
// JPA dirty checking: any modification to a managed entity
// within a @Transactional method triggers an UPDATE at flush time.

// FIX: Use @Transactional(readOnly = true) for read operations
// FIX: Use DTO projections instead of entities for read-only queries
```

### Oracle-Specific Issues

| Error | Cause | Fix |
|-------|-------|-----|
| `ORA-02289: sequence does not exist` | Wrong sequence name | Verify `@SequenceGenerator(sequenceName = "ACTUAL_SEQ_NAME")` |
| `ORA-00942: table or view does not exist` | Wrong schema | Set `hibernate.default_schema` or use `@Table(schema = "SCHEMA")` |
| `ORA-01400: cannot insert NULL` | Missing required field | Check entity `@Column(nullable = false)` and ensure value is set |
| `ORA-00001: unique constraint violated` | Duplicate unique value | Check before insert or handle `DataIntegrityViolationException` |

## Debugging Steps

1. **Enable SQL logging** in `application-dev.yml`:
   ```yaml
   logging.level.org.hibernate.SQL: DEBUG
   logging.level.org.hibernate.type.descriptor.sql.BasicBinder: TRACE
   ```
2. **Count queries** — check Hibernate statistics:
   ```yaml
   spring.jpa.properties.hibernate.generate_statistics: true
   ```
3. **Check transaction boundaries** — ensure `@Transactional` is on the right method
4. **Verify fetch strategy** — check `@ManyToOne(fetch = LAZY)` vs `EAGER`
5. **Test in isolation** — use `@DataJpaTest` to reproduce

## Rules

- Always use `FetchType.LAZY` as default — optimize with `@EntityGraph` where needed
- Use `@Transactional(readOnly = true)` for all read operations
- Never return JPA entities from controllers — always map to DTOs in service layer
- Keep `spring.jpa.open-in-view: false` — forces proper fetch patterns
