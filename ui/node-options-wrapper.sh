#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Webpack 4 hashes with MD4, which OpenSSL 3 (Node 17+) no longer provides
# by default, causing ERR_OSSL_EVP_UNSUPPORTED. --openssl-legacy-provider
# restores it, but the flag doesn't exist before Node 17 and node refuses to
# even start with it set in NODE_OPTIONS, so only add it when needed.
NODE_MAJOR=$(node -p "process.versions.node.split('.')[0]")
if [ "$NODE_MAJOR" -ge 17 ]; then
  export NODE_OPTIONS="--openssl-legacy-provider ${NODE_OPTIONS}"
fi
exec "$@"
