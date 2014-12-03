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
 


# set -x 

source /root/func.sh

lock="passwdlock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

PASSWD_FILE=/var/cache/cloud/passwords

#   $1 filename
#   $2 keyname
#   $3 value
replace_in_file() {
  local filename=$1
  local keyname=$2
  local value=$3
  sed -i /$keyname=/d $filename
  echo "$keyname=$value" >> $filename
  return $?
}

#   $1 filename
#   $2 keyname
get_value() {
  local filename=$1
  local keyname=$2
  grep -i "^$keyname=" $filename | cut -d= -f2
}

ip=$1

logger -t cloud "serve_password called to service a request for $ip."

while read input
do
	if [ "$input" == "" ]
	then
		break
	fi

	request=$(echo "$input" | grep "DomU_Request:" | cut -d: -f2 | sed 's/^[ \t]*//')

	if [ "$request" != "" ]
	then
		break
	fi
done

echo -ne "HTTP/1.0 200 OK\r\nDocumentType: text/plain\r\n\r\n"

if [ "$request" == "send_my_password" ]
then
	password=$(get_value $PASSWD_FILE $ip)
	if [ "$password" == "" ]
	then
		logger -t cloud "serve_password sent bad_request to $ip."
		# echo "bad_request"
                # Return "saved_password" for non-existed entry, to make it
                # work if domR was once destroyed.
		echo "saved_password"
	else
		logger -t cloud "serve_password sent a password to $ip."
		echo $password
	fi
else
	if [ "$request" == "saved_password" ]
	then
		replace_in_file $PASSWD_FILE $ip "saved_password"
		logger -t cloud "serve_password sent saved_password to $ip."
		echo "saved_password"
	else
		logger -t cloud "serve_password sent bad_request to $ip."
		echo "bad_request"
	fi
fi

unlock_exit 0 $lock $locked
