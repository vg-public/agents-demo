# Story: Product SKU Lookup and Soft Archive API

**Story ID**: EPIC-001-S006
**Type**: Technical API Development
**Epic**: EPIC-001 — Product Catalog Management

---

**As a** catalog consumer service,
**I want** to look up a single product by its SKU and to archive (soft-delete) a product by SKU,
**So that** downstream services can resolve products without knowing their internal IDs, and catalog administrators can retire products without permanently destroying data.

---

## Acceptance Criteria

1. `GET /api/v1/products/sku/{sku}` returns the product matching the given SKU with HTTP 200.
2. Returns HTTP 404 with RFC 7807 ProblemDetail when no product matches the SKU.
3. `PATCH /api/v1/products/sku/{sku}/archive` marks the product as archived by setting `ARCHIVED = TRUE` and refreshes `UPDATED_AT`.
4. Archiving an already-archived product returns HTTP 409 with RFC 7807 ProblemDetail (error code `ALREADY_ARCHIVED`).
5. Archiving a non-existent SKU returns HTTP 404.
6. Archived products are **excluded** from `GET /api/v1/products` (list all) and `GET /api/v1/products/search` responses.
7. `GET /api/v1/products/{id}` and `GET /api/v1/products/sku/{sku}` still return archived products (explicit lookup by identifier is allowed).

---

## Reference SQL

```sql
-- Lookup by SKU
SELECT ID, NAME, PRICE, SKU, ARCHIVED, CREATED_AT, UPDATED_AT
FROM PRODUCTS
WHERE SKU = :sku;

-- Archive a product
UPDATE PRODUCTS
SET    ARCHIVED   = 1,
       UPDATED_AT = :now
WHERE  SKU = :sku
  AND  ARCHIVED   = 0;

-- List / search — exclude archived
SELECT ID, NAME, PRICE, SKU, ARCHIVED, CREATED_AT, UPDATED_AT
FROM PRODUCTS
WHERE ARCHIVED = 0
  AND (UPPER(NAME) LIKE UPPER('%:keyword%') OR :keyword IS NULL)
  AND (PRICE >= :minPrice OR :minPrice IS NULL)
  AND (PRICE <= :maxPrice OR :maxPrice IS NULL)
ORDER BY NAME ASC;

-- Check if product is already archived
SELECT ARCHIVED
FROM PRODUCTS
WHERE SKU = :sku;
```

---

## DDL Change Required

Add the `ARCHIVED` column to the existing `PRODUCTS` table and update the DDL reference file:

```sql
ALTER TABLE PRODUCTS
    ADD ARCHIVED NUMBER(1) DEFAULT 0 NOT NULL
    CONSTRAINT CK_PRODUCTS_ARCHIVED CHECK (ARCHIVED IN (0, 1));

COMMENT ON COLUMN PRODUCTS.ARCHIVED IS '0 = active, 1 = archived (soft-deleted)';

-- Index for efficient filtering of active products
CREATE INDEX IDX_PRODUCTS_ARCHIVED ON PRODUCTS (ARCHIVED);
```

---

## Request / Response Contract

### GET /api/v1/products/sku/{sku} — HTTP 200
```json
{
  "id": 1,
  "sku": "WDG-001",
  "name": "Widget",
  "price": 19.99,
  "archived": false,
  "createdAt": "2025-01-01T10:00:00",
  "updatedAt": "2025-01-01T10:00:00"
}
```

### GET /api/v1/products/sku/{sku} — HTTP 404
```json
{
  "type": "about:blank",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Product with sku 'XYZ-999' was not found",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

### PATCH /api/v1/products/sku/{sku}/archive — HTTP 200
```json
{
  "id": 1,
  "sku": "WDG-001",
  "name": "Widget",
  "price": 19.99,
  "archived": true,
  "createdAt": "2025-01-01T10:00:00",
  "updatedAt": "2026-05-23T10:00:00"
}
```

### PATCH /api/v1/products/sku/{sku}/archive — HTTP 409 (already archived)
```json
{
  "type": "about:blank",
  "title": "Already Archived",
  "status": 409,
  "detail": "Product with sku 'WDG-001' is already archived",
  "errorCode": "ALREADY_ARCHIVED"
}
```

---

## Technical Notes

- **DDL change**: Add `ARCHIVED NUMBER(1) DEFAULT 0 NOT NULL` to `PRODUCTS` table; update `src/main/resources/db/oracle-ddl.sql`.
- **Entity change**: Add `boolean archived` field to `Product` entity mapped to column `ARCHIVED`; default `false`.
- **DTO change**: Add `boolean archived` field to `ProductResponse` record.
- **New exception**: `AlreadyArchivedException extends BusinessException` — HTTP 409, error code `ALREADY_ARCHIVED`.
- **Repository methods needed**:
  - `Optional<Product> findBySku(String sku)` — already exists, no change needed.
  - `List<Product> findAllByArchivedFalse(Pageable pageable)` — for filtered list.
  - The existing `JpaSpecificationExecutor` search should add an `archived = false` predicate via `ProductSpecification.isActive()`.
- **Service methods**:
  - `ProductResponse getBySku(String sku)` — `@Transactional(readOnly = true)`
  - `ProductResponse archive(String sku)` — `@Transactional`; throw `ResourceNotFoundException` if not found, `AlreadyArchivedException` if already archived.
- **Filter existing list/search**: Add `AND archived = false` predicate to `getAll()` and `search()` — achieved by composing `Specification.where(ProductSpecification.isActive())` into both queries.
- **`@PreUpdate`** handles `updatedAt` on archive — do not set manually.
- `GET /api/v1/products/{id}` and `GET /api/v1/products/sku/{sku}` do NOT filter on archived — explicit ID/SKU lookup must always resolve.

---

## Out of Scope

- No hard-delete endpoint (separate story).
- No un-archive / restore endpoint (separate story).
- No cascade archiving of related entities (no relations in current schema).
