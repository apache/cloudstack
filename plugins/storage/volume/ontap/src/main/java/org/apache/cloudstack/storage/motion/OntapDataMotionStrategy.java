/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.motion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.driver.OntapPrimaryDatastoreDriver;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo;
import com.cloud.agent.api.ModifyTargetsAnswer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * KVM volume and VM-with-volume migration when either side is NetApp ONTAP primary storage.
 * Same-SVM NFS offline copy is a CopyCommand from this strategy. Same-SVM iSCSI offline copy is done by the ONTAP driver before this strategy runs.
 */
@Component
public class OntapDataMotionStrategy implements DataMotionStrategy {
    protected Logger logger = LogManager.getLogger(getClass());

    private static final String ONTAP_SVM_NAME_DETAIL = "svmName";
    private static final String ONTAP_SVM_UUID_DETAIL = "svmUUID";
    private static final String ONTAP_STORAGE_IP_DETAIL = "storageIP";
    private static final String ONTAP_PROTOCOL_DETAIL = "protocol";
    private static final int QOS_PATCH_ATTEMPTS = 3;
    private static final long QOS_PATCH_RETRY_WAIT_MS = 2000L;

    @Inject
    private AgentManager _agentMgr;
    @Inject
    private DataStoreManager _dataStoreMgr;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private VolumeDetailsDao _volumeDetailsDao;
    @Inject
    private VolumeDataFactory _volumeDataFactory;
    @Inject
    private VolumeService _volumeService;
    @Inject
    private HostDao _hostDao;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private GuestOSDao _guestOsDao;
    @Inject
    private GuestOSCategoryDao _guestOsCategoryDao;

    @Override
    public StrategyPriority canHandle(DataObject srcData, DataObject destData) {
        if (!(srcData instanceof VolumeInfo) || !(destData instanceof VolumeInfo)) {
            return StrategyPriority.CANT_HANDLE;
        }
        VolumeInfo srcVolumeInfo = (VolumeInfo)srcData;
        VolumeInfo destVolumeInfo = (VolumeInfo)destData;
        if (srcVolumeInfo.getState() != Volume.State.Migrating
                || srcVolumeInfo.getDataStore().getRole() != DataStoreRole.Primary
                || destVolumeInfo.getDataStore().getRole() != DataStoreRole.Primary
                || srcVolumeInfo.getHypervisorType() != HypervisorType.KVM) {
            return StrategyPriority.CANT_HANDLE;
        }
        if (isOntapPool(_storagePoolDao.findById(srcVolumeInfo.getDataStore().getId()))
                || isOntapPool(_storagePoolDao.findById(destVolumeInfo.getDataStore().getId()))) {
            return StrategyPriority.HIGHEST;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        VolumeInfo srcVolumeInfo = (VolumeInfo)srcData;
        VolumeInfo destVolumeInfo = (VolumeInfo)destData;
        String errMsg = null;
        HostVO hostVO = null;
        try {
            checkAvailableForMigration(srcVolumeInfo.getAttachedVM());
            checkUnsupportedOntapCrossProtocolMigration(srcVolumeInfo, destVolumeInfo);

            destVolumeInfo.getDataStore().getDriver().createAsync(destVolumeInfo.getDataStore(), destVolumeInfo, null);
            // The offline destination keeps its normal state, so tag it for the driver's grantAccess.
            _volumeDetailsDao.addDetail(destVolumeInfo.getId(), OntapStorageConstants.MIGRATION_DESTINATION,
                    OntapStorageConstants.TRUE, false);
            VolumeVO volumeVO = _volumeDao.findById(destVolumeInfo.getId());
            if (volumeVO.get_iScsiName() != null) {
                volumeVO.setPath(volumeVO.get_iScsiName());
                _volumeDao.update(volumeVO.getId(), volumeVO);
            }
            destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId(), destVolumeInfo.getDataStore());
            hostVO = getHostToMigrateVolume(srcVolumeInfo, destVolumeInfo);

            _volumeService.grantAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());
            destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId(), destVolumeInfo.getDataStore());

            String path = sendVolumeMigrationCommand(srcVolumeInfo, destVolumeInfo, hostVO, isSameSvmNfsPair(srcVolumeInfo, destVolumeInfo));

