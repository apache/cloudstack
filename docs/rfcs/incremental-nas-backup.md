# RFC: Incremental NAS Backup Support for KVM Hypervisor

| Field         | Value                                      |
|---------------|--------------------------------------------|
| **Author**    | James Peru, Xcobean Systems Limited        |
| **Status**    | Draft                                      |
| **Created**   | 2026-03-27                                 |
| **Target**    | Apache CloudStack 4.23+                    |
| **Component** | Backup & Recovery (NAS Backup Provider)    |

---

## Summary

This RFC proposes adding incremental backup support to CloudStack's NAS backup provider for KVM hypervisors. By leveraging QEMU dirty bitmaps and libvirt's `backup-begin` API, CloudStack can track changed disk blocks between backups and export only the delta, reducing daily backup storage consumption by 80--95% and shortening backup windows from hours to minutes for large VMs. The feature is opt-in at the zone level, backward-compatible with existing full-backup behavior, and gracefully degrades on older QEMU/libvirt versions.

---

## Motivation

CloudStack's NAS backup provider currently performs a full disk copy every time a backup is taken. For a 500 GB VM with five daily backups retained, that amounts to 2.5 TB of storage consumed. At scale -- tens or hundreds of VMs -- this becomes a serious operational and financial burden.

**Problems with the current approach:**

1. **Storage waste.** Every backup is a full copy of the entire virtual disk, regardless of how little data actually changed since the last backup.
2. **Long backup windows.** Copying hundreds of gigabytes over NFS or SMB takes hours, increasing the risk of I/O contention on production workloads.
3. **Network bandwidth pressure.** Full-disk transfers saturate the storage network during backup windows, impacting other VMs on the same host.
4. **Uncompetitive feature set.** VMware (Changed Block Tracking / VADP), Proxmox Backup Server, and Veeam all support incremental backups natively. CloudStack's lack of incremental backup is a common complaint on the users@ mailing list and a blocker for adoption in environments with large VMs.

**What incremental backup achieves:**

- Only changed blocks are transferred and stored after the initial full backup.
- A typical daily incremental for a 500 GB VM with moderate write activity is 5--15 GB, a reduction of 97--99% compared to a full copy.
- Backup completes in minutes rather than hours.
- Retention of 30+ daily restore points becomes economically feasible.

---

## Proposed Design

### Backup Chain Model

Incremental backups form a chain anchored by a periodic full backup:

```
Full (Day 0) -> Inc 1 (Day 1) -> Inc 2 (Day 2) -> ... -> Inc 6 (Day 6) -> Full (Day 7) -> ...
```

Restoring to any point in time requires the full backup plus every incremental up to the desired restore point. To bound restore complexity and protect against chain corruption, a new full backup is forced at a configurable interval.

**Global settings (zone scope):**

| Setting                              | Type    | Default | Description                                          |
|--------------------------------------|---------|---------|------------------------------------------------------|
| `nas.backup.incremental.enabled`     | Boolean | `false` | Enable incremental backup for the zone               |
| `nas.backup.full.interval`           | Integer | `7`     | Days between full backups                             |
| `nas.backup.incremental.max.chain`   | Integer | `6`     | Max incremental backups before forcing a new full     |

When `nas.backup.incremental.enabled` is `false` (the default), behavior is identical to today -- every backup is a full copy. Existing deployments are unaffected.

---

### Technical Approach

#### 1. Dirty Bitmap Tracking (QEMU Layer)

QEMU 4.0 introduced persistent dirty bitmaps: per-disk bitmaps that record which blocks have been written since the bitmap was created. These bitmaps survive QEMU restarts (they are stored in the qcow2 image header) and are the foundation for incremental backup.

**Lifecycle:**

1. When incremental backup is enabled for a VM, the agent creates a persistent dirty bitmap on each virtual disk via QMP:
   ```json
   {
     "execute": "block-dirty-bitmap-add",
     "arguments": {
       "node": "drive-virtio-disk0",
       "name": "backup-20260327",
       "persistent": true
     }
   }
   ```
2. QEMU automatically sets bits in this bitmap whenever the guest writes to a block.
3. At backup time, the bitmap tells the backup process exactly which blocks to read.
4. After a successful backup, a new bitmap is created for the next cycle and the old bitmap is optionally removed.

