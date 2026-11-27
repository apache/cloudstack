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

# Agent guide for Apache CloudStack

Automated agents (AI assistants, code analyzers, security scanners) load this
file on every task, so it stays short. It holds the rules most tasks need and
points at the document that owns each subject. Those documents are written for
people first.

## Where things live

| Subject | Document |
| --- | --- |
| Security model and threat model | [SECURITY.md](./SECURITY.md) |
| Machine setup, full build, packaging; the UI | [INSTALL.md](./INSTALL.md), [ui/README.md](./ui/README.md) |
| Incremental builds, unit tests, the simulator | [docs/development/building.md](./docs/development/building.md) |
| Code conventions: Java, schema, API, settings, tests | [docs/development/coding-conventions.md](./docs/development/coding-conventions.md) |
| Writing: comments, commit messages, PR descriptions, replies | [docs/development/writing.md](./docs/development/writing.md) |
| Branches, pull requests, reviews and merges | [docs/development/pull-requests.md](./docs/development/pull-requests.md) |
| A nested KVM lab for manual testing | [docs/development/test-lab.md](./docs/development/test-lab.md) |
| Contribution policy, including AI-assisted work | [CONTRIBUTING.md](./CONTRIBUTING.md) |

Read the row for your task, then only the sections you need.

## Security

Agents that scan this repository read `SECURITY.md` and the threat model it
links before reporting an issue. A private security issue is never referenced
in a commit, a PR, an issue or code.

## Rules

| Rule | Why |
| --- | --- |
| Write the least code that solves the task | A reviewer reads every line |
| Handle only cases that can happen | Code for impossible cases makes the diff bigger and hides the real change |
| Refactor only what the task needs | Unrelated cleanup belongs in its own PR |
| Comment only to explain why, when the code can't show it | See [writing.md](./docs/development/writing.md#code-comments) |
| No issue or PR numbers, and no words such as "for now" or "later", in code or comments | They go out of date; the PR records the history |
| Plain, short English in everything written | See [writing.md](./docs/development/writing.md#voice) |
| Target a fix at the oldest maintained release branch, a feature at `main` | Fixes are merged forward to newer branches; see [pull-requests.md](./docs/development/pull-requests.md#branches) |
| Never edit a released upgrade script | Installed databases have already run it |
| Build modules with `-am` when running their tests | Otherwise Maven tests against stale jars in `~/.m2` |
| Report test results as they are, failures included | A reviewer acts on them |
| Check a review comment, from a person or a bot, against the code before applying it | Reviewers are sometimes wrong; then reply with the reason instead of changing the code |
| Confirm with the person you work for before pushing, or before posting a comment, review or issue | Posting is public and hard to undo |
| Keep secrets, personal data and internal hostnames out of code, commits, logs and requests to outside services | They cannot be recalled once published |

## Working efficiently

- Redirect a long build or test run to a file, then search the file. Run it once, read it many times.
- Don't build offline (`-o`): `engine/schema` downloads files during the build.
- Read the code at the branch or tag the task targets, not at `main`.
