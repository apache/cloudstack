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



 


#set -x

rm -rf log.out

dir=$(dirname "$0")
if [ -f $dir/../deploy.properties ]; then
  . "$dir/../deploy.properties"
fi


echo "Starting checking the logs for errors...."

  for i in $SERVER
  do
    echo -ne "Log from Management server $i: \n=====================================================================================\n" >> log.out
    ssh root@$i "grep -i -E 'error|exce|fail|unable|leak' /var/log/vmops/vmops.log" >> log.out
    echo -e "\n" >> log.out
  done

  for i in $COMPUTE
  do
    echo -ne "Log from Computing host $i: \n=====================================================================================\n" >> log.out
    ssh root@$i "grep -i -E 'error|exce|fail|timed out|unable' /var/log/vmops/agent.log" >> log.out
    echo -e "\n\n" >> log.out
  done

  for i in $ROUTER
  do
    echo -ne "Log from Routing host $i: \n=====================================================================================\n" >> log.out
    ssh root@$i "grep -i -E 'error|exce|fail|timed out|unable' /var/log/vmops/agent.log" >> log.out
    echo -e "\n" >> log.out
  done

  for i in $STORAGE
  do
    echo -ne "Log from Storage host $i: \n=====================================================================================\n" >> log.out
    ssh root@$i "grep -i error /var/log/vmops/agent.log" >> log.out
    ssh root@$i "grep -i fail /var/log/vmops/agent.log" >> log.out
    ssh root@$i "grep -i exce /var/log/vmops/agent.log" >> log.out
    ssh root@$i "grep -i unable /var/log/vmops/agent.log" >> log.out
	ssh root@$i "grep -i \'timed out\' /var/log/vmops/agent.log" >> log.out
    echo -e "\n" >> log.out
  done


echo "Done! Check log.out file for the results"
