# Story: Product Bulk Price Update API

**Story ID**: EPIC-001-S005
**Type**: Technical API Development
**Epic**: EPIC-001 — Product Catalog Management

---

**As a** catalog administrator service,
**I want** to update prices for multiple products in a single API call by providing a list of SKU + new price pairs,
**So that** pricing changes can be applied atomically without issuing one request per product.

---

## Acceptance Criteria

1. `PATCH /api/v1/products/prices` accepts a JSON body containing a list of `{ sku, newPrice }` entries (minimum 1, maximum 100 items).
2. Returns HTTP 200 with a `BulkPriceUpdateResponse` containing:
   - `updatedCount` — number of products successfully updated
   - `notFoundSkus` — list of SKUs that did not match any product
   - `invalidSkus` — list of SKUs whose `newPrice` failed validation (e.g. ≤ 0)
3. All valid updates are applied in a **single transaction** — if any database error occurs the entire batch is rolled back.
4. Returns HTTP 400 with RFC 7807 ProblemDetail when the request list is empty or exceeds 100 items.
5. `newPrice` for each entry must be `> 0` (validated with `@DecimalMin("0.01")`); entries failing this are collected in `invalidSkus` and skipped — they do NOT cause HTTP 400.
6. SKUs not found in the database are collected in `notFoundSkus` and skipped — they do NOT cause HTTP 404.
7. `updatedAt` is refreshed on every updated row (via `@PreUpdate`).

---

## Reference SQL

The following queries describe the data access patterns required:

```sql
-- Fetch existing products by a batch of SKUs
SELECT ID, NAME, PRICE, SKU, CREATED_AT, UPDATED_AT
FROM PRODUCTS
WHERE SKU IN (:skuList);

-- Bulk price update (one statement per row — Hibernate batch update)
UPDATE PRODUCTS
SET PRICE      = :newPrice,
    UPDATED_AT = :now
WHERE SKU = :sku
  AND :newPrice > 0;

-- Identify SKUs not found (anti-join pattern for notFoundSkus)
SELECT :requestedSku
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM PRODUCTS WHERE SKU = :requestedSku
);
```

---

## Request / Response Contract

### Request — `PATCH /api/v1/products/prices`

```json
{
  "updates": [
    { "sku": "WDG-001", "newPrice": 19.99 },
    { "sku": "GDG-001", "newPrice": 129.00 },
    { "sku": "UNKNOWN-SKU", "newPrice": 49.99 },
    { "sku": "BAD-PRICE",  "newPrice": -5.00 }
  ]
}
```

### Response — HTTP 200

```json
{
  "updatedCount": 2,
  "notFoundSkus": ["UNKNOWN-SKU"],
  "invalidSkus":  ["BAD-PRICE"]
}
```

### Error — HTTP 400 (empty or oversized list)

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "updates list must have between 1 and 100 entries",
  "errorCode": "VALIDATION_FAILED"
}
```

---

## Technical Notes

- Implement `BulkPriceUpdateRequest` as a record with `@NotNull @Size(min=1, max=100) List<PriceUpdateEntry> updates`.
- `PriceUpdateEntry` is a record with `@NotBlank String sku` and `@NotNull BigDecimal newPrice`.
  - **Do NOT** annotate `newPrice` with `@DecimalMin` at the DTO level — invalid prices must be collected in `invalidSkus`, not trigger HTTP 400.
  - Validate `newPrice > 0` inside the service and route failures to `invalidSkus`.
- Fetch all matching products in **one query** using `ProductRepository.findAllBySkuIn(List<String> skus)` (derived query method — no `@Query` needed).
- Use `PRODUCT_SEQ` (allocationSize = 50) for sequence — no changes to PK generation.
- The `updatedAt` field is managed by `@PreUpdate` on the entity — never set it manually in the service.
- Service method signature:
  ```java
  @Transactional
  BulkPriceUpdateResponse bulkUpdatePrices(BulkPriceUpdateRequest request);
  ```
- Add `BulkPriceUpdateRequest`, `PriceUpdateEntry`, and `BulkPriceUpdateResponse` under `dto/request/` and `dto/response/` respectively.
- No new exception class needed — use existing `GlobalExceptionHandler` to handle `@Valid` failures on the request body.
- Audit fields (`createdAt`, `updatedAt`) are managed via `@PrePersist` / `@PreUpdate` — never set by the service.

---

## Out of Scope

- No partial-rollback support — it is all-or-nothing for the valid subset.
- No price history or audit log table (separate story).
- No authentication / authorization checks (handled by security layer, not this story).
