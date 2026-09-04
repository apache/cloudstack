#!/usr/bin/env python3
# -*- coding: utf-8 -*-
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

"""
error_messages_tool.py — Audit and fix the CloudStack error-messages JSON file.

BACKGROUND
----------
CloudStack uses a flat JSON file (error-messages.json) to map short dot-separated
error keys (e.g. "vm.deploy.snapshot.not.found") to human-readable message templates
that may contain {{placeholder}} variables.  These keys are referenced as string
literals in Java source code via Exceptions.* helpers and ResponseMessageResolver.

This tool helps maintain that file by detecting problems and applying safe fixes.

COMMANDS
--------
  errors      Report structural problems: missing/trailing commas, unexpected
              lines, missing opening/closing braces.

  duplicates  List every key that appears more than once, together with the line
              numbers of all occurrences.

  unused      Walk a source tree and report keys that are never referenced as a
              string literal in any .java, .groovy, .kt, .xml, or .py file.
              Requires --root to point at the repository root (defaults to the
              current working directory).

  fix         Produce a syntactically correct file by:
                - removing duplicate keys (the last occurrence is kept)
                - adding missing commas between entries
                - removing trailing commas from the last entry
              Overwrites the input file unless --output is given.
              Use --dry-run to preview changes without writing.

  sort        Like fix, but also sorts all keys alphabetically.

USAGE
-----
  error_messages_tool.py <command> [options]

OPTIONS
-------
  -f, --file PATH    Path to the error messages JSON file (required).
  -r, --root PATH    Repository root used by the 'unused' command to locate
                     source files.  Default: current working directory.
  -o, --output PATH  Destination file for 'fix' and 'sort'.
                     Default: overwrites the input file in-place.
  -v, --verbose      'unused': also print the files that reference each used key.
  --dry-run          'fix'/'sort': print the result to stdout instead of writing.
  -h, --help         Show this message and exit.

EXAMPLES
--------
  # Check for structural errors
  error_messages_tool.py errors -f client/conf/error-messages.json.in

  # Find keys with no code references (run from the repository root)
  error_messages_tool.py unused -f client/conf/error-messages.json.in

  # Preview what 'fix' would produce without writing
  error_messages_tool.py fix -f client/conf/error-messages.json.in --dry-run

  # Fix and write to a separate file
  error_messages_tool.py fix -f client/conf/error-messages.json.in -o /tmp/fixed.json

  # Sort keys alphabetically and overwrite in place
  error_messages_tool.py sort -f client/conf/error-messages.json.in

EXIT STATUS
-----------
  0  No issues found / fix written successfully.
  1  Issues found (errors, duplicates, unused keys) or fix was a no-op.
"""

import os
import re
import sys
from collections import OrderedDict
from pathlib import Path

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

# File extensions searched by the 'unused' command.
SOURCE_EXTENSIONS = {".java", ".groovy", ".kt", ".xml", ".py"}

# Directories skipped during source-file discovery to avoid scanning build
# artefacts, version-control metadata, and vendored dependencies.
EXCLUDE_DIRS = {".git", "target", "build", "__pycache__", "node_modules"}

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------

class Args:
    """Parsed command-line arguments."""

    def __init__(self, argv):
        self.command  = None
        self.file     = None
        self.root     = Path.cwd()
        self.output   = None   # resolved to self.file after parsing if unset
        self.verbose  = False
        self.dry_run  = False

        it = iter(argv[1:])
        positionals = []

        for arg in it:
            if arg in ("-h", "--help"):
                print(__doc__)
                sys.exit(0)
            elif arg in ("-v", "--verbose"):
                self.verbose = True
            elif arg == "--dry-run":
                self.dry_run = True
            elif arg in ("-f", "--file"):
                self.file = Path(next(it))
            elif arg in ("-r", "--root"):
                self.root = Path(next(it))
            elif arg in ("-o", "--output"):
                self.output = Path(next(it))
            elif arg.startswith("-"):
                _die(f"Unknown option: {arg!r}. Run with --help for usage.")
            else:
                positionals.append(arg)

        if not positionals:
            print(__doc__)
            sys.exit(0)

        self.command = positionals[0]
        valid = ("unused", "duplicates", "errors", "fix", "sort")
        if self.command not in valid:
            _die(f"Unknown command: {self.command!r}. Expected one of: {', '.join(valid)}")

        if self.file is None:
            _die("No file specified. Use -f/--file to provide the error messages JSON file.")

        if self.output is None:
            self.output = self.file


# ---------------------------------------------------------------------------
# Utilities
# ---------------------------------------------------------------------------

def _die(msg):
    """Print an error message to stderr and exit with status 1."""
    print(f"error: {msg}", file=sys.stderr)
    sys.exit(1)


def _load_text(path: Path) -> str:
    """Read *path* as UTF-8 text, aborting with a clear message if not found."""
    if not path.exists():
        _die(f"File not found: {path}")
    return path.read_text(encoding="utf-8")


# ---------------------------------------------------------------------------
# Core data structures
# ---------------------------------------------------------------------------

