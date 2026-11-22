#!/bin/bash
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

# Generates a locally trusted wildcard certificate for docker-compose.yml.
# Requires mkcert: https://github.com/FiloSottile/mkcert

set -e

domain=$1
# Browsers reject wildcards directly under a TLD, hence the dot
if [[ "$domain" != *.* ]]; then
    echo "Usage: $0 <domain>   (e.g. $0 cloud.test)" >&2
    exit 1
fi

cd "$(dirname "$0")"
mkdir -p certs

mkcert -install
mkcert -cert-file certs/tls.crt -key-file certs/tls.key "*.$domain" "$domain"

echo
echo "Add this line to /etc/hosts:"
echo "127.0.0.1 cloudstack.$domain"
