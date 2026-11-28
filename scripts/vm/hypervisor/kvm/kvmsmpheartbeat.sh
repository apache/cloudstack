#!/bin/bash
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

help() {
  printf "Usage: $0
                -i identifier (required for CLI compatibility; value ignored by local-only heartbeat)
                -p path (required for CLI compatibility; value ignored by local-only heartbeat)
                -m mount point (local path where heartbeat will be written)
                -h host (host IP/name to include in heartbeat filename)
                -r write/read hb log (read-check mode)
                -c cleanup (trigger emergency reboot)
                -t interval between read hb log\n"
  exit 1
}

#set -x
NfsSvrIP=
NfsSvrPath=
MountPoint=
HostIP=
interval=
rflag=0
cflag=0

while getopts 'i:p:m:h:t:rc' OPTION
do
  case $OPTION in
  i)
     NfsSvrIP="$OPTARG"
     ;; # retained for CLI compatibility but unused for this script
  p)
     NfsSvrPath="$OPTARG"
     ;; # retained for CLI compatibility but unused for this script
  m)
     MountPoint="$OPTARG"
     ;;
  h)
     HostIP="$OPTARG"
     ;;
  r)
     rflag=1
     ;;
  t)
     interval="$OPTARG"
     ;;
  c)
    cflag=1
     ;;
  *)
     help
     ;;
  esac
done

# For heartbeat we require a mountpoint
if [ -z "$MountPoint" ]
then
   echo "Mount point (-m) is required"
   help
fi

# Validate mount point exists, is (if possible) a mounted filesystem, and is writable
if [ ! -d "$MountPoint" ]; then
  echo "Mount point directory does not exist: $MountPoint" >&2
  exit 1
fi

# Returns 0 if the given path is usable for the heartbeat:
# - the path is a mount point itself (same behaviour as before), or
# - the path is a subdirectory of a mounted (e.g. clustered) filesystem that
#   is neither the root filesystem nor a local disk filesystem.
# The second rule keeps a lost exact mount from passing: its empty directory
# then belongs to the parent filesystem (e.g. a local /var), which is refused.
is_on_mounted_fs() {
  local target fstype
  if command -v mountpoint >/dev/null 2>&1 && mountpoint -q "$1"; then
    return 0
  fi
  if command -v findmnt >/dev/null 2>&1; then
    target=$(findmnt -n -o TARGET -T "$1" 2>/dev/null)
    fstype=$(findmnt -n -o FSTYPE -T "$1" 2>/dev/null)
  else
    target=$(df -P "$1" 2>/dev/null | awk 'NR==2 {print $6}')
    fstype=$(df -PT "$1" 2>/dev/null | awk 'NR==2 {print $2}')
  fi
  [ -n "$target" ] && [ "$target" != "/" ] || return 1
  case "$fstype" in
    ext2|ext3|ext4|xfs|btrfs|zfs|f2fs|vfat|exfat|ntfs|ntfs3|tmpfs|ramfs|overlay|squashfs)
      return 1
      ;;
  esac
  return 0
}

# Ensure the path is on a mounted filesystem (not the local root filesystem)
if ! is_on_mounted_fs "$MountPoint"; then
  echo "Mount point is not on a mounted filesystem: $MountPoint" >&2
  exit 1
fi

# Ensure the mount point is writable
if [ ! -w "$MountPoint" ]; then
  echo "Mount point is not writable: $MountPoint" >&2
  exit 1
fi

hbFolder="$MountPoint/KVMHA"
hbFile="$hbFolder/hb-$HostIP"

write_hbLog() {
#write the heart beat log
  stat "$hbFile" &> /dev/null
  if [ $? -gt 0 ]
  then
     # create a new one
     mkdir -p "$hbFolder" &> /dev/null
     # touch will be done by atomic write below; ensure folder is writable
     if [ ! -w "$hbFolder" ]; then
       printf "Folder not writable: $hbFolder" >&2
       return 2
     fi
  fi

  # A run killed on timeout (SIGKILL) cannot run its trap; remove its
  # leftover temporary file on the next run.
  find "$hbFolder" -maxdepth 1 -name "hb-$HostIP.*" -mmin +1 -delete 2>/dev/null

  timestamp=$(date +%s)
  # Write atomically to avoid partial writes (write to tmp then mv)
  tmpfile="${hbFile}.$$"
  # remove the temporary file if the script is interrupted (e.g. on timeout)
  trap 'rm -f "$tmpfile"' EXIT
  trap 'exit 1' INT TERM
  printf "%s\n" "$timestamp" > "$tmpfile" 2>/dev/null
  if [ $? -ne 0 ]; then
    printf "Failed to write heartbeat to $tmpfile" >&2
    return 2
  fi
  mv -f "$tmpfile" "$hbFile" 2>/dev/null
  return $?
}

check_hbLog() {
  hb_diff=0
  if [ ! -f "$hbFile" ]; then
    # signal large difference if file missing
    hb_diff=999999
    return 1
  fi
  now=$(date +%s)
  hb=$(cat "$hbFile" 2>/dev/null)
  if [ -z "$hb" ]; then
    hb_diff=999998
    return 1
  fi
  # note: 'expr' exits with 1 when the result is 0, so use shell arithmetic
  # only accept a plain decimal timestamp of sane length
  case "$hb" in
    ''|*[!0-9]*)
      hb_diff=999997
      return 1
      ;;
  esac
  if [ ${#hb} -gt 12 ]; then
    hb_diff=999997
    return 1
  fi
  # base 10, otherwise a leading 0 (e.g. "08") is parsed as octal
  diff=$((now - 10#$hb))
  if [ -z "$interval" ]; then
    # if no interval provided, consider 0 as success
    if [ $diff -gt 0 ]; then
      hb_diff=$diff
      return 1
    else
      hb_diff=0
      return 0
    fi
  fi
  if [ $diff -gt $interval ]
  then
    hb_diff=$diff
    return 1
  fi
  hb_diff=0
  return 0
}

if [ "$rflag" == "1" ]
then
  check_hbLog
  status=$?
  diff="${hb_diff:-0}"
  if [ $status -eq 0 ]
  then
    echo "=====> ALIVE <====="
  else
    echo "=====> Considering host as DEAD because last write on [$hbFile] was [$diff] seconds ago, but the max interval is [$interval] <======"
  fi
  exit 0
elif [ "$cflag" == "1" ]
then
  /usr/bin/logger -t heartbeat "kvmsmpheartbeat.sh will reboot system because it was unable to write the heartbeat to the storage."
  sync &
  sleep 5
  echo b > /proc/sysrq-trigger
  exit $?
else
  write_hbLog
  exit $?
fi
