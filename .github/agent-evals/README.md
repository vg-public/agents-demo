# Agent Evaluation Framework

Manual evaluation framework for verifying Copilot agent quality across all custom agents in this workspace.

## Purpose

Agents evolve over time — new constraints, guardrails, and workflows are added. This framework provides a structured way to verify that agents behave correctly after changes.

## When to Run Evaluations

- After modifying any `.agent.md` file
- After updating `copilot-instructions.md` (affects all agents)
- After adding or changing skills or prompts referenced by agents
- Before merging agent-related PRs

## Evaluation Process

1. Open [eval-checklist.md](eval-checklist.md)
2. For each agent, run the listed test scenarios in VS Code Copilot Chat
3. Mark each scenario as PASS / FAIL / PARTIAL
4. Document failures with the actual vs expected output
5. Fix agent definitions and re-test until all scenarios pass

## Evaluation Criteria

Each test scenario is evaluated against these dimensions:

| Dimension | What to Check |
|-----------|---------------|
| **Correctness** | Does the agent produce valid, compilable code? |
| **Constraint adherence** | Does the agent follow its Constraints section? |
| **PII guardrail** | Does the agent use synthetic data (never real PII)? |
| **Scope discipline** | Does the agent stay within its responsibilities? |
| **Workflow completeness** | Does the agent execute all required steps? |
| **Error handling** | Does the agent handle edge cases gracefully? |
| **Idempotency** | Does re-running produce the same result without duplication? |

## Coverage Matrix

| Agent | # Scenarios | Last Evaluated | Status |
|-------|-------------|----------------|--------|
| `@dev-orchestrator` | 4 | — | Not yet |
| `@java-api-dev` | 3 | — | Not yet |
| `@new-api-scaffold` | 2 | — | Not yet |
| `@api-modification` | 2 | — | Not yet |
| `@sql-data` | 2 | — | Not yet |
| `@testing` | 3 | — | Not yet |
| `@bug-fix` | 2 | — | Not yet |
| `@code-review` | 3 | — | Not yet |
| `@fortify-fix` | 3 | — | Not yet |
| `@mend-fix` | 3 | — | Not yet |
| `@perf-optimizer` | 2 | — | Not yet |
| `@doc-gen` | 2 | — | Not yet |
