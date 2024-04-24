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

package org.apache.cloudstack.storage.volume;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVolumesOnStorageAnswer;
import com.cloud.agent.api.GetVolumesOnStorageCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.volume.ImportVolumeCmd;
import org.apache.cloudstack.api.command.admin.volume.ListVolumesForImportCmd;
import org.apache.cloudstack.api.command.admin.volume.UnmanageVolumeCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VolumeForImportResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class VolumeImportUnmanageManagerImpl implements VolumeImportUnmanageService {
    protected Logger logger = LogManager.getLogger(VolumeImportUnmanageManagerImpl.class);

    @Inject
    private AccountManager accountMgr;
    @Inject
    private AgentManager agentManager;
    @Inject
    private HostDao hostDao;
    @Inject
    private DiskOfferingDao diskOfferingDao;
    @Inject
    private ResourceLimitService resourceLimitService;
    @Inject
    private ResponseGenerator responseGenerator;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private StoragePoolHostDao storagePoolHostDao;
    @Inject
    private ConfigurationManager configMgr;
    @Inject
    private DataCenterDao dcDao;
    @Inject
    private VolumeOrchestrationService volumeManager;
    @Inject
    private VMTemplatePoolDao templatePoolDao;
    @Inject
    private VolumeApiService volumeApiService;
    @Inject
    private SnapshotDataStoreDao snapshotDataStoreDao;

    static final String DEFAULT_DISK_OFFERING_NAME = "Default Custom Offering for Volume Import";
    static final String DEFAULT_DISK_OFFERING_UNIQUE_NAME = "Volume-Import";
    static final String DISK_OFFERING_NAME_SUFFIX_LOCAL = " - Local Storage";
    static final String DISK_OFFERING_UNIQUE_NAME_SUFFIX_LOCAL = "-Local";

    protected void logFailureAndThrowException(String msg) {
        logger.error(msg);
        throw new CloudRuntimeException(msg);
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ListVolumesForImportCmd.class);
        cmdList.add(ImportVolumeCmd.class);
        cmdList.add(UnmanageVolumeCmd.class);
        return cmdList;
    }

    @Override
    public ListResponse<VolumeForImportResponse> listVolumesForImport(ListVolumesForImportCmd cmd) {
        Long poolId = cmd.getStorageId();
        String path = cmd.getPath();
        String keyword = cmd.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            keyword = keyword.trim();
        }

        StoragePoolVO pool = checkIfPoolAvailable(poolId);
        List<VolumeOnStorageTO> volumes = listVolumesForImportInternal(pool, path, keyword);

        List<VolumeForImportResponse> responses = new ArrayList<>();
        for (VolumeOnStorageTO volume : volumes) {
            if (checkIfVolumeManaged(pool, volume.getPath())
                    || checkIfVolumeForTemplate(pool, volume.getPath())
                    || checkIfVolumeForSnapshot(pool, volume.getFullPath())) {
                continue;
            }
            responses.add(createVolumeForImportResponse(volume, pool));
        }
        ListResponse<VolumeForImportResponse> listResponses = new ListResponse<>();
        listResponses.setResponses(responses, responses.size());
        return listResponses;
    }

    @Override
    public VolumeResponse importVolume(ImportVolumeCmd cmd) {
        // 1. verify owner
        final Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.Type.ADMIN) {
            throw new PermissionDeniedException(String.format("Cannot import VM as the caller account [%s] is not ROOT Admin.", caller.getUuid()));
        }
        Account owner = accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        if (owner == null) {
            logFailureAndThrowException("Cannot import volume due to unknown owner");
        }

        // 2. check if pool exists and not in maintenance
        Long poolId = cmd.getStorageId();
        StoragePoolVO pool = checkIfPoolAvailable(poolId);

        // 3. check if the volume already exists in cloudstack by path
        String volumePath = cmd.getPath();
        if (StringUtils.isBlank(volumePath)) {
            logFailureAndThrowException("Volume path is null or blank: " + volumePath);
        }
        if (checkIfVolumeManaged(pool, volumePath)){
            logFailureAndThrowException("Volume is already managed by CloudStack: " + volumePath);
        }
        if (checkIfVolumeForTemplate(pool, volumePath)) {
            logFailureAndThrowException("Volume is a base image of a template: " + volumePath);
        }

        // 4. get volume info on storage through host and check
        VolumeOnStorageTO volume = getVolumeOnStorageAndCheck(pool, volumePath);

        if (checkIfVolumeForSnapshot(pool, volume.getFullPath())) {
            logFailureAndThrowException("Volume is a reference of snapshot on primary: " + volume.getFullPath());
        }

        // 5. check resource limitation
        checkResourceLimitForImportVolume(owner, volume);

        // 6. get disk offering
        DiskOfferingVO diskOffering = getOrCreateDiskOffering(owner, cmd.getDiskOfferingId(), pool.getDataCenterId(), pool.isLocal());
        if (diskOffering.isCustomized()) {
            volumeApiService.validateCustomDiskOfferingSizeRange(volume.getVirtualSize() / ByteScaleUtils.GiB);
        }
        if (!volumeApiService.doesTargetStorageSupportDiskOffering(pool, diskOffering.getTags())) {
            logFailureAndThrowException(String.format("Disk offering: %s storage tags are not compatible with selected storage pool: %s", diskOffering.getUuid(), pool.getUuid()));
        }

        // 7. create records
        String volumeName = StringUtils.isNotBlank(cmd.getName()) ? cmd.getName().trim() : volumePath;
        VolumeVO volumeVO = importVolumeInternal(volume, diskOffering, owner, pool, volumeName);

        // 8. Update resource count
        updateResourceLimitForVolumeImport(volumeVO);

        // 9. Publish event
        publicUsageEventForVolumeImportAndUnmanage(volumeVO, true);

        return responseGenerator.createVolumeResponse(ResponseObject.ResponseView.Full, volumeVO);
    }

    protected VolumeOnStorageTO getVolumeOnStorageAndCheck(StoragePoolVO pool, String volumePath) {
        // send a command to hypervisor to check
        List<VolumeOnStorageTO> volumes = listVolumesForImportInternal(pool, volumePath, null);
        if (CollectionUtils.isEmpty(volumes)) {
            logFailureAndThrowException("Cannot find volume on storage pool: " + volumePath);
        }

        VolumeOnStorageTO volume = volumes.get(0);

        // check if volume is locked, encrypted or has backing file
        checkIfVolumeIsLocked(volume);
        checkIfVolumeIsEncrypted(volume);
        checkIfVolumeHasBackingFile(volume);

        return volume;
    }

    protected List<VolumeOnStorageTO> listVolumesForImportInternal(StoragePoolVO pool, String volumePath, String keyword) {
        Pair<HostVO, String> hostAndLocalPath = findHostAndLocalPathForVolumeImport(pool);
        HostVO host = hostAndLocalPath.first();
        checkIfHostAndPoolSupported(host, pool);

        StorageFilerTO storageTO = new StorageFilerTO(pool);
        GetVolumesOnStorageCommand command = new GetVolumesOnStorageCommand(storageTO, volumePath, keyword);
        Answer answer = agentManager.easySend(host.getId(), command);
        if (answer == null || !(answer instanceof GetVolumesOnStorageAnswer)) {
            logFailureAndThrowException("Cannot get volumes on storage pool via host " + host.getName());
        }
        if (!answer.getResult()) {
            logFailureAndThrowException("Volume cannot be imported due to " + answer.getDetails());
        }
        return  ((GetVolumesOnStorageAnswer) answer).getVolumes();
    }

    @Override
    public boolean unmanageVolume(long volumeId) {
        // 1. check if volume can be unmanaged
        VolumeVO volume = checkIfVolumeCanBeUnmanaged(volumeId);

        // 2. check if pool available
        StoragePoolVO pool = checkIfPoolAvailable(volume.getPoolId());

        // 3. unmanage volume internally
        getVolumeOnStorageAndCheck(pool, volume.getPath());

        // 3. Update resource count
        updateResourceLimitForVolumeUnmanage(volume);

        // 4. publish events
        publicUsageEventForVolumeImportAndUnmanage(volume, false);

        // 5. update the state/removed of record
        unmanageVolumeFromDatabase(volume);

        return true;
    }

    protected StoragePoolVO checkIfPoolAvailable(Long poolId) {
        StoragePoolVO pool = primaryDataStoreDao.findById(poolId);
        if (pool == null) {
            logFailureAndThrowException(String.format("Storage pool (ID: %s) does not exist", poolId));
        }
        if (pool.isInMaintenance()) {
            logFailureAndThrowException(String.format("Storage pool (name: %s) is in maintenance", pool.getName()));
        }
        if (!StoragePoolStatus.Up.equals(pool.getStatus())) {
            logFailureAndThrowException(String.format("Storage pool (ID: %s) is not Up: %s", pool.getName(), pool.getStatus()));
        }
        return pool;
    }

    protected Pair<HostVO, String> findHostAndLocalPathForVolumeImport(StoragePoolVO pool) {
        List<HostVO> hosts = new ArrayList<>();
        switch (pool.getScope()) {
            case HOST:
                return findHostAndLocalPathForVolumeImportForHostScope(pool.getId());
            case CLUSTER:
                hosts = hostDao.findHypervisorHostInCluster((pool.getClusterId()));
                break;
            case ZONE:
                hosts = hostDao.listAllHostsUpByZoneAndHypervisor(pool.getDataCenterId(), pool.getHypervisor());
                break;
        }
        for (HostVO host : hosts) {
            StoragePoolHostVO storagePoolHostVO = storagePoolHostDao.findByPoolHost(pool.getId(), host.getId());
            if (storagePoolHostVO != null) {
                return new Pair<>(host, storagePoolHostVO.getLocalPath());
            }
        }
        logFailureAndThrowException("No host found to perform volume import");
        return null;
    }

    private Pair<HostVO, String> findHostAndLocalPathForVolumeImportForHostScope(Long poolId) {
        List<StoragePoolHostVO> storagePoolHostVOs = storagePoolHostDao.listByPoolId(poolId);
        if (CollectionUtils.isNotEmpty(storagePoolHostVOs)) {
            for (StoragePoolHostVO storagePoolHostVO : storagePoolHostVOs) {
                HostVO host = hostDao.findById(storagePoolHostVO.getHostId());
                if (host != null) {
                    return new Pair<>(host, storagePoolHostVO.getLocalPath());
                }
            }
        }
        logFailureAndThrowException("No host found to perform volume import on pool: " + poolId);
        return null;
    }

    private void checkIfHostAndPoolSupported(HostVO host, StoragePoolVO pool) {
        if (!SUPPORTED_HYPERVISORS.contains(host.getHypervisorType())) {
            logFailureAndThrowException("Importing and unmanaging volume are not supported for hypervisor: " + host.getHypervisorType());
        }

        if (Hypervisor.HypervisorType.KVM.equals(host.getHypervisorType()) && !SUPPORTED_STORAGE_POOL_TYPES_FOR_KVM.contains(pool.getPoolType())) {
            logFailureAndThrowException(String.format("Importing and unmanaging volume are not supported for pool type %s on hypervisor %s", pool.getPoolType(), host.getHypervisorType()));
        }
    }

    protected VolumeForImportResponse createVolumeForImportResponse(VolumeOnStorageTO volume, StoragePoolVO pool) {
        VolumeForImportResponse response = new VolumeForImportResponse();
        response.setPath(volume.getPath());
        response.setName(volume.getName());
        response.setFullPath(volume.getFullPath());
        response.setFormat(volume.getFormat());
        response.setSize(volume.getSize());
        response.setVirtualSize(volume.getVirtualSize());
        response.setQemuEncryptFormat(volume.getQemuEncryptFormat());
        response.setStoragePoolId(pool.getUuid());
        response.setStoragePoolName(pool.getName());
        response.setStoragePoolType(String.valueOf(pool.getPoolType()));
        response.setDetails(volume.getDetails());
        response.setObjectName("volumeforimport");
        return response;
    }

    private boolean checkIfVolumeManaged(StoragePoolVO pool, String volumePath) {
        return volumeDao.findByPoolIdAndPath(pool.getId(), volumePath) != null;
    }

    private boolean checkIfVolumeForTemplate(StoragePoolVO pool, String volumePath) {
        return templatePoolDao.findByPoolPath(pool.getId(), volumePath) != null;
    }

    private boolean checkIfVolumeForSnapshot(StoragePoolVO pool, String fullVolumePath) {
        List<String> absPathList = Arrays.asList(fullVolumePath);
        return CollectionUtils.isNotEmpty(snapshotDataStoreDao.listByStoreAndInstallPaths(pool.getId(), DataStoreRole.Primary, absPathList));
    }

    protected void checkIfVolumeIsLocked(VolumeOnStorageTO volume) {
        Map<VolumeOnStorageTO.Detail, String> volumeDetails = volume.getDetails();
        if (volumeDetails != null && volumeDetails.containsKey(VolumeOnStorageTO.Detail.IS_LOCKED)) {
            String isLocked = volumeDetails.get(VolumeOnStorageTO.Detail.IS_LOCKED);
            if (Boolean.parseBoolean(isLocked)) {
                logFailureAndThrowException("Locked volume cannot be imported or unmanaged.");
            }
        }
    }

    protected void checkIfVolumeIsEncrypted(VolumeOnStorageTO volume) {
        Map<VolumeOnStorageTO.Detail, String> volumeDetails = volume.getDetails();
        if (volumeDetails != null && volumeDetails.containsKey(VolumeOnStorageTO.Detail.IS_ENCRYPTED)) {
            String isEncrypted = volumeDetails.get(VolumeOnStorageTO.Detail.IS_ENCRYPTED);
            if (Boolean.parseBoolean(isEncrypted)) {
                logFailureAndThrowException("Encrypted volume cannot be imported or unmanaged.");
            }
        }
    }

    protected void checkIfVolumeHasBackingFile(VolumeOnStorageTO volume) {
        Map<VolumeOnStorageTO.Detail, String> volumeDetails = volume.getDetails();
        if (volumeDetails != null && volumeDetails.containsKey(VolumeOnStorageTO.Detail.BACKING_FILE)) {
            String backingFile = volumeDetails.get(VolumeOnStorageTO.Detail.BACKING_FILE);
            if (StringUtils.isNotBlank(backingFile)) {
                logFailureAndThrowException("Volume with backing file cannot be imported or unmanaged.");
            }
        }
    }

    protected DiskOfferingVO getOrCreateDiskOffering(Account owner, Long diskOfferingId, Long zoneId, boolean isLocal) {
        if (diskOfferingId != null) {
            // check if disk offering exists and active
            DiskOfferingVO diskOfferingVO = diskOfferingDao.findById(diskOfferingId);
            if (diskOfferingVO == null) {
                logFailureAndThrowException(String.format("Disk offering %s does not exist", diskOfferingId));
            }
            if (!DiskOffering.State.Active.equals(diskOfferingVO.getState())) {
                logFailureAndThrowException(String.format("Disk offering with ID %s is not active", diskOfferingId));
            }
            if (diskOfferingVO.isUseLocalStorage() != isLocal) {
                logFailureAndThrowException(String.format("Disk offering with ID %s should use %s storage", diskOfferingId, isLocal ? "local": "shared"));
            }
            if (diskOfferingVO.getEncrypt()) {
                logFailureAndThrowException(String.format("Disk offering with ID %s should not support volume encryption", diskOfferingId));
            }
            // check if disk offering is accessible by the account/owner
            try {
                configMgr.checkDiskOfferingAccess(owner, diskOfferingVO, dcDao.findById(zoneId));
                return diskOfferingVO;
            } catch (PermissionDeniedException ex) {
                logFailureAndThrowException(String.format("Disk offering with ID %s is not accessible by owner %s", diskOfferingId, owner));
            }
        }
        return getOrCreateDefaultDiskOfferingIdForVolumeImport(isLocal);
    }

    private DiskOfferingVO getOrCreateDefaultDiskOfferingIdForVolumeImport(boolean isLocalStorage) {
        final StringBuilder diskOfferingNameBuilder = new StringBuilder(DEFAULT_DISK_OFFERING_NAME);
        final StringBuilder uniqueNameBuilder = new StringBuilder(DEFAULT_DISK_OFFERING_UNIQUE_NAME);
        if (isLocalStorage) {
            diskOfferingNameBuilder.append(DISK_OFFERING_NAME_SUFFIX_LOCAL);
            uniqueNameBuilder.append(DISK_OFFERING_UNIQUE_NAME_SUFFIX_LOCAL);
        }
        final String diskOfferingName = diskOfferingNameBuilder.toString();
        final String uniqueName = uniqueNameBuilder.toString();
        DiskOfferingVO diskOffering = diskOfferingDao.findByUniqueName(uniqueName);
        if (diskOffering != null) {
            return diskOffering;
        }
        DiskOfferingVO newDiskOffering = new DiskOfferingVO(diskOfferingName, diskOfferingName,
                Storage.ProvisioningType.THIN, 0, null, true, null, null, null);
        newDiskOffering.setUseLocalStorage(isLocalStorage);
        newDiskOffering.setUniqueName(uniqueName);
        newDiskOffering = diskOfferingDao.persistDefaultDiskOffering(newDiskOffering);
        return newDiskOffering;
    }

    private VolumeVO importVolumeInternal(VolumeOnStorageTO volume, DiskOfferingVO diskOffering,
                                          Account owner, StoragePoolVO pool, String volumeName) {
        DiskProfile diskProfile = volumeManager.importVolume(Volume.Type.DATADISK, volumeName, diskOffering,
                volume.getVirtualSize(), null, null, pool.getDataCenterId(), volume.getHypervisorType(), null, null,
                owner, null, pool.getId(), volume.getPath(), null);
        return volumeDao.findById(diskProfile.getVolumeId());
    }

    protected void checkResourceLimitForImportVolume(Account owner, VolumeOnStorageTO volume) {
        Long volumeSize = volume.getVirtualSize();
        try {
            resourceLimitService.checkResourceLimit(owner, Resource.ResourceType.volume);
            resourceLimitService.checkResourceLimit(owner, Resource.ResourceType.primary_storage, volumeSize);
        } catch (ResourceAllocationException e) {
            logger.error(String.format("VM resource allocation error for account: %s", owner.getUuid()), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("VM resource allocation error for account: %s. %s", owner.getUuid(), StringUtils.defaultString(e.getMessage())));
        }
    }

    private void updateResourceLimitForVolumeImport(VolumeVO volumeVO) {
        resourceLimitService.incrementResourceCount(volumeVO.getAccountId(), Resource.ResourceType.volume);
        resourceLimitService.incrementResourceCount(volumeVO.getAccountId(), Resource.ResourceType.primary_storage, volumeVO.getSize());
    }

    private void publicUsageEventForVolumeImportAndUnmanage(VolumeVO volumeVO, boolean isImport) {
        try {
            String eventType = isImport ? EventTypes.EVENT_VOLUME_IMPORT: EventTypes.EVENT_VOLUME_UNMANAGE;
            String eventDescription = isImport ? "Successfully imported volume " + volumeVO.getUuid(): "Successfully unmanaged volume "  + volumeVO.getUuid();
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, volumeVO.getAccountId(), EventVO.LEVEL_INFO,
                    eventType, eventDescription, volumeVO.getId(), ApiCommandResourceType.Volume.toString(),0);
            UsageEventUtils.publishUsageEvent(eventType, volumeVO.getAccountId(), volumeVO.getDataCenterId(),
                    volumeVO.getId(), volumeVO.getName(), volumeVO.getDiskOfferingId(), null, volumeVO.getSize(),
                    Volume.class.getName(), volumeVO.getUuid(), volumeVO.isDisplayVolume());
        } catch (Exception e) {
            logger.error(String.format("Failed to publish volume ID: %s event or usage records during volume import/unmanage", volumeVO.getUuid()), e);
        }
    }

    private void updateResourceLimitForVolumeUnmanage(VolumeVO volumeVO) {
        resourceLimitService.decrementResourceCount(volumeVO.getAccountId(), Resource.ResourceType.volume);
        resourceLimitService.decrementResourceCount(volumeVO.getAccountId(), Resource.ResourceType.primary_storage, volumeVO.getSize());
    }

    private VolumeVO checkIfVolumeCanBeUnmanaged(long volumeId) {
        VolumeVO volumeVO = volumeDao.findById(volumeId);
        if (volumeVO == null) {
            logFailureAndThrowException(String.format("Volume (ID: %s) does not exist", volumeId));
        }
        if (!Volume.State.Ready.equals(volumeVO.getState())) {
            logFailureAndThrowException(String.format("Volume (ID: %s) is not ready", volumeId));
        }
        if (volumeVO.getEncryptFormat() != null) {
            logFailureAndThrowException(String.format("Volume (ID: %s) is encrypted", volumeId));
        }
        if (volumeVO.getAttached() != null || volumeVO.getInstanceId() != null) {
            logFailureAndThrowException(String.format("Volume (ID: %s) is attached to VM (ID: %s)", volumeId, volumeVO.getInstanceId()));
        }
        return volumeVO;
    }

    private void unmanageVolumeFromDatabase(VolumeVO volume) {
        volume.setState(Volume.State.Destroy);
        volume.setRemoved(new Date());
        volumeDao.update(volume.getId(), volume);
    }
}
