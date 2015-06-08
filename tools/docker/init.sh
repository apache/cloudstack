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
#
# update database connection
# start cloudstack-management server
#/usr/bin/cloudstack-setup-databases cloud:password@$MYSQL_PORT_3306_TCP_ADDR

# initial startup of the container to generage ssh_key
# performed as privileged
if [ ! -d /var/cloudstack/management/.ssh ]; then
	mknod /dev/loop4 -m0660 b 7 4
fi
#
service mysqld start
service cloudstack-management start
sleep 20 # wait for the file management-server.log  to be created
tail -f /var/log/cloudstack/management/management-server.log
