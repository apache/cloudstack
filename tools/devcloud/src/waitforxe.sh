#! /bin/bash
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

date
interval=20
timeout=300
command="xe host-list"

count=0
maxcount=$(($timeout/$interval))

until  [ $count -gt $maxcount ]; do
    if $command > /dev/null 2>&1; then
        echo "\"$command\" executed successfully."
        date
        exit 0
    fi
    let count=count+1
    echo "Waiting for \"$command\" to run successfully."
    sleep $interval
done

echo "\"$command\" failed to complete."
date
