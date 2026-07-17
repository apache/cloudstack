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
PARENT_PATHS=""       # For incremental: comma-separated list of parent backup file paths,
                      # one per VM volume in the same order as DISK_PATHS. Each new qcow2
                      # is rebased onto its corresponding parent file. Required because
                      # data-disk backup files don't share the root volume's UUID, so
                      # each disk must be rebased onto its own parent.
logFile="/var/log/cloudstack/agent/agent.log"
UNMOUNT_TIMEOUT=60
EXIT_CLEANUP_FAILED=20

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
  # The Java wrapper (LibvirtTakeBackupCommandWrapper) pre-validates required args before
  # invoking the script; the case below is a defensive fallback for direct invocations.
  local effective_mode="${MODE:-legacy-full}"
  local make_checkpoint=0
  case "$effective_mode" in
    incremental|full)
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

  # Incremental needs the parent checkpoint registered with libvirt. CloudStack rebuilds the
  # domain XML on every VM start, wiping libvirt's checkpoint registry while the dirty bitmap
  # persists on the qcow2, so a fresh checkpoint-create fails with "Bitmap already exists".
  # Re-register the parent with --redefine (needs only a name + creationTime) via a minimal
  # synthesized XML. If the parent bitmap is missing from the qcow2 (e.g. after a migration),
  # fall back to a full backup instead of letting backup-begin fail below.
  if [[ "$effective_mode" == "incremental" ]]; then
    # The parent bitmap must be present on EVERY disk, not just one. A snapshot restore or partial
    # migration can wipe it on some disks; require it on all by comparing the disk count to the
    # number of disks that carry it.
    disk_count=$(virsh -c qemu:///system domblklist "$VM" --details 2>/dev/null | awk '$2=="disk"{c++} END{print c+0}')
    # Count per-device (one per inserted.file whose dirty-bitmaps holds the parent), mirroring
    # getVmDiskPathHasFromCheckpointMap(): query-block lists a bitmap under multiple nodes, so raw
    # name matches double-count. "|| echo 0" keeps a no-match from aborting under "set -eo pipefail"
    # before the fallback runs.
    bitmap_count=$(virsh -c qemu:///system qemu-monitor-command "$VM" '{"execute":"query-block"}' 2>/dev/null | python3 -c '
import sys, json
target = sys.argv[1]
try:
    data = json.load(sys.stdin)
except Exception:
    print(0); sys.exit(0)
files = set()
for dev in data.get("return", []) or []:
    inserted = dev.get("inserted") or {}
    f = inserted.get("file")
    if not f:
        continue
    if any((b or {}).get("name") == target for b in (inserted.get("dirty-bitmaps") or [])):
        files.add(f)
print(len(files))
' "$BITMAP_PARENT" 2>/dev/null || echo 0)
    if [[ "$disk_count" -eq 0 || "$bitmap_count" -lt "$disk_count" ]]; then
      log -e "incremental: parent bitmap $BITMAP_PARENT present on $bitmap_count/$disk_count disk(s) — falling back to full"
      echo "INCREMENTAL_FALLBACK=true"
      effective_mode="full"
    fi
  fi

  if [[ "$effective_mode" == "incremental" ]]; then
    if ! virsh -c qemu:///system checkpoint-list "$VM" --name 2>/dev/null | grep -qx "$BITMAP_PARENT"; then
      redefine_xml=$(mktemp)
      printf '<domaincheckpoint><name>%s</name><creationTime>%s</creationTime></domaincheckpoint>' \
        "$BITMAP_PARENT" "$(date +%s)" > "$redefine_xml"
      if virsh -c qemu:///system checkpoint-create "$VM" --xmlfile "$redefine_xml" --redefine > /dev/null 2>&1; then
        rm -f "$redefine_xml" # parent checkpoint re-registered; the incremental can proceed against it
      else
        rm -f "$redefine_xml"
        # Parent checkpoint could not be re-registered — fall back to a full backup in place so
        # the chain restarts cleanly instead of failing. Emit a stdout marker so the wrapper
        # records this backup as a full (incrementalFallback=true).
        log -e "incremental: parent checkpoint $BITMAP_PARENT could not be re-registered — falling back to full"
        echo "INCREMENTAL_FALLBACK=true"
        effective_mode="full"
      fi
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
    # Order matters: redirect stdout to /dev/null first, then merge stderr into stdout.
    # The reversed `2>&1 > /dev/null` form leaves stderr pointing at the original tty.
    if virsh -c qemu:///system backup-begin --domain $VM --backupxml $dest/backup.xml --checkpointxml $dest/checkpoint.xml > /dev/null 2>&1; then
      backup_begin=1;
    fi
  else
    if virsh -c qemu:///system backup-begin --domain $VM --backupxml $dest/backup.xml > /dev/null 2>&1; then
      backup_begin=1;
    fi
  fi

  if [[ $thaw -eq 1 ]]; then
    if ! response=$(virsh -c qemu:///system qemu-agent-command "$VM" '{"execute":"guest-fsfreeze-thaw"}' 2>&1); then
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
        cleanup
        exit 1 ;;
    esac
    sleep 5
  done

  # Sparsify behavior:
  # - For LINSTOR backups (existing): qemu-img convert sparsifies the bloated output.
  # - For INCREMENTAL: rebase the resulting thin qcow2 onto its parent so the chain is self-describing
  #   (so a future restore can flatten without external chain metadata).
  name="root"
  # PARENT_PATHS arrives as a comma-separated list, one entry per VM volume in the same
  # order as DISK_PATHS. Split into a bash array so we can index by disk position.
  local -a parent_paths_arr=()
  if [[ "$effective_mode" == "incremental" && -n "$PARENT_PATHS" ]]; then
    IFS=',' read -ra parent_paths_arr <<< "$PARENT_PATHS"
  fi
  local disk_idx=0
  while read -r disk fullpath; do
    if [[ "$effective_mode" == "incremental" ]]; then
      volUuid="${fullpath##*/}"
      # Pick this disk's specific parent file. Each volume's backup is named after its
      # own UUID, so a single PARENT_PATH would wrongly rebase data disks onto the root
      # parent.
      if [[ $disk_idx -ge ${#parent_paths_arr[@]} ]]; then
        echo "PARENT_PATHS list shorter than DISK_PATHS — missing parent for disk index $disk_idx"
        cleanup
        exit 1
      fi
      local this_parent_rel="${parent_paths_arr[$disk_idx]}"
      local parent_abs="$mount_point/$this_parent_rel"
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
      disk_idx=$((disk_idx + 1))
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

  # Free the parent bitmap now that the incremental is written and rebased: its delta is captured
  # here and BITMAP_NEW tracks changes going forward, so it only accrues metadata/IO cost over a
  # long chain. Remove it per-disk with block-dirty-bitmap-remove (a clean free) rather than
  # checkpoint-delete, which would merge its bits into BITMAP_NEW and re-copy backed-up regions.
  # Best-effort: a failure here does not fail the backup, the bitmap is reclaimed on a later run.
  if [[ "$effective_mode" == "incremental" && -n "$BITMAP_PARENT" ]]; then
    while read -r node; do
      [[ -z "$node" ]] && continue
      if ! virsh -c qemu:///system qemu-monitor-command "$VM" \
           "{\"execute\":\"block-dirty-bitmap-remove\",\"arguments\":{\"node\":\"$node\",\"name\":\"$BITMAP_PARENT\"}}" \
           > /dev/null 2>>"$logFile"; then
        log -e "cleanup: failed to remove parent bitmap $BITMAP_PARENT on node $node (non-fatal)"
      fi
    done < <(
      virsh -c qemu:///system qemu-monitor-command "$VM" '{"execute":"query-block"}' 2>/dev/null | python3 -c '
import sys, json
target = sys.argv[1]
try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)
seen = set()
for dev in data.get("return", []) or []:
    inserted = dev.get("inserted") or {}
    node = inserted.get("node-name")
    if not node or node in seen:
        continue
    if any((b or {}).get("name") == target for b in (inserted.get("dirty-bitmaps") or [])):
        seen.add(node)
        print(node)
' "$BITMAP_PARENT" 2>/dev/null || true
    )
  fi

  # Print statistics
  virsh -c qemu:///system domjobinfo $VM --completed
  backup_size=$(du -sb "$dest" 2>>"$logFile" | cut -f1) || { log -ne "WARNING: du failed for $dest, reporting size as 0"; backup_size=0; }
  timeout "$UNMOUNT_TIMEOUT" umount "$mount_point" 2>>"$logFile" || { log "WARNING: umount of $mount_point failed or timed out"; true; }
  rmdir "$mount_point" 2>>"$logFile" || { log "WARNING: rmdir of $mount_point failed"; true; }
  echo "$backup_size"
}

backup_stopped_vm() {
  # Stopped VMs cannot use libvirt's backup-begin (no QEMU process); take a full backup via
  # qemu-img convert. The orchestrator never sends incremental mode for a stopped VM.
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
      exit 1
    fi

    # Pre-seed a persistent bitmap on the source disk so the NEXT backup (taken
    # after this VM is started again) can be incremental against the qcow2 we
    # just wrote. Without this, every backup after a stopped-VM backup would
    # fall back to full because no parent bitmap exists on the host yet.
    # Only applies to file-backed qcow2 sources — RBD/LINSTOR have their own
    # snapshot mechanisms and qemu-img bitmap is not the right primitive there.
    # bitmap --add should not fail on a file-backed qcow2; if it does, fail the backup so the
    # underlying problem is surfaced rather than silently degrading future backups to full.
    if [[ -n "$BITMAP_NEW" && "$disk" != rbd:* && "$disk" != /dev/drbd/by-res/* ]]; then
      if ! qemu-img bitmap --add "$disk" "$BITMAP_NEW" 2>>"$logFile"; then
        echo "Failed to pre-seed bitmap $BITMAP_NEW on $disk"
        cleanup
        exit 1
      fi
    fi

    name="datadisk"
  done
  sync

  find "$dest" -type f -exec stat -c '%s' {} +
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

  # Resume the VM if it was paused during backup to prevent it from
  # remaining indefinitely paused when the backup job fails (e.g. due
  # to storage full or I/O errors on the backup target)
  local vm_state
  vm_state=$(virsh -c qemu:///system domstate "$VM" 2>/dev/null || true)
  if [[ "$vm_state" == "paused" ]]; then
    log -ne "Resuming paused VM $VM during backup cleanup"
    if ! virsh -c qemu:///system resume "$VM" > /dev/null 2>&1; then
      echo "Failed to resume VM $VM"
      status=1
    fi
  fi

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
  echo "         [-M|--mode <full|incremental>] [--bitmap-new <name>] [--bitmap-parent <name>] [--parent-paths <p1,p2,...>]"
  echo ""
  echo "Incremental backup options (running VMs only; requires QEMU >= 4.2 and libvirt >= 7.2):"
  echo "  -M|--mode full          Take a full backup AND create a checkpoint (--bitmap-new required) for future incrementals."
  echo "  -M|--mode incremental   Take an incremental backup since --bitmap-parent and create new checkpoint --bitmap-new."
  echo "                          Requires --bitmap-parent, --bitmap-new, and --parent-paths (comma-separated list, one"
  echo "                          parent qcow2 path per disk: root.<uuid>.qcow2, datadisk.<uuid>.qcow2, … same order"
  echo "                          as -d|--disks)."
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
    --parent-paths)
      PARENT_PATHS="$2"
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

# Perform initial environment sanity checks (QEMU/libvirt version).
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
