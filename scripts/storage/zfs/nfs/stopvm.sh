#!/usr/bin/env bash
# $Id: stopvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/nfs/stopvm.sh $
# stopvm.sh -- stop one or all vm 
#
#

usage() {
  printf "Usage: %s:  <-a|-l <vm label>> \n" $(basename $0) >&2
}

unmount_disks() {
  disks=$(xm list -l $1 | grep tap:aio | cut -d":" -f3 | cut -d")" -f1)
  imagedirs=$(for d in $disks; do dskdir=$(dirname $d); echo ${#dskdir}:$dskdir; done | sort -n -r | cut -d":" -f2 | uniq)
  for i in $imagedirs
  do
   umount $i
   printf "Unmounted $i result=$?\n"
  done
}

stop_one_vm() {
  if  ! xm list $1  &>/dev/null
  then
    printf "Error: vm $1 does not exist\n" >&2
    return 2
  fi

  
  local domId=0;
  if xm list $1 -l | grep domid
  then
    status=($(xm list $1 | grep $1))
    domId=${status[1]};
  fi
  
  if [ $domId -gt 0 ]
  then
    #Try a graceful shutdown
    xm shutdown $1 -w
    unmount_disks $1
  else
    #printf "Domain $1 is already shutdown\n"
    unmount_disks $1
    xm delete $1
    return 0
  fi
  
  if [ $? -gt 0 ]; then
    #Try an undignified shutdown
    xm destroy $1 -w
    unmount_disks $1
  fi
  
  if [ $? -gt 0 ]; then
    #Try an undignified shutdown
    printf "Failed to terminate instance $1\n">&2
    return 20
  else
    xm delete $1
  fi

  return 0;
    
}

stop_all_vms() {
  for i in `xm list | grep -v Domain-0| grep -v Name| awk -F" " '{print $1}'`
  do 
    stop_one_vm $i
  done

}



lflag=
aflag=

while getopts 'al:' OPTION
do
  case $OPTION in
  l)	lflag=1
		vmname="$OPTARG"
		;;
  a)	aflag=1
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$aflag$lflag" != "1" ]
then
 usage
 exit 2
fi

if [ "$aflag" == "1" ] 
then
  stop_all_vms;
  exit 0
fi

stop_one_vm $vmname

exit 0


