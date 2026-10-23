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
METHOD="qcow2"        # How the backup is taken: "qcow2" (push mode, incrementals from dirty bitmaps) or
                      # "rbd" (Ceph-native, incrementals from rbd export-diff --from-snap)
BITMAP_NEW=""         # Anchor to create with this backup: a QEMU bitmap/checkpoint name for qcow2
                      # chains (e.g. "backup-1711586400"), an RBD snapshot name for the rbd method
BITMAP_PARENT=""      # Anchor to read changes since (incremental modes only)
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

get_linstor_uuid_from_device() {
  local fullpath="$1"
  # VMs started before the /dev/drbd/by-res/ change still reference the raw DRBD
  # device node (e.g. /dev/drbd1098) in their live libvirt XML. Ask udev for the
  # device's symlinks and map it back to the volume UUID via the by-res symlink.
  local link
  for link in $(udevadm info --query=symlink --name="$fullpath" 2>/dev/null || true); do
    if [[ "$link" == drbd/by-res/cs-* ]]; then
      get_linstor_uuid_from_path "/dev/$link"
      return 0
    fi
  done
  # Without a by-res symlink we cannot derive the volume UUID. Falling back to the
  # raw device name would produce a backup that restore cannot find, so fail hard.
  return 1
}

# Globals populated by parse_rbd_opts() for the disk it was last called with.
RBD_IMAGE=""          # "<pool>/<image>"
RBD_ARGS=()           # connection arguments to pass to every rbd invocation
RBD_KEYFILE=""        # temporary cephx keyfile, removed by rbd_cleanup_keyfile
# Prefix NASBackupProvider names every chain anchor with. Snapshots carrying it belong to this
# tool and no other, which is what makes the stale-anchor sweep safe.
RBD_SNAPSHOT_PREFIX="csbackup-"

rbd_cleanup_keyfile() {
  if [[ -n "$RBD_KEYFILE" ]]; then
    rm -f "$RBD_KEYFILE"
    RBD_KEYFILE=""
  fi
}

# Split an RBD disk string produced by KVMPhysicalDisk.RBDStringBuilder into the image name and
# the connection arguments the rbd CLI needs:
#   rbd:<pool>/<image>:mon_host=<h1>\;<h2>:auth_client_required=cephx:id=<user>:key=<secret>:...
# ':' and ';' are backslash-escaped inside mon_host so that option splitting stays unambiguous;
# they are masked before the split and restored afterwards.
parse_rbd_opts() {
  local s="${1#rbd:}"
  s="${s//\\:/$'\x01'}"
  s="${s//\\;/$'\x02'}"
  local -a parts=()
  IFS=':' read -ra parts <<< "$s"

  rbd_cleanup_keyfile
  RBD_IMAGE="${parts[0]}"
  RBD_ARGS=()

  local p monHost="" authId="" authKey=""
  for p in "${parts[@]:1}"; do
    case "$p" in
      mon_host=*) monHost="${p#mon_host=}" ;;
      id=*)       authId="${p#id=}" ;;
      key=*)      authKey="${p#key=}" ;;
    esac
  done
  monHost="${monHost//$'\x01'/:}"   # host\:port  -> host:port
  monHost="${monHost//$'\x02'/,}"   # monitor list -> the comma-separated form rbd -m accepts

  if [[ -n "$monHost" ]]; then
    RBD_ARGS+=(-m "$monHost")
  fi
  if [[ -n "$authId" ]]; then
    RBD_ARGS+=(--id "$authId")
    if [[ -n "$authKey" ]]; then
      RBD_KEYFILE=$(mktemp -t csbackup-ceph.XXXXXX)
      chmod 600 "$RBD_KEYFILE"
      printf '%s' "$authKey" > "$RBD_KEYFILE"
      RBD_ARGS+=(--keyfile "$RBD_KEYFILE")
    fi
  fi
}

# Best-effort removal of snapshot $1 from each of the remaining disk arguments. Used both to roll
# back a half-created anchor and to drop the previous anchor once its delta has been exported.
rbd_remove_snapshots() {
  local snapName="$1"
  shift
  local d
  for d in "$@"; do
    [[ -z "$d" ]] && continue
    parse_rbd_opts "$d"
    if ! rbd "${RBD_ARGS[@]}" snap rm "$RBD_IMAGE@$snapName" >> "$logFile" 2>&1; then
      log -e "cleanup: failed to remove RBD snapshot $RBD_IMAGE@$snapName (non-fatal)"
    fi
  done
  rbd_cleanup_keyfile
}

