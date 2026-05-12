---
name: story-implementation
description: "Guide for implementing user stories end-to-end in Java Spring Boot, from reading acceptance criteria to writing code, tests, and creating a pull request. Use when picking up a story from the backlog."
---

# Story Implementation Workflow — Java Spring Boot

This skill guides the AI through the full lifecycle of implementing a user story for a Spring Boot REST API with Oracle Database.

## When to Use

- Starting work on a new user story from the backlog
- Implementing a feature described by acceptance criteria
- Breaking a story into implementation tasks
- Ensuring all acceptance criteria are met before marking done

## Workflow

### 1. Understand the Story

- Read the story file in `work/pmo/EPIC-XXX/stories.md` or the linked issue/ticket.
- Identify:
  - **User persona**: Who is this for?
  - **Action**: What does the user want to do?
  - **Benefit**: Why do they want to do it?
  - **Acceptance criteria**: What conditions must be true for the story to be done?
- Check for related SQL schemas in `work/sql/`.

### 2. Plan the Implementation

Break the story into small, ordered implementation tasks:

1. **Entity** — JPA entity with Oracle sequence mapping and audit fields
2. **Repository** — Spring Data JPA interface with derived/JPQL queries
3. **Service interface** — Define the contract
4. **Service implementation** — Business logic with `@Transactional`
5. **Request/Response DTOs** — Java records with Bean Validation
6. **MapStruct mapper** — Entity ↔ DTO conversion
7. **Controller** — REST endpoints with `@Valid`, `ResponseEntity`, pagination
8. **Exception handling** — Custom exceptions + handler (if not present)
9. **Unit tests** — Service tests with Mockito
10. **Controller tests** — MockMvc integration tests
11. **Repository tests** — `@DataJpaTest` for custom queries

Use the todo list tool to track these tasks.

### 3. Create a Feature Branch

- Branch naming: `feature/EPIC-XXX-SYYY-short-description`
- Example: `feature/EPIC-001-S003-add-product-search`

### 4. Implement (Bottom-Up)

1. **Entity** — Create JPA entity extending `BaseEntity` with Oracle sequence
2. **Repository** — Extend `JpaRepository` with custom queries
3. **Service** — Create interface + implementation with `@Transactional` boundaries
4. **DTOs** — Create request/response records with validation annotations
5. **Mapper** — Create MapStruct interface with `@Mapper(componentModel = "spring")`
6. **Controller** — Create `@RestController` with CRUD endpoints

### 5. Verify Acceptance Criteria

- Go through each acceptance criterion one by one.
- For each criterion, confirm:
  - The code implements the behavior described.
  - A test exists that validates the criterion.
  - Edge cases from the criterion are handled.

### 6. Testing Checklist

- [ ] Service unit tests (Mockito) — happy path + error cases
- [ ] Controller tests (MockMvc) — request validation, status codes, response body
- [ ] Repository tests (@DataJpaTest) — custom @Query methods
- [ ] Mapper tests — entity ↔ DTO mapping correctness
- [ ] Edge case tests (empty inputs, boundary values, not-found, duplicate)
- [ ] All existing tests still pass (`mvn test`)

### 7. Definition of Done

- [ ] All acceptance criteria met
- [ ] Code compiles without errors (`mvn compile`)
- [ ] Tests written and passing (`mvn test`)
- [ ] No hardcoded secrets or credentials
- [ ] Javadoc on public API methods
- [ ] Code reviewed (or ready for review)

## Tips

- Implement the simplest version first (happy path), then layer on validation and edge cases.
- Commit after each logical unit of work — do not create one giant commit.
- If a story is too large, break it into sub-tasks and track progress.
- Cross-reference other stories in the same epic to avoid conflicts.
- Run `mvn compile` frequently to catch errors early.
