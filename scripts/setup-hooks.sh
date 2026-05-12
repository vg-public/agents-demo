#!/usr/bin/env bash
# =============================================================================
# setup-hooks.sh — Register .githooks as the Git hooks directory (Mac / Linux)
#
# Usage (run once per clone):
#   chmod +x scripts/setup-hooks.sh && ./scripts/setup-hooks.sh
#
# Hooks activated:
#   pre-commit  — branch protection, secret detection, System.out check, Spotless format
#   commit-msg  — enforce Conventional Commits or Jira ticket format
#   pre-push    — compile + unit test gate before push
# =============================================================================

set -euo pipefail

HOOKS_DIR=".githooks"

echo "Configuring Git to use '$HOOKS_DIR' as the hooks directory..."

git config core.hooksPath "$HOOKS_DIR"

# Make all hook scripts executable
for HOOK in pre-commit commit-msg pre-push; do
    HOOK_FILE="$HOOKS_DIR/$HOOK"
    if [ -f "$HOOK_FILE" ]; then
        chmod +x "$HOOK_FILE"
        echo "Made '$HOOK_FILE' executable."
    else
        echo "WARNING: Expected hook not found: $HOOK_FILE"
    fi
done

echo ""
echo "Done. Git hooks are now active:"
echo "  pre-commit  -> $HOOKS_DIR/pre-commit  (branch guard | secret scan | System.out check | Spotless)"
echo "  commit-msg  -> $HOOKS_DIR/commit-msg  (Conventional Commits / Jira ticket format)"
echo "  pre-push    -> $HOOKS_DIR/pre-push    (mvn compile + mvn test)"
echo ""
echo "Tip: to bypass pre-push in an emergency: SKIP_PRE_PUSH=1 git push"
