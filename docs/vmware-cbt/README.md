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

# VMware CBT Migration Implementation Notes

## Introduction

This document describes the VMware Changed Block Tracking (CBT) migration design
and implementation for Apache CloudStack 4.24.0.0. It is written as source material
for an Apache CloudStack CWIKI architecture page and should be treated as an
implementation guide, not only as a design sketch.

The feature provides a warm VMware-to-KVM migration flow. CloudStack creates a
KVM-side source-equivalent disk replica from a VMware VDDK snapshot, keeps that
replica current with one or more VMware CBT delta cycles, requires the operator
to power off the VMware source VM, runs one final CBT delta cycle, finalizes the
replica with `virt-v2v`, and imports the finalized disk(s) as a regular
CloudStack KVM VM.

The essential architectural point is that VMware CBT migration is a two-phase
disk strategy:

- Phase 1 keeps a KVM-side source-equivalent replica current using VDDK and
  VMware CBT.
- Phase 2 finalizes the fully synchronized replica with `virt-v2v` only after
  the source VM is powered off.

This gives CloudStack warm migration behavior without trying to apply VMware
changed blocks to a disk that has already been transformed for KVM, and without
requiring a second full VMware read at cutover time.

## Problem Statement

The existing VMware import paths are useful for one-time imports, but they do
not provide a warm migration loop where the target disk is kept current while
the source VM continues to run on VMware. Large VMs therefore either require a
longer downtime window or repeated full-copy style attempts.

VMware CBT provides the changed-block metadata needed for a replication-style
workflow, but the design must avoid a common trap: once `virt-v2v` modifies a
disk for KVM guest bootability, VMware CBT byte ranges can no longer be safely
applied to that transformed disk. The implementation must therefore keep the
replication target source-equivalent until final cutover, then run guest
conversion only after the final delta is applied.

## Goals

- Provide an operator-driven warm migration flow from VMware to CloudStack KVM.
- Support both existing registered vCenter records and external vCenter details
  supplied during import.
- Run the initial full copy once through VDDK and `qemu-img`.
- Apply subsequent VMware CBT delta cycles to the KVM-side replica.
- Make long-running start, sync, and cutover operations asynchronous.
- Keep migration progress visible through list APIs and the UI migration table.
- Validate destination storage, host capabilities, and compute offering sizing
  before expensive work starts.
- Persist external vCenter credentials only through CloudStack encrypted DAO
  fields and never expose passwords in API responses.
- Reuse the existing CloudStack converted-VM import path after cutover
  finalization.

## Scope

Current implementation scope:

- Target hypervisor: KVM.
- Source hypervisor: VMware through VDDK and VMware CBT.
- Admin API surface for preflight, start, list, sync, cutover, cancel, and
  delete.
- Async API execution for long-running start, sync, and cutover operations.
- Management-server state model for migrations, disks, and delta cycles.
- External vCenter credential persistence for long-lived UI sessions, encrypted
  with CloudStack DAO encryption.
- KVM agent wrappers for initial VDDK full sync, delta block copy, cutover
  finalization, and cleanup.
- Filesystem-like primary storage target support using QCOW2 files.
- Ceph/RBD primary storage target support using raw RBD images.
- Non-in-place `virt-v2v` fallback for QCOW2 file targets, gated by global
  configuration.
- Early compute-offering validation against discovered VMware VM CPU and memory.
- Dynamic/custom service offering support through the same `details` keys used
  by the existing OVF/VDDK import flow.
- UI integration in the Import/Export Instances area, including job polling and
  table auto-refresh while CBT migrations are active.

## Non-Goals

Not implemented or intentionally blocked:

- Non-in-place `virt-v2v` fallback for Ceph/RBD targets. RBD targets require
  in-place finalization because fallback produces local QCOW2 files.
- Automatic source VM shutdown during cutover.
- Continuous byte-level progress from `qemu-img convert`. The UI currently
  receives state transitions, disk target paths, delta cycle byte counts, dirty
  rates, and cycle descriptions through the list API.
- Applying CBT deltas to a guest-converted disk.
- Replacing the existing OVF/VDDK one-time import flows.

## Key Principles

- CloudStack does not power off the VMware source VM automatically. The
  operator must shut it down before final cutover.
- CloudStack does not store vCenter passwords in clear text.
- Initial sync and delta sync keep the target disk source-equivalent.
- `virt-v2v` runs only at finalization, after the final CBT delta cycle.
- The final `virt-v2v` work is local to the KVM-side replica and does not
  perform a second full VMware read.
- The detailed status source is `listVmwareCbtMigrations`, not only the async
  job result.
- Failed final import after successful finalization is retryable through the
  `ReadyForImport` state.

## High-Level Architecture

The implementation has four main layers:

- CloudStack management-server APIs and orchestration.
- CloudStack database state for migration sessions, source disks, and delta
  cycles.
- KVM agent commands that perform VDDK reads, QCOW2 or raw RBD writes,
  `virt-v2v` finalization, and cleanup.
- UI workflow in the Import/Export Instances area.

Management-server API and orchestration:

- `StartVmwareCbtMigrationCmd`
- `CheckVmwareCbtMigrationPrerequisitesCmd`
- `ListVmwareCbtMigrationsCmd`
- `SyncVmwareCbtMigrationCmd`
- `CutoverVmwareCbtMigrationCmd`
- `CancelVmwareCbtMigrationCmd`
- `DeleteVmwareCbtMigrationCmd`
- `VmwareCbtMigrationManagerImpl`
- `VmwareCbtStorageTarget`
- `VmwareCbtMigrationCutoverPolicy`

Persistence:

- `cloud.vmware_cbt_migration`
- `cloud.vmware_cbt_migration_disk`
- `cloud.vmware_cbt_migration_cycle`

KVM agent wrappers:

- `LibvirtVmwareCbtPrepareCommandWrapper`
- `LibvirtVmwareCbtSyncCommandWrapper`
- `LibvirtVmwareCbtCutoverCommandWrapper`
- `LibvirtVmwareCbtCleanupCommandWrapper`

UI:

- `ui/src/views/tools/ImportUnmanagedInstance.vue`
- `ui/src/views/tools/ManageInstances.vue`
- `ui/src/views/tools/VmwareCbtMigrations.vue`

## Architecture Diagrams

The diagrams below are intentionally plain text so they can be pasted directly
into Apache CWIKI source and remain editable.

### End-To-End CBT Flow

```text
VMware source VM
     |
     | VMware snapshot + CBT metadata
     v
VDDK / nbdkit-vddk on KVM conversion host
     |
     | Initial baseline: full logical disk read
     v
CloudStack primary storage
     |
     | Source-equivalent replica
     |   - qcow2 file on filesystem-like storage
     |   - raw RBD image on Ceph/RBD storage
     v
Delta sync cycles
     |
     | CBT changed ranges only
     v
Operator powers off source VM
     |
     | Cutover: final delta + virt-v2v finalization
     v
CloudStack KVM VM
```

### Initial Sync Transfer Behavior

VMware CBT initial sync is not guest-filesystem-used-space-oriented. CloudStack
does not inspect the guest filesystem to decide which files are in use; it reads
the disk image exposed by VDDK/NBD and writes the target replica.

The amount of data transferred depends on what VDDK exposes for the source disk
and datastore. In lab testing, an NFS-backed VMware datastore exposed the initial
baseline as one fully allocated logical range, so a mostly empty disk still
transferred close to its full capacity. A VMFS6 datastore, after guest free-space
reclamation, exposed sparse `hole,zero` extents through the same VDDK/NBD path,
so the transfer was lower while the resulting target remained thin/sparse on
the destination backend.

```text
Example VMware disk capacity: 5 GiB
Guest-used data:              near 0 GiB
Datastore/path behavior:      datastore-dependent

During initial sync:
  VMware/VDDK exposes allocation/extents to NBD.
  CloudStack reads the ranges that qemu sees through that NBD export.
  qemu-img writes a qcow2 target and can preserve thin/sparse output.

Observed NFS-style outcome:   approximately 5 GiB transfer
Observed VMFS6/reclaim case:  sparse data/hole map, lower transfer
Target qcow2 file:            sparse/thin when zero/hole ranges are preserved
```