#### 2. Backup Flow

**Full backup (Day 0 or every `nas.backup.full.interval` days):**

```bash
# 1. Export the entire disk to the NAS mount
qemu-img convert -f qcow2 -O qcow2 \
  /var/lib/libvirt/images/vm-disk.qcow2 \
  /mnt/nas/backups/vm-uuid/backup-full-20260327.qcow2

# 2. Create a new dirty bitmap to track changes from this point
virsh qemu-monitor-command $DOMAIN --hmp \
  'block-dirty-bitmap-add drive-virtio-disk0 backup-20260327 persistent=true'
```

**Incremental backup (Day 1 through Day N):**

```bash
# 1. Use libvirt backup-begin with incremental mode
# This exports only blocks dirty since bitmap "backup-20260327"
cat > /tmp/backup.xml <<'XML'
<domainbackup mode="push">
  <disks>
    <disk name="vda" backup="yes" type="file">
      <target file="/mnt/nas/backups/vm-uuid/backup-inc-20260328.qcow2"
              type="qcow2"/>
      <driver type="qcow2"/>
    </disk>
  </disks>
  <incremental>backup-20260327</incremental>
</domainbackup>
XML

virsh backup-begin $DOMAIN /tmp/backup.xml

# 2. Wait for completion
virsh domjobinfo $DOMAIN --completed

# 3. Rotate bitmaps: remove old, create new
virsh qemu-monitor-command $DOMAIN --hmp \
  'block-dirty-bitmap-remove drive-virtio-disk0 backup-20260327'
virsh qemu-monitor-command $DOMAIN --hmp \
  'block-dirty-bitmap-add drive-virtio-disk0 backup-20260328 persistent=true'
```

**New full backup cycle (Day 7):**

```bash
# Remove all existing bitmaps
virsh qemu-monitor-command $DOMAIN --hmp \
  'block-dirty-bitmap-remove drive-virtio-disk0 backup-20260327'

# Take a full backup (same as Day 0)
# Optionally prune expired chains from NAS
```

#### 3. Restore Flow

Restoring from an incremental chain requires replaying the full backup plus all incrementals up to the target restore point. This is handled entirely within `nasbackup.sh` and is transparent to the management server and the end user.

**Example: Restore to Day 3 (full + 3 incrementals):**

```bash
# 1. Create a working copy from the full backup
cp /mnt/nas/backups/vm-uuid/backup-full-20260327.qcow2 /tmp/restored.qcow2

# 2. Apply each incremental in order using qemu-img rebase
#    Each incremental is a thin qcow2 containing only changed blocks.
#    Rebasing merges the incremental's blocks into the chain.
qemu-img rebase -u -b /tmp/restored.qcow2 \
  /mnt/nas/backups/vm-uuid/backup-inc-20260328.qcow2

qemu-img rebase -u -b /mnt/nas/backups/vm-uuid/backup-inc-20260328.qcow2 \
  /mnt/nas/backups/vm-uuid/backup-inc-20260329.qcow2

qemu-img rebase -u -b /mnt/nas/backups/vm-uuid/backup-inc-20260329.qcow2 \
  /mnt/nas/backups/vm-uuid/backup-inc-20260330.qcow2

# 3. Flatten the chain into a single image
qemu-img convert -f qcow2 -O qcow2 \
  /mnt/nas/backups/vm-uuid/backup-inc-20260330.qcow2 \
  /tmp/vm-restored-final.qcow2

# 4. Return the flattened image for CloudStack to import
```

An alternative approach uses `qemu-img commit` to merge each layer down. The implementation will benchmark both methods and choose the faster one for large images.

#### 4. Database Schema Changes

**Modified table: `backups`**

| Column             | Type         | Description                                    |
|--------------------|--------------|------------------------------------------------|
| `backup_type`      | VARCHAR(16)  | `FULL` or `INCREMENTAL`                        |
| `parent_backup_id` | BIGINT (FK)  | For incremental: ID of the previous backup     |
| `bitmap_name`      | VARCHAR(128) | QEMU dirty bitmap identifier for this backup   |
| `chain_id`         | BIGINT (FK)  | Links to the backup chain this backup belongs to |

**New table: `backup_chains`**

