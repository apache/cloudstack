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

# Contribution Evaluation Scripts

Helper scripts to gather activity numbers for a set of contributors, e.g. as
input when discussing committer or PMC nominations. The numbers are only a
rough signal; read the actual contributions before drawing conclusions.

| Script | Data source | What it counts |
|---|---|---|
| `github_org_contributions.py` | GitHub GraphQL API | PRs opened, issues opened and PR reviews per user, in CloudStack repos (by default) of an org |
| `apache_list_mentions.py` | lists.apache.org mbox archives | How often each name (or alias) appears in mailing list message bodies |

## Requirements

- Python 3.7+
- [`requests`](https://pypi.org/project/requests/): `pip install requests`

## github_org_contributions.py

Requires a GitHub token in `GITHUB_TOKEN`. A classic token with no scopes (or a
fine-grained token with public read-only access) is enough for public repos.
If the GitHub CLI is logged in, `export GITHUB_TOKEN=$(gh auth token)` works too.

```bash
export GITHUB_TOKEN=<your-token>
python3 github_org_contributions.py --org apache --users octocat monalisa
python3 github_org_contributions.py --org apache --users octocat \
    --from 2023-01-01 --to 2025-12-31 --csv contributions.csv
```

| Option | Description |
|---|---|
| `--org` | GitHub organization login (required) |
| `--users` | One or more GitHub usernames, space separated (required) |
| `--repo-pattern` | Regex matched (case-insensitively) against repo names; only matching repos are counted. Default: `^cloudstack`. Use `.` for all org repos |
| `--from` | Start, `YYYY-MM-DD` or ISO 8601. Default: one year before `--to` |
| `--to` | End, `YYYY-MM-DD` (inclusive) or ISO 8601. Default: now (UTC) |
| `--csv` | Also write the table to this CSV file |

Notes:

- Only repos owned by `--org` whose name matches `--repo-pattern` are counted.
  The default `^cloudstack` covers `apache/cloudstack`, `cloudstack-documentation`,
  `cloudstack-www`, `cloudstack-cloudmonkey`, `cloudstack-go`,
  `cloudstack-terraform-provider` and the other `cloudstack-*` repos. The `repos`
  column lists the repos that contributed to the counts.
- Dates without a time are in UTC; `--from` starts at the beginning of that day
  and `--to` includes the whole day.
- GitHub limits a query to one year, so longer windows are split into yearly
  chunks and summed.
- The counts are the ones GitHub uses for the contribution graph. Contributions
  to private repos the token cannot see are not counted.
- Exit status is 1 if any user could not be looked up; the error is shown in the
  `error` column.

## apache_list_mentions.py

Counts how often each name (or alias) appears in the bodies of a list's
messages over the last N months, including the current month.

```bash
python3 apache_list_mentions.py --list dev --domain cloudstack.apache.org \
    --months 12 --names names.txt
```

| Option | Description |
|---|---|
| `--list` | List name, e.g. `dev`, `users` (required) |
| `--domain` | List domain, e.g. `cloudstack.apache.org` (required) |
| `--names` | Names file, see below (required) |
| `--months` | Number of months back, including the current one. Default: 12 |
| `--cache-dir` | Where downloaded archives are kept. Default: `~/.cache/apache-list-mentions` |
| `--include-quotes` | Also count lines starting with `>` (quoted replies) |
| `--include-signature` | Also count text after a `-- ` signature separator |

Names file format, one person per line, results are printed in the same order:

```text
# Lines starting with # are comments
Alex Roe
Jane Doe: Jane, J. Doe
John Smith: John, JS | john@example.org
```

- The part before `:` is the name shown in the output; it is always matched
  as well as the comma separated aliases after it.
- Anything after `|` (e.g. email addresses) is currently ignored.
- Matching is case-insensitive, on whole words, and any whitespace may separate
  the words of a name.

Notes:

- The counts lean towards overcounting rather than undercounting: every mention
  in a message body counts, including a person's sign-offs in their own
  messages, and short or common aliases match wherever they appear.
- Archives of past months are downloaded once and reused from the cache. The
  current month is downloaded again on every run.
- The cache holds full copies of the list archives, so its default location is
  outside the source tree to keep them from being committed by accident.
