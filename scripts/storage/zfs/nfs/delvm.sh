#!/usr/bin/env bash
# $Id: delvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/nfs/delvm.sh $
# delvm.sh -- delete a cloned image used for a vm
#

usage() {
  printf "Usage: %s: -i <instance-fs> -u <user fs>\n" $(basename $0) >&2
}


#set -x

iflag=
uflag=
userfs=
instancefs=

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

if [ "$iflag" == 1 ] 
then
  zfs destroy -r $instancefs  
  if [ $? -gt 0 ] 
  then
    printf "Failed to destroy instance fs\n" >&2
    exit 5
  fi
fi

if [ "$uflag" == 1 ] 
then
  zfs destroy -r $userfs  
  if [ $? -gt 0 ] 
  then
    printf "Failed to destroy user fs\n" >&2
    exit 5
  fi
fi

exit 0
