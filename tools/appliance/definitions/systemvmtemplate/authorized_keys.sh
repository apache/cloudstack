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

# override this file during build to inject /root/.ssh/authorized_keys

set -e
set -x

# the key that we have in ../patches/debian/config/root/.ssh/authorized_keys for some reason
key='ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAvFu3MLSPphFRBR1yM7nBukXWS9gPdAXfqq9cfC8ZqQN9ybi531aj44CybZ4BVT4kLfzbAs7+7nJeSIpPHxjv9XFqbxjIxoFeGYkj7s0RrJgtsEmvAAubZ3mYboUAYUivMgnJFLnv4VqyAbpjix6CfECUiU4ygwo24F3F6bAmhl4Vo1R5TSUdDIX876YePJTFtuVkLl4lu/+xw1QRWrgaSFosGICT37IKY7RjE79Ozb0GjNHyJPPgVAGkUVO4LawroL9dYOBlzdHpmqqA9Kc44oQBpvcU7s1+ezRTt7fZNnP7TG9ninZtrvnP4qmwAc4iUJ7N1bwh0mCblnoTfZ28hw== anthony@mobl-ant'
mkdir -p /root/.ssh
chmod 644 /root/.ssh
echo ${key}  > /root/.ssh/authorized_keys
chmod 600 /root/.ssh/authorized_keys
