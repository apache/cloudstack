// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.backup;

import com.cloud.agent.AgentManager;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.configuration.Resource;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.DiskOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiServiceImpl;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.user.ResourceLimitService;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.BackupRepositoryDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.cloudstack.backup.BackupManager.BackupFrameworkEnabled;

public class NASBackupProvider extends AdapterBase implements BackupProvider, Configurable {
    private static final Logger LOG = LogManager.getLogger(NASBackupProvider.class);

    private static final int CHAIN_LOCK_TIMEOUT_SECONDS = 300;

    /**
     * Prefix of the RBD snapshots this provider creates as chain anchors. Deliberately distinct
     * from the names CloudStack volume snapshots use, so an operator (and the agent's cleanup) can
     * tell a backup anchor apart from a user snapshot on the same image.
     */
    private static final String RBD_SNAPSHOT_PREFIX = "csbackup-";

    ConfigKey<Integer> NASBackupRestoreMountTimeout = new ConfigKey<>("Advanced", Integer.class,
            "nas.backup.restore.mount.timeout",
            "30",
            "Timeout in seconds after which backup repository mount for restore fails.",
            true,
            BackupFrameworkEnabled.key());

    ConfigKey<Boolean> NASBackupIncrementalEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "nas.backup.incremental.enabled",
            "false",
            "Master switch for NAS incremental backups. Defaults to false so existing zones keep the " +
                    "legacy full-only behavior on upgrade; opt in per-zone when ready to use chains. " +
                    "When false, every NAS backup is taken as a full regardless of nas.backup.full.every. " +
                    "Toggling this is safe at any time: switching off forces the next backup to be a fresh " +
                    "full anchor (existing chains stay restorable), switching back on resumes incrementals " +
                    "on the next full + incremental cycle.",
            true,
            ConfigKey.Scope.Zone,
            BackupFrameworkEnabled.key());

    ConfigKey<Integer> NASBackupFullEvery = new ConfigKey<>("Advanced", Integer.class,
            "nas.backup.full.every",
            "10",
            "Take a full NAS backup every Nth backup; remaining backups in between are incremental. " +
                    "Counts backups, not days, so it works for hourly, daily, and ad-hoc schedules. " +
                    "Set to 1 to disable incrementals (every backup is full).",
            true,
            ConfigKey.Scope.Zone,
            NASBackupIncrementalEnabled.key());

    @Inject
    private BackupDao backupDao;

    @Inject
    private BackupRepositoryDao backupRepositoryDao;

    @Inject
    private BackupRepositoryService backupRepositoryService;

    @Inject
    private HostDao hostDao;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private StoragePoolHostDao storagePoolHostDao;

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;

    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;

    @Inject
    DataStoreManager dataStoreMgr;

    @Inject
    private AgentManager agentManager;

    @Inject
    private ResourceLimitService resourceLimitMgr;

    @Inject
    private VMSnapshotDao vmSnapshotDao;

    @Inject
    private VMSnapshotDetailsDao vmSnapshotDetailsDao;

    @Inject
    BackupManager backupManager;

    @Inject
    ResourceManager resourceManager;

    @Inject
    private DiskOfferingDao diskOfferingDao;

    @Inject
    private BackupDetailsDao backupDetailsDao;

    private Long getClusterIdFromRootVolume(VirtualMachine vm) {
        VolumeVO rootVolume = volumeDao.getInstanceRootVolume(vm.getId());
        StoragePoolVO rootDiskPool = primaryDataStoreDao.findById(rootVolume.getPoolId());
        if (rootDiskPool == null) {
            return null;
        }
        return rootDiskPool.getClusterId();
    }

    protected Host getVMHypervisorHost(VirtualMachine vm) {
        Long hostId = vm.getLastHostId();
        Long clusterId = null;

        if (hostId != null) {
            Host host = hostDao.findById(hostId);
            if (host.getStatus() == Status.Up) {
                return host;
            }
            // Try to find any Up host in the same cluster
            clusterId = host.getClusterId();
        } else {
            // Try to find any Up host in the same cluster as the root volume
            clusterId = getClusterIdFromRootVolume(vm);
        }

        if (clusterId != null) {
            for (final Host hostInCluster : hostDao.findHypervisorHostInCluster(clusterId)) {
                if (hostInCluster.getStatus() == Status.Up) {
                    LOG.debug("Found Host {} in cluster {}", hostInCluster, clusterId);
                    return hostInCluster;
                }
            }
        }

        // Try to find any Host in the zone
        return resourceManager.findOneRandomRunningHostByHypervisor(Hypervisor.HypervisorType.KVM, vm.getDataCenterId());
    }

    /**
     * Returned by {@link #decideChain(VirtualMachine)} to describe the next backup's place in
     * the chain: full vs incremental, how it is taken, the bitmap name to create, and (for
     * incrementals) the parent bitmap and parent file path.
     */
    static final class ChainDecision {
        final String mode;            // "full" or "incremental"
        final String method;          // NASBackupChainKeys.METHOD_QCOW2, METHOD_CONTENT or METHOD_RBD
        // On-NAS format of the chain this backup belongs to: NASBackupChainKeys.FORMAT_QCOW2 for
        // the libvirt-checkpoint path, FORMAT_RBD_DIFF for the Ceph-native rbd export-diff path.
        final String format;
        final String bitmapNew;
        final String bitmapParent;    // the anchor to read changes since; null for a full
        // Per-volume parent backup file paths, one per current VM volume in deviceId order.
        // null/empty for full. Each volume needs its own parent file because backup files
        // are named after each volume's own UUID (root.<uuid>.qcow2 / datadisk.<uuid>.qcow2).
        final List<String> parentPaths;
        final String chainId;         // chain identifier this backup belongs to
        final int chainPosition;      // 0 for full, N for the Nth incremental in the chain

        private ChainDecision(String mode, String method, String format, String bitmapNew, String bitmapParent,
                              List<String> parentPaths, String chainId, int chainPosition) {
            this.mode = mode;
            this.method = method;
            this.format = format;
            this.bitmapNew = bitmapNew;
            this.bitmapParent = bitmapParent;
            this.parentPaths = parentPaths;
            this.chainId = chainId;
            this.chainPosition = chainPosition;
        }

        static ChainDecision fullStart(String bitmapName) {
            return new ChainDecision(NASBackupChainKeys.TYPE_FULL, NASBackupChainKeys.METHOD_QCOW2,
                    NASBackupChainKeys.FORMAT_QCOW2, bitmapName, null, null, UUID.randomUUID().toString(), 0);
        }

        /**
         * Full anchor of an RBD chain: the agent takes a fresh {@code rbd snap} named
         * {@code snapshotName} and exports the whole image with {@code rbd export-diff}. Starting a
         * chain also sweeps any older backup anchors off the images, so an abandoned chain (a
         * deleted backup, an unassigned offering, a restore) cannot leave snapshots behind.
         */
        static ChainDecision rbdFullStart(String snapshotName) {
            return new ChainDecision(NASBackupChainKeys.TYPE_FULL, NASBackupChainKeys.METHOD_RBD,
                    NASBackupChainKeys.FORMAT_RBD_DIFF, snapshotName, null, null, UUID.randomUUID().toString(), 0);
        }

        /**
         * Incremental of an RBD chain: {@code rbd export-diff --from-snap parentSnapshot} against a
         * freshly created {@code snapshotName}. No parent file paths — unlike qcow2 backups, the
         * diff stream is not rebased onto its parent, the chain is replayed in order on restore.
         */
        static ChainDecision rbdIncremental(String snapshotName, String parentSnapshot, String chainId, int chainPosition) {
            return new ChainDecision(NASBackupChainKeys.TYPE_INCREMENTAL, NASBackupChainKeys.METHOD_RBD,
                    NASBackupChainKeys.FORMAT_RBD_DIFF, snapshotName, parentSnapshot, null, chainId, chainPosition);
        }

        /**
         * Decision used when the incremental feature is disabled: a plain full backup that
         * creates no bitmap and carries no chain identity, so nothing chain/checkpoint-related
         * is sent to the agent or persisted. Keeps the feature-off path byte-for-byte legacy.
         */
        static ChainDecision legacyFull() {
            return new ChainDecision(NASBackupChainKeys.TYPE_LEGACY_FULL, NASBackupChainKeys.METHOD_QCOW2,
                    NASBackupChainKeys.FORMAT_QCOW2, null, null, null, null, 0);
        }

        static ChainDecision incremental(String bitmapNew, String bitmapParent, List<String> parentPaths,
                                         String chainId, int chainPosition) {
            return new ChainDecision(NASBackupChainKeys.TYPE_INCREMENTAL, NASBackupChainKeys.METHOD_QCOW2,
                    NASBackupChainKeys.FORMAT_QCOW2, bitmapNew, bitmapParent, parentPaths, chainId, chainPosition);
        }

        boolean isIncremental() {
            return NASBackupChainKeys.TYPE_INCREMENTAL.equals(mode);
        }

        boolean isLegacyFull() {
            return NASBackupChainKeys.TYPE_LEGACY_FULL.equals(mode);
        }

        boolean isRbd() {
            return NASBackupChainKeys.METHOD_RBD.equals(method);
        }

        /**
         * The same decision re-cast as the start of a brand new chain, keeping the anchor the agent
         * actually created and the storage flavour. Used when the agent reports it could not
         * proceed as an incremental and completed a full instead.
         */
        ChainDecision asFullRestart() {
            return isRbd() ? rbdFullStart(bitmapNew) : fullStart(bitmapNew);
        }
    }

    /**
     * Which incremental mechanism, if any, a VM's volumes can support.
     */
    protected enum IncrementalFlavor {
        /** Every volume is on file-based qcow2 storage: libvirt checkpoints / QEMU dirty bitmaps. */
        QCOW2,
        /** Every volume is on Ceph/RBD: RBD snapshots + {@code rbd export-diff}. */
        RBD,
        /** Mixed or otherwise incapable storage — the VM stays on the full-only (legacy) path. */
        UNSUPPORTED
    }

    /**
     * Decides whether the next backup for {@code vm} should be a fresh full or an incremental
     * appended to the existing chain. Stopped VMs are always full (libvirt {@code backup-begin}
     * requires a running QEMU process). The {@code nas.backup.full.every} ConfigKey controls
     * how many backups (full + incrementals) form one chain before a new full is forced.
     *
     * <p>The decision is anchored on the VM's {@code nas.active_checkpoint_id} detail, which
     * records the bitmap that currently exists on the running QEMU. After a restore that
     * detail is cleared, so the next backup is automatically full — even though there may be
     * a more recent "last backup taken" row in the database. The decision deliberately avoids
     * relying on "last backup taken", because that row is misleading after a restore.</p>
     */
    protected ChainDecision decideChain(VirtualMachine vm) {
        // Master switch — when the operator disables incrementals at the zone level the backup
        // behaves exactly like the pre-incremental full-only path: no bitmap is generated and no
        // chain/checkpoint metadata is created, sent to the agent, or persisted (legacy-full).
        Boolean incrementalEnabled = NASBackupIncrementalEnabled.valueIn(vm.getDataCenterId());
        if (incrementalEnabled == null || !incrementalEnabled) {
            return ChainDecision.legacyFull();
        }

        // Pick the incremental mechanism that matches the VM's storage. Mixed or incapable
        // storage stays on the full-only (legacy) path.
        IncrementalFlavor flavor = getIncrementalFlavor(vm);
        if (IncrementalFlavor.UNSUPPORTED.equals(flavor)) {
            return ChainDecision.legacyFull();
        }
        final boolean rbd = IncrementalFlavor.RBD.equals(flavor);

        final String newAnchor = (rbd ? RBD_SNAPSHOT_PREFIX : "backup-") + System.currentTimeMillis() / 1000L;

        // A QEMU dirty bitmap needs a live QEMU process, so a stopped VM always takes a full.
        // RBD snapshots are taken inside Ceph and work exactly the same whether the VM runs or
        // not, so RBD chains keep their cadence across a stop/start.
        if (!rbd && VirtualMachine.State.Stopped.equals(vm.getState())) {
            return ChainDecision.fullStart(newAnchor);
        }

        // The VM's active_checkpoint_id records the anchor that currently exists on the storage:
        // the QEMU bitmap for qcow2 chains, the RBD snapshot for RBD chains. Null means there is
        // nothing to base an incremental on (first backup, post-restore, freshly assigned offering).
        String activeCheckpoint = readVmActiveCheckpoint(vm.getId());

        Integer fullEvery = NASBackupFullEvery.valueIn(vm.getDataCenterId());
        if (fullEvery == null || fullEvery <= 1) {
            // Disabled or every-backup-is-full mode.
            return fullDecision(rbd, newAnchor);
        }

        // 1. No anchor on the storage => nothing to take an incremental against.
        if (activeCheckpoint == null) {
            return fullDecision(rbd, newAnchor);
        }

        // 2. The most-recent BackedUp backup is the only safe parent — after restore the
        //    next backup is always a fresh full, so anything older has a rotated-out anchor.
        //    If the latest backup's anchor doesn't match the VM's active_checkpoint_id, the
        //    chain is broken: force a full.
        Backup parent = findLatestBackedUpBackup(vm.getId());
        if (parent == null || !activeCheckpoint.equals(readDetail(parent, NASBackupChainKeys.BITMAP_NAME))) {
            LOG.debug("VM {} latest backup does not match active_checkpoint_id={} — forcing full",
                    vm.getInstanceName(), activeCheckpoint);
            return fullDecision(rbd, newAnchor);
        }

        // A chain cannot change storage flavour halfway through: the on-NAS files of a qcow2 chain
        // and of an RBD chain are not interchangeable. A volume migration between pool types
        // therefore starts a new chain in the new format.
        String parentFormat = chainFormatOf(parent);
        String requiredFormat = rbd ? NASBackupChainKeys.FORMAT_RBD_DIFF : NASBackupChainKeys.FORMAT_QCOW2;
        if (!requiredFormat.equals(parentFormat)) {
            LOG.debug("VM {} parent backup {} is a {} chain but the VM's storage now needs {} — forcing full",
                    vm.getInstanceName(), parent.getUuid(), parentFormat, requiredFormat);
            return fullDecision(rbd, newAnchor);
        }

        String parentChainId = readDetail(parent, NASBackupChainKeys.CHAIN_ID);
        int parentChainPosition = chainPosition(parent);
        if (parentChainId == null || parentChainPosition == Integer.MAX_VALUE) {
            return fullDecision(rbd, newAnchor);
        }

        // Force a fresh full when the chain has reached the configured length.
        if (parentChainPosition + 1 >= fullEvery) {
            return fullDecision(rbd, newAnchor);
        }

        // The script needs the parent backup's on-NAS file path PER VOLUME so it can rebase
        // each new qcow2 onto the matching parent. The paths are stored relative to the NAS
        // mount root — the script resolves them inside its mount session. When alignment
        // fails (volume count changed, etc.) compose returns null and we fall back to full
        // so we don't risk corrupting the chain. RBD chains don't rebase, but the same call
        // still validates that the parent's volume layout still matches the VM's.
        List<String> parentPaths = composeParentBackupPaths(parent, vm.getId(), requiredFormat);
        if (parentPaths == null) {
            LOG.debug("VM {} parent backup {} volume layout no longer matches current VM — forcing full",
                    vm.getInstanceName(), parent.getUuid());
            return fullDecision(rbd, newAnchor);
        }
        return rbd
                ? ChainDecision.rbdIncremental(newAnchor, activeCheckpoint, parentChainId, parentChainPosition + 1)
                : ChainDecision.incremental(newAnchor, activeCheckpoint, parentPaths, parentChainId, parentChainPosition + 1);
    }

    /** Start of a fresh chain in the format the VM's storage calls for. */
    private ChainDecision fullDecision(boolean rbd, String newAnchor) {
        return rbd ? ChainDecision.rbdFullStart(newAnchor) : ChainDecision.fullStart(newAnchor);
    }

    /**
     * The on-NAS format of {@code backup}'s chain. Backups taken before chain formats existed (and
     * legacy fulls) carry no detail and are qcow2.
     */
    private String chainFormatOf(Backup backup) {
        String format = readDetail(backup, NASBackupChainKeys.CHAIN_FORMAT);
        return format == null ? NASBackupChainKeys.FORMAT_QCOW2 : format;
    }

    /**
     * Decide which incremental mechanism a VM's volumes can support.
     *
     * <p>{@link IncrementalFlavor#QCOW2} needs QEMU dirty bitmaps / libvirt checkpoints, which only
     * exist on file-based qcow2 storage: HOST-scope local, {@code SharedMountPoint} or
     * {@code NetworkFilesystem} (NFS) pools.</p>
     *
     * <p>{@link IncrementalFlavor#RBD} uses Ceph's own primitives instead — an {@code rbd snap} per
     * cycle plus {@code rbd export-diff --from-snap}, computed inside Ceph from the image's
     * object-map/fast-diff. RBD images are raw and have nowhere to persist a QEMU bitmap, so they
     * could never join a qcow2 chain, but they need no host-side checkpoint at all.</p>
     *
     * <p>The two mechanisms produce different on-NAS files and cannot be mixed inside one backup,
     * so a VM whose volumes span both (or sits on any other pool type, e.g. Linstor) is reported
     * {@link IncrementalFlavor#UNSUPPORTED} and stays on the legacy full-only path. A volume whose
     * pool can no longer be resolved is treated as incapable (safe default).</p>
     */
    protected IncrementalFlavor getIncrementalFlavor(VirtualMachine vm) {
        List<VolumeVO> volumes = volumeDao.findByInstance(vm.getId());
        if (CollectionUtils.isEmpty(volumes)) {
            return IncrementalFlavor.QCOW2;
        }
        IncrementalFlavor flavor = null;
        for (VolumeVO volume : volumes) {
            StoragePoolVO pool = primaryDataStoreDao.findById(volume.getPoolId());
            if (pool == null) {
                LOG.debug("VM {} volume {} has no resolvable storage pool — forcing legacy full",
                        vm.getInstanceName(), volume.getUuid());
                return IncrementalFlavor.UNSUPPORTED;
            }
            IncrementalFlavor volumeFlavor;
            if (ScopeType.HOST.equals(pool.getScope())
                    || Storage.StoragePoolType.SharedMountPoint.equals(pool.getPoolType())
                    || Storage.StoragePoolType.NetworkFilesystem.equals(pool.getPoolType())) {
                volumeFlavor = IncrementalFlavor.QCOW2;
            } else if (Storage.StoragePoolType.RBD.equals(pool.getPoolType())) {
                volumeFlavor = IncrementalFlavor.RBD;
            } else {
                LOG.debug("VM {} volume {} is on {} (scope {}) which supports neither checkpoints nor "
                                + "RBD snapshots — forcing legacy full",
                        vm.getInstanceName(), volume.getUuid(), pool.getPoolType(), pool.getScope());
                return IncrementalFlavor.UNSUPPORTED;
            }
            if (flavor == null) {
                flavor = volumeFlavor;
            } else if (!flavor.equals(volumeFlavor)) {
                LOG.debug("VM {} has volumes on both checkpoint-capable and RBD storage; a single backup "
                        + "cannot mix the two mechanisms — forcing legacy full", vm.getInstanceName());
                return IncrementalFlavor.UNSUPPORTED;
            }
        }
        return flavor;
    }

    /**
     * Read the {@code nas.active_checkpoint_id} VM detail. Returns {@code null} when no detail
     * exists (post-restore, first backup, or after explicit reset).
     */
    private String readVmActiveCheckpoint(long vmId) {
        VMInstanceDetailVO d = vmInstanceDetailsDao.findDetail(vmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID);
        if (d == null) {
            return null;
        }
        String v = d.getValue();
        return StringUtils.isBlank(v) ? null : v;
    }

    /**
     * Locate the most-recent {@code BackedUp} backup for {@code vmId}. The chain invariant
     * guarantees the latest backup is the only valid incremental parent — after restore the
     * next backup is always a fresh full, and {@link #decideChain} checks the bitmap matches.
     */
    private Backup findLatestBackedUpBackup(long vmId) {
        List<Backup> history = backupDao.listByVmId(null, vmId);
        if (CollectionUtils.isEmpty(history)) {
            return null;
        }
        return history.stream()
                .filter(b -> Backup.Status.BackedUp.equals(b.getStatus()))
                .max(Comparator.comparing(Backup::getDate))
                .orElse(null);
    }

    private String readDetail(Backup backup, String key) {
        BackupDetailVO d = backupDetailsDao.findDetail(backup.getId(), key);
        return d == null ? null : d.getValue();
    }

    /**
     * Compose the on-NAS path of EVERY parent backup file (one per VM volume) in the same
     * order the script will iterate the current VM's disks (deviceId asc). Relative to the
     * NAS mount, matches the layout written by {@code nasbackup.sh}:
     *   first  disk -> {@code <backupPath>/root.<volUuid>.<extension>}
     *   others      -> {@code <backupPath>/datadisk.<volUuid>.<extension>}
     *
     * Returns {@code null} if the parent's stored volume count doesn't match the current VM's
     * volume count. Volume attach/detach is blocked while a VM is assigned to a backup offering;
     * if the offering was removed and re-assigned the active checkpoint is cleared in
     * {@link #removeVMFromBackupOffering}, so this method doesn't need to revalidate volume
     * identities — a count mismatch is the only way to reach this branch with a non-null
     * active_checkpoint_id.
     */
    private List<String> composeParentBackupPaths(Backup parent, long vmId, String extension) {
        // backupPath is stored as externalId by createBackupObject — e.g.
        // "i-2-1234-VM/2026.04.27.13.45.00".
        String dir = parent.getExternalId();
        if (StringUtils.isBlank(dir)) {
            LOG.debug("Parent backup {} has no backup path recorded; cannot compose parent backup paths", parent.getUuid());
            return null;
        }

        List<Backup.VolumeInfo> parentVols = parent.getBackedUpVolumes();
        if (CollectionUtils.isEmpty(parentVols)) {
            LOG.debug("Parent backup {} has no backed up volumes recorded; cannot compose parent backup paths", parent.getUuid());
            return null;
        }

        List<VolumeVO> currentVols = volumeDao.findByInstance(vmId);
        if (CollectionUtils.isEmpty(currentVols) || currentVols.size() != parentVols.size()) {
            LOG.debug("VM id={} currently has {} volume(s) but parent backup {} recorded {}; cannot compose parent backup paths",
                    vmId, currentVols == null ? 0 : currentVols.size(), parent.getUuid(), parentVols.size());
            return null;
        }

        // parentVols is in deviceId order at the time the parent was taken. The script names the
        // per-disk files from the volume PATH basename (root.<path>.qcow2 / datadisk.<path>.qcow2,
        // see nasbackup.sh: volUuid="${fullpath##*/}"). Use getPath(), NOT getUuid(): after a
        // volume migration the uuid and the on-disk path diverge, and the backup file is named by
        // path — a uuid-based parent path then fails to resolve for the incremental (test 13).
        List<String> paths = new ArrayList<>(parentVols.size());
        for (int i = 0; i < parentVols.size(); i++) {
            String volPath = parentVols.get(i).getPath();
            String prefix = (i == 0) ? "root" : "datadisk";
            paths.add(dir + "/" + prefix + "." + volPath + "." + extension);
        }
        return paths;
    }

    /**
     * Persist chain metadata under backup_details. Stored here (not on the backups table) so
     * other providers can implement their own chain semantics without schema changes.
     */
    private void persistChainMetadata(Backup backup, ChainDecision decision, String bitmapFromAgent) {
        // Only persist nas.bitmap_name when the agent confirmed the bitmap exists on the host.
        // The agent wrapper sets bitmapFromAgent=null when nasbackup.sh exits
        // EXIT_BITMAP_NOT_SEEDED (=22) — currently only the stopped-VM path where qemu-img
        // bitmap --add failed on every source disk. Anchoring the next incremental on a
        // bitmap that doesn't exist would force a non-recoverable failure, so we leave the
        // detail empty and let the next backup start a fresh full chain.
        if (bitmapFromAgent != null && !bitmapFromAgent.isEmpty()) {
            backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.BITMAP_NAME, bitmapFromAgent, true));
        }
        backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.CHAIN_ID, decision.chainId, true));
        // Restore has to know how to read the files back: rebased qcow2, or an ordered replay of
        // rbd export-diff streams. Only recorded for RBD chains — no detail means qcow2.
        if (NASBackupChainKeys.FORMAT_RBD_DIFF.equals(decision.format)) {
            backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.CHAIN_FORMAT,
                    NASBackupChainKeys.FORMAT_RBD_DIFF, true));
        }
        backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.CHAIN_POSITION,
                String.valueOf(decision.chainPosition), true));
        // Backup full-vs-incremental type lives on backup.type (set by takeBackup) — single
        // source of truth. Not duplicated into backup_details.
        if (decision.isIncremental()) {
            // Resolve the parent backup's UUID so restore can walk the chain by id, not by path.
            String parentUuid = lookupParentBackupUuid(backup.getVmId(), decision.bitmapParent);
            if (parentUuid != null) {
                backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.PARENT_BACKUP_ID, parentUuid, true));
            }
        }
    }

    /**
     * Upsert the VM's {@code nas.active_checkpoint_id} detail to {@code bitmapName}. Called
     * after every successful backup so the next backup's parent-bitmap decision is anchored
     * on what actually exists on QEMU, not on "last backup taken".
     */
    private void upsertVmActiveCheckpoint(long vmId, String bitmapName) {
        VMInstanceDetailVO existing = vmInstanceDetailsDao.findDetail(vmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID);
        if (existing == null) {
            vmInstanceDetailsDao.addDetail(vmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID, bitmapName, false);
            return;
        }
        existing.setValue(bitmapName);
        vmInstanceDetailsDao.update(existing.getId(), existing);
    }

    /**
     * Remove the VM's {@code nas.active_checkpoint_id} detail. Called from the restore paths:
     * after restore the disk image has no QEMU bitmap attached, so any future incremental
     * would be based on stale state. Clearing forces the next backup to be a fresh full.
     */
    private void clearVmActiveCheckpoint(long vmId) {
        VMInstanceDetailVO existing = vmInstanceDetailsDao.findDetail(vmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID);
        if (existing != null) {
            vmInstanceDetailsDao.removeDetail(vmId, NASBackupChainKeys.VM_ACTIVE_CHECKPOINT_ID);
            LOG.debug("Cleared nas.active_checkpoint_id for VM id={} (was {})", vmId, existing.getValue());
        }
    }

    private String lookupParentBackupUuid(long vmId, String parentBitmap) {
        if (parentBitmap == null) {
            return null;
        }
        for (Backup b : backupDao.listByVmId(null, vmId)) {
            String bm = readDetail(b, NASBackupChainKeys.BITMAP_NAME);
            if (parentBitmap.equals(bm)) {
                return b.getUuid();
            }
        }
        return null;
    }

    protected Host getVMHypervisorHostForBackup(VirtualMachine vm) {
        Long hostId = vm.getHostId();
        if (hostId == null && VirtualMachine.State.Running.equals(vm.getState())) {
            throw new CloudRuntimeException(String.format("Unable to find the hypervisor host for %s. Make sure the virtual machine is running", vm.getName()));
        }
        if (VirtualMachine.State.Stopped.equals(vm.getState())) {
            hostId = vm.getLastHostId();
        }
        if (hostId == null) {
            throw new CloudRuntimeException(String.format("Unable to find the hypervisor host for stopped VM: %s", vm));
        }
        final Host host = hostDao.findById(hostId);
        if (host == null || !Status.Up.equals(host.getStatus()) || !Hypervisor.HypervisorType.KVM.equals(host.getHypervisorType())) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        }
        return host;
    }

    @Override
    public Pair<Boolean, Backup> takeBackup(final VirtualMachine vm, Boolean quiesceVM, boolean isolated) {
        final Host host = getVMHypervisorHostForBackup(vm);

        final BackupRepository backupRepository = backupRepositoryDao.findByBackupOfferingId(vm.getBackupOfferingId());
        if (backupRepository == null) {
            throw new CloudRuntimeException("No valid backup repository found for the VM, please check the attached backup offering");
        }

        if (CollectionUtils.isNotEmpty(vmSnapshotDao.findByVmAndByType(vm.getId(), VMSnapshot.Type.DiskAndMemory))) {
            logger.debug("NAS backup provider cannot take backups of a VM [{}] with disk-and-memory VM snapshots. Restoring the backup will corrupt any newer disk-and-memory " +
                    "VM snapshots.", vm);
            throw new CloudRuntimeException(String.format("Cannot take backup of VM [%s] as it has disk-and-memory VM snapshots.", vm.getUuid()));
        }

        final Date creationDate = new Date();
        final String backupPath = String.format("%s/%s", vm.getInstanceName(),
                new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(creationDate));

        // Decide full vs incremental for this backup. Stopped VMs are always full
        // (libvirt backup-begin requires a running QEMU process).
        ChainDecision decision = decideChain(vm);

        BackupVO backupVO = createBackupObject(vm, backupPath, decision.isIncremental() ? "INCREMENTAL" : "FULL");
        TakeBackupCommand command = new TakeBackupCommand(vm.getInstanceName(), backupPath);
        command.setBackupRepoType(backupRepository.getType());
        command.setBackupRepoAddress(backupRepository.getAddress());
        command.setMountOptions(backupRepository.getMountOptions());
        command.setQuiesce(quiesceVM);
        command.setMode(decision.mode);
        command.setMethod(decision.method);
        command.setBitmapNew(decision.bitmapNew);
        command.setBitmapParent(decision.bitmapParent);
        command.setParentPaths(decision.parentPaths);

        // The agent needs the per-volume pool + path to reach the disks itself whenever it cannot
        // read them out of a running libvirt domain: for a stopped VM, and for every RBD backup
        // (the RBD path drives `rbd` against the images directly, running or not).
        if (VirtualMachine.State.Stopped.equals(vm.getState()) || decision.isRbd()) {
            List<VolumeVO> vmVolumes = volumeDao.findByInstance(vm.getId());
            vmVolumes.sort(Comparator.comparing(Volume::getDeviceId));
            Pair<List<PrimaryDataStoreTO>, List<String>> volumePoolsAndPaths = getVolumePoolsAndPaths(vmVolumes);
            command.setVolumePools(volumePoolsAndPaths.first());
            command.setVolumePaths(volumePoolsAndPaths.second());
        }

        BackupAnswer answer;
        try {
            answer = (BackupAnswer) agentManager.send(host.getId(), command);
        } catch (AgentUnavailableException e) {
            logger.error("Unable to contact backend control plane to initiate backup for VM {}", vm.getInstanceName());
            backupVO.setStatus(Backup.Status.Failed);
            backupDao.update(backupVO.getId(), backupVO);
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            logger.error("Operation to initiate backup timed out for VM {}", vm.getInstanceName());
            backupVO.setStatus(Backup.Status.Failed);
            backupDao.update(backupVO.getId(), backupVO);
            throw new CloudRuntimeException("Operation to initiate backup timed out, please try again");
        }

        if (answer != null && answer.getResult()) {
            backupVO.setDate(new Date());
            backupVO.setSize(answer.getSize());
            backupVO.setStatus(Backup.Status.BackedUp);
            // If the agent fell back to full (stopped VM mid-incremental cycle), record this
            // backup as a full and start a new chain.
            ChainDecision effective = decision;
            if (answer.getIncrementalFallback()) {
                effective = decision.asFullRestart();
                backupVO.setType("FULL");
            }
            List<Volume> volumes = new ArrayList<>(volumeDao.findByInstance(vm.getId()));
            backupVO.setBackedUpVolumes(backupManager.createVolumeInfoFromVolumes(volumes));
            if (backupDao.update(backupVO.getId(), backupVO)) {
                // Legacy-full (incremental feature disabled): persist no chain/checkpoint metadata
                // and do not touch the VM's active_checkpoint_id — keep the feature-off path legacy.
                if (!decision.isLegacyFull()) {
                    persistChainMetadata(backupVO, effective, answer.getBitmapCreated());
                    // Pin the VM's active_checkpoint_id to whichever bitmap the agent actually
                    // created — the only valid parent for the next incremental (see decideChain).
                    // If the agent reports no bitmap (bitmapCreated=null), clear any stale detail
                    // so the next backup starts a fresh full.
                    String confirmedBitmap = answer.getBitmapCreated();
                    if (confirmedBitmap != null) {
                        upsertVmActiveCheckpoint(vm.getId(), confirmedBitmap);
                    } else {
                        clearVmActiveCheckpoint(vm.getId());
                    }
                }
                return new Pair<>(true, backupVO);
            } else {
                throw new CloudRuntimeException("Failed to update backup");
            }
        } else {
            logger.error("Failed to take backup for VM {}: {}", vm.getInstanceName(), answer != null ? answer.getDetails() : "No answer received");
            if (answer != null && answer.getNeedsCleanup()) {
                logger.error("Backup cleanup failed for VM {}. Leaving the backup in Error state. Backup should be manually deleted to free up the space", vm.getInstanceName());
                backupVO.setStatus(Backup.Status.Error);
                backupDao.update(backupVO.getId(), backupVO);
            } else {
                backupVO.setStatus(Backup.Status.Failed);
                backupDao.update(backupVO.getId(), backupVO);
            }
            return new Pair<>(false, null);
        }
    }

    private BackupVO createBackupObject(VirtualMachine vm, String backupPath, String type) {
        BackupVO backup = new BackupVO();
        backup.setVmId(vm.getId());
        backup.setExternalId(backupPath);
        backup.setType(type);
        backup.setDate(new Date());
        long virtualSize = 0L;
        for (final Volume volume: volumeDao.findByInstance(vm.getId())) {
            if (Volume.State.Ready.equals(volume.getState())) {
                virtualSize += volume.getSize();
            }
        }
        backup.setProtectedSize(virtualSize);
        backup.setStatus(Backup.Status.BackingUp);
        backup.setBackupOfferingId(vm.getBackupOfferingId());
        backup.setAccountId(vm.getAccountId());
        backup.setDomainId(vm.getDomainId());
        backup.setZoneId(vm.getDataCenterId());
        backup.setName(backupManager.getBackupNameFromVM(vm));
        Map<String, String> details = backupManager.getBackupDetailsFromVM(vm);
        backup.setDetails(details);

        return backupDao.persist(backup);
    }

    @Override
    public Pair<Boolean, String> restoreBackupToVM(VirtualMachine vm, Backup backup, String hostIp, String dataStoreUuid, boolean quickrestore) {
        return restoreVMBackup(vm, backup);
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup, boolean quickRestore, Long hostId) {
        return restoreVMBackup(vm, backup).first();
    }

    private Pair<Boolean, String> restoreVMBackup(VirtualMachine vm, Backup backup) {
        List<Backup.VolumeInfo> backedVolumes = backup.getBackedUpVolumes();
        List<String> backedVolumesUUIDs = backedVolumes.stream()
                .sorted(Comparator.comparingLong(Backup.VolumeInfo::getDeviceId))
                .map(Backup.VolumeInfo::getUuid)
                .collect(Collectors.toList());

        List<VolumeVO> restoreVolumes = volumeDao.findByInstance(vm.getId()).stream()
                .sorted(Comparator.comparingLong(VolumeVO::getDeviceId))
                .collect(Collectors.toList());

        LOG.debug("Restoring vm {} from backup {} on the NAS Backup Provider", vm, backup);
        BackupRepository backupRepository = getBackupRepository(backup);

        final Host host = getVMHypervisorHost(vm);
        RestoreBackupCommand restoreCommand = new RestoreBackupCommand();
        restoreCommand.setBackupPath(backup.getExternalId());
        restoreCommand.setBackupRepoType(backupRepository.getType());
        restoreCommand.setBackupRepoAddress(backupRepository.getAddress());
        restoreCommand.setMountOptions(backupRepository.getMountOptions());
        restoreCommand.setVmName(vm.getName());
        restoreCommand.setBackupVolumesUUIDs(backedVolumesUUIDs);
        Pair<List<PrimaryDataStoreTO>, List<String>> volumePoolsAndPaths = getVolumePoolsAndPaths(restoreVolumes);
        restoreCommand.setRestoreVolumePools(volumePoolsAndPaths.first());
        restoreCommand.setRestoreVolumePaths(volumePoolsAndPaths.second());
        restoreCommand.setBackupFiles(getBackupFiles(backedVolumes));
        applyBackupFormat(restoreCommand, backup);
        restoreCommand.setVmExists(vm.getRemoved() == null);
        restoreCommand.setVmState(vm.getState());
        restoreCommand.setMountTimeout(NASBackupRestoreMountTimeout.value());

        BackupAnswer answer;
        try {
            answer = (BackupAnswer) agentManager.send(host.getId(), restoreCommand);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to restore backup timed out, please try again");
        }
        if (answer == null) {
            throw new CloudRuntimeException(String.format("No answer received from the backend control plane while restoring VM %s from backup %s", vm.getInstanceName(), backup.getUuid()));
        }
        // After a restore the QEMU dirty-bitmap chain is gone, so clear active_checkpoint_id so
        // the next backup is taken as a fresh full and starts a new chain. See decideChain.
        if (answer.getResult()) {
            clearVmActiveCheckpoint(vm.getId());
        }
        return new Pair<>(answer.getResult(), answer.getDetails());
    }

    /**
     * Tell the agent how to read {@code backup} back. qcow2 chains are self-describing — every
     * incremental was rebased onto its parent, so handing the agent the leaf file is enough. RBD
     * chains are not: an {@code rbd export-diff} stream carries no pointer to the diff before it,
     * so the whole chain from the full anchor up to this backup travels with the command and the
     * agent replays it in order.
     */
    private void applyBackupFormat(RestoreBackupCommand restoreCommand, Backup backup) {
        if (!NASBackupChainKeys.FORMAT_RBD_DIFF.equals(chainFormatOf(backup))) {
            return;
        }
        restoreCommand.setBackupFormat(NASBackupChainKeys.FORMAT_RBD_DIFF);
        restoreCommand.setBackupChainPaths(composeRestoreChainDirs(backup));
    }

    /**
     * The backup directories to replay to reconstruct {@code backup}, ordered root-first: the
     * chain's full anchor, every incremental after it, and {@code backup} itself.
     *
     * <p>Ancestors that a user asked to delete are only tombstoned (Hidden) while descendants are
     * alive, precisely so their files survive on the NAS for this walk. A gap here therefore means
     * the chain really was broken, and replaying what is left would silently restore a mix of two
     * points in time — so we refuse instead.</p>
     */
    private List<String> composeRestoreChainDirs(Backup backup) {
        int targetPosition = chainPosition(backup);
        List<String> dirs = new ArrayList<>();
        for (Backup member : getChainOrderedLeafToRoot(backup)) {
            if (chainPosition(member) <= targetPosition) {
                dirs.add(member.getExternalId());
            }
        }
        // getChainOrderedLeafToRoot sorts by descending chain position; restore replays root-first.
        Collections.reverse(dirs);
        if (targetPosition != Integer.MAX_VALUE && dirs.size() != targetPosition + 1) {
            throw new CloudRuntimeException(String.format(
                    "Cannot restore backup %s: its chain is incomplete (%d of the %d backups it depends on are present).",
                    backup.getUuid(), dirs.size(), targetPosition + 1));
        }
        return dirs;
    }

    private List<String> getBackupFiles(List<Backup.VolumeInfo> backedVolumes) {
        List<String> backupFiles = new ArrayList<>();
        for (Backup.VolumeInfo backedVolume : backedVolumes) {
            backupFiles.add(backedVolume.getPath());
        }
        return backupFiles;
    }

    private Pair<List<PrimaryDataStoreTO>, List<String>> getVolumePoolsAndPaths(List<VolumeVO> volumes) {
        List<PrimaryDataStoreTO> volumePools = new ArrayList<>();
        List<String> volumePaths = new ArrayList<>();
        for (VolumeVO volume : volumes) {
            StoragePoolVO storagePool = primaryDataStoreDao.findById(volume.getPoolId());
            if (Objects.isNull(storagePool)) {
                throw new CloudRuntimeException("Unable to find storage pool associated to the volume");
            }

            DataStore dataStore = dataStoreMgr.getDataStore(storagePool.getId(), DataStoreRole.Primary);
            volumePools.add(dataStore != null ? (PrimaryDataStoreTO)dataStore.getTO() : null);

            String volumePathPrefix = getVolumePathPrefix(storagePool);
            String volumePathSuffix = getVolumePathSuffix(storagePool);
            volumePaths.add(String.format("%s%s%s", volumePathPrefix, volume.getPath(), volumePathSuffix));
        }
        return new Pair<>(volumePools, volumePaths);
    }

    private String getVolumePathPrefix(StoragePoolVO storagePool) {
        String volumePathPrefix;
        if (ScopeType.HOST.equals(storagePool.getScope()) ||
                Storage.StoragePoolType.SharedMountPoint.equals(storagePool.getPoolType()) ||
                Storage.StoragePoolType.RBD.equals(storagePool.getPoolType())) {
            volumePathPrefix = storagePool.getPath() + "/";
        } else if (Storage.StoragePoolType.Linstor.equals(storagePool.getPoolType())) {
            volumePathPrefix = "/dev/drbd/by-res/cs-";
        } else {
            // Should be Storage.StoragePoolType.NetworkFilesystem
            volumePathPrefix = String.format("/mnt/%s/", storagePool.getUuid());
        }
        return volumePathPrefix;
    }

    private String getVolumePathSuffix(StoragePoolVO storagePool) {
        if (Storage.StoragePoolType.Linstor.equals(storagePool.getPoolType())) {
            return "/0";
        } else {
            return "";
        }
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, Backup.VolumeInfo backupVolumeInfo, String hostIp, String dataStoreUuid,
            Pair<String, VirtualMachine.State> vmNameAndState, VirtualMachine vm, boolean quickRestore) {
        final VolumeVO volume = volumeDao.findByUuid(backupVolumeInfo.getUuid());
        final DiskOffering diskOffering = diskOfferingDao.findByUuid(backupVolumeInfo.getDiskOfferingId());
        final StoragePoolVO pool = primaryDataStoreDao.findByUuid(dataStoreUuid);
        final HostVO hostVO = hostDao.findByIp(hostIp);

        Backup.VolumeInfo matchingVolume = getBackedUpVolumeInfo(backup.getBackedUpVolumes(), volume.getUuid());
        if (matchingVolume == null) {
            throw new CloudRuntimeException(String.format("Unable to find volume %s in the list of backed up volumes for backup %s, cannot proceed with restore", volume.getUuid(), backup));
        }
        Long backedUpVolumeSize = matchingVolume.getSize();

        LOG.debug("Restoring vm volume {} from backup {} on the NAS Backup Provider", volume, backup);
        BackupRepository backupRepository = getBackupRepository(backup);

        VolumeVO restoredVolume = new VolumeVO(Volume.Type.DATADISK, null, backup.getZoneId(),
                backup.getDomainId(), backup.getAccountId(), 0, null,
                backup.getSize(), null, null, null);
        String volumeUUID = UUID.randomUUID().toString();
        String volumeName = volume != null ? volume.getName() : backupVolumeInfo.getUuid();
        restoredVolume.setName("RestoredVol-" + volumeName);
        restoredVolume.setProvisioningType(diskOffering.getProvisioningType());
        restoredVolume.setUpdated(new Date());
        restoredVolume.setUuid(volumeUUID);
        restoredVolume.setRemoved(null);
        restoredVolume.setDisplayVolume(true);
        restoredVolume.setPoolId(pool.getId());
        restoredVolume.setPoolType(pool.getPoolType());
        restoredVolume.setPath(restoredVolume.getUuid());
        restoredVolume.setState(Volume.State.Copying);
        restoredVolume.setSize(backedUpVolumeSize);
        restoredVolume.setDiskOfferingId(diskOffering.getId());
        if (pool.getPoolType() != Storage.StoragePoolType.RBD) {
            restoredVolume.setFormat(Storage.ImageFormat.QCOW2);
        } else {
            restoredVolume.setFormat(Storage.ImageFormat.RAW);
        }

        RestoreBackupCommand restoreCommand = new RestoreBackupCommand();
        restoreCommand.setBackupPath(backup.getExternalId());
        restoreCommand.setBackupRepoType(backupRepository.getType());
        restoreCommand.setBackupRepoAddress(backupRepository.getAddress());
        restoreCommand.setVmName(vmNameAndState.first());
        String restoreVolumePath = String.format("%s%s%s", getVolumePathPrefix(pool), volumeUUID, getVolumePathSuffix(pool));
        restoreCommand.setRestoreVolumePaths(Collections.singletonList(restoreVolumePath));
        restoreCommand.setRestoreVolumeSizes(Collections.singletonList(backedUpVolumeSize));
        DataStore dataStore = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
        restoreCommand.setRestoreVolumePools(Collections.singletonList(dataStore != null ? (PrimaryDataStoreTO)dataStore.getTO() : null));
        restoreCommand.setDiskType(backupVolumeInfo.getType().name().toLowerCase(Locale.ROOT));
        restoreCommand.setMountOptions(backupRepository.getMountOptions());
        restoreCommand.setVmExists(null);
        restoreCommand.setVmState(vmNameAndState.second());
        restoreCommand.setMountTimeout(NASBackupRestoreMountTimeout.value());
        restoreCommand.setBackupFiles(Collections.singletonList(matchingVolume.getPath()));
        applyBackupFormat(restoreCommand, backup);

        BackupAnswer answer;
        try {
            answer = (BackupAnswer) agentManager.send(hostVO.getId(), restoreCommand);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to restore backed up volume timed out, please try again");
        }

        if (answer.getResult()) {
            try {
                volumeDao.persist(restoredVolume);
            } catch (Exception e) {
                throw new CloudRuntimeException("Unable to create restored volume due to: " + e);
            }
            // The restored volume is attached to this VM with no QEMU bitmap on its image, so the
            // VM's tracked checkpoint is now stale; clear it to force the next backup to be a full
            // (mirrors the full restore paths restoreVMFromBackup/restoreBackupToVM).
            VirtualMachine restoreTargetVm = vmInstanceDao.findVMByInstanceName(vmNameAndState.first());
            if (restoreTargetVm != null) {
                clearVmActiveCheckpoint(restoreTargetVm.getId());
            }
        }

        return new Pair<>(answer.getResult(), answer.getDetails());
    }

    private BackupRepository getBackupRepository(Backup backup) {
        BackupRepository backupRepository = backupRepositoryDao.findByBackupOfferingId(backup.getBackupOfferingId());
        if (backupRepository == null) {
            throw new CloudRuntimeException(String.format("No valid backup repository found for the backup %s, please check the attached backup offering", backup.getUuid()));
        }
        return backupRepository;
    }

    private Backup.VolumeInfo getBackedUpVolumeInfo(List<Backup.VolumeInfo> backedUpVolumes, String volumeUuid) {
        return backedUpVolumes.stream()
                .filter(v -> v.getUuid().equals(volumeUuid))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean handlesChainDeleteResourceAccounting() {
        // This provider deletes whole incremental chains (leaf + swept delete-pending ancestors)
        // and decrements resource count/usage once per physically-removed backup itself, so the
        // manager must not also decrement or remove the DB row.
        return true;
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        final BackupRepository backupRepository = backupRepositoryDao.findByBackupOfferingId(backup.getBackupOfferingId());
        if (backupRepository == null) {
            throw new CloudRuntimeException("No valid backup repository found for the VM, please check the attached backup offering");
        }

        final Host host;
        final VirtualMachine vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vm != null) {
            host = getVMHypervisorHost(vm);
        } else {
            host = resourceManager.findOneRandomRunningHostByHypervisor(Hypervisor.HypervisorType.KVM, backup.getZoneId());
        }
        if (host == null) {
            throw new CloudRuntimeException(String.format("Unable to find a running KVM host in zone %d to delete backup %s", backup.getZoneId(), backup.getUuid()));
        }

        // Backups outside any tracked chain (legacy or non-incremental providers) are
        // deleted straight away — no children semantics apply.
        if (readDetail(backup, NASBackupChainKeys.CHAIN_ID) == null) {
            return deleteBackupFileAndRow(backup, backupRepository, host);
        }

        // Concurrent deletes can mutate the same chain concurrently resulting in inconsistent states.
        // take a per-VM lock before modifying the chain.
        final GlobalLock chainLock = acquireChainDeleteLock(backup.getVmId());
        try {
            Backup current = backupDao.findById(backup.getId());
            if (current == null) {
                LOG.debug("Backup {} was already removed by a concurrent chain delete", backup.getUuid());
                return true;
            }
            backup = current;

            // Snapshot-style cascade: defer the on-NAS rm + DB row while there are live children,
            // mark this backup as delete-pending, and let the leaf's deletion sweep it up later.
            // See DefaultSnapshotStrategy#deleteSnapshotChain for the same pattern on incremental
            // snapshots. forced=true means the caller wants the entire subtree gone right now.
            if (forced) {
                return cascadeDeleteSubtree(backup, backupRepository, host);
            }

            if (hasLiveChildren(backup)) {
                markDeletePending(backup);
                LOG.debug("Backup {} has live descendants in its chain; setting status as Hidden. " +
                                "The on-NAS file and DB row will be removed once the last descendant is gone, " +
                                "or pass forced=true.",
                        backup.getUuid());
                return true;
            }

            // No live children — physically delete this backup, then walk up the chain and
            // collect any ancestors that were left in Hidden state.
            return deleteLeafBackupAndSweepPendingAncestors(backup, backupRepository, host);
        } finally {
            releaseChainDeleteLock(chainLock);
        }
    }

    /**
     * Take the per-VM lock that serializes chain-mutating backup deletes.
     */
    protected GlobalLock acquireChainDeleteLock(long vmId) {
        GlobalLock lock = GlobalLock.getInternLock("nas.backup.chain.vm." + vmId);
        if (!lock.lock(CHAIN_LOCK_TIMEOUT_SECONDS)) {
            lock.releaseRef();
            throw new CloudRuntimeException(String.format(
                    "Timed out waiting for concurrent backup chain operations on VM %d to finish, please try again", vmId));
        }
        return lock;
    }

    protected void releaseChainDeleteLock(GlobalLock lock) {
        lock.unlock();
        lock.releaseRef();
    }

    /**
     * The single physical-delete step: rm the on-NAS directory, then remove the DB row.
     * Returns {@code false} (and leaves both intact) if the agent reports failure, so the
     * caller's recursion stops cleanly.
     */
    private boolean deleteBackupFileAndRow(Backup backup, BackupRepository repo, Host host) {
        DeleteBackupCommand command = new DeleteBackupCommand(backup.getExternalId(), repo.getType(),
                repo.getAddress(), repo.getMountOptions());
        BackupAnswer answer;
        try {
            answer = (BackupAnswer) agentManager.send(host.getId(), command);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to delete backup timed out, please try again");
        }
        if (answer == null || !answer.getResult()) {
            logger.error("Failed to delete backup file for {} ({}); leaving DB row intact",
                    backup.getUuid(), backup.getExternalId());
            return false;
        }
        // Capture the deleted backup's bitmap before the row (and its backup_details) are removed.
        String deletedBitmap = readDetail(backup, NASBackupChainKeys.BITMAP_NAME);
        backupDao.remove(backup.getId());
        // If this backup owned the bitmap the VM's active_checkpoint_id points to, that on-host QEMU
        // dirty-bitmap is gone with it — clear active_checkpoint_id so the next backup starts a fresh
        // full chain instead of anchoring an incremental on a deleted checkpoint (test: delete last backup).
        if (deletedBitmap != null && deletedBitmap.equals(readVmActiveCheckpoint(backup.getVmId()))) {
            clearVmActiveCheckpoint(backup.getVmId());
        }
        // Exactly-once resource accounting: decrement at the single point a backup row + file are
        // physically removed. This runs for the leaf and for every swept delete-pending ancestor,
        // so a chain delete decrements once per actually-removed backup. The manager skips its own
        // accounting for this provider (see handlesChainDeleteResourceAccounting()).
        long size = backup.getSize() != null ? backup.getSize() : 0L;
        resourceLimitMgr.decrementResourceCount(backup.getAccountId(), Resource.ResourceType.backup);
        resourceLimitMgr.decrementResourceCount(backup.getAccountId(), Resource.ResourceType.backup_storage, size);
        return true;
    }

    /**
     * Tombstone {@code backup} by moving it to {@link Backup.Status#Hidden}. Idempotent.
     * The row stays in the DB so the chain GC can sweep it once its last descendant is deleted
     * ({@code listByVmId} is status-agnostic), but it disappears from the user-facing list
     * ({@link BackupManagerImpl#listBackups} filters Hidden) and all backup operations refuse it
     * (they require {@code BackedUp}). Replaces the previous {@code nas.delete_pending} detail.
     */
    private void markDeletePending(Backup backup) {
        if (Backup.Status.Hidden.equals(backup.getStatus())) {
            return;
        }
        BackupVO vo = backupDao.findById(backup.getId());
        if (vo != null) {
            vo.setStatus(Backup.Status.Hidden);
            backupDao.update(vo.getId(), vo);
        }
    }

    /**
     * @return true if this backup is a tombstone (Hidden) awaiting chain sweep.
     */
    private boolean isDeletePending(Backup backup) {
        return Backup.Status.Hidden.equals(backup.getStatus());
    }

    /**
     * Whether there are any live (not tombstoned/Hidden, not Removed) descendants of {@code backup} within the
     * same chain. This uses {@code CHAIN_POSITION} to detect dependents deeper in the chain.
     */
    private boolean hasLiveChildren(Backup backup) {
        String chainId = readDetail(backup, NASBackupChainKeys.CHAIN_ID);
        if (chainId == null) {
            return false;
        }
        int position = chainPosition(backup);
        for (Backup b : backupDao.listByVmId(null, backup.getVmId())) {
            if (b.getId() == backup.getId() || !chainId.equals(readDetail(b, NASBackupChainKeys.CHAIN_ID))) {
                continue;
            }
            if (!isDeletePending(b) && chainPosition(b) > position) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the chain containing {@code member}, ordered leaf-first
     * (highest {@code CHAIN_POSITION} → root).
     *
     * <p>Materialises the chain via a single {@link BackupDao#listByVmId} call. Callers that
     * previously walked the chain by repeatedly calling {@link #findChainParent} were doing
     * O(N) {@code listByVmId} calls (one per ancestor); this collapses that to one.
     *
     * <p>If {@code member} has no {@code CHAIN_ID} metadata it is returned as a one-element
     * list (it is its own degenerate chain).
     */
    private List<Backup> getChainOrderedLeafToRoot(Backup member) {
        String chainId = readDetail(member, NASBackupChainKeys.CHAIN_ID);
        if (chainId == null) {
            return Collections.singletonList(member);
        }
        List<Backup> chain = new ArrayList<>();
        for (Backup b : backupDao.listByVmId(null, member.getVmId())) {
            if (chainId.equals(readDetail(b, NASBackupChainKeys.CHAIN_ID))) {
                chain.add(b);
            }
        }
        // Descending CHAIN_POSITION = leaf-first (highest position = furthest from root).
        chain.sort((a, b) -> Integer.compare(chainPosition(b), chainPosition(a)));
        return chain;
    }

    /**
     * Physically delete the leaf {@code backup}, then walk up the chain while each ancestor
     * is in Hidden state. Mirrors the snapshot subsystem pattern: once a leaf is
     * gone, garbage-collect any tombstoned parents.
     *
     * <p>Caller must guarantee {@code backup} is a leaf (no live children). Each tombstoned
     * ancestor is by definition childless once its sole child is deleted here, so no extra
     * live-children check is needed inside the loop.
     */
    private boolean deleteLeafBackupAndSweepPendingAncestors(Backup backup, BackupRepository repo, Host host) {
        // Snapshot the chain BEFORE the leaf delete — deleteBackupFileAndRow removes the row,
        // after which the in-memory list still resolves but the DB no longer would.
        List<Backup> chain = getChainOrderedLeafToRoot(backup);
        if (!deleteBackupFileAndRow(backup, repo, host)) {
            return false;
        }
        for (Backup member : chain) {
            if (member.getId() == backup.getId()) {
                continue;
            }
            if (!isDeletePending(member)) {
                break;
            }
            deleteBackupFileAndRow(member, repo, host);
        }
        return true;
    }

    /**
     * Forced delete of {@code root}'s entire chain, leaf-first. NAS backups form a linear
     * chain (full → inc → inc → …), not a tree, so we just walk the ordered chain and
     * delete each member without re-querying parents.
     */
    private boolean cascadeDeleteSubtree(Backup root, BackupRepository repo, Host host) {
        for (Backup b : getChainOrderedLeafToRoot(root)) {
            if (!deleteBackupFileAndRow(b, repo, host)) {
                return false;
            }
        }
        return true;
    }

    private int chainPosition(Backup b) {
        String s = readDetail(b, NASBackupChainKeys.CHAIN_POSITION);
        if (s == null) {
            return Integer.MAX_VALUE; // no metadata => sort to end
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public void syncBackupMetrics(Long zoneId) {
    }

    @Override
    public List<Backup.RestorePoint> listRestorePoints(VirtualMachine vm) {
        return null;
    }

    @Override
    public Backup createNewBackupEntryForRestorePoint(Backup.RestorePoint restorePoint, VirtualMachine vm) {
        return null;
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        for (VMSnapshotVO vmSnapshotVO : vmSnapshotDao.findByVmAndByType(vm.getId(), VMSnapshot.Type.Disk)) {
            List<VMSnapshotDetailsVO> vmSnapshotDetails = vmSnapshotDetailsDao.listDetails(vmSnapshotVO.getId());
            if (vmSnapshotDetails.stream().anyMatch(vmSnapshotDetailsVO -> VolumeApiServiceImpl.KVM_FILE_BASED_STORAGE_SNAPSHOT.equals(vmSnapshotDetailsVO.getName()))) {
                logger.warn("VM [{}] has VM snapshots using the KvmFileBasedStorageVmSnapshot Strategy; this provider does not support backups on VMs with these snapshots!");
                return false;
            }
        }

        return Hypervisor.HypervisorType.KVM.equals(vm.getHypervisorType());
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        // Clear the VM's active checkpoint so any future re-assignment to a backup offering
        // starts a fresh chain. Without this, a detach-volume + attach-different-volume cycle
        // while the offering is unassigned would lead to the next backup trying to rebase
        // onto a stale parent (different volume identity, same VM id).
        clearVmActiveCheckpoint(vm.getId());
        return true;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return false;
    }

    @Override
    public boolean supportsInstanceFromBackup() {
        return true;
    }

    @Override
    public boolean supportsMemoryVmSnapshot() {
        return false;
    }

    @Override
    public Pair<Long, Long> getBackupStorageStats(Long zoneId) {
        final List<BackupRepository> repositories = backupRepositoryDao.listByZoneAndProvider(zoneId, getName());
        Long totalSize = 0L;
        Long usedSize = 0L;
        for (final BackupRepository repository : repositories) {
            if (repository.getCapacityBytes() != null) {
                totalSize += repository.getCapacityBytes();
            }
            if (repository.getUsedBytes() != null) {
                usedSize += repository.getUsedBytes();
            }
        }
        return new Pair<>(usedSize, totalSize);
    }

    @Override
    public void syncBackupStorageStats(Long zoneId) {
        final List<BackupRepository> repositories = backupRepositoryDao.listByZoneAndProvider(zoneId, getName());
        if (CollectionUtils.isEmpty(repositories)) {
            return;
        }
        final Host host = resourceManager.findOneRandomRunningHostByHypervisor(Hypervisor.HypervisorType.KVM, zoneId);
        if (host == null) {
            logger.warn("Unable to find a running KVM host in zone {} to sync backup storage stats", zoneId);
            return;
        }
        for (final BackupRepository repository : repositories) {
            GetBackupStorageStatsCommand command = new GetBackupStorageStatsCommand(repository.getType(), repository.getAddress(), repository.getMountOptions());
            BackupStorageStatsAnswer answer;
            try {
                answer = (BackupStorageStatsAnswer) agentManager.send(host.getId(), command);
                backupRepositoryDao.updateCapacity(repository, answer.getTotalSize(), answer.getUsedSize());
            } catch (AgentUnavailableException e) {
                logger.warn("Unable to contact backend control plane to get backup stats for repository: {}", repository.getName());
            } catch (OperationTimedoutException e) {
                logger.warn("Operation to get backup stats timed out for the repository: " + repository.getName());
            }
        }
    }

    @Override
    public List<BackupOffering> listBackupOfferings(Long zoneId) {
        final List<BackupRepository> repositories = backupRepositoryDao.listByZoneAndProvider(zoneId, getName());
        final List<BackupOffering> offerings = new ArrayList<>();
        for (final BackupRepository repository : repositories) {
            offerings.add(new NasBackupOffering(repository.getName(), repository.getUuid()));
        }
        return offerings;
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        return true;
    }

    @Override
    public Boolean crossZoneInstanceCreationEnabled(BackupOffering backupOffering) {
        final BackupRepository backupRepository = backupRepositoryDao.findByBackupOfferingId(backupOffering.getId());
        if (backupRepository == null) {
            throw new CloudRuntimeException("Backup repository not found for the backup offering" + backupOffering.getName());
        }
        return Boolean.TRUE.equals(backupRepository.crossZoneInstanceCreationEnabled());
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                NASBackupRestoreMountTimeout,
                NASBackupFullEvery,
                NASBackupIncrementalEnabled
        };
    }

    @Override
    public String getName() {
        return "nas";
    }

    @Override
    public String getDescription() {
        return "NAS Backup Plugin";
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }
}
