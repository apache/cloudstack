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

#
# Enumerate GPUs (NVIDIA, Intel, AMD) and output JSON for libvirt:
#   - PCI metadata (address, vendor/device IDs, driver, pci_class)
#   - IOMMU group
#   - SR-IOV VF counts
#   - full_passthrough block
#   - vGPU (MDEV) instances with available_instances
#   - VF (SR-IOV/MIG) instances with actual profile names
#
# Uses `lspci -nnm` for GPU discovery and `virsh` to detect VM attachments.
# Compatible with Ubuntu (20.04+, 22.04+) and RHEL/CentOS (7/8), Bash ≥4.
#
# Sample JSON:
# {
#   "gpus": [
#     {
#       "pci_address": "00:03.0",
#       "vendor_id": "10de",
#       "device_id": "2484",
#       "vendor": "NVIDIA Corporation",
#       "device": "GeForce RTX 3070",
#       "driver": "nvidia",
#       "pci_class": "VGA compatible controller",
#       "iommu_group": "8",
#       "sriov_totalvfs": 0,
#       "sriov_numvfs": 0,

#       "full_passthrough": {
#         "enabled": true,
#         "libvirt_address": {
#           "domain": "0x0000",
#           "bus": "0x00",
#           "slot": "0x03",
#           "function": "0x0"
#         },
#         "used_by_vm": "win10"
#       },

#       "vgpu_instances": [],

#       "vf_instances": []
#     },
#     {
#       "pci_address": "00:AF.0",
#       "vendor_id": "10de",
#       "device_id": "1EB8",
#       "vendor": "NVIDIA Corporation",
#       "device": "Tesla T4",
#       "driver": "nvidia",
#       "pci_class": "3D controller",
#       "iommu_group": "12",
#       "sriov_totalvfs": 0,
#       "sriov_numvfs": 0,

#       "full_passthrough": {
#         "enabled": false,
#         "libvirt_address": {
#           "domain": "0x0000",
#           "bus": "0x00",
#           "slot": "0xAF",
#           "function": "0x0"
#         },
#         "used_by_vm": null
#       },

#       "vgpu_instances": [
#         {
#           "mdev_uuid": "a1b2c3d4-5678-4e9a-8b0c-d1e2f3a4b5c6",
#           "profile_name": "grid_t4-16c",
#           "available_instances": 4,
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0xAF",
#             "function": "0x0"
#           },
#           "used_by_vm": "vm1"
#         },
#         {
#           "mdev_uuid": "b2c3d4e5-6789-4f0a-9c1d-e2f3a4b5c6d7",
#           "profile_name": "grid_t4-8c",
#           "available_instances": 8,
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0xAF",
#             "function": "0x1"
#           },
#           "used_by_vm": "vm2"
#         }
#       ],

#       "vf_instances": []
#     },
#     {
#       "pci_address": "00:65.0",
#       "vendor_id": "10de",
#       "device_id": "20B0",
#       "vendor": "NVIDIA Corporation",
#       "device": "A100-SXM4-40GB",
#       "driver": "nvidia",
#       "pci_class": "VGA compatible controller",
#       "iommu_group": "15",
#       "sriov_totalvfs": 7,
#       "sriov_numvfs": 7,

#       "full_passthrough": {
#         "enabled": false,
#         "libvirt_address": {
#           "domain": "0x0000",
#           "bus": "0x00",
#           "slot": "0x65",
#           "function": "0x0"
#         },
#         "used_by_vm": null
#       },

#       "vgpu_instances": [
#         {
#           "mdev_uuid": "f4a2c8de-1234-4b3a-8c9d-0a1b2c3d4e5f",
#           "profile_name": "grid_a100-8c",
#           "available_instances": 8,
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x65",
#             "function": "0x0"
#           },
#           "used_by_vm": null
#         },
#         {
#           "mdev_uuid": "e5b3d9ef-5678-4c2b-9d0e-1b2c3d4e5f6a",
#           "profile_name": "grid_a100-5c",
#           "available_instances": 5,
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x65",
#             "function": "0x1"
#           },
#           "used_by_vm": null
#         }
#       ],

