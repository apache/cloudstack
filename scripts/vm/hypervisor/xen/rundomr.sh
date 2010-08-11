#!/usr/bin/env bash
# $Id: rundomr.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xen/rundomr.sh $
# rundomr.sh -- start a domR  (iscsi mode)
#
#
# set -x

usage() {
  printf "Usage: %s: -v <vnet-id> -i <eth1 ip-address> -m <memory in MB>  -a <eth0 mac address> -A <eth1 mac address> -e <eth0 ip> -E <eth0 mask> -p <eth2 mac address> -P <eth2 ip addr> -n <eth1 eth1mask> -N <eth2 eth1mask> -g <default gw ip> -l <vm label> <image-dir>\n" $(basename $0) >&2
}

_vifname() {
 local vmname=$1
 local domid=$(xm domid $vmname)
 echo vif${domid}
}

add_acct_rule() {
  local vmname=$1
  local intf=$2

  local vifname=$(_vifname $vmname)

  iptables -A FORWARD -m physdev  --physdev-out $vifname.$intf -j ACCEPT

  return $?
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
    vnetid=$(echo $vnetid | tr '[A-Z]' '[a-z]')

    local bridgeid="vnbr""$vnetid"
    local vnifid="vnif""$vnetid"
    local longvnetid="0000:0000:0000:0000:0000:0000:0000:""$vnetid"

    eval "$2=$bridgeid"

    # Create the vnet even if it already exists. /usr/sbin/vn operation is
    # idempotent
    vn vnet-create -b ${bridgeid} ${longvnetid} &> /dev/null

    #echo $bridgeid $vnifid $longvnetid
    sleep 1
    ifconfig $vnifid down
    sleep 1
    ifconfig $vnifid up

    return 0
}

tflag=
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
Bflag=
pflag=
eflag=
xflag=
Xflag=
Eflag=
Iflag=
Nflag=
Mflag=
dnsoptions=
prerun=
postrun=
vlanId=
privbr=
DISKDIR="/dev/disk/by-vm"
MIRRORDIR="/dev/md"
eth0ip=10.1.1.1
eth0mask=255.255.255.0
template=domR
option=$@

while getopts 't:v:i:m:e:E:a:A:g:l:n:d:b:B:p:I:N:Mx:X:' OPTION
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
  x)	xflag=1
		prerun="$OPTARG"
		;;
  e)	eflag=1
		eth0ip="$OPTARG"
		;;
  E)	Eflag=1
		eth0mask="$OPTARG"
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
  M)	Mflag=1 ;;
  t)	tflag=1
  		template="$OPTARG"
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

if [ -x $(dirname $0)/rundomrpre.sh ]
then
   $(dirname $0)/rundomrpre.sh $option
   if [ $? -gt 0 ]
   then
     printf "Error: pre run hook failed\n" >&2
     exit 4
   fi
fi

#make sure vnetd is running
if [ "$template" == "domR" ]
then
	check_vnetd_running
fi

if [ "$Mflag" == "" ]
then
  diskdir=$DISKDIR
else
  diskdir=$MIRRORDIR
fi

# grab the disk images
rootdisk=$diskdir/$vmname-root

if [ "$rootdisk" == "$diskdir/" ] 
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
check_ip $eth2ip

# Create the vnet locally if not already created
if [ "$template" == "domR" ]
then
	vnbr=""
	if  ! create_vnet "$vnetid" vnbr ; then
	  printf "Failed to create vnet, exiting\n" >&2
	  exit 5
	fi
fi

eth2br=
if [ "$vlanId" == "untagged" ] 
then
  eth2br=",bridge=xenbr1"
else
  eth2br=",bridge=xenbr1.$vlanId"
fi

if [ -n "$privbr" ] 
then
  eth1br=",bridge=$privbr"
fi
#echo $eth2br

# create the domR. Pass eth1 ip configuration in the "extra" flag
if [ "$template" == "domR" ]
then
	xm new /dev/null bootloader="/usr/bin/pygrub" name="$vmname"  disk="phy:$rootdisk,xvda,w"   memory=$ram vif="mac=$eth0mac,bridge=$vnbr" vif="$eth1mac$eth1br" vif="mac=$eth2mac$eth2br"  ip="$eth1ip" extra="fastboot eth0ip=$eth0ip eth0mask=$eth0mask eth1ip=$eth1ip eth1mask=$eth1mask eth2ip=$eth2ip eth2mask=$eth2mask gateway=$gateway $dnsoptions" on_crash="destroy"
else
	xm new /dev/null bootloader="/usr/bin/pygrub" name="$vmname"  disk="phy:$rootdisk,xvda1,w"   memory=$ram vif="bridge=$vnbr" vif="$eth1mac$eth1br" vif="mac=$eth2mac$eth2br"  root="/dev/xvda1 ro" ip="$eth1ip" extra="fastboot template=domP eth1ip=$eth1ip eth1mask=$eth1mask eth2ip=$eth2ip eth2mask=$eth2mask gateway=$gateway $dnsoptions" on_crash="destroy"
fi	

if [ $? -gt 0 ]; then
   exit 10
fi

#Kick off the vm
xm start $vmname

if [ $? -gt 0 ]; then
   exit 20
fi

exit 0


