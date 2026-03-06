#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.



# $Id: managesnapshot.sh 11601 2010-08-11 17:26:15Z kris $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.refactor/java/scripts/storage/qcow2/managesnapshot.sh $
# managesnapshot.sh -- manage snapshots for a single disk (create, destroy, rollback, backup)

usage() {
  printf "Usage: %s: -c <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -d <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -r <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -b <path to disk> -n <snapshot name> -p <dest dir> -t <dest file>\n" $(basename $0) >&2
  exit 2
}

qemu_img="cloud-qemu-img"
which $qemu_img >& /dev/null
if [ $? -gt 0 ]
then
   which ccp-qemu-img >& /dev/null
   if [ $? -eq 0 ]
   then
       qemu_img="ccp-qemu-img"
   else
    which qemu-img >& /dev/null
    if [ $? -eq 0 ]
    then
       qemu_img="qemu-img"
    fi
   fi
fi

is_lv() {
	# Must be a block device
	if [ -b "${1}"  -o  -L "{1}" ]; then
		# But not a volume group or physical volume
		lvm vgs "${1}" > /dev/null 2>&1 && return 1
		# And a logical volume
		lvm lvs "${1}" > /dev/null 2>&1 && return 1
	fi
	return 0
}

get_vg() {
	lvm lvs --noheadings --unbuffered --separator=/ "${1}" | cut -d '/' -f 2 | tr -d ' '
}

get_lv() {
	lvm lvs --noheadings --unbuffered --separator=/ "${1}" | cut -d '/' -f 1 | tr -d ' '
}

create_snapshot() {
  local disk=$1
  local snapshotname="$2"
  local failed=0
  is_lv ${disk}
  islv_ret=$?

  if [ ${dmsnapshot} = "yes" ] && [ "$islv_ret" == "1" ]; then
    # Modern LVM snapshot approach
    local lv=`get_lv ${disk}`
    local vg=`get_vg ${disk}`
    local lv_bytes=`blockdev --getsize64 ${disk}`

    # Calculate snapshot size (10% of origin size, minimum 100MB, maximum 10GB)
    local snapshot_size=$((lv_bytes / 10))
    local min_size=$((100 * 1024 * 1024))  # 100MB
    local max_size=$((10 * 1024 * 1024 * 1024))  # 10GB

    if [ ${snapshot_size} -lt ${min_size} ]; then
      snapshot_size=${min_size}
    elif [ ${snapshot_size} -gt ${max_size} ]; then
      snapshot_size=${max_size}
    fi

    # Round to nearest 512-byte multiple (LVM requirement)
    snapshot_size=$(((snapshot_size + 511) / 512 * 512))

    # Create LVM snapshot using native command
    lvm lvcreate -L ${snapshot_size}b -s -n "${snapshotname}" "${vg}/${lv}" >&2
    if [ $? -gt 0 ]; then
      printf "***Failed to create LVM snapshot ${snapshotname} for ${vg}/${lv}\n" >&2
      return 2
    fi

    # Activate the snapshot
    lvm lvchange --yes -ay "${vg}/${snapshotname}" >&2
    if [ $? -gt 0 ]; then
      printf "***Failed to activate LVM snapshot ${snapshotname}\n" >&2
      lvm lvremove -f "${vg}/${snapshotname}" >&2
      return 2
    fi
  elif [ -f "${disk}" ]; then
     $qemu_img snapshot -c "$snapshotname" $disk


     if [ $? -gt 0 ]
     then
       failed=2
       printf "***Failed to create snapshot $snapshotname for path $disk\n" >&2
       $qemu_img snapshot -d "$snapshotname" $disk

       if [ $? -gt 0 ]
       then
          printf "***Failed to delete snapshot $snapshotname for path $disk\n" >&2
       fi
     fi
 else
    failed=3
    printf "***Failed to create snapshot $snapshotname, undefined type $disk\n" >&2
 fi

  return $failed
}

