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


# $Id: modifyvlan.sh 11601 2010-08-11 17:26:15Z kris $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.refactor/java/scripts/vm/network/vnet/modifyvlan.sh $
# modifyvlan.sh -- adds and deletes VLANs from a Routing Server
# set -x

usage() {
  printf "Usage: %s: -o <op>(add | delete) -v <vlan id> -p <pif> -b <bridge name>\n" 
}

addVlan() {
	local vlanId=$1
	local pif=$2
	local vlanDev=$pif.$vlanId
	local vlanBr=$3

	if [ ! -d /sys/class/net/$vlanDev ]
	then
		ip link add link $pif name $vlanDev type vlan id $vlanId > /dev/null
		ip link set $vlanDev up
		
		if [ $? -gt 0 ]
		then
            # race condition that someone already creates the vlan 
			if [ ! -d /sys/class/net/$vlanDev ]
            then
			    printf "Failed to create vlan $vlanId on pif: $pif."
			    return 1
            fi
		fi
	fi
	
	# is up?
  	ifconfig |grep -w $vlanDev > /dev/null
	if [ $? -gt 0 ]
	then
		ifconfig $vlanDev up > /dev/null
	fi
	
	if [ ! -d /sys/class/net/$vlanBr ]
	then
		brctl addbr $vlanBr > /dev/null
	
		if [ $? -gt 0 ]
		then
			if [ ! -d /sys/class/net/$vlanBr ]
			then
				printf "Failed to create br: $vlanBr"
				return 2
			fi
		fi

		brctl setfd $vlanBr 0
	fi
	
	#pif is eslaved into vlanBr?
	ls /sys/class/net/$vlanBr/brif/ |grep -w "$vlanDev" > /dev/null 
	if [ $? -gt 0 ]
	then
		brctl addif $vlanBr $vlanDev > /dev/null
		if [ $? -gt 0 ]
		then
			ls /sys/class/net/$vlanBr/brif/ |grep -w "$vlanDev" > /dev/null 
			if [ $? -gt 0 ]
			then
				printf "Failed to add vlan: $vlanDev to $vlanBr"
				return 3
			fi
		fi
	fi
	# is vlanBr up?
	ifconfig |grep -w $vlanBr > /dev/null
	if [ $? -gt 0 ]
	then
		ifconfig $vlanBr up
	fi

	return 0
}

deleteVlan() {
	local vlanId=$1
	local pif=$2
	local vlanDev=$pif.$vlanId
        local vlanBr=$3

	ip link delete $vlanDev type vlan > /dev/null
	
	if [ $? -gt 0 ]
	then
		printf "Failed to del vlan: $vlanId"
		return 1
	fi	

	ifconfig $vlanBr down
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	brctl delbr $vlanBr
	
	if [ $? -gt 0 ]
	then
		printf "Failed to del bridge $vlanBr"
		return 1
	fi

	return 0
	
}

op=
vlanId=
option=$@

while getopts 'o:v:p:b:' OPTION
do
  case $OPTION in
  o)	oflag=1
		op="$OPTARG"
		;;
  v)	vflag=1
		vlanId="$OPTARG"
		;;
  p)    pflag=1
		pif="$OPTARG"
		;;
  b)    bflag=1
                brName="$OPTARG"
                ;;
  ?)	usage
		exit 2
		;;
  esac
done

# Check that all arguments were passed in
if [ "$oflag$vflag$pflag$bflag" != "1111" ]
then
	usage
	exit 2
fi

# Vlan module is loaded?
lsmod|grep ^8021q >& /dev/null
if [ $? -gt 0 ]
then
   modprobe 8021q >& /dev/null
fi

if [ "$vlanId" -eq 4095 ]
then
    exit 0
fi

if [ "$op" == "add" ]
then
	# Add the vlan
	addVlan $vlanId $pif $brName
	
	# If the add fails then return failure
	if [ $? -gt 0 ]
	then
		exit 1
	fi
else 
	if [ "$op" == "delete" ]
	then
		# Delete the vlan
		deleteVlan $vlanId $pif $brName
	
		# Always exit with success
		exit 0
	fi
fi