| Column            | Type        | Description                                    |
|-------------------|-------------|------------------------------------------------|
| `id`              | BIGINT (PK) | Auto-increment primary key                     |
| `vm_instance_id`  | BIGINT (FK) | The VM this chain belongs to                   |
| `full_backup_id`  | BIGINT (FK) | The full backup anchoring this chain           |
| `state`           | VARCHAR(16) | `ACTIVE`, `SEALED`, `EXPIRED`                  |
| `created`         | DATETIME    | When the chain was started                     |

**Schema migration** will be provided as a Liquibase changeset, consistent with CloudStack's existing migration framework. The new columns are nullable to maintain backward compatibility with existing backup records.

#### 5. Management Server Changes

**`BackupManagerImpl` (orchestration):**

- Before taking a backup, query the active chain for the VM.
- If no active chain exists, or the chain has reached `nas.backup.incremental.max.chain` incrementals, or `nas.backup.full.interval` days have elapsed since the last full backup: schedule a full backup and start a new chain.
- Otherwise: schedule an incremental backup linked to the previous backup in the chain.
- On backup failure: if the bitmap is suspected corrupt, mark the chain as `SEALED` and force a full backup on the next run.

**`NASBackupProvider.takeBackup()`:**

- Accept a new parameter `BackupType` (FULL or INCREMENTAL).
- For incremental: pass the parent backup's bitmap name and NAS path to the agent command.

**`TakeBackupCommand` / `TakeBackupAnswer`:**

- Add fields: `backupType` (FULL/INCREMENTAL), `parentBackupId`, `bitmapName`, `parentBackupPath`.
- The answer includes the actual size of the backup (important for incrementals, which are much smaller than the disk size).

**`RestoreBackupCommand`:**

- Add field: `backupChain` (ordered list of backup paths from full through the target incremental).
- The agent reconstructs the full image from the chain before importing.

#### 6. KVM Agent Changes

**`LibvirtTakeBackupCommandWrapper`:**

- For `FULL` backups: existing behavior (qemu-img convert), plus create the initial dirty bitmap.
- For `INCREMENTAL` backups: use `virsh backup-begin` with `<incremental>` XML, then rotate bitmaps.
- Pre-flight check: verify QEMU version >= 4.0 and libvirt version >= 6.0. If not met, fall back to full backup and log a warning.

**`nasbackup.sh` enhancements:**

- New flag `-i` for incremental mode.
- New flag `-p <parent_path>` to specify the parent backup on the NAS.
- New flag `-b <bitmap_name>` to specify which dirty bitmap to use.
- New subcommand `restore-chain` that accepts an ordered list of backup paths and produces a flattened image.

**`LibvirtRestoreBackupCommandWrapper`:**

- If the restore target is an incremental backup, request the full chain from the management server and pass it to `nasbackup.sh restore-chain`.

#### 7. API Changes

**Existing API: `createBackup`**

No change to the API signature. The management server automatically decides full vs. incremental based on the zone configuration and the current chain state. Callers do not need to specify the backup type.

**Existing API: `listBackups`**

Response gains two new fields:
- `backuptype` (string): `Full` or `Incremental`
- `parentbackupid` (string): UUID of the parent backup (null for full backups)

**Existing API: `restoreBackup`**

No change. The management server resolves the full chain internally.

#### 8. UI Changes

- **Backup list view:** Add a "Type" column showing `Full` or `Incremental`, with a visual indicator (e.g., a small chain icon for incrementals).
- **Backup detail view:** Show the backup chain as a vertical timeline: full backup at the top, incrementals branching down, with sizes and timestamps.
- **Restore dialog:** When the user selects an incremental restore point, display a note: "This restore will replay N backups (total chain size: X GB)."
- **Backup schedule settings** (zone-level): Toggle for incremental backup, full backup interval slider, max chain length input.

---

### Storage Savings Projections

The following estimates assume a moderate write workload (2--5% of disk blocks changed per day), which is typical for application servers, databases with WAL, and file servers.

| Scenario                          | Full Backups Only | With Incremental   | Savings    |
|-----------------------------------|-------------------|--------------------|------------|
| 500 GB VM, 7 daily backups       | 3.5 TB            | ~550 GB            | **84%**    |
| 1 TB VM, 30 daily backups        | 30 TB             | ~1.3 TB            | **96%**    |
| 100 VMs x 100 GB, weekly cycle   | 70 TB/week        | ~12 TB/week        | **83%**    |
| 50 VMs x 200 GB, 30-day retain   | 300 TB            | ~18 TB             | **94%**    |

