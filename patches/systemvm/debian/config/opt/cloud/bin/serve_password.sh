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



 


# set -x 

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

#get a value from a file in the form key=value
#   $1 filename
#   $2 keyname
get_value() {
  local filename=$1
  local keyname=$2
  grep -i $keyname= $filename | cut -d= -f2
}

ip=$1

logger -t cloud "serve_password called to service a request for $ip."

while read input
do
	if [ "$input" == "" ]
	then
		break
	fi

	request=$(echo $input | grep "DomU_Request:" | cut -d: -f2 | sed 's/^[ \t]*//')

	if [ "$request" != "" ]
	then
		break
	fi
done

# echo -e \"\\\"HTTP/1.0 200 OK\\\nDocumentType: text/plain\\\n\\\n\\\"\"; 

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

# echo -e \"\\\"\\\n\\\"\"

exit 0
