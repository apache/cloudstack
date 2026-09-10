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

"""Tests for HTTP operations against an NBD-backend transfer (real qemu-nbd)."""

import json
import os
import subprocess
import unittest
import uuid
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

from imageserver.constants import MAX_PARALLEL_READS, MAX_PARALLEL_WRITES

from .test_base import (
    IMAGE_SIZE,
    ImageServerTestCase,
    get_tmp_dir,
    http_get,
    http_options,
    http_patch,
    http_post,
    http_put,
    make_nbd_transfer,
    make_nbd_transfer_existing_disk,
    randbytes,
    shutdown_image_server,
)


class NbdBackendTestCase(ImageServerTestCase):
    """Base that creates an NBD-backend transfer per test."""

    def setUp(self):
        self._tid, self._url, self._nbd, self._cleanup = make_nbd_transfer()

    def tearDown(self):
        self._cleanup()


class TestOptions(NbdBackendTestCase):
    def test_options_returns_extents_feature(self):
        resp = http_options(self._url)
        self.assertEqual(resp.status, 200)
        body = json.loads(resp.read())
        self.assertIn("extents", body["features"])

    def test_options_includes_patch_method(self):
        resp = http_options(self._url)
        methods = resp.getheader("Access-Control-Allow-Methods")
        self.assertIn("PATCH", methods)

    def test_options_has_capabilities(self):
        resp = http_options(self._url)
        body = json.loads(resp.read())
        self.assertGreaterEqual(body["max_readers"], 1)
        self.assertGreaterEqual(body["max_writers"], 1)


class TestGetFull(NbdBackendTestCase):
    def test_get_full_returns_image_data(self):
        with open(self._nbd.image_path, "rb") as f:
            expected = f.read()
        resp = http_get(self._url)
        data = resp.read()
        self.assertEqual(resp.status, 200)
        self.assertEqual(len(data), len(expected))
        self.assertEqual(data, expected)

    def test_get_full_content_length(self):
        resp = http_get(self._url)
        resp.read()
        self.assertEqual(int(resp.getheader("Content-Length")), IMAGE_SIZE)


class TestGetRange(NbdBackendTestCase):
    def test_get_range_partial(self):
        test_data = randbytes(50, IMAGE_SIZE)
        http_put(self._url, test_data)

        resp = http_get(self._url, headers={"Range": "bytes=100-299"})
        self.assertEqual(resp.status, 206)
        self.assertEqual(resp.read(), test_data[100:300])

    def test_get_range_content_range_header(self):
        resp = http_get(self._url, headers={"Range": "bytes=0-99"})
        self.assertEqual(resp.status, 206)
        resp.read()
        self.assertEqual(resp.getheader("Content-Range"), f"bytes 0-99/{IMAGE_SIZE}")

    def test_get_range_suffix(self):
        test_data = randbytes(51, IMAGE_SIZE)
        http_put(self._url, test_data)

        resp = http_get(self._url, headers={"Range": "bytes=-100"})
        self.assertEqual(resp.status, 206)
        self.assertEqual(resp.read(), test_data[-100:])

    def test_get_range_unsatisfiable(self):
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_get(self._url, headers={"Range": f"bytes={IMAGE_SIZE + 100}-{IMAGE_SIZE + 200}"})
        self.assertEqual(ctx.exception.code, 416)


class TestPutFull(NbdBackendTestCase):
    def test_put_full_upload(self):
        new_data = randbytes(60, IMAGE_SIZE)
        resp = http_put(self._url, new_data)
        body = json.loads(resp.read())
        self.assertEqual(resp.status, 200)
        self.assertTrue(body["ok"])
        self.assertEqual(body["bytes_written"], IMAGE_SIZE)

        resp2 = http_get(self._url)
        self.assertEqual(resp2.read(), new_data)

    def test_put_with_flush(self):
        new_data = randbytes(61, IMAGE_SIZE)
        resp = http_put(f"{self._url}?flush=y", new_data)
        body = json.loads(resp.read())
        self.assertTrue(body["ok"])
        self.assertTrue(body["flushed"])


