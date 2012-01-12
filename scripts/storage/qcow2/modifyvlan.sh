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



 

# $Id: modifyvlan.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/modifyvlan.sh $
# modifyvlan.sh -- adds and deletes VLANs from a Routing Server
# set -x

usage() {
  printf "Usage: %s: -o <op> -v <vlan id> -g <vlan gateway> \n" 
}

addVlan() {
	local vlanId=$1
	
	# Try to add bond1.[VLAN ID] via vconfig, if it does not already exist
	ifconfig bond1.$vlanId > /dev/null
	
	if [ $? -gt 0 ]
	then
		vconfig add bond1 $vlanId
		
		if [ $? -gt 0 ]
		then
			return 1
		fi
	fi
	
	# Make ifcfg-bond1.$vlanId
	rm /etc/sysconfig/network-scripts/ifcfg-bond1.$vlanId
	touch /etc/sysconfig/network-scripts/ifcfg-bond1.$vlanId
	echo "DEVICE=bond1.$vlanId" >> /etc/sysconfig/network-scripts/ifcfg-bond1.$vlanId
	echo "ONBOOT=yes" >> /etc/sysconfig/network-scripts/ifcfg-bond1.$vlanId
	echo "BOOTPROTO=none" >> /etc/sysconfig/network-scripts/ifcfg-bond1.$vlanId
	echo "VLAN=yes" >> /etc/sysconfig/network-scripts/ifcfg-bond1.$vlanId
	echo "BRIDGE=xenbr1.$vlanId" >> /etc/sysconfig/network-scripts/ifcfg-bond1.$vlanId
	
	# Try to add xenbr1.$vlanId over bond1.$vlanId, if it does not already exist
	
	ifconfig xenbr1.$vlanId > /dev/null
	
	if [ $? -gt 0 ]
	then
		brctl addbr xenbr1.$vlanId
	
		if [ $? -gt 0 ]
		then
			return 1
		fi
	
		brctl addif xenbr1.$vlanId bond1.$vlanId
	
		if [ $? -gt 0 ]
		then
			return 1
		fi
		
	fi
	
	ifconfig xenbr1.$vlanId up
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	# Make ifcfg-xenbr1.$vlanId
	rm /etc/sysconfig/network-scripts/ifcfg-xenbr1.$vlanId
	touch /etc/sysconfig/network-scripts/ifcfg-xenbr1.$vlanId
	echo "TYPE=bridge" >> /etc/sysconfig/network-scripts/ifcfg-xenbr1.$vlanId
	echo "DEVICE=xenbr1.$vlanId" >> /etc/sysconfig/network-scripts/ifcfg-xenbr1.$vlanId
	echo "ONBOOT=yes" >> /etc/sysconfig/network-scripts/ifcfg-xenbr1.$vlanId
	echo "BOOTPROTO=none" >> /etc/sysconfig/network-scripts/ifcfg-xenbr1.$vlanId

	return 0
}

deleteVlan() {
	local vlanId=$1
	
	# Try to remove xenbr1.$vlanId
	ifconfig xenbr1.$vlanId down
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	brctl delbr xenbr1.$vlanId
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	# Remove ifcfg-xenbr1.$vlanId
	rm /etc/sysconfig/network-scripts/ifcfg-xenbr1.$vlanId
	
	# Try to remove bond1.$vlanId

	vconfig rem bond1.$vlanId
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	# Remove ifcfg-bond1.$vlanId
	rm /etc/sysconfig/network-scripts/ifcfg-bond1.$vlanId
	
	return 0
	
}

checkIfVlanExists() {
	local vlanId=$1
	
	if [ "$vlanId" == "untagged" ]
	then
		# This VLAN should always exist, since the bridge is xenbr1, which is created during vsetup
		return 0
	fi
	
	ifconfig bond1.$vlanId > /dev/null
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	ifconfig xenbr1.$vlanId > /dev/null
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	if [ ! -f /etc/sysconfig/network-scripts/ifcfg-xenbr1.$vlanId ]
	then
		return 1
	fi
	
	if [ ! -f /etc/sysconfig/network-scripts/ifcfg-bond1.$vlanId ]
	then
		return 1
	fi
	
	return 0
}

arpingVlan() {
	local vlanId=$1
	local vlanGateway=$2
	
	# Change!!!
	return 0
	
	success=1
	for i in $(seq 1 3)
	do
		arping -I xenbr1.$vlanId $vlanGateway > /dev/null
		
		if [ $? -gt 0 ]
		then
			success=0
			break
		fi
	done
	
	return $success
}

op=
vlanId=
vlanGateway=
option=$@

while getopts 'o:v:g:' OPTION
do
  case $OPTION in
  o)	oflag=1
		op="$OPTARG"
		;;
  v)	vflag=1
		vlanId="$OPTARG"
		;;
  g)	gflag=1
		vlanGateway="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

# Check that all arguments were passed in
if [ "$oflag$vflag$gflag" != "111" ]
then
	usage
	exit 2
fi

if [ "$op" == "add" ]
then
	# Check if the vlan already exists, and exit with success if it does
	checkIfVlanExists $vlanId
	
	if [ $? -eq 0 ]
	then
		exit 0
	fi

	# Add the vlan
	addVlan $vlanId
	
	# If the add fails then return failure
	if [ $? -gt 0 ]
	then
		exit 1
	fi
	
	# Ping the vlan 
	arpingVlan $vlanId $vlanGateway
	
	# If the ping fails then delete the vlan and return failure. Else, return success. 
	if [ $? -gt 0 ]
	then
		deleteVlan $vlanId
		exit 1
	else
		exit 0
	fi
else 
	if [ "$op" == "delete" ]
	then
		# Delete the vlan
		deleteVlan $vlanId
	
		# Always exit with success
		exit 0
	fi
fi













