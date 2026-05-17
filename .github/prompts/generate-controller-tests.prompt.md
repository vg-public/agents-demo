---
description: "Use when: writing MockMvc controller integration tests — generates tests for all REST endpoints including validation errors, not-found cases, and pagination."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Controller class name — e.g., 'ProductController' or paste the controller code"
---

# Generate Controller Tests (MockMvc)

Generate comprehensive **MockMvc** integration tests for the specified Spring Boot REST controller.

## Instructions

1. **Read the controller** to understand all endpoints, request/response shapes, and validation
2. **Read the request DTOs** to understand validation rules
3. **Read the response DTOs** to know what to assert in responses

## Test Structure

```java
@WebMvcTest(<Controller>.class)
class <Controller>Test {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private <Service> service;

    @Nested
    @DisplayName("POST /api/v1/<resources>")
    class CreateTests {
        @Test @DisplayName("201 — should create with valid request")
        @Test @DisplayName("400 — should reject missing required fields")
        @Test @DisplayName("400 — should reject invalid field values")
        @Test @DisplayName("409 — should reject duplicate resource")
    }

    @Nested
    @DisplayName("GET /api/v1/<resources>/{id}")
    class GetByIdTests {
        @Test @DisplayName("200 — should return resource when found")
        @Test @DisplayName("404 — should return problem detail when not found")
    }

    @Nested
    @DisplayName("GET /api/v1/<resources>")
    class ListTests {
        @Test @DisplayName("200 — should return paginated list")
        @Test @DisplayName("200 — should return empty page when no data")
        @Test @DisplayName("200 — should respect page and size parameters")
    }

    @Nested
    @DisplayName("PUT /api/v1/<resources>/{id}")
    class UpdateTests {
        @Test @DisplayName("200 — should update with valid request")
        @Test @DisplayName("400 — should reject invalid update request")
        @Test @DisplayName("404 — should return problem detail when not found")
    }

    @Nested
    @DisplayName("DELETE /api/v1/<resources>/{id}")
    class DeleteTests {
        @Test @DisplayName("204 — should delete when exists")
        @Test @DisplayName("404 — should return problem detail when not found")
    }
}
```

## Test Pattern for Each Endpoint

```java
@Test
@DisplayName("201 — should create product with valid request")
void shouldCreateProduct() throws Exception {
    // Arrange
    var request = new CreateProductRequest("SKU-001", "Widget", new BigDecimal("29.99"));
    var response = new ProductResponse(1L, "SKU-001", "Widget", ...);
    given(service.create(any())).willReturn(response);

    // Act & Assert
    mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.sku").value("SKU-001"));
}
```

## Rules

- Use `@WebMvcTest` (not `@SpringBootTest`) — only load the web layer
- Use `@MockBean` for service dependencies
- Test every validation rule in the request DTOs
- Assert HTTP status codes, response body fields, and headers
- For error responses, assert RFC 7807 ProblemDetail shape: `$.type`, `$.title`, `$.status`, `$.detail`
- Use `given(...).willReturn(...)` (BDDMockito) for readability
- Use `given(...).willThrow(...)` for error path tests
- **Never use real PII** in test data — use synthetic values (e.g., `John Doe`, `test@example.com`, `555-0100`)
