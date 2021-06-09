#!/bin/bash
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
for i in `xm list | awk '{ print $1 }' | egrep -v "Name|Domain-0"`
do
    xm destroy $i
done
rm /etc/ovs-agent/db/server
rm /etc/ovs-agent/db/repository
rm /etc/ocfs2/cluster.conf
/etc/init.d/ovs-agent restart
/etc/init.d/ocfs2 restart
for i in `mount | grep cs-mgmt | awk '{ print $1 }'`
do
    umount $i
done
