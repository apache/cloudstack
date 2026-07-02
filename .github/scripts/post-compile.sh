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

# Re-applies the token round-robin wiring to the gh-aw generated .lock.yml files,
# which `gh aw compile` doesn't know about. Run after every compile:
#
#     gh aw compile && bash .github/scripts/post-compile.sh
#
# Three edits per lock file (see .github/COPILOT_TOKENS.md for the design):
#   - point the agent execute step's COPILOT_GITHUB_TOKEN at the pick_copilot_token
#     job's output, falling back to the base secret
#   - point the agent job's "Redact secrets in logs" step at the same rotated token,
#     so a volunteer token is scrubbed from uploaded artifacts, not just the base one
#   - make the agent job depend on pick_copilot_token, and strip the self-reference
#     gh-aw sometimes adds to pick_copilot_token's own needs (that would be a cycle)
#
# Safe to re-run; a second run is a no-op.

set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

# Kept in an env var so perl doesn't try to interpolate the ${{ }} bits.
export NEWVAL='${{ needs.pick_copilot_token.outputs.name != '"'"''"'"' && secrets[format('"'"'COPILOT_GITHUB_TOKEN_{0}'"'"', needs.pick_copilot_token.outputs.name)] || secrets.COPILOT_GITHUB_TOKEN }}'

FILES=(
  ".github/workflows/daily-repo-status.lock.yml"
  ".github/workflows/daily-issue-triage.lock.yml"
)

fail() { echo "ERROR: $1" >&2; exit 1; }

# Fixes up `needs:` for the agent and pick_copilot_token jobs only; gh-aw adds
# pick_copilot_token to several other jobs' needs and those must stay as-is.
# Reads stdin, writes stdout. gh-aw emits inline needs (needs: foo) for single
# dependencies and block form for lists; inline is fine unless it needs editing,
# in which case "INLINE_NEEDS:<job>" is printed to stderr and the caller bails.
normalise_needs() {
  awk '
    function isjob(l){ return (l ~ /^  [A-Za-z0-9_-]+:[ \t]*$/) }
    BEGIN { job=""; inneeds=0; agentpick=0 }
    {
      line=$0
      if (isjob(line)) {
        if (inneeds && job=="agent" && !agentpick) print "      - pick_copilot_token"
        inneeds=0; agentpick=0
        name=line; sub(/^  /,"",name); sub(/:[ \t]*$/,"",name); job=name
        print line; next
      }
      if (line ~ /^    needs:[ \t]*[^ \t]/) {
        if (job=="agent" && line !~ /pick_copilot_token/) print "INLINE_NEEDS:" job > "/dev/stderr"
        if (job=="pick_copilot_token" && line ~ /pick_copilot_token/) print "INLINE_NEEDS:" job > "/dev/stderr"
        print line; next
      }
      if (line ~ /^    needs:[ \t]*$/) { inneeds=1; agentpick=0; print line; next }
      if (inneeds) {
        if (line ~ /^      - /) {
          item=line; sub(/^      - /,"",item); gsub(/[ \t\r]/,"",item)
          if (job=="pick_copilot_token" && item=="pick_copilot_token") next
          if (job=="agent" && item=="pick_copilot_token") agentpick=1
          print line; next
        } else {
          if (job=="agent" && !agentpick) print "      - pick_copilot_token"
          inneeds=0
          print line; next
        }
      }
      print line
    }
    END { if (inneeds && job=="agent" && !agentpick) print "      - pick_copilot_token" }
  '
}