class TestPutRange(NbdBackendTestCase):
    def test_put_content_range(self):
        base_data = randbytes(70, IMAGE_SIZE)
        http_put(self._url, base_data)

        patch_data = b"\xAB" * 512
        resp = http_put(self._url, patch_data, headers={
            "Content-Range": "bytes 0-511/*",
            "Content-Length": str(len(patch_data)),
        })
        body = json.loads(resp.read())
        self.assertEqual(resp.status, 200)
        self.assertTrue(body["ok"])
        self.assertEqual(body["bytes_written"], 512)

        resp2 = http_get(self._url, headers={"Range": "bytes=0-511"})
        self.assertEqual(resp2.read(), patch_data)

        resp3 = http_get(self._url, headers={"Range": "bytes=512-1023"})
        self.assertEqual(resp3.read(), base_data[512:1024])

    def test_put_content_range_with_flush(self):
        base_data = b"\x00" * IMAGE_SIZE
        http_put(self._url, base_data)

        patch_data = b"\xFF" * 256
        resp = http_put(f"{self._url}?flush=y", patch_data, headers={
            "Content-Range": "bytes 1024-1279/*",
            "Content-Length": str(len(patch_data)),
        })
        body = json.loads(resp.read())
        self.assertTrue(body["ok"])
        self.assertTrue(body["flushed"])


class TestPatchRange(NbdBackendTestCase):
    def test_patch_binary_range(self):
        base_data = randbytes(80, IMAGE_SIZE)
        http_put(self._url, base_data)

        patch_data = b"\xCD" * 1024
        resp = http_patch(self._url, patch_data, headers={
            "Range": "bytes=2048-3071",
            "Content-Type": "application/octet-stream",
            "Content-Length": str(len(patch_data)),
        })
        body = json.loads(resp.read())
        self.assertEqual(resp.status, 200)
        self.assertTrue(body["ok"])
        self.assertEqual(body["bytes_written"], 1024)

        resp2 = http_get(self._url, headers={"Range": "bytes=2048-3071"})
        self.assertEqual(resp2.read(), patch_data)

    def test_patch_multiple_ranges_preserves_unwritten(self):
        base_data = randbytes(81, IMAGE_SIZE)
        http_put(self._url, base_data)

        patch1 = b"\x11" * 256
        http_patch(self._url, patch1, headers={
            "Range": "bytes=0-255",
            "Content-Type": "application/octet-stream",
            "Content-Length": "256",
        })

        patch2 = b"\x22" * 256
        http_patch(self._url, patch2, headers={
            "Range": "bytes=512-767",
            "Content-Type": "application/octet-stream",
            "Content-Length": "256",
        })

        resp = http_get(self._url, headers={"Range": "bytes=0-767"})
        got = resp.read()
        self.assertEqual(got[:256], patch1)
        self.assertEqual(got[256:512], base_data[256:512])
        self.assertEqual(got[512:768], patch2)


class TestPatchZero(NbdBackendTestCase):
    def test_patch_zero(self):
        data = randbytes(90, IMAGE_SIZE)
        http_put(self._url, data)

        payload = json.dumps({"op": "zero", "size": 4096, "offset": 0}).encode()
        resp = http_patch(self._url, payload, headers={
            "Content-Type": "application/json",
            "Content-Length": str(len(payload)),
        })
        body = json.loads(resp.read())
        self.assertEqual(resp.status, 200)
        self.assertTrue(body["ok"])

        resp2 = http_get(self._url, headers={"Range": "bytes=0-4095"})
        self.assertEqual(resp2.read(), b"\x00" * 4096)

    def test_patch_zero_with_flush(self):
        data = b"\xFF" * IMAGE_SIZE
        http_put(self._url, data)

        payload = json.dumps({"op": "zero", "size": 512, "offset": 1024, "flush": True}).encode()
        resp = http_patch(self._url, payload, headers={
            "Content-Type": "application/json",
            "Content-Length": str(len(payload)),
        })
        body = json.loads(resp.read())
        self.assertTrue(body["ok"])

        resp2 = http_get(self._url, headers={"Range": "bytes=1024-1535"})
        self.assertEqual(resp2.read(), b"\x00" * 512)

    def test_patch_zero_preserves_neighbors(self):
        data = randbytes(91, IMAGE_SIZE)
        http_put(self._url, data)

        payload = json.dumps({"op": "zero", "size": 256, "offset": 512}).encode()
        http_patch(self._url, payload, headers={
            "Content-Type": "application/json",
            "Content-Length": str(len(payload)),
        })

        resp = http_get(self._url, headers={"Range": "bytes=0-1023"})
        got = resp.read()
        self.assertEqual(got[:512], data[:512])
        self.assertEqual(got[512:768], b"\x00" * 256)
        self.assertEqual(got[768:1024], data[768:1024])


