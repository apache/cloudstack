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

BASE_DIR="/var/www/html/copy/"
HTACCESS="$BASE_DIR/.htaccess"

config_htaccess() {
  mkdir -p $BASE_DIR
  result=$?
  echo "Options -Indexes" > $HTACCESS
  let "result=$result+$?"
  echo "order deny,allow" >> $HTACCESS
  let "result=$result+$?"
  echo "deny from all" >> $HTACCESS
  let "result=$result+$?"
  return $result
}

ips(){
  echo "allow from $1" >> $HTACCESS
  result=$?
  return $result
}

is_append="$1"
shift
if [ $is_append != "true" ]; then
	config_htaccess
fi
for i in $@
do
        ips "$i"
done
exit $?

