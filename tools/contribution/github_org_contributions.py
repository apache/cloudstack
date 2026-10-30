#!/usr/bin/env python3
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
"""
GitHub org activity counters for a set of users (GraphQL).

Counts (scoped to org-owned repos whose name matches --repo-pattern, default ^cloudstack):
- Pull requests created (opened)
- Issues created (opened)
- Pull request reviews

Time window:
- If --from is NOT provided → defaults to the year before --to
- Windows longer than a year are split into yearly chunks (GitHub limitation) and summed
- A date-only --to (YYYY-MM-DD) includes that whole day

Auth:
  export GITHUB_TOKEN=ghp_...

Examples:
  python3 github_org_contributions.py --org apache --users octocat
  python3 github_org_contributions.py --org apache --users octocat monalisa --from 2025-02-01 --to 2026-01-31

See README.md in this directory for details and caveats.
"""

import argparse
import csv
import datetime as dt
import os
import re
import sys
from typing import Any, Dict, Iterable, List, Tuple

import requests

GQL_ENDPOINT = "https://api.github.com/graphql"
# GitHub's upper limit for the *ContributionsByRepository fields
MAX_REPOSITORIES = 100

COUNTERS = {
    "prs_created": "pullRequestContributionsByRepository",
    "issues_created": "issueContributionsByRepository",
    "pr_reviews": "pullRequestReviewContributionsByRepository",
}


def utc_now() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


def iso_to_datetime(s: str, end_of_day: bool = False) -> dt.datetime:
    if len(s) == 10:  # YYYY-MM-DD
        d = dt.datetime.fromisoformat(s).replace(tzinfo=dt.timezone.utc)
        return d + dt.timedelta(days=1) if end_of_day else d
    if s.endswith(("Z", "z")):  # fromisoformat() only accepts "Z" from Python 3.11
        s = s[:-1] + "+00:00"
    d = dt.datetime.fromisoformat(s)
    if d.tzinfo is None:
        d = d.replace(tzinfo=dt.timezone.utc)
    return d.astimezone(dt.timezone.utc)


def shift_years(d: dt.datetime, years: int) -> dt.datetime:
    try:
        return d.replace(year=d.year + years)
    except ValueError:  # Feb 29
        return d.replace(year=d.year + years, day=28)


def year_chunks(from_dt: dt.datetime, to_dt: dt.datetime) -> Iterable[Tuple[dt.datetime, dt.datetime]]:
    """
    Split [from_dt, to_dt) into consecutive end-exclusive windows of at most one year.
    """
    start = from_dt
    while start < to_dt:
        end = min(shift_years(start, 1), to_dt)
        yield start, end
        start = end


def graphql(token: str, query: str, variables: Dict[str, Any]) -> Dict[str, Any]:
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
    }
    r = requests.post(GQL_ENDPOINT, json={"query": query, "variables": variables}, headers=headers, timeout=60)
    r.raise_for_status()
    payload = r.json()
    if payload.get("errors"):
        msgs = "; ".join(e.get("message", "Unknown error") for e in payload["errors"])
        raise RuntimeError(f"GraphQL error(s): {msgs}")
    return payload["data"]


ORG_ID_QUERY = """
query($org: String!) {
  organization(login: $org) { id login }
}
"""

USER_CONTRIB_QUERY = """
query($login: String!, $orgId: ID!, $from: DateTime!, $to: DateTime!, $maxRepos: Int!) {
  user(login: $login) {
    login
    contributionsCollection(organizationID: $orgId, from: $from, to: $to) {
      pullRequestContributionsByRepository(maxRepositories: $maxRepos) {
        repository { name owner { login } }
        contributions { totalCount }
      }
      issueContributionsByRepository(maxRepositories: $maxRepos) {
        repository { name owner { login } }
        contributions { totalCount }
      }
      pullRequestReviewContributionsByRepository(maxRepositories: $maxRepos) {
        repository { name owner { login } }
        contributions { totalCount }
      }
    }
  }
}
"""


