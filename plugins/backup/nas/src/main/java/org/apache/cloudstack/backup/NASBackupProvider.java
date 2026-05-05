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
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
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
        final String mode;          // "full" or "incremental"
        final String bitmapNew;
        final String bitmapParent;  // null for full
        final String parentPath;    // null for full
        final String chainId;       // chain identifier this backup belongs to
        final int chainPosition;    // 0 for full, N for the Nth incremental in the chain

        private ChainDecision(String mode, String bitmapNew, String bitmapParent, String parentPath,
                              String chainId, int chainPosition) {
            this.mode = mode;
            this.bitmapNew = bitmapNew;
            this.bitmapParent = bitmapParent;
            this.parentPath = parentPath;
            this.chainId = chainId;
            this.chainPosition = chainPosition;
        }

        static ChainDecision fullStart(String bitmapName) {
            return new ChainDecision(NASBackupChainKeys.TYPE_FULL, bitmapName, null, null,
                    UUID.randomUUID().toString(), 0);
        }

        static ChainDecision incremental(String bitmapNew, String bitmapParent, String parentPath,
                                         String chainId, int chainPosition) {
            return new ChainDecision(NASBackupChainKeys.TYPE_INCREMENTAL, bitmapNew, bitmapParent,
                    parentPath, chainId, chainPosition);
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
     */
    protected ChainDecision decideChain(VirtualMachine vm) {
        final String newBitmap = "backup-" + System.currentTimeMillis() / 1000L;

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

        // Walk this VM's backups newest→oldest, find the most recent BackedUp backup that has a
        // bitmap stored. If we don't find one, this is the first backup in a chain — start full.
        List<Backup> history = backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
        if (history == null || history.isEmpty()) {
            return ChainDecision.fullStart(newBitmap);
        }
        history.sort(Comparator.comparing(Backup::getDate).reversed());

        Backup parent = null;
        String parentBitmap = null;
        String parentChainId = null;
        int parentChainPosition = -1;
        for (Backup b : history) {
            if (!Backup.Status.BackedUp.equals(b.getStatus())) {
                continue;
            }
            String bm = readDetail(b, NASBackupChainKeys.BITMAP_NAME);
            if (bm == null) {
                continue;
            }
            parent = b;
            parentBitmap = bm;
            parentChainId = readDetail(b, NASBackupChainKeys.CHAIN_ID);
            String posStr = readDetail(b, NASBackupChainKeys.CHAIN_POSITION);
            try {
                parentChainPosition = posStr == null ? 0 : Integer.parseInt(posStr);
            } catch (NumberFormatException e) {
                parentChainPosition = 0;
            }
            break;
        }
        if (parent == null || parentBitmap == null || parentChainId == null) {
            return ChainDecision.fullStart(newBitmap);
        }

        // Force a fresh full when the chain has reached the configured length.
        if (parentChainPosition + 1 >= fullEvery) {
            return ChainDecision.fullStart(newBitmap);
        }

        // The script needs the parent backup's on-NAS file path so it can rebase the new
        // qcow2 onto it. The path is stored relative to the NAS mount point — the script
        // resolves it inside its mount session.
        String parentPath = composeParentBackupPath(parent);
        return ChainDecision.incremental(newBitmap, parentBitmap, parentPath,
                parentChainId, parentChainPosition + 1);
    }

    private String readDetail(Backup backup, String key) {
        BackupDetailVO d = backupDetailsDao.findDetail(backup.getId(), key);
        return d == null ? null : d.getValue();
    }

    /**
     * Compose the on-NAS path of a parent backup's root-disk qcow2. Relative to the NAS mount,
     * matches the layout written by {@code nasbackup.sh} ({@code <backupPath>/root.<volUuid>.qcow2}).
     */
    private String composeParentBackupPath(Backup parent) {
        // backupPath is stored as externalId by createBackupObject — e.g. "i-2-1234-VM/2026.04.27.13.45.00".
        // Volume UUID for the root volume is what the script keys backup files on.
        VolumeVO rootVolume = volumeDao.getInstanceRootVolume(parent.getVmId());
        String volUuid = rootVolume == null ? "root" : rootVolume.getUuid();
        return parent.getExternalId() + "/root." + volUuid + ".qcow2";
    }

    /**
     * Persist chain metadata under backup_details. Stored here (not on the backups table) so
     * other providers can implement their own chain semantics without schema changes.
     */
    private void persistChainMetadata(Backup backup, ChainDecision decision, String bitmapFromAgent) {
        // Prefer the bitmap name confirmed by the agent (BITMAP_CREATED= line). Fall back to
        // what we asked it to create — they should match.
        String bitmap = bitmapFromAgent != null ? bitmapFromAgent : decision.bitmapNew;
        if (bitmap != null) {
            backupDetailsDao.persist(new BackupDetailVO(backup.getId(), NASBackupChainKeys.BITMAP_NAME, bitmap, true));
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
        command.setParentPath(decision.parentPath);

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

        // Repair the chain (if any) before removing the backup file. For chained backups,
        // children that point at this backup must be re-pointed at this backup's parent
        // (with their blocks merged via qemu-img rebase). For a full at the head of a chain
        // with surviving children, refuse unless forced — `forced=true` then deletes the
        // full plus every descendant.
        ChainRepairPlan plan = computeChainRepair(backup, forced);
        if (!plan.proceed) {
            throw new CloudRuntimeException(plan.reason);
        }

        // Issue rebase commands for each child that needs re-pointing (ordered so each rebase
        // operates on a chain that still resolves: children first if there are nested ones).
        for (RebaseStep step : plan.rebaseSteps) {
            RebaseBackupCommand rebase = new RebaseBackupCommand(step.targetMountRelativePath,
                    step.newBackingMountRelativePath, backupRepository.getType(),
                    backupRepository.getAddress(), backupRepository.getMountOptions());
            BackupAnswer rebaseAnswer;
            try {
                rebaseAnswer = (BackupAnswer) agentManager.send(host.getId(), rebase);
            } catch (AgentUnavailableException e) {
                throw new CloudRuntimeException("Unable to contact backend control plane to repair backup chain");
            } catch (OperationTimedoutException e) {
                throw new CloudRuntimeException("Backup chain repair (rebase) timed out, please try again");
            }
            if (rebaseAnswer == null || !rebaseAnswer.getResult()) {
                throw new CloudRuntimeException(String.format(
                        "Backup chain repair failed: rebase of %s onto %s returned %s",
                        step.targetMountRelativePath, step.newBackingMountRelativePath,
                        rebaseAnswer == null ? "no answer" : rebaseAnswer.getDetails()));
            }
            // Update the rebased child's parent reference + position in backup_details.
            BackupDetailVO parentDetail = backupDetailsDao.findDetail(step.childBackupId, NASBackupChainKeys.PARENT_BACKUP_ID);
            if (parentDetail != null) {
                parentDetail.setValue(step.newParentUuid == null ? "" : step.newParentUuid);
                backupDetailsDao.update(parentDetail.getId(), parentDetail);
            } else if (step.newParentUuid != null) {
                backupDetailsDao.persist(new BackupDetailVO(step.childBackupId,
                        NASBackupChainKeys.PARENT_BACKUP_ID, step.newParentUuid, true));
            }
            BackupDetailVO posDetail = backupDetailsDao.findDetail(step.childBackupId, NASBackupChainKeys.CHAIN_POSITION);
            if (posDetail != null) {
                posDetail.setValue(String.valueOf(step.newChainPosition));
                backupDetailsDao.update(posDetail.getId(), posDetail);
            }
        }

        // Now delete this backup's files. For a forced delete of a full with descendants we
        // also delete all descendants' files (newest first so each rm targets a leaf).
        for (Backup victim : plan.toDelete) {
            DeleteBackupCommand command = new DeleteBackupCommand(victim.getExternalId(), backupRepository.getType(),
                    backupRepository.getAddress(), backupRepository.getMountOptions());
            BackupAnswer answer;
            try {
                answer = (BackupAnswer) agentManager.send(host.getId(), command);
            } catch (AgentUnavailableException e) {
                throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
            } catch (OperationTimedoutException e) {
                throw new CloudRuntimeException("Operation to delete backup timed out, please try again");
            }
            if (answer == null || !answer.getResult()) {
                logger.warn("Failed to delete backup file for {} ({}); leaving DB row intact", victim.getUuid(), victim.getExternalId());
                return false;
            }
            backupDao.remove(victim.getId());
        }

        // Shift chain_position down by 1 for any survivors deeper in the chain than the
        // backup we just removed (their direct parent reference is unchanged, but their
        // numeric position needs to stay consistent so future full-every cadence math works).
        if (plan.shiftPositionsBelow != null) {
            for (Backup b : backupDao.listByVmId(null, backup.getVmId())) {
                if (!plan.shiftPositionsBelow.chainId.equals(readDetail(b, NASBackupChainKeys.CHAIN_ID))) {
                    continue;
                }
                int pos = chainPosition(b);
                if (pos > plan.shiftPositionsBelow.afterPosition && pos != Integer.MAX_VALUE) {
                    BackupDetailVO posDetail = backupDetailsDao.findDetail(b.getId(), NASBackupChainKeys.CHAIN_POSITION);
                    if (posDetail != null) {
                        posDetail.setValue(String.valueOf(pos - 1));
                        backupDetailsDao.update(posDetail.getId(), posDetail);
                    }
                }
            }
        }

        return true;
    }

    private static final class PositionShift {
        final String chainId;
        final int afterPosition; // shift positions strictly greater than this by -1
        PositionShift(String chainId, int afterPosition) {
            this.chainId = chainId;
            this.afterPosition = afterPosition;
        }
    }

    /**
     * Result of {@link #computeChainRepair}: whether to proceed, what to rebase, what to delete.
     */
    private static final class ChainRepairPlan {
        final boolean proceed;
        final String reason;
        final List<RebaseStep> rebaseSteps;
        final List<Backup> toDelete;
        final PositionShift shiftPositionsBelow;

        private ChainRepairPlan(boolean proceed, String reason, List<RebaseStep> rebaseSteps, List<Backup> toDelete,
                                PositionShift shiftPositionsBelow) {
            this.proceed = proceed;
            this.reason = reason;
            this.rebaseSteps = rebaseSteps;
            this.toDelete = toDelete;
            this.shiftPositionsBelow = shiftPositionsBelow;
        }

        static ChainRepairPlan refuse(String reason) {
            return new ChainRepairPlan(false, reason, Collections.emptyList(), Collections.emptyList(), null);
        }

        static ChainRepairPlan proceed(List<RebaseStep> rebaseSteps, List<Backup> toDelete) {
            return new ChainRepairPlan(true, null, rebaseSteps, toDelete, null);
        }

        static ChainRepairPlan proceed(List<RebaseStep> rebaseSteps, List<Backup> toDelete, PositionShift shift) {
            return new ChainRepairPlan(true, null, rebaseSteps, toDelete, shift);
        }
    }

    private static final class RebaseStep {
        final long childBackupId;
        final String targetMountRelativePath;
        final String newBackingMountRelativePath;
        final String newParentUuid;        // null when re-pointed onto an existing full's UUID is desired but unavailable
        final int newChainPosition;

        RebaseStep(long childBackupId, String targetMountRelativePath, String newBackingMountRelativePath,
                   String newParentUuid, int newChainPosition) {
            this.childBackupId = childBackupId;
            this.targetMountRelativePath = targetMountRelativePath;
            this.newBackingMountRelativePath = newBackingMountRelativePath;
            this.newParentUuid = newParentUuid;
            this.newChainPosition = newChainPosition;
        }
    }

    /**
     * Compute the chain-repair plan for deleting {@code backup}. Conservative semantics:
     *   - Backups outside any tracked chain (no NAS chain metadata) are deleted as-is.
     *   - A standalone backup with no children is deleted as-is.
     *   - A middle incremental: rebase its immediate child onto its own parent, then delete it.
     *     Descendants of that child are unaffected (their backing chain still resolves).
     *   - A full with surviving descendants: refuse unless {@code forced=true}; then delete
     *     full + every descendant (newest first).
     */
    private ChainRepairPlan computeChainRepair(Backup backup, boolean forced) {
        String chainId = readDetail(backup, NASBackupChainKeys.CHAIN_ID);
        if (chainId == null) {
            // Pre-incremental backups (or callers that never wrote chain metadata) — single delete.
            return ChainRepairPlan.proceed(Collections.emptyList(), Collections.singletonList(backup));
        }

        // Gather every backup in the same chain for this VM.
        List<Backup> chain = new ArrayList<>();
        for (Backup b : backupDao.listByVmId(null, backup.getVmId())) {
            if (chainId.equals(readDetail(b, NASBackupChainKeys.CHAIN_ID))) {
                chain.add(b);
            }
        }
        chain.sort(Comparator.comparingInt(b -> chainPosition(b)));

        int targetPos = chainPosition(backup);
        boolean isFull = targetPos == 0;
        List<Backup> descendants = chain.stream()
                .filter(b -> chainPosition(b) > targetPos)
                .collect(Collectors.toList());

        if (isFull) {
            if (descendants.isEmpty()) {
                return ChainRepairPlan.proceed(Collections.emptyList(), Collections.singletonList(backup));
            }
            if (!forced) {
                return ChainRepairPlan.refuse(String.format(
                        "Backup %s is the full anchor of a chain with %d incremental(s). Delete the incrementals first, " +
                                "or pass forced=true to remove the entire chain.",
                        backup.getUuid(), descendants.size()));
            }
            // Forced delete: remove descendants newest first, then the full.
            List<Backup> victims = new ArrayList<>(descendants);
            victims.sort(Comparator.comparingInt((Backup b) -> chainPosition(b)).reversed());
            victims.add(backup);
            return ChainRepairPlan.proceed(Collections.emptyList(), victims);
        }

        // Middle (or tail) incremental.
        if (descendants.isEmpty()) {
            // Tail: nothing to rebase, just delete.
            return ChainRepairPlan.proceed(Collections.emptyList(), Collections.singletonList(backup));
        }

        // Middle: only the immediate child needs to absorb our blocks and rebase onto our parent.
        Backup immediateChild = descendants.stream()
                .min(Comparator.comparingInt(b -> chainPosition(b)))
                .orElseThrow(() -> new CloudRuntimeException("Internal error: no immediate child found for chain repair"));
        Backup ourParent = chain.stream()
                .filter(b -> chainPosition(b) == targetPos - 1)
                .findFirst()
                .orElseThrow(() -> new CloudRuntimeException(String.format(
                        "Cannot delete %s: its parent (chain_position=%d) is missing from the chain",
                        backup.getUuid(), targetPos - 1)));

        VolumeVO rootVolume = volumeDao.getInstanceRootVolume(backup.getVmId());
        String volUuid = rootVolume == null ? "root" : rootVolume.getUuid();
        String childPath = immediateChild.getExternalId() + "/root." + volUuid + ".qcow2";
        String parentPath = ourParent.getExternalId() + "/root." + volUuid + ".qcow2";

        RebaseStep step = new RebaseStep(immediateChild.getId(), childPath, parentPath,
                ourParent.getUuid(), chainPosition(immediateChild) - 1);

        // After we delete the middle backup, every descendant's numeric chain_position
        // becomes stale (off by one). Their backing-file pointers don't need re-writing
        // (only the immediate child changed parents) but their position metadata does.
        return ChainRepairPlan.proceed(
                Collections.singletonList(step),
                Collections.singletonList(backup),
                new PositionShift(chainId, targetPos));
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
                NASBackupFullEvery
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
