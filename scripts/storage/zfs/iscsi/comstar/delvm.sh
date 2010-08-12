#!/usr/bin/env bash
# $Id: delvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/comstar/delvm.sh $
# delvm.sh -- delete a cloned image used for a vm
# OpenSolaris

usage() {
  printf "Usage: %s: -i <instance-fs> -l <lun,lun,lun> | -u <user fs>\n" $(basename $0) >&2
}

delete_lu() {  # <instance-or-user-fs>
  local lu=$1
  local result=
  result=$(sbdadm delete-lu $lu 2>&1)
  if [ $? -ne 0 ]
  then
    if [ $? -ne 1 ]
    then
      printf "Unable to delete lun: $result\n" >&2
      return 4
    fi
    echo $result | grep "not found"
    if [ $? -ne 0 ]
    then
      printf "Unable to delete lun: $result\n" >&2
      return 5
    fi
  fi
  return 0
}

delete_all_lu() {
  local lu_list=$(sbdadm list-lu | grep $1 | awk '{print $1}')
  local lu
  for lu in $lu_list
  do
      delete_lu $lu
  done
}

#set -x
logzfs() {
  if [ -z "$debug" ]
  then
    return
  fi
  local elapsed=0
  printf "$(date +'%H:%M:%S') $*\n"  >&3
  if [ "$(echo $1| awk '{print $1}')" == "start:" ]
  then
    start="$(/usr/gnu/bin/date +'%s')"
    return
  fi
  if [ "$(echo $1| awk '{print $1}')" == "end:" ]
  then
    end="$(/usr/gnu/bin/date +'%s')"
    let elapsed=end-start 
    printf "t=$elapsed $*\n"  >&3
  fi
}
get_instance() {
  echo $(basename $1)
}

iflag=
uflag=
lflag=
userfs=
instancefs=
start=
end=
tgtname=
#debug=1

while getopts 'i:u:l:' OPTION
do
  case $OPTION in
  i)	iflag=1
	instancefs="$OPTARG"
		;;
  u)	uflag=1
	userfs="$OPTARG"
		;;
  l)    lflag=1
        tgtname="$OPTARG"
                ;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$iflag$uflag" != "1" -a "$iflag$uflag" != "11" ]
then
 usage
 exit 2
fi

if [[ -n $instancefs && ${instancefs:0:1} == / ]]
then
  instancefs=${instancefs:1}
fi

if [[ -n $userfs && ${userfs:0:1} == / ]]
then
  userfs=${userfs:1}
fi

if [ "$iflag" == "1" ]
then 
  result=$(zfs get -H -o value -p type $instancefs 2>&1)
  if [ $? -eq 0 ]
  then
    if [ $result != filesystem ]
	then
      printf "Supplied instance fs doesn't exist\n" >&2
      exit 1
    fi
  else
    echo $result | grep "dataset does not exist"
    if [ $? -eq 0 ]
    then
      exit 0
    else 
      printf "Unable to get information on $instancefs due to $result\n" >&2
      exit 2
    fi
  fi
fi

if [[ "$uflag" == 1 && $(zfs get -H -o value -p type $userfs) != filesystem ]]
then
  printf "Supplied user fs doesn't exist\n" >&2
  exit 1
fi

instance=$(get_instance $instancefs)
if [ -n "$debug" ]
then
  exec 3<>$(dirname $0)/../../../logs/del$instance.log
fi

if [ "$iflag" == 1 ] 
then
  printf "Going to destroy $instancefs and its children\n"
  logzfs "start: zfs destroy -r -f $instancefs  "
  if [ "$lflag" == 1 ]
  then
    for l in `echo $tgtname | tr ',' ' '`
    do 
      lu=`echo $l | cut -d':' -f5`
      delete_lu $lu
      if [ $? -ne 0 ]
      then
        exit $?
      fi
    done
  else
    delete_all_lu $instancefs
  fi

  result=$(zfs destroy -r -f $instancefs 2>&1)
  rc=$?
  logzfs "end: zfs destroy -r -f $instancefs  "
  if [ $rc -gt 0 ] 
  then
    echo $result | grep "dataset does not exist"
    if [ $? -ne 0 ]
    then
      sleep 10
      printf "Trying again to destroy instance fs $instancefs \n" >&2
      result=$(zfs destroy -r -f $instancefs 2>&1)  
      if [ $? -ne 0 ]
      then
        printf "Failed to destroy instance fs $instancefs, numchildren=$numc, result=$result\n" >&2
        exit 5
      fi
    fi
  fi
fi

if [ "$uflag" == 1 ] 
then
  printf "Going to destroy $userfs and its children\n"
  logzfs "start: zfs destroy -r -f $userfs"
  delete_all_lu $userfs
  zfs destroy -r -f $userfs  
  rc=$?
  logzfs "end: zfs destroy -r -f $userfs"
  if [ $rc -gt 0 ] 
  then
    numc=$(zfs list -Hr $userfs 2> /dev/null| wc -l)
    if [ "$numc" -eq 1 ]
    then
      printf "Trying again to destroy user fs $userfs \n" >&2
      zfs destroy -r -f $userfs  
    else
      printf "Failed to destroy user fs $userfs, numchildren=$numc\n" >&2
      exit 5
    fi
  fi
fi

exit 0
