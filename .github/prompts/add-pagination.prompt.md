---
description: "Use when: adding pagination and sorting to an existing list endpoint — configures Pageable, default sort, and paginated response wrapping."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Endpoint to paginate — e.g., 'Add pagination to GET /api/v1/products' or 'Add custom sort options to order listing'"
---

# Add Pagination and Sorting

Add **pagination and sorting** to an existing list endpoint using Spring Data's `Pageable`.

## Configuration — `config/WebConfig.java`

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
        sortResolver.setSortParameter("sort");

        PageableHandlerMethodArgumentResolver pageableResolver =
            new PageableHandlerMethodArgumentResolver(sortResolver);
        pageableResolver.setMaxPageSize(100);
        pageableResolver.setFallbackPageable(PageRequest.of(0, 20, Sort.by("id").ascending()));

        resolvers.add(pageableResolver);
    }
}
```

## Controller

```java
@GetMapping
@Operation(summary = "List products with pagination and sorting")
public ResponseEntity<Page<ProductResponse>> list(
        @ParameterObject Pageable pageable) {
    return ResponseEntity.ok(productService.list(pageable));
}
```

**Usage examples**:
```
GET /api/v1/products                         → page 0, size 20, sort by id ASC
GET /api/v1/products?page=2&size=10          → page 2, size 10
GET /api/v1/products?sort=name,asc           → sorted by name ascending
GET /api/v1/products?sort=price,desc&sort=name,asc  → multi-sort
```

## Service

```java
@Override
@Transactional(readOnly = true)
public Page<ProductResponse> list(Pageable pageable) {
    return productRepository.findAll(pageable)
        .map(productMapper::toResponse);
}
```

## Restrict Sortable Fields (prevent SQL injection via sort parameter)

```java
@Component
public class PageableValidator {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "name", "sku", "price", "status", "createdAt", "updatedAt"
    );

    public Pageable validate(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                    "Invalid sort field: " + order.getProperty() +
                    ". Allowed: " + ALLOWED_SORT_FIELDS);
            }
        }
        return pageable;
    }
}
```

## Custom Page Response (optional — flattened structure)

```java
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
```

## Application Properties

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
        one-indexed-parameters: false  # page 0 = first page
```

## Rules

- Always set `max-page-size` — prevent clients requesting 10,000 rows
- Default page size should be 20 (sensible for most UIs)
- Validate sort fields against an allowlist — don't pass arbitrary column names to JPA
- Use `@ParameterObject` (springdoc) so sort/page params appear in Swagger UI
- Return `Page<T>` — it includes `totalElements`, `totalPages`, and navigation metadata
- For Oracle, paginated queries use `OFFSET/FETCH` (Hibernate handles this automatically)