destroy_snapshot() {
  local disk=$1
  local snapshotname="$2"
  local failed=0
  is_lv ${disk}
  islv_ret=$?

  if [ "$islv_ret" == "1" ]; then
    # Modern LVM snapshot deletion
    local lv=`get_lv ${disk}`
    local vg=`get_vg ${disk}`

    # Check if snapshot exists
    if ! lvm lvs "${vg}/${snapshotname}" > /dev/null 2>&1; then
      printf "Snapshot ${vg}/${snapshotname} does not exist or was already deleted\n" >&2
      return 0
    fi

    # Remove the snapshot using native LVM command
    lvm lvremove -f "${vg}/${snapshotname}" >&2
    if [ $? -gt 0 ]; then
      printf "***Failed to remove LVM snapshot ${vg}/${snapshotname}\n" >&2
      return 2
    fi
  elif [ -f $disk ]; then
     #delete all the existing snapshots
     $qemu_img snapshot -l $disk |tail -n +3|awk '{print $1}'|xargs -I {} $qemu_img snapshot -d {} $disk >&2
     if [ $? -gt 0 ]
     then
       failed=2
       printf "Failed to delete snapshot $snapshotname for path $disk\n" >&2
     fi
  else
     failed=3
     printf "***Failed to delete snapshot $snapshotname, undefined type $disk\n" >&2
  fi
  return $failed
}

rollback_snapshot() {
  local disk=$1
  local snapshotname="$2"
  local failed=0
  is_lv ${disk}
  islv_ret=$?

  if [ ${dmrollback} = "yes" ] && [ "$islv_ret" == "1" ]; then
    # Modern LVM snapshot merge (rollback)
    local lv=`get_lv ${disk}`
    local vg=`get_vg ${disk}`

    # Check if snapshot exists
    if ! lvm lvs "${vg}/${snapshotname}" > /dev/null 2>&1; then
      printf "***Snapshot ${vg}/${snapshotname} does not exist\n" >&2
      return 1
    fi

    # Use lvconvert --merge to rollback
    lvm lvconvert --merge "${vg}/${snapshotname}" >&2
    if [ $? -gt 0 ]; then
      printf "***Failed to merge/rollback snapshot ${snapshotname} for ${vg}/${lv}\n" >&2
      return 1
    fi
  elif [ -f ${disk} ]; then
    # File-based snapshot rollback using qemu-img
    $qemu_img snapshot -a $snapshotname $disk

    if [ $? -gt 0 ]
    then
      printf "***Failed to apply snapshot $snapshotname for path $disk\n" >&2
      failed=1
    fi
  else
    printf "***Failed to rollback snapshot $snapshotname, undefined type $disk\n" >&2
    failed=1
  fi

  return $failed
}

backup_snapshot() {
  local disk=$1
  local snapshotname="$2"
  local destPath=$3
  local destName=$4
  local forceShareFlag="-U"

  if [ ! -d $destPath ]
  then
     mkdir -p $destPath >& /dev/null
     if [ $? -gt 0 ]
     then
        printf "Failed to create $destPath\n" >&2
        return 3
     fi
  fi

  is_lv ${disk}
  islv_ret=$?

  if [ ${dmsnapshot} = "yes" ] && [ "$islv_ret" == "1" ] ; then
    local vg=`get_vg ${disk}`
    local scriptdir=`dirname ${0}`

    # Check if snapshot exists using native LVM command
    if ! lvm lvs "${vg}/${snapshotname}" > /dev/null 2>&1; then
      printf "Disk ${disk} has no snapshot called ${snapshotname}.\n" >&2
      return 1
    fi

    # Use native LVM path for backup
    qemuimg_ret=$($qemu_img convert $forceShareFlag -f raw -O qcow2 "/dev/${vg}/${snapshotname}" "${destPath}/${destName}" 2>&1)
    ret_code=$?
    if [ $ret_code -gt 0 ] && ([[ $qemuimg_ret == *"invalid option"*"'U'"* ]] || [[ $qemuimg_ret == *"unrecognized option"*"'-U'"* ]])
    then
      forceShareFlag=""
      $qemu_img convert $forceShareFlag -f raw -O qcow2 "/dev/${vg}/${snapshotname}" "${destPath}/${destName}"
      ret_code=$?
    fi
    if [ $ret_code -gt 0 ]
    then
      printf "${qemu_img} failed to create backup of snapshot ${snapshotname} for disk ${disk} to ${destPath}.\n" >&2
      return 2
    fi
  elif [ -f ${disk} ]; then
    if [[ $disk == *"/snapshots/"* ]]; then
      #Backup volume snapshot
      $qemu_img convert $forceShareFlag -f qcow2 -O qcow2 $disk $destPath/$destName >& /dev/null
      ret_code=$?

      if [ $ret_code -gt 0 ]
      then
        printf "Failed to backup $snapshotname for disk $disk to $destPath\n" >&2
        return 2
      fi
    else
      # Backup VM snapshot
      qemuimg_ret=$($qemu_img snapshot $forceShareFlag -l $disk 2>&1)
      ret_code=$?
      if [ $ret_code -gt 0 ] && ([[ $qemuimg_ret == *"invalid option"*"'U'"* ]] || [[ $qemuimg_ret == *"unrecognized option"*"'-U'"* ]]); then
        forceShareFlag=""
        qemuimg_ret=$($qemu_img snapshot $forceShareFlag -l $disk 2>&1)
        ret_code=$?
      fi

      if [ $ret_code -gt 0 ] || [[ ! $qemuimg_ret == *"$snapshotname"* ]]; then
        printf "there is no $snapshotname on disk $disk\n" >&2
        return 1
      fi

      qemuimg_ret=$($qemu_img convert $forceShareFlag -f qcow2 -O qcow2 -l snapshot.name=$snapshotname $disk $destPath/$destName 2>&1)
      ret_code=$?
      if [ $ret_code -gt 0 ] && ([[ $qemuimg_ret == *"invalid option"*"'U'"* ]] || [[ $qemuimg_ret == *"unrecognized option"*"'-U'"* ]]); then
        forceShareFlag=""
        qemuimg_ret=$($qemu_img convert $forceShareFlag -f qcow2 -O qcow2 -l snapshot.name=$snapshotname $disk $destPath/$destName 2>&1)
        ret_code=$?
      fi

      if [ $ret_code -gt 0 ] && [[ $qemuimg_ret == *"convert: invalid option -- 'l'"* ]]; then
        $qemu_img convert $forceShareFlag -f qcow2 -O qcow2 -s $snapshotname $disk $destPath/$destName >& /dev/null
        ret_code=$?
      fi

      if [ $ret_code -gt 0 ]; then
        printf "Failed to backup $snapshotname for disk $disk to $destPath\n" >&2
        return 2
      fi
    fi
  else
    printf "***Failed to backup snapshot $snapshotname, undefined type $disk\n" >&2
    return 3
  fi
  return 0
}