Example VMFS6/reclaimed extent probe:

```text
export-size: 5242880000 (5000M)
         0     4194304    0  data
   4194304     1048576    3  hole,zero
   5242880     3145728    0  data
   8388608     1048576    3  hole,zero
   9437184     1048576    0  data
  10485760     7340032    3  hole,zero
```

### Linked Clone And Full Clone Baselines

The initial baseline reads the logical disk presented by VDDK. Linked clone vs
full clone is not enough by itself to predict transfer volume. The decisive
question is whether VDDK/NBD exposes trustworthy allocation and zero extents for
the source datastore and disk chain.

```text
Linked clone source
  child delta VMDK
       |
       v
  parent/base VMDK chain
       |
       v
  VDDK presents one logical disk

Full clone source
  single/full VMDK
       |
       v
  VDDK presents one logical disk

CBT initial sync:
  both are read as the full logical disk capacity.
  The target qcow2 can still be sparse/thin after the write.
```

### Delta Sync And Cutover Eligibility

```text
Initial sync completed
        |
        v
Delta cycle 1 -> changed bytes / dirty rate measured
        |
        v
Delta cycle 2 -> quiet-cycle counters updated
        |
        v
Repeat until one boundary is met:

  min cycles satisfied
  AND quiet bytes threshold satisfied
  AND quiet dirty-rate threshold satisfied
  AND quiet cycle count satisfied

OR

  max cycle count reached

        |
        v
Migration becomes ReadyForCutover
```

### Cutover Finalization Paths

```text
Final CBT delta sync
      |
      v
Check selected conversion host capability
      |
      +--> virt-v2v-in-place available
      |        |
      |        v
      |   run virt-v2v-in-place finalization
      |
      +--> virt-v2v supports --in-place
      |        |
      |        v
      |   run virt-v2v --in-place finalization
      |
      +--> non-in-place fallback explicitly allowed
               |
               v
          run regular virt-v2v -o local fallback finalization

Result:
  finalized qcow2 moved to primary storage root
  VM imported with VirtIO root disk controller
```

### Final Disk Placement

```text
During sync:
  /mnt/<pool-uuid>/cloudstack-cbt/<migration-uuid>/...

After finalization/import:
  /mnt/<pool-uuid>/<generated-volume-uuid>.qcow2

CloudStack DB target path:
  updated to the final primary-storage-root location
```

## End-To-End Flow

### 1. Preflight

`checkVmwareCbtMigrationPrerequisites` validates the source and destination
before a long-running job is started. The command is synchronous and returns a
structured preflight response with pass, warn, and fail findings.

The preflight checks include:

- Destination zone and KVM cluster lookup.
- Destination primary storage lookup or implicit pool selection.
- Storage target classification.
- KVM conversion host selection.
- Host VMware CBT capability checks.
- Host in-place finalization capability checks.
- Host qemu RBD capability checks and an active temporary RBD create/write/read/delete
  probe when the selected primary storage is Ceph/RBD.
- vCenter source resolution, either by `existingvcenterid` or external
  vCenter fields.
- Source VM discovery.
- Source VM CBT support.
- Source VM CBT enabled state.
- Source VM consolidation-needed state.
- Existing VMware snapshot count.
- Source disk discovery.
- Windows guest conversion dependency check on the selected KVM conversion host
  when the source VM is a Windows guest.
- Optional compute offering sizing validation.

The preflight response includes, among other fields:

- `ready`
- `storagewritertype`
- `storagewritersupported`
- `storagerequiresinplacefinalization`
- `convertinstancehostinplacefinalizationsupported`
- `noninplacefinalizationfallbackallowed`
- `noninplacefinalizationfallbacksupported`
- `sourcecpunumber`
- `sourcecpuspeed`
- `sourcememory`
- `sourceguestosid`
- `sourceguestos`
- `disk`
- `finding`

If CBT is supported but not enabled, preflight reports a warning. The start
path can enable CBT before creating the baseline snapshot.

The source guest OS fields are populated from the VMware inventory and are used
to decide whether Windows-specific conversion prerequisites apply. They do not
change the migration mode or disk-copy behavior by themselves.

### Guest OS Mapping And Source OS Labels

VMware exposes more than one guest OS signal. The VM's configured guest OS
option can differ from the runtime guest OS label reported by VMware Tools. For
example, a powered-on VM can show `Ubuntu Linux (64-bit)` in the vSphere summary
while VM Options still show `Other (64-bit)`. CloudStack discovery and preflight
therefore treat source guest OS values as inventory facts rather than as a safe
automatic target OS selection.

The target CloudStack guest OS is the optional `osid` supplied to
`startVmwareCbtMigration` and used later by the shared KVM import path. Operators
should verify this value before starting migration, especially for generic VMware
guest IDs such as `ubuntu64Guest`, which can represent multiple Ubuntu releases.
If the desired target guest OS is not selectable, the corresponding
`guest_os_hypervisor` mapping must exist for the source hypervisor version.

### 2. Start And Initial Full Sync

`startVmwareCbtMigration` is async. It creates the migration record, persists
source disk rows, creates a baseline VMware snapshot, prepares target paths, and
dispatches the initial full sync to the selected KVM host.

For Windows source VMs, the start path repeats the same conversion dependency
check reported by preflight before the baseline snapshot and initial sync are
started. This prevents a long CBT copy from producing a disk that cannot be made
bootable because the selected KVM conversion host is missing `virtio-win`.

State transitions:

- `Created`
- `InitialSync`
- `Replicating` when the initial VDDK full sync completes successfully
- `Failed` if any initial copy or metadata step fails

Important implementation details:

- For filesystem-like storage, target paths are prepared before dispatch and
  are shown in API/UI while the sync is running.
- Default target path pattern with explicit primary storage:
  `/mnt/<storage-pool-uuid>/cloudstack-cbt/<migration-uuid>/<disk>.qcow2`
- Default target path pattern without explicit pool:
  `/var/lib/libvirt/images/cloudstack-cbt/<migration-uuid>/<disk>.qcow2`
- The agent writes the vCenter password to a temporary password file and passes
  it to `nbdkit` as `password=+<file>`.
- After successful copy, management refreshes and records the baseline disk
  `changeId` values from the VMware snapshot.
- The baseline snapshot is removed after the initial sync attempt when possible.

### 3. Delta Synchronization

`syncVmwareCbtMigration` is async and serialized by migration ID through
`BaseAsyncCmd.migrationSyncObject`.

Allowed migration states:

- `Replicating`
- `ReadyForCutover`

Delta cycle sequence:

1. Create a new `vmware_cbt_migration_cycle` row.
2. Create a VMware snapshot for the cycle.
3. Query VMware CBT changed disk areas from the previous per-disk `changeId`.
4. Persist cycle state as `QueryingChangedAreas`, then `CopyingChangedBlocks`.
5. Send changed block ranges to the KVM agent.
6. Agent copies only those ranges from the VDDK NBD source into the target
   replica.
7. Management records changed bytes, duration, dirty rate, and the next
   per-disk `changeId`.
8. The cycle snapshot is removed when possible.
9. The cutover policy decides whether to remain in `Replicating` or move to
   `ReadyForCutover`.

The cycle response exposes:

- `cyclenumber`
- `snapshotmor`
- `changedbytes`
- `dirtyrate`
- `duration`
- `state`
- `description`
- `created`
- `lastupdated`

Delta sync progress is represented as state and cycle metadata rather than a
streamed progress bar. While a cycle is running, the row description can show
what the manager knows, for example that changed areas are being queried or
that a number of ranges has been dispatched. After the agent returns, the cycle
shows copied bytes, dirty rate, duration, and a textual summary.

### 4. Cutover

`cutoverVmwareCbtMigration` is async and serialized by migration ID.

Allowed migration states:

