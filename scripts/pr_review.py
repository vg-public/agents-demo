#!/usr/bin/env python3
"""Collect Java-only GitHub Enterprise PR review inputs and publish one consented comment."""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any

API_VERSION = "2022-11-28"
JACOCO_VERSION = "0.8.12"
COMMENT_MARKER = "<!-- pr-review-automation:v1 -->"
FINDINGS_MARKER = re.compile(r"<!-- pr-review-findings:(.*?) -->", re.DOTALL)
CONFIRMATION = "POST PR REVIEW COMMENT"
CATEGORIES = {"BLOCKER", "CRITICAL", "MAJOR", "HIGH"}


class ReviewError(RuntimeError):
    """Raised when collection or publication cannot safely continue."""


def run(command: list[str], cwd: Path | None = None, check: bool = True,
        env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(command, cwd=cwd, env=env, text=True, capture_output=True, check=False)
    if check and result.returncode:
        details = (result.stderr or result.stdout).strip()
        raise ReviewError(f"Command failed ({result.returncode}): {' '.join(command)}\n{details}")
    return result


def repo_root(start: Path | None = None) -> Path:
    result = run(["git", "rev-parse", "--show-toplevel"], cwd=start or Path.cwd())
    return Path(result.stdout.strip()).resolve()


def remote_parts(remote: str) -> tuple[str, str, str]:
    if re.match(r"^[^/]+@[^:]+:", remote):
        host, path = remote.split(":", 1)
        host = host.rsplit("@", 1)[-1]
    else:
        parsed = urllib.parse.urlparse(remote)
        if not parsed.hostname:
            raise ReviewError(f"Cannot determine host from Git remote: {remote}")
        host, path = parsed.hostname, parsed.path
    parts = [part for part in path.strip("/").removesuffix(".git").split("/") if part]
    if len(parts) < 2:
        raise ReviewError(f"Cannot determine owner/repository from Git remote: {remote}")
    return host, parts[-2], parts[-1]


def parse_target(value: str, remote: str | None = None) -> tuple[str, str, str, int]:
    if value.isdecimal():
        if not remote:
            remote = run(["git", "remote", "get-url", "origin"]).stdout.strip()
        host, owner, repository = remote_parts(remote)
        return host, owner, repository, int(value)

    parsed = urllib.parse.urlparse(value)
    match = re.search(r"/(?:pull|pulls)/(\d+)(?:/|$)", parsed.path)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname or not match:
        raise ReviewError("Use a PR number or a GitHub Enterprise pull request URL.")
    path_parts = [part for part in parsed.path.strip("/").split("/") if part]
    if len(path_parts) < 4:
        raise ReviewError(f"PR URL must include owner, repository, and number: {value}")
    owner, repository = path_parts[0], path_parts[1]
    return parsed.hostname, owner, repository.removesuffix(".git"), int(match.group(1))


def api_root(host: str) -> str:
    return f"https://{host}/api/v3"


class GitHubEnterprise:
    def __init__(self, host: str, token: str):
        self.host = host
        self.root = api_root(host)
        self.token = token

    def request(self, url: str, accept: str = "application/vnd.github+json", method: str = "GET", body: dict[str, Any] | None = None) -> Any:
        data = json.dumps(body).encode("utf-8") if body is not None else None
        request = urllib.request.Request(
            url,
            data=data,
            method=method,
            headers={
                "Accept": accept,
                "Authorization": f"Bearer {self.token}",
                "X-GitHub-Api-Version": API_VERSION,
                "User-Agent": "java-pr-review-automation",
                **({"Content-Type": "application/json"} if data is not None else {}),
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                raw = response.read().decode("utf-8")
                content_type = response.headers.get("Content-Type", "")
                return json.loads(raw) if "json" in content_type else raw
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")[:1000]
            raise ReviewError(f"GitHub Enterprise API returned HTTP {error.code}: {detail}") from error
        except urllib.error.URLError as error:
            raise ReviewError(f"GitHub Enterprise API request failed: {error.reason}") from error

    def list_paginated(self, path: str) -> list[dict[str, Any]]:
        """Fetch API pages while preserving Link headers from each response."""
        results: list[dict[str, Any]] = []
        next_url: str | None = f"{self.root}{path}{'&' if '?' in path else '?'}per_page=100"
        while next_url:
            request = urllib.request.Request(
                next_url,
                headers={
                    "Accept": "application/vnd.github+json",
                    "Authorization": f"Bearer {self.token}",
                    "X-GitHub-Api-Version": API_VERSION,
                    "User-Agent": "java-pr-review-automation",
                },
            )
            try:
                with urllib.request.urlopen(request, timeout=60) as response:
                    page = json.loads(response.read().decode("utf-8"))
                    links = response.headers.get("Link", "")
            except urllib.error.HTTPError as error:
                detail = error.read().decode("utf-8", errors="replace")[:1000]
                raise ReviewError(f"GitHub Enterprise API returned HTTP {error.code}: {detail}") from error
            except (urllib.error.URLError, json.JSONDecodeError) as error:
                raise ReviewError(f"Could not read paginated API response: {error}") from error
            if not isinstance(page, list):
                raise ReviewError(f"Expected a paginated list from {next_url}")
            results.extend(page)
            next_url = None
            for link in re.finditer(r'<([^>]+)>;\s*rel="([^"]+)"', links):
                if link.group(2) == "next":
                    candidate = link.group(1)
                    if urllib.parse.urlparse(candidate).hostname != self.host:
                        raise ReviewError("Refusing pagination link outside the configured enterprise host.")
                    next_url = candidate
                    break
        return results


def diff_sections(diff: str) -> list[tuple[str, str]]:
    chunks = re.split(r"(?=^diff --git )", diff, flags=re.MULTILINE)
    selected: list[tuple[str, str]] = []
    for chunk in chunks:
        if not chunk.startswith("diff --git "):
            continue
        header = re.search(r"^diff --git a/(.*?) b/(.*?)$", chunk, re.MULTILINE)
        if not header:
            continue
        old_path, new_path = header.groups()
        if not (old_path.endswith(".java") or new_path.endswith(".java")):
            continue
        selected.append((new_path if new_path != "/dev/null" else old_path, chunk.rstrip() + "\n"))
    return selected


def changed_added_lines(java_diff: str) -> dict[str, list[int]]:
    changed: dict[str, set[int]] = {}
    path: str | None = None
    new_line = 0
    in_hunk = False
    for line in java_diff.splitlines():
        if line.startswith("diff --git "):
            match = re.search(r"^diff --git a/(.*?) b/(.*?)$", line)
            path = match.group(2) if match else None
            in_hunk = False
        elif line.startswith("@@"):
            match = re.match(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@", line)
            if match:
                new_line = int(match.group(1))
                in_hunk = True
        elif in_hunk and line.startswith("+") and not line.startswith("+++"):
            if path and path.endswith(".java"):
                changed.setdefault(path, set()).add(new_line)
            new_line += 1
        elif in_hunk and line.startswith("-") and not line.startswith("---"):
            continue
        elif in_hunk and line.startswith(" "):
            new_line += 1
    return {path: sorted(lines) for path, lines in changed.items()}


def coverage_from_xml(report: Path, changed_lines: dict[str, list[int]]) -> dict[str, Any]:
    if not report.is_file():
        return {"status": "unavailable", "reason": f"JaCoCo report not found: {report}", "percent": None, "covered": 0, "total": 0, "unmapped": []}
    try:
        root = ET.parse(report).getroot()
    except (ET.ParseError, OSError) as error:
        return {"status": "unavailable", "reason": f"Cannot parse JaCoCo report: {error}", "percent": None, "covered": 0, "total": 0, "unmapped": []}

    jacoco_lines: dict[str, dict[int, tuple[int, int, int, int]]] = {}
    for package in root.findall(".//package"):
        package_name = package.get("name", "")
        for source in package.findall("sourcefile"):
            relative = f"src/main/java/{package_name}/{source.get('name')}" if package_name else f"src/main/java/{source.get('name')}"
            jacoco_lines[relative] = {
                int(line.get("nr", "0")): (
                    int(line.get("ci", "0")),
                    int(line.get("mi", "0")),
                    int(line.get("cb", "0")),
                    int(line.get("mb", "0")),
                )
                for line in source.findall("line")
            }

    covered = 0
    total = 0
    branches_covered = 0
    branches_total = 0
    unmapped: list[str] = []
    for file_path, lines in changed_lines.items():
        if not file_path.startswith("src/main/java/"):
            continue
        counters = jacoco_lines.get(file_path)
        if counters is None:
            if lines:
                unmapped.extend(f"{file_path}:{number}" for number in lines)
            continue
        for number in lines:
            line_counters = counters.get(number)
            if line_counters is None:
                continue  # Non-executable lines are not part of JaCoCo's line denominator.
            line_covered, line_missed, line_branches_covered, line_branches_missed = line_counters
            if line_covered + line_missed:
                total += 1
                covered += int(line_covered > 0)
            branches_covered += line_branches_covered
            branches_total += line_branches_covered + line_branches_missed
    if unmapped:
        return {"status": "unavailable", "reason": "Changed production Java lines could not be mapped to JaCoCo source files.", "percent": None, "covered": covered, "total": total, "unmapped": unmapped}
    percent = round((covered / total) * 100, 2) if total else None
    branch_percent = round((branches_covered / branches_total) * 100, 2) if branches_total else None
    return {
        "status": "measured" if total else "unavailable",
        "reason": None if total else "No added/modified executable production Java lines were measurable.",
        "percent": percent,
        "covered": covered,
        "total": total,
        "branch_percent": branch_percent,
        "branches_covered": branches_covered,
        "branches_total": branches_total,
        "unmapped": [],
    }


def snapshot_java_sources(worktree: Path, java_paths: list[str], output: Path) -> dict[str, Any]:
    copied: list[str] = []
    unavailable: list[str] = []
    root = worktree.resolve()
    for file_path in java_paths:
        if not file_path.endswith(".java"):
            continue
        source = (worktree / file_path).resolve()
        try:
            source.relative_to(root)
        except ValueError:
            unavailable.append(file_path)
            continue
        if not source.is_file():
            unavailable.append(file_path)
            continue
        destination = output / file_path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(source.read_bytes())
        copied.append(file_path)
    return {"copied": copied, "unavailable": unavailable}


def run_coverage(root: Path, pr: dict[str, Any], changed_lines: dict[str, list[int]],
                 java_paths: list[str], source_output: Path) -> dict[str, Any]:
    worktree_parent = Path(tempfile.mkdtemp(prefix="pr-review-worktree-"))
    worktree = worktree_parent / "source"
    try:
        head = pr["head"]
        clone_url = (head.get("repo") or {}).get("clone_url") or (pr.get("base", {}).get("repo") or {}).get("clone_url")
        sha = head.get("sha")
        if not clone_url or not sha:
            return {"status": "unavailable", "reason": "PR head repository URL or SHA is missing.", "percent": None, "covered": 0, "total": 0, "unmapped": []}
        run(["git", "fetch", "--no-tags", "--no-write-fetch-head", clone_url, sha], cwd=root)
        commit = run(["git", "rev-parse", f"{sha}^{{commit}}"], cwd=root).stdout.strip()
        run(["git", "worktree", "add", "--detach", str(worktree), commit], cwd=root)
        write_json(source_output / "source_files.json", snapshot_java_sources(worktree, java_paths, source_output / "source"))
        isolated_home = worktree_parent / "home"
        isolated_home.mkdir()
        safe_settings = worktree_parent / "settings.xml"
        safe_settings.write_text("<settings/>\n", encoding="utf-8")
        build_env = {key: os.environ[key] for key in ("PATH", "JAVA_HOME", "LANG", "LC_ALL", "TMPDIR") if key in os.environ}
        build_env["HOME"] = str(isolated_home)
        build_env["USERPROFILE"] = str(isolated_home)
        command = [
            "mvn", "-B", "-ntp", "-s", str(safe_settings), f"-Dmaven.repo.local={worktree_parent / 'm2'}",
            "-Dspotless.check.skip=true", "-Dexec.skip=true", "test", "jacoco:report",
        ]
        result = run(command, cwd=worktree, check=False, env=build_env)
        report = worktree / "target/site/jacoco/jacoco.xml"
        if result.returncode:
            tail = (result.stderr or result.stdout).strip()[-2000:]
            return {"status": "unavailable", "reason": f"Maven tests or JaCoCo report failed (exit {result.returncode}): {tail}", "percent": None, "covered": 0, "total": 0, "unmapped": []}
        coverage = coverage_from_xml(report, changed_lines)
        coverage["head_sha"] = sha
        return coverage
    except (ReviewError, OSError) as error:
        return {"status": "unavailable", "reason": str(error), "percent": None, "covered": 0, "total": 0, "unmapped": []}
    finally:
        if worktree.exists():
            run(["git", "worktree", "remove", "--force", str(worktree)], cwd=root, check=False)
        shutil.rmtree(worktree_parent, ignore_errors=True)
        run(["git", "worktree", "prune"], cwd=root, check=False)


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")


def collect(target: str, output_root: Path, start: Path) -> Path:
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if not token:
        raise ReviewError("Set GITHUB_TOKEN or GH_TOKEN in the environment; tokens are never accepted as CLI arguments.")
    root = repo_root(start)
    host, owner, repository, number = parse_target(target)
    client = GitHubEnterprise(host, token)
    prefix = f"/repos/{owner}/{repository}"
    pr = client.request(f"{client.root}{prefix}/pulls/{number}")
    if not isinstance(pr, dict):
        raise ReviewError("PR details response was not a JSON object.")
    issue_comments = client.list_paginated(f"{prefix}/issues/{number}/comments")
    review_comments = client.list_paginated(f"{prefix}/pulls/{number}/comments")
    reviews = client.list_paginated(f"{prefix}/pulls/{number}/reviews")
    diff = client.request(f"{client.root}{prefix}/pulls/{number}", accept="application/vnd.github.v3.diff")
    sections = diff_sections(diff)
    java_diff = "".join(section for _, section in sections)
    changed_lines = changed_added_lines(java_diff)
    user = client.request(f"{client.root}/user")
    login = user.get("login") if isinstance(user, dict) else None
    previous = next((comment for comment in sorted(issue_comments, key=lambda item: item.get("created_at", ""), reverse=True)
                     if COMMENT_MARKER in comment.get("body", "") and (comment.get("user") or {}).get("login") == login), None)

    artifact = output_root.resolve() / str(number)
    artifact.mkdir(parents=True, exist_ok=True)
    write_json(artifact / "pr.json", {"host": host, "owner": owner, "repository": repository, "number": number, "api_url": client.root, "pr": pr})
    write_json(artifact / "comments.json", {"issue_comments": issue_comments, "review_comments": review_comments, "reviews": reviews})
    (artifact / "diff.txt").write_text(java_diff, encoding="utf-8")
    write_json(artifact / "changed_lines.json", changed_lines)
    previous_data = None
    if previous:
        previous_data = {
            "id": previous.get("id"),
            "updated_at": previous.get("updated_at"),
            "body": previous.get("body", ""),
            "findings": findings_from_comment(previous.get("body", "")),
        }
    write_json(artifact / "previous_review.json", previous_data)
    coverage = run_coverage(root, pr, changed_lines, [path for path, _ in sections], artifact)
    write_json(artifact / "coverage.json", coverage)
    print(f"Collected PR #{number} ({owner}/{repository}) from {host}")
    print(f"Java diff: {len(sections)} file(s), saved to {artifact / 'diff.txt'}")
    branch = coverage.get("branch_percent")
    branch_text = f", branches {branch}%" if branch is not None else ""
    print(f"Changed-line coverage: {coverage.get('percent')}%{branch_text} ({coverage.get('status')})")
    print(f"Artifacts: {artifact}")
    return artifact


def findings_from_comment(body: str) -> list[dict[str, Any]]:
    match = FINDINGS_MARKER.search(body)
    if not match:
        return []
    try:
        data = json.loads(match.group(1))
        return data if isinstance(data, list) else []
    except json.JSONDecodeError:
        return []


def markdown_cell(value: Any) -> str:
    return str(value).replace("|", "\\|").replace("\r", " ").replace("\n", "<br>")


def validate_findings(findings: Any, changed_lines: dict[str, list[int]]) -> list[dict[str, Any]]:
    if not isinstance(findings, list):
        raise ReviewError("findings.json must contain a 'findings' array.")
    validated = []
    for index, finding in enumerate(findings, 1):
        if not isinstance(finding, dict):
            raise ReviewError(f"Finding {index} must be an object.")
        category = str(finding.get("category", "")).upper()
        file_path = str(finding.get("file", ""))
        lines = finding.get("lines", [])
        if category not in CATEGORIES or not file_path.endswith(".java") or not isinstance(lines, list) or not lines:
            raise ReviewError(f"Finding {index} needs a valid category, Java file, and changed line numbers.")
        allowed = set(changed_lines.get(file_path, []))
        if any(not isinstance(number, int) or number not in allowed for number in lines):
            raise ReviewError(f"Finding {index} references lines outside the added Java diff in {file_path}.")
        validated.append({
            "id": str(finding.get("id") or f"finding-{index}"),
            "issue": str(finding.get("issue", "")),
            "category": category,
            "file": file_path,
            "lines": sorted(set(lines)),
            "details": str(finding.get("details", "")),
            "prompt": str(finding.get("prompt", "")),
        })
    return validated


def render_comment(findings: list[dict[str, Any]], coverage: dict[str, Any], previous: dict[str, Any] | None,
                   prior_statuses: list[dict[str, Any]], summary: str) -> tuple[str, str]:
    if previous:
        old_findings = previous.get("findings", [])
        status_by_id = {str(item.get("id")): item for item in prior_statuses if isinstance(item, dict)}
        states = []
        for old in old_findings:
            state = str(status_by_id.get(str(old.get("id")), {}).get("status", "NOT REASSESSABLE")).upper()
            states.append(state if state in {"FIXED", "STILL OPEN", "NOT REASSESSABLE"} else "NOT REASSESSABLE")
        lines = [
            "### Previous review findings",
            "",
            f"Status count: {states.count('FIXED')} fixed, {states.count('STILL OPEN')} still open, "
            f"{states.count('NOT REASSESSABLE')} not reassessable.",
            "",
            "| Finding ID | Previous issue | Status | Details |",
            "|---|---|---|---|",
        ]
        if not old_findings:
            lines.append("| n/a | Previous comment did not contain parseable finding metadata | NOT REASSESSABLE | Reassess manually against the current Java changes. |")
        for old in old_findings:
            status = status_by_id.get(str(old.get("id")), {})
            state = str(status.get("status", "NOT REASSESSABLE")).upper()
            if state not in {"FIXED", "STILL OPEN", "NOT REASSESSABLE"}:
                state = "NOT REASSESSABLE"
            details = status.get("details", "No reassessment was supplied.")
            lines.append(f"| {markdown_cell(old.get('id', ''))} | {markdown_cell(old.get('issue', ''))} | {state} | {markdown_cell(details)} |")
        prior_table = "\n".join(lines)
    else:
        prior_table = ""

    table = ["| SNO | Issue | Category | Code line nos | Issue details | Proposed developer prompt |", "|---:|---|---|---|---|---|"]
    for index, finding in enumerate(findings, 1):
        locations = ", ".join(f"{finding['file']}:{number}" for number in finding["lines"])
        table.append("| " + " | ".join(markdown_cell(value) for value in (
            index, finding["issue"], finding["category"], locations, finding["details"], finding["prompt"]
        )) + " |")
    if not findings:
        table.append("| - | No actionable findings | - | - | No issues were identified in changed Java lines. | - |")

    if coverage.get("status") == "measured":
        coverage_text = f"Lines {coverage['percent']}% ({coverage['covered']}/{coverage['total']})"
        branch_percent = coverage.get("branch_percent")
        if branch_percent is not None:
            coverage_text += f"; branches {branch_percent}% ({coverage.get('branches_covered', 0)}/{coverage.get('branches_total', 0)})"
        coverage_text += " of changed executable production Java code."
        coverage_failed = coverage["percent"] < 80 or (branch_percent is not None and branch_percent < 80)
    else:
        coverage_text = f"Unavailable. {coverage.get('reason') or 'Coverage could not be measured.'}"
        coverage_failed = True
    decision = "HOLD" if findings or coverage_failed else "APPROVE"
    sections = [
        COMMENT_MARKER,
        "## PR Review Summary",
        "",
        markdown_cell(summary or "Review of the changed Java code is complete."),
        "",
        f"**Changed-code coverage:** {coverage_text} Required: 80%.",
        "",
        "### Findings",
        "",
        "\n".join(table),
    ]
    if prior_table:
        sections.extend(["", prior_table])
    sections.extend(["", f"**AI suggestion: {decision}.** This is advisory and is not a formal GitHub review decision."])
    metadata = [{"id": finding["id"], "issue": finding["issue"], "category": finding["category"], "file": finding["file"], "lines": finding["lines"]} for finding in findings]
    sections.append(f"\n<!-- pr-review-findings:{json.dumps(metadata, separators=(',', ':'))} -->")
    return "\n".join(sections), decision


def publish(artifact: Path, findings_path: Path) -> None:
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if not token:
        raise ReviewError("Set GITHUB_TOKEN or GH_TOKEN in the environment; tokens are never accepted as CLI arguments.")
    pr_data = json.loads((artifact / "pr.json").read_text(encoding="utf-8"))
    changed_lines = json.loads((artifact / "changed_lines.json").read_text(encoding="utf-8"))
    coverage = json.loads((artifact / "coverage.json").read_text(encoding="utf-8"))
    previous = json.loads((artifact / "previous_review.json").read_text(encoding="utf-8"))
    findings_data = json.loads(findings_path.read_text(encoding="utf-8"))
    findings = validate_findings(findings_data.get("findings", []), changed_lines)
    comment, decision = render_comment(findings, coverage, previous, findings_data.get("prior_statuses", []), findings_data.get("summary", ""))
    print("\n" + comment + "\n")
    if not sys.stdin.isatty():
        raise ReviewError("Refusing to post without an interactive human confirmation.")
    answer = input(f'Type "{CONFIRMATION}" to publish (anything else cancels): ').strip()
    if answer != CONFIRMATION:
        print("Cancelled; GitHub was not changed.")
        return

    client = GitHubEnterprise(pr_data["host"], token)
    prefix = f"{client.root}/repos/{pr_data['owner']}/{pr_data['repository']}/issues/{pr_data['number']}/comments"
    if previous and previous.get("id"):
        response = client.request(f"{prefix}/{previous['id']}", method="PATCH", body={"body": comment})
        action = "Updated"
    else:
        response = client.request(prefix, method="POST", body={"body": comment})
        action = "Posted"
    print(f"{action} consolidated PR comment. Advisory decision: {decision}. Comment URL: {response.get('html_url', 'unknown')}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    collect_parser = commands.add_parser("collect", help="Fetch PR data and measure changed-line coverage")
    collect_parser.add_argument("target", help="PR number or GitHub Enterprise PR URL")
    collect_parser.add_argument("--output", type=Path, default=Path("artifacts/pr-review"), help="Artifact root (default: artifacts/pr-review)")
    collect_parser.add_argument("--repo-root", type=Path, default=Path.cwd(), help="Local repository used for remote resolution and worktree (default: current directory)")
    publish_parser = commands.add_parser("publish", help="Render and, after confirmation, post one consolidated comment")
    publish_parser.add_argument("artifact", type=Path, help="Artifact directory created by collect")
    publish_parser.add_argument("--findings", type=Path, required=True, help="Agent-authored findings JSON")
    args = parser.parse_args()
    try:
        if args.command == "collect":
            collect(args.target, args.output, args.repo_root)
        else:
            publish(args.artifact.resolve(), args.findings.resolve())
    except (ReviewError, OSError, json.JSONDecodeError, KeyError, ValueError) as error:
        print(f"PR review failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
