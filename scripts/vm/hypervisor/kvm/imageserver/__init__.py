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
CloudStack image server — HTTP server backed by NBD over Unix socket or a local file.

Transfer configs are registered/unregistered by the cloudstack-agent via a
Unix domain control socket (default: /var/run/cloudstack/image-server.sock)
and stored in-memory for the lifetime of the server process.

Supports two backends (configured per-transfer at registration time):
- nbd: proxy to an NBD server via Unix socket; supports range reads/writes
  (GET/PUT/PATCH), extents, zero, flush.
- file: read/write a local qcow2/raw file; full PUT only, GET with optional
  ranges, flush.

Run as a systemd service by the CreateImageTransfer CloudStack Agent Command
"""
