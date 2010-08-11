#!/usr/bin/env bash
# $Id: runvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/nfs/runvm.sh $
# runvm.sh -- start a vm from a directory containing the kernel and os images
#
#

usage() {
  printf "Usage: %s: -v <vnet-id> -i <ip-address> -m <memory in MB>  -a <mac address> -g <gateway ip addr> -c <vnc console #> -w <vnc password>-l <vm label>  -n <# cores> -u <compute unites in pct> <image-dir>\n" $(basename $0) >&2
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

is_xen_3_3 () {
  local a;
  local b;
  a=$(xm info | grep xen_minor | cut -d":" -f2); 
  b=$(echo $a)
  if [ $b -lt 3 ]
  then
    return 1
  fi

  return 0
}


get_dom0_ip () {
  if ifconfig eth0 &> /dev/null;
  then
    eval "$1=$(ifconfig eth0 | awk '/inet addr/ {split ($2,A,":"); print A[2]}')"
    return 0
  elif  ifconfig eth1 &> /dev/null;
  then
    eval "$1=$(ifconfig eth1 | awk '/inet addr/ {split ($2,A,":"); print A[2]}')"
    return 0;
  fi
  return 1;
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


# check if gateway domain is up and running
check_gw() {
  ping -c 1 -n -q $1 > /dev/null
  if [ $? -gt 0 ]
  then
    sleep 1
    ping -c 1 -n -q $1 > /dev/null
  fi
  return $?;
}

#Append an entry into dhcp hosts file and knock the dhcp server on its head
add_dhcp_entry() {
  local gw=$1
  local mac=$2
  local ip=$3
  local vm=$4
  ssh -o StrictHostKeyChecking=no -i ./id_rsa root@$gw "/root/edithosts.sh $mac $ip $vm" >/dev/null
  if [ $? -gt 0 ]
  then
     $5=1
     return 1
  fi
  return 0
}

#set -x

vflag=
iflag=
mflag=
aflag=
gflag=
lflag=
cflag=
vcpus=0
cpucap=0
vncpwd="password"

while getopts 'v:i:m:a:g:l:c:n:u:w:' OPTION
do
  case $OPTION in
  v)	vflag=1
		vnetid="$OPTARG"
		;;
  i)	iflag=1
		ipaddr="$OPTARG"
		;;
  m)	mflag=1
		ram="$OPTARG"
		;;
  a)	aflag=1
		macaddr="$OPTARG"
		;;
  g)	gflag=1
		gateway="$OPTARG"
		;;
  l)	lflag=1
		vmname="$OPTARG"
		;;
  c)	cflag=1
		vncconsole="$OPTARG"
		;;
  w)	wflag=1
		vncpwd="$OPTARG"
		;;
  n)	nflag=1
		vcpus="$OPTARG"
		;;
  u)	uflag=1
		cpucap="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$vflag$mflag$iflag$aflag$gflag$lflag$cflag" != "1111111" ]
then
 usage
 exit 2
fi

wincpuopts=""
if [ "$vcpus" == 0 -a "$cpucap" == 0 ]
then
  #Windows doesn't like it
  wincpuopts=""
elif [ "$vcpus" == 0 ]
then
  wincpuopts="cpu_cap=$cpucap"
elif [ "$cpucap" == 0 ]
then
  wincpuopts="vcpus=$vcpus"
else
  wincpuopts="vcpus=$vcpus cpu_cap=$cpucap"
fi


shift $(($OPTIND - 1))
imagedir=$1

if [ -z $imagedir ]
then
  usage
  exit 2
fi
if [ "$vmname" == gateway ]
then
  printf "Error: $vmname is illegal (this is the hostname of domR)\n" >&2
  exit 2
fi
if  xm list $vmname  &>/dev/null
then
  printf "Error: vm $vmname already exists\n" >&2
  exit 2
fi

if [ "$ipaddr" == "10.1.1.1" ]
then
  printf "Error: 10.1.1.1 is the gateway and cannot be assigned to a vm\n" >&2
  exit 2
fi

#make sure vnetd is running
check_vnetd_running

# grab the kernel, bootloader and disk images
kernel="$imagedir/"$(ls $imagedir | grep vmlinuz)
ramdisk="$imagedir/"$(ls $imagedir | grep initrd)
rootdisk="$imagedir/"$(ls $imagedir | grep vmi-root)
swapdisk="$imagedir/"$(ls $imagedir | grep swap)
datadisk1="$imagedir/"$(ls $imagedir | grep vmi-data1)
datadisk2="$imagedir/"$(ls $imagedir | grep vmi-data2)

