---
description: "Use when: implementing a user story end-to-end — reads acceptance criteria and generates all code layers from entity to controller with tests."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Paste the user story or point to the story file — e.g., 'Implement STORY-101: As a user, I want to filter products by category'"
---

# Implement User Story

Implement a **user story** end-to-end across all layers of a Spring Boot REST API.

## Instructions

1. Read the story's acceptance criteria carefully
2. Identify what needs to be created or modified at each layer
3. Implement bottom-up: Entity → Repository → Service → DTOs → Mapper → Controller → Tests

## Implementation Workflow

### Step 1: Analyze the Story

Extract from the acceptance criteria:
- **What entities** are involved (new or existing)?
- **What endpoints** are needed (HTTP method + URL)?
- **What business rules** must be enforced?
- **What validations** apply to inputs?
- **What error cases** must be handled?

### Step 2: Entity Layer
- Create or modify `@Entity` classes
- Add fields with proper JPA annotations
- Set up relationships if needed

### Step 3: Repository Layer
- Add derived query methods or `@Query` for new lookups
- Extend `JpaSpecificationExecutor` if filtering needed

### Step 4: Service Layer
- Define method in service interface
- Implement in `ServiceImpl` with business rules
- Add `@Transactional` annotations
- Throw appropriate custom exceptions

### Step 5: DTO Layer
- Create request records with Jakarta validation
- Create response records with all needed fields
- Update MapStruct mapper

### Step 6: Controller Layer
- Add endpoint with proper HTTP method and path
- Use `@Valid` on request bodies
- Return `ResponseEntity<T>` with correct status code

### Step 7: Tests
- **Service test**: Mockito — happy path + each business rule + each error case
- **Controller test**: MockMvc — happy path + validation errors + not found
- **Repository test**: `@DataJpaTest` — custom queries only

## Acceptance Criteria → Test Mapping

```
Story: As a user, I want to filter products by category and price range
  GIVEN products exist in the catalog
  WHEN I search with category "Electronics" and maxPrice 500
  THEN I should see only matching products with pagination

Maps to tests:
├── ServiceTest
│   ├── shouldReturnProductsFilteredByCategoryAndPrice
│   ├── shouldReturnEmptyPageWhenNoMatch
│   └── shouldReturnAllWhenNoFiltersProvided
├── ControllerTest
│   ├── shouldReturn200WithFilteredResults
│   ├── shouldReturn200WithEmptyPageWhenNoMatch
│   └── shouldReturn400WhenMaxPriceNegative
└── RepositoryTest
    └── shouldFilterBySpecification
```

## Definition of Done Checklist

- [ ] All acceptance criteria implemented
- [ ] Entity, repository, service, DTOs, mapper, controller updated
- [ ] Validation on all request inputs
- [ ] Custom exceptions for business rule violations
- [ ] Service tests passing (Mockito)
- [ ] Controller tests passing (MockMvc)
- [ ] No SonarQube findings introduced
- [ ] Javadoc on all public methods
- [ ] Code compiles and all tests pass

## Rules

- Implement exactly what the story asks — no gold-plating
- If the story is ambiguous, call out assumptions explicitly
- Every acceptance criterion must have at least one test
- Follow existing code patterns in the project — don't introduce new conventions
- If the story requires changes to existing endpoints, verify backward compatibility
