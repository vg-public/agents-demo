---
description: "Use when: writing unit tests for a service class — generates comprehensive JUnit 5 + Mockito tests with edge cases, error paths, and parameterized tests."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Service class name — e.g., 'ProductServiceImpl' or paste the service code"
---

# Generate Service Unit Tests

Generate comprehensive **JUnit 5 + Mockito** unit tests for the specified Spring Boot service class.

## Instructions

1. **Read the service implementation** to understand all methods, dependencies, and business logic
2. **Read the related entity, DTOs, and mapper** to understand the data model
3. **Read any custom exceptions** thrown by the service

## Test Structure

```java
@ExtendWith(MockitoExtension.class)
class <Service>ImplTest {

    @Mock private <Repository> repository;
    @Mock private <Mapper> mapper;
    @InjectMocks private <Service>Impl service;

    // Test data — reusable fixtures
    private <Entity> sampleEntity;
    private <CreateRequest> createRequest;
    private <Response> expectedResponse;

    @BeforeEach
    void setUp() {
        // Initialize test fixtures with realistic data
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {
        @Test @DisplayName("should create successfully with valid request")
        @Test @DisplayName("should throw DuplicateResourceException when unique field exists")
    }

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {
        @Test @DisplayName("should return response when found")
        @Test @DisplayName("should throw ResourceNotFoundException when not found")
    }

    @Nested
    @DisplayName("list()")
    class ListTests {
        @Test @DisplayName("should return paginated results")
        @Test @DisplayName("should return empty page when no data")
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {
        @Test @DisplayName("should update all fields")
        @Test @DisplayName("should update partial fields (null ignored)")
        @Test @DisplayName("should throw ResourceNotFoundException when not found")
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {
        @Test @DisplayName("should delete when exists")
        @Test @DisplayName("should throw ResourceNotFoundException when not found")
    }
}
```

## Test Quality Rules

- Use `@Nested` classes to group tests by method
- Use `@DisplayName` with readable descriptions
- Follow **Arrange-Act-Assert** pattern with blank line separators
- Use **AssertJ** assertions: `assertThat(result).isEqualTo(expected)`
- Verify mock interactions: `verify(repository).save(any())` and `verify(repository, never()).delete(any())`
- Test **happy paths**, **error paths**, **edge cases**, and **boundary conditions**
- Use `@ParameterizedTest` with `@CsvSource` or `@MethodSource` for repetitive validation scenarios
- Test data should use realistic values, not `"test"` or `"foo"`
- Never use `@SpringBootTest` — this is a pure unit test with mocks
