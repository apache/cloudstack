#!/usr/bin/env bash
# $Id: createvnet.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/network/vnet/createvnet.sh $
# runvm.sh -- start a vm from a directory containing the kernel and os images
# iscsi mode
#

usage() {
  printf "Usage: %s: -v <vnet-id>\n" $(basename $0) >&2
}

VNETD=cloud-vnetd

check_vnetd_running() {
 /sbin/lsmod | grep vnet_module > /dev/null
 local module_loaded=$?
 if [[ ( -z $(pidof $VNETD) ) &&  ( $module_loaded -ne 0 ) ]]
 then
    # Try to auto load vnet
    service $VNETD start &> /dev/null

    if [ $? -gt 0 ]
    then
        printf 'vnet: Neither userspace daemon running nor kernel module loaded!, not starting vm '"$vmname\n" >&2
        exit 2
    fi
 fi

}

create_vnet () {
    local vnetid=$1
    vnetid=$(echo $vnetid | tr '[A-Z]' '[a-z]')

    local bridgeid="vnbr""$vnetid"
    local vnifid="vnif""$vnetid"
    local longvnetid="0000:0000:0000:0000:0000:0000:0000:""$vnetid"

    local br_exist=0
    ifconfig $bridgeid &> /dev/null
    br_exist=$?

    # Create the vnet even if it already exists. /usr/sbin/vn operation is
    # idempotent
    nice -n -1 $VN vnet-create -b ${bridgeid} ${longvnetid} &> /dev/null

    sleep 0.5
    ifconfig ${bridgeid} &> /dev/null
    if [ $? -gt 0 ]
    then
       local succ=0
       for i in 1 2 3
       do
    	 nice -n -1 $VN vnet-create -b ${bridgeid} ${longvnetid} &> /dev/null
	 sleep $i
    	 ifconfig ${bridgeid} &> /dev/null
	 if [ $? -eq 0 ]
    	 then
  	    succ=1
	    break;
         fi
       done
       if [ $succ -eq 0 ]
       then
       	 echo "failed to create bridge"
         exit 1
       fi
    fi
    ifconfig ${vnifid} &> /dev/null
    if [ $? -gt 0 ]
    then
       echo "failed to create vnif"
       exit 2
    fi

    #echo $bridgeid $vnifid $longvnetid
    if [ $br_exist -gt 0 ]
    then
      sleep 0.5 
      ifconfig $vnifid down
      sleep 0.5 
      ifconfig $vnifid up
    fi
    iptables-save | grep $bridgeid > /dev/null
    if [ $? -gt 0 ]
    then
      iptables -I FORWARD -i $bridgeid -o $bridgeid -j ACCEPT
    fi
    return 0
}


#set -x

vflag=

while getopts 'v:' OPTION
do
  case $OPTION in
  v)	vflag=1
		vnetid="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$vflag" != "1" ]
then
 usage
 exit 2
fi

VN=cloud-vn

#make sure vnetd is running
check_vnetd_running

# Create the vnet locally if not already created
if  ! create_vnet "$vnetid" ; then
  printf "Failed to create vnet, exiting\n" >&2
  exit 5
fi

exit 0
