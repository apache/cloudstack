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
import org.apache.cloudstack.backup.dao.BackupRepositoryDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
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
import java.util.Optional;
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

        final Date creationDate = new Date();
        final String backupPath = String.format("%s/%s", vm.getInstanceName(),
                new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(creationDate));

        BackupVO backupVO = createBackupObject(vm, backupPath);
        TakeBackupCommand command = new TakeBackupCommand(vm.getInstanceName(), backupPath);
        command.setBackupRepoType(backupRepository.getType());
        command.setBackupRepoAddress(backupRepository.getAddress());
        command.setMountOptions(backupRepository.getMountOptions());
        command.setQuiesce(quiesceVM);

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
            List<Volume> volumes = new ArrayList<>(volumeDao.findByInstance(vm.getId()));
            backupVO.setBackedUpVolumes(backupManager.createVolumeInfoFromVolumes(volumes));
            if (backupDao.update(backupVO.getId(), backupVO)) {
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

    private BackupVO createBackupObject(VirtualMachine vm, String backupPath) {
        BackupVO backup = new BackupVO();
        backup.setVmId(vm.getId());
        backup.setExternalId(backupPath);
        backup.setType("FULL");
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
        List<String> backedVolumesUUIDs = backup.getBackedUpVolumes().stream()
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
            volumePaths.add(String.format("%s/%s", volumePathPrefix, volume.getPath()));
        }
        return new Pair<>(volumePools, volumePaths);
    }

    private String getVolumePathPrefix(StoragePoolVO storagePool) {
        String volumePathPrefix;
        if (ScopeType.HOST.equals(storagePool.getScope()) ||
                Storage.StoragePoolType.SharedMountPoint.equals(storagePool.getPoolType()) ||
                Storage.StoragePoolType.RBD.equals(storagePool.getPoolType())) {
            volumePathPrefix = storagePool.getPath();
        } else {
            // Should be Storage.StoragePoolType.NetworkFilesystem
            volumePathPrefix = String.format("/mnt/%s", storagePool.getUuid());
        }
        return volumePathPrefix;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, Backup.VolumeInfo backupVolumeInfo, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
        final VolumeVO volume = volumeDao.findByUuid(backupVolumeInfo.getUuid());
        final DiskOffering diskOffering = diskOfferingDao.findByUuid(backupVolumeInfo.getDiskOfferingId());
        final StoragePoolVO pool = primaryDataStoreDao.findByUuid(dataStoreUuid);
        final HostVO hostVO = hostDao.findByIp(hostIp);

        LOG.debug("Restoring vm volume {} from backup {} on the NAS Backup Provider", backupVolumeInfo, backup);
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
        restoredVolume.setSize(backupVolumeInfo.getSize());
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
        restoreCommand.setRestoreVolumePaths(Collections.singletonList(String.format("%s/%s", getVolumePathPrefix(pool), volumeUUID)));
        DataStore dataStore = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
        restoreCommand.setRestoreVolumePools(Collections.singletonList(dataStore != null ? (PrimaryDataStoreTO)dataStore.getTO() : null));
        restoreCommand.setDiskType(backupVolumeInfo.getType().name().toLowerCase(Locale.ROOT));
        restoreCommand.setMountOptions(backupRepository.getMountOptions());
        restoreCommand.setVmExists(null);
        restoreCommand.setVmState(vmNameAndState.second());
        restoreCommand.setRestoreVolumeUUID(backupVolumeInfo.getUuid());

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

    private Optional<Backup.VolumeInfo> getBackedUpVolumeInfo(List<Backup.VolumeInfo> backedUpVolumes, String volumeUuid) {
        return backedUpVolumes.stream()
                .filter(v -> v.getUuid().equals(volumeUuid))
                .findFirst();
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

        DeleteBackupCommand command = new DeleteBackupCommand(backup.getExternalId(), backupRepository.getType(),
                backupRepository.getAddress(), backupRepository.getMountOptions());

        BackupAnswer answer;
        try {
            answer = (BackupAnswer) agentManager.send(host.getId(), command);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact backend control plane to initiate backup");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation to delete backup timed out, please try again");
        }

        if (answer != null && answer.getResult()) {
            return backupDao.remove(backup.getId());
        }

        logger.debug("There was an error removing the backup with id {}", backup.getId());
        return false;
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
        final Host host = resourceManager.findOneRandomRunningHostByHypervisor(Hypervisor.HypervisorType.KVM, zoneId);
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
                NASBackupRestoreMountTimeout
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
