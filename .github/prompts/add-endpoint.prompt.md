---
description: "Use when: adding a new REST API endpoint to an existing controller — creates the endpoint, service method, and any supporting DTOs or repository queries."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Describe the endpoint — e.g., 'GET /api/v1/products/search?keyword=... to search products by name', 'PATCH /api/v1/orders/{id}/status to update order status'"
---

# Add New Endpoint

Add a **new REST API endpoint** to an existing Spring Boot controller.

## Instructions

1. Read the existing controller, service, repository, and DTOs
2. Determine what new code is needed (may not need all layers)

## Checklist

### Controller
- Add the new `@GetMapping` / `@PostMapping` / `@PutMapping` / `@PatchMapping` / `@DeleteMapping`
- Return `ResponseEntity<T>` with appropriate HTTP status
- Add `@Operation` and `@ApiResponse` for OpenAPI docs
- Use `@Valid` on request bodies

### Service
- Add method to the service interface first
- Then add implementation in `ServiceImpl`
- Use `@Transactional` for writes, `@Transactional(readOnly = true)` for reads

### Repository
- Add derived query method or `@Query` if needed for the new operation

### DTOs
- Create new request/response records if the existing ones don't fit
- Never reuse a create/update DTO for a different purpose

### Tests
- Add MockMvc test for the new endpoint
- Add service unit test for the new method

## Common Endpoint Patterns

**Custom search:**
```java
@GetMapping("/search")
public ResponseEntity<Page<ProductResponse>> search(
        @RequestParam String keyword, Pageable pageable) { ... }
```

**Status update (PATCH):**
```java
@PatchMapping("/{id}/status")
public ResponseEntity<ProductResponse> updateStatus(
        @PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) { ... }
```

**Bulk operation:**
```java
@PostMapping("/bulk")
public ResponseEntity<List<ProductResponse>> createBulk(
        @Valid @RequestBody List<CreateProductRequest> requests) { ... }
```

**Count/aggregate:**
```java
@GetMapping("/count")
public ResponseEntity<Map<String, Long>> countByStatus() { ... }
```

**Existence check:**
```java
@GetMapping("/exists")
public ResponseEntity<Map<String, Boolean>> exists(@RequestParam String sku) { ... }
```
