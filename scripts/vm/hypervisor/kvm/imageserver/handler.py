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

import json
import logging
import re
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import parse_qs

from .backends import NbdBackend, create_backend
from .config import TransferRegistry
from .constants import CHUNK_SIZE, MAX_PARALLEL_READS, MAX_PARALLEL_WRITES, MAX_PATCH_JSON_SIZE
from .util import is_fallback_dirty_response, json_bytes, now_s


class Handler(BaseHTTPRequestHandler):
    """
    HTTP request handler for the image server.

    Routing, HTTP parsing, and response formatting live here.
    All backend I/O is delegated to ImageBackend implementations via the
    create_backend() factory.

    Class-level attribute _registry is injected
    by the server at startup (see server.py / make_handler()).
    """

    server_version = "cloudstack-image-server/1.0"
    protocol_version = "HTTP/1.1"

    _registry: TransferRegistry

    _CONTENT_RANGE_RE = re.compile(r"^bytes\s+(\d+)-(\d+)/(?:\*|\d+)$")

    def log_message(self, fmt: str, *args: Any) -> None:
        logging.info("%s - - %s", self.client_address[0], fmt % args)

    # ------------------------------------------------------------------
    # Response helpers
    # ------------------------------------------------------------------

    def _send_imageio_headers(
        self, allowed_methods: Optional[str] = None
    ) -> None:
        if allowed_methods is None:
            allowed_methods = "GET, PUT, OPTIONS"
        self.send_header("Access-Control-Allow-Methods", allowed_methods)
        self.send_header("Accept-Ranges", "bytes")

    def _send_json(
        self,
        status: int,
        obj: Any,
        allowed_methods: Optional[str] = None,
    ) -> None:
        body = json_bytes(obj)
        self.send_response(status)
        self._send_imageio_headers(allowed_methods)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except BrokenPipeError:
            logging.error(
                "HTTP response write failure status=%s method=%s path=%s err=%s",
                int(status),
                self.command,
                self.path,
                "client disconnected",
            )

    def _send_error_json(self, status: int, message: str) -> None:
        logging.error(
            "HTTP failure status=%s method=%s path=%s message=%s",
            int(status),
            self.command,
            self.path,
            message,
        )
        self._send_json(status, {"error": message})

    def _send_range_not_satisfiable(self, size: int) -> None:
        logging.error(
            "HTTP failure status=%s method=%s path=%s message=%s size=%s",
            int(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE),
            self.command,
            self.path,
            "range not satisfiable",
            size,
        )
        self.send_response(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
        self._send_imageio_headers()
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Range", f"bytes */{size}")
        body = json_bytes({"error": "range not satisfiable"})
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except BrokenPipeError:
            logging.error(
                "HTTP response write failure status=%s method=%s path=%s err=%s",
                int(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE),
                self.command,
                self.path,
                "client disconnected",
            )

    # ------------------------------------------------------------------
    # Parsing helpers
    # ------------------------------------------------------------------

    def _parse_single_range(self, range_header: str, size: int) -> Tuple[int, int]:
        """
        Parse a single HTTP byte range (RFC 7233) and return (start, end_inclusive).
        Raises ValueError for invalid headers.
        """
        if size < 0:
            raise ValueError("invalid size")
        if not range_header:
            raise ValueError("empty Range")
        if "," in range_header:
            raise ValueError("multiple ranges not supported")

        prefix = "bytes="
        if not range_header.startswith(prefix):
            raise ValueError("only bytes ranges supported")
        spec = range_header[len(prefix):].strip()
        if "-" not in spec:
            raise ValueError("invalid bytes range")

        left, right = spec.split("-", 1)
        left = left.strip()
        right = right.strip()

        if left == "":
            if right == "":
                raise ValueError("invalid suffix range")
            try:
                suffix_len = int(right, 10)
            except ValueError as e:
                raise ValueError("invalid suffix length") from e
            if suffix_len <= 0:
                raise ValueError("invalid suffix length")
            if size == 0:
                raise ValueError("unsatisfiable")
            if suffix_len >= size:
                return 0, size - 1
            return size - suffix_len, size - 1

        try:
            start = int(left, 10)
        except ValueError as e:
            raise ValueError("invalid range start") from e
        if start < 0:
            raise ValueError("invalid range start")
        if start >= size:
            raise ValueError("unsatisfiable")

        if right == "":
            return start, size - 1

        try:
            end = int(right, 10)
        except ValueError as e:
            raise ValueError("invalid range end") from e
        if end < start:
            raise ValueError("unsatisfiable")
        if end >= size:
            end = size - 1
        return start, end

    def _parse_route(self) -> Tuple[Optional[str], Optional[str]]:
        path = self.path.split("?", 1)[0]
        parts = [p for p in path.split("/") if p]
        if len(parts) < 2 or parts[0] != "images":
            return None, None
        image_id = parts[1]
        tail = parts[2] if len(parts) >= 3 else None
        if len(parts) > 3:
            return None, None
        return image_id, tail

    def _parse_content_range(self, header: str) -> Tuple[int, int]:
        """
        Parse Content-Range header "bytes start-end/*" or "bytes start-end/size".
        Returns (start, end_inclusive).
        """
        if not header:
            raise ValueError("empty Content-Range")
        m = self._CONTENT_RANGE_RE.match(header.strip())
        if not m:
            raise ValueError("invalid Content-Range")
        start_s, end_s = m.groups()
        start = int(start_s, 10)
        end = int(end_s, 10)
        if start < 0 or end < start:
            raise ValueError("invalid Content-Range range")
        return start, end

    def _parse_query(self) -> Dict[str, List[str]]:
        if "?" not in self.path:
            return {}
        query = self.path.split("?", 1)[1]
        return parse_qs(query, keep_blank_values=True)

    def _image_cfg(self, image_id: str) -> Optional[Dict[str, Any]]:
        return self._registry.get(image_id)

    # ------------------------------------------------------------------
    # HTTP verb dispatchers
    # ------------------------------------------------------------------

    def do_OPTIONS(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None or tail is not None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return
        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return

        with self._registry.request_lifecycle(image_id):
            backend = create_backend(cfg)
            try:
                max_writers = MAX_PARALLEL_WRITES
                if not backend.supports_range_write:
                    max_writers = 1

                if not backend.supports_extents:
                    allowed_methods = "GET, PUT, POST, OPTIONS"
                    features = ["flush"]
                    response = {
                        "unix_socket": None,
                        "features": features,
                        "max_readers": MAX_PARALLEL_READS,
                        "max_writers": max_writers,
                    }
                    self._send_json(HTTPStatus.OK, response, allowed_methods=allowed_methods)
                    return

                read_only = True
                can_flush = False
                can_zero = False
                try:
                    caps = backend.get_capabilities()
                    read_only = caps["read_only"]
                    can_flush = caps["can_flush"]
                    can_zero = caps["can_zero"]
                except Exception as e:
                    logging.warning("OPTIONS: could not query backend capabilities: %r", e)
                    read_only = bool(cfg.get("read_only"))
                    if not read_only:
                        can_flush = True
                        can_zero = True

                if read_only:
                    allowed_methods = "GET, OPTIONS"
                    features = ["extents"]
                    max_writers = 0
                else:
                    allowed_methods = "GET, PUT, PATCH, OPTIONS"
                    features = ["extents"]
                    if can_zero:
                        features.append("zero")
                    if can_flush:
                        features.append("flush")

                response = {
                    "unix_socket": None,
                    "features": features,
                    "max_readers": MAX_PARALLEL_READS,
                    "max_writers": max_writers,
                }
                self._send_json(HTTPStatus.OK, response, allowed_methods=allowed_methods)
            finally:
                backend.close()

    def do_GET(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return

        if tail == "extents":
            with self._registry.request_lifecycle(image_id):
                query = self._parse_query()
                context = (query.get("context") or [None])[0]
                self._handle_get_extents(image_id, cfg, context=context)
            return
        if tail is not None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        range_header = self.headers.get("Range")
        with self._registry.request_lifecycle(image_id):
            self._handle_get_image(image_id, cfg, range_header)

    def do_PUT(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None or tail is not None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return

        with self._registry.request_lifecycle(image_id):
            if self.headers.get("Range") is not None:
                self._send_error_json(
                    HTTPStatus.BAD_REQUEST,
                    "Range header not supported for PUT; use Content-Range or PATCH",
                )
                return

            content_length_hdr = self.headers.get("Content-Length")
            if content_length_hdr is None:
                self._send_error_json(HTTPStatus.BAD_REQUEST, "Content-Length required")
                return
            try:
                content_length = int(content_length_hdr)
            except ValueError:
                self._send_error_json(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
                return
            if content_length < 0:
                self._send_error_json(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
                return

            query = self._parse_query()
            flush_param = (query.get("flush") or ["n"])[0].lower()
            flush = flush_param in ("y", "yes", "true", "1")

            content_range_hdr = self.headers.get("Content-Range")
            if content_range_hdr is not None:
                backend = create_backend(cfg)
                try:
                    if not backend.supports_range_write:
                        self._send_error_json(
                            HTTPStatus.BAD_REQUEST,
                            "Content-Range PUT not supported for file backend; use full PUT",
                        )
                        return
                    self._handle_put_range_with_backend(
                        image_id,
                        backend,
                        content_range_hdr,
                        content_length,
                        flush,
                    )
                finally:
                    backend.close()
                return

            self._handle_put_image(image_id, cfg, content_length, flush)

    def do_POST(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return

        if tail == "flush":
            with self._registry.request_lifecycle(image_id):
                self._handle_post_flush(image_id, cfg)
            return
        self._send_error_json(HTTPStatus.NOT_FOUND, "not found")

    def do_PATCH(self) -> None:
        image_id, tail = self._parse_route()
        if image_id is None or tail is not None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "not found")
            return

        cfg = self._image_cfg(image_id)
        if cfg is None:
            self._send_error_json(HTTPStatus.NOT_FOUND, "unknown image_id")
            return

        with self._registry.request_lifecycle(image_id):
            backend = create_backend(cfg)
            try:
                if not backend.supports_range_write:
                    self._send_error_json(
                        HTTPStatus.BAD_REQUEST,
                        "range writes and PATCH not supported for file backend; use PUT for full upload",
                    )
                    return
                content_type = self.headers.get("Content-Type", "").split(";")[0].strip().lower()
                range_header = self.headers.get("Range")

                if range_header is not None and content_type != "application/json":
                    content_length_hdr = self.headers.get("Content-Length")
                    if content_length_hdr is None:
                        self._send_error_json(HTTPStatus.BAD_REQUEST, "Content-Length required")
                        return
                    try:
                        content_length = int(content_length_hdr)
                    except ValueError:
                        self._send_error_json(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
                        return
                    if content_length <= 0:
                        self._send_error_json(HTTPStatus.BAD_REQUEST, "Content-Length must be positive")
                        return
                    self._handle_patch_range_with_backend(
                        image_id,
                        backend,
                        range_header,
                        content_length,
                    )
                    return

                if content_type != "application/json":
                    self._send_error_json(
                        HTTPStatus.UNSUPPORTED_MEDIA_TYPE,
                        "PATCH requires Content-Type: application/json (for zero/flush) or Range with binary body",
                    )
                    return

                content_length_hdr = self.headers.get("Content-Length")
                if content_length_hdr is None:
                    self._send_error_json(HTTPStatus.BAD_REQUEST, "Content-Length required")
                    return
                try:
                    content_length = int(content_length_hdr)
                except ValueError:
                    self._send_error_json(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
                    return
                if content_length <= 0 or content_length > MAX_PATCH_JSON_SIZE:
                    self._send_error_json(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
                    return

                body = self.rfile.read(content_length)
                if len(body) != content_length:
                    self._send_error_json(HTTPStatus.BAD_REQUEST, "request body truncated")
                    return

                try:
                    payload = json.loads(body.decode("utf-8"))
                except (json.JSONDecodeError, UnicodeDecodeError) as e:
                    self._send_error_json(HTTPStatus.BAD_REQUEST, f"invalid JSON: {e}")
                    return

                if not isinstance(payload, dict):
                    self._send_error_json(HTTPStatus.BAD_REQUEST, "body must be a JSON object")
                    return

                op = payload.get("op")
                if op == "flush":
                    self._handle_post_flush_with_backend(image_id, backend)
                    return
                if op != "zero":
                    self._send_error_json(
                        HTTPStatus.BAD_REQUEST,
                        "unsupported op; only \"zero\" and \"flush\" are supported",
                    )
                    return

                try:
                    size = int(payload.get("size"))
                except (TypeError, ValueError):
                    self._send_error_json(HTTPStatus.BAD_REQUEST, "missing or invalid \"size\"")
                    return
                if size <= 0:
                    self._send_error_json(HTTPStatus.BAD_REQUEST, "\"size\" must be positive")
                    return

                offset = payload.get("offset")
                if offset is None:
                    offset = 0
                else:
                    try:
                        offset = int(offset)
                    except (TypeError, ValueError):
                        self._send_error_json(HTTPStatus.BAD_REQUEST, "invalid \"offset\"")
                        return
                    if offset < 0:
                        self._send_error_json(HTTPStatus.BAD_REQUEST, "\"offset\" must be non-negative")
                        return

                flush = bool(payload.get("flush", False))
                self._handle_patch_zero_with_backend(
                    image_id,
                    backend,
                    offset=offset,
                    size=size,
                    flush=flush,
                )
            finally:
                backend.close()

    # ------------------------------------------------------------------
    # Operation handlers
    # ------------------------------------------------------------------

    def _handle_get_image(
        self, image_id: str, cfg: Dict[str, Any], range_header: Optional[str]
    ) -> None:
        start = now_s()
        bytes_sent = 0
        expected_bytes = 0
        try:
            logging.info("GET start image_id=%s range=%s", image_id, range_header or "-")
            backend = create_backend(cfg)
            session = None
            try:
                session = backend.open_session()
                size = session.size()
            except OSError as e:
                logging.error("GET size error image_id=%s err=%r", image_id, e)
                self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "failed to access image")
                if session is not None:
                    session.close()
                backend.close()
                return

            try:
                start_off = 0
                end_off_incl = size - 1 if size > 0 else -1
                status = HTTPStatus.OK
                content_length = size
                if range_header is not None:
                    try:
                        start_off, end_off_incl = self._parse_single_range(range_header, size)
                    except ValueError as e:
                        if "unsatisfiable" in str(e):
                            self._send_range_not_satisfiable(size)
                            return
                        self._send_error_json(HTTPStatus.BAD_REQUEST, "invalid Range header")
                        return
                    status = HTTPStatus.PARTIAL_CONTENT
                    content_length = (end_off_incl - start_off) + 1
                expected_bytes = content_length

                self.send_response(status)
                self._send_imageio_headers()
                self.send_header("Content-Type", "application/octet-stream")
                self.send_header("Content-Length", str(content_length))
                if status == HTTPStatus.PARTIAL_CONTENT:
                    self.send_header("Content-Range", f"bytes {start_off}-{end_off_incl}/{size}")
                self.end_headers()

                offset = start_off
                end_excl = end_off_incl + 1
                while offset < end_excl:
                    to_read = min(CHUNK_SIZE, end_excl - offset)
                    data = session.read(offset, to_read)
                    if not data:
                        logging.error(
                            "GET short read image_id=%s expected_bytes=%d sent_bytes=%d offset=%d",
                            image_id,
                            expected_bytes,
                            bytes_sent,
                            offset,
                        )
                        self.close_connection = True
                        break
                    try:
                        self.wfile.write(data)
                    except BrokenPipeError:
                        logging.error(
                            "GET client disconnected image_id=%s at=%d expected_bytes=%d sent_bytes=%d",
                            image_id,
                            offset,
                            expected_bytes,
                            bytes_sent,
                        )
                        self.close_connection = True
                        break
                    offset += len(data)
                    bytes_sent += len(data)
            finally:
                session.close()
                backend.close()
        except Exception as e:
            logging.error("GET error image_id=%s err=%r", image_id, e)
            try:
                if not self.wfile.closed:
                    self.close_connection = True
            except Exception:
                pass
        finally:
            if expected_bytes > 0 and bytes_sent < expected_bytes:
                logging.error(
                    "GET incomplete image_id=%s expected_bytes=%d sent_bytes=%d",
                    image_id,
                    expected_bytes,
                    bytes_sent,
                )
            dur = now_s() - start
            logging.info(
                "GET end image_id=%s bytes=%d duration_s=%.3f", image_id, bytes_sent, dur
            )

    def _handle_put_image(
        self, image_id: str, cfg: Dict[str, Any], content_length: int, flush: bool
    ) -> None:
        start = now_s()
        bytes_written = 0
        try:
            logging.info("PUT start image_id=%s content_length=%d", image_id, content_length)
            backend = create_backend(cfg)
            try:
                bytes_written = backend.write_full(self.rfile, content_length, flush)
                self._send_json(
                    HTTPStatus.OK,
                    {"ok": True, "bytes_written": bytes_written, "flushed": flush},
                )
            except IOError as e:
                self._send_error_json(HTTPStatus.BAD_REQUEST, str(e))
            finally:
                backend.close()
        except Exception as e:
            logging.error("PUT error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            dur = now_s() - start
            logging.info(
                "PUT end image_id=%s bytes=%d duration_s=%.3f", image_id, bytes_written, dur
            )

    def _handle_put_range(
        self,
        image_id: str,
        cfg: Dict[str, Any],
        content_range: str,
        content_length: int,
        flush: bool,
    ) -> None:
        backend = create_backend(cfg)
        try:
            self._handle_put_range_with_backend(
                image_id,
                backend,
                content_range,
                content_length,
                flush,
            )
        finally:
            backend.close()

    def _handle_put_range_with_backend(
        self,
        image_id: str,
        backend: NbdBackend,
        content_range: str,
        content_length: int,
        flush: bool,
    ) -> None:
        start = now_s()
        bytes_written = 0
        try:
            logging.info(
                "PUT range start image_id=%s Content-Range=%s content_length=%d flush=%s",
                image_id, content_range, content_length, flush,
            )
            try:
                start_off, _end_inclusive = self._parse_content_range(content_range)
            except ValueError as e:
                self._send_error_json(
                    HTTPStatus.BAD_REQUEST, f"invalid Content-Range header: {e}"
                )
                return

            try:
                bytes_written = backend.write_range(self.rfile, start_off, content_length)
                if flush:
                    backend.flush()
                self._send_json(
                    HTTPStatus.OK,
                    {"ok": True, "bytes_written": bytes_written, "flushed": flush},
                )
            except ValueError:
                image_size = backend.size()
                self._send_range_not_satisfiable(image_size)
            except IOError as e:
                self._send_error_json(HTTPStatus.BAD_REQUEST, str(e))
        except Exception as e:
            logging.error("PUT range error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            dur = now_s() - start
            logging.info(
                "PUT range end image_id=%s bytes=%d duration_s=%.3f flush=%s",
                image_id, bytes_written, dur, flush,
            )

    def _handle_get_extents(
        self, image_id: str, cfg: Dict[str, Any], context: Optional[str] = None
    ) -> None:
        backend = create_backend(cfg)
        try:
            self._handle_get_extents_with_backend(image_id, backend, context=context)
        finally:
            backend.close()

    def _handle_get_extents_with_backend(
        self, image_id: str, backend: NbdBackend, context: Optional[str] = None
    ) -> None:
        start = now_s()
        try:
            logging.info("EXTENTS start image_id=%s context=%s", image_id, context)
            if not backend.supports_extents:
                self._send_error_json(
                    HTTPStatus.BAD_REQUEST, "extents not supported for file backend"
                )
                return
            if context == "dirty":
                export_bitmap = backend.export_bitmap
                if not export_bitmap:
                    allocation = backend.get_allocation_extents()
                    extents: List[Dict[str, Any]] = [
                        {"start": e["start"], "length": e["length"], "dirty": True, "zero": e["zero"]}
                        for e in allocation
                    ]
                else:
                    dirty_bitmap_ctx = f"qemu:dirty-bitmap:{export_bitmap}"
                    extents = backend.get_dirty_extents(dirty_bitmap_ctx)
                    if is_fallback_dirty_response(extents):
                        allocation = backend.get_allocation_extents()
                        extents = [
                            {
                                "start": e["start"],
                                "length": e["length"],
                                "dirty": True,
                                "zero": e["zero"],
                            }
                            for e in allocation
                        ]
            else:
                extents = backend.get_allocation_extents()
            self._send_json(HTTPStatus.OK, extents)
        except Exception as e:
            logging.error("EXTENTS error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            dur = now_s() - start
            logging.info("EXTENTS end image_id=%s duration_s=%.3f", image_id, dur)

    def _handle_post_flush(self, image_id: str, cfg: Dict[str, Any]) -> None:
        backend = create_backend(cfg)
        try:
            self._handle_post_flush_with_backend(image_id, backend)
        finally:
            backend.close()

    def _handle_post_flush_with_backend(self, image_id: str, backend: NbdBackend) -> None:
        start = now_s()
        try:
            logging.info("FLUSH start image_id=%s", image_id)
            backend.flush()
            self._send_json(HTTPStatus.OK, {"ok": True})
        except Exception as e:
            logging.error("FLUSH error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            dur = now_s() - start
            logging.info("FLUSH end image_id=%s duration_s=%.3f", image_id, dur)

    def _handle_patch_zero(
        self,
        image_id: str,
        cfg: Dict[str, Any],
        offset: int,
        size: int,
        flush: bool,
    ) -> None:
        backend = create_backend(cfg)
        try:
            self._handle_patch_zero_with_backend(image_id, backend, offset, size, flush)
        finally:
            backend.close()

    def _handle_patch_zero_with_backend(
        self,
        image_id: str,
        backend: NbdBackend,
        offset: int,
        size: int,
        flush: bool,
    ) -> None:
        start = now_s()
        try:
            logging.info(
                "PATCH zero start image_id=%s offset=%d size=%d flush=%s",
                image_id, offset, size, flush,
            )
            backend.zero(offset, size)
            if flush:
                backend.flush()
            self._send_json(HTTPStatus.OK, {"ok": True})
        except ValueError as e:
            self._send_error_json(HTTPStatus.BAD_REQUEST, str(e))
        except Exception as e:
            logging.error("PATCH zero error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            dur = now_s() - start
            logging.info("PATCH zero end image_id=%s duration_s=%.3f", image_id, dur)

    def _handle_patch_range(
        self,
        image_id: str,
        cfg: Dict[str, Any],
        range_header: str,
        content_length: int,
    ) -> None:
        backend = create_backend(cfg)
        try:
            self._handle_patch_range_with_backend(
                image_id,
                backend,
                range_header,
                content_length,
            )
        finally:
            backend.close()

    def _handle_patch_range_with_backend(
        self,
        image_id: str,
        backend: NbdBackend,
        range_header: str,
        content_length: int,
    ) -> None:
        start = now_s()
        bytes_written = 0
        try:
            logging.info(
                "PATCH range start image_id=%s range=%s content_length=%d",
                image_id, range_header, content_length,
            )
            try:
                image_size = backend.size()
                try:
                    start_off, end_inclusive = self._parse_single_range(
                        range_header, image_size
                    )
                except ValueError as e:
                    if "unsatisfiable" in str(e).lower():
                        self._send_range_not_satisfiable(image_size)
                    else:
                        self._send_error_json(
                            HTTPStatus.BAD_REQUEST, f"invalid Range header: {e}"
                        )
                    return
                expected_len = end_inclusive - start_off + 1
                if content_length != expected_len:
                    self._send_error_json(
                        HTTPStatus.BAD_REQUEST,
                        f"Content-Length ({content_length}) must equal range length ({expected_len})",
                    )
                    return
                nbd_backend: NbdBackend = backend  # type: ignore[assignment]
                bytes_written = nbd_backend.write_range(self.rfile, start_off, content_length)
                self._send_json(HTTPStatus.OK, {"ok": True, "bytes_written": bytes_written})
            except ValueError:
                image_size = backend.size()
                self._send_range_not_satisfiable(image_size)
            except IOError as e:
                self._send_error_json(HTTPStatus.BAD_REQUEST, str(e))
        except Exception as e:
            logging.error("PATCH range error image_id=%s err=%r", image_id, e)
            self._send_error_json(HTTPStatus.INTERNAL_SERVER_ERROR, "backend error")
        finally:
            dur = now_s() - start
            logging.info(
                "PATCH range end image_id=%s bytes=%d duration_s=%.3f",
                image_id, bytes_written, dur,
            )
