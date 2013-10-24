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


 

# Usage
#	save_password -v <user VM IP> -p <password>

source /root/func.sh

lock="passwdlock"
#default timeout value is 30 mins as password reset command is not synchronized on agent side any more,
#and multiple commands can be sent to the same VR at a time
locked=$(getLockFile $lock 1800)
if [ "$locked" != "1" ]
then
    exit 1
fi

PASSWD_FILE=/var/cache/cloud/passwords

while getopts 'v:p:' OPTION
do
  case $OPTION in
  v)	VM_IP="$OPTARG"
		;;
  p)	
		ENCODEDPASSWORD="$OPTARG"
		PASSWORD=$(echo $ENCODEDPASSWORD | tr '[a-m][n-z][A-M][N-Z]' '[n-z][a-m][N-Z][A-M]')
		;;
  ?)	echo "Incorrect usage"
                unlock_exit 1 $lock $locked
		;;
  esac
done

[ -f $PASSWD_FILE ] ||  touch $PASSWD_FILE

sed -i /$VM_IP/d $PASSWD_FILE

ps aux | grep serve_password.sh |grep -v grep 2>&1 > /dev/null
if [ $? -eq 0 ]
then
    echo "$VM_IP=$PASSWORD" >> $PASSWD_FILE
else
    echo "$VM_IP=saved_password" >> $PASSWD_FILE
fi

unlock_exit $? $lock $locked
