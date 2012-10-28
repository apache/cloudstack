#!/bin/bash
#
# Init file for Password Download Client
#
# chkconfig: 345 98 02
# description: Password Download Client

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


# Modify this line to specify the user (default is root)
user=root

# Add your DHCP lease folders here
DHCP_FOLDERS="/var/lib/dhclient/* /var/lib/dhcp3/* /var/lib/dhcp/*"
password_received=0
file_count=0
error_count=0

for DHCP_FILE in $DHCP_FOLDERS
do
	if [ -f $DHCP_FILE ]
	then
		file_count=$((file_count+1))
		PASSWORD_SERVER_IP=$(grep dhcp-server-identifier $DHCP_FILE | tail -1 | awk '{print $NF}' | tr -d '\;')

		if [ -n "$PASSWORD_SERVER_IP" ]
		then
			logger -t "cloud" "Found password server IP $PASSWORD_SERVER_IP in $DHCP_FILE"
			logger -t "cloud" "Sending request to password server at $PASSWORD_SERVER_IP"
			password=$(wget -q -t 3 -T 20 -O - --header "DomU_Request: send_my_password" $PASSWORD_SERVER_IP:8080)
			password=$(echo $password | tr -d '\r')

			if [ $? -eq 0 ]
			then
				logger -t "cloud" "Got response from server at $PASSWORD_SERVER_IP"

				case $password in
				
				"")					logger -t "cloud" "Password server at $PASSWORD_SERVER_IP did not have any password for the VM"
									continue
									;;
				
				"bad_request")		logger -t "cloud" "VM sent an invalid request to password server at $PASSWORD_SERVER_IP"
									error_count=$((error_count+1))
									continue
									;;
									
				"saved_password") 	logger -t "cloud" "VM has already saved a password from the password server at $PASSWORD_SERVER_IP"
									continue
									;;
									
				*)					logger -t "cloud" "VM got a valid password from server at $PASSWORD_SERVER_IP"
									password_received=1
									break
									;;
									
				esac
			else
				logger -t "cloud" "Failed to send request to password server at $PASSWORD_SERVER_IP"
				error_count=$((error_count+1))
			fi
		else
			logger -t "cloud" "Could not find password server IP in $DHCP_FILE"
			error_count=$((error_count+1))
		fi
	fi
done

if [ "$password_received" == "0" ]
then
	if [ "$error_count" == "$file_count" ]
	then
		logger -t "cloud" "Failed to get password from any server"
		exit 1
	else
		logger -t "cloud" "Did not need to change password."
		exit 0
	fi
fi

logger -t "cloud" "Changing password ..."
echo $user:$password | chpasswd
						
if [ $? -gt 0 ]
then
	usermod -p `mkpasswd -m SHA-512 $password` $user
		
	if [ $? -gt 0 ]
	then
		logger -t "cloud" "Failed to change password for user $user"
		exit 1
	else
		logger -t "cloud" "Successfully changed password for user $user"
	fi
fi
						
logger -t "cloud" "Sending acknowledgment to password server at $PASSWORD_SERVER_IP"
wget -t 3 -T 20 -O - --header "DomU_Request: saved_password" $PASSWORD_SERVER_IP:8080
exit 0