            volumeVO = _volumeDao.findById(destVolumeInfo.getId());
            volumeVO.setPath(path);
            if (volumeVO.getFormat() == null) {
                volumeVO.setFormat(ImageFormat.QCOW2);
            }
            _volumeDao.update(volumeVO.getId(), volumeVO);
            // Host-side rewrite clears NFS file QoS; re-attach from the destination volume's IOPS.
            reapplyOntapQosAfterContentRewrite(destVolumeInfo);
        } catch (Exception ex) {
            errMsg = "Primary storage migration failed due to an unexpected error: " + ex.getMessage();
            if (ex instanceof CloudRuntimeException) {
                throw (CloudRuntimeException)ex;
            }
            throw new CloudRuntimeException(errMsg, ex);
        } finally {
            if (hostVO != null) {
                try {
                    _volumeService.revokeAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());
                } catch (Exception e) {
                    logger.warn("Failed to revoke access for volume [{}] after a migration attempt", destVolumeInfo.getUuid(), e);
                }
            }
            try {
                _volumeDetailsDao.removeDetail(destVolumeInfo.getId(), OntapStorageConstants.MIGRATION_DESTINATION);
            } catch (Exception e) {
                logger.warn("Failed to clear the migration tag on volume [{}]", destVolumeInfo.getUuid(), e);
            }
            completeVolumeMigrationCallback(destVolumeInfo, callback, errMsg);
        }
    }

    @Override
    public StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        if (srcHost.getHypervisorType() == HypervisorType.KVM && volumeMapIncludesOntapPool(volumeMap)) {
            return StrategyPriority.HIGHEST;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    /**
     * For each volume to move: create it on the destination pool, connect it on the destination host,
     * then migrate the VM and its storage with qemu.
     */
    @Override
    public void copyAsync(Map<VolumeInfo, DataStore> volumeDataStoreMap, VirtualMachineTO vmTO, Host srcHost, Host destHost,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;
        boolean success = false;
        boolean preparedForMigration = false;
        Map<VolumeInfo, VolumeInfo> srcVolumeInfoToDestVolumeInfo = new HashMap<>();
        try {
            if (srcHost.getHypervisorType() != HypervisorType.KVM) {
                throw new CloudRuntimeException(String.format("Invalid hypervisor type [%s]. Only KVM is supported", srcHost.getHypervisorType()));
            }

            verifyLiveMigrationPools(volumeDataStoreMap);

            VMInstanceVO vmInstance = _vmDao.findById(vmTO.getId());
            vmTO.setState(vmInstance.getState());
            List<MigrateDiskInfo> migrateDiskInfoList = new ArrayList<>();
            Map<String, MigrateDiskInfo> migrateStorage = new HashMap<>();

            for (Map.Entry<VolumeInfo, DataStore> entry : volumeDataStoreMap.entrySet()) {
                VolumeInfo srcVolumeInfo = entry.getKey();
                DataStore destDataStore = entry.getValue();

                VolumeVO srcVolume = _volumeDao.findById(srcVolumeInfo.getId());
                StoragePoolVO destStoragePool = _storagePoolDao.findById(destDataStore.getId());
                StoragePoolVO sourceStoragePool = _storagePoolDao.findById(srcVolumeInfo.getPoolId());
                if (sourceStoragePool.getId() == destStoragePool.getId()) {
                    continue;
                }
                boolean isNfsDestination = destStoragePool.getPoolType() == StoragePoolType.NetworkFilesystem;

                VolumeVO destVolume = duplicateVolumeOnAnotherStorage(srcVolume, destStoragePool);
                VolumeInfo destVolumeInfo = _volumeDataFactory.getVolume(destVolume.getId(), destDataStore);
                // The destination volume row already exists. Failure cleanup only expunges volumes
                // recorded here, so record it before grant, create, or connect can throw.
                srcVolumeInfoToDestVolumeInfo.put(srcVolumeInfo, destVolumeInfo);

                destVolumeInfo.processEvent(Event.MigrationCopyRequested);
                destVolumeInfo.processEvent(Event.MigrationCopySucceeded);
                destVolumeInfo.processEvent(Event.MigrationRequested);

                destDataStore.getDriver().createAsync(destDataStore, destVolumeInfo, null);

                destVolume = _volumeDao.findById(destVolume.getId());
                destVolume.setPath(isNfsDestination ? destVolumeInfo.getUuid() : destVolumeInfo.get_iScsiName());
                _volumeDao.update(destVolume.getId(), destVolume);

                destVolumeInfo = _volumeDataFactory.getVolume(destVolume.getId(), destDataStore);
                _volumeService.grantAccess(destVolumeInfo, destHost, destDataStore);
                destVolumeInfo = _volumeDataFactory.getVolume(destVolume.getId(), destDataStore);
                // grantAccess can change the path and iSCSI name. Disconnect and revoke on failure
                // must use refreshed volume.
                srcVolumeInfoToDestVolumeInfo.put(srcVolumeInfo, destVolumeInfo);

                String destPath = connectHostToVolume(destHost, destVolumeInfo.getPoolId(), getVolumeIdentifier(destVolumeInfo, destStoragePool));

                MigrateDiskInfo migrateDiskInfo;
                if (isNfsDestination) {
                    migrateDiskInfo = new MigrateDiskInfo(srcVolumeInfo.getPath(), MigrateDiskInfo.DiskType.FILE,
                            MigrateDiskInfo.DriverType.QCOW2, MigrateDiskInfo.Source.FILE, destPath);
                } else {
                    String sourcePath = srcVolumeInfo.getPath();
                    if (sourceStoragePool.isManaged()) {
                        if (sourceStoragePool.getPoolType() == StoragePoolType.OntapiSCSI) {
                            _volumeService.grantAccess(srcVolumeInfo, srcHost, srcVolumeInfo.getDataStore());
                            srcVolumeInfo = _volumeDataFactory.getVolume(srcVolumeInfo.getId(), srcVolumeInfo.getDataStore());
                        }
                        sourcePath = connectHostToVolume(srcHost, srcVolumeInfo.getPoolId(), srcVolumeInfo.get_iScsiName());
                    }
                    migrateDiskInfo = new MigrateDiskInfo(sourcePath, MigrateDiskInfo.DiskType.BLOCK,
                            MigrateDiskInfo.DriverType.RAW, MigrateDiskInfo.Source.DEV, destPath);
                    migrateDiskInfo.setSourceDiskOnStorageFileSystem(sourceStoragePool.getPoolType() == StoragePoolType.Filesystem);
                    migrateDiskInfoList.add(migrateDiskInfo);
                }
                migrateDiskInfo.setSourcePoolType(sourceStoragePool.getPoolType());
                migrateDiskInfo.setDestPoolType(destVolumeInfo.getStoragePoolType());
                setDestinationVolumeOnVmDisk(vmTO, srcVolumeInfo, destVolumeInfo);

                migrateStorage.put(srcVolumeInfo.getPath(), migrateDiskInfo);
                if (!srcVolumeInfo.getPath().equals(migrateDiskInfo.getSerialNumber())) {
                    migrateStorage.put(migrateDiskInfo.getSerialNumber(), migrateDiskInfo);
                }
            }

            PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(vmTO);
            Answer pfma;
            try {
                pfma = _agentMgr.send(destHost.getId(), pfmc);
                if (pfma == null || !pfma.getResult()) {
                    String details = pfma != null ? pfma.getDetails() : "null answer returned";
                    throw new AgentUnavailableException("Unable to prepare for migration due to the following: " + details, destHost.getId());
                }
            } catch (OperationTimedoutException e) {
                throw new AgentUnavailableException("Operation timed out", destHost.getId());
            }
            preparedForMigration = true;

            VMInstanceVO vm = _vmDao.findById(vmTO.getId());
            boolean isWindows = _guestOsCategoryDao.findById(_guestOsDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");

            MigrateCommand migrateCommand = new MigrateCommand(vmTO.getName(), destHost.getPrivateIpAddress(), isWindows, vmTO, true);
            migrateCommand.setWait(StorageManager.KvmStorageOnlineMigrationWait.value());
            migrateCommand.setMigrateStorage(migrateStorage);
            migrateCommand.setMigrateDiskInfoList(migrateDiskInfoList);
            migrateCommand.setMigrateStorageManaged(true);
            migrateCommand.setAutoConvergence(StorageManager.KvmAutoConvergence.value());
            // The destination host returns this count, and libvirt rejects 0.
            Integer newVmCpuShares = ((PrepareForMigrationAnswer)pfma).getNewVmCpuShares();
            if (newVmCpuShares != null) {
                migrateCommand.setNewVmCpuShares(newVmCpuShares);
            }

            // The migrate command can time out after libvirt has already started the VM on the
            // destination. If that host reports PowerOn, finish the migration. Treating the timeout
            // as failure would expunge disks that VM is using.
            MigrateAnswer migrateAnswer;
            try {
                migrateAnswer = (MigrateAnswer)_agentMgr.send(srcHost.getId(), migrateCommand);
            } catch (OperationTimedoutException ex) {
                if (!isVmRunningOnDestination(vm, destHost)) {
                    throw ex;
                }
                logger.info("VM [{}] is running on destination host [{}] after a migration timeout", vmTO.getId(), destHost.getId());
                migrateAnswer = new MigrateAnswer(migrateCommand, true, ex.getMessage(), null);
            }
            if (migrateAnswer == null) {
                throw new CloudRuntimeException("Unable to get an answer to the migrate command");
            }
            if (!migrateAnswer.getResult()) {
                throw new CloudRuntimeException(migrateAnswer.getDetails());
            }

            success = true;
            handlePostMigration(srcVolumeInfoToDestVolumeInfo, srcHost);
        } catch (AgentUnavailableException | OperationTimedoutException | CloudRuntimeException ex) {
            errMsg = String.format("Copy volume(s) of VM [%s] to storage(s) and VM to host [%s] failed in OntapDataMotionStrategy.copyAsync. Error message: [%s].",
                    vmTO.getId(), destHost.getId(), ex.getMessage());
            logger.error(errMsg, ex);
            throw new CloudRuntimeException(errMsg);
        } catch (Exception ex) {
            // Anything not caught above would leave errMsg null and the callback would report success.
            errMsg = String.format("Unexpected error while migrating volume(s) of VM [%s] from host [%s] to host [%s]: %s",
                    vmTO.getId(), srcHost.getId(), destHost.getId(), StringUtils.defaultIfBlank(ex.getMessage(), ex.getClass().getSimpleName()));
            logger.error("{}. Exception: [{}], VM already migrated: [{}], destination host prepared: [{}]",
                    errMsg, ex.getClass().getName(), success, preparedForMigration, ex);
            throw new CloudRuntimeException(errMsg, ex);
        } finally {
            if (!success) {
                handleFailedMigration(srcVolumeInfoToDestVolumeInfo, vmTO, destHost, preparedForMigration);
            }
            CopyCmdAnswer copyCmdAnswer = new CopyCmdAnswer(errMsg);
            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);
            result.setResult(errMsg);
            callback.complete(result);
        }
    }

    private void checkAvailableForMigration(VirtualMachine vm) {
        if (vm != null && vm.getState() != VirtualMachine.State.Stopped && vm.getState() != VirtualMachine.State.Migrating) {
            throw new CloudRuntimeException("A volume attached to a VM can only be migrated offline when the VM is in the Stopped or Migrating state.");
        }
    }

    private HostVO getHostToMigrateVolume(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo) {
        if (ScopeType.HOST.equals(srcVolumeInfo.getDataStore().getScope().getScopeType())) {
            return _hostDao.findById(srcVolumeInfo.getDataStore().getScope().getScopeId());
        }
        if (ScopeType.HOST.equals(destVolumeInfo.getDataStore().getScope().getScopeType())) {
            return _hostDao.findById(destVolumeInfo.getDataStore().getScope().getScopeId());
        }
        // A cluster-scoped source is copied by an enabled host in that cluster, because grantAccess
        // then lets that host reach the destination pool. A source with no cluster uses an enabled
        // host in the destination zone.
        StoragePoolVO srcStoragePoolVO = _storagePoolDao.findById(srcVolumeInfo.getPoolId());
        List<HostVO> hosts;
        if (srcStoragePoolVO.getClusterId() != null) {
            PrimaryDataStoreInfo srcStore = (PrimaryDataStoreInfo)_dataStoreMgr.getPrimaryDataStore(srcStoragePoolVO.getId());
            hosts = _resourceMgr.getEligibleUpAndEnabledHostsInClusterForStorageConnection(srcStore);
        } else {
            hosts = _resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(destVolumeInfo.getDataStore(),
                    destVolumeInfo.getDataCenterId(), HypervisorType.KVM);
        }
        if (hosts != null) {
            Collections.shuffle(hosts);
            for (HostVO host : hosts) {
                if (ResourceState.Enabled.equals(host.getResourceState())) {
                    return host;
                }
            }
        }
        throw new CloudRuntimeException("Unable to locate a host to migrate the volume on");
    }

    // Returns true when the destination host reports the VM as PowerOn. A migrate timeout in that
    // state means the VM is already using the destination disks, so the migration should complete.
    private boolean isVmRunningOnDestination(VMInstanceVO vm, Host destHost) {
        try {
            Answer answer = _agentMgr.send(destHost.getId(), new CheckVirtualMachineCommand(vm.getInstanceName()));
            return answer instanceof CheckVirtualMachineAnswer && answer.getResult()
                    && VirtualMachine.PowerState.PowerOn.equals(((CheckVirtualMachineAnswer)answer).getState());
        } catch (Exception e) {
            logger.warn("Could not check whether VM [{}] is running on destination host [{}]", vm.getId(), destHost.getId(), e);
            return false;
        }
    }

    private String sendVolumeMigrationCommand(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo, HostVO hostVO, boolean useCopyCommand) {
        try {
            // The ONTAP iSCSI driver maps the LUN on grant, so the IQN must be read after it.
            _volumeService.grantAccess(srcVolumeInfo, hostVO, srcVolumeInfo.getDataStore());
            Map<String, String> srcDetails = getVolumeDetails(srcVolumeInfo);
            Map<String, String> destDetails = getVolumeDetails(destVolumeInfo);
            _volumeService.grantAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());

            if (useCopyCommand) {
                CopyCommand copyCommand = new CopyCommand(srcVolumeInfo.getTO(), destVolumeInfo.getTO(),
                        StorageManager.KvmStorageOfflineMigrationWait.value(), VirtualMachineManager.ExecuteInSequence.value());
                copyCommand.setOptions(srcDetails);
                copyCommand.setOptions2(destDetails);
                Answer answer = _agentMgr.send(hostVO.getId(), copyCommand);
                checkVolumeMigrationAnswer(answer, "Unable to copy the volume between storage pools");
                return ((VolumeObjectTO)((CopyCmdAnswer)answer).getNewData()).getPath();
            }

            MigrateVolumeCommand migrateVolumeCommand = new MigrateVolumeCommand(srcVolumeInfo.getTO(), destVolumeInfo.getTO(),
                    srcDetails, destDetails, StorageManager.KvmStorageOfflineMigrationWait.value());
            Answer answer = _agentMgr.send(hostVO.getId(), migrateVolumeCommand);
            checkVolumeMigrationAnswer(answer, "Unable to migrate the volume between storage pools");
            return ((MigrateVolumeAnswer)answer).getVolumePath();
        } catch (CloudRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CloudRuntimeException("Unexpected error during volume migration: " + ex.getMessage(), ex);
        } finally {
            try {
                _volumeService.revokeAccess(srcVolumeInfo, hostVO, srcVolumeInfo.getDataStore());
                _volumeService.revokeAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());
            } catch (Throwable e) {
                logger.warn("Failed to revoke access after a volume migration attempt", e);
            }
        }
    }

    private void checkVolumeMigrationAnswer(Answer answer, String defaultErrMsg) {
        if (answer == null || !answer.getResult()) {
            if (answer != null && StringUtils.isNotEmpty(answer.getDetails())) {
                throw new CloudRuntimeException(answer.getDetails());
            }
            throw new CloudRuntimeException(defaultErrMsg);
        }
    }

    private Map<String, String> getVolumeDetails(VolumeInfo volumeInfo) {
        StoragePoolVO storagePoolVO = _storagePoolDao.findById(volumeInfo.getPoolId());
        if (!storagePoolVO.isManaged()) {
            return null;
        }

        VolumeVO volumeVO = _volumeDao.findById(volumeInfo.getId());
        Map<String, String> volumeDetails = new HashMap<>();
        volumeDetails.put(DiskTO.STORAGE_HOST, storagePoolVO.getHostAddress());
        volumeDetails.put(DiskTO.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));
        volumeDetails.put(DiskTO.IQN, volumeVO.get_iScsiName());
        volumeDetails.put(DiskTO.PROTOCOL_TYPE, (volumeVO.getPoolType() != null) ? volumeVO.getPoolType().toString() : null);
        volumeDetails.put(StorageManager.STORAGE_POOL_DISK_WAIT.toString(), String.valueOf(StorageManager.STORAGE_POOL_DISK_WAIT.valueIn(storagePoolVO.getId())));
        volumeDetails.put(DiskTO.VOLUME_SIZE, String.valueOf(volumeVO.getSize()));
        return volumeDetails;
    }

    private void completeVolumeMigrationCallback(VolumeInfo destVolumeInfo, AsyncCompletionCallback<CopyCommandResult> callback, String errMsg) {
        destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId(), destVolumeInfo.getDataStore());
        CopyCmdAnswer copyCmdAnswer;
        if (errMsg != null) {
            copyCmdAnswer = new CopyCmdAnswer(errMsg);
        } else {
            DataTO dataTO = destVolumeInfo.getTO();
            copyCmdAnswer = new CopyCmdAnswer(dataTO);
        }
        CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);
        result.setResult(errMsg);
        callback.complete(result);
    }

    /**
     * A managed source can only move to a compatible ONTAP pool, and the destination pools must be all managed or all not managed.
     */
    private void verifyLiveMigrationPools(Map<VolumeInfo, DataStore> volumeDataStoreMap) {
        Boolean storageTypeConsistency = null;
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeDataStoreMap.entrySet()) {
            VolumeInfo volumeInfo = entry.getKey();
            StoragePoolVO srcStoragePoolVO = _storagePoolDao.findById(volumeInfo.getPoolId());
            if (srcStoragePoolVO == null) {
                throw new CloudRuntimeException("Volume with ID " + volumeInfo.getId() + " is not associated with a storage pool.");
            }
            StoragePoolVO destStoragePoolVO = _storagePoolDao.findById(entry.getValue().getId());
            if (destStoragePoolVO == null) {
                throw new CloudRuntimeException("Destination storage pool with ID " + entry.getValue().getId() + " was not located.");
            }
            if (srcStoragePoolVO.isManaged() && srcStoragePoolVO.getId() != destStoragePoolVO.getId()
                    && !isSupportedOntapMigrationPoolPair(srcStoragePoolVO, destStoragePoolVO)) {
                if (isOntapPool(srcStoragePoolVO) && isOntapPool(destStoragePoolVO)) {
                    Map<String, String> srcDetails = _storagePoolDao.getDetails(srcStoragePoolVO.getId());
                    Map<String, String> destDetails = _storagePoolDao.getDetails(destStoragePoolVO.getId());
                    throw new CloudRuntimeException(String.format("ONTAP migration requires matching storageIP, SVM, protocol, and pool type "
                                    + "[source: storageIP=%s, svmUUID=%s, svmName=%s, protocol=%s, type=%s; "
                                    + "destination: storageIP=%s, svmUUID=%s, svmName=%s, protocol=%s, type=%s].",
                            srcDetails != null ? srcDetails.get(ONTAP_STORAGE_IP_DETAIL) : null,
                            srcDetails != null ? srcDetails.get(ONTAP_SVM_UUID_DETAIL) : null,
                            srcDetails != null ? srcDetails.get(ONTAP_SVM_NAME_DETAIL) : null, srcDetails != null ? srcDetails.get(ONTAP_PROTOCOL_DETAIL) : null,
                            srcStoragePoolVO.getPoolType(), destDetails != null ? destDetails.get(ONTAP_STORAGE_IP_DETAIL) : null,
                            destDetails != null ? destDetails.get(ONTAP_SVM_UUID_DETAIL) : null,
                            destDetails != null ? destDetails.get(ONTAP_SVM_NAME_DETAIL) : null,
                            destDetails != null ? destDetails.get(ONTAP_PROTOCOL_DETAIL) : null, destStoragePoolVO.getPoolType()));
                }
                throw new CloudRuntimeException("Migrating a volume online with KVM from managed storage is not currently supported.");
            }
            if (storageTypeConsistency == null) {
                storageTypeConsistency = destStoragePoolVO.isManaged();
            } else if (storageTypeConsistency != destStoragePoolVO.isManaged()) {
                throw new CloudRuntimeException("Destination storage pools must be either all managed or all not managed");
            }
        }
    }

    private ImageFormat getImageFormatForPoolType(StoragePoolType poolType) {
        return poolType == StoragePoolType.OntapiSCSI ? ImageFormat.RAW : ImageFormat.QCOW2;
    }

    private VolumeVO duplicateVolumeOnAnotherStorage(Volume volume, StoragePoolVO storagePoolVO) {
        VolumeVO newVol = new VolumeVO(volume);
        newVol.setInstanceId(null);
        newVol.setChainInfo(null);
        newVol.setPath(null);
        newVol.set_iScsiName(null);
        newVol.setFolder(null);
        newVol.setPodId(storagePoolVO.getPodId());
        newVol.setPoolId(storagePoolVO.getId());
        newVol.setPoolType(storagePoolVO.getPoolType());
        // The row is copied from the source, so it carries the source format. The destination
        // object is created from this row, so it must carry the destination pool's format.
        newVol.setFormat(getImageFormatForPoolType(storagePoolVO.getPoolType()));
        newVol.setLastPoolId(volume.getPoolId());
        newVol.setLastId(volume.getId());
        return _volumeDao.persist(newVol);
    }

    private void setDestinationVolumeOnVmDisk(VirtualMachineTO vmTO, VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo) {
        if (vmTO.getDisks() == null) {
            return;
        }
        Arrays.stream(vmTO.getDisks()).filter(diskTO -> diskTO.getData() != null && diskTO.getData().getId() == srcVolumeInfo.getId())
                .forEach(diskTO -> diskTO.setData(destVolumeInfo.getTO()));
    }

    private void handlePostMigration(Map<VolumeInfo, VolumeInfo> srcVolumeInfoToDestVolumeInfo, Host srcHost) {
        for (Map.Entry<VolumeInfo, VolumeInfo> entry : srcVolumeInfoToDestVolumeInfo.entrySet()) {
            VolumeInfo srcVolumeInfo = entry.getKey();
            VolumeInfo destVolumeInfo = entry.getValue();

            StoragePoolVO srcPoolVO = _storagePoolDao.findById(srcVolumeInfo.getPoolId());
            StoragePoolVO destPoolVO = _storagePoolDao.findById(destVolumeInfo.getPoolId());
            VolumeVO volumeVO = _volumeDao.findById(destVolumeInfo.getId());
            volumeVO.setFormat(getImageFormatForPoolType(destPoolVO != null ? destPoolVO.getPoolType() : null));
            volumeVO.setLastId(srcVolumeInfo.getId());
            _volumeDao.update(volumeVO.getId(), volumeVO);

            // Live migrate rewrites the NFS qcow2 and clears file QoS; re-attach before source cleanup.
            reapplyOntapQosAfterContentRewrite(destVolumeInfo);

            if (srcPoolVO != null && srcPoolVO.getPoolType() == StoragePoolType.OntapiSCSI) {
                disconnectVolumeFromHost(srcHost, srcVolumeInfo);
            }
            if (destPoolVO != null && destPoolVO.getPoolType() == StoragePoolType.OntapiSCSI) {
                disconnectVolumeFromHost(srcHost, destVolumeInfo);
            }

            _volumeService.copyPoliciesBetweenVolumesAndDestroySourceVolumeAfterMigration(Event.OperationSucceeded, null, srcVolumeInfo, destVolumeInfo, false);
        }
    }

    /**
     * NFS createAsync attaches QoS, then qemu-img / libvirt live migrate overwrites the file and
     * drops the policy. Re-apply from the destination volume's stored IOPS. iSCSI LUN QoS survives
     * block copy; same-SVM offline LUN copy attaches in the driver instead.
     */
    private void reapplyOntapQosAfterContentRewrite(VolumeInfo destVolumeInfo) {
        if (destVolumeInfo == null || destVolumeInfo.getDataStore() == null) {
            return;
        }
        StoragePoolVO destPool = _storagePoolDao.findById(destVolumeInfo.getPoolId());
        if (destPool == null || destPool.getPoolType() != StoragePoolType.NetworkFilesystem
                || !DataStoreProvider.ONTAP_PLUGIN_NAME.equals(destPool.getStorageProviderName())) {
            return;
        }
        if (!(destVolumeInfo.getDataStore().getDriver() instanceof OntapPrimaryDatastoreDriver)) {
            return;
        }
        VolumeInfo refreshed = _volumeDataFactory.getVolume(destVolumeInfo.getId(), destVolumeInfo.getDataStore());
        OntapPrimaryDatastoreDriver driver = (OntapPrimaryDatastoreDriver)destVolumeInfo.getDataStore().getDriver();
        // The VM is already on the destination. A failed patch must not expunge those disks.
        for (int attempt = 1; attempt <= QOS_PATCH_ATTEMPTS; attempt++) {
            try {
                driver.applyQosPolicyToVolume(refreshed);
                return;
            } catch (Exception e) {
                if (attempt == QOS_PATCH_ATTEMPTS) {
                    logger.warn("Failed to reapply ONTAP QoS on volume [{}] after {} attempts",
                            destVolumeInfo.getId(), QOS_PATCH_ATTEMPTS, e);
                    return;
                }
                logger.warn("Failed to reapply ONTAP QoS on volume [{}], attempt {} of {}",
                        destVolumeInfo.getId(), attempt, QOS_PATCH_ATTEMPTS, e);
                try {
                    Thread.sleep(QOS_PATCH_RETRY_WAIT_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while retrying ONTAP QoS on volume [{}]", destVolumeInfo.getId(), interrupted);
                    return;
                }
            }
        }
    }

    private void handleFailedMigration(Map<VolumeInfo, VolumeInfo> srcVolumeInfoToDestVolumeInfo, VirtualMachineTO vmTO, Host destHost,
            boolean preparedForMigration) {
        if (preparedForMigration) {
            try {
                PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(vmTO);
                pfmc.setRollback(true);
                Answer pfma = _agentMgr.send(destHost.getId(), pfmc);
                if (pfma == null || !pfma.getResult()) {
                    logger.warn("Unable to roll back prepare for migration of VM [{}] on host [{}]", vmTO.getId(), destHost.getId());
                }
            } catch (Exception e) {
                logger.warn("Failed to roll back prepare for migration of VM [{}] on host [{}]", vmTO.getId(), destHost.getId(), e);
            }
        }

        for (VolumeInfo destVolumeInfo : srcVolumeInfoToDestVolumeInfo.values()) {
            disconnectVolumeFromHost(destHost, destVolumeInfo);
            try {
                _volumeService.revokeAccess(destVolumeInfo, destHost, destVolumeInfo.getDataStore());
            } catch (Exception e) {
                logger.debug("Failed to revoke access from destination volume [{}]", destVolumeInfo.getId(), e);
            }
            logger.info("Expunging destination volume [{}] after a failed migration of VM [{}]", destVolumeInfo.getId(), vmTO.getId());
            destVolumeInfo.processEvent(Event.OperationFailed);
            destVolumeInfo.processEvent(Event.DestroyRequested);
            _volumeService.expungeVolumeAsync(destVolumeInfo);
        }
    }

    private String getVolumeIdentifier(VolumeInfo volumeInfo, StoragePoolVO storagePoolVO) {
        if (storagePoolVO != null && storagePoolVO.getPoolType() == StoragePoolType.NetworkFilesystem) {
            return volumeInfo.getUuid();
        }
        return volumeInfo.get_iScsiName();
    }

    private String connectHostToVolume(Host host, long storagePoolId, String iqn) {
        return sendModifyTargetsCommand(getModifyTargetsCommand(storagePoolId, iqn, true), host).get(0);
    }

    private void disconnectVolumeFromHost(Host host, VolumeInfo volumeInfo) {
        try {
            StoragePoolVO pool = _storagePoolDao.findById(volumeInfo.getPoolId());
            disconnectHostFromVolume(host, volumeInfo.getPoolId(), getVolumeIdentifier(volumeInfo, pool));
        } catch (Exception e) {
            logger.warn("Failed to disconnect volume [{}] from host [{}]", volumeInfo.getId(), host.getId(), e);
        }
    }

    private void disconnectHostFromVolume(Host host, long storagePoolId, String iqn) {
        sendModifyTargetsCommand(getModifyTargetsCommand(storagePoolId, iqn, false), host);
    }

    private ModifyTargetsCommand getModifyTargetsCommand(long storagePoolId, String iqn, boolean add) {
        StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

        Map<String, String> details = new HashMap<>();
        details.put(ModifyTargetsCommand.IQN, iqn);
        details.put(ModifyTargetsCommand.STORAGE_TYPE, storagePool.getPoolType().name());
        details.put(ModifyTargetsCommand.STORAGE_UUID, storagePool.getUuid());
        details.put(ModifyTargetsCommand.STORAGE_HOST, storagePool.getHostAddress());
        details.put(ModifyTargetsCommand.STORAGE_PORT, String.valueOf(storagePool.getPort()));

        List<Map<String, String>> targets = new ArrayList<>();
        targets.add(details);

        ModifyTargetsCommand cmd = new ModifyTargetsCommand();
        cmd.setTargets(targets);
        cmd.setApplyToAllHostsInCluster(true);
        cmd.setAdd(add);
        cmd.setTargetTypeToRemove(ModifyTargetsCommand.TargetTypeToRemove.DYNAMIC);
        return cmd;
    }

    private List<String> sendModifyTargetsCommand(ModifyTargetsCommand cmd, Host host) {
        Answer answer = _agentMgr.easySend(host.getId(), cmd);
        if (answer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify targets command");
        }
        if (!answer.getResult()) {
            throw new CloudRuntimeException(String.format("Unable to modify targets on host [%s]: %s", host.getId(), answer.getDetails()));
        }
        if (!(answer instanceof ModifyTargetsAnswer)) {
            throw new CloudRuntimeException(String.format("Unexpected answer type [%s] while modifying targets on host [%s]",
                    answer.getClass().getSimpleName(), host.getId()));
        }
        return ((ModifyTargetsAnswer)answer).getConnectedPaths();
    }

    private boolean isSameSvmNfsPair(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo) {
        StoragePoolVO srcStoragePoolVO = _storagePoolDao.findById(srcVolumeInfo.getPoolId());
        StoragePoolVO destStoragePoolVO = _storagePoolDao.findById(destVolumeInfo.getPoolId());
        return srcStoragePoolVO != null && destStoragePoolVO != null
                && srcStoragePoolVO.getPoolType() == StoragePoolType.NetworkFilesystem
                && isSupportedOntapMigrationPoolPair(srcStoragePoolVO, destStoragePoolVO);
    }

    private boolean isSupportedOntapMigrationPoolPair(StoragePoolVO srcStoragePoolVO, StoragePoolVO destStoragePoolVO) {
        if (!isOntapPool(srcStoragePoolVO) || !isOntapPool(destStoragePoolVO)) {
            return false;
        }

        Map<String, String> srcDetails = _storagePoolDao.getDetails(srcStoragePoolVO.getId());
        Map<String, String> destDetails = _storagePoolDao.getDetails(destStoragePoolVO.getId());
        if (srcDetails == null || destDetails == null) {
            return false;
        }

        String srcSvmName = srcDetails.get(ONTAP_SVM_NAME_DETAIL);
        String destSvmName = destDetails.get(ONTAP_SVM_NAME_DETAIL);
        String srcSvmUuid = srcDetails.get(ONTAP_SVM_UUID_DETAIL);
        String destSvmUuid = destDetails.get(ONTAP_SVM_UUID_DETAIL);
        String srcStorageIp = srcDetails.get(ONTAP_STORAGE_IP_DETAIL);
        String destStorageIp = destDetails.get(ONTAP_STORAGE_IP_DETAIL);
        String srcProtocol = srcDetails.get(ONTAP_PROTOCOL_DETAIL);
        String destProtocol = destDetails.get(ONTAP_PROTOCOL_DETAIL);
        boolean isSameSvm = StringUtils.isNotBlank(srcSvmUuid) || StringUtils.isNotBlank(destSvmUuid)
                ? StringUtils.isNotBlank(srcSvmUuid) && srcSvmUuid.equals(destSvmUuid)
                : StringUtils.isNotBlank(srcSvmName) && srcSvmName.equals(destSvmName);
        return StringUtils.isNotBlank(srcStorageIp)
                && srcStorageIp.equals(destStorageIp)
                && isSameSvm
                && StringUtils.isNotBlank(srcProtocol)
                && srcProtocol.equalsIgnoreCase(destProtocol)
                && srcStoragePoolVO.getPoolType() != null
                && srcStoragePoolVO.getPoolType() == destStoragePoolVO.getPoolType();
    }

    private void checkUnsupportedOntapCrossProtocolMigration(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo) {
        StoragePoolVO srcStoragePoolVO = _storagePoolDao.findById(srcVolumeInfo.getPoolId());
        StoragePoolVO destStoragePoolVO = _storagePoolDao.findById(destVolumeInfo.getPoolId());
        if (!isOntapPool(srcStoragePoolVO) || !isOntapPool(destStoragePoolVO)) {
            return;
        }

        Map<String, String> srcDetails = _storagePoolDao.getDetails(srcStoragePoolVO.getId());
        Map<String, String> destDetails = _storagePoolDao.getDetails(destStoragePoolVO.getId());
        String srcProtocol = srcDetails != null ? srcDetails.get(ONTAP_PROTOCOL_DETAIL) : null;
        String destProtocol = destDetails != null ? destDetails.get(ONTAP_PROTOCOL_DETAIL) : null;
        if (StringUtils.isNotBlank(srcProtocol) && StringUtils.isNotBlank(destProtocol)
                && !srcProtocol.equalsIgnoreCase(destProtocol)) {
            throw new CloudRuntimeException("Cross-protocol migration between ONTAP storage pools is not supported.");
        }
    }

    private boolean isOntapPool(StoragePoolVO storagePoolVO) {
        return storagePoolVO != null && DataStoreProvider.ONTAP_PLUGIN_NAME.equals(storagePoolVO.getStorageProviderName());
    }

    private boolean volumeMapIncludesOntapPool(Map<VolumeInfo, DataStore> volumeMap) {
        for (VolumeInfo volumeInfo : volumeMap.keySet()) {
            if (isOntapPool(_storagePoolDao.findById(volumeInfo.getPoolId()))) {
                return true;
            }
        }
        for (DataStore dataStore : volumeMap.values()) {
            if (isOntapPool(_storagePoolDao.findById(dataStore.getId()))) {
                return true;
            }
        }
        return false;
    }
}