- `ReadyForCutover`
- `ReadyForImport`

Cutover requires the source VM to be powered off. CloudStack checks the VMware
power state and rejects cutover if the source VM is still powered on:

```text
Cannot cut over VMware CBT migration <uuid> while source VM <name> is in power state PowerOn.
Gracefully shut down the source VM, then retry cutover.
```

CloudStack does not attempt a graceful shutdown or power-off. The operator must
shut down the VMware source VM before final cutover. This avoids surprising the
operator and avoids needing to encode site-specific guest shutdown policy in the
migration path.

Cutover sequence:

1. Verify source VM is powered off.
2. Set migration state to `CuttingOver`.
3. Run a final CBT delta sync while the VM is off.
4. Run `virt-v2v` finalization on the KVM host.
5. Move finalized disk files to the primary storage root with generated UUID
   names, then update disk target paths from the agent result.
6. Set state to `ReadyForImport`.
7. Call the shared external KVM VM import path.
8. On success, set state to `Completed`, store the new CloudStack VM ID, and
   clear stored external vCenter credentials.
9. On import failure, keep state as `ReadyForImport` so the operator can retry
   cutover to repeat only the import step against already finalized disks.

### 5. Cancel And Delete

`cancelVmwareCbtMigration` is synchronous. If the migration is not terminal, it
sends a best-effort cleanup command, marks the migration `Cancelled`, and clears
stored external vCenter credentials.

`deleteVmwareCbtMigration` is synchronous. It is allowed for `Failed`,
`Cancelled`, and `Completed` migrations. With `cleanup=true` (the default), it
sends a cleanup command before deleting failed or cancelled child rows and the
parent row. `Completed` migration deletion is always record-only: CloudStack
removes the CBT bookkeeping rows but never deletes the imported VM, CloudStack
volumes, finalized target disks, or primary-storage files.

Deletion behavior:

- Deletes `vmware_cbt_migration_cycle` rows.
- Deletes `vmware_cbt_migration_disk` rows.
- Deletes the `vmware_cbt_migration` row.
- Clears stored source credentials before row removal.
- For `Failed` or `Cancelled` rows, cleans target files only if the paths are
  under the migration-specific `/cloudstack-cbt/<migration-uuid>/` directory
  and cleanup is enabled. For RBD targets, cleanup removes only temporary RBD
  image names containing the CloudStack-owned `cloudstack-cbt-<migration-uuid>-`
  marker.
- For `Completed` rows, ignores cleanup and removes only CBT migration records.
- Skips immediate cleanup if another active migration is running on the same
  conversion host, to avoid disturbing active agent-side work.

The KVM cleanup wrapper resolves migration directories from disk target paths
and removes only the migration-specific directory tree for file targets. For RBD
targets it uses the destination storage pool and deletes only marked temporary
images, never arbitrary RBD images.

## Conversion Model: One VMware Baseline Read

The feature deliberately separates replication from final conversion.

Initial full sync does not run `virt-v2v`. The KVM agent opens the VMware source
snapshot through `nbdkit` with the VDDK plugin and runs `qemu-img convert`
against the NBD URI. Filesystem-like primary storage writes QCOW2:

```text
nbdkit -r -U - vddk ... --run \
  'qemu-img convert -f raw -O qcow2 "$uri" "<targetPath>"'
```

Ceph/RBD primary storage writes a raw RBD image directly:

```text
nbdkit -r -U - vddk ... --run \
  'qemu-img convert -f raw -O raw "$uri" "rbd:<pool>/<image>:..."'
```

This produces a source-equivalent replica on the selected primary storage. It
is not yet a CloudStack-imported VM disk. It is a replication target that can
safely receive raw changed blocks from later CBT cycles.

Delta sync also does not run `virt-v2v`. Management creates a VMware snapshot,
queries changed disk areas using VMware CBT metadata, and dispatches changed
byte ranges to the KVM agent. The agent opens the snapshot through `nbdkit`,
parses the temporary nbdkit Unix socket from the NBD URI, uses `qemu-img
convert --image-opts` to materialize bounded raw chunks for only the changed
ranges, and patches the replica with `qemu-io`. File targets use `-f qcow2`;
RBD targets use `-f raw` against the qemu RBD URL for the target image.

Final cutover is where `virt-v2v` runs. After the source VM is powered off, the
manager runs one final CBT delta cycle so the replica is current. Then the agent
runs one of the finalization paths:

- `virt-v2v-in-place`, when available.
- `virt-v2v --in-place`, when the installed `virt-v2v` supports it.
- Regular `virt-v2v -o local`, only when the non-in-place fallback is explicitly
  enabled and the target is a QCOW2 file target.

Ceph/RBD targets require one of the in-place finalization paths. They do not
allow non-in-place fallback because fallback creates local QCOW2 output rather
than modifying the raw RBD image that CloudStack will import.

So there is not a second full VMware read during finalization. The final
`virt-v2v` step is local work against the up-to-date KVM-side replica. It may
still require local storage reads and writes, especially in fallback mode, but
it is not another VDDK full copy from vCenter.

Important caveat: the current initial full sync exposes the VMware snapshot as a
raw NBD source and lets `qemu-img convert` read it. In practice the initial
baseline reads the full logical VMware disk range: a linked clone, a full clone,
or even a mostly empty 5 GiB disk can still transfer roughly 5 GiB from VMware
during the first sync. This is expected VMware/VDDK baseline behavior and does
not mean the resulting target disk is thick. QCOW2 file targets and raw RBD
targets can still stay sparse or thin on the destination backend when zeroed
regions are written efficiently. Delta cycles are CBT-range based and copy only
changed ranges; the baseline copy is the part that still needs sparse/extent
optimization.

This design avoids applying future CBT raw block deltas to a disk that has
already been modified by `virt-v2v`. Applying VMware CBT ranges to a converted
guest disk would be unsafe because the layout no longer represents the source
VMware disk byte-for-byte.

## API Changes

All commands are admin-only.

### `checkVmwareCbtMigrationPrerequisites`

Synchronous preflight command.

Required:

- `zoneid`
- `clusterid`
- `sourcevmname`

Source vCenter selection:

- Existing registered vCenter: `existingvcenterid`
- External vCenter: `vcenter`, `datacentername`, `username`, `password`

Optional:

- `hostip`
- `clustername`
- `convertinstancehostid`
- `convertinstancestoragepoolid`
- `serviceofferingid`
- `details`

The command has `requestHasSensitiveInfo=true` because external vCenter
credentials can be provided.

### `startVmwareCbtMigration`

Async command. Starts the migration and initial full sync.

Required:

- `zoneid`
- `clusterid`
- `serviceofferingid`
- `sourcevmname`

Source vCenter selection:

- Existing registered vCenter: `existingvcenterid`
- External vCenter: `vcenter`, `datacentername`, `username`, `password`

Optional import inputs:

- `displayname`
- `hostname`
- `account`
- `domainid`
- `projectid`
- `templateid`
- `osid`
- `datadiskofferinglist`
- `nicnetworklist`
- `nicipaddresslist`
- `forced`

Optional conversion inputs:

- `convertinstancehostid`
- `convertinstancestoragepoolid`
- `details`

If `convertinstancestoragepoolid` is omitted, CloudStack auto-selects the only
CBT-compatible primary storage pool in the destination cluster/zone. Supported
target pool types are `NetworkFilesystem`, `Filesystem`, `SharedMountPoint`,
and `RBD`. If more than one compatible pool is available, the operator must
provide the pool explicitly.

Useful `details` keys:

- `vddk.lib.dir`
- `vddk.transports`
- `vddk.thumbprint`
- `cpuNumber`
- `cpuSpeed`
- `memory`

The command has `requestHasSensitiveInfo=true` and
`responseHasSensitiveInfo=false`.

### `listVmwareCbtMigrations`

Synchronous status command and the main progress source for UI and automation.

Filters:

- `id`
- `zoneid`
- `accountid`
- `vcenter`
- `sourcevmname`
- `state`

