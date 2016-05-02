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
# This should be used to create the build.
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

export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=500m -Djava.security.egd=file:/dev/./urandom"

if [ $TEST_SEQUENCE_NUMBER -eq 1 ]; then
   mvn -Pdeveloper,systemvm -Dsimulator clean install -T4 | egrep "Building|Tests|SUCCESS|FAILURE"
else
   mvn -Pdeveloper -Dsimulator clean install -DskipTests=true -T4 | egrep "Building|Tests|SUCCESS|FAILURE"
fi

# Install mysql-connector-python
pip install --user --upgrade http://cdn.mysql.com/Downloads/Connector-Python/mysql-connector-python-2.0.4.zip#md5=3df394d89300db95163f17c843ef49df 2>&1 > /dev/null

# Install marvin
pip install --user --upgrade tools/marvin/dist/Marvin-*.tar.gz

# Deploy the database
mvn -q -Pdeveloper -pl developer -Ddeploydb
mvn -q -Pdeveloper -pl developer -Ddeploydb-simulator
