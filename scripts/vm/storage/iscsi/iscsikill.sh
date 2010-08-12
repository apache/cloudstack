#!/usr/bin/env bash
# $Id: iscsikill.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/vm/storage/iscsi/iscsikill.sh $
#
# iscsikill.sh
#
# kill all vms with disk to a iscsi connection and log out of the iscsi connection.
#

usage() {
  printf "Usage: %s: -r <iscsi session id> -t <target iqn> -p <portal> \n" $(basename $0) >&2
}

#set -x

rflag=
tflag=
pflag=0

while getopts 'r:t:p:' OPTION
do
  case $OPTION in
  r)	rflag=1
		sessionid="$OPTARG"
		;;
  t)	tflag=1
        target="$OPTARG"
		;;
  p)	pflag=1
        portal="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$rflag$tflag$pflag" != "111" ]
then
 usage
 exit 2
fi

state=`iscsiadm -m session -r $sessionid -P 3 | grep "iSCSI Session State:" | awk '{print $NF}'`
if [ $state == "FREE" -o $state == "FAILED" ]
then
  for disk in `iscsiadm -m session -r $sessionid -P 3 | grep "Attached scsi disk" | awk '{print "/dev/"$4}'`
  do
    vmname=`ls -l /dev/disk/by-vm | grep $disk | grep -v datadisk | awk '{print $9}'`
    if [ "$vmname" != "" ]
    then
      vmname=${vmname%-*}
      echo "Shutting down vm: $vmname"
      xm shutdown $vmname > /dev/null 2>&1
      if [ $? -eq 0 ]
      then
        echo "Deleting vm: $vmname"
        xm delete $vmname > /dev/null 2>&1 
	    if [ $? -eq 0 ]
	    then
	      echo "Deleted vm: $vmname"
	    else
          echo "Failed to delete vm: $vmname"
        fi
      else
        echo "Failed to shutdown vm: $vmname"
      fi
    fi
  done
  
  iscsiadm -m session -r $sessionid -u
  iscsiadm -m node -T $target -p $portal -o delete
else
  echo "session is no longer in FREE or FAILED state"
fi


