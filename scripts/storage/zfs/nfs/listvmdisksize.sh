#!/usr/bin/env bash
# $Id: listvmdisksize.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/nfs/listvmdisksize.sh $
# listvmdisksize.sh -- list disk sizes of a VM
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


if [[ $(zfs get -H -o value -p type $diskfs) != filesystem  ]]
then
  printf "Supplied disk fs doesn't exist\n" >&2
  exit 1
fi


if [ "$aflag" == 1 ] 
then
  used=$(zfs list  -H -o used -t filesystem $diskfs)
  if [ $? -gt 0 ] 
  then
    exit 5
  fi
  result=$used
  size=${used:0:$((${#used}-1))}
  suffix=${used:(-1)}
  case $suffix in
    G)   result=$(float_eval "$size*1024*1024*1024")
         ;;
    M)   result=$(float_eval "$size*1024*1024")
         ;;
    K)   result=$(float_eval "$size*1024")
         ;;
  esac

  result=$(echo $result | cut -d"." -f1)   #strip out decimal precision
  printf "$result\n"
  exit 0
fi

if [ "$tflag" == 1 ] 
then
  result=$(ls -l /$diskfs | grep vmi-root | awk '{print $5}')
  if [ "$result" == "" ] 
  then
    result=$(ls -l /$diskfs | grep vmi-data| awk '{print $5}')
  fi
  printf "$result\n"
  exit 0
fi

exit 0
