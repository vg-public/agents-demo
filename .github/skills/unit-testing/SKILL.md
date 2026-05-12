---
name: unit-testing
description: "Guide for writing unit tests and integration tests for Java Spring Boot applications using JUnit 5, Mockito, AssertJ, MockMvc, and @DataJpaTest. Use when writing or improving test coverage for controllers, services, repositories, and mappers."
---

# Unit & Integration Testing — Java Spring Boot

This skill guides the AI to write effective, maintainable tests for Java Spring Boot REST API applications using JUnit 5, Mockito, AssertJ, MockMvc, and Spring test slices.

## When to Use

- Writing tests for new features or bug fixes
- Improving test coverage for existing Java code
- Writing unit tests for services, mappers, and utility classes
- Writing MockMvc integration tests for controllers
- Writing `@DataJpaTest` tests for custom repository queries
- Setting up testing infrastructure for a Spring Boot project
- Fixing or refactoring failing tests

## General Principles

1. **Test behavior, not implementation**: Assert on outputs and side effects, not internal state.
2. **One assertion per concept**: Each test should verify one logical behavior.
3. **Arrange-Act-Assert (AAA)**: Structure every test in three clear phases.
4. **Test names describe behavior**: Use `shouldReturnProductWhenIdExists` or `should_throw_when_sku_duplicate`.
5. **Keep tests independent**: No test should depend on another test's execution.
6. **Avoid over-mocking**: Mock external dependencies (repositories, HTTP clients), not the code under test.
7. **Test edge cases**: Null inputs, empty collections, boundary values, error conditions.
8. **Use `@Nested` classes**: Group related test cases for readability.

---

## Test Structure

```
src/test/java/com/<group>/<artifact>/
├── controller/
│   └── ProductControllerTest.java       # MockMvc integration tests
├── service/
│   └── ProductServiceImplTest.java      # Unit tests with Mockito
├── repository/
│   └── ProductRepositoryTest.java       # @DataJpaTest tests
├── mapper/
│   └── ProductMapperTest.java           # MapStruct mapper tests
├── exception/
│   └── GlobalExceptionHandlerTest.java  # Exception handler tests
└── integration/
    └── ProductIntegrationTest.java      # Full Spring Boot integration tests
```

---

## Service Layer Tests (JUnit 5 + Mockito)

### Conventions
- Use `@ExtendWith(MockitoExtension.class)` for mock injection
- Use `@Mock` for dependencies, `@InjectMocks` for the class under test
- Use AssertJ `assertThat()` for fluent assertions
- Use `@Nested` classes to group related test cases
- Use `@DisplayName` for human-readable test names
- Never use `@SpringBootTest` for pure service unit tests

