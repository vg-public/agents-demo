---
name: java-api-development
description: "Guide for building RESTful CRUD APIs with Java 17+ and Spring Boot 3.2+, Spring Data JPA, Oracle Database, including controllers, services, repositories, DTOs, MapStruct mappers, exception handling, validation, pagination, and auditing. Use when creating or modifying Java backend endpoints."
---

# Java Spring Boot API Development — Spring Data JPA + Oracle DB

This skill guides the AI to build **production-quality REST CRUD APIs** using Java 17+, Spring Boot 3.2+, Spring Data JPA (Hibernate 6), and Oracle Database.

## When to Use

- Creating new REST API CRUD endpoints
- Adding service-layer business logic with transactional boundaries
- Implementing JPA entities mapped to Oracle tables with sequences
- Building Spring Data JPA repositories with derived queries, JPQL, and Specifications
- Setting up request validation, global exception handling, and RFC 7807 error responses
- Creating DTOs with Java records and MapStruct mappers
- Configuring pagination, sorting, auditing, and connection pooling

## Approach

1. **Read the API contract or story**: Check the user story for endpoint specifications.
2. **Check existing code**: Search for existing controllers, services, entities, and patterns to follow established conventions.
3. **Implement layer by layer** (bottom-up):
   - **Entity** → **Repository** → **Service Interface** → **Service Impl** → **DTO** → **Mapper** → **Controller** → **Exception Handler**
4. **Add validation and error handling** at each layer.
5. **Run `mvn compile`** to verify no compilation errors.

---

## Project Structure

```
src/main/java/com/<group>/<artifact>/
├── config/                      # @Configuration: CORS, OpenAPI, audit, Jackson
├── controller/                  # @RestController: thin REST endpoints
├── dto/
│   ├── request/                 # Immutable request records with Bean Validation
│   └── response/                # Immutable response records
├── entity/                      # @Entity: JPA entities mapped to Oracle tables
├── exception/                   # Custom exceptions + @RestControllerAdvice
├── mapper/                      # @Mapper: MapStruct interfaces
├── repository/                  # JpaRepository interfaces
├── service/                     # Service interfaces
│   └── impl/                   # @Service implementations
└── util/                        # Utility classes

src/main/resources/
├── application.yml              # Spring Boot configuration
├── application-dev.yml          # Dev profile (Oracle local/container)
└── application-prod.yml         # Production profile
```

---

## Entity Layer — JPA + Oracle

### Conventions
- Use `@Entity` + `@Table(name = "TABLE_NAME")` with explicit uppercase Oracle table names
- Map Oracle sequences with `@SequenceGenerator` + `@GeneratedValue(strategy = SEQUENCE)`
- Include audit fields using `@MappedSuperclass` base entity
- Use `@Column(name = "COL_NAME")` with explicit Oracle column names
- Map relationships with `@ManyToOne`, `@OneToMany`, `@ManyToMany` with appropriate `FetchType`
- Default to `FetchType.LAZY` for all associations
- Use `@PrePersist` and `@PreUpdate` for audit timestamps

