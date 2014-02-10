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


mysql --user=cloud --password=cloud < clouddev.sql
if [ $? -ne 0 ]; then
   printf "failed to init cloudev db"
fi
mysql --user=cloud -t cloud --password=cloud -e "insert into configuration (name, value) VALUES('consoleproxy.static.publicip', \"$1\")"
mysql --user=cloud -t cloud --password=cloud -e "insert into configuration (name, value) VALUES('consoleproxy.static.port', \"$2\")"

vmids=`xe vm-list is-control-domain=false |grep uuid|awk '{print $5}'`
for vm in $vmids
    do
        echo $vm
        xe vm-shutdown uuid=$vm
        xe vm-destroy uuid=$vm
    done

vdis=`xe vdi-list |grep ^uuid |awk '{print $5}'`
for vdi in $vdis
    do
        xe vdi-destroy uuid=$vdi
        if [ $? -gt 0 ];then
            xe vdi-forget uuid=$vdi
        fi

    done
