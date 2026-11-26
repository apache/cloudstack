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

# Agent Guide for Apache CloudStack

This file is read by automated agents (security scanners, code
analyzers, AI assistants) operating on this repository.

## Security

Security model: [SECURITY.md](./SECURITY.md)

Agents that scan this repository should consult `SECURITY.md` and the
threat model it links before reporting issues.

The project-wide security threat model is linked from `SECURITY.md`.

## Repository layout

| Path             | Contents                                        |
|------------------|-------------------------------------------------|
| `api/`           | API interfaces, `*Cmd` and `*Response` classes  |
| `server/`        | Manager implementations (business logic)        |
| `engine/schema/` | VO and DAO classes, DB schema and upgrade SQL   |
| `framework/`     | Config, DB, security, async jobs                |
| `plugins/`       | Hypervisors, storage, network, extensions       |
| `core/`          | Shared DTOs, agent `Command`/`Answer` classes   |
| `ui/`            | Vue.js 3 + Ant Design frontend                  |
| `client/`        | Uber-jar; include it in every incremental build |
| `test/`          | Marvin integration tests                        |

## Build and test

```bash
# Full build (slow)
mvn clean install -Dnoredist -P developer,systemvm -DskipTests -T8
# Incremental build (slow)
mvn clean install -Dnoredist -P developer,systemvm -pl api,server,engine/schema,client -DskipTests
# Deploy DB (fast)
mvn -Pdeveloper -pl developer -Ddeploydb
# Run the management server
mvn -pl :cloud-client-ui jetty:run -Dnoredist -Djava.net.preferIPv4Stack=true
# Single unit test (fast)
mvn test -pl server -Dtest=FooManagerImplTest
# Lint and license checks
pre-commit run --all-files
```

- Always include `client` in an incremental build.
- In a linked `git worktree`, add `-Dmaven.gitcommitid.skip=true`.
- Don't build offline (`-o`); `engine/schema` downloads files.
- Run `pre-commit` before opening a PR.
- Report test results as they are, including failures.

## Code conventions

- Add the Apache License 2.0 header to every new file.
- Name VO getters and setters exactly after their fields.
- Use `get`, not `is`, for boolean getters.
- Give VO classes an empty constructor.
- Generate the UUID in the VO constructor.
- Use singular table names.
- Don't redeclare inherited parameters.
- Add global settings as `ConfigKey`s, not as `Config` enum entries.
- Declare a `ConfigKey` on the service interface, not on the `*Impl`.
- Touch as few interfaces as possible when adding settings.
- Put schema changes in the versioned upgrade path under `engine/schema`.
- Never edit an already released upgrade script.

## Keep it reviewable

- Write the minimum code that solves the task.
- Don't handle code paths that cannot happen.
- Don't refactor code the task doesn't need, unless this reduces the code, is backwareds compatible and does not obscure the change.
- Deduplicate and reduce the code before submitting, without losing readability.
- Use plain, simple English in code, comments, commits and PR text.
- Leave out text that adds no information.
- Don't write comments that repeat what the code says.
- Don't write inline comments.
- Factor an unclear block out into a method and explain it in that method's comment.
- Never reference issue, PR numbers, or numbers from outside ticketing systems in code or comments.

## Branches and targets

- Target bug fixes at the oldest maintained release branch.
- Merge fixes forward: older release, newer release, `main`.
- Target features at `main`.
- Don't backport features.
- Name branches `ghi<issue>-<short-description>`.
- Name branches without an issue `<short-description>`.
- Don't use `fix/` or `feature/` prefixes.
- Port commits with `git rebase --onto <base> <first>^ <branch>`, not `cherry-pick`.

## Commits and pull requests

- Agents don't stage, commit or push; the human does.
- When done, show the diff and suggest a commit message.
- Write one-line commit messages.
- Don't post comments, reviews or issues without asking; draft them.
- Fill in `PULL_REQUEST_TEMPLATE.md`.
- Describe in the PR how the change was tested by hand.
- Say in the PR that AI was used.
- "Depends on #N" is fine in a PR description.
- Don't put stack positions ("2/5", numbered stack lists) in PR titles or descriptions.
- Never reference a private security issue in commits or code.

## Updating a pull request

1. Fetch the PR's head branch from the remote that hosts it.
2. Fall back to `pull/<n>/head` only when no remote matches.
3. Rebase onto the latest base branch.
4. If conflicts block the rebase without committing, leave the result as working-tree changes and say so.
5. Check that it still compiles.
6. Check whether CI failures come from the PR.
7. Read all review feedback: bots and humans, inline and top-level.
8. Fix the actionable items.
9. Draft replies for the rest.
10. Run the relevant unit tests.

## Verifying merges

- Don't rely on the absence of `<<<<<<<` markers.
- For each conflicted file, diff the incoming side: `git diff $(git merge-base HEAD MERGE_HEAD) MERGE_HEAD -- <file>`.
- Confirm that change is in the result.
- Look for call sites not updated after a signature change.
- Look for both versions of a line kept back to back.

## Reporting issues

- Search existing issues and PRs first.
- Check that the latest release and `main` are still affected.
- Pin the CloudStack version.
- Read code and docs at the matching tag or branch.
- Include documentation for AI-assisted reports.
- Include tests or a reproducer for AI-assisted reports.
- Include suggested code for AI-assisted reports.
- Test AI output by hand before submitting.
- Say that AI was used.
- Never infer AI use from someone's writing style.

## Secrets

- Never type or inject secrets into anything that echoes them.
- Verify passwords by comparing hashes.
- Keep credentials out of code, commits and issues.
- Keep personal data and internal hostnames out of requests to outside services.
