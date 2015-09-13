#!/bin/sh
#
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
# Wrapper for starting CloudStack Agent under systemd
#
# ExecStart=/usr/libexec/cloudstack/cloudstack-agent-systemd-wrapper.sh
#

ACP=`ls /usr/share/cloudstack-agent/lib/*.jar | tr '\n' ':' | sed s'/.$//'`
PCP=`ls /usr/share/cloudstack-agent/plugins/*.jar 2>/dev/null | tr '\n' ':' | sed s'/.$//'`

CLASSPATH="$ACP:$PCP:/etc/cloudstack/agent:/usr/share/cloudstack-common/scripts"

${JAVA_HOME}/bin/java -Xms${JAVA_HEAP_INITIAL} -Xmx${JAVA_HEAP_MAX} -cp "$CLASSPATH" $JAVA_CLASS

exit $?