class Entry:
    """One key/value pair extracted from the JSON file, with source location.

    Attributes:
        line      1-based line number of this entry in the source file.
        key       Decoded (unescaped) key string.
        raw_key   The key exactly as it appears in the file, including quotes.
        raw_value The value exactly as it appears in the file, including quotes.
    """
    __slots__ = ("line", "key", "raw_key", "raw_value")

    def __init__(self, line: int, key: str, raw_key: str, raw_value: str):
        self.line      = line
        self.key       = key
        self.raw_key   = raw_key
        self.raw_value = raw_value


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------

# Matches a single JSON entry line of the form:
#   "key" : "value"          (with or without a trailing comma)
# Both key and value may contain backslash-escaped characters.
# Group 1 → raw key (with surrounding quotes)
# Group 2 → raw value (with surrounding quotes)
_ENTRY_RE = re.compile(
    r'^\s*("(?:[^"\\]|\\.)*")\s*:\s*("(?:[^"\\]|\\.)*")\s*,?\s*$'
)

_OPEN_BRACE_RE  = re.compile(r'^\s*\{\s*$')
_CLOSE_BRACE_RE = re.compile(r'^\s*\}\s*$')


def _unescape_json_string(s: str) -> str:
    """Strip surrounding double-quotes and decode JSON escape sequences."""
    inner = s[1:-1]  # remove leading/trailing quote
    return inner.encode("raw_unicode_escape").decode("unicode_escape")


def parse_entries(text: str) -> list:
    """Return a list of Entry objects parsed from *text* in file order.

    Parsing is regex-based rather than using json.loads so the function
    tolerates syntax errors (missing commas, etc.) that would cause a strict
    JSON parser to fail.
    """
    entries = []
    for lineno, line in enumerate(text.splitlines(), 1):
        m = _ENTRY_RE.match(line)
        if not m:
            continue
        raw_key, raw_value = m.group(1), m.group(2)
        try:
            key = _unescape_json_string(raw_key)
        except Exception:
            key = raw_key[1:-1]  # fallback: strip quotes without further decoding
        entries.append(Entry(lineno, key, raw_key, raw_value))
    return entries


# ---------------------------------------------------------------------------
# Structural error detection
# ---------------------------------------------------------------------------

def detect_errors(text: str) -> list:
    """Scan *text* for structural JSON problems and return a list of descriptions.

    Detected issues:
      - Missing opening ``{`` on the first non-blank line.
      - Missing closing ``}`` on the last non-blank line.
      - Missing comma separator between two consecutive entries.
      - Trailing comma on the final entry.
      - Non-blank, non-brace lines that do not match the expected entry format.
    """
    errors = []
    lines = text.splitlines()

    content_lines = [l for l in lines if l.strip()]
    if not content_lines:
        errors.append("File is empty.")
        return errors

    if not _OPEN_BRACE_RE.match(content_lines[0]):
        errors.append(f"Line 1: Expected opening '{{', got: {content_lines[0]!r}")
    if not _CLOSE_BRACE_RE.match(content_lines[-1]):
        errors.append(f"Line {len(lines)}: Expected closing '}}', got: {content_lines[-1]!r}")

    # Collect 0-based indices of lines that are valid entries.
    entry_indices = [i for i, l in enumerate(lines) if _ENTRY_RE.match(l)]

    for pos, idx in enumerate(entry_indices):
        raw          = lines[idx].rstrip()
        is_last      = (pos == len(entry_indices) - 1)
        has_comma    = raw.endswith(",")

        if is_last and has_comma:
            errors.append(f"Line {idx + 1}: Trailing comma on last entry.")
        elif not is_last and not has_comma:
            next_line = entry_indices[pos + 1] + 1
            errors.append(
                f"Line {idx + 1}: Missing comma after entry "
                f"(next entry is on line {next_line})."
            )

    # Flag lines inside the braces that are neither entries nor blank.
    for i, line in enumerate(lines[1:-1], 2):  # lines[0] and lines[-1] are braces
        if line.strip() and not _ENTRY_RE.match(line):
            errors.append(f"Line {i}: Unexpected content: {line!r}")

    return errors


# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------

def cmd_errors(args: Args) -> int:
    """Report structural JSON errors in the file.

    Returns the number of errors found (0 = clean).
    """
    text   = _load_text(args.file)
    issues = detect_errors(text)
    if issues:
        print(f"{len(issues)} error(s) in {args.file}:")
        for issue in issues:
            print(f"  {issue}")
    else:
        print(f"No structural errors found in {args.file}.")
    return len(issues)


def cmd_duplicates(args: Args) -> int:
    """List keys that appear more than once, with all their line numbers.

    Returns the number of distinct duplicated keys (0 = no duplicates).
    """
    text    = _load_text(args.file)
    entries = parse_entries(text)

    seen = {}
    for e in entries:
        seen.setdefault(e.key, []).append(e.line)

    dupes = {k: v for k, v in seen.items() if len(v) > 1}
    if dupes:
        print(f"{len(dupes)} duplicate key(s) in {args.file}:")
        for key, line_nums in sorted(dupes.items()):
            print(f"  {key!r}  —  lines: {', '.join(str(n) for n in line_nums)}")
    else:
        print("No duplicate keys found.")
    return len(dupes)


