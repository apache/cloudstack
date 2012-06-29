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
iteration=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select count(*) from account;")
echo "Started/Stopped for the first VM:\n"
for ((i=3; i<$iteration+1; i++))
do
vm_name=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select v.name from vm_instance v, user_vm u where v.id=u.id and u.account_id=$i limit 0,1;")
created_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$vm_name%' and type='VM.START' and level='INFO';")
destroyed_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$vm_name%' and type='VM.STOP' and level='INFO';")


if [ "$vm_name" != "" ] && [ "$destroyed_time" != "" ]
then

event_time=`expr $destroyed_time - $created_time`
cloud_usage_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select ROUND(SUM(raw_usage*3600)) from cloud_usage where usage_type=1 and description like '%$vm_name%';")

if [ "$cloud_usage_time" = "NULL" ]
then
echo "Running time is missing in cloud_usage table for VM $vm_name belonging to account $i"
else
temp=`expr $event_time - $cloud_usage_time`
if [ $temp -ne 0 ] && [ $temp != "-86400" ]
then
echo "For account $i difference in running time for vm $vm_name is $temp"
else
echo "Test passed for the first VM belonging to account $i"
fi
fi
else
echo "Skipping verification for account $i (the account either a) misses the first VM b) VM wasn't stopped 3) VM Stop failed "
fi
done



echo "Started/Stopped for the second VM:\n"
for ((i=3; i<$iteration+1; i++))
do
vm_name=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select v.name from vm_instance v, user_vm u where v.id=u.id and u.account_id=$i limit 1,1;")
created_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$vm_name%' and type='VM.START' and level='INFO';")
destroyed_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$vm_name%' and type='VM.STOP' and level='INFO';")


if [ "$vm_name" != "" ] && [ "$destroyed_time" != "" ]
then

event_time=`expr $destroyed_time - $created_time`
cloud_usage_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select ROUND(SUM(raw_usage*3600)) from cloud_usage where usage_type=1 and description like '%$vm_name%';")

if [ "$cloud_usage_time" = "NULL" ]
then
echo "Running time is missing in cloud_usage table for VM $vm_name belonging to account $i"
else
temp=`expr $event_time - $cloud_usage_time`
if [ $temp -ne 0 ] && [ $temp != "-86400" ]
then
echo "For account $i difference in running time for vm $vm_name is $temp"
else
echo "Test passed for the second vm belonging to account $i"
fi
fi
else
echo "Skipping verification for account $i (the account either a) misses the second VM b) VM wasn't stopped 3) VM Stop failed "
fi
done