For environments with higher change rates (e.g., heavy database writes), incremental sizes will be larger, but savings still typically exceed 60%.

---

### Requirements

| Requirement             | Minimum Version | Notes                                          |
|-------------------------|----------------|-------------------------------------------------|
| QEMU                    | 4.0+           | Dirty bitmap support. Ubuntu 20.04+, RHEL 8+.  |
| libvirt                 | 6.0+           | `virsh backup-begin` support. Ubuntu 22.04+, RHEL 8.3+. |
| CloudStack              | 4.19+          | NAS backup provider must already be present.    |
| NAS storage             | NFS or SMB     | No special requirements beyond existing NAS backup support. |

**Graceful degradation:** If a KVM host runs QEMU < 4.0 or libvirt < 6.0, the agent will detect this at startup and report `incrementalBackupCapable=false` to the management server. Backups for VMs on that host will remain full-only, with a warning logged. No manual intervention is required.

---

### Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Bitmap corruption** (host crash during backup, QEMU bug) | Incremental backup produces an incomplete or incorrect image | Detect bitmap inconsistency via QMP query; force a new full backup and start a fresh chain. Data in previous full backup is unaffected. |
| **Chain too long** (missed full backup schedule) | Restore time increases; single corrupt link breaks the chain | Enforce `nas.backup.incremental.max.chain` hard limit. If exceeded, the next backup is automatically a full. |
| **Restore complexity** | User confusion about which backup to pick; longer restore for deep chains | Restore logic is fully automated in `nasbackup.sh`. The UI shows a single "Restore" button per restore point, with the chain replayed transparently. |
| **VM live migration during backup** | Dirty bitmap may be lost if migrated mid-backup | Check VM state before backup; abort and retry if migration is in progress. Bitmaps persist across clean shutdowns and restarts but not across live migration in older QEMU versions. For QEMU 6.2+, bitmaps survive migration. |
| **Backward compatibility** | Existing full-backup users should not be affected | Feature is disabled by default. No schema changes affect existing rows (new columns are nullable). Full-backup code path is unchanged. |
| **Disk space during restore** | Flattening a chain requires temporary disk space equal to the full disk size | Use the same scratch space already used for full backup restores. Document the requirement. |

---

### Implementation Plan

| Phase | Scope | Estimated Effort |
|-------|-------|------------------|
| **Phase 1** | Core incremental backup and restore in `nasbackup.sh` and KVM agent wrappers. Dirty bitmap lifecycle management. Manual testing with `virsh` and `qemu-img`. | 2--3 weeks |
| **Phase 2** | Management server changes: chain management, scheduling logic, global settings, database schema migration, API response changes. | 2 weeks |
| **Phase 3** | UI changes: backup type column, chain visualization, restore dialog enhancements, zone-level settings. | 1 week |
| **Phase 4** | Integration testing (full cycle: enable, backup, restore, disable, upgrade from older version). Edge case testing (host crash, bitmap loss, migration, mixed QEMU versions). Documentation. | 2 weeks |

**Total estimated effort: 7--8 weeks.**

We (Xcobean Systems) intend to implement this and submit PRs against the `main` branch. We would appreciate early design feedback before starting implementation to avoid rework.

---

### Prior Art

- **VMware VADP / Changed Block Tracking (CBT):** VMware's CBT is the industry-standard approach. A change tracking driver inside the hypervisor records changed blocks, and backup vendors query the CBT via the vSphere API. This RFC's approach is analogous, using QEMU dirty bitmaps as the CBT equivalent.

- **Proxmox Backup Server (PBS):** PBS uses QEMU dirty bitmaps to implement incremental backups natively. Their implementation validates that the dirty bitmap approach is production-ready for KVM/QEMU environments. PBS has been stable since Proxmox VE 6.4 (2020).

- **Veeam Backup & Replication:** Veeam uses a "reverse incremental" model where the most recent backup is always a synthetic full, and older backups are stored as reverse deltas. This simplifies restore (always restore from the latest full) at the cost of more I/O during backup. We chose the forward-incremental model for simplicity and because it aligns with how QEMU dirty bitmaps work natively.

