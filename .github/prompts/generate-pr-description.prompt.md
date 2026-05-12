---
description: "Use when: generating a pull request description from code changes — creates a structured PR description with summary, changes, testing, and checklist."
agent: "agent"
tools: [read, search]
argument-hint: "Describe the PR or point to the changed files — e.g., 'Generate PR description for the product CRUD feature'"
---

# Generate PR Description

Generate a **structured pull request description** from the current code changes.

## Instructions

1. Identify the changed/added files
2. Understand the purpose of the changes
3. Generate the PR description in the format below

## PR Template

```markdown
## Summary
<!-- One-paragraph description of what this PR does and why -->

## Type of Change
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] Breaking change (fix or feature that causes existing functionality to change)
- [ ] Refactoring (no functional changes)
- [ ] Documentation update
- [ ] Database migration

## Changes

### Added
- `ClassName.java` — description of what was added

### Modified
- `ClassName.java` — description of what changed

### Database
- `V<N>__description.sql` — schema change description

## API Changes
<!-- Include if REST endpoints were added or modified -->

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST   | `/api/v1/products` | Create a new product |

## Testing

### Unit Tests
- `ServiceTest` — X tests covering Y scenarios

### Integration Tests
- `ControllerTest` — X tests covering Y scenarios

### Manual Testing
<!-- Steps to manually verify if needed -->
1. Start the application
2. Send POST to `/api/v1/products` with: `{ ... }`
3. Verify 201 response with generated ID

## Checklist
- [ ] Code compiles without errors
- [ ] All new and existing tests pass
- [ ] No new SonarQube/Fortify findings
- [ ] Javadoc added for public methods
- [ ] API contract updated (if endpoints changed)
- [ ] No hardcoded secrets or credentials
```

## Rules

- Keep the summary concise — one paragraph
- List every changed file with a brief description
- If database migrations are included, call them out explicitly
- Include specific API endpoint table if REST changes are involved
- Don't include generated files (target/, .class) in the changes list
