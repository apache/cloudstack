#!/usr/bin/env bash
# $Id: runvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xen/runvm.sh $
# runvm.sh -- start a vm from a directory containing the kernel and os images
# iscsi mode
#

usage() {
  printf "Usage: %s: -v <vnet-id> -i <ip-address> -m <memory in MB>  -a <mac address> -g <gateway ip addr> -c <vnc console #> -w <vnc password>-l <vm label>  -n <# cores> -u <compute units in pct> -r <vif rate in Mbits/sec> -R <multicast rate in Mbits/sec> -b <bootloader> <image-dir>\n" $(basename $0) >&2
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

limit_multicast() {
  local vmname=$1
  local mcrate=$2 ## Mbits/sec
  local domid=$(xm domid $vmname)
  local vifname=$(_vifname $vmname)
  vifname=${vifname}.0
  local privip=$(ip addr show xenbr0 | awk "/^.*inet.*xenbr0\$/{print \$2}" | sed -n '1 s,/.*,,p')
 
  if [ "$mcrate" -eq 0 ]
  then
    return 0
  fi
 
  local pkts
  local burst
  let pkts=$mcrate*2000/3
  if [ $pkts -gt 10000 ]
  then
    pkts=10000
  fi
  burst=$pkts

  ebtables -A  FORWARD --pkttype-type broadcast  -i $vifname -j mark --mark-set $domid --mark-target CONTINUE
  ebtables -A  FORWARD --pkttype-type multicast  -i $vifname -j mark --mark-set $domid --mark-target CONTINUE
  ebtables -A FORWARD --mark ${domid} --limit ${pkts}/sec --limit-burst ${burst} -j ACCEPT
  ebtables -A FORWARD --mark ${domid} -j DROP
}

create_vnet () {
    local vnetid=$1
    vnetid=$(echo $vnetid | tr '[A-Z]' '[a-z]')

    local bridgeid="vnbr""$vnetid"
    local vnifid="vnif""$vnetid"
    local longvnetid="0000:0000:0000:0000:0000:0000:0000:""$vnetid"

    eval "$2=$bridgeid"
    local br_exist=0
    ifconfig $bridgeid &> /dev/null
    br_exist=$?

    # Create the vnet even if it already exists. /usr/sbin/vn operation is
    # idempotent
    vn vnet-create -b ${bridgeid} ${longvnetid} &> /dev/null

    #echo $bridgeid $vnifid $longvnetid
    if [ $br_exist -gt 0 ]
    then
      sleep 0.5 
      ifconfig $vnifid down
      sleep 0.5 
      ifconfig $vnifid up
    fi
    return 0
}


# check if gateway domain is up and running
check_gw() {
  pings=1
  while  ! ping -c 1 -n -q  $1 > /dev/null  && [ $pings -ne $2 ] 
  do
   let pings=pings+1
  done

  printf "##check_gw: number of pings=$pings\n"
  [ $pings -eq $2 ] && return 1
  return 0;
}

#Append an entry into dhcp hosts file and knock the dhcp server on its head
add_dhcp_entry() {
  local gw=$1
  local mac=$2
  local ip=$3
  local vm=$4
   ssh -p 3922 -o StrictHostKeyChecking=no -i ./id_rsa root@$gw "/root/edithosts.sh $mac $ip $vm" >/dev/null
  if [ $? -gt 0 ]
  then
     $5=1
     return 1
  fi
  return 0
}

#set -x
DISKDIR="/dev/disk/by-vm/"
MIRRORDIR="/dev/md/"

vflag=
iflag=
mflag=
Mflag=
aflag=
gflag=
lflag=
cflag=
bflag=
Rflag=
rflag=
vcpus=0
cpucap=0
rate=200
mcrate=10
vncpwd="password"
bootloader="PyGrub"

while getopts 'Mv:i:m:a:g:l:c:n:u:w:b:R:r:' OPTION
do
  case $OPTION in
  v)	vflag=1
		vnetid="$OPTARG"
		;;
  i)	iflag=1
		ipaddr="$OPTARG"
		;;
  r)	rflag=1
		rate="$OPTARG"
		;;
  R)	Rflag=1
		mcrate="$OPTARG"
		;;
  m)	mflag=1
		ram="$OPTARG"
		;;
  M)	Mflag=1
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
  b)	bflag=1
		bootloader="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done
