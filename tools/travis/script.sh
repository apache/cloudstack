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
#
# This script should be responsible for bring up the
# test environment and executing the tests.
#

export TEST_JOB_NUMBER=`echo $TRAVIS_JOB_NUMBER | cut -d. -f1`
export TEST_SEQUENCE_NUMBER=`echo $TRAVIS_JOB_NUMBER | cut -d. -f2`

#run regression test only on $REGRESSION_CYCLE
MOD=$(( $TEST_JOB_NUMBER % $REGRESSION_CYCLE ))

if [ $MOD -ne 0 ]; then
 if [ $TEST_SEQUENCE_NUMBER -ge $REGRESSION_INDEX ]; then
   #skip test
   echo "Skipping tests ... SUCCESS !"
   exit 0
 fi
fi

mkdir -p integration-test-results/smoke/misc
mkdir -p integration-test-results/component


for suite in $1; do
  nosetests --with-xunit --xunit-file=integration-test-results/$suite.xml --with-marvin --marvin-config=setup/dev/advanced.cfg test/integration/$suite.py -s -a tags=advanced,required_hardware=false --zone=Sandbox-simulator --hypervisor=simulator || true ;
done


python ./tools/travis/xunit-reader.py integration-test-results/
