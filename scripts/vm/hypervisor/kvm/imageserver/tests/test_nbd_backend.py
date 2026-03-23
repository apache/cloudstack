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
import unittest
import urllib.error
import urllib.request

from .test_base import (
    IMAGE_SIZE,
    ImageServerTestCase,
    http_get,
    http_options,
    http_patch,
    http_post,
    http_put,
    make_nbd_transfer,
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
