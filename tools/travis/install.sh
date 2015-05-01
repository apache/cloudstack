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

export CATALINA_BASE=/opt/tomcat
export CATALINA_HOME=/opt/tomcat
export M2_HOME="/usr/local/maven-3.2.1/"
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=500m"

# Compile Cloudstack
mvn -q -Pimpatient -Dsimulator clean install -DskipTests=true

# Compile API Docs
cd tools/apidoc
mvn -q clean install
cd ../../

# Compile marvin
cd tools/marvin
mvn -q clean install
sudo python setup.py install 2>&1 > /dev/null
cd ../../

# Deploy the database
mvn -q -Pdeveloper -pl developer -Ddeploydb
mvn -q -Pdeveloper -pl developer -Ddeploydb-simulator

