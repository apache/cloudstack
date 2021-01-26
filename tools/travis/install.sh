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

export TEST_SEQUENCE_NUMBER=`echo $TRAVIS_JOB_NUMBER | cut -d. -f2`
export MAVEN_OPTS="-Xmx4096m -XX:MaxPermSize=800m -Djava.security.egd=file:/dev/./urandom"

set -e

DIR=$(pwd)

cd ~
wget https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar zxvf apache-maven-3.6.3-bin.tar.gz
export PATH=`pwd`/apache-maven-3.6.3/bin:$PATH
cd $DIR

echo -e "\nJDK version"
export JAVA_HOME=$(readlink -f /usr/lib/jvm/java-11-openjdk-amd64/bin/java | sed "s:bin/java::")
mvn -v

if [ $TEST_SEQUENCE_NUMBER -eq 1 ]; then
   # npm lint, test and build
   cd ui && npm install && npm run lint && npm run test:unit && npm run build
   cd $DIR
   # Pylint/pep8 systemvm python codebase
   cd systemvm/test && bash -x runtests.sh
   # Build noredist
   git clone https://github.com/rhtyd/cloudstack-nonoss.git nonoss && cd nonoss && bash -x install-non-oss.sh
   cd $DIR && echo $DIR
   git clean -fdx .
   # Perform rat checks
   mvn -P developer,systemvm -Dsimulator -Dnoredist -pl . org.apache.rat:apache-rat-plugin:0.12:check
   mvn -q -B -P developer,systemvm -Dsimulator -Dnoredist clean install
else
   mvn -Pdeveloper -Dsimulator clean install -DskipTests=true -T4
fi

# Install mysql-connector-python
python3 -m pip install --user --upgrade http://cdn.mysql.com/Downloads/Connector-Python/mysql-connector-python-2.0.4.zip#md5=3df394d89300db95163f17c843ef49df 2>&1 > /dev/null

# Install marvin
python3 -m pip install --user --upgrade tools/marvin/dist/Marvin-*.tar.gz

# Deploy the database
mvn -q -Pdeveloper -pl developer -Ddeploydb
mvn -q -Pdeveloper -pl developer -Ddeploydb-simulator