bootloader=$(echo $bootloader | tr '[A-Z]' '[a-z]' )
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

if [ -z "$imagedir" ]
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
if [ "$Mflag" == "" ]
then
  diskdir=$DISKDIR
else
  diskdir=$MIRRORDIR
fi
kernel="$imagedir/"$(ls $imagedir | grep vmlinuz)
ramdisk="$imagedir/"$(ls $imagedir | grep initrd)
rootdisk=$diskdir/$vmname-root
swapdisk=$diskdir/$vmname-swap
datadisk1=$diskdir/$vmname-datadisk-1
datadisk2=$diskdir/$vmname-datadisk-2

#set -x

if [ "$rootdisk" == "$diskdir/" ] 
then
  printf "Error: No root disk found\nVM $vmname not started\n" >&2
  exit 2
fi
if [ "$datadisk1" == "$diskdir/" ] 
then
  printf "Error: No data disk found\nVM $vmname not started\n" >&2
  exit 2
fi

linux=0
windows=0
pygr=0

case "$bootloader" in 
  "pygrub") 
         pygr=1
         ;;

  "hvm")
         kernel="/usr/lib/xen/boot/hvmloader"
         windows=1
         builder="hvm"
         ;;
  
   "external")
         if [ "$kernel" != "$imagedir/" ] && [ "$ramdisk" != "$imagedir/" ]
         then
           linux=1
           builder="linux"
         else
           printf "Error: No kernel&ramdisk found\nVM $vmname not started\n" >&2
           exit 2
         fi
         ;;
   *) printf "Error: invalid bootloader\nVM $vmname not started\n" >&2
      exit 2
esac

if [ "$kernel" != "$imagedir/" ] && [ "$ramdisk" != "$imagedir/" ]
then
  #Linux kernel
  linux=1
  builder="linux"
elif [ -f "$imagedir/pygrub" ]
then
  #pygrub linux image
  pygr=1
else
  kernel="/usr/lib/xen/boot/hvmloader"
  windows=1
  builder="hvm"
fi


# Create the vnet locally if not already created
bridge=""
if  ! create_vnet "$vnetid" bridge ; then
  printf "Failed to create vnet, exiting\n" >&2
  exit 5
fi

# create  the vm (linux)
if [ $linux -eq 1 ]; then
   xm new /dev/null  kernel="$kernel" name="$vmname"  disk="phy:$rootdisk,xvda1,w" disk="phy:$swapdisk,xvda2,w" disk="phy:$datadisk1,xvda3,w" ramdisk="$ramdisk" memory=$ram vif="mac=$macaddr,bridge=$bridge,rate=${rate}Mb/s" dhcp="dhcp" root="/dev/xvda1 ro" vnc=1 vfb="type=vnc,vncdisplay=$vncconsole,vncpasswd=$vncpwd" ip="$ipaddr" extra="fastboot" vcpus=$vcpus cpu_cap=$cpucap on_crash="destroy"
elif [ $pygr -eq 1 ]; then
   xm new /dev/null  bootloader="/usr/bin/pygrub" name="$vmname"  disk="phy:$rootdisk,xvda,w" disk="phy:$datadisk1,xvdb,w"  memory=$ram vif="mac=$macaddr,bridge=$bridge,rate=${rate}Mb/s" vfb="type=vnc,vncdisplay=$vncconsole,vncpasswd=$vncpwd" extra="fastboot" vcpus=$vcpus cpu_cap=$cpucap on_crash="destroy"
else
  #create the vm (windows/HVM)
  xm new /dev/null  kernel="$kernel" name="$vmname"  disk="phy:$rootdisk,hda,w" disk="phy:$datadisk1,hdb,w" builder="$builder" memory=$ram vif="mac=$macaddr,bridge=$bridge,type=ioemu,rate=${rate}Mb/s" vnc=1 vncdisplay="$vncconsole" vncpasswd="$vncpwd" usbdevice="tablet" $wincpuopts on_crash="destroy"
fi

if [ $? -gt 0 ]; then
   exit 10
fi

#Kick off the vm
xm start $vmname

if [ $? -gt 0 ]; then
   exit 20
fi

add_acct_rule $vmname 0
limit_multicast $vmname $mcrate

exit 0


