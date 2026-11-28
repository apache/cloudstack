<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 -->

# Pull requests

Branches, reviews and merges. The contribution policy, including forks and
AI-assisted work, is in [CONTRIBUTING.md](../../CONTRIBUTING.md); writing rules
for titles and descriptions are in [writing.md](./writing.md).

## Contents

- [Branches](#branches)
- [Before opening a pull request](#before-opening-a-pull-request)
- [Reviews and merging](#reviews-and-merging)
- [CI results](#ci-results)
- [Updating a pull request](#updating-a-pull-request)
- [Verifying a merge or rebase](#verifying-a-merge-or-rebase)
- [Reporting issues](#reporting-issues)

## Branches

| Change | Target |
| --- | --- |
| Bug fix | The oldest maintained release branch it applies to |
| Feature or behavior change | `main` |

- Committers merge each release branch into the next newer one, and finally into `main`. Open a fix once, against the oldest branch; the merges carry it to the others.
- Features are not backported.
- A change that depends on another PR says "Depends on #N" in its description.

## Before opening a pull request

1. Rebase onto the latest target branch.
2. Build the touched modules and run their unit tests ([building.md](./building.md)).
3. Run `pre-commit run --all-files`.
4. Test the change by hand: the simulator for API behavior, a lab for anything that touches a hypervisor, storage or the network ([test-lab.md](./test-lab.md)).
5. Fill in the PR template, including how it was tested.

## Reviews and merging

- A PR is merged after two approvals, at least one of them with a test report from someone other than the author. Text-only, test-only and CI-only changes are the exception.
- A test report says the version or commit tested, the environment (hypervisor, storage, network type) and what was checked.
- A committer runs the packaging and Trillian (BlueOrangutan) jobs on a PR; ask in the PR if one is needed.

## CI results

- Check whether a failing job is caused by the change: compare it with the same job on a recent push to the base branch. Some simulator smoke tests fail intermittently.
- Authors cannot re-run workflows. Closing and reopening the PR re-runs them; rebasing does too.
- Say in the PR when a failure is unrelated, with a link to the base-branch run that shows it.

## Updating a pull request

1. Fetch the PR's branch from the remote that hosts it; fall back to `pull/<n>/head` only when none does.
2. Rebase onto the latest base branch, or merge it in when the PR's history already uses merges.
3. Check that it still compiles and the relevant unit tests pass.
4. Read all review feedback: inline threads, review summaries and top-level comments, from people and bots.
5. Fix what is right; reply with the reason to what is not.
6. Push with `--force-with-lease` after a rebase.

## Verifying a merge or rebase

A merge with no `<<<<<<<` markers left can still have lost one side's change.

- For each conflicted file, diff the incoming side: `git diff $(git merge-base HEAD MERGE_HEAD) MERGE_HEAD -- <file>`, and confirm that change is in the result.
- After a rebase, `git range-diff <old-base>..<old-head> <new-base>..<new-head>` shows what each commit became.
- Look for call sites not updated after a signature change, and for both versions of a line kept back to back.

## Reporting issues

1. Search existing issues and PRs first.
2. Check that the latest release and `main` are still affected, and read the code at that tag or branch.
3. Pin the CloudStack version, hypervisor and storage in the report.
4. Give steps to reproduce, or a test that fails.

[CONTRIBUTING.md](../../CONTRIBUTING.md) says what an AI-assisted issue or PR includes.
