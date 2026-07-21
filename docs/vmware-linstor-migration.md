<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Linstor primary storage as a destination for VMware-to-KVM migration

This document describes the Linstor-destination portion of the VMware-to-KVM
migration work, in enough detail for LINSTOR/DRBD maintainers to review the
design choices and advise on Linstor-side behavior.

## Summary

This change lets Linstor (DRBD) primary storage be the **destination** for
VMware-to-KVM instance imports, across all three migration modes CloudStack
offers for Ceph/RBD:

- **Cold, staged** — convert with virt-v2v to a temporary NFS location, then
  copy the finalized disks into Linstor.
- **Cold, direct (VDDK)** — read each disk over nbdkit/VDDK straight into the
  DRBD device and finalize it in place, with no NFS staging.
- **Warm (CBT)** — replicate the running VM with an initial full sync plus
  incremental changed-block cycles into the DRBD device, then finalize in
  place at cutover.

Before this change Linstor was blocked everywhere in the import/conversion
path by NFS-only (and, more recently, RBD-only) assumptions. The work
generalizes those code paths rather than special-casing Linstor: a new
`RAW_BLOCK_DEVICE` target type covers "write RAW into a host-local block
device provided by the storage adaptor", which also lays the groundwork for
other block backends (PowerFlex, FiberChannel, StorPool) later.

## Background and motivation

CloudStack can import VMware VMs onto KVM by converting them with virt-v2v
(optionally reading disks over the VMware VDDK). The converted disks have to
land on KVM primary storage. That path historically supported only
filesystem-style pools (NFS/Filesystem/SharedMountPoint); the companion
commits in this series add Ceph/RBD as a destination and a full VMware
**CBT warm-migration** framework. Linstor — a very common CloudStack block
backend — was still unsupported as a conversion destination.

Linstor differs from RBD in ways that shaped the design:

- A Linstor volume is a **local block device** (`/dev/drbd/by-res/<res>/0`),
  which is the universal abstraction the whole toolchain (qemu-img, qemu-io,
  virt-v2v-in-place) already consumes — so finalization is actually *simpler*
  than RBD (no qemu-nbd credential bridge needed; a plain
  `<disk type='block'>` works).
- Unlike RBD, `qemu-img convert` **cannot create** the device — the resource
  must be pre-created at the source capacity through the storage adaptor,
  then written with `qemu-img convert -n` (no-create).
- LINSTOR resource names are capped at ~48 characters and cannot be renamed,
  so the RBD-style naming carrying a full migration UUID does not fit; short,
  deterministic names are used and imported volumes keep the name as their
  recorded volume path.

## What this change does

### New generic block-device target type
`VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE`, alongside the existing
`QCOW2_FILE` and `RBD_RAW`. Linstor pools classify to it and require in-place
finalization (the qcow2 fallback cannot write to a device).

### Server-side (orchestration)
- Linstor added to the staged-conversion destination pool types, the direct
  VDDK-convert allow-list, and the CBT-compatible pool types, for both
  explicit and implicit destination-pool selection.
- Conversion/import host selection validates that the host is a LINSTOR
  satellite connected to the pool (in addition to the VDDK / in-place
  virt-v2v capability checks), for auto-selection and explicit host IDs.
- `CheckConvertInstanceCommand` carries a new in-place-finalization capability
  check so unsupported hosts fail fast with a clear message.
- The powered-off, non-cloned source requirement (already enforced for direct
  RBD) now applies to any direct block-storage import.

### KVM agent-side (data plane)
- **Staged**: the converted-disk move already dispatches per pool type to
  `LinstorStorageAdaptor.copyPhysicalDisk` (qemu-img into the DRBD device);
  converted-disk metadata for Linstor reports the pool UUID and volume name
  instead of parsing an NFS mount.
- **Direct VDDK**: pre-creates each resource at source capacity via the
  adaptor, then `nbdkit vddk --run 'qemu-img convert -n ... <device>'`, and
  finalizes with `virt-v2v-in-place` fed a `<disk type='block'>` domain XML.
