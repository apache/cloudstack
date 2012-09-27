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



 

PATHSEP=':'
if [[ $OSTYPE == "cygwin" ]] ; then
  PATHSEP=';'
fi

CP=commons-httpclient-3.1.jar${PATHSEP}commons-logging-1.1.1.jar${PATHSEP}commons-codec-1.3.jar${PATHSEP}testclient.jar${PATHSEP}log4j-1.2.15.jar${PATHSEP}utils.jar${PATHSEP}./conf
java -cp $CP com.vmops.test.longrun.BuildGuestNetwork $*


