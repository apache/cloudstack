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

if [ ! -f "/usr/bin/java" ];then
    echo "ERROR: /usr/bin/java does not exist"
    exit 1
fi

JAVA_MAJOR_VERSION=$(/usr/bin/java --version | grep -oE '[0-9]+\.[0-9]+(\.[0-9]+)?' | head -n 1 |cut -d "." -f1)

if [ -z $JAVA_MAJOR_VERSION ];then
    echo "WARNING: Cannot determine the JAVA version"
    exit 0
fi

if [ "$JAVA_MAJOR_VERSION" != "17" ] && [ "$JAVA_MAJOR_VERSION" != "11" ];then
    echo "ERROR: JAVA $JAVA_MAJOR_VERSION is not supported. Currently only JAVA 17 and JAVA 11 are supported."
    exit 1
fi

if [ "$JAVA_MAJOR_VERSION" != "17" ];then
    echo "WARNING: JAVA version is $JAVA_MAJOR_VERSION. JAVA 17 is recommended."
fi