### Base Auditable Entity Example

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @Column(name = "ID")
    private Long id;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "CREATED_BY", length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "VERSION")
    private Long version;
}
```

### Entity Example — Oracle Sequence Mapping

```java
@Entity
@Table(name = "PRODUCTS", indexes = {
    @Index(name = "IDX_PRODUCTS_CATEGORY", columnList = "CATEGORY_ID"),
    @Index(name = "IDX_PRODUCTS_SKU", columnList = "SKU", unique = true),
    @Index(name = "IDX_PRODUCTS_STATUS", columnList = "STATUS")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "PRODUCT_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SKU", nullable = false, length = 50, unique = true)
    private String sku;

    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "PRICE", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID", nullable = false,
                foreignKey = @ForeignKey(name = "FK_PRODUCTS_CATEGORY"))
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    // --- Business helper methods ---

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }
}
```

### Enum Example

```java
public enum ProductStatus {
    DRAFT,
    ACTIVE,
    DISCONTINUED,
    OUT_OF_STOCK
}
```

---

## Repository Layer — Spring Data JPA

### Conventions
- Extend `JpaRepository<Entity, Long>` for full CRUD + pagination
- Use derived query methods for simple lookups (Spring auto-generates SQL)
- Use `@Query` with JPQL for complex queries — never concatenate user input
- Use `Pageable` for all list endpoints
- Use `Optional<T>` for single-result lookups
- Use `@EntityGraph` to solve N+1 queries
- Use `JpaSpecificationExecutor<T>` for dynamic filtering

### Repository Example

```java
public interface ProductRepository extends JpaRepository<Product, Long>,
                                           JpaSpecificationExecutor<Product> {

    // --- Derived query methods ---
    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByStatus(ProductStatus status);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // --- JPQL queries ---
    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByStatusAndPriceRange(
            @Param("status") ProductStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    // --- JPQL with JOIN FETCH to avoid N+1 ---
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "images"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithCategoryAndImages(@Param("id") Long id);

    // --- Custom update (bulk) ---
    @Modifying
    @Query("UPDATE Product p SET p.status = :status WHERE p.category.id = :categoryId")
    int updateStatusByCategoryId(@Param("status") ProductStatus status,
                                  @Param("categoryId") Long categoryId);

    // --- Projection query ---
    @Query("SELECT p.id AS id, p.name AS name, p.price AS price FROM Product p WHERE p.status = 'ACTIVE'")
    List<ProductSummaryProjection> findActiveProductSummaries();

    // --- Count query ---
    long countByStatus(ProductStatus status);

    // --- Delete ---
    @Modifying
    void deleteByStatusAndUpdatedAtBefore(ProductStatus status, LocalDateTime cutoff);
}
```

### Projection Interface Example

```java
public interface ProductSummaryProjection {
    Long getId();
    String getName();
    BigDecimal getPrice();
}
```

### JPA Specification Example (Dynamic Filtering)

```java
public class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("price"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.lessThanOrEqualTo(root.get("price"), max);
        };
    }

    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> keyword == null ? null
                : cb.like(cb.upper(root.get("name")), "%" + keyword.toUpperCase() + "%");
    }
}
```

---

## Service Layer

### Conventions
- Define a **service interface** for each domain concept
- Create `impl/` package with `@Service` implementations
- Use **constructor injection** exclusively — no `@Autowired` on fields
- Mark read operations with `@Transactional(readOnly = true)` — enables Hibernate flush-mode optimization
- Mark write operations with `@Transactional`
- Never expose JPA entities outside the service — always return DTOs via MapStruct
- Throw custom business exceptions — never generic `RuntimeException`
- Log business operations at INFO level, debug details at DEBUG level

### Service Interface Example

```java
public interface ProductService {

    ProductResponse getById(Long id);

    ProductResponse getBySku(String sku);

    Page<ProductResponse> getAll(Pageable pageable);

    Page<ProductResponse> search(ProductSearchRequest searchRequest, Pageable pageable);

    ProductResponse create(CreateProductRequest request);

    ProductResponse update(Long id, UpdateProductRequest request);

    void delete(Long id);

    long countByStatus(ProductStatus status);
}
```

### Service Implementation Example

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        log.debug("Fetching product by id: {}", id);
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getBySku(String sku) {
        log.debug("Fetching product by SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        log.debug("Fetching all products, page: {}", pageable);
        return productRepository.findAll(pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(ProductSearchRequest searchRequest, Pageable pageable) {
        log.debug("Searching products with criteria: {}", searchRequest);
        Specification<Product> spec = Specification.where(
                ProductSpecifications.hasStatus(searchRequest.status()))
                .and(ProductSpecifications.hasCategoryId(searchRequest.categoryId()))
                .and(ProductSpecifications.priceBetween(searchRequest.minPrice(), searchRequest.maxPrice()))
                .and(ProductSpecifications.nameContains(searchRequest.keyword()));
        return productRepository.findAll(spec, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        log.info("Creating product with SKU: {}", request.sku());

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product", "sku", request.sku());
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setStatus(ProductStatus.DRAFT);

        Product saved = productRepository.save(product);
        log.info("Created product with id: {}", saved.getId());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        log.info("Updating product id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        productMapper.updateEntity(request, product);

        if (request.categoryId() != null && !request.categoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));
            product.setCategory(category);
        }

        Product updated = productRepository.save(product);
        log.info("Updated product id: {}", updated.getId());
        return productMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting product id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        productRepository.delete(product);
        log.info("Deleted product id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(ProductStatus status) {
        return productRepository.countByStatus(status);
    }
}
```

---

## DTO Layer — Java Records with Bean Validation

### Conventions
- Use Java `record` types for immutable DTOs
- Separate `CreateXxxRequest`, `UpdateXxxRequest`, and `XxxResponse` records
- Apply Jakarta Bean Validation annotations on request records
- Use `@Valid` on nested objects
- Use custom validators for complex business rules

### Request DTO Examples

```java
public record CreateProductRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 50, message = "SKU must not exceed 50 characters")
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU must contain only uppercase letters, digits, and hyphens")
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
        String name,

        @Size(max = 4000, message = "Description must not exceed 4000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @DecimalMax(value = "9999999999.99", message = "Price must not exceed 9999999999.99")
        @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
        BigDecimal price,

        @NotNull(message = "Category ID is required")
        @Positive(message = "Category ID must be positive")
        Long categoryId
) {}
```