class TestPatchFlush(NbdBackendTestCase):
    def test_patch_flush_op(self):
        payload = json.dumps({"op": "flush"}).encode()
        resp = http_patch(self._url, payload, headers={
            "Content-Type": "application/json",
            "Content-Length": str(len(payload)),
        })
        body = json.loads(resp.read())
        self.assertEqual(resp.status, 200)
        self.assertTrue(body["ok"])


class TestPostFlush(NbdBackendTestCase):
    def test_post_flush(self):
        resp = http_post(f"{self._url}/flush")
        body = json.loads(resp.read())
        self.assertEqual(resp.status, 200)
        self.assertTrue(body["ok"])


class TestExtents(NbdBackendTestCase):
    def test_get_allocation_extents(self):
        resp = http_get(f"{self._url}/extents")
        self.assertEqual(resp.status, 200)
        extents = json.loads(resp.read())
        self.assertIsInstance(extents, list)
        self.assertGreaterEqual(len(extents), 1)
        for ext in extents:
            self.assertIn("start", ext)
            self.assertIn("length", ext)
            self.assertIn("zero", ext)

    def test_extents_cover_full_image(self):
        resp = http_get(f"{self._url}/extents")
        extents = json.loads(resp.read())
        total = sum(e["length"] for e in extents)
        self.assertEqual(total, IMAGE_SIZE)

    def test_extents_dirty_context_without_bitmap(self):
        resp = http_get(f"{self._url}/extents?context=dirty")
        self.assertEqual(resp.status, 200)
        extents = json.loads(resp.read())
        self.assertIsInstance(extents, list)
        self.assertGreaterEqual(len(extents), 1)
        for ext in extents:
            self.assertIn("dirty", ext)
            self.assertTrue(ext["dirty"])

    def test_extents_after_write_and_zero(self):
        http_put(self._url, randbytes(95, IMAGE_SIZE))

        payload = json.dumps({"op": "zero", "size": 4096, "offset": 0}).encode()
        http_patch(self._url, payload, headers={
            "Content-Type": "application/json",
            "Content-Length": str(len(payload)),
        })

        resp = http_get(f"{self._url}/extents")
        extents = json.loads(resp.read())
        self.assertGreaterEqual(len(extents), 1)
        total = sum(e["length"] for e in extents)
        self.assertEqual(total, IMAGE_SIZE)


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
    # stdout=PIPE + universal_newlines: Python 3.6 compatible (no capture_output/text).
    cp = subprocess.run(
        ["qemu-img", "info", "--output=json", path],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        universal_newlines=True,
        check=True,
    )
    return int(json.loads(cp.stdout)["virtual-size"])


def _http_error_detail(exc: urllib.error.HTTPError) -> str:
    """Build a readable message from an ``HTTPError`` (status, url, JSON/text body)."""
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


def _http_get_checked(
    url,
    headers=None,
    expected_status=200,
    label="GET",
):
    """
    Like ``http_get`` but raises ``AssertionError`` with ``_http_error_detail`` on failure.
    """
    try:
        resp = http_get(url, headers=headers)
    except urllib.error.HTTPError as e:
        raise AssertionError(
            "%s failed for %r: %s" % (label, url, _http_error_detail(e))
        ) from e
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
        raise AssertionError(
            "%s failed for %r: %s" % (label, url, _http_error_detail(e))
        ) from e
    body = resp.read()
    if resp.status != 200:
        raise AssertionError(
            "%s %r: expected HTTP 200, got %s; body=%r"
            % (label, url, resp.status, body)
        )
    return resp, body


def _http_post_checked(url, data=b"", headers=None, label="POST"):
    try:
        resp = http_post(url, data=data, headers=headers)
    except urllib.error.HTTPError as e:
        raise AssertionError(
            "%s failed for %r: %s" % (label, url, _http_error_detail(e))
        ) from e
    body = resp.read()
    if resp.status != 200:
        raise AssertionError(
            "%s %r: expected HTTP 200, got %s; body=%r"
            % (label, url, resp.status, body)
        )
    return resp, body


