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

The code-aware commands (unused, missing-keys, placeholder-gaps) need a Java
source tree to cross-reference against. The JSON file given via --file can be
anywhere, including a live/deployed error-messages.json; what matters is that
--source points at a checkout of matching CloudStack source (e.g. an operator
auditing a deployed file can check out the matching release tag separately
and point --source at it).

COMMANDS
--------
  errors            Report structural problems: missing/trailing commas,
                    unexpected lines, missing opening/closing braces.

  duplicates        List every key that appears more than once, together with
                    the line numbers of all occurrences.

  unused            Walk a source tree (or scan a single file) and report keys
                    that are never referenced as a string literal in any
                    .java, .groovy, .kt, .xml, or .py file.

  missing-keys      The inverse of 'unused': scan Exceptions.*(...) call sites
                    under --source and report keys referenced in code that
                    have no entry in the JSON file at all (typo'd or never
                    added). These resolve to the raw key at runtime instead of
                    a message.

  placeholder-gaps  Cross-reference each key's {{placeholder}} tokens against
                    the metadata keys supplied at its Exceptions.*(...) call
                    site(s) under --source, and report placeholders with no
                    matching metadata (these are left in the rendered message
                    verbatim, e.g. "... is already the default for the
                    {{instance}}."). Best-effort: metadata built indirectly
                    (e.g. via CallContext.putErrorContextParameters elsewhere
                    in the code) can't be traced statically and is reported
                    separately as "could not be resolved" rather than as a
                    confirmed gap; check those by hand.

  fix               Produce a syntactically correct file by:
                        - removing duplicate keys (the last occurrence is kept)
                        - adding missing commas between entries
                        - removing trailing commas from the last entry
                    Overwrites the input file unless --output is given.
                    Use --dry-run to preview changes without writing.

  sort              Like fix, but also sorts all keys alphabetically.

USAGE
-----
  error_messages_tool.py <command> [options]

OPTIONS
-------
  -f, --file PATH    Path to the error messages JSON file (required).
  -s, --source PATH  File or directory to scan for 'unused', 'missing-keys',
                     and 'placeholder-gaps'. A directory is walked recursively;
                     a single file (e.g. one Java class) is scanned on its own
                     so you don't have to scan the whole tree.
                     Default: current working directory.
  -o, --output PATH  Destination file for 'fix' and 'sort'.
                     Default: overwrites the input file in-place.
  -v, --verbose      'unused': also print the files that reference each used key.
  --dry-run          'fix'/'sort': print the result to stdout instead of writing.
  -h, --help         Show this message and exit.

EXAMPLES
--------
  # Check for structural errors
  error_messages_tool.py errors -f client/conf/messages/error-messages.json.in

  # Find keys with no code references (run from the repository root)
  error_messages_tool.py unused -f client/conf/messages/error-messages.json.in

  # Find keys referenced in one class that don't exist in the JSON file
  error_messages_tool.py missing-keys -f client/conf/messages/error-messages.json.in \\
      -s server/src/main/java/com/cloud/vm/UserVmManagerImpl.java

  # Find call sites whose metadata doesn't cover all of the key's placeholders
  error_messages_tool.py placeholder-gaps -f client/conf/messages/error-messages.json.in \\
      -s server/src/main/java/com/cloud/vm/

  # Preview what 'fix' would produce without writing
  error_messages_tool.py fix -f client/conf/messages/error-messages.json.in --dry-run

  # Fix and write to a separate file
  error_messages_tool.py fix -f client/conf/messages/error-messages.json.in -o /tmp/fixed.json

  # Sort keys alphabetically and overwrite in place
  error_messages_tool.py sort -f client/conf/messages/error-messages.json.in

EXIT STATUS
-----------
  0  No issues found / fix written successfully.
  1  Issues found (errors, duplicates, unused/missing keys, placeholder gaps)
     or fix was a no-op.
"""

import os
import re
import sys
from collections import OrderedDict
from pathlib import Path

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

# File extensions searched by the 'unused', 'missing-keys', and
# 'placeholder-gaps' commands.
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
        self.source   = Path.cwd()
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
            elif arg in ("-s", "--source"):
                self.source = Path(next(it))
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
        valid = ("unused", "missing-keys", "placeholder-gaps", "duplicates", "errors", "fix", "sort")
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


def _collect_source_files(source: Path) -> list:
    """Return the files to scan for *source*.

    If *source* is a single file, it is scanned on its own regardless of
    extension. If it's a directory, it's walked recursively and filtered to
    SOURCE_EXTENSIONS, skipping EXCLUDE_DIRS.
    """
    if source.is_file():
        return [source]
    if not source.is_dir():
        _die(f"Source path not found: {source}. Use -s/--source to point at a directory or file.")

    files = []
    for dirpath, dirnames, filenames in os.walk(source):
        dirnames[:] = [d for d in dirnames if d not in EXCLUDE_DIRS]
        for name in filenames:
            if Path(name).suffix in SOURCE_EXTENSIONS:
                files.append(Path(dirpath) / name)
    return files


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
# JSON parsing
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
    """Strip surrounding double-quotes and decode JSON/Java escape sequences."""
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
# Java call-site scanning (shared by unused / missing-keys / placeholder-gaps)
# ---------------------------------------------------------------------------

# A key, by convention, is lowercase dot-separated segments, e.g.
# "vm.deploy.template.not.found". Used to pick the key literal out of a call's
# argument list regardless of its position (serverApiException's overloads put
# an ApiErrorCode and/or a free-text message before it).
_KEY_SHAPE_RE = re.compile(r'^[a-z][a-z0-9]*(?:\.[a-z0-9]+)+$')

_STRING_LITERAL_RE = re.compile(r'"(?:[^"\\]|\\.)*"')
_FULL_STRING_RE     = re.compile(r'^"(?:[^"\\]|\\.)*"$')
_IDENTIFIER_RE      = re.compile(r'^[A-Za-z_]\w*$')
_PLACEHOLDER_RE     = re.compile(r'\{\{(\w+)\}\}')

_EXCEPTIONS_CALL_RE = re.compile(r'\bExceptions\.(\w+)\s*\(')

# Inline metadata map constructors we know how to pull literal keys out of.
_MAP_OF_PREFIXES = ("Map.of(", "ImmutableMap.of(", "Collections.singletonMap(")


def split_top_level(s: str) -> list:
    """Split *s* on commas that are not nested inside (), [], {}, or a string.

    This is what lets us tell apart the top-level arguments of a call like
    ``"key", Map.of("a", b, "c", d)`` (two arguments) from the inner
    ``Map.of(...)`` arguments (four), without a real Java parser.
    """
    parts = []
    depth = 0
    in_string = False
    escape = False
    current = []
    for ch in s:
        if in_string:
            current.append(ch)
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
            current.append(ch)
        elif ch in "([{":
            depth += 1
            current.append(ch)
        elif ch in ")]}":
            depth -= 1
            current.append(ch)
        elif ch == "," and depth == 0:
            parts.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
    tail = "".join(current).strip()
    if tail:
        parts.append(tail)
    return parts


def find_exceptions_calls(text: str) -> list:
    """Return (method_name, args_text, line) for every Exceptions.<factory>(...) call in *text*.

    The argument list is extracted by tracking paren depth from the opening
    '(' (ignoring parens inside string literals), so multi-line calls and
    nested calls like ``Map.of(...)`` are handled correctly.
    """
    calls = []
    for m in _EXCEPTIONS_CALL_RE.finditer(text):
        method = m.group(1)
        start  = m.end()  # just after the opening '('
        depth  = 1
        i      = start
        in_string = False
        escape    = False
        while i < len(text) and depth > 0:
            ch = text[i]
            if in_string:
                if escape:
                    escape = False
                elif ch == "\\":
                    escape = True
                elif ch == '"':
                    in_string = False
            elif ch == '"':
                in_string = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
            i += 1
        args_text = text[start:i - 1]
        line = text.count("\n", 0, m.start()) + 1
        calls.append((method, args_text, line))
    return calls


def analyze_call(args_text: str):
    """Split a call's argument list and locate its error-key literal.

    Returns (key, metadata_arg) where key is the unescaped key string (or
    None if no key-shaped literal is found among the top-level arguments),
    and metadata_arg is the raw text of the argument immediately following
    the key (or None if the key is the last argument): this is always where
    metadata lives across every Exceptions.* factory signature.
    """
    top = split_top_level(args_text)
    for idx, arg in enumerate(top):
        if _FULL_STRING_RE.match(arg):
            literal = _unescape_json_string(arg)
            if _KEY_SHAPE_RE.match(literal):
                metadata_arg = top[idx + 1] if idx + 1 < len(top) else None
                return literal, metadata_arg
    return None, None


def resolve_metadata_keys(metadata_arg, file_text: str):
    """Best-effort extraction of the metadata map's keys for one call site.

    Returns (keys, status) where status is one of:
      'empty'    no metadata argument (or an explicitly empty map) - keys={}
      'inline'   an inline Map.of(...)/ImmutableMap.of(...)/
                 Collections.singletonMap(...)/Map.ofEntries(...) literal -
                 keys extracted with confidence
      'variable' metadata is a bare identifier; keys come from a file-wide
                 scan for '<var>.put("key", ...)', which can miss keys set
                 via other means (chained builders, helper methods, etc.)
      'unknown'  metadata is some other expression (a method call, ternary,
                 etc.) that can't be statically resolved at all
    """
    if metadata_arg is None:
        return set(), "empty"

    arg = metadata_arg.strip()

    if arg.startswith("Collections.emptyMap("):
        return set(), "empty"

    if arg.startswith(_MAP_OF_PREFIXES):
        inner = arg[arg.index("(") + 1: arg.rindex(")")]
        inner_parts = split_top_level(inner)
        keys = set()
        for i in range(0, len(inner_parts) - 1, 2):
            if _FULL_STRING_RE.match(inner_parts[i]):
                keys.add(_unescape_json_string(inner_parts[i]))
        return keys, "inline"

    if arg.startswith("Map.ofEntries("):
        inner = arg[arg.index("(") + 1: arg.rindex(")")]
        keys = set()
        for part in split_top_level(inner):
            part = part.strip()
            if part.startswith("Map.entry("):
                entry_inner = split_top_level(part[part.index("(") + 1: part.rindex(")")])
                if entry_inner and _FULL_STRING_RE.match(entry_inner[0]):
                    keys.add(_unescape_json_string(entry_inner[0]))
        return keys, "inline"

    if _IDENTIFIER_RE.match(arg):
        put_re = re.compile(re.escape(arg) + r'\.put\(\s*("(?:[^"\\]|\\.)*")')
        keys = {_unescape_json_string(m.group(1)) for m in put_re.finditer(file_text)}
        return keys, ("variable" if keys else "unknown")

    return set(), "unknown"


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
    """Find keys that have no references in the source under --source.

    Each key is searched as a plain substring across all files whose extension
    is in SOURCE_EXTENSIONS. A key is considered unused only if the literal
    string does not appear anywhere in those files.

    Returns the number of unused keys (0 = all keys referenced).
    """
    text    = _load_text(args.file)
    entries = parse_entries(text)
    keys    = [e.key for e in entries]

    print(f"Loaded {len(keys)} keys from {args.file}", flush=True)

    source_files = _collect_source_files(args.source)
    print(f"Scanning {len(source_files)} source file(s) under {args.source} ...", flush=True)

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


def cmd_missing_keys(args: Args) -> int:
    """Find keys referenced via Exceptions.*(...) under --source that don't
    exist in the JSON file.

    These resolve to the raw key string at runtime (ResponseMessageResolver
    falls back to the key itself when no template is found), so this catches
    both typos (key doesn't match any real entry) and keys that were simply
    never added.

    Returns the number of distinct missing keys (0 = all referenced keys exist).
    """
    text        = _load_text(args.file)
    entries     = parse_entries(text)
    known_keys  = {e.key for e in entries}

    print(f"Loaded {len(known_keys)} keys from {args.file}", flush=True)

    source_files = _collect_source_files(args.source)
    print(f"Scanning {len(source_files)} source file(s) under {args.source} ...", flush=True)

    occurrences = {}
    for f in source_files:
        try:
            file_text = f.read_text(errors="replace")
        except OSError:
            continue
        for _method, call_args, line in find_exceptions_calls(file_text):
            key, _metadata_arg = analyze_call(call_args)
            if key is not None:
                occurrences.setdefault(key, []).append((f, line))

    missing = {k: v for k, v in occurrences.items() if k not in known_keys}

    print()
    if missing:
        count = sum(len(v) for v in missing.values())
        print(f"Keys referenced in code but missing from {args.file} "
              f"({len(missing)} key(s), {count} call site(s)):")
        for key in sorted(missing):
            for f, line in missing[key]:
                print(f"  {key}  ({f}:{line})")
    else:
        print("All referenced keys exist in the JSON file.")

    return len(missing)


def cmd_placeholder_gaps(args: Args) -> int:
    """Find Exceptions.*(...) call sites whose metadata doesn't cover all of
    the key's {{placeholder}} tokens.

    Only call sites for keys that actually have placeholders are analyzed.
    Call sites whose metadata argument can't be statically resolved (status
    'unknown' from resolve_metadata_keys) are reported separately rather than
    treated as confirmed gaps, since the metadata may be supplied indirectly
    (e.g. CallContext.putErrorContextParameters elsewhere in the method).

    A call site is also checked against its '.admin' variant, if one exists in
    the JSON file, using the same metadata: '.admin'-suffixed keys are never
    referenced directly in Java (ResponseMessageResolver selects them
    automatically for a root admin caller), so without this a template that
    only adds a placeholder in its '.admin' variant would never be checked.

    Returns the number of call sites with a confirmed gap (0 = none found;
    this does not count 'could not be resolved' call sites).
    """
    text    = _load_text(args.file)
    entries = parse_entries(text)

    templates = {}
    for e in entries:
        try:
            value = _unescape_json_string(e.raw_value)
        except Exception:
            value = e.raw_value[1:-1]
        placeholders = set(_PLACEHOLDER_RE.findall(value))
        if placeholders:
            templates[e.key] = placeholders

    print(f"Loaded {len(templates)} key(s) with placeholders "
          f"(out of {len(entries)} total) from {args.file}", flush=True)

    source_files = _collect_source_files(args.source)
    print(f"Scanning {len(source_files)} source file(s) under {args.source} ...", flush=True)

    confirmed = []   # (key, file, line, missing_placeholders)
    uncertain = []   # (key, file, line)

    for f in source_files:
        try:
            file_text = f.read_text(errors="replace")
        except OSError:
            continue
        for _method, call_args, line in find_exceptions_calls(file_text):
            key, metadata_arg = analyze_call(call_args)
            if key is None:
                continue
            variants = [v for v in (key, key + ".admin") if v in templates]
            if not variants:
                continue
            meta_keys, status = resolve_metadata_keys(metadata_arg, file_text)
            if status == "unknown":
                uncertain.append((key, f, line))
                continue
            for variant in variants:
                missing = templates[variant] - meta_keys
                if missing:
                    confirmed.append((variant, f, line, sorted(missing)))

    print()
    if confirmed:
        print(f"Call sites with unresolved placeholders ({len(confirmed)}):")
        for key, f, line, missing in confirmed:
            print(f"  {f}:{line}  [{key}]  missing: {', '.join(missing)}")
    else:
        print("No call sites with statically-confirmed missing placeholders.")

    if uncertain:
        print(f"\nCall sites where metadata could not be statically resolved "
              f"({len(uncertain)}), check these by hand:")
        for key, f, line in uncertain:
            print(f"  {f}:{line}  [{key}]")

    return len(confirmed)


def _build_fixed_text(entries: list) -> str:
    """Serialise *entries* as a properly formatted JSON object.

    Produces exactly one entry per line with 4-space indentation. All entries
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
    "errors":            cmd_errors,
    "duplicates":        cmd_duplicates,
    "unused":            cmd_unused,
    "missing-keys":      cmd_missing_keys,
    "placeholder-gaps":  cmd_placeholder_gaps,
    "fix":               cmd_fix,
    "sort":              cmd_sort,
}


def main():
    args    = Args(sys.argv)
    result  = _COMMANDS[args.command](args)
    sys.exit(0 if not result else 1)


if __name__ == "__main__":
    main()
