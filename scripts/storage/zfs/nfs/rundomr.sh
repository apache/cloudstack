#!/usr/bin/env bash
# $Id: rundomr.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/nfs/rundomr.sh $
# rundomr.sh -- start a domR 
#
#
#set -x

usage() {
  printf "Usage: %s: -v <vnet-id> -i <eth1 ip-address> -m <memory in MB>  -a <eth0 mac address> -A <eth1 mac address>  -p <eth2 mac address> -P <eth2 ip addr> -n <eth1 eth1mask> -N <eth2 eth1mask> -g <default gw ip> -l <vm label> <image-dir>\n" $(basename $0) >&2
}

check_vnetd_running() {
 /sbin/lsmod | grep vnet_module > /dev/null
 local module_loaded=$?
 if [[ ( -z $(pidof vnetd) ) &&  ( $module_loaded -ne 0 ) ]]
 then
   printf 'vnet: Neither userspace daemon running nor kernel module loaded!, not starting vm '"$vmname\n" >&2
   exit 2
 fi

}

# check if ip address is already used
check_ip() {
  ping -c 1 -n -q $1 > /dev/null
  if [ $? -eq 0 ] 
  then
     printf "Error: ip address $1 already in use...exiting\n" >&2
     exit 2
  fi
}


create_vnet () {
    local vnetid=$1
    local bridgeid="vnbr""$vnetid"
    local vnifid="vnif""$vnetid"
    local longvnetid="0000:0000:0000:0000:0000:0000:0000:""$vnetid"

    eval "$2=$bridgeid"

    # Create the vnet even if it already exists. /usr/sbin/vn operation is
    # idempotent
    vn vnet-create -b ${bridgeid} ${longvnetid} &> /dev/null

    #echo $bridgeid $vnifid $longvnetid
    return 0
}


vflag=
iflag=
mflag=
aflag=
gflag=
lflag=
nflag=
Aflag=
dflag=
bflag=
pflag=
Iflag=
Nflag=
dnsoptions=
xenbr=

while getopts 'v:i:m:a:A:g:l:n:d:b:p:I:N:' OPTION
do
  case $OPTION in
  v)	vflag=1
		vnetid="$OPTARG"
		;;
  i)	iflag=1
		eth1ip="$OPTARG"
		;;
  m)	mflag=1
		ram="$OPTARG"
		;;
  a)	aflag=1
		eth0mac="$OPTARG"
		;;
  A)	Aflag=1
		eth1mac="mac=$OPTARG"
		;;
  g)	gflag=1
		gateway="$OPTARG"
		;;
  n)	nflag=1
		eth1mask="$OPTARG"
		;;
  l)	lflag=1
		vmname="$OPTARG"
		;;
  d)	dflag=1
		dnsoptions="$OPTARG"
		;;
  p)	pflag=1
		eth2mac="$OPTARG"
		;;
  I)	Iflag=1
		eth2ip="$OPTARG"
		;;
  N)	Nflag=1
		eth2mask="$OPTARG"
		;;
  b)	bflag=1
		xenbr="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

#-d & -b is optional
if [ "$Aflag$vflag$mflag$iflag$aflag$nflag$gflag$lflag" != "11111111" ]
then
 usage
 exit 2
fi

shift $(($OPTIND - 1))
imagedir=$1

if [ -z $imagedir ]
then
  usage
  exit 2
fi


if  xm list $vmname  &>/dev/null
then
  printf "Error: domR $vmname already exists\n" >&2
  exit 2
fi

#make sure vnetd is running
check_vnetd_running

# grab the kernel, bootloader and disk images
kernel="$imagedir/"$(ls $imagedir | grep vmlinuz)
ramdisk="$imagedir/"$(ls $imagedir | grep initrd)
rootdisk="$imagedir/"$(ls $imagedir | grep vmi-root)
swapdisk="$imagedir/"$(ls $imagedir | grep swap)

if [ "$rootdisk" == "$imagedir/" ] 
then
  printf "Error: No root disk found\nVM $vmname not started\n" >&2
  exit 2
fi


if [ "$kernel" == "$imagedir/" ] || [ "$ramdisk" == "$imagedir/" ]
then
  printf "Could not find kernel and initrd images, exiting\n" >&2
  exit 2
fi

#ensure no ip address clash
check_ip $eth1ip



# Create the vnet locally if not already created
bridge=""
if  ! create_vnet "$vnetid" bridge ; then
  printf "Failed to create vnet, exiting\n" >&2
  exit 5
fi

eth2br=
if [ -n "$xenbr" ] 
then
  eth2br=",bridge=$xenbr"
fi
#echo $eth2br

# create the domR. Pass eth1 ip configuration in the "extra" flag
xm new /dev/null  kernel="$kernel" name="$vmname"  disk="tap:aio:$rootdisk,xvda1,w" disk="tap:aio:$swapdisk,xvda2,w"  ramdisk="$ramdisk" memory=$ram vif="mac=$eth0mac,bridge=$bridge" vif="$eth1mac,bridge=eth0" vif="mac=$eth2mac$eth2br"  root="/dev/xvda1 ro" ip="$eth1ip" extra="fastboot eth1ip=$eth1ip eth1mask=$eth1mask eth2ip=$eth2ip eth2mask=$eth2mask gateway=$gateway $dnsoptions"


if [ $? -gt 0 ]; then
   exit 10
fi

#Kick off the vm
xm start $vmname

if [ $? -gt 0 ]; then
   exit 20
fi

exit 0


