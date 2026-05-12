---
description: "Use when: adding JPA relationships between entities (@OneToMany, @ManyToOne, @ManyToMany, @OneToOne) — generates entity mappings, DTOs, and Oracle DDL scripts."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Describe the relationship — e.g., 'Product belongs to Category (ManyToOne)', 'Order has many OrderItems (OneToMany)'"
---

# Add Entity Relationship

Add a **JPA relationship** between two existing entities, updating all layers.

## Instructions

1. Read both entities to understand existing fields and table names
2. Determine the relationship type and owning side

## Relationship Patterns

### @ManyToOne (child → parent, owning side)

**Entity (child side — Product):**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "CATEGORY_ID", nullable = false,
    foreignKey = @ForeignKey(name = "FK_PRODUCT_CATEGORY"))
private Category category;
```

**Entity (parent side — Category, inverse/optional):**
```java
@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Product> products = new ArrayList<>();
```

### @OneToMany (parent → children)

```java
// Parent entity
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> items = new ArrayList<>();

// Helper methods on parent
public void addItem(OrderItem item) {
    items.add(item);
    item.setOrder(this);
}

public void removeItem(OrderItem item) {
    items.remove(item);
    item.setOrder(null);
}
```

### @ManyToMany (join table)

```java
// Owning side
@ManyToMany
@JoinTable(
    name = "PRODUCT_TAGS",
    joinColumns = @JoinColumn(name = "PRODUCT_ID"),
    inverseJoinColumns = @JoinColumn(name = "TAG_ID"),
    foreignKey = @ForeignKey(name = "FK_PT_PRODUCT"),
    inverseForeignKey = @ForeignKey(name = "FK_PT_TAG")
)
private Set<Tag> tags = new HashSet<>();

// Inverse side
@ManyToMany(mappedBy = "tags")
private Set<Product> products = new HashSet<>();
```

### @OneToOne

```java
@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "ADDRESS_ID", unique = true,
    foreignKey = @ForeignKey(name = "FK_USER_ADDRESS"))
private Address address;
```

## Oracle DDL Script

```sql
-- add_category_to_products.sql
ALTER TABLE PRODUCTS ADD CATEGORY_ID NUMBER(19);

ALTER TABLE PRODUCTS ADD CONSTRAINT FK_PRODUCT_CATEGORY
    FOREIGN KEY (CATEGORY_ID) REFERENCES CATEGORIES(ID);

CREATE INDEX IDX_PRODUCTS_CATEGORY_ID ON PRODUCTS(CATEGORY_ID);

-- If NOT NULL required, run data update first:
-- UPDATE PRODUCTS SET CATEGORY_ID = (SELECT ID FROM CATEGORIES WHERE NAME = 'Default');
-- ALTER TABLE PRODUCTS MODIFY CATEGORY_ID NUMBER(19) NOT NULL;
```

## DTO Updates

### Request DTO — use ID reference (not nested object)
```java
public record CreateProductRequest(
    // ... existing fields
    @NotNull Long categoryId
) {}
```

### Response DTO — include summary (not full nested entity)
```java
public record ProductResponse(
    // ... existing fields
    Long categoryId,
    String categoryName  // denormalized for convenience
) {}
```

## Service Updates

```java
public ProductResponse create(CreateProductRequest request) {
    Category category = categoryRepository.findById(request.categoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

    Product product = productMapper.toEntity(request);
    product.setCategory(category);
    // ...
}
```

## Rules

- Always use `FetchType.LAZY` — avoid `EAGER` (N+1 risk)
- Use `@EntityGraph` or `JOIN FETCH` queries where you need eager loading
- Always create a foreign key index on the child table column
- In DTOs: use IDs for inputs, use summary data for responses — never nest full entities
- Add `orphanRemoval = true` on `@OneToMany` only if children cannot exist without parent
- Add `cascade = CascadeType.ALL` only if the parent owns the full lifecycle
