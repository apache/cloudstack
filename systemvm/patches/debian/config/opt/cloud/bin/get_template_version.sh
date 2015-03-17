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

# As the last command send to router before any rules operation, wait until boot up done

__TIMEOUT=240
__FLAGFILE=/var/cache/cloud/boot_up_done
done=0
for i in `seq 1 $(($__TIMEOUT * 10))`
do
    if [ -e $__FLAGFILE ]
    then
        done=1
        break
    fi
    sleep 0.1
    if [ $((i % 10)) -eq 0 ]
    then
        logger -t cloud "Waiting for VM boot up done for one second"
    fi
done

if [ -z $done ]
then
    # declare we failed booting process
    echo "Waited 60 seconds but boot up haven't been completed"
    exit
fi

echo -n `cat /etc/cloudstack-release`'&'
cat /var/cache/cloud/cloud-scripts-signature