# Remove every snapshot carrying the backup prefix except $1, across the remaining disk arguments.
# Only snapshots this tool created are ever touched: user and CloudStack volume snapshots do not
# use the prefix.
rbd_sweep_stale_anchors() {
  local keepSnap="$1"
  shift
  local d stale
  for d in "$@"; do
    [[ -z "$d" ]] && continue
    parse_rbd_opts "$d"
    while read -r stale; do
      [[ -z "$stale" || "$stale" == "$keepSnap" ]] && continue
      if ! rbd "${RBD_ARGS[@]}" snap rm "$RBD_IMAGE@$stale" >> "$logFile" 2>&1; then
        log -e "cleanup: failed to remove stale RBD snapshot $RBD_IMAGE@$stale (non-fatal)"
      fi
    done < <(
      rbd "${RBD_ARGS[@]}" snap ls "$RBD_IMAGE" --format json 2>>"$logFile" | python3 -c '
import sys, json
try:
    snaps = json.load(sys.stdin)
except Exception:
    sys.exit(0)
for s in snaps or []:
    name = (s or {}).get("name") or ""
    if name.startswith(sys.argv[1]):
        print(name)
' "$RBD_SNAPSHOT_PREFIX" 2>/dev/null || true
    )
  done
  rbd_cleanup_keyfile
}

rbd_snapshot_exists() {
  local snapName="$1"
  rbd "${RBD_ARGS[@]}" snap ls "$RBD_IMAGE" --format json 2>>"$logFile" | python3 -c '
import sys, json
try:
    snaps = json.load(sys.stdin)
except Exception:
    sys.exit(1)
sys.exit(0 if any((s or {}).get("name") == sys.argv[1] for s in snaps or []) else 1)
' "$snapName"
}

