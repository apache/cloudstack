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
# This script should be used to bring up the environment.
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


export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=500m -Djava.security.egd=file:/dev/./urandom"
echo -e "\nStarting simulator"
mvn -Dsimulator -pl :cloud-client-ui jetty:run 2>&1 > /tmp/jetty-log &

while ! nc -vzw 5 localhost 8096 2>&1 > /dev/null; do grep Exception /tmp/jetty-log; sleep 10; done
echo -e "\nStarting DataCenter deployment"
python -m marvin.deployDataCenter -i setup/dev/advanced.cfg 2>&1 || true