- **libvirt backup API:** The `virsh backup-begin` command and its underlying `virDomainBackupBegin()` API were specifically designed for this use case. The libvirt documentation includes examples of incremental backup using dirty bitmaps. See: https://libvirt.org/kbase/incremental-backup.html

---

### About the Author

Xcobean Systems Limited operates a production Apache CloudStack deployment providing IaaS to 50+ client VMs. We use the NAS backup provider daily and have contributed several improvements to it:

- PR [#12805](https://github.com/apache/cloudstack/pull/12805) -- NAS backup NPE fix
- PR [#12822](https://github.com/apache/cloudstack/pull/12822) -- Backup restore improvements
- PR [#12826](https://github.com/apache/cloudstack/pull/12826) -- NAS backup script hardening
- PRs [#12843](https://github.com/apache/cloudstack/pull/12843)--[#12848](https://github.com/apache/cloudstack/pull/12848) -- Various NAS backup fixes
- PR [#12872](https://github.com/apache/cloudstack/pull/12872) -- Additional backup provider fixes

We experience the storage and bandwidth cost of full-only backups firsthand and are motivated to solve this problem upstream rather than maintaining a fork.

---

## Open Questions for Discussion

We welcome feedback from the community on the following:

1. **Interest level.** Is there sufficient demand for this feature to justify the implementation effort? We believe so based on mailing list threads, but would like confirmation.

2. **Dirty bitmaps vs. alternatives.** Are there concerns about relying on QEMU dirty bitmaps? Alternative approaches include file-level deduplication on the NAS (less efficient, not hypervisor-aware) or `qemu-img compare` (slower, requires reading both images).

3. **Target release.** Should this target CloudStack 4.23, or is a later release more appropriate given the scope?

4. **Chain model.** We proposed forward-incremental with periodic full backups. Would the community prefer a different model (e.g., reverse-incremental like Veeam, or forever-incremental with periodic synthetic fulls)?

5. **Scope of first PR.** Should we submit the entire feature as one PR, or break it into smaller PRs (e.g., nasbackup.sh changes first, then agent, then management server, then UI)?

6. **Testing infrastructure.** We can test against our production environment (Ubuntu 22.04, QEMU 6.2, libvirt 8.0). Are there CI environments or community test labs available for broader testing (RHEL, Rocky, older QEMU versions)?

---

*This RFC is posted as a GitHub Discussion to gather community feedback before implementation begins. Please share your thoughts, concerns, and suggestions.*

---

## Appendix: Related Proposal — CloudStack Infrastructure Backup to NAS

### Problem
CloudStack's NAS backup provider only backs up VM disks. The management server database, agent configurations, SSL certificates, and global settings are not backed up. If the management server fails, all metadata is lost unless someone manually configured mysqldump.

### Proposed Solution
Add a new scheduled task that automatically backs up CloudStack infrastructure to the same NAS backup storage.

**What gets backed up:**
| Component | Method | Size |
|-----------|--------|------|
| CloudStack database (`cloud`, `cloud_usage`) | mysqldump | ~50-500MB |
| Management server config (`/etc/cloudstack/management/`) | tar | <1MB |
| Agent configs (`/etc/cloudstack/agent/`) | tar | <1MB |
| SSL certificates and keystores | tar | <1MB |
| Global settings export | SQL dump | <1MB |

**Configuration:**
- `nas.infra.backup.enabled` (global, default: false)
- `nas.infra.backup.schedule` (cron expression, default: `0 2 * * *` — daily at 2am)
- `nas.infra.backup.retention` (number of backups to keep, default: 7)

**Implementation:**
- New class: `InfrastructureBackupTask` extending `ManagedContextRunnable`
- Runs on management server (not KVM agent)
- Uses existing NAS mount point from backup storage pool
- Creates timestamped directory: `infra-backup/2026-03-27/`
- Runs `mysqldump --single-transaction` for hot backup
- Tars config directories
- Manages retention (delete backups older than N days)
- Logs to CloudStack events for audit trail

**Restore:** 
- Manual via CLI: `mysql cloud < backup.sql` + extract config tars
- Future: one-click restore from UI

This is a much simpler change (~200 lines of Java) that addresses a real operational gap. Could target 4.22.1 or 4.23.

