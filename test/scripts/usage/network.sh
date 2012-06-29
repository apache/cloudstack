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
iteration=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select count(*) from user_statistics;")
echo "Bytes received:"
for ((i=3; i<$iteration; i++))
do
cloud_usage=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select SUM(raw_usage) from cloud_usage where account_id=$i and usage_type=5;")
if [ "$cloud_usage" = "NULL" ]
then
cloud_usage=0
fi

user_stat=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select net_bytes_received+current_bytes_received from user_statistics where account_id=$i;")

if [ "$user_stat" = "" ]
then
user_stat=0
fi

temp=`expr $user_stat - $cloud_usage`
if [ $temp -ne 0 ]
then
echo "For account $i difference in bytes_received is $temp"
fi
done

echo "\n"
echo "Bytes sent:"
for ((i=3; i<$iteration; i++))
do
cloud_usage=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select SUM(raw_usage) from cloud_usage where account_id=$i and usage_type=4;")

if [ "$cloud_usage" = "NULL" ]
then
cloud_usage=0
fi

user_stat=$(mysql -h $1 --user=root --skip-column-names -U cloud_usage -e "select net_bytes_sent+current_bytes_sent from user_statistics where account_id=$i;")

if [ "$user_stat" = "" ]
then
user_stat=0
fi

temp=`expr $user_stat - $cloud_usage`
if [ $temp -ne 0 ]
then
echo "For account $i difference in bytes_sent is $temp"
fi
done
