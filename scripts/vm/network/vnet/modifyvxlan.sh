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

# modifyvxlan.sh -- adds and deletes VXLANs from a Routing Server
# set -x

## TODO(VXLAN): MTU, IPv6 underlying

usage() {
  printf "Usage: %s: -o <op>(add | delete) -v <vxlan id> -p <pif> -b <bridge name>\n" 
}

addVxlan() {
	local vxlanId=$1
	local pif=$2
	local vxlanDev=vxlan$vxlanId
	local vxlanBr=$3
	local mcastGrp="239.$(( ($vxlanId >> 16) % 256 )).$(( ($vxlanId >> 8) % 256 )).$(( $vxlanId % 256 ))"
	
	## TODO(VXLAN): $brif (trafficlabel) should be passed from caller because we cannot assume 1:1 mapping between pif and brif.
	# lookup bridge interface 
	local sysfs_dir=/sys/devices/virtual/net/
	local brif=`find ${sysfs_dir}*/brif/ -name $pif | sed -e "s,$sysfs_dir,," | sed -e 's,/brif/.*$,,'`
	
	if [ "$brif " == " " ]
	then
		if [ -d "/sys/class/net/${pif}" ]
		then
			# if bridge is not found, but matches a pif, use it
			brif=$pif
		else
			printf "Failed to lookup bridge interface which includes pif: $pif."
			return 1
		fi
	else
		# confirm ip address of $brif
		ip addr show $brif | grep -w inet
		if [ $? -gt 0 ]
		then
			printf "Failed to find vxlan multicast source ip address on brif: $brif."
			return 1
		fi
	fi

	# mcast route
	## TODO(VXLAN): Can we assume there're only one IP address which can be multicast src IP on the IF?
	ip route get $mcastGrp | grep -w "dev $brif"
	if [ $? -gt 0 ]
	then
		ip route add $mcastGrp/32 dev $brif
		if [ $? -gt 0 ]
		then
			printf "Failed to add vxlan multicast route on brif: $brif."
			return 1
		fi
	fi
	
	if [ ! -d /sys/class/net/$vxlanDev ]
	then
		ip link add $vxlanDev type vxlan id $vxlanId group $mcastGrp ttl 10 dev $brif
		
		if [ $? -gt 0 ]
		then
			# race condition that someone already creates the vxlan 
			if [ ! -d /sys/class/net/$vxlanDev ]
			then
				printf "Failed to create vxlan $vxlanId on brif: $brif."
				return 1
			fi
		fi
	fi
	
	# is up?
	ip link show $vxlanDev | grep -w UP > /dev/null
	if [ $? -gt 0 ]
	then
		ip link set $vxlanDev up > /dev/null
	fi
	
	if [ ! -d /sys/class/net/$vxlanBr ]
	then
		brctl addbr $vxlanBr > /dev/null
	
		if [ $? -gt 0 ]
		then
			if [ ! -d /sys/class/net/$vxlanBr ]
			then
				printf "Failed to create br: $vxlanBr"
				return 2
			fi
		fi

		brctl setfd $vxlanBr 0
	fi
	
	#pif is eslaved into vxlanBr?
	ls /sys/class/net/$vxlanBr/brif/ | grep -w "$vxlanDev" > /dev/null 
	if [ $? -gt 0 ]
	then
		brctl addif $vxlanBr $vxlanDev > /dev/null
		if [ $? -gt 0 ]
		then
			ls /sys/class/net/$vxlanBr/brif/ | grep -w "$vxlanDev" > /dev/null 
			if [ $? -gt 0 ]
			then
				printf "Failed to add vxlan: $vxlanDev to $vxlanBr"
				return 3
			fi
		fi
	fi
	
	# is vxlanBr up?
	ip link show $vxlanBr  | grep -w UP > /dev/null
	if [ $? -gt 0 ]
	then
		ip link set $vxlanBr up
	fi
	
	return 0
}

deleteVxlan() {
	local vxlanId=$1
	local pif=$2
	local vxlanDev=vxlan$vxlanId
	local vxlanBr=$3
	local mcastGrp="239.$(( ($vxlanId >> 16) % 256 )).$(( ($vxlanId >> 8) % 256 )).$(( $vxlanId % 256 ))"
	
	local sysfs_dir=/sys/devices/virtual/net/
	local brif=`find ${sysfs_dir}*/brif/ -name $pif | sed -e "s,$sysfs_dir,," | sed -e 's,/brif/.*$,,'`
	
	ip route del $mcastGrp/32 dev $brif
	
	ip link delete $vxlanDev 
	
	if [ $? -gt 0 ]
	then
		printf "Failed to del vxlan: $vxlanId"
		printf "Continue..."
	fi	
	
	ip link set $vxlanBr down
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	brctl delbr $vxlanBr
	
	if [ $? -gt 0 ]
	then
		printf "Failed to del bridge $vxlanBr"
		return 1
	fi
	
	return 0
}

op=
vxlanId=
option=$@

while getopts 'o:v:p:b:' OPTION
do
  case $OPTION in
  o)	oflag=1
		op="$OPTARG"
		;;
  v)	vflag=1
		vxlanId="$OPTARG"
		;;
  p)	pflag=1
		pif="$OPTARG"
		;;
  b)	bflag=1
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

# Do we support Vxlan?
lsmod|grep ^vxlan >& /dev/null
if [ $? -gt 0 ]
then
   modprobe=`modprobe vxlan 2>&1`
   if [ $? -gt 0 ]
   then
     printf "Failed to load vxlan kernel module: $modprobe"
     exit 1
   fi
fi

if [ "$op" == "add" ]
then
	# Add the vxlan
	addVxlan $vxlanId $pif $brName
	
	# If the add fails then return failure
	if [ $? -gt 0 ]
	then
		exit 1
	fi
else 
	if [ "$op" == "delete" ]
	then
		# Delete the vxlan
		deleteVxlan $vxlanId $pif $brName
		
		# Always exit with success
		exit 0
	fi
fi

