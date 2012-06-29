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
 
usage() {
  printf "Usage: %s [name label]  \n" $(basename $0) 
}

if [ -z $1 ]; then
  usage
  echo "3#no namelabel"
  exit 0
else
  namelabel=$1
fi

pid=`ps -ef | grep "dd" | grep $namelabel | grep -v "grep" | awk '{print $2}'`
if [ -z $pid ]; then
  echo "true"
  exit 0
fi 

kill -9 $pid
if [ $? -ne 0 ]; then
  echo "false"
  exit 0
fi
echo "true"
exit 0
