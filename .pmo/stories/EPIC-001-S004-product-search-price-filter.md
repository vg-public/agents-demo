# Story: Product Search and Price Filter API

**Story ID**: EPIC-001-S004  
**Type**: Technical API Development

---

**As a** catalog consumer service,  
**I want** to search products by name keyword and filter by price range via a REST API,  
**So that** downstream clients can retrieve targeted product listings without fetching the entire catalog.

---

## Acceptance Criteria

1. `GET /api/v1/products/search` accepts query params: `keyword` (optional), `minPrice` (optional, must be > 0), `maxPrice` (optional, must be ≥ `minPrice`), `page` (default 0), `size` (default 20), `sort` (default `name,asc`).
2. Returns a paginated `Page<ProductResponse>` with HTTP 200.
3. Returns HTTP 400 with RFC 7807 ProblemDetail when `minPrice > maxPrice`.
4. Returns an empty page (not 404) when no products match.
5. `keyword` match is case-insensitive and partial (contains).

---

## Reference SQL

The following queries describe the data access patterns required:

```sql
-- Keyword search with price range filter
SELECT ID, NAME, PRICE, SKU, CREATED_AT, UPDATED_AT
FROM PRODUCTS
WHERE (UPPER(NAME) LIKE UPPER('%:keyword%') OR :keyword IS NULL)
  AND (PRICE >= :minPrice OR :minPrice IS NULL)
  AND (PRICE <= :maxPrice OR :maxPrice IS NULL)
ORDER BY NAME ASC;

-- Count query for pagination
SELECT COUNT(*)
FROM PRODUCTS
WHERE (UPPER(NAME) LIKE UPPER('%:keyword%') OR :keyword IS NULL)
  AND (PRICE >= :minPrice OR :minPrice IS NULL)
  AND (PRICE <= :maxPrice OR :maxPrice IS NULL);

-- Price boundary validation
SELECT MIN(PRICE), MAX(PRICE)
FROM PRODUCTS;
```

---

## Technical Notes

- Implement using `JpaSpecificationExecutor` with a dynamic `Specification<Product>` builder — do not use a hardcoded `@Query`.
- All PRODUCTS columns (`ID`, `NAME`, `PRICE`, `SKU`, `CREATED_AT`, `UPDATED_AT`) must be mapped.
- Use `PRODUCT_SEQ` (increment 50) for `@SequenceGenerator`.
- `minPrice` / `maxPrice` must be validated with `@DecimalMin("0.01")` on the request DTO.
- Audit fields (`CREATED_AT`, `UPDATED_AT`) are managed via `@PrePersist` / `@PreUpdate` — never set by the client.