# Incremental backup of Ceph/RBD volumes.
#
# RBD images are raw, so they have nowhere to persist a QEMU dirty bitmap and cannot take part in
# the libvirt-checkpoint scheme backup_running_vm uses. Ceph provides the equivalent natively: an
# `rbd snap` per cycle as the anchor, and `rbd export-diff --from-snap` to stream out exactly the
# extents that changed between two anchors (computed inside Ceph from the image object-map).
#
# Because the snapshot lives in Ceph rather than in the QEMU process, this works identically for
# running and stopped VMs, and survives a live migration of the VM between hosts.
#
# Only the newest anchor is kept on each image: once this run's delta is exported, the parent
# snapshot is removed. A full additionally sweeps every other snapshot carrying the backup prefix,
# so anchors abandoned by a deleted chain, an unassigned offering or a restore cannot accumulate.
backup_rbd_vm() {
  if [[ -z "$DISK_PATHS" ]]; then
    echo "RBD backup mode requires the volume paths (-d|--diskpaths)"
    exit 1
  fi
  if [[ -z "$BITMAP_NEW" ]]; then
    echo "RBD backup mode requires --bitmap-new (the snapshot to create)"
    exit 1
  fi

  if ! command -v rbd > /dev/null 2>&1; then
    echo "RBD backup mode requires the rbd command (ceph-common) on this host"
    exit 1
  fi

  local effective_mode="$MODE"
  case "$effective_mode" in
    full|incremental)
      ;;
    *)
      echo "Mode $effective_mode is not supported by the rbd method"
      cleanup
      exit 1
      ;;
  esac
  local -a disks=()
  IFS=',' read -ra disks <<< "$DISK_PATHS"

  local d
  for d in "${disks[@]}"; do
    if [[ "$d" != rbd:* ]]; then
      echo "RBD backup mode was requested but disk $d is not an RBD volume"
      exit 1
    fi
  done

  # The parent anchor must still be present on EVERY disk. A volume migration or an out-of-band
  # snapshot purge can drop it; fall back to a full rather than letting export-diff fail. Checked
  # before anything is mounted or created so the fallback costs nothing.
  if [[ "$effective_mode" == "incremental" ]]; then
    for d in "${disks[@]}"; do
      parse_rbd_opts "$d"
      if ! rbd_snapshot_exists "$BITMAP_PARENT"; then
        log -e "rbd incremental: parent snapshot $BITMAP_PARENT missing on $RBD_IMAGE - falling back to full"
        echo "INCREMENTAL_FALLBACK=true"
        effective_mode="full"
        break
      fi
    done
    rbd_cleanup_keyfile
  fi

  mount_operation
  mkdir -p "$dest" || { echo "Failed to create backup directory $dest"; exit 1; }

  # Freezing the guest filesystems makes the snapshot set filesystem-consistent instead of merely
  # crash-consistent. Only possible while the VM runs and its guest agent answers.
  local thaw=0
  local resume=0
  local vm_state
  vm_state=$(virsh -c qemu:///system domstate "$VM" 2>/dev/null || true)
  if [[ ${QUIESCE} == "true" && "$vm_state" == "running" ]]; then
    if virsh -c qemu:///system qemu-agent-command "$VM" '{"execute":"guest-fsfreeze-freeze"}' > /dev/null 2>/dev/null; then
      thaw=1
    fi
  fi

  # Without a freeze the snapshots of a VM with several disks are taken one after another while
  # the guest keeps writing, so the set would not be crash consistent. Pausing the VM for the few
  # moments the snapshots take restores that, much as a backup-begin checkpoint does. A single
  # disk needs no pause: one snapshot is already a single point in time.
  if [[ $thaw -eq 0 && "$vm_state" == "running" && ${#disks[@]} -gt 1 ]]; then
    if virsh -c qemu:///system suspend "$VM" > /dev/null 2>&1; then
      resume=1
    else
      log -ne "WARNING: unable to pause VM $VM, the disk snapshots will not be taken at the same instant"
    fi
  fi

  # Create this cycle's anchor on every disk back to back, so the set is as close to a single
  # point in time for the VM as a whole as Ceph allows.
  local -a snapped=()
  local snap_failed=0
  for d in "${disks[@]}"; do
    parse_rbd_opts "$d"
    if rbd "${RBD_ARGS[@]}" snap create "$RBD_IMAGE@$BITMAP_NEW" >> "$logFile" 2>&1; then
      snapped+=("$d")
    else
      log -e "Failed to create RBD snapshot $RBD_IMAGE@$BITMAP_NEW"
      snap_failed=1
      break
    fi
  done
  rbd_cleanup_keyfile

  if [[ $thaw -eq 1 ]]; then
    if ! response=$(virsh -c qemu:///system qemu-agent-command "$VM" '{"execute":"guest-fsfreeze-thaw"}' 2>&1); then
      echo "Failed to thaw the filesystem for vm $VM: $response"
      rbd_remove_snapshots "$BITMAP_NEW" "${snapped[@]}"
      cleanup
      exit 1
    fi
  fi

  if [[ $resume -eq 1 ]]; then
    if ! virsh -c qemu:///system resume "$VM" > /dev/null 2>&1; then
      echo "Failed to resume vm $VM after taking the disk snapshots"
      rbd_remove_snapshots "$BITMAP_NEW" "${snapped[@]}"
      cleanup
      exit 1
    fi
  fi

  if [[ $snap_failed -eq 1 ]]; then
    echo "Failed to create RBD snapshot $BITMAP_NEW for vm $VM"
    rbd_remove_snapshots "$BITMAP_NEW" "${snapped[@]}"
    cleanup
    exit 1
  fi

  # Export each disk's delta. A full exports every allocated extent of the image, which replayed
  # into a freshly created image of the same size reproduces it exactly.
  local name="root"
  local volUuid output
  for d in "${disks[@]}"; do
    parse_rbd_opts "$d"
    volUuid=$(get_ceph_uuid_from_path "$d")
    output="$dest/$name.$volUuid.rbddiff"
    if [[ "$effective_mode" == "incremental" ]]; then
      if ! rbd "${RBD_ARGS[@]}" export-diff --from-snap "$BITMAP_PARENT" "$RBD_IMAGE@$BITMAP_NEW" "$output" >> "$logFile" 2>&1; then
        echo "rbd export-diff --from-snap $BITMAP_PARENT failed for $RBD_IMAGE"
        rbd_remove_snapshots "$BITMAP_NEW" "${snapped[@]}"
        cleanup
        exit 1
      fi
    else
      if ! rbd "${RBD_ARGS[@]}" export-diff "$RBD_IMAGE@$BITMAP_NEW" "$output" >> "$logFile" 2>&1; then
        echo "rbd export-diff failed for $RBD_IMAGE"
        rbd_remove_snapshots "$BITMAP_NEW" "${snapped[@]}"
        cleanup
        exit 1
      fi
    fi
    name="datadisk"
  done
  rbd_cleanup_keyfile

  # Backup domain information (running VMs only — a stopped VM has no libvirt domain defined).
  if [[ "$vm_state" == "running" ]]; then
    virsh -c qemu:///system dumpxml $VM > $dest/domain-config.xml 2>/dev/null || true
    virsh -c qemu:///system dominfo $VM > $dest/dominfo.xml 2>/dev/null || true
    virsh -c qemu:///system domiflist $VM > $dest/domiflist.xml 2>/dev/null || true
    virsh -c qemu:///system domblklist $VM > $dest/domblklist.xml 2>/dev/null || true
  fi

  sync

  # The parent anchor has served its purpose: its delta is on the NAS and BITMAP_NEW tracks
  # changes from here on. Dropping it keeps exactly one backup snapshot per image.
  if [[ "$effective_mode" == "incremental" && -n "$BITMAP_PARENT" && "$BITMAP_PARENT" != "$BITMAP_NEW" ]]; then
    rbd_remove_snapshots "$BITMAP_PARENT" "${disks[@]}"
  else
    # A full starts a new chain, so every anchor this tool left on the image before it is stale --
    # including ones whose chain was abandoned without a chance to clean up. Sweep them all.
    rbd_sweep_stale_anchors "$BITMAP_NEW" "${disks[@]}"
  fi

  local backup_size
  backup_size=$(du -sb "$dest" 2>>"$logFile" | cut -f1) || { log -ne "WARNING: du failed for $dest, reporting size as 0"; backup_size=0; }
  timeout "$UNMOUNT_TIMEOUT" umount "$mount_point" 2>>"$logFile" || { log "WARNING: umount of $mount_point failed or timed out"; true; }
  rmdir "$mount_point" 2>>"$logFile" || { log "WARNING: rmdir of $mount_point failed"; true; }
  echo "$backup_size"
}

qemu_agent_command() {
  local timeout_args=()
  if [[ -n "$QUIESCE_TIMEOUT" ]]; then
    timeout_args=(--timeout "$QUIESCE_TIMEOUT")
  fi
  virsh -c qemu:///system qemu-agent-command "$VM" "${timeout_args[@]}" "$1"
}

guest_fsfreeze_status() {
  local response
  response=$(qemu_agent_command '{"execute":"guest-fsfreeze-status"}' 2>>"$logFile") || return 1
  grep -oE 'thawed|frozen' <<< "$response" | head -n 1
}

guest_agent_connected() {
  local xml
  xml=$(virsh -c qemu:///system dumpxml "$VM" 2>/dev/null) || return 1
  grep -q "name='org.qemu.guest_agent.0' state='connected'" <<< "$xml"
}

# Thaws the guest filesystems. A thaw that times out on the host can still complete in the
# guest, so each failure is checked against guest-fsfreeze-status before retrying. Succeeds
# once the guest reports thawed, or when the freeze failed and the guest agent is not
# connected, which means the freeze never reached the guest.
thaw_guest() {
  local freeze_ok=$1
  local attempt response state
  for ((attempt = 1; attempt <= 3; attempt++)); do
    if response=$(qemu_agent_command '{"execute":"guest-fsfreeze-thaw"}' 2>&1); then
      return 0
    fi
    log -e "Thaw attempt $attempt for vm $VM failed: $response"
    state=$(guest_fsfreeze_status || true)
    if [[ "$state" == "thawed" ]]; then
      log -e "Guest filesystem of vm $VM reports thawed after the failed thaw"
      return 0
    fi
    if [[ -z "$state" && $freeze_ok -eq 0 ]] && ! guest_agent_connected; then
      log -e "Guest agent of vm $VM is not connected, so the failed freeze never reached the guest"
      return 0
    fi
    sleep 5
  done
  echo "Failed to thaw the filesystem for vm $VM, guest filesystem state: ${state:-unknown}: $response"
  return 1
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
    elif [[ "$fullpath" == /dev/drbd[0-9]* ]]; then
        if ! volUuid=$(get_linstor_uuid_from_device "$fullpath"); then
            echo "Failed to resolve LINSTOR volume UUID for $fullpath"
            cleanup
            exit 1
        fi
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
    if [[ "$fullpath" == /dev/drbd/by-res/* ]]; then
      volUuid=$(get_linstor_uuid_from_path "$fullpath")
    elif [[ "$fullpath" == /dev/drbd[0-9]* ]]; then
      if ! volUuid=$(get_linstor_uuid_from_device "$fullpath"); then
        echo "Failed to resolve LINSTOR volume UUID for $fullpath"
        cleanup
        exit 1
      fi
    else
      name="datadisk"
      continue
    fi
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
    elif [[ "$disk" == /dev/drbd[0-9]* ]]; then
      if ! volUuid=$(get_linstor_uuid_from_device "$disk"); then
        echo "Failed to resolve LINSTOR volume UUID for $disk"
        cleanup
        exit 1
      fi
    else
      volUuid="${disk##*/}"
    fi
    output="$dest/$name.$volUuid.qcow2"
    if ! qemu-img convert -O qcow2 "$disk" "$output" >> "$logFile" 2> >(cat >&2); then
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

# Remove every backup anchor (csbackup-*) from the RBD disks named by -d. The anchor of a chain is
# only kept to export the next incremental from it, so once the chain is gone - its last backup was
# deleted, or the Instance left its backup offering - the next backup is a full one and the anchor
# only pins the space of the data it still references in the pool.
remove_rbd_anchors() {
  if [[ -z "$DISK_PATHS" ]]; then
    echo "Removing the RBD anchors requires the volume paths (-d|--diskpaths)"
    exit 1
  fi
  local -a disks=()
  local d
  IFS=',' read -ra disks <<< "$DISK_PATHS"
  for d in "${disks[@]}"; do
    # a volume of any other pool type carries no anchor, and is not an rbd image to ask about
    [[ "$d" == rbd:* ]] || continue
    rbd_sweep_stale_anchors "" "$d"
  done
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

  rbd_cleanup_keyfile

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
  echo "Operations: backup, delete, stats, removeanchors"
  echo "  removeanchors           Remove every backup anchor (csbackup-*) from the RBD volumes given with -d|--disks."
  echo "                          Used once a chain is gone, as the next backup is a full one and the anchor would"
  echo "                          only keep the data it references allocated in the pool."
  echo ""
  echo "Incremental backup options (running VMs only; requires QEMU >= 4.2 and libvirt >= 7.2):"
  echo "  -M|--mode full          Take a full backup AND create a checkpoint (--bitmap-new required) for future incrementals."
  echo "  -M|--mode incremental   Take an incremental backup since --bitmap-parent and create new checkpoint --bitmap-new."
  echo "                          Requires --bitmap-parent, --bitmap-new, and --parent-paths (comma-separated list, one"
  echo "                          parent qcow2 path per disk: root.<uuid>.qcow2, datadisk.<uuid>.qcow2, … same order"
  echo "                          as -d|--disks)."
  echo "  --method qcow2              Default. Push-mode backup; incrementals come from QEMU dirty bitmaps."
  echo "  --method rbd                Ceph-native backup of RBD volumes. With -M full, snapshots every volume"
  echo "                              (--bitmap-new) and exports it whole with rbd export-diff; --bitmap-parent,"
  echo "                              if given, names the previous chain's snapshot and is removed once the"
  echo "                              export succeeds. With -M incremental, exports only what changed since"
  echo "                              --bitmap-parent. Requires -d|--disks, and falls back to a full when the"
  echo "                              parent snapshot is missing on any disk."
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
    --method)
      METHOD="$2"
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

# The cephx key file is written to /tmp for the lifetime of an rbd call. cleanup() removes it,
# but only the failure paths call cleanup, so leave its removal to the shell as well: every exit
# from here on drops the key, including the ones that end the script without reaching cleanup.
trap rbd_cleanup_keyfile EXIT

# Perform initial environment sanity checks (QEMU/libvirt version).
sanity_checks

if [ "$OP" = "backup" ]; then
  if [[ "$METHOD" == "rbd" ]]; then
    # RBD chains snapshot inside Ceph, so the VM's run state does not change the procedure.
    backup_rbd_vm
  else
    STATE=$(virsh -c qemu:///system list | awk -v vm="$VM" '$2 == vm {print $3}')
    if [ -n "$STATE" ] && [ "$STATE" = "running" ]; then
      backup_running_vm
    else
      backup_stopped_vm
    fi
  fi
elif [ "$OP" = "delete" ]; then
  delete_backup
elif [ "$OP" = "removeanchors" ]; then
  remove_rbd_anchors
elif [ "$OP" = "stats" ]; then
  get_backup_stats
fi