def count_user(token: str, login: str, org_id: str, org_login: str, repo_rx: re.Pattern,
               from_dt: dt.datetime, to_dt: dt.datetime) -> Dict[str, Any]:
    """
    Sum the user's contributions to matching org repos over all yearly chunks of the window.
    """
    counts = {k: 0 for k in COUNTERS}
    repos = set()
    user_login = login
    for start, end in year_chunks(from_dt, to_dt):
        data = graphql(token, USER_CONTRIB_QUERY, {
            "login": login,
            "orgId": org_id,
            "from": start.isoformat(),
            "to": end.isoformat(),
            "maxRepos": MAX_REPOSITORIES,
        })
        user = data.get("user")
        if not user:
            raise RuntimeError(f"User not found: {login}")
        user_login = user["login"]

        cc = user["contributionsCollection"]
        for key, field in COUNTERS.items():
            entries = cc[field]
            if len(entries) >= MAX_REPOSITORIES:
                print(f"WARN: {login}: {field} hit the {MAX_REPOSITORIES} repository limit for "
                      f"{start.date()} -> {end.date()}; counts may be incomplete", file=sys.stderr)
            for e in entries:
                repo = e["repository"]
                if repo["owner"]["login"].lower() != org_login.lower() or not repo_rx.search(repo["name"]):
                    continue
                counts[key] += e["contributions"]["totalCount"]
                repos.add(repo["name"])

    return {"user": user_login, **counts, "repos": " ".join(sorted(repos)), "error": ""}


def main() -> int:
    ap = argparse.ArgumentParser(description="Count PRs, issues, and PR reviews for GitHub users within an org.")
    ap.add_argument("--org", required=True, help="GitHub organization login (e.g. apache)")
    ap.add_argument("--users", nargs="+", required=True, help="GitHub usernames")
    ap.add_argument("--repo-pattern", default="^cloudstack",
                    help="Regex matched against repo names; only matching repos are counted (default: ^cloudstack). "
                         "Use '.' to count all org repos")
    ap.add_argument("--from", dest="from_", default=None,
                    help="Start date (YYYY-MM-DD or ISO). Default: one year before --to")
    ap.add_argument("--to", default=None,
                    help="End date (YYYY-MM-DD, inclusive, or ISO). Default: now (UTC)")
    ap.add_argument("--csv", default=None, help="Optional CSV output path")
    args = ap.parse_args()

    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        print("ERROR: GITHUB_TOKEN is not set", file=sys.stderr)
        return 2

    try:
        repo_rx = re.compile(args.repo_pattern, re.IGNORECASE)
    except re.error as e:
        print(f"ERROR: invalid --repo-pattern: {e}", file=sys.stderr)
        return 2

    try:
        to_dt = iso_to_datetime(args.to, end_of_day=True) if args.to else utc_now()
        from_dt = iso_to_datetime(args.from_) if args.from_ else shift_years(to_dt, -1)
    except ValueError as e:
        print(f"ERROR: invalid date: {e}", file=sys.stderr)
        return 2

    if to_dt <= from_dt:
        print("ERROR: --to must be after --from", file=sys.stderr)
        return 2

    try:
        org = graphql(token, ORG_ID_QUERY, {"org": args.org}).get("organization")
    except Exception as e:
        print(f"ERROR: failed to look up organization '{args.org}': {e}", file=sys.stderr)
        return 2
    if not org:
        print(f"ERROR: Organization '{args.org}' not found or inaccessible", file=sys.stderr)
        return 2

    rows: List[Dict[str, Any]] = []

    for u in args.users:
        try:
            rows.append(count_user(token, u, org["id"], org["login"], repo_rx, from_dt, to_dt))
        except Exception as e:
            rows.append({"user": u, **{k: None for k in COUNTERS}, "repos": "", "error": str(e)})

    headers = ["user", *COUNTERS, "repos", "error"]

    # Print table
    colw = {h: len(h) for h in headers}
    for r in rows:
        for h in headers:
            colw[h] = max(colw[h], len("" if r[h] is None else str(r[h])))

    def fmt(r):
        return "  ".join(("" if r[h] is None else str(r[h])).ljust(colw[h]) for h in headers).rstrip()

    print(f"Org: {org['login']}  (repos matching: {args.repo_pattern})")
    print(f"Window (UTC): {from_dt.isoformat()}  ->  {to_dt.isoformat()}")
    print(fmt({h: h for h in headers}))
    print("-" * (sum(colw.values()) + 2 * (len(headers) - 1)))
    for r in rows:
        print(fmt(r))

    if args.csv:
        with open(args.csv, "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=headers)
            w.writeheader()
            w.writerows(rows)
        print(f"\nWrote CSV: {args.csv}")

    return 1 if any(r["error"] for r in rows) else 0


if __name__ == "__main__":
    raise SystemExit(main())
