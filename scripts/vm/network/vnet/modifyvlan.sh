#!/usr/bin/env bash
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



 

# $Id: modifyvlan.sh 11601 2010-08-11 17:26:15Z kris $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.refactor/java/scripts/vm/network/vnet/modifyvlan.sh $
# modifyvlan.sh -- adds and deletes VLANs from a Routing Server
# set -x

usage() {
  printf "Usage: %s: -o <op>(add | delete) -v <vlan id> -p <pif> \n" 
}

VIRBR=cloudVirBr
addVlan() {
	local vlanId=$1
	local pif=$2
	local vlanDev=$pif.$vlanId
	local vlanBr=$VIRBR$vlanId
    
    vconfig set_name_type DEV_PLUS_VID_NO_PAD	

	if [ ! -d /sys/class/net/$vlanDev ]
	then
		vconfig add $pif $vlanId > /dev/null
		
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
	local vlanBr=$VIRBR$vlanId

	vconfig rem $vlanDev > /dev/null
	
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

while getopts 'o:v:p:' OPTION
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
  ?)	usage
		exit 2
		;;
  esac
done

# Check that all arguments were passed in
if [ "$oflag$vflag$pflag" != "111" ]
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

if [ "$op" == "add" ]
then
	# Add the vlan
	addVlan $vlanId $pif
	
	# If the add fails then return failure
	if [ $? -gt 0 ]
	then
		exit 1
	fi
else 
	if [ "$op" == "delete" ]
	then
		# Delete the vlan
		deleteVlan $vlanId $pif
	
		# Always exit with success
		exit 0
	fi
fi