- **Warm CBT**: initial sync pre-creates the device and `qemu-img convert -n`
  into it; delta cycles patch changed extents; cutover finalizes in place
  with block-device XML (no qemu-nbd bridges). Cleanup deletes only
  marker-guarded volumes.
- `LinstorStorageAdaptor.listPhysicalDisks` is implemented (resource
  definitions filtered by the pool's resource group), replacing the previous
  `UnsupportedOperationException`, so the forced-conversion import short
  circuit works for Linstor.

### Performance: nbdcopy for full-disk copies to block devices
The cold direct-VDDK import and the CBT initial full sync copy an entire disk
from the nbdkit/VDDK source into the target. For a local raw block-device
target (Linstor/DRBD), this now uses `nbdcopy` (libnbd) when the host has it,
falling back to `qemu-img convert`. nbdcopy keeps many requests in flight
(it is what virt-v2v itself uses for this step) and is typically faster than a
single-connection `qemu-img convert`. Availability is probed locally on the
agent; it is a pure optimization with automatic fallback, so no server-side
gating is needed. Only block-device targets are affected — the RBD URI target
and qcow2 file targets keep qemu-img convert (nbdcopy is raw-only and cannot
address an rbd: URI without a bridge). On a 16 GiB disk over network NBD
transport it measured ~20% faster than qemu-img convert in the lab, and the
result was byte-identical to a full VDDK read of the source.

### Performance: direct CBT delta copy for block devices
Previously a CBT delta copied each changed extent in two hops — qemu-img the
nbd source window into a temporary raw file, then qemu-io write that file into
the target. For a raw block-device target that doubles local I/O and needs
scratch space. This change streams each changed extent from the nbd source
window straight into the same device window in a single
`qemu-img convert -n -S 0 --image-opts --target-image-opts`. `-S 0` disables
zero/sparse skipping so blocks the source cleared to zero are actually
overwritten in the target — a full copy can skip zeros because the target
starts zeroed, but a delta into an already-populated device cannot. The
qcow2-file and RBD paths are unchanged.

## Migration support matrix (Linstor destination)

| Mode | Trigger | Source VM state | Data movement | Guest downtime |
|---|---|---|---|---|
| Cold, staged | `importVm` (no `forceconverttopool`) | Running (auto-cloned on vSphere) or stopped | virt-v2v -> qcow2 on NFS temp -> qemu-img copy to DRBD device | Offline copy (running source undisturbed via clone) |
| Cold, direct VDDK | `importVm usevddk=true forceconverttopool=true` -> Linstor pool | Stopped only (non-cloned) | nbdkit/VDDK -> qemu-img convert -n into DRBD device -> virt-v2v-in-place | Whole migration (VM is off) |
| Warm, CBT | `startVmwareCbtMigration` -> sync cycles -> `cutoverVmwareCbtMigration` | Running | Initial full sync + incremental CBT deltas into DRBD device; final delta + in-place finalize at cutover | Minimal (only the final cutover delta) |

Notes:
- Warm CBT does **not** power off the source automatically. The operator
  gracefully shuts down the VMware VM, then triggers cutover; the code
  rejects cutover on a running VM. This is deliberate — never auto-kill a
  production VM.
- Direct VDDK requires a powered-off, non-cloned source.
- Imported Linstor volumes are recorded QCOW2 in the DB (Linstor convention;
  RAW on the device).

## How to use

Cold, direct VDDK into Linstor (source powered off):

    import vm zoneid=<zone> clusterid=<kvm-cluster> \
      importsource=vmware hypervisor=KVM \
      vcenter=<vc> datacentername=<dc> username=<u> password=<p> \
      clustername=<vmware-cluster> host=<esxi-ip> \
      name=<source-vm> displayname=<new-name> serviceofferingid=<so> \
      usevddk=true forceconverttopool=true \
      convertinstancepoolid=<linstor-pool> \
      nicnetworklist[0].nic="Network adapter 1" nicnetworklist[0].network=<net> \
      datadiskofferinglist[0].disk=<data-disk-id> datadiskofferinglist[0].diskOffering=<do>

Warm CBT into Linstor: `startVmwareCbtMigration` with the Linstor pool as
`storagepoolid`, let the delta cycles run, power off the source, then
`cutoverVmwareCbtMigration`.

(The web import wizard fills the NIC/disk mappings from the discovered VM, so
the UI is the easiest way to drive it.)

## Host prerequisites

For any Linstor destination the conversion host must be a LINSTOR satellite
connected to the pool (its `hostname` must match its LINSTOR node name).
Direct-VDDK and warm-CBT additionally need VDDK, an nbdkit VDDK plugin, and
in-place virt-v2v (`virt-v2v-in-place` or `virt-v2v --in-place`). No extra
qemu block-driver is needed (writes go to a local device). Prefer a diskful
satellite as the conversion host so writes are local rather than traversing
the DRBD replication network.

## Backwards compatibility

- No behavior change for existing NFS or RBD destinations; Linstor is added
  to the same gate lists and dispatch branches.
- The CBT-delta optimization only affects `RAW_BLOCK_DEVICE` targets; qcow2
  and RBD keep their existing code paths.
- One host-capability check flag was added to `CheckConvertInstanceCommand`
  (old constructor preserved, null-tolerant).

## Testing

- **Unit tests**: full server + KVM-plugin suites pass, including new
  Linstor-specific tests across the gate lists, host selection, the
  convert/import/CBT wrappers, `listPhysicalDisks`, and the block-device
  delta script.
- **Data plane, real 3-node LINSTOR/DRBD lab, real VMware source**:
  - Cold direct VDDK: a real VM's disks read over VDDK into DRBD devices +
    in-place finalized; `qemu-img compare` against the source = identical.
  - Warm CBT: baseline + delta + power-off + final delta + finalize;
    integrity verified (file checksum inside the migrated volume matched the
    source, and `qemu-img compare` of a delta-applied device against a full
    read of the snapshot = identical).
  - CBT-delta optimization: 144 real changed extents applied via the new
    direct-copy script; `qemu-img compare` = images identical.
- **Full end-to-end through CloudStack**: `importVm` (usevddk +
  forceconverttopool) against a real vCenter produced a Stopped CloudStack VM
  with its root and data disks as Linstor volumes on the pool, each backed by
  a DRBD resource UpToDate on all three nodes.

## Known limitations / possible follow-ups

- Full-disk copies into block devices use nbdcopy when present (see above).
  RBD URI and qcow2 file targets still use qemu-img convert; extending
  nbdcopy to RBD would need a qemu-nbd bridge (nbdcopy is raw-device/file
  only), which is a possible follow-up.
- The conversion host is usually not ESXi-adjacent, so VDDK typically uses
  network (NBD/NBDSSL) transport rather than the faster HotAdd; largely
  inherent to the KVM conversion-host model.
- A diskful conversion host is recommended but not enforced; a diskless
  satellite makes every write traverse the replication network.
- The staged path keeps its extra NFS hop (needed for running-VM clones and
  hosts without in-place support).

## Relationship to the RBD series

The Linstor support is layered on the VMware-to-KVM RBD series in this same
change set (direct VDDK-to-RBD imports, the CBT warm-migration framework,
importVm RBD adoption, and the consolidated host-capability probe names).
The Linstor-specific commits, in order:

- KVM: allow staged VMware instance imports into Linstor primary storage
- KVM: support direct VDDK VMware imports into Linstor primary storage
- VMware CBT warm migration to KVM with Linstor destination storage
- Document Linstor destination support for VMware-to-KVM imports
- Drop unsupported -O option from virt-v2v-in-place invocations
- KVM: stream VMware CBT block-device deltas directly, without a temp file
- KVM: use nbdcopy for full-disk copies into Linstor block devices when available
