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

if [ ! -f cloudbridge_db.sql ]; then
  printf "Error: Unable to find cloudbridge_db.sql\n"
  exit 4
fi

if [ ! -f cloudbridge_schema.sql ]; then
  printf "Error: Unable to find cloudbridge_schema.sql\n"
  exit 5
fi

if [ ! -f cloudbridge_index.sql ]; then
  printf "Error: Unable to find cloudbridge_index.sql\n"
  exit 6;
fi

echo "Recreating Database."
mysql --user=root --password=$1 < cloudbridge_db.sql > /dev/null 2>/dev/null
mysqlout=$?
if [ $mysqlout -eq 1 ]; then
  printf "Please enter root password for MySQL.\n" 
  mysql --user=root --password < cloudbridge_db.sql
  if [ $? -ne 0 ]; then
    printf "Error: Cannot execute cloudbridge_db.sql\n"
    exit 10
  fi
elif [ $mysqlout -ne 0 ]; then
  printf "Error: Cannot execute cloudbridge_db.sql\n"
  exit 11
fi

mysql --user=cloud --password=cloud < cloudbridge_schema.sql
if [ $? -ne 0 ]; then
  printf "Error: Cannot execute cloudbridge_schema.sql\n"
  exit 11
fi

mysql --user=cloud --password=cloud < cloudbridge_multipart.sql
if [ $? -ne 0 ]
then
    exit 1
fi

echo "Creating Indice and Foreign Keys"
mysql --user=cloud --password=cloud < cloudbridge_index.sql
if [ $? -ne 0 ]; then
  printf "Error: Cannot execute cloudbridge_index.sql\n"
  exit 13
fi

mysql --user=cloud --password=cloud < cloudbridge_multipart_alter.sql
mysql --user=cloud --password=cloud < cloudbridge_bucketpolicy.sql
mysql --user=cloud --password=cloud < cloudbridge_policy_alter.sql
mysql --user=cloud --password=cloud < cloudbridge_offering.sql
mysql --user=cloud --password=cloud < cloudbridge_offering_alter.sql
