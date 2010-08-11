#!/usr/bin/env bash
# $Id: createvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/iscsi/comstar/createvm.sh $
# createvm.sh -- create a vm image directory by cloning (iscsi mode)
# OpenSolaris

usage() {
  printf "Usage: %s: -t <template-fs> -d <disk-fs> -i <instance-fs> -u <user fs>\n" $(basename $0) >&2
}

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

#ensure that the instance fs is mounted within the user fs
check_valid_userfs() {
  local ifs=$1
  local ufs=$2
  local child=${ifs#$ufs}
  if [ ${#child} -eq $(( ${#ifs}-${#ufs} )) ] 
  then  
    return 0
  else  
   printf "instance fs $ifs is not contained within user fs $ufs. Bailing\n" >&2
   exit 3;
  fi
}

get_instance() {
  echo $(basename $1)
}

get_latest_snapshot() {
  local fs=$1
  local tsnap=$(echo $fs | cut -f1 -d'@')
  if [ "$tsnap" == "$fs" ]
  then
    snap=$(zfs list -r -H -o name -S name -S creation -t snapshot $tsnap| head -1)
    if [ -z $snap ] 
    then
      return 1
    fi
    echo $snap
    return 0
  else
    echo $fs
    return 
  fi
}

#trap 'echo "killed..." >&3; exec 3>&-; exit 9' TERM INT KILL
#set -x

tflag=
iflag=
uflag=
dflag=
sflag=
start=
end=
#debug=1

while getopts 't:i:u:d:s:' OPTION
do
  case $OPTION in
  t)	tflag=1
		templatefs="$OPTARG"
		;;
  i)	iflag=1
		instancefs="$OPTARG"
		;;
  u)	uflag=1
		userfs="$OPTARG"
		;;
  d)	dflag=1
		diskfs="$OPTARG"
		;;
  s)	sflag=1
		diskfs=""
		disksize="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$tflag$iflag$uflag" != "111" ]
then
 usage
 exit 2
fi

#either -d or -s but not both
if [ "$dflag$sflag" == "11" ]
then
  usage
  exit 2
fi

#if user has provided leading slash, strip it out
if [ ${userfs:0:1} == / ]
then
  userfs=${userfs:1}
fi

if [ ${templatefs:0:1} == / ]
then
  templatefs=${templatefs:1}
fi

if [ ${instancefs:0:1} == / ]
then
  instancefs=${instancefs:1}
fi

if [ -n "$diskfs" ]
then
  if [ ${diskfs:0:1} == / ]
  then
    diskfs=${diskfs:1}
  fi
fi

if [ -n "$disksize" ]
then
  suffix=${disksize:(-1)}
  echo $suffix
  case $suffix in
    G)   
         ;;
    [0-9])   disksize=${disksize}G
         ;;
    *)   printf "Error in disk size: expect G as a suffix or no suffix\n"
         exit 2
         ;;
  esac
  
fi

instance=$(get_instance $instancefs)
if [ -n "$debug" ]
then
  exec 3<>$(dirname $0)/../../../logs/create$instance.log
fi

check_valid_userfs $instancefs $userfs


#if user has provided the exact snapshot of the template fs, use it, 
#else get the latest snapshot
tsnap=$(echo $templatefs | cut -f1 -d'@')
if [ "$tsnap" == "$templatefs" ]
then
  logzfs "start: get_latest_snapshot" 
  tsnap=$(get_latest_snapshot $templatefs)
  logzfs "end: get_latest_snapshot" 
  if [ -z "$tsnap" ] 
  then
    printf "No snapshots exist of filesystem $templatefs..bailing\n" >&2
    exit 4
  fi
else
  tsnap=$templatefs
  templatefs=$(echo $templatefs | cut -f1 -d'@')  #strip out snap version
fi

snapt=$(echo $tsnap | cut -f2 -d'@')

if [ -n "$diskfs" ]
then
  logzfs "start: get_latest_snapshot" 
  disksnap=$(get_latest_snapshot $diskfs)
  logzfs "end: get_latest_snapshot" 
  if [ -z "$disksnap" ] 
  then
    printf "No snapshots exist of disk filesystem $diskfs..bailing\n" >&2
    exit 6
  fi
  diskfs=$(echo $diskfs | cut -d'@' -f1)
fi

#Clone root disk and associated files
printf "Cloning root disk $tsnap to $instancefs\n" >&2
logzfs "start: zfs clone -p $tsnap $instancefs" 
zfs clone -p $tsnap $instancefs  
if [ $? -gt 0 ] 
then
  printf "Failed to clone root disk $tsnap\n" >&2
  exit 5
fi
logzfs "end: zfs clone -p $tsnap $instancefs" 

#get root disk/swapdisk volume contained within templatefs
logzfs "start: zfs list -H -o name -t volume -r $templatefs"  
for vol in $(zfs list -H -o name -t volume -r $templatefs )
do
  #clone the snapshot
  logzfs "start: zfs clone $vol@$snapt $instancefs/$(basename $vol)" 
  zfs clone $vol@$snapt $instancefs/$(basename $vol)
  if [ $? -gt 0 ] 
  then
    printf "Failed to clone root disk $vol$snapt\n" >&2
    exit 5
  fi
  sbdadm create-lu /dev/zvol/dsk/$instancefs/$(basename $vol)
  if [ $? -gt 0 ]
  then
    printf "Failed to create the lun for /dev/zvol/dsk/$instancefs/$(basename $vol)\n" >&2
    exit 10
  fi
  logzfs "end: zfs clone $vol@$snapt $instancefs/$(basename $vol)" 
done

rc=0
#Clone datadisk
if [ -n "$diskfs" ]
then
  logzfs "start: zfs clone $disksnap $instancefs/datadisk1-$(basename $diskfs)" 
  zfs clone $disksnap $instancefs/datadisk1-$(basename $diskfs)
  if [ $? -gt 0 ] 
  then
  printf "Failed to clone data disk $disksnap\n" >&2
    exit 5
  fi
  sbdadm create-lu /dev/zvol/dsk/$instancefs/datadisk1-$(basename $diskfs)
  if [ $? -gt 0 ]
  then
    printf "Failed to create the lun for /dev/zvol/dsk/$instancefs/datadisk1-$(basename $diskfs)\n" >&2
    exit 10
  fi
  logzfs "end: zfs clone $disksnap $instancefs/datadisk1-$(basename $diskfs)" 
fi

if [ -n "$disksize" ]
then
  logzfs "start: zfs create $instancefs/datadisk1" 
  zfs create -V $disksize -s $instancefs/datadisk1 #-s for sparse
  if [ $rc -gt 0 ] 
  then
    printf "Failed to create data disk $instancefs/datadisk1\n" >&2
    exit 6
  fi
  sbdadm create-lu /dev/zvol/dsk/$instancefs/datadisk1
  if [ $? -gt 0 ]
  then
    printf "Failed to create the lun for /dev/zvol/dsk/$instancefs/datadisk1\n" >&2
    exit 10
  fi
  logzfs "end: zfs create $instancefs/datadisk1" 
fi

exit 0