The response includes migration-level state, current step, current step
duration, last error, cycle counters, changed byte totals, disks, and cycles.
`currentstepduration` is populated for non-terminal migrations and is intended
for the UI table marker similar to Import VM Tasks.

### `syncVmwareCbtMigration`

Async command. Runs one delta sync cycle.

Required:

- `id`

Optional:

- `username`
- `password`

For an external vCenter migration, stored credentials are used when available.
If override credentials are supplied, both username and password must be
provided. Overrides are stored back to the migration so a UI operator can return
later and continue syncing.

### `cutoverVmwareCbtMigration`

Async command. Runs final powered-off sync, finalizes disks, and imports the VM.

Required:

- `id`

Optional:

- `username`
- `password`

The command fails early if the source VMware VM is not powered off.

### `cancelVmwareCbtMigration`

Synchronous command.

Required:

- `id`

Cancels a non-terminal migration, attempts cleanup, and clears stored external
source credentials.

### `deleteVmwareCbtMigration`

Synchronous command.

Required:

- `id`

Optional:

- `cleanup` (default `true`)

Allowed for `Failed`, `Cancelled`, and `Completed` migrations. Completed
migration deletes are record-only and never remove the imported CloudStack VM,
volumes, finalized target disks, or primary-storage files.

## Configuration And Cutover Policy

The cutover recommendation is based on global settings:

| Global setting | Default | Meaning |
| --- | ---: | --- |
| `vmware.cbt.migration.min.cycles` | `1` | Minimum number of manual/API delta cycles that must complete before quiet-cycle readiness can make the migration eligible for cutover. |
| `vmware.cbt.migration.max.cycles` | `5` | Maximum number of manual/API delta cycles before CloudStack marks the migration ready for cutover even if the quiet-cycle thresholds were not met. |
| `vmware.cbt.migration.quiet.cycles` | `2` | Number of consecutive quiet cycles required before CloudStack marks the migration ready for cutover. |
| `vmware.cbt.migration.quiet.bytes` | `1073741824` | Maximum changed bytes in one cycle for that cycle to be considered quiet. The default is 1 GiB. |
| `vmware.cbt.migration.quiet.dirty.rate` | `16777216` | Maximum changed bytes per second for that cycle to be considered quiet. The default is 16 MiB/s. |
| `vmware.cbt.migration.agent.command.timeout` | `86400` | Timeout in seconds for long-running VMware CBT data-plane commands on the KVM agent. This covers initial full sync, regular delta sync, final delta sync, and cutover finalization. The default is 24 hours. |

The manager tracks:

- Number of completed delta cycles.
- Number of consecutive quiet cycles.
- Last changed bytes.
- Last dirty rate.
- Total changed bytes.

A cycle is quiet when its changed bytes and dirty rate are below the configured
thresholds. Both thresholds must pass. For example, with defaults, a cycle that
copies 1.2 GiB at 10 MiB/s is not quiet because the changed-byte threshold is
exceeded even though the dirty-rate threshold passed.

Once enough quiet cycles have accumulated, or once the maximum manual/API
delta-cycle count is reached, the migration moves to `ReadyForCutover`.

The operator can still manually run sync cycles while the migration is in
`Replicating` or `ReadyForCutover`.

The UI intentionally exposes the Cutover button only when the server-side
migration state is exactly `ReadyForCutover`. Before that point the UI exposes
Sync delta, because the server has not yet recommended final cutover. If a
cutover is started, CloudStack creates one additional final CBT delta cycle
inside the cutover operation. Therefore the final displayed cycle count can be
one higher than `vmware.cbt.migration.max.cycles`: for example, five ordinary
delta cycles can make the migration ready, and cutover can then record cycle 6
as the final synchronization cycle.

### Timeouts And Large VMs

The long-running CBT work is done by KVM agent commands. CloudStack sets the
agent command `wait` value from
`vmware.cbt.migration.agent.command.timeout`, and the KVM wrappers use the same
value as the timeout for child processes such as `nbdkit`, `qemu-img`, and
`virt-v2v`.

The timeout is finite by design. The shared CloudStack `Script` helper treats a
zero timeout as its default one-hour timeout, so `0` is not a safe way to mean
"run forever". For large source disks, tune the timeout above the expected
worst-case duration. A rough estimate is:

```text
timeout_seconds >= source_bytes / expected_bytes_per_second * safety_factor
```

For example, a 1 TiB initial sync at 100 MiB/s is roughly 3 hours before
overhead; at 25 MiB/s it is roughly 12 hours. The default 24-hour timeout is
intended to be safe for large lab and production migrations while still
protecting the agent from permanently stuck child processes.

Related import settings that operators often notice are:

| Global setting | Applies to CBT? | Default | Meaning |
| --- | --- | ---: | --- |
| `convert.vmware.instance.to.kvm.timeout` | No | `3` | OVF/VDDK import timeout, in hours, for the `ConvertInstanceCommand` virt-v2v path. CBT cutover has its own timeout because it uses CBT-specific agent commands. |
| `remote.kvm.instance.disks.copy.timeout` | No | `30` | Remote KVM unmanaged import disk-copy timeout, in minutes. It is unrelated to VMware CBT. |

If a CBT command exceeds `vmware.cbt.migration.agent.command.timeout`, the
migration is marked failed with the agent error details in `last_error`.
Failed or cancelled migration records can be deleted with cleanup enabled to
remove CloudStack-owned `cloudstack-cbt/<migration-uuid>` working files.

## Database Changes

The feature targets the Apache CloudStack 4.24.0.0 release, so the schema changes
are delivered through the 4.24.0.0 upgrade path:

- `engine/schema/src/main/resources/META-INF/db/schema-42300to42400.sql`
- `engine/schema/src/main/java/com/cloud/upgrade/dao/Upgrade42300to42400.java`

> Until `main` opens `4.24.0.0-SNAPSHOT`, the DDL rides in the current
> in-development `schema-42210to42300.sql`; it is moved to the 4.24.0.0 upgrade
> file before merge.

### `cloud.vmware_cbt_migration`

One row per migration session.

Important columns:

- `uuid`
- `zone_id`
- `account_id`
- `user_id`
- `vm_id`
- `existing_vcenter_id`
- `source_username`
- `source_password`
- `destination_cluster_id`
- `convert_host_id`
- `storage_pool_id`
- `display_name`
- `host_name`
- `template_id`
- `service_offering_id`
- `guest_os_id`
- `data_disk_offering_map`
- `nic_network_map`
- `nic_ip_address_map`
- `import_details`
- `forced`
- `vcenter`
- `datacenter`
- `source_host`
- `source_cluster`
- `source_vm_name`
- `vddk_lib_dir`
- `vddk_transports`
- `vddk_thumbprint`
- `state`
- `current_step`
- `last_error`
- `completed_cycles`
- `quiet_cycles`
- `total_changed_bytes`
- `last_changed_bytes`
- `last_dirty_rate`
- `created`
- `updated`
- `removed`

`source_password` is annotated with `@Encrypt` in `VmwareCbtMigrationVO`, using
the same CloudStack encrypted DAO field mechanism used by VMware datacenter
credentials and VM VNC passwords.

### `cloud.vmware_cbt_migration_disk`

One row per source VMware disk.

Important columns:

- `migration_id`
- `source_disk_id`
- `source_disk_device_key`
- `source_disk_path`
- `datastore_name`
- `capacity_bytes`
- `target_path`
- `target_format`
- `change_id`
- `snapshot_moref`
- `state`

Disk states:

- `Created`
- `Prepared`
- `Syncing`
- `Ready`
- `Failed`

### `cloud.vmware_cbt_migration_cycle`

One row per delta cycle.

Important columns:

- `migration_id`
- `cycle_number`
- `snapshot_moref`
- `changed_bytes`
- `dirty_rate`
- `duration`
- `state`
- `description`

Cycle states:

- `Created`
- `QueryingChangedAreas`
- `CopyingChangedBlocks`
- `Completed`
- `Failed`

## Migration State Machine

Migration states:

