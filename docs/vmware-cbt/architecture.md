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

# VMware CBT incremental migration architecture for Apache CloudStack

## Goal

CloudStack now has VMware-to-KVM migration based on full disk copy through VDDK and
virt-v2v. That is a good foundation, but it still copies the whole VMware disk for
every migration attempt. The next step is a CBT-based replication mode:

1. Create an initial KVM-side disk replica.
2. Use VMware Changed Block Tracking (CBT) to query only changed extents.
3. Read those extents from VMware through VDDK.
4. Apply them to a KVM-side replica.
5. Do a short final sync at cutover and boot the VM on KVM.

The practical target is warm migration and replication-assisted cutover. True
near-live behavior is feasible for byte replication, but the remaining virt-v2v
guest conversion step must be handled carefully because virt-v2v modifies guest
disk contents, not only the container format.

## Existing CloudStack architecture

The merged VDDK work in Apache CloudStack PR
[#12970](https://github.com/apache/cloudstack/pull/12970) extends the existing
VMware-to-KVM import path rather than creating a new subsystem.

Current relevant flow:

- API/UI entrypoint: `ImportVmCmd` / `ImportUnmanagedInstanceCmd`.
- Server orchestration: `UnmanagedVMsManagerImpl`.
- Transport objects: `RemoteInstanceTO`, `ConvertInstanceCommand`,
  `CheckConvertInstanceCommand`.
- KVM agent implementation:
  - `LibvirtComputingResource`
  - `LibvirtCheckConvertInstanceCommandWrapper`
  - `LibvirtConvertInstanceCommandWrapper`
  - `LibvirtReadyCommandWrapper`
- Host capability details:
  - `host.instance.conversion`
  - `host.vddk.support`
  - `vddk.lib.dir`
  - `host.vddk.version`
- Agent properties:
  - `vddk.lib.dir`
  - `vddk.transports`
  - `vddk.thumbprint`
  - `libguestfs.backend`

The VDDK path currently delegates to `virt-v2v -it vddk`, producing qcow2 output
on the KVM conversion host. CloudStack then imports the converted disks into the
destination KVM cluster. This should be treated as the bootstrap path for CBT:
the new feature should reuse host selection, credential handling, import task
tracking, temporary storage selection, and converted-volume import.

For Windows guests, CBT should also reuse the existing import dependency check
instead of inventing a parallel probe. The selected KVM conversion host is
validated with `CheckConvertInstanceCommand` in the same way as OVF/VDDK import
with Windows guest conversion enabled. The CBT source preflight therefore carries
the VMware guest OS identifier and guest OS name only so the manager can decide
whether this Windows-specific `virtio-win` dependency check applies.

## Important design constraint: raw replication vs virt-v2v conversion

CBT extents describe byte ranges changed on the original VMware guest disk. They
can be applied safely to a target disk only if that target is a byte-equivalent
replica of the VMware disk at the same logical offsets.

That is not always true after virt-v2v. Virt-v2v may inject virtio drivers,
rewrite boot configuration, adjust initramfs, install services, and otherwise
modify guest disk contents. Applying later VMware CBT deltas directly on top of a
virt-v2v-converted disk can overwrite some of those modifications or produce a
mixed state that never existed on either source or target.

Recommended architecture:

- Maintain a source-equivalent replica during incremental synchronization.
- Apply VMware CBT deltas only to that replica.
- At cutover, after the final delta is applied, run local virt-v2v conversion
  from the up-to-date replica into the CloudStack import image.

This reduces WAN/vCenter bandwidth and source read time substantially, but the
cutover still includes local conversion time. To reach very low downtime, a later
phase can add a conversion-cache strategy, but the first correct design should
not mix raw source deltas into a post-conversion image.

Possible exception: Linux guests that are already KVM-ready, or source VMs where
virtio drivers are preinstalled and no guest rewrite is required, can use direct
CBT-to-final-disk mode. That should be an explicit advanced mode, not the default.

## High-level architecture

Add a new "VMware CBT migration session" concept beside the existing one-shot
`importVm` flow.

```text
CloudStack management server
  |
  | 1. API orchestration, state, vCenter snapshots, CBT metadata
  v
KVM conversion/replication host agent
  |
  | 2. VDDK or nbdkit-vddk reads changed VMware extents
  | 3. qemu-nbd/libnbd writes changed bytes to KVM-side replica
  v
Primary/temporary storage
  |
  | 4. Final local virt-v2v conversion/import
  v
CloudStack-managed KVM VM
```

Recommended separation:

- Management server owns durable state, CloudStack API jobs, vCenter inventory,
  snapshot orchestration, and final VM registration/import.
- KVM agent owns heavy data movement, local file/block operations, VDDK access,
  qemu-nbd lifecycle, target writes, checksums, and progress reporting.
- A small native helper on the agent should do the block streaming. Java should
  orchestrate it rather than bind directly to VDDK C APIs.

## New CloudStack components

Server-side services/classes:

- `VmwareCbtMigrationManager`
  - Main orchestration service.
  - Starts, syncs, cuts over, cancels, and cleans migration sessions.
- `VmwareCbtSnapshotOrchestrator`
  - Enables CBT if needed.
  - Creates/removes VMware snapshots.
  - Reads per-disk `changeId` values.
  - Calls `QueryChangedDiskAreas`.
- `VmwareCbtDiskMapper`
  - Maps VMware `deviceKey`, controller/unit, VMDK path, and capacity to
    CloudStack volume records and target file paths.
- `VmwareCbtPlanner`
  - Chooses KVM conversion host, target pool, disk format, chunk size, and
    parallelism.
- `VmwareCbtValidationService`
  - Rejects unsupported conditions: independent disks, disk resize during
    migration, missing CBT support, unsupported datastore behavior, mismatched
    disk capacity, inaccessible snapshots.
- `VmwareCbtMigrationDao`
- `VmwareCbtMigrationDiskDao`
- `VmwareCbtSyncRunDao`

Agent-side classes:

- `VmwareCbtPrepareCommand`
- `VmwareCbtSyncCommand`
- `VmwareCbtFinalizeCommand`
- `VmwareCbtCleanupCommand`
- `LibvirtVmwareCbtPrepareCommandWrapper`
- `LibvirtVmwareCbtSyncCommandWrapper`
- `LibvirtVmwareCbtFinalizeCommandWrapper`
- `LibvirtVmwareCbtCleanupCommandWrapper`
- `VmwareCbtSyncHelper`
  - Java wrapper around a native executable such as
    `cloudstack-vmware-cbt-sync`.

Native helper:

- `cloudstack-vmware-cbt-sync`
  - Inputs: JSON manifest, vCenter connection parameters, snapshot moref,
    disk paths/device keys, changed extents, target image path.
  - Outputs: progress JSON lines, bytes copied, failed extent, final checksum
    samples, exit code.
  - Implementation candidates: C/C++ with VDDK and libnbd, Rust with C FFI, or
    Go with cgo. For a PoC, Python/pyvmomi plus nbdkit-vddk/libnbd is acceptable.

## Database/state tracking

Add durable state so a management-server restart does not leave unknown VMware
snapshots or half-written replicas.

`vmware_cbt_migration`:

- `id`, `uuid`
- `account_id`, `domain_id`, `project_id`
- `zone_id`, `destination_cluster_id`
- `source_vcenter_id` or external vCenter endpoint reference
- `source_vm_moref`, `source_instance_uuid`, `source_vm_name`
- `convert_host_id`, `import_host_id`
- `target_pool_id`, `temporary_pool_id`
- `state`: `PREPARING`, `FULL_SYNCING`, `READY_FOR_DELTA`,
  `DELTA_SYNCING`, `READY_FOR_CUTOVER`, `FINAL_SYNCING`, `CONVERTING`,
  `IMPORTING`, `COMPLETED`, `FAILED`, `CANCELLED`, `CLEANUP_REQUIRED`
- `mode`: `REPLICA_THEN_CONVERT`, `DIRECT_TO_KVM_DISK`
- `sync_policy`: `MANUAL`, `SCHEDULED`, `ADAPTIVE`
- `sync_interval_seconds`
- `min_delta_cycles`
- `max_delta_cycles`
- `target_downtime_seconds`
- `target_final_delta_bytes`
- `target_final_delta_seconds`
- `non_convergence_threshold_percent`
- `quiesce_snapshots`
- `latest_dirty_rate_bytes_per_second`
- `latest_copy_rate_bytes_per_second`
- `estimated_final_delta_seconds`
- `estimated_final_cutover_seconds`
- `readiness`: `NOT_READY`, `READY`, `NON_CONVERGING`, `NEEDS_OPERATOR`
- `last_error`
- `created`, `updated`, `completed`

`vmware_cbt_migration_disk`:

- `id`, `migration_id`
- `source_device_key`
- `source_controller_key`, `source_unit_number`
- `source_vmdk_path`
- `source_capacity_bytes`
- `source_backing_uuid` / `contentId` when available
- `target_replica_path`
- `target_final_path`
- `target_format`: `raw`, `qcow2`, or block-device-backed raw
- `current_change_id`
- `current_change_uuid`
- `last_snapshot_moref`
- `bytes_full_synced`
- `bytes_delta_synced`
- `last_synced_at`
- `state`

`vmware_cbt_sync_run`:

- `id`, `migration_id`, `generation`
- `snapshot_moref`
- `start_change_id`
- `end_change_id`
- `state`
- `bytes_planned`, `bytes_copied`
- `extents_planned`, `extents_completed`
- `query_seconds`, `read_seconds`, `write_seconds`, `flush_seconds`
- `snapshot_create_seconds`, `snapshot_remove_seconds`
- `dirty_rate_bytes_per_second`
- `copy_rate_bytes_per_second`
- `estimated_remaining_delta_seconds`
- `started`, `finished`, `error`

Avoid storing every CBT extent in the database for large VMs. Store the current
sync run and a manifest file path. If a run fails, repeat the whole generation
from the previous durable `changeId`.

## Snapshot orchestration flow

### Preparation

1. Discover the VMware VM and all disks.
2. Validate that the disk set will not change during migration.
3. Enable CBT if disabled.
4. Record VM hardware metadata needed by CloudStack import.
5. Select a KVM replication/conversion host with VDDK, nbdkit, qemu-img,
   qemu-nbd, and libnbd support.
6. Create target replica disks with exact virtual sizes.

### Initial full sync

Preferred correctness-first flow:

1. Create VMware snapshot `cloudstack-cbt-baseline-N`.
2. Read each disk through VDDK at that snapshot.
3. Write a source-equivalent raw/qcow2 replica on KVM storage.
4. Extract each disk's `changeId` from the snapshot backing info.
5. Remove the snapshot after the full copy succeeds.
6. Persist `current_change_id` per disk.

The initial full sync can use VDDK directly, nbdkit-vddk, or a local VDDK helper.
It should not use virt-v2v as the canonical replica if future CBT deltas will be
applied to the same disk.

### Repeated delta sync

For each sync generation:

1. Create VMware snapshot `cloudstack-cbt-delta-N`.
2. For each disk, call `QueryChangedDiskAreas(snapshot, deviceKey, startOffset,
   previousChangeId)` until the whole virtual disk is covered.
3. Coalesce adjacent or near-adjacent extents.
4. Agent reads those byte ranges from the snapshot via VDDK.
5. Agent writes those byte ranges to the KVM-side replica at the same offsets.
6. Agent flushes target writes.
7. Server extracts and persists the new snapshot `changeId` for the next
   generation.
8. Remove the VMware snapshot.

### Final cutover

1. Require the operator to power off the VMware source VM.
2. Because the VM is now powered off, use current disk state as the final CBT end
   point when supported; otherwise create a final snapshot.
3. Query changed extents from the last persisted `changeId`.
4. Apply final changed extents to the replica.
5. Run local virt-v2v conversion from the final source-equivalent replica to
   CloudStack KVM disk images.
6. Import/register the VM in CloudStack using the existing import pipeline.
7. Boot the KVM VM.
8. Keep the VMware VM powered off and renamed/tagged until operator acceptance.
9. Cleanup old VMware snapshots and temporary replica/conversion artifacts.

## Delta synchronization pipeline

Pipeline per disk:

```text
QueryChangedDiskAreas
  -> extent normalization
  -> chunk planner
  -> VDDK read
  -> optional checksum/sample verification
  -> qemu-nbd/libnbd write
  -> flush
  -> progress update
```

Mapping rules:

- VMware CBT returns byte offsets and lengths.
- VDDK reads sectors; VDDK sector size is 512 bytes, so reads must be aligned to
  sector boundaries.
- Target writes use the same logical byte offsets.
- For raw files or block devices, `pwrite` is sufficient if CloudStack controls
  exclusive access.
- For qcow2, prefer qemu-nbd plus libnbd so writes go through QEMU's qcow2 block
  driver rather than corrupting qcow2 metadata.
- Coalesce small extents into larger chunks, for example 4 MiB to 64 MiB, while
  preserving sector alignment.
- Always flush after a disk generation and before persisting the new `changeId`.

## Delta cycle policy and cutover readiness

The number of delta cycles should not be hard-coded. CloudStack should support
manual, scheduled, and adaptive policies.

Manual policy:

- Operator starts the initial full sync.
- Operator clicks `sync now` one or more times.
- CloudStack reports dirty rate, copy throughput, and estimated cutover time.
- Operator decides when to cut over.

Scheduled policy:

- CloudStack runs delta sync every configured interval.
- The session remains operator-driven for final cutover.
- This is useful when teams want the VM kept warm before a maintenance window.

Adaptive policy:

- CloudStack runs at least `min_delta_cycles`.
- After every cycle it recomputes readiness.
- It stops or marks `READY_FOR_CUTOVER` when estimated final downtime fits the
  requested target.
- It marks `NON_CONVERGING` when the VM writes faster than CloudStack can copy,
  or when repeated cycles do not materially reduce the estimated final delta.

Readiness formula:

```text
estimated_cutover_time =
  estimated_final_delta_sync_time
  + estimated_local_virt_v2v_time
  + estimated_import_registration_time
  + configured_safety_margin
```

Important metrics per cycle:

- `changed_bytes`
- `elapsed_since_previous_sync`
- `dirty_rate = changed_bytes / elapsed_since_previous_sync`
- `copy_rate = bytes_copied / delta_sync_elapsed_seconds`
- `snapshot_overhead = snapshot_create + snapshot_remove`
- `replication_margin = copy_rate - dirty_rate`
- `estimated_final_delta_sync_time = latest_changed_bytes / copy_rate`

The cutover decision should be configurable but metric-based:

- `target_downtime_seconds`, for example 1800 seconds
- `target_final_delta_bytes`, for example 2 GiB
- `target_final_delta_seconds`, for example 300 seconds
- `min_delta_cycles`, for example 2
- `max_delta_cycles`, for example 10
- `non_convergence_threshold_percent`, for example dirty rate above 80 percent
  of sustained copy rate

Current implementation settings:

- `vmware.cbt.migration.min.cycles`
- `vmware.cbt.migration.max.cycles`
- `vmware.cbt.migration.quiet.cycles`
- `vmware.cbt.migration.quiet.bytes`
- `vmware.cbt.migration.quiet.dirty.rate`
- `vmware.cbt.migration.agent.command.timeout`

`vmware.cbt.migration.agent.command.timeout` is the long-running KVM agent
command timeout in seconds. It applies to initial full sync, regular delta
sync, final delta sync, and cutover finalization. The default is 86400 seconds.

For quiet VMs, one or two delta cycles may be enough. For write-heavy database
VMs, CloudStack should not keep creating endless snapshots. It should report that
the VM is not converging and recommend operator action: choose a quieter window,
stop services, throttle writes, accept longer downtime, or perform a final
powered-off sync.

Only one CloudStack-owned VMware snapshot should be active per cycle. The system
should persist the new per-disk `changeId` after each successful cycle and then
remove the snapshot. A long snapshot chain is not the state model.

## Tooling choices

### qemu-nbd

Useful to expose a target qcow2/raw image as an NBD export for writes. The agent
can start qemu-nbd with `--cache=none`, a Unix socket, explicit format, and a
single client. This avoids direct qcow2 file mutation.

### libnbd

Best low-level writer for the helper. It supports offset writes and flushes
cleanly and avoids shelling out for each extent.

### qcow2 dirty bitmaps

Not required for VMware-source replication, because VMware CBT is the source of
truth before cutover. They become useful in two cases:

- test-booting a KVM candidate with an overlay and tracking target-side writes;
- future reverse replication or post-cutover backup integration.

Persistent bitmaps require qcow2; raw images can use transient QEMU bitmaps only
while a QEMU process is alive.

### qemu-img

Useful for create, resize, check, convert, and final image validation. It is not
the right primitive for applying many small changed ranges.

### libvirt blockcopy APIs

Useful after the VM is on KVM, for CloudStack-side storage movement or if a
staging KVM domain is introduced. It does not solve reading changed blocks from
VMware. If used, prefer blockcopy flags that force convergence/synchronous write
propagation where appropriate.

### nbdkit-vddk

Good PoC option because it can expose VMware disks through VDDK as NBD and
already understands vCenter, VM morefs, snapshot morefs, thumbprints, and VDDK
transport selection. Production still needs strict lifecycle handling and tests
against snapshot chains.

## Consistency concerns

- Running Linux imports are generally crash-consistent unless a quiesced VMware
  snapshot is used.
- Windows should use VSS/quiesced snapshots where possible and final graceful
  shutdown for correctness.
- Final cutover should prefer powered-off final sync. This gives the cleanest
  current-disk end point and avoids writes racing with the last delta.
- Guest-level iSCSI or application-managed remote disks are invisible to VMware
  CBT because those writes are not processed as virtual disk writes by ESXi.
- VMware snapshot creation can stun the VM; snapshot deletion/consolidation can
  create backend load and should be rate-limited.
- If the source disk is resized, added, removed, or reconfigured during a
  session, fail the session and require a new full sync.
- If the CBT `changeId` UUID changes, assume the CBT chain is invalid and require
  a full resync.

## Snapshot cleanup

CloudStack must own all snapshots it creates with unique names and metadata:

- `cloudstack-cbt-<migrationUuid>-baseline`
- `cloudstack-cbt-<migrationUuid>-delta-<generation>`
- `cloudstack-cbt-<migrationUuid>-final`

Cleanup rules:

- Remove a snapshot only after all disk writes for that generation have flushed
  and the new per-disk `changeId` values are persisted.
- On failure, mark `CLEANUP_REQUIRED` and expose a retry cleanup API.
- At startup, the management server should scan for CloudStack-owned CBT
  snapshots and reconcile them with DB state.
- Never delete snapshots not matching the migration UUID.

## Rollback scenarios

- Before final cutover: source VM remains authoritative. The KVM replica can be
  discarded and rebuilt.
- During final sync failure: leave source VM powered off if final consistency is
  unknown; operator can either retry final sync or explicitly power source back
  on.
- After KVM boot failure: keep source VM powered off by default to avoid
  split-brain. Allow controlled rollback by destroying the KVM VM and powering
  VMware back on.
- After successful cutover: keep VMware VM renamed/tagged for a retention period,
  then delete or archive by operator action.

## Performance bottlenecks

- `QueryChangedDiskAreas` may return many small extents. Coalesce them.
- NBD/NBDSSL VDDK transport can saturate ESXi management networking or CPU.
- Snapshot consolidation can become the dominant cost on busy VMs.
- qcow2 random writes are slower than raw/block device writes.
- Multi-disk VMs need parallelism, but per-vCenter and per-host concurrency must
  be capped.
- Full local virt-v2v conversion after final delta may dominate downtime for
  complex Windows VMs.

Mitigations:

- Parallelize across disks, not unlimited extents.
- Use larger read/write chunks.
- Prefer raw/block target replicas for the sync phase.
- Keep one active VMware snapshot per VM only as long as needed.
- Expose dirty-rate estimates to operators before cutover.

## VMware CBT limitations

Known practical limitations to design around:

- CBT must be enabled before the snapshot/change interval that will be queried.
- Existing snapshots created before CBT was enabled may not have usable
  `changeId` values.
- `QueryChangedDiskAreas("*")` reports allocated areas for initial discovery, not
  necessarily application-level changed data.
- CBT can fail or return overly broad ranges on unsupported datastore scenarios.
- VMware documentation warns that CBT is meaningful primarily when the ESXi
  storage stack can track the virtual disk writes.
- Broadcom documents cases where `QueryChangedDiskAreas` can return `FileFault`
  and require CBT reset.
- Broadcom also documents an ESXi 8.0U2 issue where CBT could return incorrect
  sectors after hot-extending a VMDK; the safe policy is to reject disk resize
  during migration and require full resync after resize.

## UI integration strategy

CBT should start inside the existing VMware-to-KVM import experience, but it
should not disrupt the existing import form. The current UI already has many
operator controls for conversion host, import host, staging storage, direct
storage-pool conversion, Management Server OVF download, guest OS, compute
offering, network mapping, and MAC behavior. CBT should build on that entrypoint
because operators are already choosing the same source vCenter, source VM,
destination cluster, service offering, disk offerings, networks, and storage.

The existing VDDK work already adds `Use VDDK` to
`ui/src/views/tools/ImportUnmanagedInstance.vue` and exposes host VDDK support in
`HostInfo.vue`. The safest UI evolution is to make the migration method explicit
near the current `Use VDDK` area, while preserving the existing advanced controls
and their conditional behavior.

Recommended first UI design:

- Add a compact migration method control in the existing import form, near the
  current `Use VDDK` toggle:
  - `OVF / ovftool full copy`
  - `VDDK full copy`
  - `CBT warm migration`
- Do not create a separate CBT wizard for the initial implementation.
- Preserve existing conversion/import host selection and storage selection
  controls.
- Keep `Enable to force OVF Download via Management Server` visible only for the
  OVF method.
- Keep guest OS, compute offering, network mapping, duplicate MAC, and new MAC
  behavior unchanged across all methods.
- Preserve the current VDDK behavior where direct storage-pool conversion is
  automatically enabled because VDDK writes directly to the selected destination
  storage instead of using temporary OVF staging.
- When `CBT warm migration` is selected, show CBT-specific settings:
  - sync policy: manual, scheduled, adaptive
  - sync interval
  - target downtime
  - minimum and maximum delta cycles
  - quiesced snapshot preference
  - final cutover shutdown policy
- Submitting the wizard should create a CBT migration session, not immediately
  block the wizard until final import completes.

Suggested method-to-parameter mapping:

- `OVF / ovftool full copy`
  - `usevddk=false`
  - `forcemstoimportvmfiles` remains available
  - conversion host, import host, and staging storage remain optional as today
- `VDDK full copy`
  - `usevddk=true`
  - `forceconverttopool=true`
  - `forcemstoimportvmfiles=false`
  - import host selection is hidden or ignored when the conversion host is also
    the destination writer
- `CBT warm migration`
  - `usevddk=true`
  - `forceconverttopool=true`
  - creates a `VmwareCbtMigration` session instead of running one-shot import
  - selected storage becomes the replica/final destination storage
  - final import still runs local virt-v2v at cutover

Recommended session UI:

- Add a dedicated view under `Tools > Migration` or `Tools > VMware CBT
  migrations`.
- The view lists active and completed sessions with state, source VM, target
  cluster, last sync time, dirty rate, copied bytes, estimated downtime, and
  readiness.
- The session detail page provides actions:
  - `Sync now`
  - `Pause schedule`
  - `Resume schedule`
  - `Cut over`
  - `Cancel`
  - `Cleanup`
- The detail page should show per-disk progress and warnings, especially
  snapshot cleanup failures, CBT reset requirements, and non-convergence.

Suggested UI modules:

- Extend `ui/src/views/tools/ImportUnmanagedInstance.vue` with an inline
  migration method control and CBT policy fields.
- Add `ui/src/views/tools/VmwareCbtMigrations.vue` for the session list.
- Add `ui/src/views/tools/VmwareCbtMigrationDetail.vue` for progress, metrics,
  actions, and warnings.
- Add API metadata and labels for CBT policy fields, readiness, dirty rate, copy
  rate, estimated cutover time, and non-convergence.
- Extend host details display to show CBT helper capability after the KVM agent
  reports it, similar to the current VDDK support display.
- Disable or clearly reject KVM conversion hosts that do not fully support the
  selected method. For VDDK/CBT, `host.vddk.support=false` should not be
  silently selectable even if a related VDDK or nbdkit version string is present.
- Keep unsupported hosts visible only if that helps operators understand why
  auto-selection is not available; otherwise filter them out of the selectable
  list.

This hybrid approach is better than hiding CBT entirely inside the current import
form. The form is still the right place to define migration intent, but a
long-running replication session needs its own operational surface. It also keeps
room for future bulk migration, scheduled cutover windows, and retry/cleanup
workflows.

## Suggested API contracts

### Start session

`startVmwareCbtMigration`

Inputs:

- `zoneid`
- `destinationclusterid`
- `sourcevcenterid` or external vCenter connection details
- `sourcevmmoref` or `name`
- `serviceofferingid`
- `datadiskofferinglist`
- `networkmapping`
- `targetpoolid`
- `migrationmethod`: `ovfFullCopy`, `vddkFullCopy`, or `cbtWarmMigration`
- `mode`: `replicaThenConvert` or `directToKvmDisk`
- `quiesce`: boolean
- `syncintervalminutes` optional
- `syncpolicy`: `manual`, `scheduled`, or `adaptive`
- `targetdowntimeseconds`
- `mindeltacycles`
- `maxdeltacycles`
- `targetfinaldeltabytes`
- `targetfinaldeltaseconds`
- `details`: `vddk.lib.dir`, `vddk.transports`, `vddk.thumbprint`, chunk size,
  parallelism

Output:

- `migrationid`
- selected host/pool
- disk map
- initial state
- readiness estimate

### Sync now

`syncVmwareCbtMigration`

Inputs:

- `migrationid`
- `generation` optional

Output:

- bytes planned/copied
- extents planned/copied
- new lag estimate
- dirty rate
- copy rate
- estimated final delta time
- readiness
- state

### Update policy

`updateVmwareCbtMigrationPolicy`

Inputs:

- `migrationid`
- `syncpolicy`
- `syncintervalminutes`
- `targetdowntimeseconds`
- `mindeltacycles`
- `maxdeltacycles`
- `targetfinaldeltabytes`
- `targetfinaldeltaseconds`

Output:

- updated policy
- updated readiness estimate

### Cutover

`cutoverVmwareCbtMigration`

Inputs:

- `migrationid`
- `shutdownpolicy`: `guestShutdown`, `powerOff`, `manualAlreadyStopped`
- `timeout`
- `bootafterimport`
- `rollbackretentionhours`

Output:

- CloudStack VM ID
- final sync stats
- conversion stats
- cleanup state

### Cancel/cleanup

`cancelVmwareCbtMigration`

Inputs:

- `migrationid`
- `deleteReplica`
- `removeSnapshots`

## Suggested agent command contract

`VmwareCbtPrepareCommand`:

- create target replica files/devices
- validate qemu-nbd/libnbd/VDDK availability
- return helper versions and target paths

`VmwareCbtSyncCommand`:

- vCenter endpoint and credential token/password file reference
- VM moref
- snapshot moref or current-disk mode
- per-disk manifest: source VMDK path/device key, target path, extents
- target format
- chunk size and concurrency

`VmwareCbtSyncAnswer`:

- success/failure
- per-disk bytes copied
- failed disk/extent
- elapsed time
- helper logs path
- checksum samples if enabled

`VmwareCbtFinalizeCommand`:

- run local virt-v2v from replica into CloudStack import location, or skip if
  direct-to-KVM mode was selected
- return converted disk paths and metadata for existing import code

## Phased implementation plan

### Phase 0: standalone PoC

- Build an external helper that can:
  - create a VMware snapshot;
  - call `QueryChangedDiskAreas`;
  - read changed blocks via VDDK or nbdkit-vddk;
  - write to a local raw file;
  - repeat a delta sync;
  - verify selected block hashes.
- Use local config only. Do not integrate with CloudStack DB yet.
- Validate with one Linux VM and one Windows VM.

### Phase 1: CloudStack-managed full replica

- Add migration session DB tables.
- Add API start/list/cancel/sync.
- Add agent prepare/sync commands.
- Implement initial full sync and repeated delta sync to raw replica.
- Extend the existing import form with the inline migration method control and
  CBT policy fields.
- Add basic session list/detail UI with `sync now` and `cancel`.
- No final CloudStack import yet.

### Phase 2: cutover and import

- Add final powered-off sync.
- Run local virt-v2v from the synced replica.
- Reuse the existing converted-disk import path in `UnmanagedVMsManagerImpl`.
- Add `cut over`, rollback visibility, and cleanup actions to the session UI.
- Implement rollback and cleanup.

### Phase 3: operational hardening

- Add adaptive sync policy, dirty-rate estimate, cutover readiness, and
  non-convergence warnings.
- Add snapshot reconciliation after management-server restart.
- Add per-vCenter concurrency limits.
- Add automatic fallback to full resync on invalid CBT chain.
- Add Marvin/integration tests for failure paths.

### Phase 4: near-live optimization

- Explore direct-to-KVM mode for KVM-ready guests.
- Explore preinstalling virtio drivers/tools before migration.
- Explore conversion-cache/overlay strategies to reduce final local virt-v2v
  time.
- Explore target-side qcow2 dirty bitmaps for isolated test boot and reverse
  replication.

## Practical PoC recommendation

Start outside CloudStack with a helper and one test VM:

1. Enable CBT and verify the VM has no old snapshots.
2. Create baseline snapshot.
3. Full-copy disk through nbdkit-vddk to raw replica.
4. Store baseline `changeId`.
5. Generate guest writes.
6. Create delta snapshot.
7. Query changed areas from baseline `changeId`.
8. Read only changed extents and patch the raw replica.
9. Compare random block hashes between VMware snapshot and replica.
10. Power off source, final sync, then run local virt-v2v from replica to qcow2.
11. Boot converted VM on isolated KVM network.

This proves the hardest part first: CBT extent correctness and target patching.
CloudStack integration should come after that works repeatably.

## References

- Apache CloudStack VDDK PR:
  <https://github.com/apache/cloudstack/pull/12970>
- Apache CloudStack VMware-to-KVM import documentation:
  <https://docs.cloudstack.apache.org/en/latest/adminguide/virtual_machines/importing_vmware_vms_into_kvm.html>
- Broadcom vSphere `QueryChangedDiskAreas` API:
  <https://developer.broadcom.com/xapis/virtual-infrastructure-json-api/latest/sdk/vim25/release/VirtualMachine/moId/QueryChangedDiskAreas/post/>
- Broadcom Virtual Disk API disk operations:
  <https://developer.broadcom.com/xapis/virtual-disk-api/latest/vddkFunctions.6.4.html>
- Broadcom VDDK CBT notes and limitations:
  <https://developer.broadcom.com/xapis/virtual-disk-api/latest/vddkBkupVadp.9.5.html>
- QEMU dirty bitmaps:
  <https://www.qemu.org/docs/master/interop/bitmaps.html>
- QEMU qemu-nbd:
  <https://www.qemu.org/docs/master/tools/qemu-nbd.html>
- libvirt incremental backup internals:
  <https://libvirt.org/kbase/internals/incremental-backup.html>
- libvirt backup XML:
  <https://libvirt.org/formatbackup.html>
- libvirt checkpoint XML:
  <https://www.libvirt.org/formatcheckpoint.html>
- nbdkit VDDK plugin:
  <https://libguestfs.org/nbdkit-vddk-plugin.1.html>
