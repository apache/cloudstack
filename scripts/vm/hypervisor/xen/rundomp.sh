#!/usr/bin/env bash
# $Id: rundomp.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xen/rundomp.sh $
#
# rundomp.sh -- start a console proxy domU (domP)  (iscsi mode)
# 	Usage : rundomp [options] <image-dir>
#		-m memory in MB
#		-i console proxy VM private IP address
#		-A console proxy VM private mac address
#		-n console proxy VM private network mask
#		-p console proxy VM public mac address
#		-I console proxy VM public IP address
#		-N console proxy VM public network mask
#		-d "DNS configuration"
#		-b network device name towards public network
#		-g default gateway
#		-l VM label
#
# eth1 : VM private network
# eth2 : VM public network
# 

usage() {
  printf "Usage: %s: -m <memory in MB> -i <private ip address> -A <private mac address> -p <public mac address> -P <public ip address> -n <private network mask> -N <public network mask> -g <default gw ip> -l <vm label> <image-dir>\n" $(basename $0) >&2
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

iflag=
mflag=
gflag=
lflag=
nflag=
Aflag=
dflag=
bflag=
pflag=
Iflag=
Bflag=
Nflag=
dnsoptions=
vlanId=
DISKDIR="/dev/disk/by-vm/"

while getopts 'i:m:A:g:l:n:d:b:B:p:I:N:' OPTION
do
  case $OPTION in
  i)	iflag=1
		eth1ip="$OPTARG"
		;;
  m)	mflag=1
		ram="$OPTARG"
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
		vlanId="$OPTARG"
		;;
  B)	Bflag=1
		privbr="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

#-d & -b is optional
if [ "$Aflag$mflag$iflag$nflag$gflag$lflag" != "111111" ]
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

# grab the kernel, bootloader and disk images
# kernel="$imagedir/"$(ls $imagedir | grep vmlinuz)
# ramdisk="$imagedir/"$(ls $imagedir | grep initrd)
# swapdisk=$DISKDIR/$vmname-swap

rootdisk=$DISKDIR/$vmname-root

if [ "$rootdisk" == "$DISKDIR/" ] 
then
  printf "Error: No root disk found\nVM $vmname not started\n" >&2
  exit 2
fi

#if [ "$kernel" == "$imagedir/" ] || [ "$ramdisk" == "$imagedir/" ]
#then
#  printf "Could not find kernel and initrd images, exiting\n" >&2
#  exit 2
#fi

#ensure no ip address clash
check_ip $eth2ip

eth2br=
eth1br=
if [ -n "$privbr" ] 
then
  eth1br=",bridge=$privbr"
fi

eth2br=
if [ "$vlanId" == "untagged" ] 
then
  eth2br=",bridge=xenbr1"
else
  eth2br=",bridge=xenbr1.$vlanId"
fi

# create the domU. 
#xm new /dev/null kernel="$kernel" name="$vmname" disk="phy:$rootdisk,xvda1,w" disk="phy:$swapdisk,xvda2,w" ramdisk="$ramdisk" memory=$ram vif="" vif="$eth1mac$eth1br" vif="mac=$eth2mac$eth2br" root="/dev/xvda1 ro" extra="fastboot eth1ip=$eth1ip eth1mask=$eth1mask eth2ip=$eth2ip eth2mask=$eth2mask gateway=$gateway $dnsoptions"
xm new /dev/null bootloader="/usr/bin/pygrub" name="$vmname" disk="phy:$rootdisk,xvda1,w" memory=$ram vif="" vif="$eth1mac$eth1br" vif="mac=$eth2mac$eth2br" root="/dev/xvda1 ro" extra="fastboot eth1ip=$eth1ip eth1mask=$eth1mask eth2ip=$eth2ip eth2mask=$eth2mask gateway=$gateway $dnsoptions" on_crash="destroy"

if [ $? -gt 0 ]; then
   exit 10
fi

#Kick off the vm
xm start $vmname

if [ $? -gt 0 ]; then
   exit 20
fi

exit 0
