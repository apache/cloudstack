#!/usr/bin/env bash
# $Id: listvmdisksize.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/iscsi/listvmdisksize.sh $
# listvmdisksize.sh -- list disk sizes of a VM (iscsi mode)
# 

usage() {
  printf "Usage: %s: -d <disk-fs> [-t | -a ] \n" $(basename $0) >&2
}


#####################################################################
# Evaluate a floating point number expression.
function float_eval()
{
    local stat=0
    local result=0.0
    if [[ $# -gt 0 ]]; then
        result=$(echo "scale=0; $*" | bc  2>/dev/null)
        stat=$?
        if [[ $stat -eq 0  &&  -z "$result" ]]; then stat=1; fi
    fi
    echo $result
    return $stat
}

kmg_to_number()
{
  local s=$1;
  local size=${s:0:$((${#s}-1))}
  local result=$1;
  local suffix=${s:(-1)}
  case $suffix in
    G)   result=$(float_eval "$size*1024*1024*1024")
         ;;
    M)   result=$(float_eval "$size*1024*1024")
         ;;
    K)   result=$(float_eval "$size*1024")
         ;;
  esac

  result=$(echo $result | cut -d"." -f1)   #strip out decimal precision
  echo $result
}

#set -x

aflag=
tflag=
aflag=
diskfs=

while getopts 'd:ta' OPTION
do
  case $OPTION in
  d)	dflag=1
		diskfs="$OPTARG"
		;;
  t)	tflag=1
		;;
  a)	aflag=1
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$dflag" != "1"  -a "$tflag$aflag" != "1" ]
then
 usage
 exit 2
fi

if [ ${diskfs:0:1} == / ]
then
  diskfs=${diskfs:1}
fi


if [[ $(zfs get -H -o value -p type $diskfs) != volume  ]]
then
  printf "Supplied disk volume doesn't exist\n" >&2
  exit 1
fi


if [ "$aflag" == 1 ] 
then
  used=$(zfs list  -H -o used -t volume $diskfs)
  if [ $? -gt 0 ] 
  then
    exit 5
  fi
  result=$(kmg_to_number $used)
  printf "$result\n"
  exit 0
fi

if [ "$tflag" == 1 ] 
then
  total=$(zfs list -H -o volsize $diskfs)
  if [ $? -gt 0 ] 
  then
    exit 5
  fi
  result=$(kmg_to_number $total)
  printf "$result\n"
  exit 0
fi

exit 0
