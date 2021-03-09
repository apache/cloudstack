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
  printf "Usage: %s: -o <op>(add | delete) -v <vlan id> -p <pif> -b <bridge name> -d <delete bridge>(true | false)\n"
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
	ip link set $vlanDev up > /dev/null 2>/dev/null
	
	if [ ! -d /sys/class/net/$vlanBr ]
	then
		ip link add name $vlanBr type bridge
		ip link set $vlanBr up
	
		if [ $? -gt 0 ]
		then
			if [ ! -d /sys/class/net/$vlanBr ]
			then
				printf "Failed to create br: $vlanBr"
				return 2
			fi
		fi
	fi
	
	#pif is eslaved into vlanBr?
	ls /sys/class/net/$vlanBr/brif/ |grep -w "$vlanDev" > /dev/null 
	if [ $? -gt 0 ]
	then
		ip link set $vlanDev master $vlanBr
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
	ip link set $vlanBr up > /dev/null 2>/dev/null

	return 0
}

deleteVlan() {
	local vlanId=$1
	local pif=$2
	local vlanDev=$pif.$vlanId
  local vlanBr=$3
  local deleteBr=$4

  if [ $deleteBr == "true" ]
  then
	  ip link delete $vlanDev type vlan > /dev/null
	
  	if [ $? -gt 0 ]
	  then
		  printf "Failed to del vlan: $vlanId"
		  return 1
	  fi
    ip link set $vlanBr down

    if [ $? -gt 0 ]
    then
      return 1
    fi

    ip link delete $vlanBr type bridge

    if [ $? -gt 0 ]
    then
      printf "Failed to del bridge $vlanBr"
      return 1
    fi
  fi
	return 0
	
}

op=
vlanId=
deleteBr="true"
option=$@

while getopts 'o:v:p:b:d:' OPTION
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
  d)    dflag=1
    deleteBr="$OPTARG"
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
		deleteVlan $vlanId $pif $brName $deleteBr
	
		# Always exit with success
		exit 0
	fi
fi
