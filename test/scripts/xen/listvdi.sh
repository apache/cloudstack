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



 


uuid=""
name=""
host=""
var=""

while getopts u:n:h: OPTION
do
  case $OPTION in
  u)    uuid="$OPTARG"
        ;;
  n)    name="$OPTARG"
  		;;
  h)	host="$OPTARG"
  esac
done



if [ "$uuid" != "" ]
then
        var=`ssh root@$host "xe vdi-list uuid=$uuid"`
else
        if [ "$name" != "" ]
        then
                var=`ssh root@$host "xe vdi-list name-label=$name"`
        fi
fi


if  [ "$var" == "" ]
then echo "VDI $name $uuid doesn't exist on host $host"
exit 2
else
exit 0
fi