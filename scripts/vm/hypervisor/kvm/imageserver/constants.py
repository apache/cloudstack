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

CHUNK_SIZE = 256 * 1024  # 256 KiB

# NBD base:allocation flags (hole=1, zero=2; hole|zero=3)
NBD_STATE_HOLE = 1
NBD_STATE_ZERO = 2
# NBD qemu:dirty-bitmap flags (dirty=1)
NBD_STATE_DIRTY = 1

MAX_PARALLEL_READS = 8
MAX_PARALLEL_WRITES = 1

# HTTP server defaults
DEFAULT_LISTEN_ADDRESS = "127.0.0.1"
DEFAULT_HTTP_PORT = 54323

# Control socket
CONTROL_SOCKET = "/var/run/cloudstack/image-server.sock"
CONTROL_SOCKET_BACKLOG = 32
CONTROL_SOCKET_PERMISSIONS = 0o660
CONTROL_RECV_BUFFER = 4096

# Maximum size of a JSON body in a PATCH request (zero / flush ops)
MAX_PATCH_JSON_SIZE = 64 * 1024  # 64 KiB

# Byte range requested per block_status call for NBD extent queries
NBD_BLOCK_STATUS_CHUNK = 64 * 1024 * 1024  # 64 MiB

CFG_DIR = "/tmp/imagetransfer"
