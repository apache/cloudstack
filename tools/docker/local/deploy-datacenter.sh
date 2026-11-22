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

set -e
cd /root

zones=$(mysql -h mysql -ucloud -pcloud -N -e 'SELECT COUNT(*) FROM cloud.data_center WHERE removed IS NULL')
if [ "$zones" != "0" ]; then
    echo "Zone already deployed, skipping"
    exit 0
fi

sed -e 's/"mgtSvrIp": "localhost"/"mgtSvrIp": "management"/' \
    -e 's/"dbSvr": "localhost"/"dbSvr": "mysql"/' \
    setup/dev/advanced.cfg > /tmp/advanced.cfg

python3 tools/marvin/marvin/deployDataCenter.py -i /tmp/advanced.cfg
