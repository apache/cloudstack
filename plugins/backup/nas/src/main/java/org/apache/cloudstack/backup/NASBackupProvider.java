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
import com.cloud.utils.exception.CloudRuntimeException;
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

    ConfigKey<Integer> NASBackupRestoreMountTimeout = new ConfigKey<>("Advanced", Integer.class,
            "nas.backup.restore.mount.timeout",
            "30",
            "Timeout in seconds after which backup repository mount for restore fails.",
            true,
            BackupFrameworkEnabled.key());

    ConfigKey<Integer> NASBackupFullEvery = new ConfigKey<>("Advanced", Integer.class,
            "nas.backup.full.every",
            "10",
            "Take a full NAS backup every Nth backup; remaining backups in between are incremental. " +
                    "Counts backups, not days, so it works for hourly, daily, and ad-hoc schedules. " +
                    "Set to 1 to disable incrementals (every backup is full).",
            true,
            ConfigKey.Scope.Zone,
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
     * the chain: full vs incremental, the bitmap name to create, and (for incrementals) the
     * parent bitmap and parent file path.
     */
    static final class ChainDecision {
        final String mode;            // "full" or "incremental"
        final String bitmapNew;
        final String bitmapParent;    // null for full
        // Per-volume parent backup file paths, one per current VM volume in deviceId order.
        // null/empty for full. Replaces the single parentPath field — each volume needs its
        // own parent file because backup files are named after each volume's own UUID
        // (root.<uuid>.qcow2 / datadisk.<uuid>.qcow2), abh1sar review at line 340.
        final List<String> parentPaths;
        final String chainId;         // chain identifier this backup belongs to
        final int chainPosition;      // 0 for full, N for the Nth incremental in the chain

        private ChainDecision(String mode, String bitmapNew, String bitmapParent, List<String> parentPaths,
                              String chainId, int chainPosition) {
            this.mode = mode;
            this.bitmapNew = bitmapNew;
            this.bitmapParent = bitmapParent;
            this.parentPaths = parentPaths;
            this.chainId = chainId;
            this.chainPosition = chainPosition;
        }

        static ChainDecision fullStart(String bitmapName) {
            return new ChainDecision(NASBackupChainKeys.TYPE_FULL, bitmapName, null, null,
                    UUID.randomUUID().toString(), 0);
        }

        static ChainDecision incremental(String bitmapNew, String bitmapParent, List<String> parentPaths,
                                         String chainId, int chainPosition) {
            return new ChainDecision(NASBackupChainKeys.TYPE_INCREMENTAL, bitmapNew, bitmapParent,
                    parentPaths, chainId, chainPosition);
        }

        boolean isIncremental() {
            return NASBackupChainKeys.TYPE_INCREMENTAL.equals(mode);
        }
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
     * a more recent "last backup taken" row in the database. This matches the prescription in
     * the PR review (avoid relying on "last backup" because that breaks after restore).</p>
     */
    protected ChainDecision decideChain(VirtualMachine vm) {
        final String newBitmap = "backup-" + System.currentTimeMillis() / 1000L;

        // Master switch — when the operator disables incrementals at the zone level every
        // backup is taken as a fresh full. Existing chains stay restorable because each
        // backup's metadata is kept independently; restoring an incremental still walks its
        // own chain (the per-backup chain_id / parent_backup_id details persist regardless
        // of the live config). The next backup with this flag back on starts a new chain.
        Boolean incrementalEnabled = NASBackupIncrementalEnabled.valueIn(vm.getDataCenterId());
        if (incrementalEnabled == null || !incrementalEnabled) {
            return ChainDecision.fullStart(newBitmap);
        }

        // Stopped VMs cannot do incrementals — script will also fall back, but we make the
        // decision here so we register the right type up-front.
        if (VirtualMachine.State.Stopped.equals(vm.getState())) {
            return ChainDecision.fullStart(newBitmap);
        }

        Integer fullEvery = NASBackupFullEvery.valueIn(vm.getDataCenterId());
        if (fullEvery == null || fullEvery <= 1) {
            // Disabled or every-backup-is-full mode.
            return ChainDecision.fullStart(newBitmap);
        }

        // 1. If the VM has no active_checkpoint_id, there is no bitmap on the host to use as
        //    a parent. This is the case after restore (we clear it), after VM was just assigned
        //    to the offering, or on the very first backup.
        String activeCheckpoint = readVmActiveCheckpoint(vm.getId());
        if (activeCheckpoint == null) {
            return ChainDecision.fullStart(newBitmap);
        }

        // 2. Find the latest BackedUp backup of this VM whose BITMAP_NAME matches the VM's
        //    active_checkpoint_id. Only that backup is a safe parent — any earlier backup
        //    would have a bitmap that QEMU may have rotated out. Per the review:
        //      "The latest backup should have the bitmap_name equal to the VM's
        //       active_checkpoint_id which will become the parent backup. If not, force full."
        Backup parent = findLatestBackedUpBackupWithBitmap(vm.getId(), activeCheckpoint);
        if (parent == null) {
            LOG.debug("VM {} has active_checkpoint_id={} but no matching backup found — forcing full",
                    vm.getInstanceName(), activeCheckpoint);
            return ChainDecision.fullStart(newBitmap);
        }

        String parentChainId = readDetail(parent, NASBackupChainKeys.CHAIN_ID);
        int parentChainPosition = chainPosition(parent);
        if (parentChainId == null || parentChainPosition == Integer.MAX_VALUE) {
            return ChainDecision.fullStart(newBitmap);
        }

        // Force a fresh full when the chain has reached the configured length.
        if (parentChainPosition + 1 >= fullEvery) {
            return ChainDecision.fullStart(newBitmap);
        }

        // The script needs the parent backup's on-NAS file path PER VOLUME so it can rebase
        // each new qcow2 onto the matching parent. The paths are stored relative to the NAS
        // mount root — the script resolves them inside its mount session. When alignment
        // fails (volume count changed, etc.) compose returns null and we fall back to full
        // so we don't risk corrupting the chain.
        List<String> parentPaths = composeParentBackupPaths(parent, vm.getId());
        if (parentPaths == null) {
            LOG.debug("VM {} parent backup {} volume layout no longer matches current VM — forcing full",
                    vm.getInstanceName(), parent.getUuid());
            return ChainDecision.fullStart(newBitmap);
        }
        return ChainDecision.incremental(newBitmap, activeCheckpoint, parentPaths,
                parentChainId, parentChainPosition + 1);
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
        return (v == null || v.isEmpty()) ? null : v;
    }

    /**
     * Locate the most-recent {@code BackedUp} backup for {@code vmId} whose bitmap name equals
     * {@code bitmapName}. Used by {@link #decideChain} to anchor the next incremental.
     */
    private Backup findLatestBackedUpBackupWithBitmap(long vmId, String bitmapName) {
        List<Backup> history = backupDao.listByVmId(null, vmId);
        if (history == null || history.isEmpty()) {
            return null;
        }
        history.sort(Comparator.comparing(Backup::getDate).reversed());
        for (Backup b : history) {
            if (!Backup.Status.BackedUp.equals(b.getStatus())) {
                continue;
            }
            if (bitmapName.equals(readDetail(b, NASBackupChainKeys.BITMAP_NAME))) {
                return b;
            }
        }
        return null;
    }

    private String readDetail(Backup backup, String key) {
        BackupDetailVO d = backupDetailsDao.findDetail(backup.getId(), key);
        return d == null ? null : d.getValue();
    }

    /**
     * Compose the on-NAS path of EVERY parent backup file (one per VM volume) in the same
     * order the script will iterate the current VM's disks (deviceId asc). Relative to the
     * NAS mount, matches the layout written by {@code nasbackup.sh}:
     *   first  disk -> {@code <backupPath>/root.<volUuid>.qcow2}
     *   others      -> {@code <backupPath>/datadisk.<volUuid>.qcow2}
     *
     * Returns {@code null} if the parent's stored volume list can't be aligned with the
     * current VM's volumes (count mismatch / different volume identities). In that case the
     * caller should force a fresh full so we don't accidentally rebase a data disk onto the
     * root parent — exactly the bug abh1sar flagged at line 340.
     */
    private List<String> composeParentBackupPaths(Backup parent, long vmId) {
        // backupPath is stored as externalId by createBackupObject — e.g.
        // "i-2-1234-VM/2026.04.27.13.45.00".
        String dir = parent.getExternalId();
        if (dir == null || dir.isEmpty()) {
            return null;
        }

        // Read the parent's backed-up volume list (uuid order = deviceId order at the time
        // the parent was taken). The script names files as root.<uuid>.qcow2 for the first
        // entry and datadisk.<uuid>.qcow2 for subsequent entries — match that here.
        List<Backup.VolumeInfo> parentVols = parent.getBackedUpVolumes();
        if (parentVols == null || parentVols.isEmpty()) {
            return null;
        }

        // Sanity 1: the current VM must have the same number of volumes. If it doesn't (volume
        // added or removed since the parent), positional alignment is unsafe — caller falls back to full.
        List<VolumeVO> currentVols = volumeDao.findByInstance(vmId);
        if (currentVols == null || currentVols.size() != parentVols.size()) {
            return null;
        }

        // Sanity 2: VolumeDao.findByInstance() has no ordering guarantee. Sort the current
        // volumes by deviceId so positional comparison against parentVols (also deviceId-ordered
        // at backup time) is meaningful.
        List<VolumeVO> currentSorted = new ArrayList<>(currentVols);
        currentSorted.sort(Comparator.comparing(Volume::getDeviceId));

        // Sanity 3: verify each current volume's UUID matches the parent's recorded UUID at the
        // same position. If any disk was detached + a different one attached in its place, the
        // chain cannot be safely continued — force a full instead of silently rebasing onto the
        // wrong parent file.
        for (int i = 0; i < parentVols.size(); i++) {
            String parentUuid = parentVols.get(i).getUuid();
            String currentUuid = currentSorted.get(i).getUuid();
            if (parentUuid == null || parentUuid.isEmpty()
                    || currentUuid == null || !parentUuid.equals(currentUuid)) {
                LOG.debug("Volume identity mismatch at position {} for VM {}: parent uuid {} vs current uuid {}. " +
                        "Forcing a full backup to start a fresh chain.",
                        i, vmId, parentUuid, currentUuid);
                return null;
            }
        }

        List<String> paths = new ArrayList<>(parentVols.size());
        for (int i = 0; i < parentVols.size(); i++) {
            String volUuid = parentVols.get(i).getUuid();
            String prefix = (i == 0) ? "root" : "datadisk";
            paths.add(dir + "/" + prefix + "." + volUuid + ".qcow2");
        }
        return paths;
    }

    /**
     * Persist chain metadata under backup_details. Stored here (not on the backups table) so
     * other providers can implement their own chain semantics without schema changes.
     */
    private void persistChainMetadata(Backup backup, ChainDecision decision, String bitmapFromAgent) {
        // Only persist nas.bitmap_name when the agent confirmed it via BITMAP_CREATED=. If we
        // fall back to decision.bitmapNew when the agent didn't emit BITMAP_CREATED= (e.g.,
        // stopped-VM path where the qemu-img pre-seed failed, or running-VM path where libvirt
        // backup-begin succeeded but the bitmap line wasn't surfaced for any reason), we'd
        // anchor the next incremental on a bitmap that doesn't exist on the host. Better to
        // leave it empty so the next backup sees no checkpoint and starts a fresh full chain.
        if (bitmapFromAgent != null && !bitmapFromAgent.isEmpty()) {
            backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.BITMAP_NAME, bitmapFromAgent, true));
        }
        backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.CHAIN_ID, decision.chainId, true));
        backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.CHAIN_POSITION,
                String.valueOf(decision.chainPosition), true));
        backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.TYPE, decision.mode, true));
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
    public Pair<Boolean, Backup> takeBackup(final VirtualMachine vm, Boolean quiesceVM) {
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
        command.setBitmapNew(decision.bitmapNew);
        command.setBitmapParent(decision.bitmapParent);
        command.setParentPaths(decision.parentPaths);

        if (VirtualMachine.State.Stopped.equals(vm.getState())) {
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
            backupDao.remove(backupVO.getId());
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            logger.error("Operation to initiate backup timed out for VM {}", vm.getInstanceName());
            backupVO.setStatus(Backup.Status.Failed);
            backupDao.remove(backupVO.getId());
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
                effective = ChainDecision.fullStart(decision.bitmapNew);
                backupVO.setType("FULL");
            }
            List<Volume> volumes = new ArrayList<>(volumeDao.findByInstance(vm.getId()));
            backupVO.setBackedUpVolumes(backupManager.createVolumeInfoFromVolumes(volumes));
            if (backupDao.update(backupVO.getId(), backupVO)) {
                persistChainMetadata(backupVO, effective, answer.getBitmapCreated());
                if (answer.getBitmapRecreated() != null) {
                    backupDetailsDao.persist(new BackupDetailVO(backupVO.getId(),
                            NASBackupChainKeys.BITMAP_RECREATED, answer.getBitmapRecreated(), true));
                    logger.info("NAS incremental for VM {} recreated parent bitmap {} (likely VM was restarted since last backup)",
                            vm.getInstanceName(), answer.getBitmapRecreated());
                }
                // Pin the VM's active_checkpoint_id to whichever bitmap the agent actually
                // created. This is the only valid parent for the next incremental (see
                // decideChain). For the stopped-VM offline path BITMAP_CREATED is null —
                // no bitmap exists on the host, so we also clear any stale detail from a
                // prior online backup. Either way, after this step the detail accurately
                // reflects what's on the running QEMU (or absence thereof).
                String confirmedBitmap = answer.getBitmapCreated();
                if (confirmedBitmap != null) {
                    upsertVmActiveCheckpoint(vm.getId(), confirmedBitmap);
                } else {
                    clearVmActiveCheckpoint(vm.getId());
                }
                return new Pair<>(true, backupVO);
            } else {
                throw new CloudRuntimeException("Failed to update backup");
            }
        } else {
            logger.error("Failed to take backup for VM {}: {}", vm.getInstanceName(), answer != null ? answer.getDetails() : "No answer received");
            if (answer.getNeedsCleanup()) {
                logger.error("Backup cleanup failed for VM {}. Leaving the backup in Error state.", vm.getInstanceName());
                backupVO.setStatus(Backup.Status.Error);
                backupDao.update(backupVO.getId(), backupVO);
            } else {
                backupVO.setStatus(Backup.Status.Failed);
                backupDao.remove(backupVO.getId());
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
    public Pair<Boolean, String> restoreBackupToVM(VirtualMachine vm, Backup backup, String hostIp, String dataStoreUuid) {
        return restoreVMBackup(vm, backup);
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
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
        // After a restore the QEMU dirty-bitmap chain is gone — clear active_checkpoint_id so
        // the next backup is taken as a fresh full and starts a new chain. See decideChain.
        if (answer != null && answer.getResult()) {
            clearVmActiveCheckpoint(vm.getId());
        }
        return new Pair<>(answer.getResult(), answer.getDetails());
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
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, Backup.VolumeInfo backupVolumeInfo, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
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

        // Snapshot-style cascade: defer the on-NAS rm + DB row while there are live children,
        // mark this backup as delete-pending, and let the leaf's deletion sweep it up later.
        // See DefaultSnapshotStrategy#deleteSnapshotChain for the same pattern on incremental
        // snapshots. forced=true means the caller wants the entire subtree gone right now.
        if (forced) {
            return cascadeDeleteSubtree(backup, backupRepository, host);
        }

        List<Backup> liveChildren = findLiveChildren(backup);
        if (!liveChildren.isEmpty()) {
            markDeletePending(backup);
            LOG.debug("Backup {} has {} live child backup(s); marking as delete-pending. The on-NAS file " +
                            "and DB row will be removed once the last descendant is gone, or pass forced=true.",
                    backup.getUuid(), liveChildren.size());
            return true;
        }

        // No live children — physically delete this backup, then walk up the chain and
        // collect any ancestors that were left in delete-pending state.
        return deleteBackupAndSweepPendingAncestors(backup, backupRepository, host);
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
            logger.warn("Failed to delete backup file for {} ({}); leaving DB row intact",
                    backup.getUuid(), backup.getExternalId());
            return false;
        }
        backupDao.remove(backup.getId());
        return true;
    }

    /**
     * Mark {@code backup} as delete-pending in {@code backup_details}. Idempotent.
     */
    private void markDeletePending(Backup backup) {
        BackupDetailVO existing = backupDetailsDao.findDetail(backup.getId(), NASBackupChainKeys.DELETE_PENDING);
        if (existing == null) {
            backupDetailsDao.persist(new BackupDetailVO(backup.getId(),
                    NASBackupChainKeys.DELETE_PENDING, "true", true));
        }
    }

    /**
     * @return true if this backup carries the delete-pending tombstone.
     */
    private boolean isDeletePending(Backup backup) {
        BackupDetailVO d = backupDetailsDao.findDetail(backup.getId(), NASBackupChainKeys.DELETE_PENDING);
        return d != null && "true".equalsIgnoreCase(d.getValue());
    }

    /**
     * Return the live (not delete-pending, not Removed) children of {@code parent} within the
     * same chain. Equivalent to "incrementals whose parent_backup_id points at parent".
     */
    private List<Backup> findLiveChildren(Backup parent) {
        String parentUuid = parent.getUuid();
        String chainId = readDetail(parent, NASBackupChainKeys.CHAIN_ID);
        if (parentUuid == null || chainId == null) {
            return Collections.emptyList();
        }
        List<Backup> children = new ArrayList<>();
        for (Backup b : backupDao.listByVmId(null, parent.getVmId())) {
            if (b.getId() == parent.getId()) {
                continue;
            }
            if (!chainId.equals(readDetail(b, NASBackupChainKeys.CHAIN_ID))) {
                continue;
            }
            if (!parentUuid.equals(readDetail(b, NASBackupChainKeys.PARENT_BACKUP_ID))) {
                continue;
            }
            if (isDeletePending(b)) {
                // Tombstoned children don't keep us alive — they're already on the way out.
                continue;
            }
            children.add(b);
        }
        return children;
    }

    /**
     * Look up this backup's immediate parent in the chain (by {@code PARENT_BACKUP_ID}).
     * Returns {@code null} if this is the full (no parent) or the parent row is gone.
     */
    private Backup findChainParent(Backup backup) {
        String parentUuid = readDetail(backup, NASBackupChainKeys.PARENT_BACKUP_ID);
        if (parentUuid == null || parentUuid.isEmpty()) {
            return null;
        }
        for (Backup b : backupDao.listByVmId(null, backup.getVmId())) {
            if (parentUuid.equals(b.getUuid())) {
                return b;
            }
        }
        return null;
    }

    /**
     * Physically delete {@code backup}, then walk up the chain while each ancestor is in
     * delete-pending state with no other live children. Mirrors the snapshot subsystem
     * pattern: once a leaf is gone, garbage-collect any tombstoned parents.
     */
    private boolean deleteBackupAndSweepPendingAncestors(Backup backup, BackupRepository repo, Host host) {
        if (!deleteBackupFileAndRow(backup, repo, host)) {
            return false;
        }
        Backup parent = findChainParent(backup);
        while (parent != null && isDeletePending(parent) && findLiveChildren(parent).isEmpty()) {
            Backup nextParent = findChainParent(parent);
            if (!deleteBackupFileAndRow(parent, repo, host)) {
                // Stop the sweep; the rest of the tombstoned chain will be collected on a
                // future delete that re-runs the sweep.
                return true;
            }
            parent = nextParent;
        }
        return true;
    }

    /**
     * Forced delete: remove this backup plus every descendant, leaf-first. Used when the
     * caller explicitly passes {@code forced=true} and wants the whole subtree gone now.
     */
    private boolean cascadeDeleteSubtree(Backup root, BackupRepository repo, Host host) {
        List<Backup> subtree = collectSubtree(root);
        // Sort by chain_position descending so leaves go first — this keeps every rm
        // operating on a chain that still resolves, even if the user is watching mid-flight.
        subtree.sort(Comparator.comparingInt((Backup b) -> chainPosition(b)).reversed());
        boolean ok = true;
        for (Backup victim : subtree) {
            if (!deleteBackupFileAndRow(victim, repo, host)) {
                ok = false;
                break;
            }
        }
        return ok;
    }

    /**
     * Collect {@code root} plus every transitive child in the same chain. BFS-style.
     */
    private List<Backup> collectSubtree(Backup root) {
        List<Backup> result = new ArrayList<>();
        result.add(root);
        int idx = 0;
        while (idx < result.size()) {
            Backup cur = result.get(idx++);
            // findLiveChildren skips delete-pending — for forced cascade we want them too.
            String parentUuid = cur.getUuid();
            String chainId = readDetail(cur, NASBackupChainKeys.CHAIN_ID);
            if (parentUuid == null || chainId == null) {
                continue;
            }
            for (Backup b : backupDao.listByVmId(null, cur.getVmId())) {
                if (b.getId() == cur.getId()) {
                    continue;
                }
                if (!chainId.equals(readDetail(b, NASBackupChainKeys.CHAIN_ID))) {
                    continue;
                }
                if (parentUuid.equals(readDetail(b, NASBackupChainKeys.PARENT_BACKUP_ID))) {
                    result.add(b);
                }
            }
        }
        return result;
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
