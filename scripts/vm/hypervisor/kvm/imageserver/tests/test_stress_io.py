# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

"""
Stress IO tests
They run only when IMAGESERVER_STRESS_TEST_QCOW_DIR is set to an existing
directory containing qcow2 files.
"""

import json
import os
import subprocess
import time
import unittest
import uuid
import urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed

from imageserver.constants import MAX_PARALLEL_READS, MAX_PARALLEL_WRITES

from .test_base import get_tmp_dir, http_get, http_post, http_put, make_nbd_transfer_existing_disk


def _allocated_subranges(extents, granularity):
    """Split each non-hole extent (zero=False) into [start, end] inclusive byte ranges."""
    out = []
    for ext in extents:
        if ext.get("zero"):
            continue
        start = int(ext["start"])
        length = int(ext["length"])
        pos = start
        end_abs = start + length
        while pos < end_abs:
            chunk_end = min(pos + granularity, end_abs)
            out.append((pos, chunk_end - 1))
            pos = chunk_end
    return out


def _qemu_img_virtual_size(path: str) -> int:
    """Return virtual size in bytes (requires ``qemu-img`` on PATH)."""
    cp = subprocess.run(
        ["qemu-img", "info", "--output=json", path],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        universal_newlines=True,
        check=True,
    )
    return int(json.loads(cp.stdout)["virtual-size"])


def _http_error_detail(exc: urllib.error.HTTPError) -> str:
    parts = ["HTTP %s %r" % (exc.code, exc.reason), "url=%r" % getattr(exc, "url", "")]
    try:
        if exc.fp is not None:
            raw = exc.fp.read()
            if raw:
                text = raw.decode("utf-8", errors="replace")
                parts.append("response_body=%r" % (text,))
    except Exception as read_err:
        parts.append("read_body_error=%r" % (read_err,))
    return "; ".join(parts)


def _http_get_checked(url, headers=None, expected_status=200, label="GET"):
    try:
        resp = http_get(url, headers=headers)
    except urllib.error.HTTPError as e:
        raise AssertionError("%s failed for %r: %s" % (label, url, _http_error_detail(e))) from e
    if resp.status != expected_status:
        body = resp.read()
        raise AssertionError(
            "%s %r: expected HTTP %s, got %s; body=%r"
            % (label, url, expected_status, resp.status, body)
        )
    return resp


def _http_put_checked(url, data, headers, label="PUT"):
    try:
        resp = http_put(url, data, headers=headers)
    except urllib.error.HTTPError as e:
        raise AssertionError("%s failed for %r: %s" % (label, url, _http_error_detail(e))) from e
    body = resp.read()
    if resp.status != 200:
        raise AssertionError(
            "%s %r: expected HTTP 200, got %s; body=%r" % (label, url, resp.status, body)
        )
    return resp, body


def _http_post_checked(url, data=b"", headers=None, label="POST"):
    try:
        resp = http_post(url, data=data, headers=headers)
    except urllib.error.HTTPError as e:
        raise AssertionError("%s failed for %r: %s" % (label, url, _http_error_detail(e))) from e
    body = resp.read()
    if resp.status != 200:
        raise AssertionError(
            "%s %r: expected HTTP 200, got %s; body=%r" % (label, url, resp.status, body)
        )
    return resp, body


def _list_qcow2_files(dir_path: str):
    entries = []
    for name in os.listdir(dir_path):
        p = os.path.join(dir_path, name)
        if not os.path.isfile(p):
            continue
        # Keep this intentionally permissive; qemu-nbd can still reject invalid files.
        if name.lower().endswith(".qcow2") or name.lower().endswith(".qcow"):
            entries.append(p)
    entries.sort()
    return entries


