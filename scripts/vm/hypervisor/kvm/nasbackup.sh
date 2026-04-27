#!/usr/bin/bash

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

set -eo pipefail

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
QUIESCE=""
# Incremental backup parameters (all optional; legacy callers omit them)
MODE=""               # "full" or "incremental"; empty => legacy full-only behavior (no checkpoint created)
BITMAP_NEW=""         # Bitmap/checkpoint name to create with this backup (e.g. "backup-1711586400")
BITMAP_PARENT=""      # For incremental: parent bitmap name to read changes since
PARENT_PATH=""        # For incremental: parent backup file path (used as backing for qemu-img rebase)
logFile="/var/log/cloudstack/agent/agent.log"

EXIT_CLEANUP_FAILED=20
EXIT_INCREMENTAL_UNSUPPORTED=21

log() {
  [[ "$verb" -eq 1 ]] && builtin echo "$@"
  if [[ "$1" == "-ne"  || "$1" == "-e" || "$1" == "-n" ]]; then
    builtin echo -e "$(date '+%Y-%m-%d %H-%M-%S>')" "${@: 2}" >> "$logFile"
  else
    builtin echo "$(date '+%Y-%m-%d %H-%M-%S>')" "$@" >> "$logFile"
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

get_ceph_uuid_from_path() {
  local fullpath="$1"
  # disk for rbd => rbd:<pool>/<uuid>:mon_host=<monitor_host>...
  # sample: rbd:cloudstack/53d5c355-d726-4d3e-9422-046a503a0b12:mon_host=10.0.1.2...
  local beforeUuid="${fullpath#*/}" # Remove up to first slash after rbd:
  local volUuid="${beforeUuid%%:*}" # Remove everything after colon to get the uuid
  echo ""$volUuid""
}

get_linstor_uuid_from_path() {
  local fullpath="$1"
  # disk for linstor => /dev/drbd/by-res/cs-<uuid>/0
  # sample: /dev/drbd/by-res/cs-53d5c355-d726-4d3e-9422-046a503a0b12/0
  local beforeUuid="${fullpath#/dev/drbd/by-res/}"
  local volUuid="${beforeUuid%%/*}"
  volUuid="${volUuid#cs-}"
  echo "$volUuid"
}

backup_running_vm() {
  mount_operation
  mkdir -p "$dest" || { echo "Failed to create backup directory $dest"; exit 1; }

  # Determine effective mode for this run.
  # Legacy callers (no -M argument) get the original full-only behavior with no checkpoint.
  local effective_mode="${MODE:-legacy-full}"
  local make_checkpoint=0
  case "$effective_mode" in
    incremental)
      if [[ -z "$BITMAP_PARENT" || -z "$BITMAP_NEW" || -z "$PARENT_PATH" ]]; then
        echo "incremental mode requires --bitmap-parent, --bitmap-new, and --parent-path"
        cleanup
        exit 1
      fi
      make_checkpoint=1
      ;;
    full)
      if [[ -z "$BITMAP_NEW" ]]; then
        echo "full mode requires --bitmap-new (the bitmap to create for the next incremental)"
        cleanup
        exit 1
      fi
      make_checkpoint=1
      ;;
    legacy-full)
      make_checkpoint=0
      ;;
    *)
      echo "Unknown mode: $effective_mode"
      cleanup
      exit 1
      ;;
  esac

  # When incremental, verify the parent bitmap still exists on the running domain.
  # CloudStack rebuilds the libvirt domain XML on every VM start, so persistent bitmaps
  # are lost across stop/start. If the parent is missing, recreate it as a fresh bitmap
  # so libvirt accepts the <incremental> reference. The first backup after a recreate
  # captures all writes since the recreate point — slightly larger than ideal, but correct.
  if [[ "$effective_mode" == "incremental" ]]; then
    if ! virsh -c qemu:///system checkpoint-list "$VM" --name 2>/dev/null | grep -qx "$BITMAP_PARENT"; then
      cat > $dest/recreate-checkpoint.xml <<XML