```java
public record UpdateProductRequest(
        @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
        String name,

        @Size(max = 4000, message = "Description must not exceed 4000 characters")
        String description,

        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        ProductStatus status,

        @Positive(message = "Category ID must be positive")
        Long categoryId
) {}
```

### Response DTO Example

```java
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        ProductStatus status,
        String categoryName,
        Long categoryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy
) {}
```

### Search/Filter Request DTO

```java
public record ProductSearchRequest(
        String keyword,
        ProductStatus status,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {}
```

### Paginated Response Wrapper

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
    public static <T> PagedResponse<T> from(Page<T> springPage) {
        return new PagedResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isFirst(),
                springPage.isLast()
        );
    }
}
```

---

## Mapper Layer — MapStruct

### Conventions
- Use `@Mapper(componentModel = "spring")` for Spring DI integration
- Define `toResponse`, `toEntity`, `updateEntity` methods
- Use `@Mapping` for field name mismatches
- Use `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` for partial updates

### MapStruct Mapper Example

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryId", source = "category.id")
    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Product toEntity(CreateProductRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(UpdateProductRequest request, @MappingTarget Product product);
}
```

---

## Controller Layer

### Conventions
- Annotate with `@RestController` and `@RequestMapping("/api/v1/<resource>")`
- Use `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`
- Return `ResponseEntity<T>` for explicit HTTP status control
- Use `@Valid` on `@RequestBody` parameters
- Use `@PathVariable` for identifiers, `@RequestParam` for filters/pagination
- Keep controllers thin — no business logic, only delegation
- Add `@Tag` annotation for Swagger grouping

### Full CRUD Controller Example

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product CRUD operations")
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ProductResponse> getBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getBySku(sku));
    }

    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<PagedResponse<ProductResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponse> results = productService.getAll(pageable);
        return ResponseEntity.ok(PagedResponse.from(results));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products with filters")
    public ResponseEntity<PagedResponse<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchRequest searchRequest = new ProductSearchRequest(
                keyword, status, categoryId, minPrice, maxPrice);
        Pageable pageable = PageRequest.of(page, size);

        Page<ProductResponse> results = productService.search(searchRequest, pageable);
        return ResponseEntity.ok(PagedResponse.from(results));
    }

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Exception Handling — RFC 7807 ProblemDetail

### Custom Exception Hierarchy

```java
public abstract class BusinessException extends RuntimeException {
    private final String errorCode;

    protected BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

```java
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: %s", resource, field, value), "RESOURCE_NOT_FOUND");
    }
}
```

```java
public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String resource, String field, Object value) {
        super(String.format("%s already exists with %s: %s", resource, field, value), "DUPLICATE_RESOURCE");
    }
}
```

```java
public class BusinessRuleViolationException extends BusinessException {
    public BusinessRuleViolationException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Resource");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Business Rule Violation");
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setProperty("errorCode", "VALIDATION_ERROR");
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Constraint Violation");
        problem.setProperty("errorCode", "CONSTRAINT_VIOLATION");
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> violations = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(v -> violations.put(v.getPropertyPath().toString(), v.getMessage()));
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "A data integrity constraint was violated");
        problem.setTitle("Data Integrity Violation");
        problem.setProperty("errorCode", "DATA_INTEGRITY_VIOLATION");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The resource was modified by another user. Please refresh and retry.");
        problem.setTitle("Concurrent Modification");
        problem.setProperty("errorCode", "OPTIMISTIC_LOCK_FAILURE");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setProperty("errorCode", "INTERNAL_ERROR");
        problem.setProperty("timestamp", Instant.now());
        // Never expose stack traces or internal details
        return problem;
    }
}
```

---

## Configuration

### application.yml — Oracle + JPA

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//localhost:1521/XEPDB1
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 600000
      connection-timeout: 30000
      pool-name: AppHikariPool
      connection-test-query: SELECT 1 FROM DUAL

  jpa:
    database-platform: org.hibernate.dialect.OracleDialect
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        default_schema: ${DB_SCHEMA:APP_SCHEMA}
        format_sql: true
        generate_statistics: false
        jdbc:
          batch_size: 50
          order_inserts: true
          order_updates: true
        query:
          in_clause_parameter_padding: true
          fail_on_pagination_over_collection_fetch: true
          plan_cache_max_size: 2048

server:
  port: 8080

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    root: INFO
    com.example: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### JPA Auditing Configuration

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .or(() -> Optional.of("SYSTEM"));
    }
}
```

