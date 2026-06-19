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

import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceDefinitionModify;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;


/**
 * Current state:
 *   just changing the resource-group on same storage pool resource-group is not really good enough.
 *   Linstor lacks currently of a good way to move resources to another resource-group and respecting
 *   every auto-filter setting.
 *   Also linstor clone would simply set the new resource-group without any adjustments of storage pools or
 *   auto-select resource placement.
 *   So currently, we will create a new resource in the wanted primary storage and let qemu copy the data into the
 *   devices.
 */

@Component
public class LinstorDataMotionStrategy implements DataMotionStrategy {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private SnapshotDataStoreDao _snapshotStoreDao;
    @Inject
    private PrimaryDataStoreDao _storagePool;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private VolumeDataFactory _volumeDataFactory;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private GuestOSDao _guestOsDao;
    @Inject
    private VolumeService _volumeService;
    @Inject
    private GuestOSCategoryDao _guestOsCategoryDao;
    @Inject
    private SnapshotDao _snapshotDao;
    @Inject
    private AgentManager _agentManager;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;

    @Override
    public StrategyPriority canHandle(DataObject srcData, DataObject dstData) {
        DataObjectType srcType = srcData.getType();
        DataObjectType dstType = dstData.getType();
        logger.debug("canHandle: {} -> {}", srcType, dstType);
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        throw new CloudRuntimeException("not implemented");
    }

