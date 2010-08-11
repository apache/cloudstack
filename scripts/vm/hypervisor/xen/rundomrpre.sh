#/bin/bash
# $Id: rundomrpre.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xen/rundomrpre.sh $

# set -x

mntpath() {
  local vmname=$1
  echo "/images/local/$vmname"
}

mount_local() {
   local vmname=$1
   local disk=$2
   local path=$(mntpath $vmname)

   mkdir -p ${path}
   mount $disk ${path} 

   return $?
}

umount_local() {
   local vmname=$1
   local path=$(mntpath $vmname)

   umount  $path
   local ret=$?
   
   rm -rf $path
   return $ret
}

change_password() {
  local vmname=$1
  local path=$(mntpath $vmname)
  local newpasswd=$2

  echo $newpasswd | chroot $path passwd --stdin root
  return $?
}

set_ssh_key() {
   local vmname=$1
   local keyfile=$2
   local path=$(mntpath $vmname)
   cat $keyfile > ${path}/root/.ssh/authorized_keys2 
   return 0
}

patch() {
   local vmname=$1
   local patchfile=$2
   local path=$(mntpath $vmname)

   local oldmd5=
   local md5file=${path}/md5sum
   [ -f ${md5file} ] && oldmd5=$(cat ${md5file})
   local newmd5=$(md5sum $patchfile | awk '{print $1}')

   if [ "$oldmd5" != "$newmd5" ]
   then
     tar xzf $patchfile -C ${path}
     echo ${newmd5} > ${md5file}
   fi

   return 0
}

#
# To use existing console proxy .zip-based package file
#
patch_console_proxy() {
   local vmname=$1
   local patchfile=$2
   local path=$(mntpath $vmname)
   local oldmd5=
   local md5file=${path}/usr/local/vmops/consoleproxy/md5sum

   [ -f ${md5file} ] && oldmd5=$(cat ${md5file})
   local newmd5=$(md5sum $patchfile | awk '{print $1}')

   if [ "$oldmd5" != "$newmd5" ]
   then
     echo "All" | unzip $patchfile -d ${path}/usr/local/vmops/consoleproxy
     echo ${newmd5} > ${md5file}
   fi

   return 0
}

consoleproxy_svcs() {
   local vmname=$1
   local path=$(mntpath $vmname)

   chroot ${path} /sbin/chkconfig vmops on
   chroot ${path} /sbin/chkconfig domr_webserver off  
   chroot ${path} /sbin/chkconfig haproxy off ; 
   chroot ${path} /sbin/chkconfig dnsmasq off
   chroot ${path} /sbin/chkconfig sshd off
   chroot ${path} /sbin/chkconfig httpd off

   cp ${path}/etc/sysconfig/iptables-domp ${path}/etc/sysconfig/iptables
}

routing_svcs() {
   local vmname=$1
   local path=$(mntpath $vmname)

   chroot ${path} /sbin/chkconfig vmops off
   chroot ${path} /sbin/chkconfig domr_webserver on ; 
   chroot ${path} /sbin/chkconfig haproxy on ; 
   chroot ${path} /sbin/chkconfig dnsmasq on
   chroot ${path} /sbin/chkconfig sshd on
   chroot ${path} /sbin/chkconfig httpd on

   cp ${path}/etc/sysconfig/iptables-domr ${path}/etc/sysconfig/iptables
}

lflag=
xflag=
prerun=
vmname=
DISKDIR="/dev/disk/by-vm"
MIRRORDIR="/dev/md"
template=domR

while getopts 't:v:i:m:e:E:a:A:g:l:n:d:b:B:p:I:N:Mx:X:' OPTION
do
  case $OPTION in
  l)	lflag=1
	vmname="$OPTARG"
        ;;
  x)    xflag=1
        prerun="$OPTARG"
        ;;
  t)    template="$OPTARG"
        ;;
  *)    ;;
  esac
done

if [ "$Mflag" == "" ]
then
  diskdir=$DISKDIR
else
  diskdir=$MIRRORDIR
fi
rootdisk=$diskdir/$vmname-root

if [ "$rootdisk" == "$diskdir/" ] 
then
  printf "Error: No root disk found\nVM $vmname not started\n" >&2
  exit 2
fi

if [ "$xflag" != "" ]
then
  prerun=$(echo $prerun | sed 's/,/ /g')
  for opt in $prerun
  do
    # search for foo=bar pattern and cut out foo
    KEY=$(echo $opt | cut -d= -f1)
    VALUE=$(echo $opt | cut -d= -f2)
    case $KEY in 
      password)
          password=$VALUE
          ;;
      sshkey)
          sshkey=$VALUE
          ;;
      patch)
          patch=$VALUE
     esac
  done
fi

mount_local $vmname $rootdisk

if [ $? -gt 0 ]
then
  printf "Failed to mount disk $rootdisk for $vmname\n" >&2
  exit 1
fi

if [ "$password" != "" ]
then
  change_password $vmname $password
  if [ $? -gt 0 ]
  then
    printf "Failed to change password for $vmname\n" >&2
    umount_local $vmname
    exit 2
  fi
fi

if [ "$sshkey" != "" ]
then
  set_ssh_key $vmname $sshkey
  if [ $? -gt 0 ]
  then
    printf "Failed to change ssh key for $vmname\n" >&2
    umount_local $vmname
    exit 3
  fi
fi

if [ "$patch" == "" ]
then
  if [ -f $(dirname $0)/../../../../patch.tgz ]
  then
    patch=$(dirname $0)/../../../../patch.tgz 
  fi
fi

if [ "$patch" != "" ]
then
  patch $vmname $patch
  if [ $? -gt 0 ]
  then
    printf "Failed to apply patch $patch to $vmname\n" >&2
    umount_local $vmname
    exit 4
  fi
fi

cpfile=$(dirname $0)/../../../../console-proxy-premium.zip
[ -f $cpfile ] || $(dirname $0)/../../../../console-proxy.zip
[ -f $cpfile ] || $(dirname $0)/../../console-proxy-premium.zip
[ -f $cpfile ] || $(dirname $0)/../../console-proxy.zip
[ -f $cpfile ] || $(dirname $0)/console-proxy-premium.zip
[ -f $cpfile ] || $(dirname $0)/console-proxy.zip
if [[ "$template" == "domP" &&  -f $cpfile ]]
then
  patch_console_proxy $vmname $cpfile
  if [ $? -gt 0 ]
  then
    printf "Failed to apply patch $cpfile to $vmname\n" >&2
    umount_local $vmname
    exit 4
  fi
fi

[ "$template" == "domP" ] && consoleproxy_svcs $vmname
[ "$template" == "domR" ] && routing_svcs $vmname


umount_local $vmname

exit $?