<domaincheckpoint><name>$BITMAP_PARENT</name><disks>
$(virsh -c qemu:///system domblklist "$VM" --details 2>/dev/null | awk '$2=="disk"{printf "<disk name=\"%s\"/>\n", $3}')
</disks></domaincheckpoint>
XML
      if ! virsh -c qemu:///system checkpoint-create "$VM" --xmlfile $dest/recreate-checkpoint.xml > /dev/null 2>&1; then
        echo "Failed to recreate parent bitmap $BITMAP_PARENT for $VM"
        cleanup
        exit 1
      fi
      # Marker for the orchestrator: this incremental is larger because the bitmap was rebuilt.
      echo "BITMAP_RECREATED=$BITMAP_PARENT"
      rm -f $dest/recreate-checkpoint.xml
    fi
  fi

  # Build backup XML (and matching checkpoint XML when applicable).
  name="root"
  echo "<domainbackup mode='push'>" > $dest/backup.xml
  if [[ "$effective_mode" == "incremental" ]]; then
    echo "<incremental>$BITMAP_PARENT</incremental>" >> $dest/backup.xml
  fi
  echo "<disks>" >> $dest/backup.xml
  if [[ $make_checkpoint -eq 1 ]]; then
    echo "<domaincheckpoint><name>$BITMAP_NEW</name><disks>" > $dest/checkpoint.xml
  fi
  while read -r disk fullpath; do
    if [[ "$fullpath" == /dev/drbd/by-res/* ]]; then
        volUuid=$(get_linstor_uuid_from_path "$fullpath")
    else
        volUuid="${fullpath##*/}"
    fi
    if [[ "$effective_mode" == "incremental" ]]; then
      # Incremental disk entry — no backupmode attr, libvirt picks it up from <incremental>.
      echo "<disk name='$disk' backup='yes' type='file'><driver type='qcow2'/><target file='$dest/$name.$volUuid.qcow2' /></disk>" >> $dest/backup.xml
    else
      echo "<disk name='$disk' backup='yes' type='file' backupmode='full'><driver type='qcow2'/><target file='$dest/$name.$volUuid.qcow2' /></disk>" >> $dest/backup.xml
    fi
    if [[ $make_checkpoint -eq 1 ]]; then
      echo "<disk name='$disk'/>" >> $dest/checkpoint.xml
    fi
    name="datadisk"
  done < <(
    virsh -c qemu:///system domblklist "$VM" --details 2>/dev/null | awk '$2=="disk"{print $3, $4}'
  )
  echo "</disks></domainbackup>" >> $dest/backup.xml
  if [[ $make_checkpoint -eq 1 ]]; then
    echo "</disks></domaincheckpoint>" >> $dest/checkpoint.xml
  fi

  local thaw=0
  if [[ ${QUIESCE} == "true" ]]; then
    if virsh -c qemu:///system qemu-agent-command "$VM" '{"execute":"guest-fsfreeze-freeze"}' > /dev/null 2>/dev/null; then
      thaw=1
    fi
  fi

  # Start push backup, atomically registering the new checkpoint when applicable.
  local backup_begin=0
  if [[ $make_checkpoint -eq 1 ]]; then
    if virsh -c qemu:///system backup-begin --domain $VM --backupxml $dest/backup.xml --checkpointxml $dest/checkpoint.xml 2>&1 > /dev/null; then
      backup_begin=1;
    fi
  else
    if virsh -c qemu:///system backup-begin --domain $VM --backupxml $dest/backup.xml 2>&1 > /dev/null; then
      backup_begin=1;
    fi
  fi

  if [[ $thaw -eq 1 ]]; then
    if ! response=$(virsh -c qemu:///system qemu-agent-command "$VM" '{"execute":"guest-fsfreeze-thaw"}' 2>&1 > /dev/null); then
      echo "Failed to thaw the filesystem for vm $VM: $response"
      cleanup
      exit 1
    fi
  fi

  if [[ $backup_begin -ne 1 ]]; then
    cleanup
    exit 1
  fi

  # Backup domain information
  virsh -c qemu:///system dumpxml $VM > $dest/domain-config.xml 2>/dev/null
  virsh -c qemu:///system dominfo $VM > $dest/dominfo.xml 2>/dev/null
  virsh -c qemu:///system domiflist $VM > $dest/domiflist.xml 2>/dev/null
  virsh -c qemu:///system domblklist $VM > $dest/domblklist.xml 2>/dev/null

  while true; do
    status=$(virsh -c qemu:///system domjobinfo $VM --completed --keep-completed | awk '/Job type:/ {print $3}')
    case "$status" in
      Completed)
        break ;;
      Failed)
        echo "Virsh backup job failed"
        cleanup ;;
    esac
    sleep 5
  done

  # Sparsify behavior:
  # - For LINSTOR backups (existing): qemu-img convert sparsifies the bloated output.
  # - For INCREMENTAL: rebase the resulting thin qcow2 onto its parent so the chain is self-describing
  #   (so a future restore can flatten without external chain metadata).
  name="root"
  while read -r disk fullpath; do
    if [[ "$effective_mode" == "incremental" ]]; then
      volUuid="${fullpath##*/}"
      if [[ "$fullpath" == /dev/drbd/by-res/* ]]; then
        volUuid=$(get_linstor_uuid_from_path "$fullpath")
      fi
      # PARENT_PATH from the orchestrator is the parent backup's path relative to the
      # NAS mount root (e.g. "i-2-X/2026.04.27.12.00.00/root.UUID.qcow2"). Convert it to
      # a path relative to THIS new qcow2's directory so the backing reference resolves
      # correctly the next time the NAS is mounted (mount points are ephemeral).
      local parent_abs="$mount_point/$PARENT_PATH"
      if [[ ! -f "$parent_abs" ]]; then
        echo "Parent backup file does not exist on NAS: $parent_abs"
        cleanup
        exit 1
      fi
      local parent_rel
      parent_rel=$(realpath --relative-to="$dest" "$parent_abs")
      if ! qemu-img rebase -u -b "$parent_rel" -F qcow2 "$dest/$name.$volUuid.qcow2" >> "$logFile" 2> >(cat >&2); then
        echo "qemu-img rebase failed for $dest/$name.$volUuid.qcow2 onto $parent_rel"
        cleanup
        exit 1
      fi
      name="datadisk"
      continue
    fi
    if [[ "$fullpath" != /dev/drbd/by-res/* ]]; then
      continue
    fi
    volUuid=$(get_linstor_uuid_from_path "$fullpath")
    if ! qemu-img convert -O qcow2 "$dest/$name.$volUuid.qcow2" "$dest/$name.$volUuid.qcow2.tmp" >> "$logFile" 2> >(cat >&2); then
      echo "qemu-img convert failed for $dest/$name.$volUuid.qcow2"
      cleanup
      exit 1
    fi

    mv "$dest/$name.$volUuid.qcow2.tmp" "$dest/$name.$volUuid.qcow2"
    name="datadisk"
  done < <(
    virsh -c qemu:///system domblklist "$VM" --details 2>/dev/null | awk '$2=="disk"{print $3, $4}'
  )

  rm -f $dest/backup.xml $dest/checkpoint.xml
  sync

  # Print statistics
  virsh -c qemu:///system domjobinfo $VM --completed
  du -sb $dest | cut -f1
  if [[ -n "$BITMAP_NEW" ]]; then
    # Echo the bitmap name on its own line so the Java caller can capture it for backup_details.
    echo "BITMAP_CREATED=$BITMAP_NEW"
  fi

  umount $mount_point
  rmdir $mount_point
}

backup_stopped_vm() {
  # Stopped VMs cannot use libvirt's backup-begin (no QEMU process). Take a full
  # backup via qemu-img convert. If the caller asked for incremental, fall back
  # to full and signal the fallback so the orchestrator can record it as a full
  # in the chain.
  if [[ "$MODE" == "incremental" ]]; then
    echo "INCREMENTAL_FALLBACK=full (VM stopped — incremental requires running VM)" >&2
  fi

  mount_operation
  mkdir -p "$dest" || { echo "Failed to create backup directory $dest"; exit 1; }

  IFS=","

  name="root"
  for disk in $DISK_PATHS; do
    if [[ "$disk" == rbd:* ]]; then
      volUuid=$(get_ceph_uuid_from_path "$disk")
    elif [[ "$disk" == /dev/drbd/by-res/* ]]; then
      volUuid=$(get_linstor_uuid_from_path "$disk")
    else
      volUuid="${disk##*/}"
    fi
    output="$dest/$name.$volUuid.qcow2"
    if ! qemu-img convert -O qcow2 "$disk" "$output" > "$logFile" 2> >(cat >&2); then
      echo "qemu-img convert failed for $disk $output"
      cleanup
    fi
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

get_backup_stats() {
  mount_operation

  echo $mount_point
  df -P $mount_point 2>/dev/null | awk 'NR==2 {print $2, $3}'
  umount $mount_point
  rmdir $mount_point
}

mount_operation() {
  mount_point=$(mktemp -d -t csbackup.XXXXX)
  dest="$mount_point/${BACKUP_DIR}"
  if [ ${NAS_TYPE} == "cifs" ]; then
    MOUNT_OPTS="${MOUNT_OPTS},nobrl"
  fi
  mount -t ${NAS_TYPE} ${NAS_ADDRESS} ${mount_point} $([[ ! -z "${MOUNT_OPTS}" ]] && echo -o ${MOUNT_OPTS}) 2>&1 | tee -a "$logFile"
  if [ $? -eq 0 ]; then
      log -ne "Successfully mounted ${NAS_TYPE} store"
  else
      echo "Failed to mount ${NAS_TYPE} store"
      exit 1
  fi
}

cleanup() {
  local status=0

  rm -rf "$dest" || { echo "Failed to delete $dest"; status=1; }
  umount "$mount_point" || { echo "Failed to unmount $mount_point"; status=1; }
  rmdir "$mount_point" || { echo "Failed to remove mount point $mount_point"; status=1; }

  if [[ $status -ne 0 ]]; then
    echo "Backup cleanup failed"
    exit $EXIT_CLEANUP_FAILED
  fi
}

function usage {
  echo ""
  echo "Usage: $0 -o <operation> -v|--vm <domain name> -t <storage type> -s <storage address> -m <mount options> -p <backup path> -d <disks path> -q|--quiesce <true|false>"
  echo "         [-M|--mode <full|incremental>] [--bitmap-new <name>] [--bitmap-parent <name>] [--parent-path <path>]"
  echo ""
  echo "Incremental backup options (running VMs only; requires QEMU >= 4.2 and libvirt >= 7.2):"
  echo "  -M|--mode full          Take a full backup AND create a checkpoint (--bitmap-new required) for future incrementals."
  echo "  -M|--mode incremental   Take an incremental backup since --bitmap-parent and create new checkpoint --bitmap-new."
  echo "                          Requires --bitmap-parent, --bitmap-new, and --parent-path (parent backup file for rebase)."
  echo "  Without -M, behaves as legacy full-only backup with no checkpoint creation."
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
    -q|--quiesce)
      QUIESCE="$2"
      shift
      shift
      ;;
    -d|--diskpaths)
      DISK_PATHS="$2"
      shift
      shift
      ;;
    -M|--mode)
      MODE="$2"
      shift
      shift
      ;;
    --bitmap-new)
      BITMAP_NEW="$2"
      shift
      shift
      ;;
    --bitmap-parent)
      BITMAP_PARENT="$2"
      shift
      shift
      ;;
    --parent-path)
      PARENT_PATH="$2"
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

# Perform Initial sanity checks
sanity_checks

if [ "$OP" = "backup" ]; then
  STATE=$(virsh -c qemu:///system list | awk -v vm="$VM" '$2 == vm {print $3}')
  if [ -n "$STATE" ] && [ "$STATE" = "running" ]; then
    backup_running_vm
  else
    backup_stopped_vm
  fi
elif [ "$OP" = "delete" ]; then
  delete_backup
elif [ "$OP" = "stats" ]; then
  get_backup_stats
fi
