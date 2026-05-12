---
description: "Use when: generating @DataJpaTest repository tests for Spring Data JPA repositories — tests derived queries, custom @Query methods, and entity constraints."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Repository name — e.g., 'ProductRepository' or 'OrderRepository'"
---

# Generate Repository Tests

Generate **@DataJpaTest unit tests** for a Spring Data JPA repository using JUnit 5, AssertJ, and an embedded database or Testcontainers.

## Instructions

1. Read the repository interface to find all custom methods
2. Read the entity to understand fields, constraints, and relationships

## Test Structure

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditConfig.class)  // if using auditing
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        savedProduct = entityManager.persistFlush(Product.builder()
            .name("Test Product")
            .sku("TEST-001")
            .price(new BigDecimal("99.99"))
            .status(ProductStatus.ACTIVE)
            .build());
    }

    @Nested
    @DisplayName("findBySku")
    class FindBySku {

        @Test
        @DisplayName("should return product when SKU exists")
        void shouldReturnProduct_whenSkuExists() {
            Optional<Product> result = productRepository.findBySku("TEST-001");

            assertThat(result)
                .isPresent()
                .get()
                .satisfies(p -> {
                    assertThat(p.getName()).isEqualTo("Test Product");
                    assertThat(p.getSku()).isEqualTo("TEST-001");
                });
        }

        @Test
        @DisplayName("should return empty when SKU does not exist")
        void shouldReturnEmpty_whenSkuNotFound() {
            Optional<Product> result = productRepository.findBySku("MISSING-SKU");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("should return paginated results for status")
        void shouldReturnPagedResults() {
            Page<Product> result = productRepository.findByStatus(
                ProductStatus.ACTIVE, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getSku()).isEqualTo("TEST-001");
        }
    }

    @Nested
    @DisplayName("Constraints")
    class Constraints {

        @Test
        @DisplayName("should enforce unique SKU constraint")
        void shouldRejectDuplicateSku() {
            Product duplicate = Product.builder()
                .name("Duplicate")
                .sku("TEST-001")  // same SKU
                .price(BigDecimal.TEN)
                .status(ProductStatus.ACTIVE)
                .build();

            assertThatThrownBy(() -> {
                entityManager.persistAndFlush(duplicate);
            }).isInstanceOf(PersistenceException.class);
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            Product invalid = Product.builder()
                .sku("TEST-002")
                .price(BigDecimal.TEN)
                .status(ProductStatus.ACTIVE)
                .build();

            assertThatThrownBy(() -> {
                entityManager.persistAndFlush(invalid);
            }).isInstanceOf(ConstraintViolationException.class);
        }
    }
}
```

## What to Test

| Category | Test Cases |
|----------|-----------|
| Derived queries | Each `findBy...` method with matching data, no matches, edge values |
| Custom `@Query` | Correct results, empty results, parameter binding |
| Pagination | Page content, totalElements, totalPages, empty pages |
| Sorting | Default sort order, custom sort columns |
| Constraints | Unique violations, NOT NULL violations, CHECK constraints |
| Relationships | Cascade persist, orphan removal, lazy loading |

## Rules

- Use `TestEntityManager` for setup — not the repository under test
- Each test should set up its own specific data (don't rely on shared state beyond `@BeforeEach`)
- Test both positive and negative cases for every query method
- Use `@Nested` classes grouped by method being tested
- Use descriptive `@DisplayName` on every test
- Clean assertions with AssertJ — no JUnit `assertEquals`
