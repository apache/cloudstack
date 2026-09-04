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

"""Tests for HTTP operations against a file-backend transfer."""

import json
import os
import unittest
import urllib.error

from .test_base import (
    IMAGE_SIZE,
    ImageServerTestCase,
    http_get,
    http_options,
    http_patch,
    http_post,
    http_put,
    make_file_transfer,
    randbytes,
    shutdown_image_server,
)


class FileBackendTestCase(ImageServerTestCase):
    """Base that creates a file-backend transfer per test."""

    def setUp(self):
        self._tid, self._url, self._path, self._cleanup = make_file_transfer()

    def tearDown(self):
        self._cleanup()


class TestOptions(FileBackendTestCase):
    def test_options_returns_features(self):
        resp = http_options(self._url)
        self.assertEqual(resp.status, 200)
        body = json.loads(resp.read())
        self.assertIn("flush", body["features"])
        self.assertGreaterEqual(body["max_readers"], 1)
        self.assertGreaterEqual(body["max_writers"], 1)

    def test_options_allowed_methods(self):
        resp = http_options(self._url)
        methods = resp.getheader("Access-Control-Allow-Methods")
        for m in ("GET", "PUT", "POST", "OPTIONS"):
            self.assertIn(m, methods)


class TestGetFull(FileBackendTestCase):
    def test_get_full_returns_file_content(self):
        with open(self._path, "rb") as f:
            expected = f.read()
        resp = http_get(self._url)
        self.assertEqual(resp.status, 200)
        data = resp.read()
        self.assertEqual(len(data), len(expected))
        self.assertEqual(data, expected)

    def test_get_full_content_type(self):
        resp = http_get(self._url)
        resp.read()
        self.assertIn("application/octet-stream", resp.getheader("Content-Type"))

    def test_get_full_content_length(self):
        resp = http_get(self._url)
        resp.read()
        self.assertEqual(int(resp.getheader("Content-Length")), os.path.getsize(self._path))


class TestGetRange(FileBackendTestCase):
    def test_get_range_partial(self):
        with open(self._path, "rb") as f:
            f.seek(100)
            expected = f.read(200)
        resp = http_get(self._url, headers={"Range": "bytes=100-299"})
        self.assertEqual(resp.status, 206)
        self.assertEqual(resp.read(), expected)

    def test_get_range_content_range_header(self):
        size = os.path.getsize(self._path)
        resp = http_get(self._url, headers={"Range": "bytes=0-99"})
        self.assertEqual(resp.status, 206)
        resp.read()
        self.assertEqual(resp.getheader("Content-Range"), f"bytes 0-99/{size}")

    def test_get_range_suffix(self):
        with open(self._path, "rb") as f:
            expected = f.read()[-100:]
        resp = http_get(self._url, headers={"Range": "bytes=-100"})
        self.assertEqual(resp.status, 206)
        self.assertEqual(resp.read(), expected)

    def test_get_range_open_ended(self):
        with open(self._path, "rb") as f:
            f.seek(IMAGE_SIZE - 50)
            expected = f.read()
        resp = http_get(self._url, headers={"Range": f"bytes={IMAGE_SIZE - 50}-"})
        self.assertEqual(resp.status, 206)
        self.assertEqual(resp.read(), expected)

    def test_get_range_unsatisfiable(self):
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_get(self._url, headers={"Range": f"bytes={IMAGE_SIZE + 100}-{IMAGE_SIZE + 200}"})
        self.assertEqual(ctx.exception.code, 416)


class TestPut(FileBackendTestCase):
    def test_put_full_upload(self):
        new_data = randbytes(99, IMAGE_SIZE)
        resp = http_put(self._url, new_data)
        body = json.loads(resp.read())
        self.assertEqual(resp.status, 200)
        self.assertTrue(body["ok"])
        self.assertEqual(body["bytes_written"], IMAGE_SIZE)

        with open(self._path, "rb") as f:
            self.assertEqual(f.read(), new_data)

    def test_put_with_flush(self):
        new_data = randbytes(100, IMAGE_SIZE)
        resp = http_put(f"{self._url}?flush=y", new_data)
        body = json.loads(resp.read())
        self.assertTrue(body["ok"])
        self.assertTrue(body["flushed"])

    def test_put_verify_by_get(self):
        new_data = randbytes(101, IMAGE_SIZE)
        http_put(self._url, new_data)
        resp = http_get(self._url)
        self.assertEqual(resp.read(), new_data)

    def test_put_with_content_range_rejected(self):
        data = b"x" * 100
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_put(self._url, data, headers={"Content-Range": "bytes 0-99/*"})
        self.assertEqual(ctx.exception.code, 400)

    def test_put_with_range_header_rejected(self):
        data = b"x" * 100
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_put(self._url, data, headers={"Range": "bytes=0-99"})
        self.assertEqual(ctx.exception.code, 400)


class TestFlush(FileBackendTestCase):
    def test_post_flush(self):
        resp = http_post(f"{self._url}/flush")
        body = json.loads(resp.read())
        self.assertEqual(resp.status, 200)
        self.assertTrue(body["ok"])


class TestPatchRejected(FileBackendTestCase):
    def test_patch_rejected_for_file(self):
        data = json.dumps({"op": "zero", "size": 100}).encode()
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_patch(self._url, data, headers={
                "Content-Type": "application/json",
                "Content-Length": str(len(data)),
            })
        self.assertEqual(ctx.exception.code, 400)


class TestExtentsRejected(FileBackendTestCase):
    def test_extents_rejected_for_file(self):
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_get(f"{self._url}/extents")
        self.assertEqual(ctx.exception.code, 400)


class TestUnknownImage(ImageServerTestCase):
    def test_get_unknown_image(self):
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_get(f"{self.base_url}/images/nonexistent-id")
        self.assertEqual(ctx.exception.code, 404)

    def test_put_unknown_image(self):
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_put(f"{self.base_url}/images/nonexistent-id", b"data")
        self.assertEqual(ctx.exception.code, 404)

    def test_options_unknown_image(self):
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_options(f"{self.base_url}/images/nonexistent-id")
        self.assertEqual(ctx.exception.code, 404)


class TestRoundTrip(FileBackendTestCase):
    def test_put_then_get_roundtrip(self):
        payload = randbytes(200, IMAGE_SIZE)
        http_put(self._url, payload)
        resp = http_get(self._url)
        self.assertEqual(resp.read(), payload)

    def test_put_then_ranged_get_roundtrip(self):
        payload = randbytes(201, IMAGE_SIZE)
        http_put(self._url, payload)
        resp = http_get(self._url, headers={"Range": "bytes=512-1023"})
        self.assertEqual(resp.read(), payload[512:1024])

    def test_multiple_puts_last_wins(self):
        first = randbytes(300, IMAGE_SIZE)
        second = randbytes(301, IMAGE_SIZE)
        http_put(self._url, first)
        http_put(self._url, second)
        resp = http_get(self._url)
        self.assertEqual(resp.read(), second)


if __name__ == "__main__":
    try:
        unittest.main()
    finally:
        shutdown_image_server()