class TestQcow2ExtentsParallelReads(unittest.TestCase):
    """
    For each qcow2 in IMAGESERVER_STRESS_TEST_QCOW_DIR,
    export it via qemu-nbd, fetch allocation extents, and perform parallel range reads
    over allocated regions. A second test copies allocated extents into a new qcow2
    and validates via qemu-img compare.

    Env:
    - IMAGESERVER_STRESS_TEST_QCOW_DIR: directory containing qcow2 files (required)
    - IMAGESERVER_STRESS_TEST_READ_GRANULARITY: byte step (default 4 MiB)
      (fallback: IMAGESERVER_TEST_QCOW2_READ_GRANULARITY for compatibility)
    """

    def setUp(self):
        super().setUp()
        self._qcow_dir = os.environ.get("IMAGESERVER_STRESS_TEST_QCOW_DIR", "").strip()
        if not self._qcow_dir or not os.path.isdir(self._qcow_dir):
            self.skipTest(
                "Set IMAGESERVER_STRESS_TEST_QCOW_DIR to an existing directory containing qcow2 files"
            )

        self._dest_dir = self._qcow_dir.rstrip(os.sep) + ".test"
        try:
            os.makedirs(self._dest_dir, exist_ok=True)
        except OSError as e:
            self.skipTest("failed to create dest dir %r: %r" % (self._dest_dir, e))

        raw_g = os.environ.get("IMAGESERVER_STRESS_TEST_READ_GRANULARITY", "").strip()
        if not raw_g:
            raw_g = os.environ.get("IMAGESERVER_TEST_QCOW2_READ_GRANULARITY", "").strip()
        self._read_granularity = int(raw_g) if raw_g else 4 * 1024 * 1024
        if self._read_granularity <= 0:
            self.skipTest("IMAGESERVER_STRESS_TEST_READ_GRANULARITY must be positive")

        self._qcow2_files = _list_qcow2_files(self._qcow_dir)
        if not self._qcow2_files:
            self.skipTest("no qcow2 files found in IMAGESERVER_STRESS_TEST_QCOW_DIR")

        # Avoid pathological oversubscription by default; still runs multiple files concurrently.
        cpu = os.cpu_count() or 4
        self._file_workers = max(1, min(len(self._qcow2_files), cpu))

    def test_parallel_range_reads_allocated_extents(self):
        """
        For every qcow2 in the directory: GET /extents then do parallel Range GETs across
        allocated spans. All qcow2 files are processed concurrently.
        """

        def run_one(path: str):
            _, url, server, cleanup = make_nbd_transfer_existing_disk(path, "qcow2")
            try:
                resp = _http_get_checked(
                    "%s/extents" % (url,),
                    expected_status=200,
                    label="GET /extents",
                )
                extents = json.loads(resp.read())
                ranges = _allocated_subranges(extents, self._read_granularity)
                if not ranges:
                    # Not an error; some images can legitimately be all holes.
                    return {"path": path, "ranges": 0, "skipped": True}

                def fetch(span):
                    start_b, end_b = span
                    range_hdr = "bytes=%s-%s" % (start_b, end_b)
                    r = _http_get_checked(
                        url,
                        headers={"Range": range_hdr},
                        expected_status=206,
                        label="Range GET %s" % (range_hdr,),
                    )
                    data = r.read()
                    expected_len = end_b - start_b + 1
                    if len(data) != expected_len:
                        raise AssertionError(
                            "Range GET %s: got %d bytes, expected %d (url=%r, file=%r)"
                            % (range_hdr, len(data), expected_len, url, path)
                        )

                with ThreadPoolExecutor(max_workers=MAX_PARALLEL_READS) as pool:
                    list(pool.map(fetch, ranges))
                return {"path": path, "ranges": len(ranges), "skipped": False}
            finally:
                cleanup()

        started = time.perf_counter()
        results = []
        failures = []
        with ThreadPoolExecutor(max_workers=self._file_workers) as pool:
            futs = {pool.submit(run_one, p): p for p in self._qcow2_files}
            for fut in as_completed(futs):
                p = futs[fut]
                try:
                    results.append(fut.result())
                except Exception as e:
                    failures.append((p, e))

        elapsed = time.perf_counter() - started
        skipped = sum(1 for r in results if r.get("skipped"))
        total_ranges = sum(int(r.get("ranges", 0)) for r in results)
        print(
            "stress_io: test_parallel_range_reads_allocated_extents: files=%d workers=%d skipped=%d total_ranges=%d elapsed=%.3fs"
            % (len(self._qcow2_files), self._file_workers, skipped, total_ranges, elapsed)
        )

        if failures:
            first_path, first_exc = failures[0]
            raise AssertionError(
                "stress_io: %d/%d files failed (first=%r): %r"
                % (len(failures), len(self._qcow2_files), first_path, first_exc)
            ) from first_exc

    def test_parallel_reads_then_put_range_copy_matches_source(self):
        """
        For every qcow2 in the directory: create an empty qcow2 with same virtual size,
        then copy every allocated range using a worker pool (Range GET then Content-Range PUT),
        flush, and validate via qemu-img compare. All qcow2 files are processed concurrently.
        """

        def run_one(src_path: str):
            try:
                vsize = _qemu_img_virtual_size(src_path)
            except (
                FileNotFoundError,
                subprocess.CalledProcessError,
                KeyError,
                json.JSONDecodeError,
                TypeError,
                ValueError,
            ) as e:
                raise AssertionError("qemu-img info failed for %r: %r" % (src_path, e)) from e

            base = os.path.basename(src_path)
            # Keep dest names unique in case the same basename appears more than once.
            dest_name = "%s.copy.%s.qcow2" % (base, uuid.uuid4().hex[:8])
            dest_path = os.path.join(self._dest_dir, dest_name)
            try:
                subprocess.run(
                    ["qemu-img", "create", "-f", "qcow2", dest_path, str(vsize)],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    universal_newlines=True,
                    check=True,
                )
            except (FileNotFoundError, subprocess.CalledProcessError) as e:
                raise AssertionError("qemu-img create failed for %r: %r" % (dest_path, e)) from e

            _, src_url, _, cleanup_src = make_nbd_transfer_existing_disk(src_path, "qcow2")
            _, dest_url, _, cleanup_dest = make_nbd_transfer_existing_disk(dest_path, "qcow2")
            try:
                resp = _http_get_checked(
                    "%s/extents" % (src_url,),
                    expected_status=200,
                    label="GET src /extents",
                )
                extents = json.loads(resp.read())
                ranges = _allocated_subranges(extents, self._read_granularity)
                if not ranges:
                    return {"path": src_path, "ranges": 0, "skipped": True}

                transfer_workers = max(1, min(MAX_PARALLEL_READS, MAX_PARALLEL_WRITES))

                def transfer_span(span):
                    start_b, end_b = span
                    range_hdr = "bytes=%s-%s" % (start_b, end_b)
                    r = _http_get_checked(
                        src_url,
                        headers={"Range": range_hdr},
                        expected_status=206,
                        label="Range GET src %s" % (range_hdr,),
                    )
                    data = r.read()
                    expected_len = end_b - start_b + 1
                    if len(data) != expected_len:
                        raise AssertionError(
                            "Range GET src %s: got %d bytes, expected %d (url=%r, file=%r)"
                            % (range_hdr, len(data), expected_len, src_url, src_path)
                        )
                    end_inclusive = start_b + len(data) - 1
                    cr = "bytes %s-%s/*" % (start_b, end_inclusive)
                    _put_resp, put_body = _http_put_checked(
                        dest_url,
                        data,
                        headers={
                            "Content-Range": cr,
                            "Content-Length": str(len(data)),
                        },
                        label="PUT dest %s" % (cr,),
                    )
                    try:
                        body = json.loads(put_body)
                    except ValueError:
                        raise AssertionError(
                            "PUT dest %s: invalid JSON body=%r (url=%r, file=%r)"
                            % (cr, put_body, dest_url, src_path)
                        )
                    if not body.get("ok"):
                        raise AssertionError(
                            "PUT dest %s: JSON ok=false, full=%r (url=%r, file=%r)"
                            % (cr, body, dest_url, src_path)
                        )
                    if body.get("bytes_written") != len(data):
                        raise AssertionError(
                            "PUT dest %s: bytes_written=%r expected %d (url=%r, file=%r)"
                            % (cr, body.get("bytes_written"), len(data), dest_url, src_path)
                        )

                with ThreadPoolExecutor(max_workers=transfer_workers) as pool:
                    list(pool.map(transfer_span, ranges))

                _flush, flush_body = _http_post_checked(
                    "%s/flush" % (dest_url,),
                    label="POST dest /flush",
                )
                try:
                    flush_json = json.loads(flush_body)
                except ValueError as e:
                    raise AssertionError(
                        "POST dest /flush: invalid JSON body=%r (url=%r, file=%r)"
                        % (flush_body, dest_url, src_path)
                    ) from e
                if not flush_json.get("ok"):
                    raise AssertionError(
                        "POST dest /flush: ok=false, full=%r (url=%r, file=%r)"
                        % (flush_json, dest_url, src_path)
                    )
            finally:
                cleanup_dest()
                cleanup_src()

            try:
                cmp = subprocess.run(
                    ["qemu-img", "compare", src_path, dest_path],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    universal_newlines=True,
                )
                if cmp.returncode != 0:
                    raise AssertionError(
                        "qemu-img compare %r vs %r failed (rc=%s): stderr=%r stdout=%r"
                        % (src_path, dest_path, cmp.returncode, cmp.stderr, cmp.stdout)
                    )
            finally:
                try:
                    os.unlink(dest_path)
                except FileNotFoundError:
                    pass

            return {"path": src_path, "ranges": len(ranges), "skipped": False}

        started = time.perf_counter()
        results = []
        failures = []
        with ThreadPoolExecutor(max_workers=self._file_workers) as pool:
            futs = {pool.submit(run_one, p): p for p in self._qcow2_files}
            for fut in as_completed(futs):
                p = futs[fut]
                try:
                    results.append(fut.result())
                except Exception as e:
                    failures.append((p, e))

        elapsed = time.perf_counter() - started
        skipped = sum(1 for r in results if r.get("skipped"))
        total_ranges = sum(int(r.get("ranges", 0)) for r in results)
        print(
            "stress_io: test_parallel_reads_then_put_range_copy_matches_source: files=%d workers=%d skipped=%d total_ranges=%d elapsed=%.3fs"
            % (len(self._qcow2_files), self._file_workers, skipped, total_ranges, elapsed)
        )

        if failures:
            first_path, first_exc = failures[0]
            raise AssertionError(
                "stress_io: %d/%d files failed (first=%r): %r"
                % (len(failures), len(self._qcow2_files), first_path, first_exc)
            ) from first_exc


if __name__ == "__main__":
    unittest.main()
