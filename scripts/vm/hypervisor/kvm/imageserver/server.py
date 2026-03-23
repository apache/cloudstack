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
import logging
from http.server import HTTPServer
from socketserver import ThreadingMixIn
from typing import Type

try:
    from http.server import ThreadingHTTPServer
except ImportError:
    class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):  # type: ignore[no-redef]
        pass

from .concurrency import ConcurrencyManager
from .config import TransferConfigLoader
from .constants import CFG_DIR, MAX_PARALLEL_READS, MAX_PARALLEL_WRITES
from .handler import Handler


def make_handler(
    concurrency: ConcurrencyManager,
    config_loader: TransferConfigLoader,
) -> Type[Handler]:
    """
    Create a Handler subclass with injected dependencies.

    BaseHTTPRequestHandler is instantiated per-request by the server, so we
    cannot pass constructor args.  Instead we set class-level attributes.
    """

    class ConfiguredHandler(Handler):
        _concurrency = concurrency
        _config_loader = config_loader

    return ConfiguredHandler


def main() -> None:
    parser = argparse.ArgumentParser(
        description="CloudStack image server backed by NBD / local file"
    )
    parser.add_argument("--listen", default="127.0.0.1", help="Address to bind")
    parser.add_argument("--port", type=int, default=54323, help="Port to listen on")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )

    concurrency = ConcurrencyManager(MAX_PARALLEL_READS, MAX_PARALLEL_WRITES)
    config_loader = TransferConfigLoader(CFG_DIR)
    handler_cls = make_handler(concurrency, config_loader)

    addr = (args.listen, args.port)
    httpd = ThreadingHTTPServer(addr, handler_cls)
    logging.info("listening on http://%s:%d", args.listen, args.port)
    logging.info("image configs are read from %s/<transferId>", config_loader.cfg_dir)
    httpd.serve_forever()
