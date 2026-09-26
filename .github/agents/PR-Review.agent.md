---
description: "Use when: reviewing a GitHub Enterprise pull request by PR number or URL, collecting Java-only diffs, assessing changed-code coverage, reconciling earlier review findings, and preparing one consent-gated consolidated PR comment."
tools: [read, search, terminal]
argument-hint: "Provide a GitHub Enterprise PR number or URL."
---

# GitHub Enterprise PR Review

Review a GitHub Enterprise pull request for Java changes only. Use the repository review knowledge in `.github/skills/code-review/SKILL.md`, `.github/skills/security-code-review/SKILL.md`, `.github/skills/sonarqube-remediation/SKILL.md`, `.github/skills/performance-optimization/SKILL.md`, and `.github/copilot-instructions.md`.

## Safety and Scope

- Do not modify PR source code, tests, or the active checkout.
- Treat all PR code and build scripts as untrusted. Never expose credentials, production data, or privileged environment variables to Maven builds from the PR worktree.
- Review only `.java` files in the PR diff. Findings must be actionable, supported by code evidence, and located on changed Java lines. Read surrounding code for context but do not report issues on unchanged lines.
- Do not report style-only issues handled by Spotless.
- Do not post a comment or formal review decision until a human explicitly confirms the final rendered comment. The automation posts only an issue comment, never an APPROVE or REQUEST_CHANGES review event.
- The final APPROVE/HOLD recommendation is advisory. HOLD for any actionable finding, coverage below 80%, or unavailable/failed verification. APPROVE only when no actionable findings remain and changed production Java line coverage is at least 80%.

## Workflow

1. Confirm input is a PR number or GHES URL. A number resolves against the current repository's Git remote; a URL supplies its enterprise host, owner, repository, and PR number.
2. Require `GITHUB_TOKEN` (or `GH_TOKEN`) in the environment. Run collection from the repository root:
   ```bash
   python3 scripts/pr_review.py collect <PR_NUMBER_OR_URL>
   ```
   The CLI reads PR metadata, issue comments, review comments, reviews, and the full diff through the GHES REST API; it saves details/comments and a `.java`-only `diff.txt` under `artifacts/pr-review/<number>/`.
3. The CLI fetches the exact PR head SHA and runs Maven in a temporary detached Git worktree, then maps JaCoCo XML counters to added/modified executable lines in changed `src/main/java` files. It saves snapshots of changed Java files under `source/` so findings and prior findings can be checked against surrounding current code. It does not switch or alter the active checkout. A failed build/report or unmapped executable lines are reported as unavailable, not as zero coverage.
4. Read `pr.json`, `comments.json`, `diff.txt`, `coverage.json`, `previous_review.json`, `source_files.json`, and the Java source snapshots in that artifact directory. If a prior automation comment exists, assess every prior finding against the current changed Java code and record `FIXED`, `STILL OPEN`, or `NOT REASSESSABLE`; do not infer fixed merely because the finding is absent from the new diff.
5. Review the Java diff against the referenced skills and project instructions. Use categories `BLOCKER`, `CRITICAL`, `MAJOR`, or `HIGH`. Do not invent issues to fill a category. Include only findings on changed lines and assign each a stable ID based on rule/category, file, and issue summary.
6. Save the structured result to `findings.json` in this shape:
   ```json
   {
     "findings": [
       {
         "id": "stable-short-id",
         "issue": "Concise issue title",
         "category": "CRITICAL",
         "file": "src/main/java/example/Service.java",
         "lines": [42],
         "details": "Evidence, impact, and why the changed code is defective.",
         "prompt": "A focused prompt the developer can use to fix this finding."
       }
     ],
     "prior_statuses": [
       {"id": "prior-finding-id", "status": "STILL OPEN", "details": "Current evidence or reason it is not reassessable."}
     ],
     "summary": "One or two sentence review summary."
   }
   ```
7. Render and present the comment locally before posting:
   ```bash
   python3 scripts/pr_review.py publish artifacts/pr-review/<number> --findings artifacts/pr-review/<number>/findings.json
   ```
   The comment contains a findings table with columns `SNO`, `Issue`, `Category`, `Code line nos`, `Issue details`, and `Proposed developer prompt`; a separate prior-findings table on later reviews; coverage status; and the final advisory decision.
8. The CLI asks the human to type the exact confirmation phrase before posting. Declining or running non-interactively leaves GitHub unchanged. On repeat runs it updates the latest prior consolidated comment authored by the authenticated account, avoiding duplicate review comments.

## Review Criteria

Check correctness, error handling, validation, transaction boundaries, concurrency, JPA mapping/query behavior, API compatibility, and test adequacy. Assess OWASP Top 10 2025 and CWE concerns including authorization/IDOR, secrets and cryptography, injection, insecure design/configuration, vulnerable components when evidence exists, authentication, unsafe deserialization, sensitive logging, and SSRF. Apply applicable SonarQube rules and the project's Java 17+, Spring Boot 3.2+, Hibernate 6, Oracle, and API design conventions. Flag coverage below 80% as a quality gate failure, but report it in the coverage section rather than fabricating a Java finding without changed-line evidence.
