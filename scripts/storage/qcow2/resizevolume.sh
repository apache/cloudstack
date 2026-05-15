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



# resizevolume.sh -- resize a volume

usage() {
  printf "Usage: %s: -c <current-volume-size> -s <new-volume-size> -p <volume path> -v <vm instance name> -t <storage-type> -r <shrink-bool>\n" $(basename $0) >&2
}

getdevmappername() {
  local path=$1
  local devmappername=`readlink -f $path |cut -d/ -f3`
  if [[ $devmappername =~ "dm-" ]]
  then
    dmname=$devmappername
    return 0
  else
    return 1;
  fi
}

getdevmappersize() {
  local dm=$1
  if [ ! -e "/sys/block/${dm}/size" ]
  then
    log "unable to find ${dm} in /sys/block" 1
    exit 1
  fi
  actualsize=$((`cat /sys/block/${dm}/size`*512));

  if [[ -z "$actualsize" ]]
  then
    log "unable to find actual size of ${dm}" 1
    exit 1
  fi
  return 0
}

# log "message" 1  <-- prints to stdout as well as log, to pass error up to cloudstack
# log "message" prints only to log file
# variable shouldwelog controls whether we print to log file
log() {
  local d=`date`
  local msg=${1}
  local stdout=${2}

  if [ ! -z "$stdout" ]
  then
    echo $1
  fi

  if [ $shouldwelog -eq 1 ]
  then
    echo "$d - $1" >> /var/log/cloudstack/agent/resizevolume.log
  fi
}

failshrink() {
  # if this is a shrink operation, fail if commands will shrink the volume and we haven't signed of on shrinking
  if [ $actualsize -gt $newsize ]
  then
    if [ "$shrink" == "false" ]
    then
      log "result would shrink the volume from $actualsize to $newsize, but confirmation to shrink wasn't passed. Shrink='$shrink'" 1
      exit 1
    fi
  fi
}

notifyqemu() {
  #move this back into cloudstack libvirt calls once the libvirt java bindings support block resize
  #we try to inform hypervisor of new size, but don't fail if we can't
  if `virsh help 2>/dev/null | grep -q blockresize`
  then
    if `virsh domstate $vmname >/dev/null 2>&1`
    then
      sizeinkb=$(($newsize/1024))
      devicepath=$(virsh domblklist $vmname | grep $path | awk '{print $1}')
      virsh blockresize --path $devicepath --size $sizeinkb $vmname >/dev/null 2>&1
      retval=$?
      if [ -z $retval ] || [ $retval -ne 0 ]
      then
        log "failed to live resize $path to size of $sizeinkb kb" 1
      else
        liveresize='true'
      fi
    fi
  fi
}

resizelvm() {
  local dmname=''
  local actualsize=''
  local liveresize='false'

  ##### sanity checks #####
  if ! `lvresize --version > /dev/null 2>&1`
  then
    log "unable to resolve executable 'lvresize'" 1
    exit 1
  fi

  if ! `virsh --version > /dev/null 2>&1`
  then
    log "unable to resolve executable 'virsh'" 1
    exit 1
  fi
  ##### end sanity #####

  if ! getdevmappername $path
  then
    log "unable to resolve a device mapper dev from $path" 1
    exit 1
  fi

  getdevmappersize $dmname

  if [ $actualsize -ne $currentsize ]
  then
    log "disk isn't the size we think it is: cloudstack said $currentsize, disk said $actualsize."
  fi

  # if this is a shrink operation, fail if commands will shrink the volume and we haven't signed of on shrinking
  failshrink

  output=`lvresize -f -L ${newsize}B $path 2>&1`
  retval=$?

  if [ -z $retval ] || [ $retval -ne 0 ]
  then
    log "lvresize failed: $output " 1
    exit 1
  fi

  #move this back into cloudstack libvirt calls once the libvirt java bindings support block resize
  #we try to inform hypervisor of new size, but don't fail if we can't
  notifyqemu

  log "performed successful resize - dm:$dmname currentsize:$currentsize newsize:$newsize path:$path type:$ptype vmname:$vmname live:$liveresize shrink:$shrink"
}

