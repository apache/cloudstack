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

# Contributing a Copilot token for the agentic workflows

This repo runs scheduled [GitHub Agentic Workflows](https://github.github.com/gh-aw/) (the
`*.lock.yml` files compiled from `*.md` in `.github/workflows/`) that drive the GitHub Copilot
CLI. Each run needs a GitHub token from an account with an active Copilot license. So that no
single person's Copilot quota gets burned through, runs rotate day by day across a pool of
volunteer tokens.

If you have a Copilot license and want to help share the load, add your token to the pool.

## What kind of token

- A fine-grained personal access token. Classic PATs don't work with the Copilot CLI.
- Resource owner: your own personal account.
- Permission: Account permissions > "Copilot Requests" > Read. That's the only permission it
  needs, no repo access.
- Your account must have an active Copilot seat.

Create it at <https://github.com/settings/personal-access-tokens/new>. Give it a sensible
expiration; when it lapses the health check (below) will flag it and you can re-add it.

## How to add it

1. Pick a short alias for yourself, e.g. `t1`, `t2`, `vol3`. The alias shows up in workflow logs,
   so keep it non-identifying if you prefer.
2. Add your token as a repository secret named `COPILOT_GITHUB_TOKEN_<alias>`
   (e.g. `COPILOT_GITHUB_TOKEN_t1`). Repo admins do this via
   *Settings > Secrets and variables > Actions > New repository secret*, or:
   ```
   gh secret set COPILOT_GITHUB_TOKEN_t1 --body "github_pat_xxx"
   ```
3. Ask a repo admin to register the alias by appending it to the repository variable
   `GH_AW_COPILOT_TOKEN_NAMES`, which is a JSON array:
   ```
   gh variable set GH_AW_COPILOT_TOKEN_NAMES --body '["t1","t2","t3"]'
   ```
   The workflows can't enumerate secrets, so this variable is the source of truth for the pool.
   A token isn't used until its alias is listed there.

## How rotation works (for maintainers)

Each agent workflow (`daily-repo-status`, `daily-issue-triage`) defines a `pick_copilot_token`
job in its `.md` source. The job has to run outside the agent job because strict mode forbids
reading secrets there. It picks today's alias by day-of-year mod N, checks the token is live
(`GET /user` returns 200, otherwise it moves on to the next candidate) and outputs the chosen
alias. The token value itself never crosses jobs. The two workflows use different
`ROTATION_SLOT`s, which start them half the pool apart so they don't land on the same
volunteer on the same day (with at least two tokens in the pool).

The agent job resolves the secret itself via
`secrets[format('COPILOT_GITHUB_TOKEN_{0}', needs.pick_copilot_token.outputs.name)]` and falls
back to the base `COPILOT_GITHUB_TOKEN` when the pick job outputs an empty name. Keep the base
secret set to one reliable token.

`gh aw compile` doesn't know about this wiring, so after editing the `.md` sources run:

```
gh aw compile && bash .github/scripts/post-compile.sh
```

See the header of `.github/scripts/post-compile.sh` for what it patches.

To check the pool, trigger the "Copilot token health" workflow
(`.github/workflows/copilot-token-health.yml`) from the Actions tab. It prints an HTTP status
code per alias and nothing else, so no account identities end up in logs. Note it can't tell
when a token is live but has used up its monthly Copilot requests.

## Removing a token

Delete the `COPILOT_GITHUB_TOKEN_<alias>` secret and remove `<alias>` from
`GH_AW_COPILOT_TOKEN_NAMES`.
