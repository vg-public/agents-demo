---
description: "Use when: improving test coverage for untested or under-tested code — identifies gaps and generates missing unit, integration, and edge-case tests."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Class or package to improve coverage — e.g., 'OrderServiceImpl' or 'all service classes'"
---

# Improve Test Coverage

Analyze existing tests and generate **missing test cases** to achieve the **mandatory 80% line and branch coverage** for a Java Spring Boot class or package.

**Target**: Minimum 80% line and branch coverage (measured by JaCoCo, enforced by SonarQube).

## Instructions

1. Read the source class to identify all public methods
2. Read existing test class (if any) to identify what's already covered
3. Run `mvn test jacoco:report` to get current coverage baseline
3. Identify untested paths: branches, exceptions, edge cases, null inputs
4. Generate the missing tests

## Coverage Analysis

For each public method, check coverage of:

| Path Type | What to Test |
|-----------|-------------|
| Happy path | Normal input → expected output |
| Null/empty input | Null or empty parameters |
| Boundary values | Min/max values, zero, negative, empty collections |
| Exception paths | Every `throw` statement has a test |
| Branch coverage | Every `if/else`, `switch`, ternary |
| Edge cases | Concurrent modification, large datasets, special characters |

## Gap Detection Pattern

```
Source: ProductServiceImpl
├── create(CreateProductRequest)
│   ├── ✅ Happy path — testCreate_success
│   ├── ❌ MISSING — duplicate SKU throws DuplicateResourceException
│   ├── ❌ MISSING — null request handling
│   └── ❌ MISSING — name with max length (100 chars)
├── getById(Long)
│   ├── ✅ Happy path — testGetById_success
│   └── ✅ Not found — testGetById_notFound
├── update(Long, UpdateProductRequest)
│   ├── ✅ Happy path — testUpdate_success
│   ├── ❌ MISSING — partial update (only some fields)
│   ├── ❌ MISSING — update non-existent product
│   └── ❌ MISSING — SKU conflict on update
├── delete(Long)
│   ├── ✅ Happy path — testDelete_success
│   └── ❌ MISSING — delete non-existent product
└── list(Pageable)
    ├── ✅ Happy path — testList_success
    ├── ❌ MISSING — empty results
    └── ❌ MISSING — pagination beyond total pages
```

## Test Generation Rules

### Service Tests (Mockito)
```java
@Test
@DisplayName("should throw DuplicateResourceException when SKU already exists")
void shouldThrowWhenSkuDuplicate() {
    when(productRepository.existsBySku("EXISTING-SKU")).thenReturn(true);
    CreateProductRequest request = new CreateProductRequest("Name", "EXISTING-SKU", BigDecimal.TEN);

    assertThatThrownBy(() -> productService.create(request))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("EXISTING-SKU");

    verify(productRepository, never()).save(any());
}
```

### Controller Tests (MockMvc)
```java
@Test
@DisplayName("should return 400 when name is blank")
void shouldReturn400WhenNameBlank() throws Exception {
    String json = """
        {"name": "", "sku": "TEST-001", "price": 10.00}
        """;

    mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.name").exists());
}
```

## Output Format

1. **Coverage Analysis Table** — list each method with covered/missing paths
2. **Generated Tests** — complete test methods for each missing path
3. **Coverage Summary** — estimated before/after coverage percentage

## Rules

- Don't rewrite existing passing tests — only add missing ones
- Use `@Nested` to group tests by method
- Every test must have a `@DisplayName` that reads like a sentence
- Each test verifies ONE behavior — no multi-assertion tests
- Use AssertJ for all assertions — no JUnit `assertEquals`
- Mock only direct dependencies — never mock the class under test
- Prefer `verify()` for void methods and side-effect assertions