### Full Service Test Example

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    // --- Test data factory ---
    private Product createProduct(Long id, String sku, String name, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setPrice(price);
        product.setStatus(ProductStatus.ACTIVE);
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        product.setCategory(category);
        return product;
    }

    private ProductResponse createProductResponse(Long id, String name, BigDecimal price) {
        return new ProductResponse(id, "SKU-001", name, "Description", price,
                ProductStatus.ACTIVE, "Electronics", 1L,
                LocalDateTime.now(), LocalDateTime.now(), "system", "system");
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return product when ID exists")
        void shouldReturnProductWhenIdExists() {
            // Arrange
            Product product = createProduct(1L, "SKU-001", "Widget", new BigDecimal("29.99"));
            ProductResponse expected = createProductResponse(1L, "Widget", new BigDecimal("29.99"));

            when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
            when(productMapper.toResponse(product)).thenReturn(expected);

            // Act
            ProductResponse result = productService.getById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Widget");
            assertThat(result.price()).isEqualByComparingTo("29.99");

            verify(productRepository).findByIdWithCategory(1L);
            verify(productMapper).toResponse(product);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when ID does not exist")
        void shouldThrowNotFoundWhenIdDoesNotExist() {
            // Arrange
            when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product")
                    .hasMessageContaining("99");

            verify(productRepository).findByIdWithCategory(99L);
            verifyNoInteractions(productMapper);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create product successfully")
        void shouldCreateProductSuccessfully() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest(
                    "SKU-NEW", "New Product", "Description",
                    new BigDecimal("49.99"), 1L);

            Category category = new Category();
            category.setId(1L);
            category.setName("Electronics");

            Product entity = createProduct(null, "SKU-NEW", "New Product", new BigDecimal("49.99"));
            Product savedEntity = createProduct(1L, "SKU-NEW", "New Product", new BigDecimal("49.99"));
            ProductResponse expectedResponse = createProductResponse(1L, "New Product", new BigDecimal("49.99"));

            when(productRepository.existsBySku("SKU-NEW")).thenReturn(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(productMapper.toEntity(request)).thenReturn(entity);
            when(productRepository.save(entity)).thenReturn(savedEntity);
            when(productMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

            // Act
            ProductResponse result = productService.create(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("New Product");

            verify(productRepository).existsBySku("SKU-NEW");
            verify(categoryRepository).findById(1L);
            verify(productRepository).save(entity);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when SKU exists")
        void shouldThrowDuplicateWhenSkuExists() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest(
                    "SKU-EXISTS", "Duplicate", "Desc", new BigDecimal("10.00"), 1L);
            when(productRepository.existsBySku("SKU-EXISTS")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> productService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("SKU-EXISTS");

            verify(productRepository).existsBySku("SKU-EXISTS");
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when category does not exist")
        void shouldThrowNotFoundWhenCategoryMissing() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest(
                    "SKU-NEW", "Product", "Desc", new BigDecimal("10.00"), 999L);
            when(productRepository.existsBySku("SKU-NEW")).thenReturn(false);
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category")
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("should return paginated products")
        void shouldReturnPaginatedProducts() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20);
            Product product = createProduct(1L, "SKU-001", "Widget", new BigDecimal("29.99"));
            Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);
            ProductResponse response = createProductResponse(1L, "Widget", new BigDecimal("29.99"));

            when(productRepository.findAll(pageable)).thenReturn(productPage);
            when(productMapper.toResponse(product)).thenReturn(response);

            // Act
            Page<ProductResponse> result = productService.getAll(pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Widget");
        }

        @Test
        @DisplayName("should return empty page when no products exist")
        void shouldReturnEmptyPageWhenNoProducts() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20);
            when(productRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

            // Act
            Page<ProductResponse> result = productService.getAll(pageable);

            // Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete product successfully")
        void shouldDeleteProductSuccessfully() {
            // Arrange
            Product product = createProduct(1L, "SKU-001", "Widget", new BigDecimal("29.99"));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // Act
            productService.delete(1L);

            // Assert
            verify(productRepository).delete(product);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when deleting non-existent product")
        void shouldThrowNotFoundWhenDeletingNonExistent() {
            // Arrange
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(productRepository, never()).delete(any());
        }
    }
}
```

### Parameterized Test Example

```java
@ParameterizedTest
@CsvSource({
    "ACTIVE, 5",
    "DRAFT, 3",
    "DISCONTINUED, 0"
})
@DisplayName("should return correct count for each status")
void shouldReturnCountByStatus(ProductStatus status, long expectedCount) {
    when(productRepository.countByStatus(status)).thenReturn(expectedCount);

    long result = productService.countByStatus(status);

    assertThat(result).isEqualTo(expectedCount);
}
```

---

## Controller Tests (MockMvc)

### Conventions
- Use `@WebMvcTest(ProductController.class)` for slice testing
- Use `@MockBean` for service layer dependencies
- Use `MockMvc` with `perform()`, `andExpect()`, `andDo()` chain
- Test request validation, status codes, response body, and error handling
- Use `ObjectMapper` to serialize request bodies

### Full Controller Test Example

```java
@WebMvcTest(ProductController.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetById {

        @Test
        @DisplayName("should return 200 with product when found")
        void shouldReturn200WhenFound() throws Exception {
            ProductResponse response = new ProductResponse(
                    1L, "SKU-001", "Widget", "A widget", new BigDecimal("29.99"),
                    ProductStatus.ACTIVE, "Electronics", 1L,
                    LocalDateTime.now(), LocalDateTime.now(), "system", "system");

            when(productService.getById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/products/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.sku").value("SKU-001"))
                    .andExpect(jsonPath("$.name").value("Widget"))
                    .andExpect(jsonPath("$.price").value(29.99))
                    .andExpect(jsonPath("$.categoryName").value("Electronics"));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(productService.getById(99L))
                    .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

            mockMvc.perform(get("/api/v1/products/99")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"))
                    .andExpect(jsonPath("$.detail").value("Product not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProduct {

        @Test
        @DisplayName("should return 201 with created product")
        void shouldReturn201WhenCreated() throws Exception {
            CreateProductRequest request = new CreateProductRequest(
                    "SKU-NEW", "New Widget", "Description",
                    new BigDecimal("49.99"), 1L);
            ProductResponse response = new ProductResponse(
                    1L, "SKU-NEW", "New Widget", "Description", new BigDecimal("49.99"),
                    ProductStatus.DRAFT, "Electronics", 1L,
                    LocalDateTime.now(), null, "system", null);

            when(productService.create(any(CreateProductRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.sku").value("SKU-NEW"))
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 400 when SKU is blank")
        void shouldReturn400WhenSkuBlank() throws Exception {
            CreateProductRequest request = new CreateProductRequest(
                    "", "Widget", "Desc", new BigDecimal("10.00"), 1L);

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.sku").exists());
        }

        @Test
        @DisplayName("should return 400 when price is negative")
        void shouldReturn400WhenPriceNegative() throws Exception {
            CreateProductRequest request = new CreateProductRequest(
                    "SKU-001", "Widget", "Desc", new BigDecimal("-5.00"), 1L);

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.price").exists());
        }

        @Test
        @DisplayName("should return 400 when required fields are null")
        void shouldReturn400WhenFieldsNull() throws Exception {
            String emptyRequest = "{}";

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isMap());
        }

        @Test
        @DisplayName("should return 409 when SKU already exists")
        void shouldReturn409WhenSkuDuplicate() throws Exception {
            CreateProductRequest request = new CreateProductRequest(
                    "SKU-EXISTS", "Widget", "Desc", new BigDecimal("10.00"), 1L);

            when(productService.create(any()))
                    .thenThrow(new DuplicateResourceException("Product", "sku", "SKU-EXISTS"));

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Duplicate Resource"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("should return 204 when deleted successfully")
        void shouldReturn204WhenDeleted() throws Exception {
            doNothing().when(productService).delete(1L);

            mockMvc.perform(delete("/api/v1/products/1"))
                    .andExpect(status().isNoContent());

            verify(productService).delete(1L);
        }
    }
}
```

---

## Repository Tests (@DataJpaTest)

### Conventions
- Use `@DataJpaTest` — loads only JPA components (no web layer, no service beans)
- Use `@AutoConfigureTestDatabase(replace = NONE)` with Testcontainers for Oracle, or `replace = ANY` for H2
- Use `TestEntityManager` for fixture setup
- Test custom `@Query` methods, derived queries, and Specifications

### Repository Test Example

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@DisplayName("ProductRepository Tests")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category savedCategory;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Electronics");
        savedCategory = entityManager.persistAndFlush(category);
    }

    private Product createAndSaveProduct(String sku, String name, BigDecimal price, ProductStatus status) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(price);
        product.setStatus(status);
        product.setCategory(savedCategory);
        return entityManager.persistAndFlush(product);
    }

    @Test
    @DisplayName("should find product by SKU")
    void shouldFindBySku() {
        createAndSaveProduct("SKU-001", "Widget", new BigDecimal("29.99"), ProductStatus.ACTIVE);

        Optional<Product> result = productRepository.findBySku("SKU-001");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Widget");
    }

    @Test
    @DisplayName("should return empty when SKU does not exist")
    void shouldReturnEmptyWhenSkuNotFound() {
        Optional<Product> result = productRepository.findBySku("NONEXISTENT");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should find products by status with pagination")
    void shouldFindByStatusPaginated() {
        createAndSaveProduct("SKU-001", "Widget 1", new BigDecimal("10.00"), ProductStatus.ACTIVE);
        createAndSaveProduct("SKU-002", "Widget 2", new BigDecimal("20.00"), ProductStatus.ACTIVE);
        createAndSaveProduct("SKU-003", "Widget 3", new BigDecimal("30.00"), ProductStatus.DRAFT);

        Page<Product> result = productRepository.findByCategoryId(
                savedCategory.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("should check SKU existence")
    void shouldCheckSkuExistence() {
        createAndSaveProduct("SKU-001", "Widget", new BigDecimal("10.00"), ProductStatus.ACTIVE);

        assertThat(productRepository.existsBySku("SKU-001")).isTrue();
        assertThat(productRepository.existsBySku("SKU-999")).isFalse();
    }

    @Test
    @DisplayName("should count by status")
    void shouldCountByStatus() {
        createAndSaveProduct("SKU-001", "Widget 1", new BigDecimal("10.00"), ProductStatus.ACTIVE);
        createAndSaveProduct("SKU-002", "Widget 2", new BigDecimal("20.00"), ProductStatus.ACTIVE);
        createAndSaveProduct("SKU-003", "Widget 3", new BigDecimal("30.00"), ProductStatus.DRAFT);

        assertThat(productRepository.countByStatus(ProductStatus.ACTIVE)).isEqualTo(2);
        assertThat(productRepository.countByStatus(ProductStatus.DRAFT)).isEqualTo(1);
        assertThat(productRepository.countByStatus(ProductStatus.DISCONTINUED)).isZero();
    }
}
```

---

## Mapper Tests

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductMapper Tests")
class ProductMapperTest {

    private final ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

    @Test
    @DisplayName("should map entity to response DTO")
    void shouldMapEntityToResponse() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        Product product = new Product();
        product.setId(1L);
        product.setSku("SKU-001");
        product.setName("Widget");
        product.setPrice(new BigDecimal("29.99"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        ProductResponse result = mapper.toResponse(product);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sku()).isEqualTo("SKU-001");
        assertThat(result.name()).isEqualTo("Widget");
        assertThat(result.categoryName()).isEqualTo("Electronics");
        assertThat(result.categoryId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should map create request to entity")
    void shouldMapCreateRequestToEntity() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-001", "Widget", "A widget", new BigDecimal("29.99"), 1L);

        Product result = mapper.toEntity(request);

        assertThat(result.getSku()).isEqualTo("SKU-001");
        assertThat(result.getName()).isEqualTo("Widget");
        assertThat(result.getPrice()).isEqualByComparingTo("29.99");
        assertThat(result.getId()).isNull();           // ID should not be set
        assertThat(result.getCategory()).isNull();      // Category set in service
    }

    @Test
    @DisplayName("should update entity with non-null fields only")
    void shouldPartialUpdateEntity() {
        Product existing = new Product();
        existing.setName("Old Name");
        existing.setPrice(new BigDecimal("10.00"));
        existing.setDescription("Old description");

        UpdateProductRequest request = new UpdateProductRequest(
                "New Name", null, new BigDecimal("25.00"), null, null);

        mapper.updateEntity(request, existing);

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getPrice()).isEqualByComparingTo("25.00");
        assertThat(existing.getDescription()).isEqualTo("Old description");  // Unchanged
    }
}
```

---

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ProductServiceImplTest

# Run specific test method
mvn test -Dtest="ProductServiceImplTest#shouldReturnProductWhenIdExists"

# Run tests with coverage report (JaCoCo)
mvn test jacoco:report

# Run only unit tests (skip integration)
mvn test -Dgroups="unit"

# Run only integration tests
mvn test -Dgroups="integration"
```

## What to Test (Priority)

| Layer | What to Test | Framework |
|-------|-------------|-----------|
| Service | Business logic, exception paths, edge cases | JUnit 5 + Mockito |
| Controller | Request validation, status codes, response body, error handling | MockMvc |
| Repository | Custom `@Query` methods, derived queries, Specifications | `@DataJpaTest` |
| Mapper | Entity ↔ DTO mapping, null handling, partial updates | JUnit 5 |
| Exception Handler | Each exception type maps to correct HTTP status and error body | MockMvc |

## Anti-Patterns to Avoid

- Do NOT use `@SpringBootTest` for unit tests — it loads the entire application context
- Do NOT test getters/setters or framework behavior
- Do NOT write tests that depend on execution order
- Do NOT mock the class under test — mock its dependencies
- Do NOT ignore flaky tests — fix them or understand why they flake
- Do NOT use `Thread.sleep()` in tests — use `Awaitility` for async assertions
- Do NOT hardcode timestamps — use `Clock` injection or fixed `LocalDateTime` values
