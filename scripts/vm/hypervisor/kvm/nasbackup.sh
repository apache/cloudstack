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
COMPRESS=""
BANDWIDTH=""
ENCRYPT_PASSFILE=""
VERIFY=""
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

encrypt_backup() {
  local backup_dir="$1"
  if [[ -z "$ENCRYPT_PASSFILE" ]]; then
    return
  fi
  if [[ ! -f "$ENCRYPT_PASSFILE" ]]; then
    echo "Encryption passphrase file not found: $ENCRYPT_PASSFILE"
    return 1
  fi
  log -ne "Encrypting backup files with LUKS"
  # No -c here: qcow2 cannot compress and encrypt the same image, so the combination is
  # rejected before the backup starts (see the check before the operation dispatch).
  for img in "$backup_dir"/*.qcow2; do
    [[ -f "$img" ]] || continue
    local tmp_img="${img}.luks"
    if qemu-img convert -O qcow2 \
        --object "secret,id=sec0,file=$ENCRYPT_PASSFILE" \
        -o "encrypt.format=luks,encrypt.key-secret=sec0" \
        "$img" "$tmp_img" >> "$logFile" 2>&1; then
      mv "$tmp_img" "$img"
      log -ne "Encrypted: $img"
    else
      echo "Encryption failed for $img"
      rm -f "$tmp_img"
      return 1
    fi
  done
}

verify_backup() {
  local backup_dir="$1"
  local failed=0
  # If encryption was applied to this backup, qemu-img check has to open the
  # qcow2 with the same LUKS secret — otherwise every verification call fails
  # with a "Could not open" error and --verify is unusable on encrypted
  # backups.
  local check_secret=()
  if [[ -n "$ENCRYPT_PASSFILE" && -f "$ENCRYPT_PASSFILE" ]]; then
    check_secret=(--object "secret,id=sec0,file=$ENCRYPT_PASSFILE")
  fi
  for img in "$backup_dir"/*.qcow2; do
    [[ -f "$img" ]] || continue
    local check_rc=0
    if [[ ${#check_secret[@]} -gt 0 ]]; then
      qemu-img check "${check_secret[@]}" --image-opts \
        "driver=qcow2,file.filename=$img,encrypt.key-secret=sec0" \
        > /dev/null 2>&1 || check_rc=$?
    else
      qemu-img check "$img" > /dev/null 2>&1 || check_rc=$?
    fi
    # qemu-img check: 0 = clean, 3 = leaked clusters only (wasted space, data intact),
    # 2 = corruption, 1 = check could not complete, 63 = format cannot be checked.
    case $check_rc in
      0)
        log -ne "Backup verification passed: $img" ;;
      3)
        log -ne "Backup verification passed with leaked clusters (wasted space only, data intact): $img" ;;
      *)
        echo "Backup verification failed for $img (qemu-img check exit code $check_rc)"
        log -ne "Backup verification FAILED (qemu-img check exit code $check_rc): $img"
        failed=1 ;;
    esac
  done
  if [[ $failed -ne 0 ]]; then
    echo "One or more backup files failed verification"
    return 1
  fi
}

# qemu-img convert -r (rate limit) arrived in QEMU 5.2; older hosts reject the option. Probe it on
# a throwaway 1 MiB image rather than parsing --help, whose format differs between releases.
qemu_img_supports_rate_limit() {
  local probe_dir rc=0
  probe_dir=$(mktemp -d) || return 1
  { qemu-img create -q -f qcow2 "$probe_dir/src.qcow2" 1M &&
    qemu-img convert -r 1G -O qcow2 "$probe_dir/src.qcow2" "$probe_dir/dst.qcow2"; } > /dev/null 2>&1 || rc=$?
  rm -rf "$probe_dir"
  return $rc
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

  # Compression and encryption rewrite each file with qemu-img convert, which would flatten an
  # incremental through its backing chain. The management server never combines them (a zone
  # with either enabled takes full backups); refuse the combination rather than silently
  # producing a full-size or unreadable chain member.
  if [[ "$effective_mode" == "incremental" && ( "$COMPRESS" == "true" || -n "$ENCRYPT_PASSFILE" ) ]]; then
    echo "Incremental mode cannot be combined with compression or encryption"
    cleanup
    exit 1
  fi

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

  # Throttle backup bandwidth if requested (MiB/s per disk). Log what actually happened per disk:
  # a failed set-speed leaves that disk unthrottled, and the log must not claim otherwise.
  if [[ -n "$BANDWIDTH" ]]; then
    local throttled=0 not_throttled=0 bw_out
    for disk in $(virsh -c qemu:///system domblklist $VM --details 2>/dev/null | awk '$2=="disk"{print $3}'); do
      if bw_out=$(virsh -c qemu:///system blockjob $VM $disk --bandwidth "${BANDWIDTH}" 2>&1); then
        throttled=$((throttled + 1))
      else
        not_throttled=$((not_throttled + 1))
        log -ne "WARNING: could not limit backup bandwidth on $VM disk $disk, it runs unthrottled: $bw_out"
      fi
    done
    log -ne "Backup bandwidth limit of ${BANDWIDTH} MiB/s applied to $throttled disk(s) of $VM; $not_throttled disk(s) unthrottled"
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
        return 1 ;;
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

  # Compress backup files if requested
  if [[ "$COMPRESS" == "true" ]]; then
    log -ne "Compressing backup files for $VM"
    for img in "$dest"/*.qcow2; do
      [[ -f "$img" ]] || continue
      local tmp_img="${img}.tmp"
      if qemu-img convert -c -O qcow2 "$img" "$tmp_img" >> "$logFile" 2>&1; then
        mv "$tmp_img" "$img"
      else
        log -ne "Warning: compression failed for $img, keeping uncompressed"
        rm -f "$tmp_img"
      fi
    done
  fi

  # Encrypt backup files if requested
  if ! encrypt_backup "$dest"; then
    cleanup
    return 1
  fi

  sync

  # Verify backup integrity if requested
  if [[ "$VERIFY" == "true" ]]; then
    if ! verify_backup "$dest"; then
      cleanup
      return 1
    fi
  fi

  # Print statistics
  virsh -c qemu:///system domjobinfo $VM --completed
  du -sb $dest | cut -f1

  umount $mount_point
  rmdir $mount_point
}

backup_stopped_vm() {
  # Stopped VMs cannot use libvirt's backup-begin (no QEMU process); take a full backup via
  # qemu-img convert. The orchestrator never sends incremental mode for a stopped VM.
  mount_operation
  mkdir -p "$dest" || { echo "Failed to create backup directory $dest"; exit 1; }

  # Optional convert flags. ionice and -r only apply when a bandwidth limit is configured, so
  # backups without the new settings run exactly as before.
  local convert_opts=() io_prio=()
  if [[ "$COMPRESS" == "true" ]]; then
    convert_opts+=(-c)
  fi
  if [[ -n "$BANDWIDTH" ]]; then
    io_prio=(ionice -c 3)
    if qemu_img_supports_rate_limit; then
      convert_opts+=(-r "${BANDWIDTH}M")
      log -ne "Backup bandwidth limited to ${BANDWIDTH} MiB/s per disk for $VM"
    else
      log -ne "WARNING: qemu-img on this host does not support convert -r (QEMU >= 5.2 required); $VM is backed up without the ${BANDWIDTH} MiB/s limit, at idle I/O priority only"
    fi
  fi

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
    if ! "${io_prio[@]}" qemu-img convert "${convert_opts[@]}" -O qcow2 "$disk" "$output" >> "$logFile" 2> >(cat >&2); then
      echo "qemu-img convert failed for $disk $output"
      cleanup
      return 1
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

  # Encrypt backup files if requested
  if ! encrypt_backup "$dest"; then
    cleanup
    return 1
  fi

  sync

  # Verify backup integrity if requested
  if [[ "$VERIFY" == "true" ]]; then
    if ! verify_backup "$dest"; then
      cleanup
      return 1
    fi
  fi

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
  if mount -t ${NAS_TYPE} ${NAS_ADDRESS} ${mount_point} $([[ ! -z "${MOUNT_OPTS}" ]] && echo -o ${MOUNT_OPTS}) >> "$logFile" 2>&1; then
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
  echo "Usage: $0 -o <operation> -v|--vm <domain name> -t <storage type> -s <storage address> -m <mount options> -p <backup path> -d <disks path> -q|--quiesce <true|false> [-c] [-b <MiB/s>] [-e <passphrase file>] [--verify]"
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
    -c|--compress)
      COMPRESS="true"
      shift
      ;;
    -b|--bandwidth)
      BANDWIDTH="$2"
      shift
      shift
      ;;
    -e|--encrypt)
      ENCRYPT_PASSFILE="$2"
      shift
      shift
      ;;
    --verify)
      VERIFY="true"
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

# qcow2 cannot compress and encrypt the same image. Refuse the combination before anything is
# mounted or written, so no partial backup is created and then deleted.
if [[ "$OP" == "backup" && "$COMPRESS" == "true" && -n "$ENCRYPT_PASSFILE" ]]; then
  echo "Compression and encryption cannot be combined: qcow2 does not support both on one image"
  exit 1
fi

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
