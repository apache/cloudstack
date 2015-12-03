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
      virsh blockresize --domain $vmname --path $path --size $sizeinkb >/dev/null 2>&1
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

  actualsize=`qemu-img info $path | grep "virtual size" | sed -re  's/^.*\(([0-9]+).*$/\1/g'`

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
