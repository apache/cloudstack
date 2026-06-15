---
on:
  schedule: daily around 14:00 on weekdays
  workflow_dispatch: null
permissions:
  issues: read
imports:
- github/gh-aw/.github/workflows/shared/reporting.md@359795d49ada21681ab616bd4cbcb144a7387115
- github/gh-aw/.github/workflows/shared/noop-reminder.md@359795d49ada21681ab616bd4cbcb144a7387115
safe-outputs:
  add-comment: {}
  add-labels:
    allowed:
    - bug
    - feature
    - enhancement
    - documentation
    - question
    - help-wanted
    - good-first-issue
emoji: 🔧
source: github/gh-aw/.github/workflows/issue-triage-agent.md@359795d49ada21681ab616bd4cbcb144a7387115
strict: true
timeout-minutes: 5
tools:
  cli-proxy: true
  github:
    toolsets:
    - issues
    - labels
---
# Issue Triage Agent

List open issues in ${{ github.repository }} that have no labels. For each unlabeled issue, analyze the title and body, then add one of the allowed labels: `bug`, `feature`, `enhancement`, `documentation`, `question`, `help-wanted`, or `good-first-issue`.

Skip issues that:
- Already have any of these labels
- Have been assigned to any user (especially non-bot users)

After adding the label to an issue, mention the issue author in a comment using this format (follow shared/reporting.md guidelines):

**Comment Template**:
```markdown
### 🏷️ Issue Triaged

Hi @{author}! I've categorized this issue as **{label_name}** based on the following analysis:

**Reasoning**: {brief_explanation_of_why_this_label}

<details>
<summary>View Triage Details</summary>

#### Analysis
- **Keywords detected**: {list_of_keywords_that_matched}
- **Issue type indicators**: {what_made_this_fit_the_category}
- **Confidence**: {High/Medium/Low}

#### Recommended Next Steps
- {context_specific_suggestion_1}
- {context_specific_suggestion_2}

</details>

**References**: [Triage run §{run_id}](https://github.com/github/gh-aw/actions/runs/{run_id})
```

**Key formatting requirements**:
- Use h3 (###) for the main heading
- Keep reasoning visible for quick understanding
- Wrap detailed analysis in `<details>` tags
- Include workflow run reference
- Keep total comment concise (collapsed details prevent noise)

## Batch Comment Optimization

For efficiency, if multiple issues are triaged in a single run:
1. Add individual labels to each issue
2. Add a brief comment to each issue (using the template above)
3. Optionally: Create a discussion summarizing all triage actions for that run

This provides both per-issue context and batch visibility.

## Labels

- `bug`: Indicates a problem or error in the code that needs fixing.
- `feature`: Represents a new feature request or enhancement to existing functionality.
- `enhancement`: Suggests improvements to existing features or code.
- `documentation`: Pertains to issues related to documentation, such as missing or unclear docs.
- `question`: Used for issues that are asking for clarification or have questions about the project.
- `help-wanted`: Indicates that the issue is a good candidate for external contributions and help
- `good-first-issue`: Marks issues that are suitable for newcomers to the project, often with simpler scope.
