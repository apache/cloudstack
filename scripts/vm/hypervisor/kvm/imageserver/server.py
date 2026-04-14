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

import argparse
import json
import logging
import os
import socket
import ssl
import threading
import time
from http.server import HTTPServer
from socketserver import ThreadingMixIn
from typing import Type

try:
    from http.server import ThreadingHTTPServer
except ImportError:
    class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):  # type: ignore[no-redef]
        pass

from .config import TransferRegistry, validate_transfer_config
from .constants import (
    CONTROL_RECV_BUFFER,
    CONTROL_SOCKET,
    CONTROL_SOCKET_BACKLOG,
    CONTROL_SOCKET_PERMISSIONS,
    DEFAULT_HTTP_PORT,
    DEFAULT_LISTEN_ADDRESS,
)
from .handler import Handler


def make_handler(
    registry: TransferRegistry,
) -> Type[Handler]:
    """
    Create a Handler subclass with injected dependencies.

    BaseHTTPRequestHandler is instantiated per-request by the server, so we
    cannot pass constructor args. Instead, we set class-level attributes.
    """

    class ConfiguredHandler(Handler):
        _registry = registry

    return ConfiguredHandler


def _handle_control_conn(conn: socket.socket, registry: TransferRegistry) -> None:
    """Handle a single control-socket connection (one JSON request/response)."""
    try:
        data = b""
        while True:
            chunk = conn.recv(CONTROL_RECV_BUFFER)
            if not chunk:
                break
            data += chunk
            if b"\n" in data:
                break

        msg = json.loads(data.strip())
        action = msg.get("action")

        if action == "register":
            transfer_id = msg.get("transfer_id")
            raw_config = msg.get("config")
            if not transfer_id or not isinstance(raw_config, dict):
                resp = {"status": "error", "message": "missing transfer_id or config"}
            else:
                try:
                    config = validate_transfer_config(raw_config)
                except ValueError as e:
                    resp = {"status": "error", "message": str(e)}
                else:
                    if registry.register(transfer_id, config):
                        resp = {"status": "ok", "active_transfers": registry.active_count()}
                    else:
                        resp = {"status": "error", "message": "invalid transfer_id"}
        elif action == "unregister":
            transfer_id = msg.get("transfer_id")
            if not transfer_id:
                resp = {"status": "error", "message": "missing transfer_id"}
            else:
                remaining = registry.unregister(transfer_id)
                resp = {"status": "ok", "active_transfers": remaining}
        elif action == "status":
            resp = {"status": "ok", "active_transfers": registry.active_count()}
        else:
            resp = {"status": "error", "message": f"unknown action: {action}"}

        conn.sendall((json.dumps(resp) + "\n").encode("utf-8"))
    except Exception as e:
        logging.error("control socket error: %r", e)
        try:
            conn.sendall((json.dumps({"status": "error", "message": str(e)}) + "\n").encode("utf-8"))
        except Exception:
            pass
    finally:
        conn.close()


def _idle_sweep_loop(registry: TransferRegistry, interval_s: float = 10.0) -> None:
    while True:
        time.sleep(interval_s)
        try:
            registry.sweep_expired_transfers()
        except Exception:
            logging.exception("idle sweep error")


def _control_listener(registry: TransferRegistry, sock_path: str) -> None:
    """Accept loop for the Unix domain control socket (runs in a daemon thread)."""
    if os.path.exists(sock_path):
        os.unlink(sock_path)
    os.makedirs(os.path.dirname(sock_path), exist_ok=True)

    srv = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    srv.bind(sock_path)
    os.chmod(sock_path, CONTROL_SOCKET_PERMISSIONS)
    srv.listen(CONTROL_SOCKET_BACKLOG)
    logging.info("control socket listening on %s", sock_path)

    while True:
        conn, _ = srv.accept()
        threading.Thread(
            target=_handle_control_conn,
            args=(conn, registry),
            daemon=True,
        ).start()


def main() -> None:
    parser = argparse.ArgumentParser(
        description="CloudStack image server backed by NBD / local file"
    )
    parser.add_argument("--listen", default=DEFAULT_LISTEN_ADDRESS, help="Address to bind")
    parser.add_argument("--port", type=int, default=DEFAULT_HTTP_PORT, help="Port to listen on")
    parser.add_argument(
        "--control-socket",
        default=CONTROL_SOCKET,
        help="Path to the Unix domain control socket",
    )
    parser.add_argument(
        "--tls-enabled",
        action="store_true",
        help="Enable TLS for the HTTP transfer endpoint",
    )
    parser.add_argument(
        "--tls-cert-file",
        default=None,
        help="Path to PEM certificate file used when TLS is enabled",
    )
    parser.add_argument(
        "--tls-key-file",
        default=None,
        help="Path to PEM private key file used when TLS is enabled",
    )
    args = parser.parse_args()

    if args.tls_enabled and (not args.tls_cert_file or not args.tls_key_file):
        parser.error("--tls-enabled requires --tls-cert-file and --tls-key-file")

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )

    registry = TransferRegistry()
    handler_cls = make_handler(registry)

    ctrl_thread = threading.Thread(
        target=_control_listener,
        args=(registry, args.control_socket),
        daemon=True,
    )
    ctrl_thread.start()

    sweep_thread = threading.Thread(
        target=_idle_sweep_loop,
        args=(registry,),
        daemon=True,
    )
    sweep_thread.start()

    addr = (args.listen, args.port)
    httpd = ThreadingHTTPServer(addr, handler_cls)

    scheme = "http"
    if args.tls_enabled:
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)

        if hasattr(ssl, "TLSVersion") and hasattr(context, "minimum_version"):
            context.minimum_version = ssl.TLSVersion.TLSv1_2
        else:
            if hasattr(ssl, "OP_NO_TLSv1"):
                context.options |= ssl.OP_NO_TLSv1
            if hasattr(ssl, "OP_NO_TLSv1_1"):
                context.options |= ssl.OP_NO_TLSv1_1

        context.load_cert_chain(certfile=args.tls_cert_file, keyfile=args.tls_key_file)

        httpd.socket = context.wrap_socket(httpd.socket, server_side=True)
        scheme = "https"

    logging.info("listening on %s://%s:%d", scheme, args.listen, args.port)
    httpd.serve_forever()
