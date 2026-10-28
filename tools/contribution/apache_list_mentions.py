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
Apache mailing list name-mention counter (cached mbox downloads).

What it does:
- Downloads monthly mbox archives from lists.apache.org ONLY if not already cached locally
  (the current, still-incomplete month is always re-downloaded)
- Scans the email bodies (optionally excluding quoted lines and signatures)
- Counts how many times EACH provided name (and its aliases) is mentioned
- Prints counts in the SAME ORDER as your names file (no top-N / no ranking)

Names file format (names.txt):
  # comments allowed
  Canonical Name
  Canonical Name: alias1, alias2
  Canonical Name: alias1, alias2 | email1, email2   (emails optional; currently not used for mention counting)

Example:
  Jane Doe: Jane, JD
  John Smith: John, J. Smith

Usage:
  python3 apache_list_mentions.py --list dev --domain cloudstack.apache.org --months 12 --names names.txt
"""

from __future__ import annotations

import argparse
import datetime as dt
import email
import mailbox
import re
import sys
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

import requests

MBOX_API = "https://lists.apache.org/api/mbox.lua?list={list}&domain={domain}&d={ym}"
DEFAULT_CACHE_DIR = Path.home() / ".cache" / "apache-list-mentions"

# Heuristics for cleaning bodies before counting mentions
QUOTE_LINE_RE = re.compile(r"^\s*>")       # drop quoted lines (reply history)
SIG_CUTOFF_RE = re.compile(r"^\s*--\s*$")  # signature separator

def utc_now() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)

def month_starts(end_utc: dt.datetime, months: int) -> Iterable[dt.date]:
    """
    Yield month starts for the last N months ending at end_utc month (inclusive).
    """
    y, m = end_utc.year, end_utc.month
    for i in range(months):
        mm = m - i
        yy = y
        while mm <= 0:
            mm += 12
            yy -= 1
        yield dt.date(yy, mm, 1)

def download_mbox(list_name: str, domain: str, ym: str, out_dir: Path) -> Path:
    """
    Download a single YYYY-MM mbox archive from lists.apache.org and save to out_dir.
    """
    url = MBOX_API.format(list=list_name, domain=domain, ym=ym)
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{list_name}@{domain}-{ym}.mbox"

    headers = {"User-Agent": "apache-list-mentions/1.0"}
    with requests.get(url, headers=headers, timeout=120, stream=True) as r:
        if r.status_code == 404:
            raise FileNotFoundError(f"No archive found for {ym}")
        r.raise_for_status()

        # Stream to a temp file so an interrupted download is never reused as a cached archive
        tmp_path = out_path.with_suffix(".mbox.part")
        has_content = False
        try:
            with tmp_path.open("wb") as f:
                for chunk in r.iter_content(chunk_size=1024 * 1024):
                    has_content = has_content or bool(chunk.strip())
                    f.write(chunk)
        except BaseException:
            tmp_path.unlink(missing_ok=True)
            raise

    if not has_content:
        tmp_path.unlink(missing_ok=True)
        raise FileNotFoundError(f"No archive found for {ym}")
    tmp_path.replace(out_path)
    return out_path

def strip_html(html: str) -> str:
    """
    Very simple HTML->text stripping (good enough for mailing list bodies).
    """
    html = re.sub(r"(?is)<(script|style).*?>.*?</\1>", " ", html)
    html = re.sub(r"(?is)<.*?>", " ", html)
    html = re.sub(r"&nbsp;|&#160;", " ", html)
    html = re.sub(r"&amp;", "&", html)
    html = re.sub(r"&lt;", "<", html)
    html = re.sub(r"&gt;", ">", html)
    return html

def extract_text(msg: email.message.Message) -> str:
    """
    Prefer text/plain parts; fallback to text/html stripped; ignore attachments.
    """
    if msg.is_multipart():
        plains: List[str] = []
        htmls: List[str] = []
        for part in msg.walk():
            disp = (part.get("Content-Disposition") or "").lower()
            if "attachment" in disp:
                continue

            ctype = part.get_content_type()
            try:
                raw = part.get_payload(decode=True)
                if raw is None:
                    continue
                text = raw.decode(part.get_content_charset() or "utf-8", "replace")
            except Exception:
                continue

            if ctype == "text/plain":
                plains.append(text)
            elif ctype == "text/html":
                htmls.append(text)

        if plains:
            return "\n".join(plains)
        if htmls:
            return strip_html("\n".join(htmls))
        return ""

    # non-multipart
    ctype = msg.get_content_type()
    try:
        raw = msg.get_payload(decode=True)
        if raw is None:
            return ""
        text = raw.decode(msg.get_content_charset() or "utf-8", "replace")
    except Exception:
        return ""

    return strip_html(text) if ctype == "text/html" else text

def normalize_text(text: str, drop_quotes: bool, drop_signature: bool) -> str:
    """
    Remove quoted lines and/or signatures to reduce false mention counts.
    """
    lines = text.splitlines()
    out: List[str] = []
    for ln in lines:
        if drop_signature and SIG_CUTOFF_RE.match(ln):
            break
        if drop_quotes and QUOTE_LINE_RE.match(ln):
            continue
        out.append(ln)
    return "\n".join(out)

def parse_names_file(path: Path) -> Tuple[List[str], Dict[str, List[str]]]:
    """
    Parse names file while preserving order.

    Supports:
      Canonical Name
      Canonical Name: alias1, alias2
      Canonical Name: alias1, alias2 | email1, email2   (emails ignored in this script)

    Returns:
      ordered_canonicals: list of canonical names in file order
      aliases_map: canonical -> [canonical + aliases]
    """
    ordered: List[str] = []
    aliases_map: Dict[str, List[str]] = {}

    for raw in path.read_text(encoding="utf-8").splitlines():
        ln = raw.strip()
        if not ln or ln.startswith("#"):
            continue

        left = ln.split("|", 1)[0].strip()

        if ":" in left:
            canon, aliases = left.split(":", 1)
            canon = canon.strip()
            alias_list = [a.strip() for a in aliases.split(",") if a.strip()]
            variants = [canon] + [a for a in alias_list if a.lower() != canon.lower()]
        else:
            canon = left
            variants = [canon]

        ordered.append(canon)
        aliases_map[canon] = variants

    return ordered, aliases_map

def build_mention_regex(aliases_map: Dict[str, List[str]]) -> Dict[str, re.Pattern]:
    """
    Build case-insensitive regex per canonical name that matches any alias as a whole token.
    """
    out: Dict[str, re.Pattern] = {}
    for canon, variants in aliases_map.items():
        pats: List[str] = []
        for v in variants:
            v = v.strip()
            if not v:
                continue
            v_esc = re.escape(v)
            v_esc = re.sub(r"\\\s+", r"\\s+", v_esc)  # flexible whitespace for multi-word names
            pats.append(rf"(?<!\w){v_esc}(?!\w)")
        out[canon] = re.compile("|".join(pats), re.IGNORECASE) if pats else re.compile(r"(?!x)x")
    return out

def main() -> int:
    ap = argparse.ArgumentParser(description="Count mentions of provided names in Apache mailing list archives.")
    ap.add_argument("--list", required=True, help="List name, e.g. dev, users")
    ap.add_argument("--domain", required=True, help="List domain, e.g. cloudstack.apache.org")
    ap.add_argument("--months", type=int, default=12, help="How many months back (default: 12)")
    ap.add_argument("--cache-dir", default=str(DEFAULT_CACHE_DIR),
                    help=f"Cache directory for downloaded mbox files (default: {DEFAULT_CACHE_DIR})")
    ap.add_argument("--names", required=True, help="Path to names file (canonical names + aliases)")
    ap.add_argument("--include-quotes", action="store_true", help="Include quoted lines in counting (default: exclude)")
    ap.add_argument("--include-signature", action="store_true", help="Include signature in counting (default: exclude)")
    args = ap.parse_args()

    cache_dir = Path(args.cache_dir).expanduser()
    end = utc_now()
    current_ym = f"{end.year:04d}-{end.month:02d}"

    ordered_names, aliases_map = parse_names_file(Path(args.names))
    mention_rx = build_mention_regex(aliases_map)

    mentions = {canon: 0 for canon in ordered_names}

    downloaded = 0
    reused = 0
    missing = 0
    total_msgs = 0

    for d in month_starts(end, args.months):
        ym = f"{d.year:04d}-{d.month:02d}"
        mbox_path = cache_dir / f"{args.list}@{args.domain}-{ym}.mbox"

        try:
            # The current month is still receiving mail, so never trust its cached copy
            if mbox_path.exists() and ym != current_ym:
                reused += 1
            else:
                mbox_path = download_mbox(args.list, args.domain, ym, cache_dir)
                downloaded += 1

            mbox = mailbox.mbox(mbox_path)
            for msg in mbox:
                total_msgs += 1
                body = extract_text(msg)
                body = normalize_text(
                    body,
                    drop_quotes=not args.include_quotes,
                    drop_signature=not args.include_signature,
                )
                for canon in ordered_names:
                    mentions[canon] += len(mention_rx[canon].findall(body))

        except FileNotFoundError:
            missing += 1
        except Exception as e:
            print(f"WARN: {ym}: {e}", file=sys.stderr)

    print(f"List: {args.list}@{args.domain}")
    print(f"Months scanned: {args.months} (missing months: {missing})")
    print(f"Archives reused: {reused}, downloaded this run: {downloaded}")
    print(f"Total messages parsed: {total_msgs}")
    print("")
    print("Name mentions (in file order):")
    width = max((len(n) for n in ordered_names), default=10)
    for canon in ordered_names:
        print(f"{canon:<{width}} : {mentions.get(canon, 0)}")

    return 0

if __name__ == "__main__":
    raise SystemExit(main())
