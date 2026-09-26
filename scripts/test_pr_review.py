import json
import os
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

import pr_review


class FakeResponse:
    def __init__(self, body, headers=None):
        self.body = body
        self.headers = headers or {}

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def read(self):
        return self.body.encode("utf-8")


class TargetParsingTests(unittest.TestCase):
    def test_pr_url_extracts_github_enterprise_repository(self):
        self.assertEqual(
            pr_review.parse_target("https://github.example.corp/team/catalog/pull/47"),
            ("github.example.corp", "team", "catalog", 47),
        )

    def test_number_uses_enterprise_ssh_remote(self):
        with patch.object(pr_review, "run") as run:
            run.return_value.stdout = "git@github.example.corp:team/catalog.git\n"
            self.assertEqual(pr_review.parse_target("8"), ("github.example.corp", "team", "catalog", 8))

    def test_rejects_unrecognized_url(self):
        with self.assertRaises(pr_review.ReviewError):
            pr_review.parse_target("https://github.example.corp/team/catalog/issues/9")


class DiffParsingTests(unittest.TestCase):
    def setUp(self):
        self.diff = (
            "diff --git a/src/main/java/demo/Service.java b/src/main/java/demo/Service.java\n"
            "index 123..456 100644\n--- a/src/main/java/demo/Service.java\n+++ b/src/main/java/demo/Service.java\n"
            "@@ -4,3 +4,4 @@ class Service {\n"
            " old();\n-removed();\n+added();\n+next();\n }\n"
            "diff --git a/README.md b/README.md\nindex 123..456 100644\n"
            "--- a/README.md\n+++ b/README.md\n@@ -1 +1 @@\n-a\n+b\n"
        )

    def test_keeps_only_java_sections_and_tracks_added_line_numbers(self):
        sections = pr_review.diff_sections(self.diff)
        self.assertEqual(len(sections), 1)
        java_diff = "".join(section for _, section in sections)
        self.assertEqual(pr_review.changed_added_lines(java_diff), {"src/main/java/demo/Service.java": [5, 6]})

    def test_tracks_renamed_java_file_using_new_path(self):
        diff = (
            "diff --git a/src/Old.java b/src/New.java\nrename from src/Old.java\nrename to src/New.java\n"
            "@@ -1 +1,2 @@\n old();\n+new();\n"
        )
        self.assertEqual(pr_review.diff_sections(diff)[0][0], "src/New.java")
        self.assertEqual(pr_review.changed_added_lines(diff), {"src/New.java": [2]})