- `Created`
- `InitialSync`
- `Replicating`
- `ReadyForCutover`
- `CuttingOver`
- `ReadyForImport`
- `Completed`
- `Failed`
- `Cancelled`

Terminal states:

- `Completed`
- `Failed`
- `Cancelled`

Normal successful path:

```text
Created
  -> InitialSync
  -> Replicating
  -> ReadyForCutover
  -> CuttingOver
  -> ReadyForImport
  -> Completed
```

`ReadyForImport` is intentionally retryable. If the final `virt-v2v`
finalization succeeded but CloudStack VM import failed, the next cutover command
does not rerun the final CBT copy and finalization. It retries the VM import
against the already finalized disk paths.

## Storage Target And Finalization

`VmwareCbtStorageTarget` maps primary storage to a target writer:

- `NetworkFilesystem`, `Filesystem`, and `SharedMountPoint` use `QCOW2_FILE`.
- No explicit pool also uses a filesystem QCOW2 target under the default local
  CBT path.
- `RBD` maps to `RBD_RAW`. Initial sync writes a raw RBD image directly with
  `qemu-img convert -O raw`, delta sync writes changed ranges with `qemu-io -f
  raw`, and cutover finalization is in-place only.
- Other primary storage types are unsupported.

For QCOW2 file targets, the initial replica is stored under the selected
primary storage:

```text
/mnt/<pool-uuid>/cloudstack-cbt/<migration-uuid>/<disk>.qcow2
```

If no storage pool is selected, the fallback path is:

```text
/var/lib/libvirt/images/cloudstack-cbt/<migration-uuid>/<disk>.qcow2
```

For Ceph/RBD targets, the initial replica is a raw RBD image in the selected
pool:

```text
<ceph-pool>/cloudstack-cbt-<migration-uuid>-<source-disk-id>-<source-name>
```

Only the image name is persisted as the migration disk target path. The agent
reconstructs the qemu RBD URL from the selected storage pool when writing
initial and delta data. During in-place `virt-v2v` finalization, the agent
starts a temporary localhost `qemu-nbd` bridge for each RBD image and gives
`virt-v2v` a libvirt XML disk that points to that local NBD endpoint. This keeps
RBD credentials and monitor options in the qemu layer while avoiding the
libguestfs/RBD XML path that failed to expose usable disks in testing.

The preferred cutover finalization is in-place:

- `virt-v2v-in-place`, if present.
- `virt-v2v --in-place`, if the installed `virt-v2v` supports it.

If neither in-place method is available, QCOW2 file targets can use regular
`virt-v2v -o local` only when this global configuration is enabled:

```text
vmware.cbt.allow.non.inplace.finalization=true
```

Default:

```text
false
```

When fallback is enabled:

- The fallback is only allowed for supported QCOW2 file targets.
- The agent stages both `TMPDIR` and `virt-v2v` output under the current
  migration directory on the selected primary storage.
- The agent validates free space before running fallback finalization.
- Required free space is conservatively estimated as two times the summed disk
  capacity.
- After successful fallback finalization, source replica disks are deleted.

Fallback is never allowed for RBD targets. RBD migrations require
`virt-v2v-in-place` or `virt-v2v --in-place` support on the selected conversion
host.

Example fallback output path:

```text
/mnt/<pool-uuid>/cloudstack-cbt/<migration-uuid>/virt-v2v-output-<random>/<migration-uuid>-sda
```

After successful finalization, both in-place and fallback paths are normalized
to the same final layout used by the OVF/VDDK import flow: the KVM agent moves
each finalized QCOW2 file to the selected primary storage root and gives it a
generated UUID filename:

```text
/mnt/<pool-uuid>/<generated-volume-uuid>
```

The agent returns the relocated target paths in the cutover answer. The
management server persists them in `vmware_cbt_migration_disk.target_path`
through the normal disk-result update path, so no manual database update or
schema change is required for this relocation. CloudStack then imports the VM
using the flat root-level file name, matching the final placement used by
regular OVF/VDDK migrations.

For RBD targets, successful in-place finalization keeps the raw RBD image in
place and CloudStack imports it by image name from the selected RBD storage
pool. No post-finalization file move is performed, and completed migration
deletion remains record-only.

RBD finalization creates only temporary localhost `qemu-nbd` processes and
temporary XML/script files on the conversion host. The wrapper installs a shell
trap to stop those bridge processes and remove the PID files after `virt-v2v`
finishes or fails. The persistent storage artifact remains the raw RBD image
whose name contains the `cloudstack-cbt-<migration-uuid>-` marker until
CloudStack imports it or cleanup removes it for a failed/cancelled migration.

CloudStack imports finalized CBT disks with the KVM `virtio` root disk
controller. The conversion host must have `virtio-win` available for Windows
guests so that `virt-v2v` can enable the matching boot driver before the VM is
started under KVM. This keeps the persisted `rootDiskController` detail aligned
with the controller model used by the converted guest.

## Ceph/RBD Conversion Host Requirements

Ceph/RBD support relies on the same KVM host plumbing that normal CloudStack RBD
primary storage uses, plus the CBT-specific VMware conversion tools. The
selected conversion host must be able to access the destination RBD pool from
both Java/libvirt storage code and the qemu command-line tools used by the CBT
wrappers.

Required conversion-host capabilities:

- CloudStack KVM agent with the normal RBD primary-storage dependencies.
- The selected KVM/conversion host must already be able to use the destination
  CloudStack RBD primary storage pool.
- `librados` and `librbd` client libraries compatible with the Ceph cluster.
- Java RADOS/RBD bindings used by CloudStack's RBD storage adaptor.
- qemu RBD block driver support so `qemu-img` and `qemu-io` can open
  `rbd:<pool>/<image>:...` URLs.
- VDDK and `nbdkit-vddk` for VMware source reads.
- `virt-v2v-in-place` or `virt-v2v --in-place` for RBD cutover finalization.
- `virtio-win` or equivalent VirtIO driver media/packages for Windows guests,
  same as the existing VMware import requirements.

CloudStack RBD primary-storage configuration must be valid on the selected
KVM/conversion host. For CBT-to-RBD, CloudStack uses the existing KVM
storage-pool metadata to build qemu RBD URLs, including monitor addresses,
authentication mode, Ceph user, and secret. The CBT qemu path therefore does not
rely on `/etc/ceph/ceph.conf` or a local keyring as its primary credential source
when CloudStack supplies explicit RBD URL options.

A local Ceph configuration and keyring can still be useful for operator
diagnostics and site-local tooling, for example running `ceph -s`,
`rbd ls <pool>`, or similar commands without manually passing monitor and
authentication options each time. Sites may choose to install those files for
operational convenience, but they should not be documented as the required
credential source for CBT RBD writes.

The Ceph client packages on every KVM/conversion host should match the Ceph
cluster major release, or at least be explicitly supported by that cluster
release. Avoid relying on the distribution's stock Ceph packages if they are
older than the deployed Ceph cluster. Ancient `librbd`, `librados`, qemu RBD
block-driver, or Java binding packages can fail in ways that look like CBT
errors even though the actual problem is a client/cluster compatibility mismatch.

Practical guidance:

- If the Ceph cluster is deployed from upstream Ceph packages, install the
  corresponding upstream Ceph client repository on all KVM/conversion hosts.
- If the Ceph cluster is deployed from a vendor repository, use that vendor's
  matching client packages on the KVM/conversion hosts.
- Keep `ceph-common`, `librados`, `librbd`, qemu RBD block-driver packages, and
  Java RADOS/RBD bindings aligned as a set. Do not mix a modern Ceph cluster
  with old OS-default client libraries unless the Ceph vendor explicitly
  supports that combination.
- Verify the KVM host can already use the selected RBD primary storage for
  ordinary CloudStack VM volumes before enabling CBT migrations to the same
  pool.
- Verify that qemu tools can see RBD support. Package names vary by OS, but on
  many EL-like distributions this is provided by a qemu RBD block-driver package
  such as `qemu-kvm-block-rbd`; on Debian/Ubuntu-like systems it is commonly
  part of qemu block extras.

