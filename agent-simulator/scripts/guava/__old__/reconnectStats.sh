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


 

name=$1
while [ 1 ]
do
	mysql -s -r -uroot -Dcloud -h10.1.1.215 -e"select count(id),now(),max(disconnected),mgmt_server_id,status from host group by mgmt_server_id,status;" >> $1
	sleep 5
	echo --------------------------------------------------------------------------------------------------------------------- >> $1
done
