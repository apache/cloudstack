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

### Operation methods ###

backup_vm() {
  mount_point=$(mktemp -d -t csbackup.XXXXX)
  dest="$mount_point/${BACKUP_DIR}"

  mount -t ${NAS_TYPE} ${NAS_ADDRESS} ${mount_point} $([[ ! -z "${MOUNT_OPTS}" ]] && echo -o ${MOUNT_OPTS})
  mkdir -p $dest

  echo "<domainbackup mode='push'><disks>" > $dest/backup.xml
  for disk in $(virsh -c qemu:///system domblklist $vm --details 2>/dev/null | awk '/disk/{print$3}'); do
    echo "<disk name='$disk' backup='yes' type='file' backupmode='full'><driver type='qcow2'/><target file='$dest/$disk' /></disk>" >> $dest/backup.xml
  done
  echo "</disks></domainbackup>" >> $dest/backup.xml

  virsh -c qemu:///system backup-begin --domain $vm --backupxml $dest/backup.xml > /dev/null 2>/dev/null
  virsh -c qemu:///system dumpxml $vm > $dest/domain-$vm.xml 2>/dev/null

  until virsh -c qemu:///system domjobinfo $vm --completed 2>/dev/null | grep "Completed" > /dev/null; do
    sleep 5
  done
  rm -f $dest/backup.xml

  # Print directory size
  sync
  du -sb $dest | cut -f1
  umount $mount_point
  rmdir $mount_point
}

delete_backup() {
  mount_point=$(mktemp -d -t csbackup.XXXXX)
  dest="$mount_point/${BACKUP_DIR}"

  mount -t ${NAS_TYPE} ${NAS_ADDRESS} ${mount_point} $([[ ! -z "${MOUNT_OPTS}" ]] && echo -o ${MOUNT_OPTS})

  rm -frv $dest
  sync

  umount $mount_point
  rmdir $mount_point
}

function usage {
  echo ""
  echo "Usage: $0 -b <domain> -s <NAS storage mount path> -p <backup dest path>"
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
  backup_vm
elif [ "$OP" = "delete" ]; then
  delete_backup
fi