#set -x

if [ "$rootdisk" == "$imagedir/" ] 
then
  printf "Error: No root disk found\nVM $vmname not started\n" >&2
  exit 2
fi
if [ "$datadisk1" == "$imagedir/" ] 
then
  #look for subdirs called 'datadisk'
  i=0
  datadisks=( )
  for diskfs in $(find $imagedir -type d | grep datadisk)
  do
    datadisks[$i]=$(find $diskfs | grep vmi-data | head -1)  #expect only 1 disk
    let i=i+1
  done
  datadisk1=${datadisks[0]};
  if [ -z "$datadisk1" ]
  then
    printf "Error: No data disk found\nVM $vmname not started\n" >&2
    exit 2
  fi 
fi

linux=0
windows=0
pygr=0

if [ "$kernel" != "$imagedir/" ] && [ "$ramdisk" != "$imagedir/" ]
then
  #Linux kernel
  linux=1
  builder="linux"
  device_model=""
elif [ -f "$imagedir/pygrub" ]
then
  #pygrub linux image
  pygr=1
else
  kernel="/usr/lib/xen/boot/hvmloader"
  windows=1
  builder="hvm"
  device_model="/usr/lib64/xen/bin/qemu-dm"
fi

#get dom 0 ip to figure out which ip to bind vnc to
dom0ip=""
get_dom0_ip dom0ip

# check if gateway domain is up and running
if ! check_gw "$gateway"
then
   printf "Unable to ping the gateway domain, exiting\n" >&2
   exit 3
fi

#Append an entry into dhcp hosts file and knock the dhcp server on its head
added=0
if  ! add_dhcp_entry $gateway $macaddr $ipaddr $vmname $added 
then
   printf  "Unable add dhcp entry on gateway (reason=$added), exiting\n" >&2
   exit 4
fi

# Create the vnet locally if not already created
bridge=""
if  ! create_vnet "$vnetid" bridge ; then
  printf "Failed to create vnet, exiting\n" >&2
  exit 5
fi

#hvm disk prefix for 3.1 is file:, for 3.3 it is tap:aio
hvmdisk="tap:aio"
if ! is_xen_3_3
then
  hvmdisk="file"
fi

# create  the vm (linux)
if [ $linux -eq 1 ]; then
   xm new /dev/null  kernel="$kernel" name="$vmname"  disk="tap:aio:$rootdisk,xvda1,w" disk="tap:aio:$swapdisk,xvda2,w" disk="tap:aio:$datadisk1,xvda3,w" ramdisk="$ramdisk" memory=$ram vif="mac=$macaddr,bridge=$bridge" dhcp="dhcp" root="/dev/xvda1 ro" vnc=1 vnclisten="$dom0ip" vfb="type=vnc,vncdisplay=$vncconsole,vncpasswd=$vncpwd" ip="$ipaddr" extra="fastboot" vcpus=$vcpus cpu_cap=$cpucap
elif [ $pygr -eq 1 ]; then
   xm new /dev/null  bootloader="/usr/bin/pygrub" name="$vmname"  disk="tap:aio:$rootdisk,sda,w" disk="tap:aio:$datadisk1,sdb,w"  memory=$ram vif="mac=$macaddr,bridge=$bridge" dhcp="dhcp" root="/dev/sda ro" vnc=1 vnclisten="$dom0ip" vfb="type=vnc,vncdisplay=$vncconsole,vncpasswd=$vncpwd" ip="$ipaddr" extra="fastboot" vcpus=$vcpus cpu_cap=$cpucap
else
  #create the vm (windows/HVM)
  xm new /dev/null  kernel="$kernel" name="$vmname"  disk="$hvmdisk:$rootdisk,hda,w" disk="$hvmdisk:$datadisk1,hdb,w" builder="$builder" device_model="$device_model" memory=$ram vif="mac=$macaddr,bridge=$bridge,type=ioemu" dhcp="dhcp"  vnc=1 vnclisten="$dom0ip"  vncdisplay="$vncconsole" vncpasswd="$vncpwd" usbdevice="tablet" localtime="yes" $wincpuopts
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


