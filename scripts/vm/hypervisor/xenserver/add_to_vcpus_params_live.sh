#!/bin/sh
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

set -x

vmname=$1
key=$2
value=$3
uuid=`xe vm-list name-label=$vmname | grep uuid | awk '{print $NF}'`
if [[ $key == "weight" ]]
then
    xe vm-param-set VCPUs-params:weight=$value uuid=$uuid
fi
if [[ $key == "cap" ]]
then
    xe vm-param-set VCPUs-params:cap=$value uuid=$uuid
fi