CBT preflight and `startVmwareCbtMigration` actively probe RBD access when the
selected destination pool is RBD. The management server sends the selected
conversion host a small probe command using a temporary image named
`cloudstack-cbt-probe-<uuid>`. The KVM agent builds the qemu RBD URL from the
existing CloudStack storage-pool metadata, runs `qemu-img create`, writes and
reads a 4 KiB pattern with `qemu-io`, and removes the temporary image through
CloudStack's existing RBD storage adaptor. This catches qemu RBD driver issues,
Ceph monitor/authentication problems, and Java RADOS/RBD cleanup failures before
the long-running VMware copy starts.

Useful host checks:

```bash
ceph -s
ceph osd lspools
rbd ls <pool>
qemu-img --help | grep -i rbd
qemu-io --help | grep -i rbd
```

Useful package/version checks:

```bash
ceph --version
rbd --version
ldconfig -p | egrep 'librbd|librados'
java -version
```

Useful CloudStack database check after the host reconnects:

```sql
SELECT h.id, h.name, hd.name, hd.value
FROM cloud.host h
JOIN cloud.host_details hd ON hd.host_id = h.id
WHERE h.name = '<kvm-host-name>'
  AND hd.name IN (
    'host.vddk.blockcopy.support',
    'host.vddk.blockcopy.inplace.finalization.support',
    'host.vddk.blockcopy.rbd.support',
    'host.vddk.support',
    'host.vddk.version',
    'host.virtv2v.inplace.version',
    'vddk.lib.dir'
  )
ORDER BY hd.name;
```

An RBD-capable conversion host should already report VMware CBT capability as
`true` before a migration starts, and the selected destination storage pool must
be the RBD pool that the host can access. If these host details are stale after
installing packages, restart `cloudstack-agent`, ensure the management server is
running, and wait for host details to be refreshed.

## Host Capability Reporting

The agent checks these on startup/ready and sends them as host details. Management then persists them into `cloud.host_details`.

| Host detail | How agent checks it |
| --- | --- |
| `host.virtv2v.version` | Runs `virt-v2v --version`. On Ubuntu/Debian it also checks `dpkg -l nbdkit`. Version is parsed from `virt-v2v --version`. |
| `vddk.lib.dir` | Reads `vddk.lib.dir` from `agent.properties`; if blank/invalid, auto-detects with `find / -type d -name 'vmware-vix-disklib-distrib' 2>/dev/null \| head -n 1`. Valid means `<dir>/lib64/libvixDiskLib.so*` exists. |
| `host.vddk.version` | Runs `nbdkit vddk --dump-plugin libdir=<vddk-lib-dir>` and parses `vddk_library_version=<n>`. |
| `host.vddk.support` | True only if `virt-v2v`/`nbdkit` support is present, VDDK dir is valid, and `nbdkit-vddk-plugin` can load VDDK via `nbdkit vddk --dump-plugin libdir=<dir>`. |
| `host.qemu.img.version` | Runs `qemu-img --version`, parses first non-empty line. |
| `host.qemu.nbd.version` | Runs `qemu-nbd --version`, parses first non-empty line. |
| `host.qemu.io.version` | Runs `qemu-io --version`, parses first non-empty line. |
| `host.vddk.blockcopy.support` | True only if `host.vddk.support` is true and `qemu-img --version`, `qemu-nbd --version`, and `qemu-io --version` all exit `0`. |
| `host.vddk.blockcopy.inplace.finalization.support` | True only if CBT support is true and either `virt-v2v-in-place --version` works, or `virt-v2v --help 2>&1 \| grep -q -- '--in-place'` succeeds. |
| `host.virtv2v.inplace.version` | If `virt-v2v-in-place` exists, parses `virt-v2v-in-place --version`. If only `virt-v2v --in-place` exists, reports the normal `virt-v2v` version. |
| `host.vddk.blockcopy.rbd.support` | True only if in-place finalization support is true and `qemu-img --help` advertises the `rbd` block driver. |

Important nuance: `host.vddk.blockcopy.rbd.support` is only the coarse host capability. For an actual selected RBD pool, preflight/start also runs an active probe: create a temporary `cloudstack-cbt-probe-<uuid>` RBD image, write/read 4 KiB using `qemu-io`, then delete it through CloudStack's RBD storage adapter. That catches Ceph auth, monitor, qemu RBD, librados/librbd, and Java binding problems.

Windows source VMs also require `virtio-win` on the selected KVM conversion
host. CBT preflight and `startVmwareCbtMigration` use the same
`CheckConvertInstanceCommand` path as OVF/VDDK import with Windows guest
conversion enabled, so a missing `virtio-win` package is reported before the
initial sync starts.

VDDK detection uses either agent configuration or auto-detection. The agent
validates the VDDK library by checking that `lib64/libvixDiskLib.so*` exists
and that `nbdkit vddk --dump-plugin libdir=<path>` reports a library version.

## VDDK Details

VDDK options can be provided as API `details` at migration start:

- `vddk.lib.dir`
- `vddk.transports`
- `vddk.thumbprint`

If a value is not provided by the API, the agent uses its configured or detected
value. If `vddk.thumbprint` is not provided, the agent attempts to fetch the
vCenter SHA1 certificate thumbprint with `openssl`.

Lab note: the OL8 validation environment required the Linux VDDK package and a
VDDK build compatible with the host OpenSSL libraries. The implementation does
not hard-code one VDDK version; it relies on the agent capability check.

## Credential Handling

There are two source credential modes:

- Existing registered vCenter: credentials are read from the registered
  `vmware_data_center` record.
- External vCenter: credentials are supplied to start/preflight and stored on
  the migration record so later UI sync/cutover actions can run without asking
  again.

External credential behavior:

- `source_password` is stored through the CloudStack encrypted DAO field
  mechanism via `@Encrypt`.
- Start, preflight, sync, and cutover commands that can carry credentials are
  marked `requestHasSensitiveInfo=true`.
- API responses are marked `responseHasSensitiveInfo=false`.
- Agent-side VDDK commands pass the password through a temporary password file
  and `nbdkit` `password=+<file>`, not by embedding the clear-text password in
  the command line.
- Management sanitizes returned error messages by replacing the resolved source
  password with `******` before storing `last_error` or cycle descriptions.
- Stored external credentials are cleared when the migration completes,
  when it is cancelled, and before deletion.

The implementation avoids logging clear-text passwords. As with other
CloudStack secret handling, this also depends on callers not placing secrets in
unrelated free-form log messages.

## Compute Offering And Preflight Validation

CBT migration uses the same eventual import path as the existing OVF/VDDK VM
import code. The final CloudStack VM sizing still comes from the selected
service offering and any dynamic offering details.

The CBT-specific difference is timing: CBT validates obvious sizing problems
early, before the long initial full sync begins. This is meant to avoid spending
hours copying disks only to fail final VM import because the selected offering
is too small.

The source VM sizing captured during preflight/start includes:

- `sourcecpunumber`
- `sourcecpuspeed`
- `sourcememory`

Validation rule:

- If the source value and requested value are both known and greater than zero,
  the requested value must be greater than or equal to the source value.
- Unknown or zero source values are not used to reject the offering.

Validated fields:

- CPU number
- CPU speed
- Memory

Fixed offerings:

- CPU number comes from `service_offering.cpu`.
- CPU speed comes from `service_offering.speed`.
- Memory comes from `service_offering.ram_size`.

Dynamic/custom offerings:

- Caller details can provide `cpuNumber`, `cpuSpeed`, and `memory`.
- If caller details are missing, offering minimums such as `mincpunumber` and
  `minmemory` can be used where applicable.
- This mirrors the shared import behavior in
  `UnmanagedVMsManagerImpl.addServiceOfferingDetailsToParams`.

Example custom offering details:

```text
details[0].key=cpuNumber
details[0].value=4
details[1].key=cpuSpeed
details[1].value=2100
details[2].key=memory
details[2].value=4096
```

