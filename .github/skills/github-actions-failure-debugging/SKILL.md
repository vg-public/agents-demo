---
name: github-actions-failure-debugging
description: Guide for debugging failing GitHub Actions workflows.
---

# Debugging GitHub Actions Workflows

This skill guides the AI to analyze failed workflows in a pull request by:
1. Identifying runs with `list_workflow_runs`.
2. Summarizing failures via `summarize_job_log_failures`.
3. Analyzing full logs with `get_job_logs` if needed.
4. Reproducing and proposing a fix.