resizeclvmng() {
  local liveresize='false'

  if ! `lvresize --version > /dev/null 2>&1`
  then
    log "unable to resolve executable 'lvresize'" 1
    exit 1
  fi

  if ! `qemu-img info /dev/null > /dev/null 2>&1`
  then
    log "unable to resolve executable 'qemu-img'" 1
    exit 1
  fi

  if ! `virsh --version > /dev/null 2>&1`
  then
    log "unable to resolve executable 'virsh'" 1
    exit 1
  fi

  vgname=$(echo "$path" | awk -F'/' '{print $3}')
  if [[ -z "$vgname" ]]
  then
    log "unable to derive VG name from path $path" 1
    exit 1
  fi

  # Query PE size in bytes (strip trailing 'B' if present)
  pe_size_raw=$(vgdisplay --units b -C --noheadings -o vg_extent_size "$vgname" 2>/dev/null | tr -d ' ')
  pe_size_raw="${pe_size_raw%B}"
  if [[ -z "$pe_size_raw" || ! "$pe_size_raw" =~ ^[0-9]+$ ]]
  then
    log "could not query PE size for VG $vgname, defaulting to 4MiB"
    pe_size=$((4 * 1024 * 1024))
  else
    pe_size=$pe_size_raw
  fi

  # Calculate new LV size: newsize (virtual) + QCOW2 metadata overhead, rounded up to PE
  # QCOW2 cluster size = 64KiB, L2 table covers 4096 clusters each
  cluster_size=$((64 * 1024))
  l2_multiplier=4096
  num_data_clusters=$(( (newsize + cluster_size - 1) / cluster_size ))
  num_l2_clusters=$(( (num_data_clusters + l2_multiplier - 1) / l2_multiplier ))
  l2_table_size=$(( num_l2_clusters * cluster_size ))
  refcount_table_size=$l2_table_size
  header_overhead=$(( 2 * 1024 * 1024 ))
  metadata_overhead=$(( l2_table_size + refcount_table_size + header_overhead ))
  target_lv_size=$(( newsize + metadata_overhead ))
  # Round up to PE boundary
  lv_size=$(( ((target_lv_size + pe_size - 1) / pe_size) * pe_size ))

  log "CLVM_NG resize: path=$path vg=$vgname pe_size=${pe_size}B virtual=${newsize}B metadata=${metadata_overhead}B lv_size=${lv_size}B"

  # Use -U (force-share) if qemu-img >= 2.10 so we can read info even when
  # QEMU has the file open with an exclusive lock (VM running case)
  qemu_force_share_flag=""
  regex=".*version\s([0-9]+)\.([0-9]+).*"
  content=$(qemu-img --version | grep version)
  if [[ $content =~ $regex ]]
  then
    ver_major="${BASH_REMATCH[1]}"
    ver_minor="${BASH_REMATCH[2]}"
    if [[ ${ver_major} -gt 2 ]] || [[ ${ver_major} -eq 2 && ${ver_minor} -ge 10 ]]
    then
      qemu_force_share_flag="-U"
    fi
  fi

  actualsize=`qemu-img info $qemu_force_share_flag $path | grep "virtual size" | sed -re 's/^.*\(([0-9]+).*$/\1/g'`
  if [[ -z "$actualsize" ]]
  then
    log "unable to determine current QCOW2 virtual size for $path" 1
    exit 1
  fi

  if [ $actualsize -ne $currentsize ]
  then
    log "disk isn't the size we think it is: cloudstack said $currentsize, disk said $actualsize."
  fi

  # Shrink guard on virtual size
  if [ $actualsize -gt $newsize ]
  then
    if [ "$shrink" == "false" ]
    then
      log "result would shrink the volume from $actualsize to $newsize, but shrink wasn't confirmed" 1
      exit 1
    fi
  fi

  # Step 1: resize the LV to accommodate new virtual size + overhead
  output=`lvresize -f -L ${lv_size}B $path 2>&1`
  retval=$?
  if [ -z $retval ] || [ $retval -ne 0 ]
  then
    log "lvresize failed: $output" 1
    exit 1
  fi
  log "lvresize succeeded: $path to ${lv_size}B"

  # Step 2: resize the QCOW2 virtual disk.
  # If the VM is running QEMU has the file open, calling qemu-img resize on an open file
  # is unsafe. Instead use virsh blockresize which tells QEMU to resize the virtual disk
  # safely from within. If the VM is stopped, qemu-img resize is safe to use directly.
  if `virsh domstate $vmname >/dev/null 2>&1`
  then
    log "VM $vmname is running, using virsh blockresize for safe live QCOW2 resize"
    sizeinkb=$(($newsize/1024))
    devicepath=$(virsh domblklist $vmname | grep $path | awk '{print $1}')
    if [[ -z "$devicepath" ]]
    then
      log "could not find device alias for $path in VM $vmname domblklist" 1
      exit 1
    fi
    output=`virsh blockresize --path $devicepath --size $sizeinkb $vmname 2>&1`
    retval=$?
    if [ -z $retval ] || [ $retval -ne 0 ]
    then
      log "virsh blockresize failed: $output" 1
      exit 1
    fi
    liveresize='true'
    log "virsh blockresize succeeded: $vmname $devicepath to ${sizeinkb}KiB virtual"
  else
    log "VM $vmname is not running, using qemu-img resize"
    output=`qemu-img resize $path $newsize 2>&1`
    retval=$?
    if [ -z $retval ] || [ $retval -ne 0 ]
    then
      log "qemu-img resize failed: $output" 1
      exit 1
    fi
    log "qemu-img resize succeeded: $path to ${newsize}B virtual"
  fi

  log "performed successful CLVM_NG resize - currentsize:$currentsize newsize:$newsize lv_size:$lv_size path:$path vmname:$vmname live:$liveresize shrink:$shrink"
}

