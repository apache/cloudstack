#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# Checks @APICommand/@Parameter/@Param `since` usage on branches that have
# moved past the old 4.x version scheme (i.e. project version >= 24):
#
#  1. A newly added since="4.x" is flagged - contributors often add this out
#     of muscle memory even though the project is now versioned e.g. 24.0.0.
#  2. A brand-new @APICommand/@Parameter/@Param (its annotation AND the
#     class/field it annotates are both newly added together) that has no
#     since attribute at all is flagged - new API surface should record when
#     it was introduced. Editing an existing annotation (its declaration
#     line is not part of the diff) never requires since, even if the
#     existing element never had one.
#
# Only lines actually added by the PR are inspected. A value/field is
# ignored if the same PR also removes the identical since="..." value, or
# the identical field/class name, elsewhere in the same file's diff - this
# covers a field being moved or reformatted rather than a genuinely new
# API/param/response field.

import argparse
import re
import subprocess
import sys

ANNOTATION_START_RE = re.compile(r"@(Param|Parameter|APICommand)\s*\(")
SINCE_RE = re.compile(r'since\s*=\s*"(4\.\d[\w.]*)"')
HAS_SINCE_RE = re.compile(r"\bsince\s*=")
VERSION_RE = re.compile(r"<artifactId>cloudstack</artifactId>\s*<version>([^<]+)</version>")
FIELD_DECL_RE = re.compile(r"^\s*(?:private|protected|public)\b[^=;(){}]*?(\w+)\s*;\s*$")
CLASS_DECL_RE = re.compile(r"^\s*(?:public\s+)?(?:final\s+)?class\s+(\w+)")


def read_project_version(pom_path: str) -> str:
    with open(pom_path, encoding="utf-8") as f:
        content = f.read()
    match = VERSION_RE.search(content)
    if not match:
        raise SystemExit(f"Could not find the cloudstack project version in {pom_path}")
    return match.group(1)


def major_version(version: str) -> int:
    match = re.match(r"(\d+)", version)
    if not match:
        raise SystemExit(f"Could not parse a major version from '{version}'")
    return int(match.group(1))


def git_diff(base: str, head: str) -> str:
    return subprocess.run(
        ["git", "diff", "--no-color", "--unified=0", base, head, "--", "*.java"],
        check=True,
        capture_output=True,
        text=True,
    ).stdout


def diff_path(line: str) -> str:
    path = line[4:]
    return path[2:] if path.startswith(("a/", "b/")) else path


def parse_hunks(diff_text: str) -> dict:
    """file -> list of hunks, each hunk = {"added": [lines], "removed": [lines]}"""
    files: dict = {}
    current_file = None
    current_hunk = None
    for line in diff_text.splitlines():
        if line.startswith("+++ "):
            current_file = diff_path(line)
            files.setdefault(current_file, [])
            current_hunk = None
        elif line.startswith("--- "):
            continue
        elif line.startswith("@@"):
            current_hunk = {"added": [], "removed": []}
            files[current_file].append(current_hunk)
        elif current_hunk is not None:
            if line.startswith("+") and not line.startswith("+++"):
                current_hunk["added"].append(line[1:])
            elif line.startswith("-") and not line.startswith("---"):
                current_hunk["removed"].append(line[1:])
    return files


def file_removed_text(hunks: list) -> str:
    return "\n".join(l for h in hunks for l in h["removed"])


def find_old_scheme_violations(files: dict) -> list:
    violations = []
    for path, hunks in files.items():
        removed_values = set(SINCE_RE.findall(file_removed_text(hunks)))
        for hunk in hunks:
            for line in hunk["added"]:
                for value in SINCE_RE.findall(line):
                    if value in removed_values:
                        continue
                    violations.append(("old_scheme", path, line.strip(), value))
    return violations


def find_matching_paren(text: str, open_pos: int) -> int:
    depth = 1
    i = open_pos + 1
    while i < len(text) and depth:
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
        i += 1
    return i - 1 if depth == 0 else -1


def find_missing_since_violations(files: dict) -> list:
    violations = []
    for path, hunks in files.items():
        removed_text = file_removed_text(hunks)
        for hunk in hunks:
            joined = "\n".join(hunk["added"])
            for match in ANNOTATION_START_RE.finditer(joined):
                kind = match.group(1)
                open_pos = match.end() - 1
                close_pos = find_matching_paren(joined, open_pos)
                if close_pos == -1:
                    continue  # annotation not fully contained in this hunk; can't tell, skip
                annotation_text = joined[match.start():close_pos + 1]
                if HAS_SINCE_RE.search(annotation_text):
                    continue  # has since (old-scheme check handles wrong values separately)

                remainder = joined[close_pos + 1:]
                decl_re = CLASS_DECL_RE if kind == "APICommand" else FIELD_DECL_RE
                decl_match = None
                for candidate in remainder.splitlines():
                    candidate = candidate.strip()
                    if not candidate:
                        continue
                    decl_match = decl_re.match(candidate)
                    break  # only look at the next non-blank added line

                if not decl_match:
                    continue  # declaration wasn't (re)added alongside the annotation -> a modification, not new

                name = decl_match.group(1)
                if re.search(rf"\b{re.escape(name)}\b\s*[;{{]", removed_text):
                    continue  # same name also removed elsewhere in this file's diff -> likely a move/reformat

                violations.append(("missing_since", path, annotation_text.strip(), kind))
    return violations


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pom", default="pom.xml")
    parser.add_argument("--base", required=True, help="Base commit SHA of the PR")
    parser.add_argument("--head", required=True, help="Head commit SHA of the PR")
    args = parser.parse_args()

    version = read_project_version(args.pom)
    major = major_version(version)
    print(f"Project version from {args.pom}: {version} (major: {major})")

    if major < 24:
        print(
            "Project major version is below 24; API annotations still use the "
            "4.x 'since' scheme on this branch. Skipping check."
        )
        return 0

    diff_text = git_diff(args.base, args.head)
    files = parse_hunks(diff_text)

    old_scheme = find_old_scheme_violations(files)
    missing_since = find_missing_since_violations(files)

    if not old_scheme and not missing_since:
        print("No newly added/changed API annotations have a 'since' problem.")
        return 0

    for _, path, line, value in old_scheme:
        print(
            f'::error file={path}::since="{value}" uses the pre-24 CloudStack versioning scheme. '
            f'This project is now versioned {version}; new @APICommand/@Parameter/@Param annotations '
            f'should use since="{major}.x" (e.g. "{major}.0") instead. Offending line: {line}'
        )

    for _, path, annotation_text, kind in missing_since:
        snippet = " ".join(annotation_text.split())
        print(
            f"::error file={path}::A newly added @{kind} is missing a 'since' attribute. "
            f'New API commands/params/response fields should record when they were introduced, '
            f'e.g. since="{major}.0". Annotation: {snippet}'
        )

    total = len(old_scheme) + len(missing_since)
    print(f"\n{total} issue(s) found ({len(old_scheme)} outdated 4.x value(s), {len(missing_since)} missing since).")
    print(
        "Note: this only flags brand-new annotations (added together with the class/field they "
        "annotate) and newly added since values; editing an existing annotation never requires "
        "adding since, and a moved/reformatted field is detected by its name also appearing on a "
        "removed line in the same file and is not flagged."
    )
    return 1


if __name__ == "__main__":
    sys.exit(main())
