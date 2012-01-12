#!/bin/bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 



 

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


