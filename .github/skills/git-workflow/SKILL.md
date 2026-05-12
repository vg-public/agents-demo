---
name: git-workflow
description: "Guide for Git branching strategies, commit conventions, pull request workflows, merge conflict resolution, and common Git operations. Use when working with Git in daily development."
---

# Git Workflow

This skill guides the AI through standard Git workflows used in team-based development.

## When to Use

- Creating feature branches for new work
- Writing commit messages
- Resolving merge conflicts
- Preparing and managing pull requests
- Recovering from Git mistakes (accidental commits, wrong branch)
- Rebasing, cherry-picking, or squashing commits

## Branching Strategy

### Branch Naming Convention

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/<ticket>-<short-desc>` | `feature/EPIC-001-S003-product-search` |
| Bug fix | `bugfix/<ticket>-<short-desc>` | `bugfix/BUG-042-cart-total-rounding` |
| Hotfix | `hotfix/<short-desc>` | `hotfix/fix-payment-timeout` |
| Release | `release/<version>` | `release/2.1.0` |
| Chore | `chore/<short-desc>` | `chore/update-dependencies` |

### Branch Flow

```
main ─── develop ─── feature/EPIC-001-S003-product-search
              │
              └───── bugfix/BUG-042-cart-total
```

- `main`: Production-ready code. Protected branch.
- `develop`: Integration branch for features. Protected branch.
- Feature/bugfix branches: Created from `develop`, merged back via PR.

## Commit Message Convention (Conventional Commits)

```
<type>(<scope>): <short description>

<optional body: explain WHAT and WHY, not HOW>

<optional footer: Fixes #123, BREAKING CHANGE: ...>
```

### Types

| Type | When to Use |
|------|-------------|
| `feat` | New feature for the user |
| `fix` | Bug fix |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test` | Adding or fixing tests |
| `docs` | Documentation changes |
| `style` | Code style changes (formatting, semicolons) |
| `chore` | Build, CI, or tooling changes |
| `perf` | Performance improvement |

### Examples

```
feat(cart): add quantity selector to shopping cart

Allows users to change item quantity directly from the cart page.
Quantity is validated to be between 1 and 99.

Fixes #234
```

```
fix(auth): prevent token refresh loop on 401 response

Root cause: interceptor was retrying token refresh indefinitely
when the refresh endpoint itself returned 401.
Fix: add a flag to skip refresh retry for the refresh request.

Fixes #567
```

## Pull Request Workflow

### Creating a PR

1. Push your branch: `git push -u origin feature/EPIC-001-S003-product-search`
2. Create PR targeting `develop` (or your team's integration branch).
3. PR title: Follow the same commit convention as the squash commit message.
4. PR description template:
   ```markdown
   ## What
   Brief description of the change.

   ## Why
   Link to ticket/story. Context for why this change is needed.

   ## How
   High-level description of the approach.

   ## Testing
   - [ ] Unit tests added
   - [ ] Manual testing done
   - [ ] All existing tests pass

   ## Screenshots (if UI change)
   ```

### Before Requesting Review

- [ ] Code compiles and lints without errors
- [ ] All tests pass
- [ ] Branch is up-to-date with target branch
- [ ] Self-reviewed the diff for accidental changes
- [ ] No console.log / debug statements left in code
- [ ] No hardcoded secrets or credentials

## Merge Conflict Resolution

1. Update your branch: `git fetch origin && git rebase origin/develop`
2. When conflicts appear, open the conflicting files.
3. For each conflict:
   - Understand both sides: what your branch changed and what the other branch changed.
   - Choose the correct resolution — do not blindly accept one side.
   - If both changes are needed, merge them manually.
4. After resolving: `git add <resolved-files> && git rebase --continue`
5. Test that the resolved code works correctly.

## Common Git Recovery Operations

| Situation | Command |
|-----------|---------|
| Undo last commit (keep changes) | `git reset --soft HEAD~1` |
| Discard all local changes | `git checkout -- .` |
| Committed on wrong branch | `git stash && git checkout correct-branch && git stash pop` |
| Remove a file from staging | `git reset HEAD <file>` |
| See what changed in a file | `git diff <file>` |
| See commit history for a file | `git log --oneline <file>` |
| Find who changed a line | `git blame <file>` |
| Find which commit introduced a bug | `git bisect start && git bisect bad && git bisect good <known-good-commit>` |

## Anti-Patterns to Avoid

- Do NOT commit directly to `main` or `develop`.
- Do NOT use `git push --force` on shared branches without team agreement.
- Do NOT create giant PRs — keep changes focused and reviewable (<400 lines).
- Do NOT leave merge conflict markers in code (`<<<<<<<`, `=======`, `>>>>>>>`).
- Do NOT commit generated files, build artifacts, or `target/` directories.
