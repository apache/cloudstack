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

# VMware CBT migration architecture

This document describes the implemented Apache CloudStack architecture for
warm VMware-to-KVM migration based on VMware Changed Block Tracking (CBT).
Detailed API, database, configuration, and operational information is in
[`README.md`](README.md). KVM host preparation is documented in
[`../vmware-cbt-migration.md`](../vmware-cbt-migration.md).

## Design invariant

The KVM-side replica remains byte-compatible with the VMware source disk until
the final CBT cycle has completed. Raw VMware CBT ranges are never applied to a
disk after guest conversion has changed its layout.

The migration therefore has two distinct phases:

1. Replication: copy the initial source-equivalent baseline and apply CBT delta
   ranges to it.
2. Finalization: after the source VM is powered off, apply one final delta and
   run `virt-v2v` locally against the current KVM-side replica.

This avoids a second full VMware read during cutover. CloudStack does not power
off the source VM; shutdown remains an explicit operator action.

## Components

### Management server

The API commands are:

- `checkVmwareCbtMigrationPrerequisites`
- `startVmwareCbtMigration`
- `listVmwareCbtMigrations`
- `syncVmwareCbtMigration`
- `cutoverVmwareCbtMigration`
- `cancelVmwareCbtMigration`
- `deleteVmwareCbtMigration`

`VmwareCbtMigrationManagerImpl` owns migration orchestration, host and storage
validation, state transitions, cutover policy, agent command dispatch, import,
and cleanup. `VmwareCbtMigrationServiceImpl` performs VMware inventory,
snapshot, power-state, CBT enablement, and changed-area operations.

`VmwareCbtStorageTarget` classifies the selected primary storage and determines
the target representation and finalization constraints.

### Persistence

The feature persists its state in three CloudStack tables:

- `cloud.vmware_cbt_migration`: one row per migration session.
- `cloud.vmware_cbt_migration_disk`: source and target metadata for each disk,
  including the current per-disk VMware `changeId`.
- `cloud.vmware_cbt_migration_cycle`: metrics and status for each delta cycle.

The schema and configuration entries are delivered through the standard
4.23.0.0-to-24.0.0 database upgrade path:

- `engine/schema/src/main/resources/META-INF/db/schema-42300to2400.sql`
- `engine/schema/src/main/java/com/cloud/upgrade/dao/Upgrade42300to2400.java`

### KVM agent

The management server sends these commands to the selected conversion host:

- `VmwareCbtPrepareCommand`: create or resolve target disks and perform the
  initial VDDK full copy.
- `VmwareCbtSyncCommand`: read and apply VMware CBT ranges.
- `VmwareCbtCutoverCommand`: finalize the current replica with `virt-v2v`.
- `VmwareCbtCleanupCommand`: terminate migration-owned processes and remove
  migration-owned temporary artifacts.
- `VmwareCbtRbdProbeCommand`: actively validate RBD create/write/read/delete
  access for the selected pool.

The corresponding KVM wrappers are
`LibvirtVmwareCbtPrepareCommandWrapper`,
`LibvirtVmwareCbtSyncCommandWrapper`,
`LibvirtVmwareCbtCutoverCommandWrapper`,
`LibvirtVmwareCbtCleanupCommandWrapper`, and
`LibvirtVmwareCbtRbdProbeCommandWrapper`.

### User interface

The workflow is integrated into Import/Export Instances. The UI starts async
operations through the API and uses `listVmwareCbtMigrations` as the detailed
source for migration, disk, and cycle state.

## End-to-end flow

```text
VMware source VM
    |
    | preflight and source discovery
    v
baseline VMware snapshot
    |
    | nbdkit-vddk full logical disk read
    v
source-equivalent replica on KVM primary storage
    |
    | repeated VMware snapshots + QueryChangedDiskAreas
    | changed ranges copied through VDDK and patched in place
    v
ReadyForCutover
    |
    | operator powers off the VMware VM
    | final CBT delta
    | local virt-v2v finalization
    v
CloudStack KVM import
```

### Preflight

Preflight resolves the source, destination cluster, conversion host, and
primary storage. It validates source CBT state, source disks, snapshots,
storage support, host block-copy and in-place-finalization capabilities, RBD or
LINSTOR access where applicable, Windows conversion dependencies, and optional
compute-offering sizing.

The selected conversion host reports capability through host details. The
principal gates are:

- `host.vddk.blockcopy.support`
- `host.vddk.blockcopy.inplace.finalization.support`
- `host.vddk.blockcopy.rbd.support`
- `vddk.lib.dir`
- `host.vddk.version`
- qemu and `virt-v2v` version details

### Initial synchronization

