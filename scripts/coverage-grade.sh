#!/usr/bin/env bash
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
# coverage-grade.sh
#
# Parses the JaCoCo aggregate XML report and outputs an A–F coverage grade.
#
# Usage:
#   ./scripts/coverage-grade.sh [path/to/jacoco.xml]
#
# Exit codes:
#   0 – grade is D or above  (line coverage >= 20%)
#   1 – grade is F            (line coverage <  20%)
#
# Environment variables (optional, used when writing GitHub outputs):
#   GITHUB_OUTPUT       – set automatically by GitHub Actions
#   GITHUB_STEP_SUMMARY – set automatically by GitHub Actions

set -euo pipefail

JACOCO_XML="${1:-client/target/site/jacoco-aggregate/jacoco.xml}"

if [[ ! -f "$JACOCO_XML" ]]; then
  echo "ERROR: JaCoCo report not found at: $JACOCO_XML" >&2
  exit 2
fi

# ---------------------------------------------------------------------------
# Parse LINE and BRANCH counters from the top-level <report> element using
# Python's built-in xml.etree.ElementTree (no extra dependencies needed).
# ---------------------------------------------------------------------------
read -r LINE_COVERED LINE_MISSED BRANCH_COVERED BRANCH_MISSED < <(python3 - "$JACOCO_XML" <<'PYEOF'
import sys, xml.etree.ElementTree as ET

tree = ET.parse(sys.argv[1])
root = tree.getroot()

lc = lm = bc = bm = 0
# Sum counters from all <package> children so we get the true aggregate,
# avoiding any duplicate top-level counter that some JaCoCo versions emit.
for pkg in root.iter('package'):
    for counter in pkg.findall('counter'):
        t = counter.get('type')
        if t == 'LINE':
            lc += int(counter.get('covered', 0))
            lm += int(counter.get('missed',  0))
        elif t == 'BRANCH':
            bc += int(counter.get('covered', 0))
            bm += int(counter.get('missed',  0))

print(lc, lm, bc, bm)
PYEOF
)

# ---------------------------------------------------------------------------
# Compute percentages
# ---------------------------------------------------------------------------
line_total=$(( LINE_COVERED + LINE_MISSED ))
branch_total=$(( BRANCH_COVERED + BRANCH_MISSED ))

if (( line_total == 0 )); then
  echo "ERROR: No LINE counters found in $JACOCO_XML – was the build run with -P quality?" >&2
  exit 2
fi

# Use awk for floating-point arithmetic
LINE_PCT=$(awk "BEGIN { printf \"%.2f\", ($LINE_COVERED / $line_total) * 100 }")

if (( branch_total > 0 )); then
  BRANCH_PCT=$(awk "BEGIN { printf \"%.2f\", ($BRANCH_COVERED / $branch_total) * 100 }")
else
  BRANCH_PCT="N/A"
fi

# ---------------------------------------------------------------------------
# Assign grade based on LINE coverage
#
#  A  ≥ 80%   Excellent
#  B  60–79%  Good
#  C  40–59%  Acceptable
#  D  20–39%  Marginal (meets minimum gate)
#  F  < 20%   Failing
# ---------------------------------------------------------------------------
LINE_INT=$(awk "BEGIN { printf \"%d\", $LINE_PCT }")   # truncate, not round

if   (( LINE_INT >= 80 )); then GRADE="A"; EMOJI="🟢"; LABEL="Excellent"
elif (( LINE_INT >= 60 )); then GRADE="B"; EMOJI="🟡"; LABEL="Good"
elif (( LINE_INT >= 40 )); then GRADE="C"; EMOJI="🟠"; LABEL="Acceptable"
elif (( LINE_INT >= 20 )); then GRADE="D"; EMOJI="🔴"; LABEL="Marginal"
else                             GRADE="F"; EMOJI="⛔"; LABEL="Failing"
fi

# ---------------------------------------------------------------------------
# Human-readable output (always printed to stdout)
# ---------------------------------------------------------------------------
echo "┌─────────────────────────────────────────────────┐"
echo "│          CloudStack Test Coverage Report         │"
echo "├─────────────────────────────────────────────────┤"
printf  "│  Grade        : %s %-5s  %-20s     │\n" "$EMOJI" "$GRADE" "($LABEL)"
printf  "│  Line coverage: %6s%%  (%d / %d lines)%*s│\n" \
        "$LINE_PCT" "$LINE_COVERED" "$line_total" \
        $(( 14 - ${#LINE_COVERED} - ${#line_total} )) " "
if [[ "$BRANCH_PCT" != "N/A" ]]; then
  printf "│  Branch cov.  : %6s%%  (%d / %d branches)%*s│\n" \
         "$BRANCH_PCT" "$BRANCH_COVERED" "$branch_total" \
         $(( 11 - ${#BRANCH_COVERED} - ${#branch_total} )) " "
else
  printf "│  Branch cov.  : N/A (no branch data)           │\n"
fi
echo "└─────────────────────────────────────────────────┘"
echo ""
echo "Grade scale:  A ≥80%  B 60-79%  C 40-59%  D 20-39%  F <20%  (line coverage)"

# ---------------------------------------------------------------------------
# GitHub Actions: write outputs and step summary
# ---------------------------------------------------------------------------
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "coverage_grade=$GRADE"
    echo "coverage_grade_label=$LABEL"
    echo "line_coverage=$LINE_PCT"
    echo "branch_coverage=$BRANCH_PCT"
  } >> "$GITHUB_OUTPUT"
fi

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "## $EMOJI Test Coverage Grade: **$GRADE** — $LABEL"
    echo ""
    echo "| Metric | Covered | Total | Percentage |"
    echo "|--------|---------|-------|------------|"
    echo "| Line coverage | $LINE_COVERED | $line_total | **${LINE_PCT}%** |"
    if [[ "$BRANCH_PCT" != "N/A" ]]; then
      echo "| Branch coverage | $BRANCH_COVERED | $branch_total | **${BRANCH_PCT}%** |"
    fi
    echo ""
    echo "### Grade Scale"
    echo "| Grade | Line Coverage | Meaning |"
    echo "|-------|--------------|---------|"
    echo "| 🟢 A | ≥ 80% | Excellent - this code sleeps well at night 😴 |"
    echo "| 🟡 B | 60-79% | Good - almost there, don't stop now 😉 |"
    echo "| 🟠 C | 40-59% | Acceptable - your code is wearing a seatbelt, but no airbags 😬 |"
    echo "| 🔴 D | 20-39% | Marginal - boldly shipping where no test has gone before 🖖 |"
    echo "| ⛔ F | < 20% | tests? what tests? 🔥 |"
    echo ""
    echo "> Branch coverage is shown as a secondary signal. Grade is based on line coverage."
  } >> "$GITHUB_STEP_SUMMARY"
fi

# ---------------------------------------------------------------------------
# Exit non-zero for grade F so the CI job can be configured to fail
# ---------------------------------------------------------------------------
if [[ "$GRADE" == "F" ]]; then
  echo ""
  echo "⛔  FAIL: Line coverage ${LINE_PCT}% is below the minimum threshold of 20%." >&2
  exit 1
fi

exit 0
