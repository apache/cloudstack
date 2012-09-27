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



 

set -e
DST='../src/'
#java -cp ${DST}commons-httpclient-3.1.jar:${DST}mysql-connector-java-5.1.7-bin.jar:${DST}commons-logging-1.1.1.jar:${DST}commons-codec-1.3.jar:${DST}cloud-test.jar:${DST}log4j-1.2.15.jar:${DST}trilead-ssh2-build213.jar:${DST}cloud-utils.jar:.././conf com.cloud.test.utils.SignRequest $*

java -cp ${DST}commons-httpclient-3.1.jar:${DST}mysql-connector-java-5.1.7-bin.jar:${DST}commons-logging-1.1.1.jar:${DST}commons-codec-1.3.jar:${DST}cloud-test.jar:${DST}log4j-1.2.15.jar:${DST}trilead-ssh2-build213.jar:${DST}cloud-utils.jar:.././conf com.cloud.test.utils.SignEC2 $*
