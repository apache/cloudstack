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
iteration=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select count(*) from account;")
echo "Created/Destroyed for the first VM:\n"
for ((i=3; i<$iteration+1; i++))
do
vm_name=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select v.name from vm_instance v, user_vm u where v.id=u.id and u.account_id=$i limit 0,1;")
created_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$vm_name%' and type='VM.CREATE' and level='INFO';")
destroyed_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$vm_name%' and type='VM.DESTROY' and level='INFO';")


if [ "$vm_name" != "" ] && [ "$destroyed_time" != "" ]
then

event_time=`expr $destroyed_time - $created_time`
cloud_usage_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select ROUND(SUM(raw_usage*3600)) from cloud_usage where usage_type=2 and description like '%$vm_name%';")

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
echo "Skipping verification for account $i (the account either a) misses the first VM b) VM wasn't destroyed 3) VM Destroy failed "
fi
done

echo "Created/Destroyed for the second VM:\n"
for ((i=3; i<$iteration+1; i++))
do
vm_name=$(mysql -h $1 --user=root --skip-column-names -U cloud -e "select v.name from vm_instance v, user_vm u where v.id=u.id and u.account_id=$i limit 1,1;")
created_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$vm_name%' and type='VM.CREATE' and level='INFO';")
destroyed_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select TIME_TO_SEC(created) from event where description like '%$vm_name%' and type='VM.DESTROY' and level='INFO';")


if [ "$vm_name" != "" ] && [ "$destroyed_time" != "" ]
then

event_time=`expr $destroyed_time - $created_time`
cloud_usage_time=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select ROUND(SUM(raw_usage*3600)) from cloud_usage where usage_type=2 and description like '%$vm_name%';")

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

