---
description: |
  This workflow creates daily repo status reports. It gathers recent repository
  activity (issues, PRs, discussions, releases, code changes) and generates
  engaging GitHub issues with productivity insights, community highlights,
  and project recommendations.

on:
  schedule: daily
  workflow_dispatch:

permissions:
  contents: read
  issues: read
  pull-requests: read

network: defaults

engine:
  id: copilot
  model: claude-haiku-4.5

# Rotates the Copilot token across volunteer PATs, see .github/COPILOT_TOKENS.md.
# Strict mode forbids reading secrets in the agent job, so this job picks today's
# token and outputs its alias only; the agent job resolves the secret itself.
# After `gh aw compile`, run `bash .github/scripts/post-compile.sh` to re-wire the
# agent job to this output.
jobs:
  pick_copilot_token:
    runs-on: ubuntu-latest
    outputs:
      name: ${{ steps.pick.outputs.name }}
    steps:
      - name: Compute candidate names by date
        id: names
        env:
          NAMES_JSON: "${{ vars.GH_AW_COPILOT_TOKEN_NAMES }}"
          ROTATION_SLOT: "0"
        run: |
          set -euo pipefail
          NAMES=()
          if [ -n "${NAMES_JSON:-}" ]; then
            mapfile -t NAMES < <(printf '%s' "$NAMES_JSON" | jq -r '.[]')
          fi
          N=${#NAMES[@]}
          K=3   # today's pick plus 2 fallbacks in case it's dead
          if [ "$N" -eq 0 ]; then
            for o in $(seq 0 $((K-1))); do echo "name_$o=" >> "$GITHUB_OUTPUT"; done
            echo "GH_AW_COPILOT_TOKEN_NAMES is empty -> agent will use base COPILOT_GITHUB_TOKEN"
            exit 0
          fi
          DOY=$(date -u +%-j)
          # slot 1 starts half the pool away from slot 0 so the two workflows
          # pick different tokens whenever the pool has at least 2
          START=$(( (DOY - 1 + ROTATION_SLOT * ((N + 1) / 2)) % N ))
          for o in $(seq 0 $((K-1))); do
            i=$(( (START + o) % N ))
            echo "name_$o=${NAMES[$i]}" >> "$GITHUB_OUTPUT"
          done
      - name: Pick first live token name
        id: pick
        env:
          NAME_0: "${{ steps.names.outputs.name_0 }}"
          NAME_1: "${{ steps.names.outputs.name_1 }}"
          NAME_2: "${{ steps.names.outputs.name_2 }}"
          CAND_0: "${{ secrets[format('COPILOT_GITHUB_TOKEN_{0}', steps.names.outputs.name_0)] }}"
          CAND_1: "${{ secrets[format('COPILOT_GITHUB_TOKEN_{0}', steps.names.outputs.name_1)] }}"
          CAND_2: "${{ secrets[format('COPILOT_GITHUB_TOKEN_{0}', steps.names.outputs.name_2)] }}"
          BASE: "${{ secrets.COPILOT_GITHUB_TOKEN }}"
        run: |
          set -euo pipefail
          live() {
            [ -n "$1" ] && [ "$(curl -s -o /dev/null -w '%{http_code}' \
              -H "Authorization: Bearer $1" https://api.github.com/user || echo 000)" = "200" ]
          }
          for pair in "$NAME_0|$CAND_0" "$NAME_1|$CAND_1" "$NAME_2|$CAND_2"; do
            nm="${pair%%|*}"; tok="${pair#*|}"
            if [ -z "$tok" ]; then continue; fi
            echo "::add-mask::$tok"
            if live "$tok"; then
              echo "name=$nm" >> "$GITHUB_OUTPUT"
              echo "Selected rotated token '$nm'"
              exit 0
            fi
          done
          # empty name makes the agent job fall back to the base COPILOT_GITHUB_TOKEN secret
          [ -n "$BASE" ] && echo "::add-mask::$BASE"
          echo "name=" >> "$GITHUB_OUTPUT"
          if live "$BASE"; then echo "Falling back to base COPILOT_GITHUB_TOKEN"; else
            echo "WARNING: no live Copilot token (rotated or base)" >&2; fi

tools:
  github:
    # If in a public repo, setting `lockdown: false` allows
    # reading issues, pull requests and comments from 3rd-parties
    # If in a private repo this has no particular effect.
    lockdown: false
    min-integrity: none # This workflow is allowed to examine and comment on any issues

safe-outputs:
  mentions: false
  allowed-github-references: []
  create-issue:
    title-prefix: "[repo-status] "
    labels: [report, daily-status]
    close-older-issues: true
source: githubnext/agentics/workflows/repo-status.md@main
---

# Repo Status

Create an upbeat daily status report for the repo as a GitHub issue.

## What to include

- Recent repository activity (issues, PRs, discussions, releases, code changes)
- Progress tracking, goal reminders and highlights
- Project status and recommendations
- Actionable next steps for maintainers

## Style

- Be positive, encouraging, and helpful 🌟
- Use emojis moderately for engagement
- Keep it concise - adjust length based on actual activity

## Process

1. Gather recent activity from the repository
2. Study the repository, its issues and its pull requests
3. Create a new GitHub issue with your findings and insights