class CoverageTests(unittest.TestCase):
    def test_maps_changed_lines_from_jacoco_xml(self):
        xml = """<report name="demo"><package name="demo"><sourcefile name="Service.java">
          <line nr="6" mi="1" ci="0" mb="0" cb="0"/>
          <line nr="7" mi="0" ci="1" mb="1" cb="1"/>
        </sourcefile></package></report>"""
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "jacoco.xml"
            report.write_text(xml, encoding="utf-8")
            result = pr_review.coverage_from_xml(report, {"src/main/java/demo/Service.java": [6, 7]})
        self.assertEqual(result["status"], "measured")
        self.assertEqual(result["percent"], 50.0)
        self.assertEqual(result["covered"], 1)
        self.assertEqual(result["total"], 2)
        self.assertEqual(result["branch_percent"], 50.0)
        self.assertEqual(result["branches_total"], 2)

    def test_missing_report_is_unavailable_not_zero(self):
        result = pr_review.coverage_from_xml(Path("missing-jacoco.xml"), {})
        self.assertEqual(result["status"], "unavailable")
        self.assertIsNone(result["percent"])

    def test_snapshots_only_java_sources_inside_worktree(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            worktree = base / "worktree"
            source = worktree / "src/main/java/demo/Service.java"
            source.parent.mkdir(parents=True)
            source.write_text("class Service {}\n", encoding="utf-8")
            output = base / "artifacts"
            result = pr_review.snapshot_java_sources(
                worktree, ["src/main/java/demo/Service.java", "../../outside.java"], output
            )
            self.assertEqual(result["copied"], ["src/main/java/demo/Service.java"])
            self.assertEqual(result["unavailable"], ["../../outside.java"])
            self.assertEqual((output / "src/main/java/demo/Service.java").read_text(encoding="utf-8"), "class Service {}\n")

    def test_coverage_build_strips_token_and_cleans_worktree_on_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = root / "artifacts"
            output.mkdir()
            commands = []

            def fake_run(command, cwd=None, check=True, env=None):
                commands.append(command)
                if command[:3] == ["git", "worktree", "add"]:
                    Path(command[4]).mkdir()
                    source = Path(command[4]) / "src/main/java/demo/Service.java"
                    source.parent.mkdir(parents=True)
                    source.write_text("class Service {}\n", encoding="utf-8")
                if command[0] == "mvn":
                    self.assertNotIn("GITHUB_TOKEN", env)
                    self.assertNotEqual(env["HOME"], os.path.expanduser("~"))
                    return SimpleNamespace(returncode=1, stdout="", stderr="tests failed")
                if command[1:3] == ["rev-parse", "abc123^{commit}"]:
                    return SimpleNamespace(returncode=0, stdout="abc123\n", stderr="")
                return SimpleNamespace(returncode=0, stdout="", stderr="")

            pr = {"head": {"sha": "abc123", "repo": {"clone_url": "https://github.example/team/repo.git"}}}
            with patch.dict(os.environ, {"GITHUB_TOKEN": "do-not-pass-to-maven"}), patch.object(pr_review, "run", side_effect=fake_run):
                result = pr_review.run_coverage(root, pr, {}, ["src/main/java/demo/Service.java"], output)

            self.assertEqual(result["status"], "unavailable")
            self.assertTrue((output / "source/src/main/java/demo/Service.java").is_file())
            self.assertTrue(any(command[:3] == ["git", "worktree", "remove"] for command in commands))


class CommentTests(unittest.TestCase):
    def test_rejects_finding_outside_added_lines(self):
        finding = [{"id": "x", "category": "MAJOR", "file": "src/A.java", "lines": [3]}]
        with self.assertRaises(pr_review.ReviewError):
            pr_review.validate_findings(finding, {"src/A.java": [4]})

    def test_comment_holds_below_coverage_threshold(self):
        comment, decision = pr_review.render_comment(
            [], {"status": "measured", "percent": 79.9, "covered": 799, "total": 1000}, None, [], "Reviewed."
        )
        self.assertEqual(decision, "HOLD")
        self.assertIn("79.9%", comment)
        self.assertIn(pr_review.COMMENT_MARKER, comment)

    def test_comment_holds_when_branch_coverage_is_below_threshold(self):
        _, decision = pr_review.render_comment(
            [], {"status": "measured", "percent": 100.0, "covered": 10, "total": 10,
                 "branch_percent": 75.0, "branches_covered": 3, "branches_total": 4}, None, [], "Reviewed."
        )
        self.assertEqual(decision, "HOLD")

    def test_prior_findings_are_rendered_as_a_separate_table(self):
        previous = {"findings": [{"id": "S2259-service", "issue": "Null dereference"}]}
        comment, _ = pr_review.render_comment(
            [], {"status": "measured", "percent": 100.0, "covered": 2, "total": 2}, previous,
            [{"id": "S2259-service", "status": "FIXED", "details": "Guard now handles missing data."}], "Reviewed."
        )
        self.assertIn("### Previous review findings", comment)
        self.assertIn("1 fixed, 0 still open, 0 not reassessable", comment)
        self.assertIn("FIXED", comment)

    def test_finding_metadata_can_be_read_from_previous_comment(self):
        comment = '<!-- pr-review-findings:[{"id":"X1","issue":"Example"}] -->'
        self.assertEqual(pr_review.findings_from_comment(comment)[0]["id"], "X1")


class PaginationTests(unittest.TestCase):
    def test_follows_next_link_for_same_enterprise_host(self):
        responses = [
            FakeResponse('[{"id":1}]', {"Link": '<https://ghe.example/api/v3/repos/a/b/issues/1/comments?per_page=100&page=2>; rel="next"'}),
            FakeResponse('[{"id":2}]'),
        ]
        with patch("pr_review.urllib.request.urlopen", side_effect=responses) as urlopen:
            client = pr_review.GitHubEnterprise("ghe.example", "token")
            result = client.list_paginated("/repos/a/b/issues/1/comments")
        self.assertEqual([item["id"] for item in result], [1, 2])
        self.assertEqual(urlopen.call_count, 2)

    def test_rejects_pagination_link_to_another_host(self):
        response = FakeResponse('[{"id":1}]', {"Link": '<https://evil.example/page>; rel="next"'})
        with patch("pr_review.urllib.request.urlopen", return_value=response):
            client = pr_review.GitHubEnterprise("ghe.example", "token")
            with self.assertRaises(pr_review.ReviewError):
                client.list_paginated("/repos/a/b/issues/1/comments")


if __name__ == "__main__":
    unittest.main()