    private boolean isDestinationLinstorPrimaryStorage(Map<VolumeInfo, DataStore> volumeMap) {
        if (MapUtils.isNotEmpty(volumeMap)) {
            for (DataStore dataStore : volumeMap.values()) {
                StoragePoolVO storagePoolVO = _storagePool.findById(dataStore.getId());
                if (storagePoolVO == null
                        || !storagePoolVO.getStorageProviderName().equals(LinstorUtil.PROVIDER_NAME)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        logger.debug("canHandle -- {}: {} -> {}", volumeMap, srcHost, destHost);
        if (srcHost.getId() != destHost.getId() && isDestinationLinstorPrimaryStorage(volumeMap)) {
            return StrategyPriority.HIGHEST;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    private VolumeVO createNewVolumeVO(Volume volume, StoragePoolVO storagePoolVO) {
        VolumeVO newVol = new VolumeVO(volume);
        newVol.setInstanceId(null);
        newVol.setChainInfo(null);
        newVol.setPath(newVol.getUuid());
        newVol.setFolder(null);
        newVol.setPodId(storagePoolVO.getPodId());
        newVol.setPoolId(storagePoolVO.getId());
        newVol.setLastPoolId(volume.getPoolId());

        return _volumeDao.persist(newVol);
    }

    private void removeExactSizeProperty(VolumeInfo volumeInfo) {
        StoragePoolVO destStoragePool = _storagePool.findById(volumeInfo.getDataStore().getId());
        DevelopersApi api = LinstorUtil.getLinstorAPI(destStoragePool.getHostAddress());

        ResourceDefinitionModify rdm = new ResourceDefinitionModify();
        rdm.setDeleteProps(Collections.singletonList(LinstorUtil.LIN_PROP_DRBDOPT_EXACT_SIZE));
        try {
            String rscName = LinstorUtil.RSC_PREFIX + volumeInfo.getPath();
            ApiCallRcList answers = api.resourceDefinitionModify(rscName, rdm);
            LinstorUtil.checkLinstorAnswersThrow(answers);
        } catch (ApiException apiEx) {
            logger.error("Linstor: ApiEx - {}", apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    private void handlePostMigration(boolean success, Map<VolumeInfo, VolumeInfo> srcVolumeInfoToDestVolumeInfo,
                                     VirtualMachineTO vmTO, Host destHost) {
        if (!success) {
            try {
                PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(vmTO);

                pfmc.setRollback(true);

                Answer pfma = _agentManager.send(destHost.getId(), pfmc);

                if (pfma == null || !pfma.getResult()) {
                    String details = pfma != null ? pfma.getDetails() : "null answer returned";
                    String msg = "Unable to rollback prepare for migration due to the following: " + details;

                    throw new AgentUnavailableException(msg, destHost.getId());
                }
            } catch (Exception e) {
                logger.debug("Failed to disconnect one or more (original) dest volumes", e);
            }
        }

        for (Map.Entry<VolumeInfo, VolumeInfo> entry : srcVolumeInfoToDestVolumeInfo.entrySet()) {
            VolumeInfo srcVolumeInfo = entry.getKey();
            VolumeInfo destVolumeInfo = entry.getValue();

            if (success) {
                srcVolumeInfo.processEvent(ObjectInDataStoreStateMachine.Event.OperationSucceeded);
                destVolumeInfo.processEvent(ObjectInDataStoreStateMachine.Event.OperationSucceeded);

                _volumeDao.updateUuid(srcVolumeInfo.getId(), destVolumeInfo.getId());

                VolumeVO volumeVO = _volumeDao.findById(destVolumeInfo.getId());

                volumeVO.setFormat(Storage.ImageFormat.QCOW2);

                _volumeDao.update(volumeVO.getId(), volumeVO);

                // remove exact size property
                removeExactSizeProperty(destVolumeInfo);

                try {
                    _volumeService.destroyVolume(srcVolumeInfo.getId());

                    srcVolumeInfo = _volumeDataFactory.getVolume(srcVolumeInfo.getId());

                    AsyncCallFuture<VolumeService.VolumeApiResult> destroyFuture =
                            _volumeService.expungeVolumeAsync(srcVolumeInfo);

                    if (destroyFuture.get().isFailed()) {
                        logger.debug("Failed to clean up source volume on storage");
                    }
                } catch (Exception e) {
                    logger.debug("Failed to clean up source volume on storage", e);
                }

                // Update the volume ID for snapshots on secondary storage
                if (!_snapshotDao.listByVolumeId(srcVolumeInfo.getId()).isEmpty()) {
                    _snapshotDao.updateVolumeIds(srcVolumeInfo.getId(), destVolumeInfo.getId());
                    _snapshotStoreDao.updateVolumeIds(srcVolumeInfo.getId(), destVolumeInfo.getId());
                }
            } else {
                try {
                    _volumeService.revokeAccess(destVolumeInfo, destHost, destVolumeInfo.getDataStore());
                } catch (Exception e) {
                    logger.debug("Failed to revoke access from dest volume", e);
                }

                destVolumeInfo.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);
                srcVolumeInfo.processEvent(ObjectInDataStoreStateMachine.Event.OperationFailed);

                try {
                    _volumeService.destroyVolume(destVolumeInfo.getId());

                    destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId());

                    AsyncCallFuture<VolumeService.VolumeApiResult> destroyFuture =
                            _volumeService.expungeVolumeAsync(destVolumeInfo);

                    if (destroyFuture.get().isFailed()) {
                        logger.debug("Failed to clean up dest volume on storage");
                    }
                } catch (Exception e) {
                    logger.debug("Failed to clean up dest volume on storage", e);
                }
            }
        }
    }

    /**
     * Determines whether the destination volume should have the DRBD exact-size property set
     * during migration.
     *
     * <p>This method queries the Linstor API to check if the source volume's resource definition
     * has the exact-size DRBD option enabled. The exact-size property ensures that DRBD uses
     * the precise volume size rather than rounding, which is important for maintaining size
     * consistency during migrations.</p>
     *
     * @param srcVolumeInfo the source volume information to check
     * @return {@code true} if the exact-size property should be set on the destination volume,
     *         which occurs when the source volume has this property enabled, or when the
     *         property cannot be determined (defaults to {@code true} for safety);
     *         {@code false} only when the source is confirmed to not have the exact-size property
     */
    private boolean needsExactSizeProp(VolumeInfo srcVolumeInfo) {
        StoragePoolVO srcStoragePool = _storagePool.findById(srcVolumeInfo.getDataStore().getId());
        if (srcStoragePool.getPoolType() == Storage.StoragePoolType.Linstor) {
            DevelopersApi api = LinstorUtil.getLinstorAPI(srcStoragePool.getHostAddress());

            String rscName = LinstorUtil.RSC_PREFIX + srcVolumeInfo.getPath();
            try {
                List<ResourceDefinition> rscDfns = api.resourceDefinitionList(
                        Collections.singletonList(rscName),
                        false,
                        Collections.emptyList(),
                        null,
                        null);
                if (!CollectionUtils.isEmpty(rscDfns)) {
                    ResourceDefinition srcRsc = rscDfns.get(0);
                    String exactSizeProp = srcRsc.getProps().get(LinstorUtil.LIN_PROP_DRBDOPT_EXACT_SIZE);
                    return "true".equalsIgnoreCase(exactSizeProp);
                } else {
                    logger.warn("Unknown resource {} on {}", rscName, srcStoragePool.getHostAddress());
                }
            } catch (ApiException apiEx) {
                logger.error("Unable to fetch resource definition {}: {}", rscName, apiEx.getBestMessage());
            }
        }
        return true;
    }

    /**
     * Verify that the destination KVM host is a registered LINSTOR satellite on the controller
     * backing every destination pool involved in this migration. Throws CloudRuntimeException
     * with a clear message when it isn't, instead of letting the resource creation later fail
     * obscurely inside auto-placement.
     *
     * Best-effort: a transient controller error during this check does not block the migration
     * — we log a warning and let the downstream resource-create surface the real issue. Only a
     * confirmed "host not in node list" outcome aborts the migration up-front.
     */
    private void verifyDestinationIsLinstorSatellite(Map<VolumeInfo, DataStore> volumeDataStoreMap, Host destHost) {
        if (destHost == null || destHost.getName() == null) {
            // Without a destination host name to match, the only sensible thing is to let the
            // existing flow run and report whatever it would have reported.
            return;
        }
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeDataStoreMap.entrySet()) {
            DataStore destDataStore = entry.getValue();
            StoragePoolVO destStoragePool = _storagePool.findById(destDataStore.getId());
            if (destStoragePool == null
                    || destStoragePool.getPoolType() != Storage.StoragePoolType.Linstor) {
                continue;
            }
            DevelopersApi api = LinstorUtil.getLinstorAPI(destStoragePool.getHostAddress());
            try {
                List<String> nodes = LinstorUtil.getLinstorNodeNames(api);
                if (nodes == null) {
                    logger.warn("LINSTOR controller {} returned null node list; skipping pre-flight",
                            destStoragePool.getHostAddress());
                    return;
                }
                if (!nodes.contains(destHost.getName())) {
                    throw new CloudRuntimeException(String.format(
                            "Cannot migrate to host '%s': it is not a registered LINSTOR satellite on " +
                                    "controller %s (pool '%s'). Known satellites: %s. Either register the " +
                                    "host with `linstor node create` or pick a different destination.",
                            destHost.getName(),
                            destStoragePool.getHostAddress(),
                            destStoragePool.getName(),
                            nodes));
                }
            } catch (ApiException apiEx) {
                // Don't block migration on a transient controller hiccup — log and let the
                // downstream resource creation handle the real failure.
                logger.warn("LINSTOR pre-flight check could not contact controller {}: {}; " +
                                "letting downstream resource creation proceed",
                        destStoragePool.getHostAddress(), apiEx.getBestMessage());
                return;
            }
        }
    }

    @Override
    public void copyAsync(Map<VolumeInfo, DataStore> volumeDataStoreMap, VirtualMachineTO vmTO, Host srcHost,
            Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {

        if (srcHost.getHypervisorType() != Hypervisor.HypervisorType.KVM) {
            throw new CloudRuntimeException(
                    String.format("Invalid hypervisor type [%s]. Only KVM supported", srcHost.getHypervisorType()));
        }

        // Pre-flight: verify the destination KVM host is registered as a satellite on the
        // LINSTOR controller backing each destination pool. Without this check, resource
        // creation falls through to the resource-group's auto-placement filters and may
        // either silently place the resource on the wrong node or fail with an opaque
        // auto-place error from the LINSTOR API. Failing fast here gives operators a clear
        // actionable message instead of having to correlate the live-migration failure with
        // an unrelated LINSTOR controller log entry.
        verifyDestinationIsLinstorSatellite(volumeDataStoreMap, destHost);

        String errMsg = null;
        VMInstanceVO vmInstance = _vmDao.findById(vmTO.getId());
        vmTO.setState(vmInstance.getState());
        List<MigrateCommand.MigrateDiskInfo> migrateDiskInfoList = new ArrayList<>();

        Map<String, MigrateCommand.MigrateDiskInfo> migrateStorage = new HashMap<>();
        Map<VolumeInfo, VolumeInfo> srcVolumeInfoToDestVolumeInfo = new HashMap<>();

        try {
            for (Map.Entry<VolumeInfo, DataStore> entry : volumeDataStoreMap.entrySet()) {
                VolumeInfo srcVolumeInfo = entry.getKey();
                DataStore destDataStore = entry.getValue();
                VolumeVO srcVolume = _volumeDao.findById(srcVolumeInfo.getId());
                StoragePoolVO destStoragePool = _storagePool.findById(destDataStore.getId());

                if (srcVolumeInfo.getPassphraseId() != null) {
                    throw new CloudRuntimeException(
                            String.format("Cannot live migrate encrypted volume: %s", srcVolumeInfo.getVolume()));
                }

                VolumeVO destVolume = createNewVolumeVO(srcVolume, destStoragePool);

                VolumeInfo destVolumeInfo = _volumeDataFactory.getVolume(destVolume.getId(), destDataStore);

                destVolumeInfo.processEvent(ObjectInDataStoreStateMachine.Event.MigrationCopyRequested);
                destVolumeInfo.processEvent(ObjectInDataStoreStateMachine.Event.MigrationCopySucceeded);
                destVolumeInfo.processEvent(ObjectInDataStoreStateMachine.Event.MigrationRequested);

                boolean exactSize = needsExactSizeProp(srcVolumeInfo);

                String devPath = LinstorUtil.createResource(
                        destVolumeInfo, destStoragePool, _storagePoolDao, exactSize);

                _volumeDao.update(destVolume.getId(), destVolume);
                destVolume = _volumeDao.findById(destVolume.getId());

                destVolumeInfo = _volumeDataFactory.getVolume(destVolume.getId(), destDataStore);

                MigrateCommand.MigrateDiskInfo migrateDiskInfo = new MigrateCommand.MigrateDiskInfo(
                        srcVolumeInfo.getPath(),
                        MigrateCommand.MigrateDiskInfo.DiskType.BLOCK,
                        MigrateCommand.MigrateDiskInfo.DriverType.RAW,
                        MigrateCommand.MigrateDiskInfo.Source.DEV,
                        devPath);
                migrateDiskInfoList.add(migrateDiskInfo);

                migrateStorage.put(srcVolumeInfo.getPath(), migrateDiskInfo);

                srcVolumeInfoToDestVolumeInfo.put(srcVolumeInfo, destVolumeInfo);
            }

            PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(vmTO);
            try {
                Answer pfma = _agentManager.send(destHost.getId(), pfmc);

                if (pfma == null || !pfma.getResult()) {
                    String details = pfma != null ? pfma.getDetails() : "null answer returned";
                    errMsg = String.format("Unable to prepare for migration due to the following: %s", details);

                    throw new AgentUnavailableException(errMsg, destHost.getId());
                }
            } catch (final OperationTimedoutException e) {
                errMsg = String.format("Operation timed out due to %s", e.getMessage());
                throw new AgentUnavailableException(errMsg, destHost.getId());
            }

            VMInstanceVO vm = _vmDao.findById(vmTO.getId());
            boolean isWindows = _guestOsCategoryDao.findById(_guestOsDao.findById(vm.getGuestOSId()).getCategoryId())
                    .getName().equalsIgnoreCase("Windows");

            MigrateCommand migrateCommand = new MigrateCommand(vmTO.getName(),
                    destHost.getPrivateIpAddress(), isWindows, vmTO, true);
            migrateCommand.setWait(StorageManager.KvmStorageOnlineMigrationWait.value());
            migrateCommand.setMigrateStorage(migrateStorage);
            migrateCommand.setMigrateStorageManaged(true);
            migrateCommand.setNewVmCpuShares(
                    vmTO.getCpus() * ObjectUtils.defaultIfNull(vmTO.getMinSpeed(), vmTO.getSpeed()));
            migrateCommand.setMigrateDiskInfoList(migrateDiskInfoList);

            boolean kvmAutoConvergence = StorageManager.KvmAutoConvergence.value();
            migrateCommand.setAutoConvergence(kvmAutoConvergence);

            MigrateAnswer migrateAnswer = (MigrateAnswer) _agentManager.send(srcHost.getId(), migrateCommand);
            boolean success = migrateAnswer != null && migrateAnswer.getResult();

            handlePostMigration(success, srcVolumeInfoToDestVolumeInfo, vmTO, destHost);

            if (migrateAnswer == null) {
                throw new CloudRuntimeException("Unable to get an answer to the migrate command");
            }

            if (!migrateAnswer.getResult()) {
                errMsg = migrateAnswer.getDetails();

                throw new CloudRuntimeException(errMsg);
            }
        } catch (AgentUnavailableException | OperationTimedoutException | CloudRuntimeException ex) {
            errMsg = String.format(
                    "Copy volume(s) of VM [%s] to storage(s) [%s] and VM to host [%s] failed in LinstorDataMotionStrategy.copyAsync. Error message: [%s].",
                    vmTO, srcHost, destHost, ex.getMessage());
            logger.error(errMsg, ex);

            throw new CloudRuntimeException(errMsg);
        } finally {
            CopyCmdAnswer copyCmdAnswer = new CopyCmdAnswer(errMsg);

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);
            result.setResult(errMsg);
            callback.complete(result);
        }
    }
}
