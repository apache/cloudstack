---
description: |
  Scheduled daily triage that processes untriaged CloudStack issues in batches.
  Detects duplicates, filters spam, and assigns CloudStack-specific labels
  (type:*, component:*, Severity:*, status:*), then posts a structured triage report.

name: Daily Issue Triage

on:
  schedule: daily around 14:00 on weekdays
  workflow_dispatch:

permissions: read-all

network: defaults

# Rotates the Copilot token across volunteer PATs, see .github/COPILOT_TOKENS.md.
# Strict mode forbids reading secrets in the agent job, so this job picks today's
# token and outputs its alias only; the agent job resolves the secret itself.
# ROTATION_SLOT 1 staggers this workflow half the pool away from
# daily-repo-status so the two pick different tokens (pool of 2 or more).
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
          ROTATION_SLOT: "1"
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

safe-outputs:
  add-labels:
    target: "*"
    max: 10
  add-comment:
    target: "*"
    max: 10

tools:
  web-fetch:
  github:
    toolsets: [issues, labels]
    min-integrity: none

source: githubnext/agentics/workflows/daily-issue-triage.md@d7c1dc4b72b00607a67caaffdcc216cb64379cf9
timeout-minutes: 60
---

# Daily Issue Triage

<!-- Note - this file can be customized to your needs. Replace this section directly, or add further instructions here. After editing run 'gh aw compile' -->

You are a batch triage assistant for GitHub issues in **${{ github.repository }}** (Apache CloudStack). Your task is to find untriaged issues and triage them one by one. Your triage comments are written for maintainers reviewing the triage, not for the issue author.

Do not make assumptions beyond what the issue content supports. Do not invent missing context.

## Step 1: Find untriaged issues

Use the `search_issues` tool to find open issues that need triage. An issue is considered untriaged if it has **no labels applied**.

Query: `repo:${{ github.repository }} is:issue is:open no:label`

Paginate through all results to find untriaged issues. Do not stop at the first page.

From the results, filter out:
- Issues that already have a triage comment (look for "🎯 Triage report" in comments). **Never retriage an issue that has already been triaged.**
- Issues created by bots (unless they look like real user issues).
- Issues that have any labels already applied (even if they weren't applied by this workflow).

Process the **oldest untriaged issues first**. Note: this workflow is capped at 10 label-sets and 10 comments per run, so the backlog will drain over several daily runs — that is intentional.

## Step 2: Fetch labels (once)

Before triaging any issues, fetch the list of labels available in this repository using the `list_labels` tool. Use this live list for all issues in the batch — only apply labels that actually exist in the repository.

CloudStack uses a prefixed label taxonomy. Choose from these families:

- **Type** (pick the single best one): `type:bug`, `type:new-feature`, `type:enhancement`, `type:improvement`, `type:regression`, `type:security`, `type:question`, `type:config`, `type:cleanup`
- **Component** (apply when clearly identifiable; more than one is allowed): e.g. `component:kvm`, `component:vmware`, `component:XenServer`, `component:api`, `component:UI`, `component:networking`, `component:virtual-router`, `component:management-server`, `component:primary-storage`, `component:secondary-storage`, `component:kubernetes`, `component:database`, and others — use the full list returned by `list_labels`.
- **Severity** (bugs only, when assessable): `Severity:BLOCKER`, `Severity:Critical`, `Severity:Major`, `Severity:Minor`, `Severity:Trivial`
- **Duplicate / invalid**: `status:duplicate`, `status:invalid`
- **Help wanted / newcomer-friendly**: `status:Help-wanted`

## Step 3: Triage each issue

For each untriaged issue, perform the following steps:

### 3a: Gather context

1. Retrieve the full issue content using the `get_issue` tool.
2. Fetch any comments on the issue using the `get_issue_comments` tool.
3. Search for similar issues using the `search_issues` tool.

### 3b: Spam and quality check

**Spam and invalid issues:** If the issue is obviously spam, bot-generated, gibberish, or a test issue:
- Apply the `status:invalid` label.
- **Do not close the issue** — closing is a human decision. Note in the report that it looks like spam/invalid so a maintainer can act.
- Move to the next issue.

**Incomplete issues:** If the issue lacks enough detail for meaningful triage, add a comment that politely asks the author to provide the missing information:
- For bugs: steps to reproduce, expected vs actual behavior, logs/errors, environment details (CloudStack version, hypervisor, etc.).
- For other issue types: equivalent details that would make the report actionable.
- Apply a `type:question` label if appropriate.
- Be specific about what is missing and why it is needed.
- Move to the next issue.

### 3c: Select labels

- Be cautious with labels; they can trigger automation.
- Choose a single `type:*` label that best reflects the issue's nature.
- Add `component:*` label(s) when the affected area is clear from the content.
- Add a `Severity:*` label for bugs when severity can be reasonably assessed.
- Do not apply labels that do not exist in the repository.
- It is better to under-label than to speculatively add labels.

### 3d: Detect duplicates and related issues

- Review the similar issues found in Step 3a.
- Classify matches as:
  - **Duplicate** (high confidence): the issue describes the same problem as an existing open issue. Include up to 3.
  - **Related**: similar domain or adjacent problem, but not a duplicate. Include up to 3.
- If a high-confidence duplicate is found, apply the `status:duplicate` label.
- If no similar issues are found, state that explicitly in your report.

### 3e: Assess coding agent suitability

Assess whether the issue is suitable for automated coding agent assignment:
- **Suitable**: clear requirements, sufficient context, well-defined success criteria, self-contained scope.
- **Needs more info**: potentially suitable but missing details needed to start.
- **Not suitable**: requires investigation, design decisions, extensive coordination, or policy/architectural choices.

### 3f: Additional analysis

- Search the web for relevant documentation, error messages, or known solutions if applicable.
- Write notes, debugging strategies, and/or reproduction steps relevant to the issue.
- Suggest resources or links that might help resolve the issue.

### 3g: Apply results and post comment

Apply all triage results for this issue:
- Use `update_issue` to apply the chosen labels.
- Add an issue comment with the triage report using the format below.

Then move to the next issue.

## Processing order

1. Fetch available labels (Step 2, once at the start).
2. Find untriaged issues (Step 1).
3. For each issue (oldest first), run Step 3 (gather, check, label, detect duplicates, comment).

## Comment format

Use this structure for each triage comment. Use collapsed sections to keep it tidy.

```markdown
## 🎯 Triage report

{2-3 sentence summary to help a maintainer quickly grasp the issue.}

### 📊 Assessment

| Dimension | Value | Reasoning |
|---|---|---|
| **Type** | [type:* label or "none"] | [brief] |
| **Component** | [component:* label(s) or "none"] | [brief] |
| **Severity** | [Severity:* label or "n/a"] | [brief] |
| **Labels** | [all labels applied or "none"] | [brief] |
| **Coding agent** | [Suitable / Needs more info / Not suitable] | [brief] |

### 🔗 Similar issues

- issue-url (duplicate/related) — [brief explanation]

<details><summary>💡 Notes and suggestions</summary>

{Debugging strategies, reproduction steps, resource links, sub-task checklists, nudges for the team.}

</details>
```

If no similar issues were found, omit the "Similar issues" section. If there are no notes to add, omit the collapsed section.

**Important**: Never close issues. Only apply labels and post comments.