#       "vf_instances": [
#         {
#           "vf_pci_address": "65:00.2",
#           "vf_profile": "1g.5gb",
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x65",
#             "function": "0x2"
#           },
#           "used_by_vm": "ml"
#         },
#         {
#           "vf_pci_address": "65:00.3",
#           "vf_profile": "2g.10gb",
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x65",
#             "function": "0x3"
#           },
#           "used_by_vm": null
#         }
#       ]
#     },
#     {
#       "pci_address": "00:02.0",
#       "vendor_id": "8086",
#       "device_id": "46A6",
#       "vendor": "Intel Corporation",
#       "device": "Alder Lake-P GT2 [Iris Xe Graphics]",
#       "driver": "i915",
#       "pci_class": "VGA compatible controller",
#       "iommu_group": "0",
#       "sriov_totalvfs": 4,
#       "sriov_numvfs": 4,

#       "full_passthrough": {
#         "enabled": false,
#         "libvirt_address": {
#           "domain": "0x0000",
#           "bus": "0x00",
#           "slot": "0x02",
#           "function": "0x0"
#         },
#         "used_by_vm": null
#       },

#       "vgpu_instances": [
#         {
#           "mdev_uuid": "b7c8d9fe-1111-2222-3333-444455556666",
#           "profile_name": "i915-GVTg_V5_4",
#           "available_instances": 4,
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x02",
#             "function": "0x0"
#           },
#           "used_by_vm": null
#         },
#         {
#           "mdev_uuid": "c8d9e0af-7777-8888-9999-000011112222",
#           "profile_name": "i915-GVTg_V5_8",
#           "available_instances": 8,
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x02",
#             "function": "0x1"
#           },
#           "used_by_vm": null
#         }
#       ],

#       "vf_instances": [
#         {
#           "vf_pci_address": "00:02.1",
#           "vf_profile": "Intel SR-IOV VF 1",
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x02",
#             "function": "0x1"
#           },
#           "used_by_vm": "linux01"
#         },
#         {
#           "vf_pci_address": "00:02.2",
#           "vf_profile": "Intel SR-IOV VF 2",
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x02",
#             "function": "0x2"
#           },
#           "used_by_vm": null
#         }
#       ]
#     },
#     {
#       "pci_address": "00:03.0",
#       "vendor_id": "1002",
#       "device_id": "7340",
#       "vendor": "AMD",
#       "device": "Instinct MI210",
#       "driver": "amdgpu",
#       "pci_class": "3D controller",
#       "iommu_group": "8",
#       "sriov_totalvfs": 8,
#       "sriov_numvfs": 8,

#       "full_passthrough": {
#         "enabled": false,
#         "libvirt_address": {
#           "domain": "0x0000",
#           "bus": "0x00",
#           "slot": "0x03",
#           "function": "0x0"
#         },
#         "used_by_vm": null
#       },

#       "vgpu_instances": [],

#       "vf_instances": [
#         {
#           "vf_pci_address": "03:00.1",
#           "vf_profile": "mi210-4c",
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x03",
#             "function": "0x1"
#           },
#           "used_by_vm": null
#         },
#         {
#           "vf_pci_address": "03:00.2",
#           "vf_profile": "mi210-2c",
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x03",
#             "function": "0x2"
#           },
#           "used_by_vm": null
#         },
#         {
#           "vf_pci_address": "03:00.3",
#           "vf_profile": "mi210-1c",
#           "libvirt_address": {
#             "domain": "0x0000",
#             "bus": "0x00",
#             "slot": "0x03",
#             "function": "0x3"
#           },
#           "used_by_vm": null
#         }
#       ]
#     }
#   ]
# }
#

