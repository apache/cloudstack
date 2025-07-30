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
# Enumerate GPUs (NVIDIA, Intel, AMD) and output JSON for libvirt,
# including:
#   - PCI metadata (address, vendor/device IDs, driver, pci_class)
#   - IOMMU group
#   - PCI root (for PCIe topology grouping)
#   - NUMA node
#   - SR-IOV VF counts
#   - full_passthrough block (with VM usage)
#   - vGPU (MDEV) instances (fetching profile “name” and “max_instance” from description)
#   - VF (SR-IOV / MIG) instances (with VM usage)
#
# Uses `lspci -nnm` for GPU discovery and `virsh` to detect VM attachments.
# Compatible with Ubuntu (20.04+, 22.04+) and RHEL/CentOS (7/8), Bash ≥4.
#
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
#           "max_instances": 4,
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
#           "max_instances": 8,
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
#           "max_instances": 8,
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
#           "max_instances": 5,
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
#           "max_instances": 4,
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
#           "max_instances": 8,
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
	local str="$1"
	str=${str//\\/\\\\}
	str=${str//\"/\\\"}
	str=${str//
/\\n}
	str=${str//
/\\r}
	str=${str//	/\\t}
	printf '"%s"' "$str"
}

# Cache for nodedev XML data to avoid repeated virsh calls
declare -A nodedev_cache

# Get nodedev name for a PCI address (e.g. "00:02.0" -> "pci_0000_00_02_0")
get_nodedev_name() {
	local addr="$1"
	echo "pci_$(echo "$addr" | sed 's/[:.]/\_/g' | sed 's/^/0000_/')"
}

# Get cached nodedev XML for a PCI address
get_nodedev_xml() {
	local addr="$1"
	local nodedev_name
	nodedev_name=$(get_nodedev_name "$addr")

	if [[ -z "${nodedev_cache[$nodedev_name]:-}" ]]; then
		if nodedev_cache[$nodedev_name]=$(virsh nodedev-dumpxml "$nodedev_name" 2>/dev/null); then
			true # Cache populated successfully
		else
			nodedev_cache[$nodedev_name]="" # Cache empty result to avoid retries
		fi
	fi

	echo "${nodedev_cache[$nodedev_name]}"
}

# Given a PCI address (e.g. "00:02.0"), return its IOMMU group or "null"
get_iommu_group() {
	local addr="$1"
	local xml
	xml=$(get_nodedev_xml "$addr")
	local group
	group=$(echo "$xml" | xmlstarlet sel -t -v "//iommuGroup/@number" 2>/dev/null || true)
	echo "${group:-null}"
}

# Given a PCI address, output "TOTALVFS NUMVFS"
get_sriov_counts() {
	local addr="$1"
	local xml
	xml=$(get_nodedev_xml "$addr")

	local totalvfs=0
	local numvfs=0

	if [[ -n "$xml" ]]; then
		# Check for SR-IOV capability before parsing
		local cap_xml
		cap_xml=$(echo "$xml" | xmlstarlet sel -t -c "//capability[@type='virt_functions']" 2>/dev/null || true)

		if [[ -n "$cap_xml" ]]; then
			totalvfs=$(echo "$cap_xml" | xmlstarlet sel -t -v "/capability/@maxCount" 2>/dev/null || true)
			numvfs=$(echo "$cap_xml" | xmlstarlet sel -t -v "count(/capability/address)" 2>/dev/null || true)
		fi
	fi

	echo "${totalvfs:-0} ${numvfs:-0}"
}

# Given a PCI address, return its NUMA node (or -1 if none)
get_numa_node() {
	local addr="$1"
	local xml
	xml=$(get_nodedev_xml "$addr")
	local node
	node=$(echo "$xml" | xmlstarlet sel -t -v "//numa/@node" 2>/dev/null || true)
	echo "${node:--1}"
}

# Given a PCI address, return its PCI root (the top‐level bridge ID, e.g. "0000:00:03")
get_pci_root() {
	local addr="$1"
	local xml
	xml=$(get_nodedev_xml "$addr")

	if [[ -n "$xml" ]]; then
		# Extract the parent device from XML
		local parent
		parent=$(echo "$xml" | xmlstarlet sel -t -v "/device/parent" 2>/dev/null || true)
		if [[ -n "$parent" ]]; then
			# If parent is a PCI device, recursively find its root
			if [[ $parent =~ ^pci_0000_([0-9A-Fa-f]{2})_([0-9A-Fa-f]{2})_([0-9A-Fa-f])$ ]]; then
				local parent_addr="${BASH_REMATCH[1]}:${BASH_REMATCH[2]}.${BASH_REMATCH[3]}"
				get_pci_root "$parent_addr"
				return
			else
				# Parent is not PCI device, so current device is the root
				echo "0000:$addr"
				return
			fi
		fi
	fi

	# fallback
	echo "0000:$addr"
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

	# -- PCI hostdevs: use xmlstarlet to extract BDF for all PCI host devices --
	while read -r bus slot func; do
		[[ -n "$bus" && -n "$slot" && -n "$func" ]] || continue
		# Format to match lspci output (e.g., 01:00.0) by padding with zeros
		bus_fmt=$(printf "%02x" "0x$bus")
		slot_fmt=$(printf "%02x" "0x$slot")
		func_fmt=$(printf "%x" "0x$func")
		BDF="$bus_fmt:$slot_fmt.$func_fmt"
		pci_to_vm["$BDF"]="$VM"
	done < <(echo "$xml" | xmlstarlet sel -T -t -m "//hostdev[@type='pci']/source/address" \
		-v "substring-after(@bus, '0x')" -o " " \
		-v "substring-after(@slot, '0x')" -o " " \
		-v "substring-after(@function, '0x')" -n 2>/dev/null || true)

	# -- MDEV hostdevs: use xmlstarlet to extract UUIDs --
	while IFS= read -r UUID; do
		[[ -n "$UUID" ]] && mdev_to_vm["$UUID"]="$VM"
	done < <(echo "$xml" | xmlstarlet sel -T -t -m "//hostdev[@type='mdev']/source/address" -v "@uuid" -n 2>/dev/null || true)
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

# Parse a "description" file for GPU properties and set global variables
# Expects one argument: the path to the description file
parse_and_add_gpu_properties() {
    local desc_file="$1"
    # Reset properties to null defaults
    MAX_INSTANCES="null"
    VIDEO_RAM="null"
    MAX_HEADS="null"
    MAX_RESOLUTION_X="null"
    MAX_RESOLUTION_Y="null"

    if [[ -f "$desc_file" ]]; then
        local desc
        desc=$(<"$desc_file")
        if [[ $desc =~ max_instance=([0-9]+) ]]; then
            MAX_INSTANCES="${BASH_REMATCH[1]}"
        fi
        if [[ $desc =~ framebuffer=([0-9]+)M? ]]; then # Support with or without 'M' suffix
            VIDEO_RAM="${BASH_REMATCH[1]}"
        fi
        if [[ $desc =~ num_heads=([0-9]+) ]]; then
            MAX_HEADS="${BASH_REMATCH[1]}"
        fi
        if [[ $desc =~ max_resolution=([0-9]+)x([0-9]+) ]]; then
            MAX_RESOLUTION_X="${BASH_REMATCH[1]}"
            MAX_RESOLUTION_Y="${BASH_REMATCH[2]}"
        fi
    fi
}

# Finds and formats mdev instances for a given PCI device (PF or VF).
# Appends JSON strings for each found mdev instance to the global 'vlist' array.
# Arguments:
#   $1: mdev_base_path (e.g., /sys/bus/pci/devices/.../mdev_supported_types)
#   $2: bdf (e.g., 01:00.0)
process_mdev_instances() {
	local mdev_base_path="$1"
	local bdf="$2"

	if [[ ! -d "$mdev_base_path" ]]; then
		return
	fi

	for PROF_DIR in "$mdev_base_path"/*; do
		[[ -d "$PROF_DIR" ]] || continue

		local PROFILE_NAME
		if [[ -f "$PROF_DIR/name" ]]; then
			PROFILE_NAME=$(<"$PROF_DIR/name")
		else
			PROFILE_NAME=$(basename "$PROF_DIR")
		fi

		parse_and_add_gpu_properties "$PROF_DIR/description"

		local DEVICE_DIR="$PROF_DIR/devices"
		if [[ -d "$DEVICE_DIR" ]]; then
			for UDIR in "$DEVICE_DIR"/*; do
				[[ -d "$UDIR" ]] || continue
				local MDEV_UUID
				MDEV_UUID=$(basename "$UDIR")

				local DOMAIN="0x0000"
				local BUS="0x${bdf:0:2}"
				local SLOT="0x${bdf:3:2}"
				local FUNC="0x${bdf:6:1}"

				local raw
				raw="${mdev_to_vm[$MDEV_UUID]:-}"
				local USED_JSON
				USED_JSON=$(to_json_vm "$raw")

				vlist+=(
					"{\"mdev_uuid\":\"$MDEV_UUID\",\"profile_name\":$(json_escape "$PROFILE_NAME"),\"max_instances\":$MAX_INSTANCES,\"video_ram\":$VIDEO_RAM,\"max_heads\":$MAX_HEADS,\"max_resolution_x\":$MAX_RESOLUTION_X,\"max_resolution_y\":$MAX_RESOLUTION_Y,\"libvirt_address\":{\"domain\":\"$DOMAIN\",\"bus\":\"$BUS\",\"slot\":\"$SLOT\",\"function\":\"$FUNC\"},\"used_by_vm\":$USED_JSON}")
			done
		fi
	done
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

	# If this is a VF, skip it. It will be processed under its PF.
	if [[ -e "/sys/bus/pci/devices/0000:$PCI_ADDR/physfn" ]]; then
		continue
	fi

	# Only process GPU classes (3D controller)
	if [[ ! "$PCI_CLASS" =~ (3D\ controller) ]]; then
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

	# PCI root (to group GPUs under same PCIe switch/root complex)
	PCI_ROOT=$(get_pci_root "$PCI_ADDR")

	# NUMA node
	NUMA_NODE=$(get_numa_node "$PCI_ADDR")

	# SR-IOV counts
	read -r TOTALVFS NUMVFS < <(get_sriov_counts "$PCI_ADDR")

	# Get Physical GPU properties from its own description file, if available
	PF_DESC_PATH="/sys/bus/pci/devices/0000:$PCI_ADDR/description"
	parse_and_add_gpu_properties "$PF_DESC_PATH"
	# Save physical function's properties before they are overwritten by vGPU/VF processing
	PF_MAX_INSTANCES=$MAX_INSTANCES
	PF_VIDEO_RAM=$VIDEO_RAM
	PF_MAX_HEADS=$MAX_HEADS
	PF_MAX_RESOLUTION_X=$MAX_RESOLUTION_X
	PF_MAX_RESOLUTION_Y=$MAX_RESOLUTION_Y

	# === full_passthrough usage ===
	raw="${pci_to_vm[$PCI_ADDR]:-}"
	FULL_USED_JSON=$(to_json_vm "$raw")

	# === vGPU (MDEV) instances ===
	VGPU_ARRAY="[]"
	declare -a vlist=()
	# Process mdev on the Physical Function
	MDEV_BASE="/sys/bus/pci/devices/0000:$PCI_ADDR/mdev_supported_types"
	process_mdev_instances "$MDEV_BASE" "$PCI_ADDR"

	# === VF instances (SR-IOV / MIG) ===
	VF_ARRAY="[]"
	declare -a flist=()
	if ((TOTALVFS > 0)); then
		for VF_LINK in /sys/bus/pci/devices/0000:"$PCI_ADDR"/virtfn*; do
			[[ -L $VF_LINK ]] || continue
			VF_PATH=$(readlink -f "$VF_LINK")
			VF_ADDR=${VF_PATH##*/} # e.g. "0000:65:00.2"
			VF_BDF="${VF_ADDR:5}"  # "65:00.2"

			# For NVIDIA SR-IOV, check for vGPU (mdev) on the VF itself
			if [[ "$VENDOR_ID" == "10de" ]]; then
				VF_MDEV_BASE="$VF_PATH/mdev_supported_types"
				process_mdev_instances "$VF_MDEV_BASE" "$VF_BDF"
			fi

			DOMAIN="0x0000"
			BUS="0x${VF_BDF:0:2}"
			SLOT="0x${VF_BDF:3:2}"
			FUNC="0x${VF_BDF:6:1}"

			# Determine vf_profile
			VF_PROFILE=""
			if VF_LINE=$(lspci -nnm -s "$VF_BDF" 2>/dev/null); then
				if [[ $VF_LINE =~ \"([^\"]+)\"[[:space:]]\"([^\"]+)\"[[:space:]]\"([^\"]+)\"[[:space:]]\"([^\"]+)\" ]]; then
					VF_DEVICE_FIELD="${BASH_REMATCH[4]}"
					VF_PROFILE=$(sed -E 's/ \[[0-9A-Fa-f]{4}\]$//' <<<"$VF_DEVICE_FIELD")
				fi
			fi
			VF_PROFILE_JSON=$(json_escape "$VF_PROFILE")

			# Determine which VM uses this VF_BDF
			raw="${pci_to_vm[$VF_BDF]:-}"
			USED_JSON=$(to_json_vm "$raw")

			flist+=(
				"{\"vf_pci_address\":\"$VF_BDF\",\"vf_profile\":$VF_PROFILE_JSON,\"libvirt_address\":{\"domain\":\"$DOMAIN\",\"bus\":\"$BUS\",\"slot\":\"$SLOT\",\"function\":\"$FUNC\"},\"used_by_vm\":$USED_JSON}")
		done
		if [ ${#flist[@]} -gt 0 ]; then
			VF_ARRAY="[$(
				IFS=,
				echo "${flist[*]}"
			)]"
		fi
	fi

	# Consolidate all vGPU instances (from PF and VFs)
	if [ ${#vlist[@]} -gt 0 ]; then
		VGPU_ARRAY="[$(
			IFS=,
			echo "${vlist[*]}"
		)]"
	fi

	# === full_passthrough block ===
	# If vgpu_instances and vf_instances are empty, we can assume full passthrough
	FP_ENABLED=0
	if [[ ${#vlist[@]} -eq 0 && ${#flist[@]} -eq 0 ]]; then
		FP_ENABLED=1
	fi
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
      "pci_root":$(json_escape "$PCI_ROOT"),
      "numa_node":$NUMA_NODE,
      "sriov_totalvfs":$TOTALVFS,
      "sriov_numvfs":$NUMVFS,
      "max_instances":$PF_MAX_INSTANCES,
      "video_ram":$PF_VIDEO_RAM,
      "max_heads":$PF_MAX_HEADS,
      "max_resolution_x":$PF_MAX_RESOLUTION_X,
      "max_resolution_y":$PF_MAX_RESOLUTION_Y,

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