def cmd_unused(args: Args) -> int:
    """Find keys that have no references in the source tree under --root.

    Each key is searched as a plain substring across all files whose extension
    is in SOURCE_EXTENSIONS.  A key is considered unused only if the literal
    string does not appear anywhere in those files.

    Returns the number of unused keys (0 = all keys referenced).
    """
    text    = _load_text(args.file)
    entries = parse_entries(text)
    keys    = [e.key for e in entries]

    print(f"Loaded {len(keys)} keys from {args.file}", flush=True)

    if not args.root.is_dir():
        _die(f"Source root not found: {args.root}. Use -r to specify the repository root.")

    source_files = []
    for dirpath, dirnames, filenames in os.walk(args.root):
        dirnames[:] = [d for d in dirnames if d not in EXCLUDE_DIRS]
        for name in filenames:
            if Path(name).suffix in SOURCE_EXTENSIONS:
                source_files.append(Path(dirpath) / name)

    print(f"Scanning {len(source_files)} source files under {args.root} ...", flush=True)

    file_contents = {}
    for f in source_files:
        try:
            file_contents[f] = f.read_text(errors="replace")
        except OSError:
            pass

    unused = []
    for key in keys:
        usages = [f for f, c in file_contents.items() if key in c]
        if not usages:
            unused.append(key)
        elif args.verbose:
            print(f"  [used] {key} — {len(usages)} file(s)")
            for f in usages:
                print(f"    {f}")

    print()
    if unused:
        print(f"Unused keys ({len(unused)} / {len(keys)}):")
        for key in sorted(unused):
            print(f"  {key}")
    else:
        print("All keys are referenced in the codebase.")

    return len(unused)


def _build_fixed_text(entries: list) -> str:
    """Serialise *entries* as a properly formatted JSON object.

    Produces exactly one entry per line with 4-space indentation.  All entries
    except the last have a trailing comma; the last entry does not.
    """
    lines = ["{"]
    last  = len(entries) - 1
    for i, e in enumerate(entries):
        comma = "" if i == last else ","
        lines.append(f"    {e.raw_key}: {e.raw_value}{comma}")
    lines.append("}")
    return "\n".join(lines) + "\n"


def _deduplicate(entries: list) -> tuple:
    """Remove duplicate keys, keeping the last occurrence of each.

    Returns:
        (deduped_entries, removed_info) where removed_info is a list of
        (key, dropped_line_numbers, kept_line_number) tuples.
    """
    by_key = OrderedDict()
    for e in entries:
        by_key.setdefault(e.key, []).append(e)

    deduped      = []
    removed_info = []
    for key, group in by_key.items():
        kept = group[-1]
        if len(group) > 1:
            removed_info.append((key, [e.line for e in group[:-1]], kept.line))
        deduped.append(kept)

    return deduped, removed_info


def cmd_fix(args: Args, sort: bool = False) -> int:
    """Fix the file by resolving duplicates and correcting comma placement.

    When *sort* is True the keys are also sorted alphabetically.

    Behaviour:
      - Duplicate keys: the last occurrence is kept; earlier ones are removed.
      - Missing commas: added automatically.
      - Trailing comma on last entry: removed.
      - With --dry-run: prints the result to stdout, nothing is written.
      - With --output: writes to the specified path instead of overwriting.

    Returns 0 on success.
    """
    text    = _load_text(args.file)
    entries = parse_entries(text)

    entries, removed = _deduplicate(entries)
    if sort:
        entries.sort(key=lambda e: e.key)

    new_text = _build_fixed_text(entries)

    # Report structural fixes
    errors = detect_errors(text)
    if errors:
        print(f"Structural errors fixed ({len(errors)}):")
        for e in errors:
            print(f"  {e}")

    # Report duplicate removals
    if removed:
        print(f"Duplicate keys removed ({len(removed)}):")
        for key, dropped, kept in removed:
            print(f"  {key!r}: dropped line(s) {', '.join(str(l) for l in dropped)}, kept line {kept}")
    elif not errors and not sort:
        print("No changes needed.")

    if sort:
        print(f"Keys sorted alphabetically ({len(entries)} entries).")

    if args.dry_run:
        print("\n--- dry run: output not written ---")
        print(new_text)
        return 0

    if new_text == text:
        print("File already clean, nothing to write.")
        return 0

    args.output.write_text(new_text, encoding="utf-8")
    print(f"\nWrote {args.output} ({len(entries)} keys).")
    return 0


def cmd_sort(args: Args) -> int:
    """Sort all keys alphabetically and fix any structural issues.

    Thin wrapper around cmd_fix with sort=True.
    """
    return cmd_fix(args, sort=True)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

_COMMANDS = {
    "errors":     cmd_errors,
    "duplicates": cmd_duplicates,
    "unused":     cmd_unused,
    "fix":        cmd_fix,
    "sort":       cmd_sort,
}


def main():
    args    = Args(sys.argv)
    result  = _COMMANDS[args.command](args)
    sys.exit(0 if not result else 1)


if __name__ == "__main__":
    main()
