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
#Check first ROOT volume
iteration=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select count(*) from account;")
echo "Created/removed for the first ROOT volume:\n"
for ((i=3; i<$iteration+1; i++))
do
volume_name=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select name from volumes where account_id=$i and volume_type='ROOT' and name like 'i-%' limit 0,1;")
volume_id=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select id from volumes where account_id=$i and volume_type='ROOT' and name like 'i-%'limit 0,1;")
created_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$volume_name%' and type='VOLUME.CREATE' and level='INFO';")
destroyed_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$volume_name%' and type='VOLUME.DELETE' and level='INFO';")


if [ "$volume_name" != "" ] && [ "$destroyed_time" != "" ]
then

event_time=`expr $destroyed_time - $created_time`
cloud_usage_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select ROUND(SUM(raw_usage*3600)) from cloud_usage where usage_type=6 and description like '%Volume Id: $volume_id%';")

if [ "$cloud_usage_time" = "NULL" ]
then
echo "Allocated time is missing in cloud_usage table for volume $volume_name belonging to account $i"
else
temp=`expr $event_time - $cloud_usage_time`
if [ $temp -ne 0 ] && [ $temp != "-86400" ]
then
echo "For account $i difference in time for volume $volume_name is $temp"
else
echo "Test passed for the ROOT volume $volume_name belonging to account $i"
fi
fi
else
echo "Skipping verification for account $i (the account either a) misses root volume $volume_name b) volume wasn't deleted 3) Delete volume failed "
fi
done

#Check second ROOT volume
iteration=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select count(*) from account;")
echo "Created/removed for the second ROOT volume:\n"
for ((i=3; i<$iteration+1; i++))
do
volume_name=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select name from volumes where account_id=$i and volume_type='ROOT' and name like 'i-%' limit 1,1;")
volume_id=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select id from volumes where account_id=$i and volume_type='ROOT' and name like 'i-%'limit 1,1;")
created_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$volume_name%' and type='VOLUME.CREATE' and level='INFO';")
destroyed_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$volume_name%' and type='VOLUME.DELETE' and level='INFO';")


if [ "$volume_name" != "" ] && [ "$destroyed_time" != "" ]
then

event_time=`expr $destroyed_time - $created_time`
cloud_usage_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select ROUND(SUM(raw_usage*3600)) from cloud_usage where usage_type=6 and description like '%Volume Id: $volume_id%';")

if [ "$cloud_usage_time" = "NULL" ]
then
echo "Allocated time is missing in cloud_usage table for volume $volume_name belonging to account $i"
else
temp=`expr $event_time - $cloud_usage_time`
if [ $temp -ne 0 ] && [ $temp != "-86400" ]
then
echo "For account $i difference in time for volume $volume_name is $temp"
else
echo "Test passed for the ROOT volume $volume_name belonging to account $i"
fi
fi
else
echo "Skipping verification for account $i (the account either a) misses root volume $volume_name b) volume wasn't deleted 3) Delete volume failed "
fi
done


#Check first DATADISK volume
iteration=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select count(*) from account;")
echo "Created/removed for the first DATADISK volume:\n"
for ((i=3; i<$iteration+1; i++))
do
volume_name=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select name from volumes where account_id=$i and volume_type='DATADISK' and name like 'i-%' limit 0,1;")
volume_id=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select id from volumes where account_id=$i and volume_type='DATADISK' and name like 'i-%'limit 0,1;")
created_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$volume_name%' and type='VOLUME.CREATE' and level='INFO';")
destroyed_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$volume_name%' and type='VOLUME.DELETE' and level='INFO';")


if [ "$volume_name" != "" ] && [ "$destroyed_time" != "" ]
then

event_time=`expr $destroyed_time - $created_time`
cloud_usage_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select ROUND(SUM(raw_usage*3600)) from cloud_usage where usage_type=6 and description like '%Volume Id: $volume_id%';")

if [ "$cloud_usage_time" = "NULL" ]
then
echo "Allocated time is missing in cloud_usage table for volume $volume_name belonging to account $i"
else
temp=`expr $event_time - $cloud_usage_time`
if [ $temp -ne 0 ] && [ $temp != "-86400" ]
then
echo "For account $i difference in time for volume $volume_name is $temp"
else
echo "Test passed for the DATADISK volume $volume_name belonging to account $i"
fi
fi
else
echo "Skipping verification for account $i (the account either a) misses root volume $volume_name b) volume wasn't deleted 3) Delete volume failed "
fi
done

#Check second DATADISK volume
iteration=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select count(*) from account;")
echo "Created/removed for the second DATADISK volume:\n"
for ((i=3; i<$iteration+1; i++))
do
volume_name=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select name from volumes where account_id=$i and volume_type='DATADISK' and name like 'i-%' limit 1,1;")
volume_id=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select id from volumes where account_id=$i and volume_type='DATADISK' and name like 'i-%'limit 1,1;")
created_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$volume_name%' and type='VOLUME.CREATE' and level='INFO';")
destroyed_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$volume_name%' and type='VOLUME.DELETE' and level='INFO';")


if [ "$volume_name" != "" ] && [ "$destroyed_time" != "" ]
then

event_time=`expr $destroyed_time - $created_time`
cloud_usage_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select ROUND(SUM(raw_usage*3600)) from cloud_usage where usage_type=6 and description like '%Volume Id: $volume_id%';")

if [ "$cloud_usage_time" = "NULL" ]
then
echo "Allocated time is missing in cloud_usage table for volume $volume_name belonging to account $i"
else
temp=`expr $event_time - $cloud_usage_time`
if [ $temp -ne 0 ] && [ $temp != "-86400" ]
then
echo "For account $i difference in time for volume $volume_name is $temp"
else
echo "Test passed for the DATADISK volume $volume_name belonging to account $i"
fi
fi
else
echo "Skipping verification for account $i (the account either a) misses root volume $volume_name b) volume wasn't deleted 3) Delete volume failed "
fi
done


