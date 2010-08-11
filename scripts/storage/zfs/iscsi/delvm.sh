#!/usr/bin/env bash
# $Id: delvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/iscsi/delvm.sh $
# delvm.sh -- delete a cloned image used for a vm
#

usage() {
  printf "Usage: %s: -i <instance-fs> -u <user fs>\n" $(basename $0) >&2
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
userfs=
instancefs=
start=
end=
debug=1

while getopts 'i:u:' OPTION
do
  case $OPTION in
  i)	iflag=1
		instancefs="$OPTARG"
		;;
  u)	uflag=1
		userfs="$OPTARG"
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

if [[ "$iflag" == 1 && $(zfs get -H -o value -p type $instancefs) != filesystem  ]]
then
  printf "Supplied instance fs doesn't exist\n" >&2
  exit 1
fi

if [[ "$uflag" == 1 && $(zfs get -H -o value -p type $userfs) != filesystem ]]
then
  printf "Supplied user fs doesn't exist\n" >&2
  exit 1
fi
instance=$(get_instance $instancefs)
if [ -n "$debug" ]
then
  exec 3<>$(dirname $0)/../../logs/del$instance.log
fi

if [ "$iflag" == 1 ] 
then
  printf "Going to destroy $instancefs and its children\n"
  logzfs "start: zfs destroy -r -f $instancefs  "
  zfs destroy -r -f $instancefs  
  rc=$?
  logzfs "end: zfs destroy -r -f $instancefs  "
  if [ $rc -gt 0 ] 
  then
    numc=$(zfs list -Hr $instancefs 2> /dev/null| wc -l)
    if [ "$numc" -eq 1 ]
    then
      printf "Trying again to destroy instance fs $instancefs \n" >&2
      zfs destroy -r -f $instancefs  
    else
      printf "Failed to destroy instance fs $instancefs, numchildren=$numc\n" >&2
      exit 5
    fi
  fi
fi

if [ "$uflag" == 1 ] 
then
  printf "Going to destroy $userfs and its children\n"
  logzfs "start: zfs destroy -r -f $userfs"
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
