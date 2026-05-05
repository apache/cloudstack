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
Multi-operation sequences, parallel reads across multiple transfer objects,
cross-backend scenarios, and edge cases.
"""

import json
import logging
import unittest
import urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed

from .test_base import (
    IMAGE_SIZE,
    ImageServerTestCase,
    http_get,
    http_patch,
    http_post,
    http_put,
    make_file_transfer,
    make_nbd_transfer,
    randbytes,
    shutdown_image_server,
    test_timeout,
)

log = logging.getLogger(__name__)
FUTURES_TIMEOUT = 60  # seconds for as_completed to collect all results


def _fetch(url, headers=None):
    """GET *url* and return the body bytes, properly closing the response."""
    resp = http_get(url, headers=headers)
    try:
        return resp.read()
    finally:
        resp.close()


class TestParallelReadsFileBackend(ImageServerTestCase):
    """Multiple concurrent GET requests to multiple file-backed transfers."""

    @test_timeout(120)
    def test_parallel_reads_single_file_transfer(self):
        data = randbytes(500, IMAGE_SIZE)
        tid, url, path, cleanup = make_file_transfer(data=data)
        try:
            results = {}
            with ThreadPoolExecutor(max_workers=8) as pool:
                futures = {}
                for i in range(8):
                    start = i * (IMAGE_SIZE // 8)
                    end = start + (IMAGE_SIZE // 8) - 1
                    f = pool.submit(
                        _fetch, url, headers={"Range": f"bytes={start}-{end}"}
                    )
                    futures[f] = (start, end)

                for f in as_completed(futures, timeout=FUTURES_TIMEOUT):
                    start, end = futures[f]
                    results[(start, end)] = f.result()

            for (start, end), chunk in sorted(results.items()):
                self.assertEqual(chunk, data[start:end + 1], f"Mismatch at {start}-{end}")
        finally:
            cleanup()

    @test_timeout(120)
    def test_parallel_reads_multiple_file_transfers(self):
        """Parallel reads across 4 different file-backed transfer objects."""
        transfers = []
        try:
            for i in range(4):
                data = randbytes(600 + i, IMAGE_SIZE)
                tid, url, path, cleanup = make_file_transfer(data=data)
                transfers.append((tid, url, data, cleanup))

            with ThreadPoolExecutor(max_workers=8) as pool:
                futures = {}
                for idx, (tid, url, data, _) in enumerate(transfers):
                    for j in range(2):
                        f = pool.submit(_fetch, url)
                        futures[f] = (idx, data)

                for f in as_completed(futures, timeout=FUTURES_TIMEOUT):
                    idx, expected_data = futures[f]
                    got = f.result()
                    self.assertEqual(got, expected_data, f"Transfer {idx} mismatch")
        finally:
            for _, _, _, cleanup in transfers:
                cleanup()


class TestParallelReadsNbdBackend(ImageServerTestCase):
    """Multiple concurrent GET requests to multiple NBD-backed transfers."""

    @test_timeout(120)
    def test_parallel_reads_single_nbd_transfer(self):
        data = randbytes(700, IMAGE_SIZE)
        tid, url, nbd_server, cleanup = make_nbd_transfer()
        try:
            log.info("Writing %d bytes to NBD transfer %s", IMAGE_SIZE, tid)
            http_put(url, data)
            log.info("NBD write done, starting 8 parallel range reads")

            results = {}
            with ThreadPoolExecutor(max_workers=8) as pool:
                futures = {}
                for i in range(8):
                    start = i * (IMAGE_SIZE // 8)
                    end = start + (IMAGE_SIZE // 8) - 1
                    f = pool.submit(
                        _fetch, url, headers={"Range": f"bytes={start}-{end}"}
                    )
                    futures[f] = (start, end)

                completed = 0
                for f in as_completed(futures, timeout=FUTURES_TIMEOUT):
                    start, end = futures[f]
                    results[(start, end)] = f.result()
                    completed += 1
                    log.info("NBD range read %d/8 done: bytes=%d-%d", completed, start, end)

            for (start, end), chunk in sorted(results.items()):
                self.assertEqual(chunk, data[start:end + 1], f"Mismatch at {start}-{end}")
        finally:
            cleanup()

    @test_timeout(120)
    def test_parallel_reads_multiple_nbd_transfers(self):
        """Parallel reads across 4 different NBD-backed transfer objects."""
        transfers = []
        try:
            for i in range(4):
                data = randbytes(800 + i, IMAGE_SIZE)
                log.info("Setting up NBD transfer %d", i)
                tid, url, nbd_server, cleanup = make_nbd_transfer()
                log.info("Writing data to NBD transfer %d (tid=%s)", i, tid)
                http_put(url, data)
                transfers.append((tid, url, data, cleanup))
                log.info("NBD transfer %d ready", i)

            log.info("Starting parallel reads across %d NBD transfers", len(transfers))
            with ThreadPoolExecutor(max_workers=8) as pool:
                futures = {}
                for idx, (tid, url, data, _) in enumerate(transfers):
                    for j in range(2):
                        f = pool.submit(_fetch, url)
                        futures[f] = (idx, data)

                completed = 0
                for f in as_completed(futures, timeout=FUTURES_TIMEOUT):
                    idx, expected_data = futures[f]
                    got = f.result()
                    completed += 1
                    log.info("Read %d/%d done: NBD transfer idx=%d, %d bytes",
                             completed, len(futures), idx, len(got))
                    self.assertEqual(got, expected_data, f"NBD transfer {idx} mismatch")
        finally:
            for _, _, _, cleanup in transfers:
                cleanup()


class TestParallelReadsMixedBackends(ImageServerTestCase):
    """Parallel reads across a mix of file and NBD transfers simultaneously."""

    @test_timeout(120)
    def test_parallel_reads_file_and_nbd_mixed(self):
        transfers = []
        try:
            for i in range(2):
                log.info("Setting up file transfer %d", i)
                data = randbytes(900 + i, IMAGE_SIZE)
                tid, url, path, cleanup = make_file_transfer(data=data)
                transfers.append(("file", tid, url, data, cleanup))
                log.info("File transfer %d ready: tid=%s", i, tid)

            for i in range(2):
                log.info("Setting up NBD transfer %d", i)
                data = randbytes(950 + i, IMAGE_SIZE)
                tid, url, nbd_server, cleanup = make_nbd_transfer()
                log.info("NBD transfer %d registered: tid=%s, writing data...", i, tid)
                http_put(url, data)
                transfers.append(("nbd", tid, url, data, cleanup))
                log.info("NBD transfer %d ready", i)

            log.info("Starting parallel reads across %d transfers (2 file + 2 nbd)",
                     len(transfers))
            with ThreadPoolExecutor(max_workers=8) as pool:
                futures = {}
                for idx, (backend_type, tid, url, data, _) in enumerate(transfers):
                    for j in range(2):
                        f = pool.submit(_fetch, url)
                        futures[f] = (idx, backend_type, data)

                completed = 0
                for f in as_completed(futures, timeout=FUTURES_TIMEOUT):
                    idx, backend_type, expected = futures[f]
                    got = f.result()
                    completed += 1
                    log.info("Read %d/%d done: %s transfer idx=%d, %d bytes",
                             completed, len(futures), backend_type, idx, len(got))
                    self.assertEqual(got, expected, f"{backend_type} transfer {idx} mismatch")

            log.info("All parallel mixed reads completed successfully")
        except TimeoutError:
            log.error("TIMEOUT in mixed parallel reads — dumping server logs")
            self.dump_server_logs()
            raise
        finally:
            for _, _, _, _, cleanup in transfers:
                cleanup()


class TestWriteThenReadNbd(ImageServerTestCase):
    """Multi-step write sequences on NBD backend."""

    def setUp(self):
        self._tid, self._url, self._nbd, self._cleanup = make_nbd_transfer()

    def tearDown(self):
        self._cleanup()

    def test_partial_writes_then_full_read(self):
        http_put(self._url, b"\x00" * IMAGE_SIZE)

        chunk_size = IMAGE_SIZE // 4
        for i in range(4):
            offset = i * chunk_size
            end = offset + chunk_size - 1
            data = bytes([i & 0xFF]) * chunk_size
            http_patch(self._url, data, headers={
                "Range": f"bytes={offset}-{end}",
                "Content-Type": "application/octet-stream",
                "Content-Length": str(chunk_size),
            })

        resp = http_get(self._url)
        full = resp.read()
        for i in range(4):
            offset = i * chunk_size
            self.assertEqual(full[offset:offset + chunk_size], bytes([i & 0xFF]) * chunk_size)

    def test_zero_then_extents(self):
        http_put(self._url, randbytes(1000, IMAGE_SIZE))

        payload = json.dumps({"op": "zero", "size": IMAGE_SIZE // 2, "offset": 0}).encode()
        http_patch(self._url, payload, headers={
            "Content-Type": "application/json",
            "Content-Length": str(len(payload)),
        })

        resp = http_get(f"{self._url}/extents")
        extents = json.loads(resp.read())
        total = sum(e["length"] for e in extents)
        self.assertEqual(total, IMAGE_SIZE)

    def test_write_flush_read(self):
        data = randbytes(1001, IMAGE_SIZE)
        resp = http_put(f"{self._url}?flush=y", data)
        body = json.loads(resp.read())
        self.assertTrue(body["flushed"])

        resp2 = http_get(self._url)
        self.assertEqual(resp2.read(), data)


class TestWriteThenReadFile(ImageServerTestCase):
    def setUp(self):
        self._tid, self._url, self._path, self._cleanup = make_file_transfer()

    def tearDown(self):
        self._cleanup()

    def test_put_then_get_roundtrip(self):
        data = randbytes(1100, IMAGE_SIZE)
        http_put(self._url, data)
        resp = http_get(self._url)
        self.assertEqual(resp.read(), data)


class TestRegisterUseUnregisterUse(ImageServerTestCase):
    def test_unregistered_transfer_returns_404(self):
        data = randbytes(1200, IMAGE_SIZE)
        tid, url, path, cleanup = make_file_transfer(data=data)

        resp = http_get(url)
        self.assertEqual(resp.read(), data)

        cleanup()

        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_get(url)
        self.assertEqual(ctx.exception.code, 404)


class TestMultipleTransfersSimultaneous(ImageServerTestCase):
    @test_timeout(120)
    def test_operate_on_file_and_nbd_concurrently(self):
        file_data = randbytes(1300, IMAGE_SIZE)
        nbd_data = randbytes(1301, IMAGE_SIZE)

        ftid, furl, fpath, fcleanup = make_file_transfer(data=file_data)
        ntid, nurl, nbd_server, ncleanup = make_nbd_transfer()

        try:
            log.info("Writing data to NBD transfer %s", ntid)
            http_put(nurl, nbd_data)

            log.info("Starting concurrent file + NBD reads")
            with ThreadPoolExecutor(max_workers=4) as pool:
                f_file = pool.submit(_fetch, furl)
                f_nbd = pool.submit(_fetch, nurl)

                self.assertEqual(f_file.result(timeout=FUTURES_TIMEOUT), file_data)
                self.assertEqual(f_nbd.result(timeout=FUTURES_TIMEOUT), nbd_data)
            log.info("Concurrent reads completed successfully")
        finally:
            fcleanup()
            ncleanup()


class TestLargeChunkedTransfer(ImageServerTestCase):
    def test_put_larger_than_chunk_size_file(self):
        """Upload data that spans multiple CHUNK_SIZE boundaries."""
        tid, url, path, cleanup = make_file_transfer()
        try:
            data = randbytes(1400, IMAGE_SIZE)
            http_put(url, data)
            resp = http_get(url)
            self.assertEqual(resp.read(), data)
        finally:
            cleanup()

    def test_nbd_put_larger_than_chunk_size(self):
        tid, url, nbd_server, cleanup = make_nbd_transfer()
        try:
            data = randbytes(1401, IMAGE_SIZE)
            http_put(url, data)
            resp = http_get(url)
            self.assertEqual(resp.read(), data)
        finally:
            cleanup()


class TestEdgeCases(ImageServerTestCase):
    def test_get_not_found_path(self):
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            http_get(f"{self.base_url}/not/images/path")
        self.assertEqual(ctx.exception.code, 404)

    def test_post_unknown_tail(self):
        tid, url, path, cleanup = make_file_transfer()
        try:
            with self.assertRaises(urllib.error.HTTPError) as ctx:
                http_post(f"{url}/unknown")
            self.assertEqual(ctx.exception.code, 404)
        finally:
            cleanup()

    def test_get_extents_then_flush_nbd(self):
        tid, url, nbd_server, cleanup = make_nbd_transfer()
        try:
            http_put(url, randbytes(1500, IMAGE_SIZE))

            resp = http_get(f"{url}/extents")
            self.assertEqual(resp.status, 200)
            resp.read()

            resp2 = http_post(f"{url}/flush")
            body = json.loads(resp2.read())
            self.assertTrue(body["ok"])
        finally:
            cleanup()


if __name__ == "__main__":
    try:
        unittest.main()
    finally:
        shutdown_image_server()
