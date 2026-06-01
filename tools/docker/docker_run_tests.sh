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

MODE=${1:-advanced}
SUITE=${2:-smoke}

export MARVIN_CONFIG=setup/dev/$MODE.cfg
export TEST_SUITE=test/integration/$SUITE
export ZONE_NAME=Sandbox-simulator

cd /root

python tools/marvin/marvin/deployDataCenter.py -i setup/dev/$MODE.cfg

cat <<EOF

RUN WHOLE '$SUITE' SUITE
--------------------------
nosetests-2.7 \
  --with-marvin \
  --marvin-config=${MARVIN_CONFIG} \
  -w ${TEST_SUITE} \
  --with-xunit \
  --xunit-file=/tmp/bvt_selfservice_cases.xml \
  --zone=${ZONE_NAME} \
  --hypervisor=simulator \
  -a tags=$MODE,required_hardware=false
--------------------------
OR INDIVIDUAL TEST LIKE
--------------------------
nosetests-2.7 -s --with-marvin --marvin-config=${MARVIN_CONFIG} --zone=${ZONE_NAME} \
    --hypervisor=simulator -a tags=$MODE,required_hardware=false \
    test/integration/smoke/test_accounts.py:TestAccounts
--------------------------
EOF