This early validation is not intended to be stricter than OVF/VDDK import. It
is intended to fail earlier for the same class of sizing mismatch.

## UI Changes

The CBT UI is integrated into the Import/Export Instances area.

Implemented UI behavior:

- Start migration uses the async `startVmwareCbtMigration` job.
- Sync delta uses the async `syncVmwareCbtMigration` job.
- Cutover uses the async `cutoverVmwareCbtMigration` job.
- Each async action uses CloudStack job polling.
- The CBT migration table also calls `listVmwareCbtMigrations` as the detailed
  status source.
- The table auto-refreshes while the CBT tab is active and any migration is in
  an active state or a CBT action job is still running.

Active states for UI refresh:

- `Created`
- `InitialSync`
- `Replicating`
- `CuttingOver`

The table exposes:

- Migration state.
- Current step.
- Completed cycles.
- Quiet cycles.
- Last changed bytes.
- Last dirty rate.
- Total changed bytes.
- Last error.
- Per-disk target path, target format, change ID, snapshot MOR, and state.
- Per-cycle state, changed bytes, dirty rate, duration, description, created,
  and updated timestamps.

Initial sync progress is currently coarse. While `qemu-img convert` runs on the
agent, management can show the migration state, current step, target path, and
disk state, but not a continuously updated percentage. Delta syncs have better
post-cycle metrics because changed bytes and dirty rate are returned by the
cycle result.

## Failure And Retry Semantics

Initial full sync failure:

- Migration moves to `Failed`.
- Created or syncing disks are marked `Failed`.
- Last error is stored on the migration.
- Delete with cleanup can remove the migration directory.

Delta sync failure:

- The cycle is marked `Failed`.
- The migration moves to `Failed`.
- Last error and cycle description are stored.
- VMware cycle snapshot removal is still attempted.

Cutover finalization failure:

- Migration moves to `Failed`.
- The source VM remains under operator control in vCenter.
- Delete with cleanup can remove replicated/finalized target directories if the
  paths are under the migration directory.

Import failure after successful finalization:

- Migration remains `ReadyForImport`.
- Target disk paths point at the finalized disk files.
- Retrying cutover retries the import step.

Cancel:

- Best-effort cleanup.
- State becomes `Cancelled`.
- Stored external credentials are cleared.

Delete:

- `Failed`, `Cancelled`, or `Completed`.
- Optional cleanup, default `true`, applies only to `Failed` and `Cancelled`.
- `Completed` deletion is record-only and intentionally preserves the imported
  VM and all destination storage artifacts.
- Child rows and parent row are removed.

## Security And Logging Considerations

Important security choices:

- Credential-bearing commands are marked sensitive.
- External vCenter password storage uses encrypted DAO fields.
- Passwords are passed to VDDK through temp files instead of clear-text command
  arguments.
- Password temp files are owner-readable and owner-writable where POSIX
  permissions are supported.
- Returned failure messages are sanitized against the active source password
  before persistence.
- CBT delta and cutover agent wrappers include the last useful command output
  line in returned failure details when available. This keeps management server
  and UI errors actionable for `qemu-nbd`, `qemu-io`, `nbdkit`, and `virt-v2v`
  failures without logging or returning clear-text passwords.

Operational caution:

- `source_username` is not encrypted.
- vCenter host, datacenter, VM name, datastore names, disk paths, change IDs,
  and snapshot MORs are not treated as secrets and can appear in API responses
  and logs.
- Operators should still avoid manually pasting credentials into log-visible
  free-form fields.

## Testing Strategy

Relevant unit coverage includes:

- `VmwareCbtMigrationOfferingValidationTest`
- `VmwareCbtMigrationDeletePolicyTest`
- `VmwareCbtStorageTargetTest`
- `VmwareCbtMigrationCutoverPolicyTest`
- `LibvirtVmwareCbtSyncCommandWrapperTest`
- `VmwareCbtSyncPlanTest`
- `LibvirtVmwareCbtCutoverCommandWrapperTest`
- `LibvirtVmwareCbtCleanupCommandWrapperTest`
- `LibvirtVmwareCbtRbdProbeCommandWrapperTest`
- `LibvirtStoragePoolTest`

The tests cover:

- Fixed offering validation.
- Dynamic/custom offering detail handling.
- Storage target classification.
- Non-in-place finalization gating.
- Cutover policy decisions.
- Delta copy script planning.
- Chunked changed-range reads from the nbdkit Unix socket.
- RBD probe create/write/read/delete command planning.
- RBD finalization through temporary localhost `qemu-nbd` bridges.
- Fallback `virt-v2v` output path handling.
- Cleanup scoping to migration directories.
- Cleanup scoping to migration-owned RBD image names.
- Nested relative KVM volume path resolution under filesystem pools.

## Operational Notes For Lab Validation

Useful host capability check:

```bash
cmk list hosts name=<kvm-host-name> details=all | egrep \
'host.vddk.blockcopy.support|host.vddk.blockcopy.inplace.finalization.support|host.vddk.blockcopy.rbd.support|host.vddk.support|host.vddk.version|host.virtv2v.version|host.virtv2v.inplace.version|host.qemu.img.version|host.qemu.nbd.version|host.qemu.io.version|vddk.lib.dir'
```

Useful VDDK plugin check on the KVM host:

```bash
nbdkit vddk --dump-plugin libdir=/opt/vmware-vix-disklib-distrib | \
  egrep 'vddk_library_version|vddk_dll|vddk_transport_modes'
```

Useful process check during initial sync:

```bash
pgrep -af 'nbdkit|qemu-img'
```

Useful target disk inspection:

```bash
qemu-img info /mnt/<pool-uuid>/cloudstack-cbt/<migration-uuid>/<disk>.qcow2
```

Manual SQL for labs that do not run the 4.24.0.0 upgrade path:

```sql
INSERT INTO `cloud`.`configuration`
    (`category`, `instance`, `component`, `name`, `value`, `description`,
     `default_value`, `updated`, `scope`, `is_dynamic`)
VALUES
    ('Advanced', 'DEFAULT', 'VmwareCbtMigrationManagerImpl',
     'vmware.cbt.allow.non.inplace.finalization', 'false',
     'If true, VMware CBT cutover may fall back to regular virt-v2v finalization for qcow2 file targets when true in-place finalization is unavailable. The fallback stages temporary data on the selected primary storage and requires additional free space.',
     'false', NOW(), 1, 1)
ON DUPLICATE KEY UPDATE
    `category` = VALUES(`category`),
    `component` = VALUES(`component`),
    `description` = VALUES(`description`),
    `default_value` = VALUES(`default_value`),
    `updated` = NOW(),
    `scope` = VALUES(`scope`),
    `is_dynamic` = VALUES(`is_dynamic`);

INSERT INTO `cloud`.`configuration`
    (`category`, `instance`, `component`, `name`, `value`, `description`,
     `default_value`, `updated`, `scope`, `is_dynamic`)
VALUES
    ('Advanced', 'DEFAULT', 'VmwareCbtMigrationManagerImpl',
     'vmware.cbt.migration.agent.command.timeout', '86400',
     'Timeout in seconds for long-running VMware CBT data-plane commands dispatched to the KVM agent, including initial full sync, delta sync, final delta sync, and cutover finalization.',
     '86400', NOW(), 1, 1)
ON DUPLICATE KEY UPDATE
    `category` = VALUES(`category`),
    `component` = VALUES(`component`),
    `description` = VALUES(`description`),
    `default_value` = VALUES(`default_value`),
    `updated` = NOW(),
    `scope` = VALUES(`scope`),
    `is_dynamic` = VALUES(`is_dynamic`);
```

To allow regular `virt-v2v` fallback for QCOW2 file targets where in-place
finalization is unavailable:

```sql
UPDATE `cloud`.`configuration`
SET `value` = 'true'
WHERE `name` = 'vmware.cbt.allow.non.inplace.finalization';
```

## Next Phase Manifest

