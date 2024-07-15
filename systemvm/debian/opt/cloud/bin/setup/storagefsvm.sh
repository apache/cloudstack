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

. /opt/cloud/bin/setup/common.sh

setup_storagefsvm() {
    log_it "Starting cloud-init services"
    log_it "Setting up storagefsvm"

    if [ -f /home/cloud/success ]; then
      systemctl stop cloud-init cloud-config cloud-final
      systemctl disable cloud-init cloud-config cloud-final
    else
      systemctl start --no-block cloud-init
      systemctl start --no-block cloud-config
      systemctl start --no-block cloud-final
    fi
}

setup_storagefsvm