for f in "${FILES[@]}"; do
  if [ ! -f "$f" ]; then
    echo "WARN: $f not found, run 'gh aw compile' first? Skipping" >&2
    continue
  fi

  # Repoint the agent execute step's token. The anchor is the GH_AW_PHASE: agent env
  # var further down the same env block; the detection job's block has
  # GH_AW_PHASE: detection so it doesn't match and keeps the base token.
  before=$(grep -cF 'COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_GITHUB_TOKEN }}' "$f" || true)
  perl -0pi -e \
    's/^([ \t]*)COPILOT_GITHUB_TOKEN:[ \t]*\$\{\{[ \t]*secrets\.COPILOT_GITHUB_TOKEN[ \t]*\}\}[ \t]*\n(?=(?:[ \t]+[A-Z][A-Za-z0-9_]*:[^\n]*\n)*?[ \t]+GH_AW_PHASE:[ \t]*agent[ \t]*\n)/$1."COPILOT_GITHUB_TOKEN: ".$ENV{NEWVAL}."\n"/me' \
    "$f"
  after=$(grep -cF 'COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_GITHUB_TOKEN }}' "$f" || true)
  removed=$(( before - after ))
  if [ "$removed" -eq 1 ]; then token_edit="applied"
  elif [ "$removed" -eq 0 ] && grep -qE '^[ \t]+COPILOT_GITHUB_TOKEN: \$\{\{ needs\.pick_copilot_token' "$f"; then token_edit="already"
  else fail "$f: execute-step token line not patched as expected (removed=$removed), anchor drifted?"
  fi

  # Repoint the redact step's SECRET_COPILOT_GITHUB_TOKEN the same way; the line is
  # unique to the agent job's "Redact secrets in logs" step.
  before=$(grep -cF 'SECRET_COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_GITHUB_TOKEN }}' "$f" || true)
  if [ "$before" -gt 1 ]; then
    fail "$f: expected at most one redact-step SECRET_COPILOT_GITHUB_TOKEN line, found $before"
  fi
  perl -0pi -e \
    's/^([ \t]*)SECRET_COPILOT_GITHUB_TOKEN:[ \t]*\$\{\{[ \t]*secrets\.COPILOT_GITHUB_TOKEN[ \t]*\}\}[ \t]*$/$1."SECRET_COPILOT_GITHUB_TOKEN: ".$ENV{NEWVAL}/me' \
    "$f"
  after=$(grep -cF 'SECRET_COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_GITHUB_TOKEN }}' "$f" || true)
  if [ "$before" -eq 1 ] && [ "$after" -eq 0 ]; then redact_edit="applied"
  elif [ "$before" -eq 0 ] && grep -qF 'SECRET_COPILOT_GITHUB_TOKEN: ${{ needs.pick_copilot_token.outputs.name' "$f"; then redact_edit="already"
  else fail "$f: redact-step SECRET_COPILOT_GITHUB_TOKEN line not patched (before=$before after=$after)"
  fi

  errf="$(mktemp)"
  normalise_needs < "$f" > "$f.tmp" 2>"$errf"
  if grep -q '^INLINE_NEEDS:' "$errf"; then
    rm -f "$f.tmp"; rm -f "$errf"
    fail "$f: agent/pick_copilot_token have inline 'needs:' that would need editing, update normalise_needs"
  fi
  rm -f "$errf"
  mv "$f.tmp" "$f"

  # sanity checks
  self_refs=$(awk '
    /^  pick_copilot_token:[ \t]*$/{p=1;next}
    /^  [A-Za-z0-9_-]+:[ \t]*$/{p=0}
    p && /^      - pick_copilot_token[ \t]*$/{c++}
    END{print c+0}' "$f")
  [ "$self_refs" -eq 0 ] || fail "$f: pick_copilot_token still self-references (cycle)"
  awk '/^  agent:[ \t]*$/{a=1} /^  [A-Za-z0-9_-]+:[ \t]*$/ && !/agent/{if(a&&!seen)exit 3} a && /^      - pick_copilot_token/{seen=1} END{exit (seen?0:3)}' "$f" \
    || fail "$f: agent job does not depend on pick_copilot_token"
  grep -qF 'Validate COPILOT_GITHUB_TOKEN secret' "$f" || echo "WARN: validate-secret step missing in $f" >&2
  grep -qE '^  pick_copilot_token:$' "$f" || echo "WARN: pick_copilot_token job missing in $f, did compile include the .md jobs: block?" >&2

  echo "$f: token-ref=$token_edit, redact-ref=$redact_edit, needs=normalised (self-refs=0, agent->pick ok)"
done

echo "Done."
