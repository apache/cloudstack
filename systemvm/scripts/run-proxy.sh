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



 

#run.sh runs the console proxy.

# make sure we delete the old files from the original template 
rm console-proxy.jar
rm console-common.jar
rm conf/cloud.properties

CP=./:./conf
for file in *.jar
do
  CP=${CP}:$file
done

#CMDLINE=$(cat /proc/cmdline)
#for i in $CMDLINE
#  do
#     KEY=$(echo $i | cut -d= -f1)
#     VALUE=$(echo $i | cut -d= -f2)
#     case $KEY in
#       mgmt_host)
#          MGMT_HOST=$VALUE
#          ;;
#     esac
#  done
   
java -mx700m -cp $CP:./conf com.cloud.consoleproxy.ConsoleProxy $@
