---
description: "Use when: triaging a batch of Fortify SAST findings — paste multiple findings to get a prioritized remediation plan before fixing."
agent: "fortify-fix"
tools: [read, search]
argument-hint: "Paste multiple Fortify findings (table, CSV, or list) — e.g., 'Triage these 12 Fortify findings from the latest scan'"
---

# Triage Fortify Report

Analyze a **batch of Fortify SAST findings** and produce a prioritized remediation plan.

## Instructions

1. Parse all findings: extract Category, CWE, Severity, File, Line for each
2. Group findings by file (minimizes context switching)
3. Sort by severity: **Critical → High → Medium → Low**
4. For each finding, assess: **True Positive** or **False Positive**
5. Produce a prioritized remediation plan

## Triage Criteria

Mark as **False Positive** if:
- Input does not come from user-controlled source
- Sanitization/validation exists between source and sink that Fortify missed
- Code is in test files, dead code, or build artifacts
- Data is from trusted internal source (config, enum, constant)

Mark as **True Positive** if:
- User-controlled input reaches dangerous sink without sanitization
- No validation exists between tainted source and sink
- The CWE pattern is clearly present in the code

## Output Format

```markdown
## Fortify Triage Summary

**Total Findings**: X
**True Positives**: Y (to fix)
**False Positives**: Z (to suppress)

### Remediation Priority

| # | Category | CWE | Severity | File:Line | Verdict | Estimated Effort |
|---|----------|-----|----------|-----------|---------|-----------------|
| 1 | SQL Injection | 89 | Critical | OrderRepo.java:45 | TP | Low (parameterize query) |
| 2 | ... | ... | ... | ... | ... | ... |

### False Positives (Suppress with Justification)

| # | Category | File:Line | Reason |
|---|----------|-----------|--------|
| 1 | Insecure Randomness | TestUtil.java:12 | Test-only code, not security context |

### Recommended Fix Order
1. [Critical TPs first — highest risk]
2. [High TPs — fix before release]
3. [Medium TPs — plan for next sprint]
```
