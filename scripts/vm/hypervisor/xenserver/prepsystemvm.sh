#/bin/bash
# $Id: prepsystemvm.sh 10800 2010-07-16 13:48:39Z edison $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x/java/scripts/vm/hypervisor/xenserver/prepsystemvm.sh $

#set -x

mntpath() {
  local vmname=$1
  echo "/mnt/$vmname"
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


patch_scripts() {
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
   local md5file=${path}/usr/local/cloud/systemvm/md5sum

   [ -f ${md5file} ] && oldmd5=$(cat ${md5file})
   local newmd5=$(md5sum $patchfile | awk '{print $1}')

   if [ "$oldmd5" != "$newmd5" ]
   then
     echo "All" | unzip $patchfile -d ${path}/usr/local/cloud/systemvm >/dev/null 2>&1
     chmod 555 ${path}/usr/local/cloud/systemvm/run.sh
     find ${path}/usr/local/cloud/systemvm/ -name \*.sh | xargs chmod 555
     echo ${newmd5} > ${md5file}
   fi

   return 0
}

consoleproxy_svcs() {
   local vmname=$1
   local path=$(mntpath $vmname)

   chroot ${path} /sbin/chkconfig cloud on
   chroot ${path} /sbin/chkconfig postinit on
   chroot ${path} /sbin/chkconfig domr_webserver off
   chroot ${path} /sbin/chkconfig haproxy off ;
   chroot ${path} /sbin/chkconfig dnsmasq off
   chroot ${path} /sbin/chkconfig sshd on
   chroot ${path} /sbin/chkconfig httpd off
   chroot ${path} /sbin/chkconfig nfs off
   chroot ${path} /sbin/chkconfig nfslock off
   chroot ${path} /sbin/chkconfig rpcbind off
   chroot ${path} /sbin/chkconfig rpcidmap off

   cp ${path}/etc/sysconfig/iptables-consoleproxy ${path}/etc/sysconfig/iptables
}

secstorage_svcs() {
   local vmname=$1
   local path=$(mntpath $vmname)

   chroot ${path} /sbin/chkconfig cloud on
   chroot ${path} /sbin/chkconfig postinit on
   chroot ${path} /sbin/chkconfig domr_webserver off
   chroot ${path} /sbin/chkconfig haproxy off ;
   chroot ${path} /sbin/chkconfig dnsmasq off
   chroot ${path} /sbin/chkconfig sshd on
   chroot ${path} /sbin/chkconfig httpd off
    

   cp ${path}/etc/sysconfig/iptables-secstorage ${path}/etc/sysconfig/iptables
   mkdir -p ${path}/var/log/cloud
}

routing_svcs() {
   local vmname=$1
   local path=$(mntpath $vmname)

   chroot ${path} /sbin/chkconfig cloud off
   chroot ${path} /sbin/chkconfig domr_webserver on ; 
   chroot ${path} /sbin/chkconfig haproxy on ; 
   chroot ${path} /sbin/chkconfig dnsmasq on
   chroot ${path} /sbin/chkconfig sshd on
   chroot ${path} /sbin/chkconfig nfs off
   chroot ${path} /sbin/chkconfig nfslock off
   chroot ${path} /sbin/chkconfig rpcbind off
   chroot ${path} /sbin/chkconfig rpcidmap off
   cp ${path}/etc/sysconfig/iptables-domr ${path}/etc/sysconfig/iptables
}

lflag=
dflag=

while getopts 't:l:d:' OPTION
do
  case $OPTION in
  l)	lflag=1
	vmname="$OPTARG"
        ;;
  t)    tflag=1
        vmtype="$OPTARG"
        ;;
  d)    dflag=1
        rootdisk="$OPTARG"
        ;;
  *)    ;;
  esac
done

if [ "$lflag$tflag$dflag" != "111" ]
then
  printf "Error: Not enough parameter\n" >&2
  exit 1
fi


mount_local $vmname $rootdisk

if [ $? -gt 0 ]
then
  printf "Failed to mount disk $rootdisk for $vmname\n" >&2
  exit 1
fi

if [ -f $(dirname $0)/patch.tgz ]
then
  patch_scripts $vmname $(dirname $0)/patch.tgz
  if [ $? -gt 0 ]
  then
    printf "Failed to apply patch patch.zip to $vmname\n" >&2
    umount_local $vmname
    exit 4
  fi
fi

cpfile=$(dirname $0)/systemvm-premium.zip
if [ "$vmtype" == "consoleproxy" ] || [ "$vmtype" == "secstorage" ]  && [ -f $cpfile ]
then
  patch_console_proxy $vmname $cpfile
  if [ $? -gt 0 ]
  then
    printf "Failed to apply patch $patch $cpfile to $vmname\n" >&2
    umount_local $vmname
    exit 5
  fi
fi

# domr is 64 bit, need to copy 32bit chkconfig to domr
# this is workaroud, will use 32 bit domr
dompath=$(mntpath $vmname)
cp /sbin/chkconfig $dompath/sbin
# copy public key to system vm
cp $(dirname $0)/id_rsa.pub  $dompath/root/.ssh/authorized_keys
#empty known hosts
echo "" > $dompath/root/.ssh/known_hosts

if [ "$vmtype" == "router" ]
then
  routing_svcs $vmname
  if [ $? -gt 0 ]
  then
    printf "Failed to execute routing_svcs\n" >&2
    umount_local $vmname
    exit 6
  fi
fi


if [ "$vmtype" == "consoleproxy" ]
then
  consoleproxy_svcs $vmname
  if [ $? -gt 0 ]
  then
    printf "Failed to execute consoleproxy_svcs\n" >&2
    umount_local $vmname
    exit 7
  fi
fi

if [ "$vmtype" == "secstorage" ]
then
  secstorage_svcs $vmname
  if [ $? -gt 0 ]
  then
    printf "Failed to execute secstorage_svcs\n" >&2
    umount_local $vmname
    exit 8
  fi
fi


umount_local $vmname

exit $?
