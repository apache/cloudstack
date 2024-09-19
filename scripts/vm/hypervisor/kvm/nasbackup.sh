#!/usr/bin/bash
## Licensed to the Apache Software Foundation (ASF) under one
## or more contributor license agreements.  See the NOTICE file
## distributed with this work for additional information
## regarding copyright ownership.  The ASF licenses this file
## to you under the Apache License, Version 2.0 (the
## "License"); you may not use this file except in compliance
## with the License.  You may obtain a copy of the License at
##
##   http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing,
## software distributed under the License is distributed on an
## "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
## KIND, either express or implied.  See the License for the
## specific language governing permissions and limitations
## under the License.

set -e

# CloudStack B&R NAS Backup and Recovery Tool for KVM

# TODO: do libvirt/logging etc checks

### Declare variables ###

OP=""
VM=""
NAS_TYPE=""
NAS_ADDRESS=""
MOUNT_OPTS=""
BACKUP_DIR=""
DISK_PATHS=""

### Operation methods ###

backup_running_vm() {
  mount_operation
  mkdir -p $dest

  name="root"
  echo "<domainbackup mode='push'><disks>" > $dest/backup.xml
  for disk in $(virsh -c qemu:///system domblklist $VM --details 2>/dev/null | awk '/disk/{print$3}'); do
    volpath=$(virsh -c qemu:///system domblklist $VM --details | awk "/$disk/{print $4}" | sed 's/.*\///')
    echo "<disk name='$disk' backup='yes' type='file' backupmode='full'><driver type='qcow2'/><target file='$dest/$name.$volpath.qcow2' /></disk>" >> $dest/backup.xml
    name="datadisk"
  done
  echo "</disks></domainbackup>" >> $dest/backup.xml

  # Start push backup
  virsh -c qemu:///system backup-begin --domain $VM --backupxml $dest/backup.xml > /dev/null 2>/dev/null

  # Backup domain information
  virsh -c qemu:///system dumpxml $VM > $dest/domain-config.xml 2>/dev/null
  virsh -c qemu:///system dominfo $VM > $dest/dominfo.xml 2>/dev/null
  virsh -c qemu:///system domiflist $VM > $dest/domiflist.xml 2>/dev/null
  virsh -c qemu:///system domblklist $VM > $dest/domblklist.xml 2>/dev/null

  until virsh -c qemu:///system domjobinfo $VM --completed --keep-completed 2>/dev/null | grep "Completed" > /dev/null; do
    sleep 5
  done
  rm -f $dest/backup.xml
  sync

  # Print statistics
  virsh -c qemu:///system domjobinfo $VM --completed
  du -sb $dest | cut -f1

  umount $mount_point
  rmdir $mount_point
}

backup_stopped_vm() {
  mount_operation
  mkdir -p $dest

  IFS=","

  name="root"
  for disk in $DISK_PATHS; do
    volUuid="${disk##*/}"
    qemu-img convert -O qcow2 $disk $dest/$name.$volUuid.qcow2
    name="datadisk"
  done
  sync

  ls -l --numeric-uid-gid $dest | awk '{print $5}'
}

delete_backup() {
  mount_operation

  rm -frv $dest
  sync
  umount $mount_point
  rmdir $mount_point
}

mount_operation() {
  mount_point=$(mktemp -d -t csbackup.XXXXX)
  dest="$mount_point/${BACKUP_DIR}"
  mount -t ${NAS_TYPE} ${NAS_ADDRESS} ${mount_point} $([[ ! -z "${MOUNT_OPTS}" ]] && echo -o ${MOUNT_OPTS})
}

function usage {
  echo ""
  echo "Usage: $0 -o <operation> -v|--vm <domain name> -t <storage type> -s <storage address> -m <mount options> -p <backup path> -d <disks path>"
  echo ""
  exit 1
}

while [[ $# -gt 0 ]]; do
  case $1 in
    -o|--operation)
      OP="$2"
      shift
      shift
      ;;
    -v|--vm)
      VM="$2"
      shift
      shift
      ;;
    -t|--type)
      NAS_TYPE="$2"
      shift
      shift
      ;;
    -s|--storage)
      NAS_ADDRESS="$2"
      shift
      shift
      ;;
    -m|--mount)
      MOUNT_OPTS="$2"
      shift
      shift
      ;;
    -p|--path)
      BACKUP_DIR="$2"
      shift
      shift
      ;;
    -d|--diskpaths)
      DISK_PATHS="$2"
      shift
      shift
      ;;
    -h|--help)
      usage
      shift
      ;;
    *)
      echo "Invalid option: $1"
      usage
      ;;
  esac
done

if [ "$OP" = "backup" ]; then
  STATE=$(virsh -c qemu:///system list | grep $VM | awk '{print $3}')
  if [ "$STATE" = "running" ]; then
    backup_running_vm
  else
    backup_stopped_vm
  fi
elif [ "$OP" = "delete" ]; then
  delete_backup
fi