### OpenAPI Configuration

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product Catalog API")
                        .version("1.0.0")
                        .description("Spring Boot REST API with Oracle Database")
                        .contact(new Contact().name("API Team").email("api-team@company.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

---

## Oracle DDL — Reference

### Naming Convention
- Use uppercase Oracle identifiers
- Include constraints, indexes, sequences, and comments
- Save scripts to `work/sql/` directory

### DDL Script Example

```sql
-- create_categories_and_products.sql
-- Create core product catalog tables

-- Category table
CREATE TABLE CATEGORIES (
    ID            NUMBER(19)    NOT NULL,
    NAME          VARCHAR2(100) NOT NULL,
    DESCRIPTION   VARCHAR2(500),
    PARENT_ID     NUMBER(19),
    CREATED_AT    TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT    TIMESTAMP(6) WITH TIME ZONE,
    CREATED_BY    VARCHAR2(100),
    UPDATED_BY    VARCHAR2(100),
    VERSION       NUMBER(19) DEFAULT 0 NOT NULL,
    CONSTRAINT PK_CATEGORIES PRIMARY KEY (ID),
    CONSTRAINT FK_CATEGORIES_PARENT FOREIGN KEY (PARENT_ID) REFERENCES CATEGORIES(ID),
    CONSTRAINT UQ_CATEGORIES_NAME UNIQUE (NAME)
);

CREATE SEQUENCE CATEGORY_SEQ START WITH 1 INCREMENT BY 50 NOCACHE;

COMMENT ON TABLE CATEGORIES IS 'Product categories with hierarchical support';
COMMENT ON COLUMN CATEGORIES.PARENT_ID IS 'Self-referencing FK for subcategories';

-- Product table
CREATE TABLE PRODUCTS (
    ID            NUMBER(19)      NOT NULL,
    SKU           VARCHAR2(50)    NOT NULL,
    NAME          VARCHAR2(255)   NOT NULL,
    DESCRIPTION   VARCHAR2(4000),
    PRICE         NUMBER(12,2)    NOT NULL,
    STATUS        VARCHAR2(20)    DEFAULT 'DRAFT' NOT NULL,
    CATEGORY_ID   NUMBER(19)      NOT NULL,
    CREATED_AT    TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT    TIMESTAMP(6) WITH TIME ZONE,
    CREATED_BY    VARCHAR2(100),
    UPDATED_BY    VARCHAR2(100),
    VERSION       NUMBER(19) DEFAULT 0 NOT NULL,
    CONSTRAINT PK_PRODUCTS PRIMARY KEY (ID),
    CONSTRAINT FK_PRODUCTS_CATEGORY FOREIGN KEY (CATEGORY_ID) REFERENCES CATEGORIES(ID),
    CONSTRAINT UQ_PRODUCTS_SKU UNIQUE (SKU),
    CONSTRAINT CHK_PRODUCTS_PRICE CHECK (PRICE > 0),
    CONSTRAINT CHK_PRODUCTS_STATUS CHECK (STATUS IN ('DRAFT','ACTIVE','DISCONTINUED','OUT_OF_STOCK'))
);

CREATE SEQUENCE PRODUCT_SEQ START WITH 1 INCREMENT BY 50 NOCACHE;

CREATE INDEX IDX_PRODUCTS_CATEGORY ON PRODUCTS(CATEGORY_ID);
CREATE INDEX IDX_PRODUCTS_STATUS ON PRODUCTS(STATUS);
CREATE INDEX IDX_PRODUCTS_NAME ON PRODUCTS(UPPER(NAME));

COMMENT ON TABLE PRODUCTS IS 'Product catalog master table';
COMMENT ON COLUMN PRODUCTS.SKU IS 'Stock Keeping Unit — unique product identifier';
COMMENT ON COLUMN PRODUCTS.STATUS IS 'DRAFT, ACTIVE, DISCONTINUED, OUT_OF_STOCK';
```

---

## Security Checklist

- Validate and sanitize all input — never trust client data
- Use parameterized queries (JPA handles this) — never concatenate user input into SQL
- Return 403/401 for unauthorized access — do not leak whether a resource exists
- Do not log sensitive data (passwords, tokens, PII)
- Apply `@PreAuthorize` or method-level security for role-based access control
- Use `@JsonIgnore` on entity fields that should never be serialized
- Set `spring.jpa.open-in-view=false` to prevent lazy-loading in controllers
- Enable CSRF protection for browser-based clients
- Configure CORS explicitly — never use `allowedOrigins("*")` in production

## Anti-Patterns to Avoid

- Do NOT put business logic in controllers — keep them thin
- Do NOT return JPA entities directly from controllers (lazy-loading, circular refs, data leakage)
- Do NOT catch and swallow exceptions silently — log and rethrow or handle appropriately
- Do NOT use `@Autowired` on fields — prefer constructor injection
- Do NOT create overly generic endpoints — be explicit about the contract
- Do NOT use `FetchType.EAGER` on collections — use `JOIN FETCH` or `@EntityGraph` when needed
- Do NOT use `spring.jpa.hibernate.ddl-auto=update` in production — use `validate` with externally managed schema
- Do NOT return `Optional` from controller methods — unwrap in the service layer
- Do NOT mix business logic and data access in the same class