revert_snapshot() {
  local snapshotPath=$1
  local destPath=$2
  local output_format="qcow2"
  if [ -b "$destPath" ]; then
    output_format="raw"
  fi

  ${qemu_img} convert -f qcow2 -O ${output_format} "$snapshotPath" "$destPath" || \
   ( printf "${qemu_img} failed to revert snapshot ${snapshotPath} to disk ${destPath}.\n" >&2; return 2 )
  return 0
}
#set -x

cflag=
dflag=
rflag=
bflag=
vflag=
nflag=
pathval=
snapshot=
tmplName=
deleteDir=
dmsnapshot=no
dmrollback=no

while getopts 'c:d:r:n:b:v:p:t:f' OPTION
do
  case $OPTION in
  c)	cflag=1
	pathval="$OPTARG"
	;;
  d)    dflag=1
        pathval="$OPTARG"
        ;;
  r)    rflag=1
        pathval="$OPTARG"
        ;;
  b)    bflag=1
        pathval="$OPTARG"
        ;;
  v)    vflag=1
        pathval="$OPTARG"
        ;;
  n)	nflag=1
	snapshot="$OPTARG"
	;;
  p)    destPath="$OPTARG"
        ;;
  t)    tmplName="$OPTARG"
	;;
  f)    deleteDir=1
	;;
  ?)	usage
	;;
  esac
done

if modprobe dm-snapshot; then
  dmsnapshot=yes
  dmsetup targets | grep -q "^snapshot-merge" && dmrollback=yes
fi

[ -z "${snapshot}" ] && usage

[ -b "$pathval" ] && snapshot=`echo "${snapshot}" | md5sum -t | awk '{ print $1 }'`

if [ "$cflag" == "1" ]
then
  create_snapshot $pathval "$snapshot"
  exit $?
elif [ "$dflag" == "1" ]
then
  destroy_snapshot $pathval "$snapshot" $deleteDir
  exit $?
elif [ "$bflag" == "1" ]
then
  [ -z "${destPath}" -o -z "${tmplName}" ] && usage
  backup_snapshot $pathval $snapshot $destPath $tmplName
  exit $?
elif [ "$rflag" == "1" ]
then
  rollback_snapshot $pathval "$snapshot" $destPath
  exit $?
elif [ "$vflag" == "1" ]
then
  revert_snapshot $pathval $destPath
  exit $?
fi


exit 0