This section is the proposed follow-up roadmap for the next development cycle.
It is intentionally written as a working manifest so implementation decisions can
be checked against it during review.

### Non-Negotiable Guardrails

- Source VM shutdown remains operator-controlled by default.
- Any CloudStack-initiated source shutdown must be explicit, audited, and
  opt-in per cutover request or per migration policy.
- Deleting a completed CBT migration record must remain record-only and must
  never delete the imported VM, CloudStack volumes, finalized target disks, or
  primary-storage files.
- Raw VMware CBT deltas must only be applied to a source-equivalent replica.
  They must not be applied after `virt-v2v` has transformed the disk.
- Credential handling must continue to avoid clear-text passwords in command
  arguments, persisted logs, and returned API errors.
- In-place finalization remains preferred. Non-in-place `virt-v2v` fallback must
  stay explicitly gated by configuration because it needs more temporary storage.

### P0: Operator-Grade Progress Reporting

Add a real progress model for long-running CBT work.

Planned behavior:

- Track `currentsteppercent` when the agent can derive it.
- Track `currentstepbytesdone` and `currentstepbytestotal` where byte totals are
  known.
- Track `lastprogressupdated` so the UI can distinguish "still running" from
  "possibly stalled".
- Parse or capture progress from:
  - `qemu-img convert -p` during initial full sync.
  - Delta sync range execution, using completed changed ranges and copied bytes.
  - `virt-v2v` and `virt-v2v-in-place` output during finalization.
- Show progress consistently in the VMware CBT Migrations table and expanded
  row, similar to Import VM Tasks but with CBT-specific byte/range context.

Expected API/data model additions:

- `currentsteppercent`
- `currentstepbytesdone`
- `currentstepbytestotal`
- `lastprogressupdated`
- optional per-disk progress fields for multi-disk migrations

### P0: Automated Delta Sync Policies

Add server-side policy support so operators do not have to manually click Sync
delta for every migration.

Planned behavior:

- Per-migration mode:
  - `manual`
  - `auto-until-ready`
  - `scheduled`
- Configurable interval, for example every 5, 15, or 30 minutes.
- Optional quiet-window behavior: keep syncing until the existing cutover policy
  marks the migration `ReadyForCutover`.
- Pause and resume automatic sync without deleting migration state.
- Respect per-host, per-cluster, and per-vCenter concurrency limits.

Guardrail:

- Automatic sync must not automatically perform final cutover unless a later
  explicit cutover policy is added and enabled.

### P0: Cutover Controls And Optional Graceful Shutdown

Keep the current safe default, but offer an explicit controlled shutdown option.

Planned behavior:

- Default cutover behavior remains unchanged: reject cutover when the VMware
  source VM is still powered on.
- Add an explicit cutover option such as:
  - `shutdownsource=true`
  - `shutdownmode=guest`
  - `shutdownwaitseconds=<n>`
- Guest shutdown uses VMware Tools / vCenter guest shutdown where available.
- If graceful shutdown fails or times out, cutover fails cleanly unless an
  explicit future force-off option is added.
- All CloudStack-initiated source shutdown attempts are logged and visible in the
  migration event/audit trail.

Non-goal for the next phase:

- Silent source VM shutdown.
- Default force power-off.

### P1: Bulk Operations And Retention

Make CBT usable for tens or hundreds of migrations.

Planned behavior:

- Bulk actions:
  - sync selected migrations
  - pause/resume automatic sync
  - cancel selected migrations
  - delete completed records
- Completed migration retention policy:
  - keep forever
  - delete records after N days
  - delete records immediately after successful import, if configured
- Retention cleanup for completed migrations remains record-only.
- Failed/cancelled cleanup continues to remove only CloudStack-owned
  `cloudstack-cbt/<migration-uuid>` working directories.

### P1: Preflight And UI Guardrails

Make unsafe choices visible before an operator starts a long migration.

Planned behavior:

- Host capability matrix in the UI:
  - VDDK support and version
  - `qemu-img`, `qemu-nbd`, `qemu-io`, and `nbdkit` versions
  - `virt-v2v` availability
  - `virt-v2v-in-place` / `--in-place` support
  - `virtio-win` availability for Windows guest conversion
- Grey out incompatible compute offerings in the migration form when CPU cores,
  CPU speed, or memory are below source requirements.
- Keep server-side offering validation as the source of truth.
- Warn when VMware configured guest OS differs from VMware Tools runtime guest
  OS, when both values are available.
- Require or strongly prompt for explicit target Guest OS selection when source
  mapping is generic, such as `Other (64-bit)` or `ubuntu64Guest`.

### P1: Failure Taxonomy And Retry Semantics

Make retries predictable and explainable.

Planned behavior:

- Classify failures by phase:
  - preflight
  - initial sync
  - delta sync
  - final delta
  - finalization
  - import
  - cleanup
- Preserve the current `ReadyForImport` retry behavior.
- Make retry affordances explicit in the UI.
- Return the last useful sanitized agent output line in API/UI errors.
- Track whether a retry will repeat VMware reads, local finalization, or import
  only.

### P2: Baseline Transfer Optimization Research

Investigate whether the initial full logical VMware read can be reduced.

Research questions:

- Validate VDDK allocation/extents for VMware snapshots in a way usable by the
  current NBD/qemu path.
- Can `qemu-img map`, NBD allocation metadata, or VDDK APIs distinguish
  genuinely unallocated regions from allocated-zero regions for linked clones
  and full clones?
- Can a safe sparse baseline mode skip network transfer of known-zero or
  unallocated ranges without corrupting guest-visible disk contents?
- Is behavior different across OL8, OL9, Ubuntu 24.04, qemu versions, VDDK
  versions, VMFS/NFS/vSAN datastore types, linked clones, and full clones?
- Specifically compare NFS-backed VMware datastores with VMFS6-backed
  datastores. NFS testing showed the initial baseline can appear fully
  allocated to the NBD/qemu path, causing a full logical disk read. VMFS6
  testing after guest space reclamation showed sparse `data` and `hole,zero`
  extents through `nbdinfo --map`, and observed lower transfer on the KVM host.
- Keep the manual probe aligned with the production CBT path: pass `vm=moref`,
  the exact datastore VMDK path, VDDK `libdir`, `transports`, and the vCenter
  SHA1 `thumbprint`. Missing `thumbprint` can make the probe fail before any
  useful extent map is returned.
- Test whether VDDK transport choice changes extent behavior. In particular,
  compare `nbd`, `nbdssl`, and any available SAN/hotadd-compatible path where
  the lab environment can support it.

Possible outcomes:

- Implement sparse/extent-aware baseline copy.
- Keep the current full-logical-baseline behavior and document it as a VMware
  VDDK/NBD limitation.
- Offer an experimental mode only when the source stack proves allocation
  metadata is trustworthy.

### P2: Scheduling And Cutover Windows

Add change-window-aware workflows for production migrations.

Planned behavior:

- Allow migrations to sync automatically during business hours but cut over only
  inside an approved window.
- Add policy fields such as:
  - sync interval
  - cutover window start/end
  - timezone
  - maximum concurrent cutovers
- The UI should explain why a migration is waiting: not quiet yet, outside the
  cutover window, source VM still powered on, or host concurrency limit reached.

### P2: API And Documentation Hardening

Keep public surfaces coherent as features are added.

Planned behavior:

- Add API documentation for policy fields and progress fields.
- Add cloudmonkey examples for:
  - manual mode
  - automatic sync mode
  - scheduled mode
  - explicit graceful shutdown cutover
  - record-only completed migration deletion
- Add upgrade notes for new configuration keys and schema changes.
- Keep CWIKI, Markdown README, UI labels, and API descriptions aligned.

## References

- Apache CloudStack 4.24.0.0 Design Documents CWIKI page.
- Multiple CD-ROM / ISO Support Per VM design page.
- CloudStack Veeam KVM Integration design page.
- DNS Framework and Plugins design page.
- Error Message Consistency, Customization, and Localization Framework design
  page.
- KVM Backup on Secondary Storage (KBOSS) design page.
