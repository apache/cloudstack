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

set -eo pipefail

# CloudStack B&R NAS Backup and Recovery Tool for KVM

### Declare variables ###

OP=""
VM=""
NAS_TYPE=""
NAS_ADDRESS=""
MOUNT_OPTS=""
BACKUP_DIR=""
DISK_PATHS=""
QUIESCE=""
logFile="/var/log/cloudstack/agent/agent.log"

# Exit codes
EXIT_CLEANUP_FAILED=20

# Backup job timeout in seconds (default: 6 hours)
BACKUP_TIMEOUT=${BACKUP_TIMEOUT:-21600}
# Minimum free space required on backup target in bytes (default: 1 GB)
MIN_FREE_SPACE=${MIN_FREE_SPACE:-1073741824}

log() {
  [[ "$verb" -eq 1 ]] && builtin echo "$@"
  if [[ "$1" == "-ne"  || "$1" == "-e" || "$1" == "-n" ]]; then
    builtin echo -e "$(date '+%Y-%m-%d %H-%M-%S>')" "${@: 2}" >> "$logFile"
  else
    builtin echo "$(date '+%Y-%m-%d %H-%M-%S>')" "$@" >> "$logFile"
  fi
}

cleanup() {
  local status=0

  # Resume the VM if it was paused during backup to prevent it from
  # remaining indefinitely paused when the backup job fails (e.g. due
  # to storage full or I/O errors on the backup target)
  if [[ -n "$VM" ]]; then
    local vm_state
    vm_state=$(virsh -c qemu:///system domstate "$VM" 2>/dev/null || true)
    if [[ "$vm_state" == "paused" ]]; then
      log -ne "Resuming paused VM $VM during backup cleanup"
      if ! virsh -c qemu:///system resume "$VM" > /dev/null 2>&1; then
        echo "Failed to resume VM $VM"
        status=1
      fi
    fi
  fi

  if [[ -n "$dest" && -d "$dest" ]]; then
    rm -rf "$dest" || { echo "Failed to delete $dest"; status=1; }
  fi
  if [[ -n "$mount_point" && -d "$mount_point" ]]; then
    umount "$mount_point" 2>/dev/null || { echo "Failed to unmount $mount_point"; status=1; }
    rmdir "$mount_point" 2>/dev/null || true
  fi

  if [[ $status -ne 0 ]]; then
    echo "Backup cleanup failed"
    exit $EXIT_CLEANUP_FAILED
  fi
}

# Trap ensures cleanup always runs on exit (error, signal, or normal exit)
# This prevents orphan NFS mounts from accumulating after failed backups
trap cleanup EXIT

check_free_space() {
  local free_bytes
  free_bytes=$(df -P "$mount_point" 2>/dev/null | awk 'NR==2 {print $4}')
  if [[ -n "$free_bytes" ]]; then
    # df reports in 1K blocks, convert to bytes
    free_bytes=$((free_bytes * 1024))
    if [[ $free_bytes -lt $MIN_FREE_SPACE ]]; then
      echo "Insufficient free space on backup target: $((free_bytes / 1048576)) MB available, $((MIN_FREE_SPACE / 1048576)) MB required"
      exit 1
    fi
    log -ne "Backup target has $((free_bytes / 1073741824)) GB free space"
  fi
}

vercomp() {
  local IFS=.
  local i ver1=($1) ver2=($3)

  # Compare each segment of the version numbers
  for ((i=0; i<${#ver1[@]}; i++)); do
      if [[ -z ${ver2[i]} ]]; then
          ver2[i]=0
      fi

      if ((10#${ver1[i]} > 10#${ver2[i]})); then
          return  0 # Version 1 is greater
      elif ((10#${ver1[i]} < 10#${ver2[i]})); then
          return 2  # Version 2 is greater
      fi
  done
  return 0  # Versions are equal
}

sanity_checks() {
  hvVersion=$(virsh version | grep hypervisor | awk '{print $(NF)}')
  libvVersion=$(virsh version | grep libvirt | awk '{print $(NF)}' | tail -n 1)
  apiVersion=$(virsh version | grep API | awk '{print $(NF)}')

  # Compare qemu version (hvVersion >= 4.2.0)
  vercomp "$hvVersion" ">=" "4.2.0"
  hvStatus=$?

  # Compare libvirt version (libvVersion >= 7.2.0)
  vercomp "$libvVersion" ">=" "7.2.0"
  libvStatus=$?

  if [[ $hvStatus -eq 0 && $libvStatus -eq 0 ]]; then
    log -ne "Success... [ QEMU: $hvVersion Libvirt: $libvVersion apiVersion: $apiVersion ]"
  else
    echo "Failure... Your QEMU version $hvVersion or libvirt version $libvVersion is unsupported. Consider upgrading to the required minimum version of QEMU: 4.2.0 and Libvirt: 7.2.0"
    exit 1
  fi

  log -ne "Environment Sanity Checks successfully passed"
}

### Operation methods ###

backup_running_vm() {
  mount_operation
  check_free_space
  mkdir -p $dest

  name="root"
  echo "<domainbackup mode='push'><disks>" > $dest/backup.xml
  for disk in $(virsh -c qemu:///system domblklist $VM --details 2>/dev/null | awk '/disk/{print$3}'); do
    volpath=$(virsh -c qemu:///system domblklist $VM --details | awk "/$disk/{print $4}" | sed 's/.*\///')
    echo "<disk name='$disk' backup='yes' type='file' backupmode='full'><driver type='qcow2'/><target file='$dest/$name.$volpath.qcow2' /></disk>" >> $dest/backup.xml
    name="datadisk"
  done
  echo "</disks></domainbackup>" >> $dest/backup.xml

  # Quiesce guest filesystem before backup if requested and agent is available
  if [[ "$QUIESCE" == "true" ]]; then
    if virsh -c qemu:///system qemu-agent-command $VM '{"execute":"guest-ping"}' > /dev/null 2>&1; then
      log -ne "Quiescing guest filesystem on $VM"
      virsh -c qemu:///system domfsfreeze $VM > /dev/null 2>&1 || log -ne "Warning: fsfreeze failed on $VM, proceeding without quiesce"
    else
      log -ne "Warning: qemu-guest-agent not available on $VM, skipping quiesce"
    fi
  fi

  # Start push backup
  if ! virsh -c qemu:///system backup-begin --domain $VM --backupxml $dest/backup.xml > /dev/null 2>&1; then
    echo "Failed to start backup for VM $VM"
    # Thaw filesystem if we froze it
    [[ "$QUIESCE" == "true" ]] && virsh -c qemu:///system domfsthaw $VM > /dev/null 2>&1 || true
    exit 1
  fi

  # Thaw filesystem immediately after backup-begin (QEMU has its own consistent snapshot)
  if [[ "$QUIESCE" == "true" ]]; then
    virsh -c qemu:///system domfsthaw $VM > /dev/null 2>&1 || true
    log -ne "Thawed guest filesystem on $VM"
  fi

  # Backup domain information
  virsh -c qemu:///system dumpxml $VM > $dest/domain-config.xml 2>/dev/null
  virsh -c qemu:///system dominfo $VM > $dest/dominfo.xml 2>/dev/null
  virsh -c qemu:///system domiflist $VM > $dest/domiflist.xml 2>/dev/null
  virsh -c qemu:///system domblklist $VM > $dest/domblklist.xml 2>/dev/null

  # Wait for backup to complete with timeout
  local elapsed=0
  until virsh -c qemu:///system domjobinfo $VM --completed --keep-completed 2>/dev/null | grep "Completed" > /dev/null; do
    if [[ $elapsed -ge $BACKUP_TIMEOUT ]]; then
      echo "Backup timed out after ${BACKUP_TIMEOUT}s for VM $VM"
      virsh -c qemu:///system domjobabort $VM > /dev/null 2>&1 || true
      exit 1
    fi
    sleep 5
    elapsed=$((elapsed + 5))
  done
  rm -f $dest/backup.xml
  sync

  # Print statistics
  virsh -c qemu:///system domjobinfo $VM --completed
  du -sb $dest | cut -f1
}

backup_stopped_vm() {
  mount_operation
  check_free_space
  mkdir -p $dest

  IFS=","

  name="root"
  for disk in $DISK_PATHS; do
    volUuid="${disk##*/}"
    if ! qemu-img convert -O qcow2 "$disk" "$dest/$name.$volUuid.qcow2" 2>&1 | tee -a "$logFile"; then
      echo "Failed to convert disk $disk"
      exit 1
    fi
    name="datadisk"
  done
  sync

  ls -l --numeric-uid-gid $dest | awk '{print $5}'
}

delete_backup() {
  mount_operation

  rm -frv "$dest"
  sync
  # cleanup trap handles umount and rmdir
}

mount_operation() {
  mount_point=$(mktemp -d -t csbackup.XXXXX)
  dest="$mount_point/${BACKUP_DIR}"
  if [[ "${NAS_TYPE}" == "cifs" ]]; then
    MOUNT_OPTS="${MOUNT_OPTS},nobrl"
  fi
  if mount -t "${NAS_TYPE}" "${NAS_ADDRESS}" "${mount_point}" $([[ -n "${MOUNT_OPTS}" ]] && echo "-o" "${MOUNT_OPTS}") 2>&1 | tee -a "$logFile"; then
      log -ne "Successfully mounted ${NAS_TYPE} store"
  else
      echo "Failed to mount ${NAS_TYPE} store"
      exit 1
  fi
}

function usage {
  echo ""
  echo "Usage: $0 -o <operation> -v|--vm <domain name> -t <storage type> -s <storage address> -m <mount options> -p <backup path> -d <disks path> [-q]"
  echo ""
  echo "Options:"
  echo "  -o, --operation   Operation to perform: backup, delete"
  echo "  -v, --vm          VM domain name"
  echo "  -t, --type        NAS type: nfs, cifs"
  echo "  -s, --storage     NAS address (e.g. 192.168.1.1:/share)"
  echo "  -m, --mount       Mount options"
  echo "  -p, --path        Backup directory path on NAS"
  echo "  -d, --diskpaths   Comma-separated disk paths (for stopped VM backup)"
  echo "  -q, --quiesce     Quiesce guest filesystem before backup (requires qemu-guest-agent)"
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
    -q|--quiesce)
      QUIESCE="true"
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

# Perform Initial sanity checks
sanity_checks

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
