---
description: "Use when: adding audit fields (createdAt, updatedAt, createdBy, updatedBy) to entities — configures JPA auditing with Spring Data and AuditorAware."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Entity or entities to add auditing to — e.g., 'Add audit fields to Product and Category entities'"
---

# Add JPA Auditing

Set up **Spring Data JPA Auditing** with automatic timestamp and user tracking on entities.

## 1. Audit Configuration — `config/AuditConfig.java`

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // With Spring Security:
            // return Optional.ofNullable(SecurityContextHolder.getContext())
            //     .map(SecurityContext::getAuthentication)
            //     .filter(Authentication::isAuthenticated)
            //     .map(Authentication::getName);

            // Without Spring Security (fallback):
            return Optional.of("system");
        };
    }
}
```

## 2. Auditable Base Entity — `entity/BaseEntity.java`

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "CREATED_BY", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
```

## 3. Entity Usage

```java
@Entity
@Table(name = "PRODUCTS")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "PRODUCT_SEQ", allocationSize = 1)
    private Long id;

    // ... business fields only, no audit fields needed
}
```

## 4. Oracle DDL Script

```sql
-- add_audit_columns.sql
ALTER TABLE PRODUCTS ADD CREATED_AT TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL;
ALTER TABLE PRODUCTS ADD UPDATED_AT TIMESTAMP WITH TIME ZONE;
ALTER TABLE PRODUCTS ADD CREATED_BY VARCHAR2(100) DEFAULT 'system' NOT NULL;
ALTER TABLE PRODUCTS ADD UPDATED_BY VARCHAR2(100);
```

## 5. Response DTO (include audit fields)

```java
public record ProductResponse(
    Long id,
    // ... business fields
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy
) {}
```

## Rules

- Use `@MappedSuperclass` for the base entity — not `@Inheritance`
- `CREATED_AT` and `CREATED_BY` must be `updatable = false`
- Use `OffsetDateTime` (not `LocalDateTime`) for Oracle `TIMESTAMP WITH TIME ZONE`
- Never expose `createdBy`/`updatedBy` in create/update request DTOs
- Always include audit fields in response DTOs
- Exclude audit fields from `equals()`/`hashCode()`