Start creates the migration and disk records, enables CBT when required,
creates a baseline VMware snapshot, prepares destination targets, and dispatches
`VmwareCbtPrepareCommand`.

The KVM agent exposes each snapshot disk with `nbdkit-vddk`. It uses
`qemu-img convert` for the general copy path and can use `nbdcopy` as an
optional accelerator for supported pre-created or NBD-bridged targets. The
result remains source-equivalent; `virt-v2v` is not run during initial sync.

After a successful copy, CloudStack records the baseline per-disk `changeId`
and removes the baseline snapshot when possible.

### Delta synchronization

Each synchronization cycle:

1. Creates a VMware snapshot.
2. Calls VMware `QueryChangedDiskAreas` from the previous per-disk `changeId`.
3. Persists the changed ranges and cycle state.
4. Opens the snapshot disk through `nbdkit-vddk` on the KVM host.
5. Copies only the changed ranges and writes them at their original byte
   offsets with `qemu-io`.
6. Records changed bytes, duration, dirty rate, and the new `changeId`.
7. Removes the cycle snapshot when possible.

`VmwareCbtMigrationCutoverPolicy` evaluates the configured minimum, maximum,
and quiet-cycle thresholds and moves the migration to `ReadyForCutover` when
the policy boundary is reached.

### Cutover

Cutover is accepted only when the source VM is powered off. CloudStack runs one
final CBT cycle, then finalizes the current KVM-side replica with one of these
paths:

- `virt-v2v-in-place`;
- `virt-v2v --in-place`; or
- regular `virt-v2v -o local` for QCOW2 file targets only, when the explicit
  non-in-place fallback configuration is enabled.

After finalization, CloudStack imports the disks through the shared external
KVM import path. A successful import sets the migration to `Completed`. If
finalization succeeds but import fails, the migration remains `ReadyForImport`
and a retry repeats only the import stage.

## Storage targets

| Primary storage | Replica | Initial copy | Delta writes | Finalization |
| --- | --- | --- | --- | --- |
| NFS, filesystem, shared mount | QCOW2 file | `qemu-img` | `qemu-io -f qcow2` | In-place preferred; explicitly gated local fallback supported |
| Ceph/RBD | Raw RBD image | Direct qemu RBD path or `nbdcopy` through a local NBD bridge | `qemu-io -f raw` | In-place only |
| LINSTOR | Pre-created raw DRBD block device | `nbdcopy` when available, otherwise `qemu-img` | `qemu-io -f raw` | In-place only |

For filesystem targets, temporary replicas are stored under a migration-owned
`cloudstack-cbt/<migration-uuid>/` directory. RBD and LINSTOR resources use
CloudStack-owned migration markers. Cleanup is restricted to these owned paths
and resource names.

## State model

Migration states:

```text
Created
  -> InitialSync
  -> Replicating
  -> ReadyForCutover
  -> CuttingOver
  -> ReadyForImport
  -> Completed
```

`Failed` and `Cancelled` are terminal states. `ReadyForImport` is deliberately
retryable.

Disk states are `Created`, `Prepared`, `Syncing`, `Ready`, and `Failed`.
Cycle states are `Created`, `QueryingChangedAreas`, `CopyingChangedBlocks`,
`Completed`, and `Failed`.

## Concurrency and cleanup

Sync and cutover commands are serialized by migration ID. Cancellation sends a
best-effort agent cleanup command before marking a non-terminal migration as
cancelled. Failed and cancelled migrations can delete their migration-owned
temporary replicas when cleanup is requested.

Deleting a completed migration is record-only. It removes CBT bookkeeping but
does not delete the imported VM, its CloudStack volumes, or finalized primary
storage artifacts.

VMware snapshots are removed after initial and delta attempts when possible.
Snapshot identifiers and cleanup errors remain visible in migration state so
operators can identify VMware-side cleanup work.

## Credential handling

Existing registered vCenter credentials are resolved from CloudStack's VMware
datacenter record. External-source credentials are accepted by sensitive API
commands and the password is stored through CloudStack's encrypted DAO field
handling until the migration no longer needs it.

On the KVM host, the password is written to a temporary owner-only file and
passed to `nbdkit` with `password=+<file>`. It is not placed directly in the
process command line. Returned errors are sanitized before persistence.

## Operational boundaries

- The operator is responsible for shutting down the source VM before cutover.
- Initial baseline transfer size depends on the extents exposed by VMware VDDK
  and the source datastore; CBT optimization applies to subsequent cycles.
- Invalidated VMware CBT state requires a new baseline migration.
- RBD and LINSTOR require in-place finalization and active backend access from
  the selected conversion host.
- Windows guests require a usable `virtio-win` driver bundle on that host.
- The source VMware VM remains authoritative until cutover succeeds.
