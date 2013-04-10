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



 


BASE_DIR="/var/www/html/copy/template/"
HTACCESS="$BASE_DIR/.htaccess"

PASSWDFILE="/etc/httpd/.htpasswd"
if [ -d /etc/apache2 ]
then
  PASSWDFILE="/etc/apache2/.htpasswd"
fi

config_htaccess() {
  mkdir -p $BASE_DIR
  result=$?
  echo "Options -Indexes" > $HTACCESS
  let "result=$result+$?"
  echo "AuthType Basic" >> $HTACCESS
  let "result=$result+$?"
  echo "AuthName \"Authentication Required\"" >> $HTACCESS
  let "result=$result+$?"
  echo "AuthUserFile  \"$PASSWDFILE\"" >> $HTACCESS
  let "result=$result+$?"
  echo "Require valid-user" >> $HTACCESS
  let "result=$result+$?"
  return $result 
}

write_passwd() {
  local user=$1
  local passwd=$2
  htpasswd -bc $PASSWDFILE $user $passwd
  return $?
}

if [ $# -ne 2 ] ; then
	echo $"Usage: `basename $0` username password "
	exit 0
fi

write_passwd $1 $2
if [ $? -ne 0 ]
then
  echo "Failed to update password"
  exit 2
fi

config_htaccess 
exit $?
