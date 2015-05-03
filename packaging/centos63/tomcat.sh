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

. /etc/sysconfig/cloudstack-management

if [ -r /usr/share/java-utils/java-functions ]; then
  . /usr/share/java-utils/java-functions
else
  echo "Can't read Java functions library, aborting"
  exit 1
fi

set_javacmd

MAIN_CLASS="org.apache.catalina.startup.Bootstrap"

FLAGS="$JAVA_OPTS $CATALINA_OPTS"
OPTIONS="-Dcatalina.base=$CATALINA_BASE \
-Dcatalina.home=$CATALINA_HOME \
-Djava.endorsed.dirs=$JAVA_ENDORSED_DIRS \
-Djava.io.tmpdir=$CATALINA_TMPDIR \
-Djava.util.logging.config.file=$CATALINA_BASE/conf/log4j-cloud.xml \
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"

if [ "$1" = "start" ] ; then
  run start
elif [ "$1" = "stop" ] ; then
  run stop
fi


