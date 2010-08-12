#!/usr/bin/env bash
# $Id: mountvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/vm/storage/nfs/mountvm.sh $
# mounvm.sh -- mount a remote nfs directory as the image directory
#
#

usage() {
  printf "Usage: %s: [-u | -m ] -h <nfs-host> -r <remote-dir> -l <local dir>\n" $(basename $0) >&2
}

# check if server is up and running
check_nfs_server() {
  ping -c 1 -n -q $1 > /dev/null
  return $?;
}

#check if somebody else has mounted this disk
#Only issue a warning since Solaris
#sometimes keeps around mounts for a longer time
check_in_use() {
  local warn=0
  local nfshost=$1
  local remotedir=$2
  warn=$(ssh -o StrictHostKeyChecking=no -i ./id_rsa root@$nfshost "showmount -a | grep $remotedir" |  wc -l)
  if [ $warn -gt 1 ] 
  then
    printf "!!!Warning!!!! $remotedir is already mounted by $warn other hosts: ">&2
    warn=$(ssh -o StrictHostKeyChecking=no -i ./id_rsa root@$nfshost "showmount -a | grep $remotedir" | cut -d":" -f1)
   for ips in $warn 
   do
     printf "$ips, "
   done
   printf "\n"
  fi
}

# unmount a local directory and all data disks within
unmount_all() {
  local localdir=$1
  
  #unmount all datadisks 
  for diskfs in $(find $localdir -type d | grep datadisk)
  do
     umount  $diskfs >&2  #ignore errors
     printf "Unmounting $diskfs result=$?\n"
  done

  #unmount the root disk
  umount  $localdir >&2  #ignore errors
  printf "Unmounting $localdir result=$?\n"
}

#set -x

hflag=
rflag=
lflag=
uflag=
mflag=

while getopts 'umxh:r:l:' OPTION
do
  case $OPTION in
  h)	hflag=1
		nfshost="$OPTARG"
		;;
  r)	rflag=1
		remotedir="$OPTARG"
		;;
  l)	lflag=1
		localdir="$OPTARG"
		;;
  u)    uflag=1
		;;
  m)    mflag=1
		;;
  x)    set -x	
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$hflag$rflag$lflag" != "111" ] && [ "$uflag$lflag" != "11" ]
then
 usage
 exit 2
fi

if [ "$uflag$mflag" != "1" ]  && [ "$uflag$mflag" != "" ]
then
  printf "Specify one of -u (unmount) or -m (mount)\n" >&2
  usage
  exit 2
fi

if [ "$uflag" == "1" ]
then
  unmount_all $localdir
  exit 0
fi

#create the local dir if necessary
if ! mkdir -p $localdir
then
  printf "Unable to create local directory, exiting\n" >&2
  exit 2
fi

#check if the nfs server is up and running
if ! check_nfs_server $nfshost
then
   printf "Unable to ping the nfs host, exiting\n" >&2
   exit 3
fi

#warn if the remote disk has already been mounted by someone else
#check_in_use $nfshost $remotedir

#mount the root disk
mount -t nfs $nfshost:$remotedir $localdir -o intr,rsize=32768,wsize=32768,hard
if [ $? -gt 0 ] 
then
  printf "Failed to mount $remotedir at $localdir\n" >&2
  exit 5
fi

#mount all datadisks as well
for diskfs in $(find $localdir -type d | grep datadisk)
do
   disk=$(basename $diskfs)
   mount -t nfs $nfshost:$remotedir/$disk $diskfs
   if [ $? -gt 0 ] 
   then
     printf "Failed to mount $remotedir/$disk at $diskfs\n" >&2
     unmount_all $localdir #undo what we did
     exit 5
   fi
done

exit 0