set -euo pipefail

# === Utility Functions ===

# Escape a string for JSON
json_escape() {
  local str=${1//\\/\\\\}
  str=${str//\"/\\\"}
  str=${str//$'\n'/\\n}
  printf '"%s"' "$str"
}

# Given a PCI address (e.g. "00:02.0"), return its IOMMU group or "null"
get_iommu_group() {
  local addr="$1"
  for grp in /sys/kernel/iommu_groups/*/devices/*; do
    if [[ "${grp##*/}" == "0000:$addr" ]]; then
      echo "${grp#*/iommu_groups/}" | cut -d/ -f1
      return
    fi
  done
  echo "null"
}

# Given a PCI address, output "TOTALVFS NUMVFS"
get_sriov_counts() {
  local addr="$1"
  local path="/sys/bus/pci/devices/0000:$addr"
  if [[ -f "$path/sriov_totalvfs" ]]; then
    echo "$(<"$path/sriov_totalvfs") $(<"$path/sriov_numvfs")"
  else
    echo "0 0"
  fi
}

# Build VM → hostdev maps:
#   pci_to_vm[BDF] = VM name that attaches that BDF
#   mdev_to_vm[UUID] = VM name that attaches that MDEV UUID
declare -A pci_to_vm mdev_to_vm

# Gather all VM names (including inactive)
mapfile -t VMS < <(virsh list --all --name | grep -v '^$')
for VM in "${VMS[@]}"; do
  # Skip if dumpxml fails
  if ! xml=$(virsh dumpxml "$VM" 2>/dev/null); then
    continue
  fi

  # -- PCI hostdevs: locate <hostdev type='pci'> blocks and extract BDF --
  while IFS= read -r line; do
    if [[ $line =~ \<hostdev && $line =~ type=\'pci\' ]]; then
      # Within this hostdev block, find the <address .../> line
      while IFS= read -r sub; do
        if [[ $sub =~ bus=\'0x([0-9A-Fa-f]{2})\'[[:space:]]slot=\'0x([0-9A-Fa-f]{2})\'[[:space:]]function=\'0x([0-9A-Fa-f])\' ]]; then
          B="${BASH_REMATCH[1]}"
          S="${BASH_REMATCH[2]}"
          F="${BASH_REMATCH[3]}"
          BDF="${B}:${S}.${F}"
          pci_to_vm["$BDF"]="$VM"
          break
        fi
      done
    fi
  done <<< "$xml"

  # -- MDEV hostdevs: locate <hostdev type='mdev'> and extract UUID --
  while IFS= read -r line; do
    if [[ $line =~ \<hostdev && $line =~ type=\'mdev\' ]]; then
      # Within this hostdev block, find the <address uuid='...'/> line
      while IFS= read -r sub; do
        if [[ $sub =~ uuid=\'([0-9a-fA-F-]+)\' ]]; then
          UUID="${BASH_REMATCH[1]}"
          mdev_to_vm["$UUID"]="$VM"
          break
        fi
      done
    fi
  done <<< "$xml"
done

# Helper: convert a VM name to JSON value (quoted string or null)
to_json_vm() {
  local vm="$1"
  if [[ -z "$vm" ]]; then
    echo "null"
  else
    json_escape "$vm"
  fi
}

# === GPU Discovery ===

mapfile -t LINES < <(lspci -nnm)

echo '{ "gpus": ['

first_gpu=true
for LINE in "${LINES[@]}"; do
  # Parse lspci -nnm fields: SLOT "CLASS [CODE]" "VENDOR [VID]" "DEVICE [DID]" ...
  if [[ $LINE =~ ^([^[:space:]]+)[[:space:]]\"([^\"]+)\"[[:space:]]\"([^\"]+)\"[[:space:]]\"([^\"]+)\" ]]; then
    PCI_ADDR="${BASH_REMATCH[1]}"
    PCI_CLASS="${BASH_REMATCH[2]}"
    VENDOR_FIELD="${BASH_REMATCH[3]}"
    DEVICE_FIELD="${BASH_REMATCH[4]}"
  else
    continue
  fi

  # Only process GPU classes
  if [[ ! "$PCI_CLASS" =~ (VGA\ compatible\ controller|3D\ controller) ]]; then
    continue
  fi

  # Extract vendor name and ID
  VENDOR=$(sed -E 's/ \[[0-9A-Fa-f]{4}\]$//' <<<"$VENDOR_FIELD")
  VENDOR_ID=$(sed -E 's/.*\[([0-9A-Fa-f]{4})\]$/\1/' <<<"$VENDOR_FIELD")
  # Extract device name and ID
  DEVICE=$(sed -E 's/ \[[0-9A-Fa-f]{4}\]$//' <<<"$DEVICE_FIELD")
  DEVICE_ID=$(sed -E 's/.*\[([0-9A-Fa-f]{4})\]$/\1/' <<<"$DEVICE_FIELD")

  # Kernel driver
  DRV_PATH="/sys/bus/pci/devices/0000:$PCI_ADDR/driver"
  if [[ -L $DRV_PATH ]]; then
    DRIVER=$(basename "$(readlink "$DRV_PATH")")
  else
    DRIVER="unknown"
  fi

  # IOMMU group
  IOMMU=$(get_iommu_group "$PCI_ADDR")

  # SR-IOV counts
  read -r TOTALVFS NUMVFS < <(get_sriov_counts "$PCI_ADDR")

  # === full_passthrough usage ===
  FULL_USED_JSON="null"
  if (( TOTALVFS == 0 )); then
    raw="${pci_to_vm[$PCI_ADDR]:-}"
    FULL_USED_JSON=$(to_json_vm "$raw")
  fi

  # === vGPU (MDEV) instances ===
  VGPU_ARRAY="[]"
  MDEV_PATH="/sys/class/mdev_bus/0000:$PCI_ADDR"
  if [[ -d $MDEV_PATH ]]; then
    declare -a vlist=()
    for U in "$MDEV_PATH"/devices/*; do
      [[ -d $U ]] || continue
      MDEV_UUID=${U##*/}

      # Profile name from mdev_type/name
      TYPE_PATH="$(readlink -f "$U/mdev_type")"
      PROFILE_NAME=""
      if [[ -f "$TYPE_PATH/name" ]]; then
        PROFILE_NAME=$(<"$TYPE_PATH/name")
      fi

      # Available instances
      AVAILABLE_INSTANCES=0
      if [[ -f "$TYPE_PATH/available_instances" ]]; then
        AVAILABLE_INSTANCES=$(<"$TYPE_PATH/available_instances")
      fi

      # libvirt_address uses PF BDF
      DOMAIN="0x0000"
      BUS="0x${PCI_ADDR:0:2}"
      SLOT="0x${PCI_ADDR:3:2}"
      FUNC="0x${PCI_ADDR:6:1}"

      # Determine which VMs use this UUID
      raw="${mdev_to_vm[$MDEV_UUID]:-}"
      USED_JSON=$(to_json_vm "$raw")

      vlist+=( \
        "{\"mdev_uuid\":\"$MDEV_UUID\",\"profile_name\":$(json_escape "$PROFILE_NAME"),\"available_instances\":$AVAILABLE_INSTANCES,\"libvirt_address\":{"\
        "\"domain\":\"$DOMAIN\",\"bus\":\"$BUS\",\"slot\":\"$SLOT\",\"function\":\"$FUNC\"},\"used_by_vm\":$USED_JSON}" )
    done
    VGPU_ARRAY="[${vlist[*]}]"
  fi

  # === VF instances (SR-IOV / MIG) ===
  VF_ARRAY="[]"
  if (( TOTALVFS > 0 )); then
    declare -a flist=()
    for VF_LINK in /sys/bus/pci/devices/0000:"$PCI_ADDR"/virtfn*; do
      [[ -L $VF_LINK ]] || continue
      VF_PATH=$(readlink -f "$VF_LINK")
      VF_ADDR=${VF_PATH##*/}   # e.g. "0000:65:00.2"
      VF_BDF="${VF_ADDR:5}"   # "65:00.2"

      DOMAIN="0x0000"
      BUS="0x${VF_BDF:0:2}"
      SLOT="0x${VF_BDF:3:2}"
      FUNC="0x${VF_BDF:6:1}"

      # Determine vf_type and vf_profile
      VF_TYPE="sr-iov"
      VF_PROFILE=""
      if [[ $VENDOR_ID == "10de" && -f "/sys/bus/pci/devices/$VF_ADDR/device/mig_profile" ]]; then
        VF_TYPE="mig"
        VF_PROFILE=$(<"/sys/bus/pci/devices/$VF_ADDR/device/mig_profile")
      else
        if VF_LINE=$(lspci -nnm -s "$VF_BDF" 2>/dev/null); then
          if [[ $VF_LINE =~ \"([^\"]+)\"[[:space:]]\"([^\"]+)\"[[:space:]]\"([^\"]+)\"[[:space:]]\"([^\"]+)\" ]]; then
            VF_DEVICE_FIELD="${BASH_REMATCH[4]}"
            VF_PROFILE=$(sed -E 's/ \[[0-9A-Fa-f]{4}\]$//' <<<"$VF_DEVICE_FIELD")
          fi
        fi
      fi
      VF_PROFILE_JSON=$(json_escape "$VF_PROFILE")

      # Determine which VMs use this VF_BDF
      raw="${pci_to_vm[$VF_BDF]:-}"
      USED_JSON=$(to_json_vm "$raw")

      flist+=( \
        "{\"vf_pci_address\":\"$VF_BDF\",\"vf_profile\":$VF_PROFILE_JSON,\"libvirt_address\":{"\
        "\"domain\":\"$DOMAIN\",\"bus\":\"$BUS\",\"slot\":\"$SLOT\",\"function\":\"$FUNC\"},\"used_by_vm\":$USED_JSON}" )
    done
    VF_ARRAY="[${flist[*]}]"
  fi

  # === full_passthrough block ===
  FP_ENABLED=$(( TOTALVFS == 0 ))
  DOMAIN="0x0000"
  BUS="0x${PCI_ADDR:0:2}"
  SLOT="0x${PCI_ADDR:3:2}"
  FUNC="0x${PCI_ADDR:6:1}"

  # Emit JSON
  if $first_gpu; then
    first_gpu=false
  else
    echo ","
  fi

  cat <<JSON
    {
      "pci_address":$(json_escape "$PCI_ADDR"),
      "vendor_id":$(json_escape "$VENDOR_ID"),
      "device_id":$(json_escape "$DEVICE_ID"),
      "vendor":$(json_escape "$VENDOR"),
      "device":$(json_escape "$DEVICE"),
      "driver":$(json_escape "$DRIVER"),
      "pci_class":$(json_escape "$PCI_CLASS"),
      "iommu_group":$(json_escape "$IOMMU"),
      "sriov_totalvfs":$TOTALVFS,
      "sriov_numvfs":$NUMVFS,

      "full_passthrough": {
        "enabled":$FP_ENABLED,
        "libvirt_address": {
          "domain":$(json_escape "$DOMAIN"),
          "bus":$(json_escape "$BUS"),
          "slot":$(json_escape "$SLOT"),
          "function":$(json_escape "$FUNC")
        },
        "used_by_vm":$FULL_USED_JSON
      },

      "vgpu_instances":$VGPU_ARRAY,
      "vf_instances":$VF_ARRAY
    }
JSON

done

echo ""
echo "]}"
