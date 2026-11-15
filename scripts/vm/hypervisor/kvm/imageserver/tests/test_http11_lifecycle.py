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

"""Integration tests for HTTP/1.1 connection lifecycle behavior."""

import http.client
import socket
import threading
import time

from .test_base import (
    HTTP_TIMEOUT,
    ImageServerTestCase,
    make_file_transfer,
    randbytes,
    test_timeout,
)


def _read_http_headers(sock: socket.socket, timeout: float = HTTP_TIMEOUT) -> bytes:
    sock.settimeout(timeout)
    data = b""
    while b"\r\n\r\n" not in data:
        chunk = sock.recv(4096)
        if not chunk:
            break
        data += chunk
    return data


class TestHttp11PersistentConnections(ImageServerTestCase):
    @test_timeout(60)
    def test_multiple_get_requests_reuse_single_socket(self):
        data = randbytes(1111, 512 * 1024)
        transfer_id, _url, _path, cleanup = make_file_transfer(data=data)
        try:
            conn = http.client.HTTPConnection("127.0.0.1", self.server["port"], timeout=HTTP_TIMEOUT)
            try:
                path = f"/images/{transfer_id}"

                conn.request("GET", path, headers={"Range": "bytes=0-1023"})
                resp1 = conn.getresponse()
                body1 = resp1.read()
                self.assertEqual(resp1.status, 206)
                self.assertEqual(body1, data[:1024])
                self.assertIsNotNone(conn.sock)
                first_local_port = conn.sock.getsockname()[1]

                conn.request("GET", path, headers={"Range": "bytes=1024-2047"})
                resp2 = conn.getresponse()
                body2 = resp2.read()
                self.assertEqual(resp2.status, 206)
                self.assertEqual(body2, data[1024:2048])
                self.assertIsNotNone(conn.sock)
                second_local_port = conn.sock.getsockname()[1]

                conn.request("OPTIONS", path)
                resp3 = conn.getresponse()
                _ = resp3.read()
                self.assertEqual(resp3.status, 200)
                self.assertIsNotNone(conn.sock)
                third_local_port = conn.sock.getsockname()[1]

                self.assertEqual(first_local_port, second_local_port)
                self.assertEqual(second_local_port, third_local_port)
            finally:
                conn.close()
        finally:
            cleanup()


class TestTeardownTiming(ImageServerTestCase):
    @test_timeout(60)
    def test_unregister_waits_for_inflight_put(self):
        transfer_id, _url, _path, cleanup = make_file_transfer(data=b"\x00" * (2 * 1024 * 1024))
        started = threading.Event()
        put_done = threading.Event()
        put_result = {"status_line": "", "error": None}
        body = randbytes(2222, 1024 * 1024)

        def send_slow_put() -> None:
            sock = None
            try:
                sock = socket.create_connection(("127.0.0.1", self.server["port"]), timeout=HTTP_TIMEOUT)
                request_headers = (
                    f"PUT /images/{transfer_id} HTTP/1.1\r\n"
                    f"Host: 127.0.0.1:{self.server['port']}\r\n"
                    f"Content-Length: {len(body)}\r\n"
                    "Connection: close\r\n"
                    "\r\n"
                ).encode("ascii")
                sock.sendall(request_headers)

                sent = 0
                chunk_size = 16 * 1024
                while sent < len(body):
                    end = min(sent + chunk_size, len(body))
                    sock.sendall(body[sent:end])
                    sent = end
                    if sent >= chunk_size and not started.is_set():
                        started.set()
                    time.sleep(0.02)

                headers = _read_http_headers(sock)
                if headers:
                    put_result["status_line"] = headers.split(b"\r\n", 1)[0].decode("ascii", "replace")
            except Exception as e:
                put_result["error"] = repr(e)
            finally:
                if sock is not None:
                    try:
                        sock.close()
                    except OSError:
                        pass
                put_done.set()

        sender = threading.Thread(target=send_slow_put, daemon=True)
        sender.start()

        try:
            self.assertTrue(started.wait(5), "PUT request did not start in time")

            t0 = time.monotonic()
            unregister_resp = self.ctrl({"action": "unregister", "transfer_id": transfer_id})
            elapsed = time.monotonic() - t0

            self.assertTrue(put_done.wait(5), "PUT request did not finish in time")
            self.assertEqual(unregister_resp.get("status"), "ok")
            self.assertGreater(elapsed, 0.2)
            self.assertIsNone(put_result["error"], put_result["error"])
            self.assertIn(" 200 ", put_result["status_line"])
        finally:
            sender.join(timeout=1)
            cleanup()