class TestQcow2ExtentsParallelReads(ImageServerTestCase):
    """
    Optional integration tests: export a user-supplied qcow2 via qemu-nbd, fetch
    allocation extents, parallel range GETs over allocated regions, and (second
    test) per-range GET-then-PUT pipeline with ``min(MAX_PARALLEL_READS,
    MAX_PARALLEL_WRITES)`` workers.

    Requires ``qemu-img`` and ``qemu-nbd`` on PATH.

    Set IMAGESERVER_TEST_QCOW2 to the absolute path of a qcow2 file.
    Optional: IMAGESERVER_TEST_QCOW2_READ_GRANULARITY — byte step (default 4 MiB).
    """

    def setUp(self):
        super().setUp()
        self._qcow2_path = os.environ.get("IMAGESERVER_TEST_QCOW2", "").strip()
        if not self._qcow2_path or not os.path.isfile(self._qcow2_path):
            self.skipTest(
                "Set IMAGESERVER_TEST_QCOW2 to an existing qcow2 path to run this test"
            )
        raw_g = os.environ.get("IMAGESERVER_TEST_QCOW2_READ_GRANULARITY", "").strip()
        self._read_granularity = int(raw_g) if raw_g else 4 * 1024 * 1024
        if self._read_granularity <= 0:
            self.skipTest("IMAGESERVER_TEST_QCOW2_READ_GRANULARITY must be positive")

    def test_parallel_range_reads_allocated_extents(self):
        _, url, _, cleanup = make_nbd_transfer_existing_disk(
            self._qcow2_path, "qcow2"
        )
        try:
            resp = _http_get_checked(
                "%s/extents" % (url,),
                expected_status=200,
                label="GET /extents",
            )
            extents = json.loads(resp.read())
            self.assertIsInstance(extents, list)
            ranges = _allocated_subranges(extents, self._read_granularity)
            if not ranges:
                self.skipTest("no allocated extents (all holes/zero) in qcow2")

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
                        "Range GET %s: got %d bytes, expected %d (url=%r)"
                        % (range_hdr, len(data), expected_len, url)
                    )

            with ThreadPoolExecutor(max_workers=MAX_PARALLEL_READS) as pool:
                pool.map(fetch, ranges)
        finally:
            cleanup()

    def test_parallel_reads_then_put_range_copy_matches_source(self):
        """
        Create an empty qcow2 with the same virtual size as the source, copy every
        allocated range using one worker pool: for each span, Range GET from src
        then Content-Range PUT to dest.
        Worker count is ``min(MAX_PARALLEL_READS, MAX_PARALLEL_WRITES)`` so each
        worker holds at most one chunk.
        """
        src_path = self._qcow2_path
        try:
            vsize = _qemu_img_virtual_size(src_path)
        except (FileNotFoundError, subprocess.CalledProcessError, KeyError, json.JSONDecodeError, TypeError, ValueError) as e:
            self.skipTest(f"qemu-img info failed: {e}")

        tmp = get_tmp_dir()
        dest_path = os.path.join(tmp, f"qcow2_copy_{uuid.uuid4().hex[:8]}.qcow2")
        try:
            subprocess.run(
                ["qemu-img", "create", "-f", "qcow2", dest_path, str(vsize)],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                universal_newlines=True,
                check=True,
            )
        except (FileNotFoundError, subprocess.CalledProcessError) as e:
            self.skipTest(f"qemu-img create failed: {e}")

        _, src_url, _, cleanup_src = make_nbd_transfer_existing_disk(
            src_path, "qcow2"
        )
        _, dest_url, _, cleanup_dest = make_nbd_transfer_existing_disk(
            dest_path, "qcow2"
        )
        try:
            resp = _http_get_checked(
                "%s/extents" % (src_url,),
                expected_status=200,
                label="GET src /extents",
            )
            extents = json.loads(resp.read())
            ranges = _allocated_subranges(extents, self._read_granularity)
            if not ranges:
                self.skipTest("no allocated extents (all holes/zero) in qcow2")

            transfer_workers = max(
                1, min(MAX_PARALLEL_READS, MAX_PARALLEL_WRITES)
            )

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
                        "Range GET src %s: got %d bytes, expected %d (url=%r)"
                        % (range_hdr, len(data), expected_len, src_url)
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
                        "PUT dest %s: invalid JSON body=%r (url=%r)"
                        % (cr, put_body, dest_url)
                    )
                if not body.get("ok"):
                    raise AssertionError(
                        "PUT dest %s: JSON ok=false, full=%r (url=%r)"
                        % (cr, body, dest_url)
                    )
                if body.get("bytes_written") != len(data):
                    raise AssertionError(
                        "PUT dest %s: bytes_written=%r expected %d (url=%r)"
                        % (cr, body.get("bytes_written"), len(data), dest_url)
                    )

            with ThreadPoolExecutor(max_workers=transfer_workers) as pool:
                pool.map(transfer_span, ranges)

            _flush, flush_body = _http_post_checked(
                "%s/flush" % (dest_url,),
                label="POST dest /flush",
            )
            try:
                flush_json = json.loads(flush_body)
            except ValueError:
                raise AssertionError(
                    "POST dest /flush: invalid JSON body=%r (url=%r)"
                    % (flush_body, dest_url)
                )
            if not flush_json.get("ok"):
                raise AssertionError(
                    "POST dest /flush: ok=false, full=%r (url=%r)"
                    % (flush_json, dest_url)
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
            self.assertEqual(
                cmp.returncode,
                0,
                "qemu-img compare %r vs %r failed (rc=%s): stderr=%r stdout=%r"
                % (
                    src_path,
                    dest_path,
                    cmp.returncode,
                    cmp.stderr,
                    cmp.stdout,
                ),
            )
        finally:
            try:
                os.unlink(dest_path)
            except FileNotFoundError:
                pass


class TestErrorCases(NbdBackendTestCase):
    def test_patch_unsupported_op(self):
        payload = json.dumps({"op": "invalid"}).encode()
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_patch(self._url, payload, headers={
                "Content-Type": "application/json",
                "Content-Length": str(len(payload)),
            })
        self.assertEqual(ctx.exception.code, 400)

    def test_patch_zero_missing_size(self):
        payload = json.dumps({"op": "zero", "offset": 0}).encode()
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_patch(self._url, payload, headers={
                "Content-Type": "application/json",
                "Content-Length": str(len(payload)),
            })
        self.assertEqual(ctx.exception.code, 400)

    def test_put_missing_content_length(self):
        import http.client
        from urllib.parse import urlparse
        parsed = urlparse(self._url)
        conn = http.client.HTTPConnection(parsed.hostname, parsed.port, timeout=30)
        try:
            conn.putrequest("PUT", parsed.path)
            conn.endheaders()
            resp = conn.getresponse()
            self.assertEqual(resp.status, 400)
        finally:
            conn.close()


class TestRoundTrip(NbdBackendTestCase):
    def test_write_read_full_roundtrip(self):
        payload = randbytes(110, IMAGE_SIZE)
        http_put(self._url, payload)
        resp = http_get(self._url)
        self.assertEqual(resp.read(), payload)

    def test_write_read_range_roundtrip(self):
        payload = randbytes(111, IMAGE_SIZE)
        http_put(self._url, payload)

        for start, end in [(0, 255), (1024, 2047), (IMAGE_SIZE - 512, IMAGE_SIZE - 1)]:
            resp = http_get(self._url, headers={"Range": f"bytes={start}-{end}"})
            self.assertEqual(resp.read(), payload[start:end + 1])

    def test_range_write_read_roundtrip(self):
        http_put(self._url, b"\x00" * IMAGE_SIZE)

        chunk = randbytes(112, 4096)
        http_put(self._url, chunk, headers={
            "Content-Range": "bytes 8192-12287/*",
            "Content-Length": str(len(chunk)),
        })

        resp = http_get(self._url, headers={"Range": "bytes=8192-12287"})
        self.assertEqual(resp.read(), chunk)

        resp2 = http_get(self._url, headers={"Range": "bytes=0-4095"})
        self.assertEqual(resp2.read(), b"\x00" * 4096)


if __name__ == "__main__":
    try:
        unittest.main()
    finally:
        shutdown_image_server()