resizeqcow2() {

   ##### sanity checks #####
  if [ ! -e "$path" ]
  then
    log "unable to find file $path" 1
    exit 1
  fi

  if ! `qemu-img info /dev/null > /dev/null 2>&1`
  then
    log "unable to resolve executable 'qemu-img'" 1
    exit 1
  fi

  if ! `virsh --version > /dev/null 2>&1`
  then
    log "unable to resolve executable 'virsh'" 1
    exit 1
  fi
  ##### end sanity #####

  regex=".*version\s([0-9]+)\.([0-9]+).*"
  content=$(qemu-img --version | grep version)

  qemu_force_share_flag=""
  if [[ $content =~ $regex ]]
    then
       version_first_element="${BASH_REMATCH[1]}"
       version_second_element="${BASH_REMATCH[2]}"
       if [[ ${version_first_element} -gt 2 ]] || [[ ${version_first_element} -eq 2 && ${version_second_element} -ge 10 ]]
       then
          qemu_force_share_flag=" -U "
       fi
    else
      echo "Could not retrieve qemu version. Skipping validation to add --force-share flag."
  fi

  actualsize=`qemu-img info $qemu_force_share_flag $path | grep "virtual size" | sed -re  's/^.*\(([0-9]+).*$/\1/g'`

  if [ $actualsize -ne $currentsize ]
  then
    log "disk isn't the size we think it is: cloudstack said $currentsize, disk said $actualsize."
  fi

  # if this is a shrink operation, fail if commands will shrink the volume and we haven't signed of on shrinking
  failshrink

  #move this back into cloudstack libvirt calls once the libvirt java bindings support block resize
  #we try to inform hypervisor of new size, but don't fail if we can't
  if `virsh help 2>/dev/null | grep -q blockresize`
  then
    if `virsh domstate $vmname >/dev/null 2>&1`
    then
        log "vm $vmname is running, use 'virsh blockresize' to resize the volume"
        notifyqemu
        if [ $? -eq 0 ]
        then
            log "performed successful resize - currentsize:$currentsize newsize:$newsize path:$path type:$ptype vmname:$vmname live:$liveresize shrink:$shrink"
            exit 0
        fi
    fi
  fi

  output=`qemu-img resize $path $newsize 2>&1`
  retval=$?

  if [ -z $retval ] || [ $retval -ne 0 ]
  then
    log "qemu-img resize failed: $output" 1
    exit 1
  fi

  log "performed successful resize - currentsize:$currentsize newsize:$newsize path:$path type:$ptype vmname:$vmname live:$liveresize shrink:$shrink"
}

sflag=
cflag=
pflag=
vflag=
tflag=
rflag=

while getopts 'c:s:v:p:t:r:' OPTION
do
  case $OPTION in
  s)	sflag=1
		newsize="$OPTARG"
		;;
  c)    cflag=1
                currentsize="$OPTARG"
                ;;
  v)	vflag=1
		vmname="$OPTARG"
		;;
  p)	dflag=1
		path="$OPTARG"
		;;
  t)    tflag=1
                ptype="$OPTARG"
                ;;
  r)    rflag=1
                shrink="$OPTARG"
                ;;
  ?)	usage
		exit 2
		;;
  esac
done

shouldwelog=1 #set this to 1 while debugging to get output in /var/log/cloudstack/agent/resizevolume.log

if [ "$ptype" == "CLVM" ]
then
  resizelvm
elif [ "$ptype" == "CLVM_NG" ]
then
  resizeclvmng
elif [ "$ptype" == "QCOW2" ]
then
  resizeqcow2
elif [ "$ptype" == "NOTIFYONLY" ]
then
  notifyqemu
else
  echo "unsupported type $ptype"
  exit 1;
fi

exit 0
