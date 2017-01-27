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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.configuration.Config;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.server.ManagementService;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineManager;
import com.google.common.base.Preconditions;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.ResignatureAnswer;
import org.apache.cloudstack.storage.command.ResignatureCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class StorageSystemDataMotionStrategy implements DataMotionStrategy {
    private static final Logger LOGGER = Logger.getLogger(StorageSystemDataMotionStrategy.class);
    private static final Random RANDOM = new Random(System.nanoTime());

    @Inject private AgentManager _agentMgr;
    @Inject private ConfigurationDao _configDao;
    @Inject private DataStoreManager dataStoreMgr;
    @Inject private DiskOfferingDao _diskOfferingDao;
    @Inject private ClusterDao clusterDao;
    @Inject private HostDao _hostDao;
    @Inject private HostDetailsDao hostDetailsDao;
    @Inject private ManagementService _mgr;
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private SnapshotDao _snapshotDao;
    @Inject private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject private VolumeDao _volumeDao;
    @Inject private VolumeDataFactory _volumeDataFactory;
    @Inject private VolumeDetailsDao volumeDetailsDao;
    @Inject private VolumeService _volumeService;
    @Inject private StorageCacheManager cacheMgr;
    @Inject private EndPointSelector selector;
    @Override
    public StrategyPriority canHandle(DataObject srcData, DataObject destData) {
        if (srcData instanceof SnapshotInfo) {
            if (canHandle(srcData) || canHandle(destData)) {
                return StrategyPriority.HIGHEST;
            }
        }

        if (srcData instanceof TemplateInfo && destData instanceof VolumeInfo &&
                (srcData.getDataStore().getId() == destData.getDataStore().getId()) &&
                (canHandle(srcData) || canHandle(destData))) {
            // Both source and dest are on the same storage, so just clone them.
            return StrategyPriority.HIGHEST;
        }

        return StrategyPriority.CANT_HANDLE;
    }

    private boolean canHandle(DataObject dataObject) {
        Preconditions.checkArgument(dataObject != null, "Passing 'null' to dataObject of canHandle(DataObject) is not supported.");

        DataStore dataStore = dataObject.getDataStore();

        if (dataStore.getRole() == DataStoreRole.Primary) {
            Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

            if (mapCapabilities == null) {
                return false;
            }

            if (dataObject instanceof VolumeInfo || dataObject instanceof  SnapshotInfo) {
                String value = mapCapabilities.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString());
                Boolean supportsStorageSystemSnapshots = Boolean.valueOf(value);

                if (supportsStorageSystemSnapshots) {
                    LOGGER.info("Using 'StorageSystemDataMotionStrategy' (dataObject is a volume or snapshot and the storage system supports snapshots)");

                    return true;
                }
            } else if (dataObject instanceof TemplateInfo) {
                // If the storage system can clone volumes, we can cache templates on it.
                String value = mapCapabilities.get(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString());
                Boolean canCloneVolume = Boolean.valueOf(value);

                if (canCloneVolume) {
                    LOGGER.info("Using 'StorageSystemDataMotionStrategy' (dataObject is a template and the storage system can create a volume from a volume)");

                    return true;
                }

            }
        }

        return false;
    }

    @Override
    public StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        if (srcData instanceof SnapshotInfo) {
            SnapshotInfo snapshotInfo = (SnapshotInfo)srcData;

            validate(snapshotInfo);

            boolean canHandleSrc = canHandle(srcData);

            if (canHandleSrc && (destData instanceof TemplateInfo || destData instanceof SnapshotInfo) &&
                    (destData.getDataStore().getRole() == DataStoreRole.Image || destData.getDataStore().getRole() == DataStoreRole.ImageCache)) {
                handleCopyDataToSecondaryStorage(snapshotInfo, destData, callback);

                return;
            }

            if (destData instanceof VolumeInfo) {
                VolumeInfo volumeInfo = (VolumeInfo)destData;

                boolean canHandleDest = canHandle(destData);

                if (canHandleSrc && canHandleDest) {
                    if (snapshotInfo.getDataStore().getId() == volumeInfo.getDataStore().getId()) {
                        handleCreateVolumeFromSnapshotBothOnStorageSystem(snapshotInfo, volumeInfo, callback);
                        return;
                    }
                    else {
                        String errMsg = "This operation is not supported (DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT " +
                                "not supported by source or destination storage plug-in). " + getSrcDestDataStoreMsg(srcData, destData);

                        LOGGER.warn(errMsg);

                        throw new UnsupportedOperationException(errMsg);
                    }
                }

                if (canHandleDest) {
                    handleCreateVolumeFromSnapshotOnSecondaryStorage(snapshotInfo, volumeInfo, callback);

                    return;
                }

                if (canHandleSrc) {
                    String errMsg = "This operation is not supported (DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT " +
                            "not supported by source storage plug-in). " + getSrcDataStoreMsg(srcData);

                    LOGGER.warn(errMsg);

                    throw new UnsupportedOperationException(errMsg);
                }
            }
        } else if (srcData instanceof TemplateInfo && destData instanceof VolumeInfo) {
            boolean canHandleSrc = canHandle(srcData);

            if (!canHandleSrc) {
                String errMsg = "This operation is not supported (DataStoreCapabilities.STORAGE_CAN_CREATE_VOLUME_FROM_VOLUME " +
                        "not supported by destination storage plug-in). " + getDestDataStoreMsg(destData);

                LOGGER.warn(errMsg);

                throw new UnsupportedOperationException(errMsg);
            }

            handleCreateVolumeFromTemplateBothOnStorageSystem((TemplateInfo)srcData, (VolumeInfo)destData, callback);

            return;
        }

        throw new UnsupportedOperationException("This operation is not supported.");
    }

    private String getSrcDestDataStoreMsg(DataObject srcData, DataObject destData) {
        Preconditions.checkArgument(srcData != null, "Passing 'null' to srcData of getSrcDestDataStoreMsg(DataObject, DataObject) is not supported.");
        Preconditions.checkArgument(destData != null, "Passing 'null' to destData of getSrcDestDataStoreMsg(DataObject, DataObject) is not supported.");

        return "Source data store = " + srcData.getDataStore().getName() + "; " + "Destination data store = " + destData.getDataStore().getName() + ".";
    }

    private String getSrcDataStoreMsg(DataObject srcData) {
        Preconditions.checkArgument(srcData != null, "Passing 'null' to srcData of getSrcDataStoreMsg(DataObject) is not supported.");

        return "Source data store = " + srcData.getDataStore().getName() + ".";
    }

    private String getDestDataStoreMsg(DataObject destData) {
        Preconditions.checkArgument(destData != null, "Passing 'null' to destData of getDestDataStoreMsg(DataObject) is not supported.");

        return "Destination data store = " + destData.getDataStore().getName() + ".";
    }

    private void validate(SnapshotInfo snapshotInfo) {
        long volumeId = snapshotInfo.getVolumeId();

        VolumeVO volumeVO = _volumeDao.findByIdIncludingRemoved(volumeId);

        if (volumeVO.getFormat() != ImageFormat.VHD) {
            throw new CloudRuntimeException("Only the " + ImageFormat.VHD.toString() + " image type is currently supported.");
        }
    }

    private boolean usingBackendSnapshotFor(SnapshotInfo snapshotInfo) {
        String property = getProperty(snapshotInfo.getId(), "takeSnapshot");

        return Boolean.parseBoolean(property);
    }

    protected boolean needCacheStorage(DataObject srcData, DataObject destData) {
        DataTO srcTO = srcData.getTO();
        DataStoreTO srcStoreTO = srcTO.getDataStore();
        DataTO destTO = destData.getTO();
        DataStoreTO destStoreTO = destTO.getDataStore();

        // both snapshot and volume are on primary datastore. No need for a cache storage as
        // hypervisor will copy directly
        if (srcStoreTO instanceof PrimaryDataStoreTO && destStoreTO instanceof PrimaryDataStoreTO) {
            return false;
        }

        if (srcStoreTO instanceof NfsTO || srcStoreTO.getRole() == DataStoreRole.ImageCache) {
            return false;
        }


        if (destStoreTO instanceof NfsTO || destStoreTO.getRole() == DataStoreRole.ImageCache) {
            return false;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("needCacheStorage true, dest at " + destTO.getPath() + " dest role " + destStoreTO.getRole().toString() + srcTO.getPath() + " src role " +
                srcStoreTO.getRole().toString());
        }
        return true;
    }

    private Scope pickCacheScopeForCopy(DataObject srcData, DataObject destData) {
        Scope srcScope = srcData.getDataStore().getScope();
        Scope destScope = destData.getDataStore().getScope();

        Scope selectedScope = null;
        if (srcScope.getScopeId() != null) {
            selectedScope = getZoneScope(srcScope);
        } else if (destScope.getScopeId() != null) {
            selectedScope = getZoneScope(destScope);
        } else {
            LOGGER.warn("Cannot find a zone-wide scope for movement that needs a cache storage");
        }
        return selectedScope;
    }

    private Scope getZoneScope(Scope scope) {
        ZoneScope zoneScope;
        if (scope instanceof ClusterScope) {
            ClusterScope clusterScope = (ClusterScope)scope;
            zoneScope = new ZoneScope(clusterScope.getZoneId());
        } else if (scope instanceof HostScope) {
            HostScope hostScope = (HostScope)scope;
            zoneScope = new ZoneScope(hostScope.getZoneId());
        } else {
            zoneScope = (ZoneScope)scope;
        }
        return zoneScope;
    }

    /**
     * This function is responsible for copying a volume from the managed store to a secondary store. This is used in two cases
     * 1) When creating a template from a snapshot
     * 2) When createSnapshot is called with location=SECONDARY
     *
     * @param snapshotInfo Source snapshot
     * @param destData destination (can be template or snapshot)
     * @param callback callback for async
     */
    private void handleCopyDataToSecondaryStorage(SnapshotInfo snapshotInfo, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        try {
            snapshotInfo.processEvent(Event.CopyingRequested);
        }
        catch (Exception ex) {
            throw new CloudRuntimeException("This snapshot is not currently in a state where it can be used to create a template.");
        }

        HostVO hostVO = getHost(snapshotInfo);

        boolean usingBackendSnapshot = usingBackendSnapshotFor(snapshotInfo);
        boolean computeClusterSupportsResign = clusterDao.getSupportsResigning(hostVO.getClusterId());
        boolean needCache = needCacheStorage(snapshotInfo, destData);

        DataObject destOnStore = destData;

        if (needCache) {
            // creates an object in the DB for data to be cached
            Scope selectedScope = pickCacheScopeForCopy(snapshotInfo, destData);
            destOnStore = cacheMgr.getCacheObject(snapshotInfo, selectedScope);
            destOnStore.processEvent(Event.CreateOnlyRequested);
        }

        if (usingBackendSnapshot && !computeClusterSupportsResign) {
            String noSupportForResignErrMsg = "Unable to locate an applicable host with which to perform a resignature operation : Cluster ID = " + hostVO.getClusterId();

            LOGGER.warn(noSupportForResignErrMsg);

            throw new CloudRuntimeException(noSupportForResignErrMsg);
        }

        try {
            if (usingBackendSnapshot) {
                createVolumeFromSnapshot(hostVO, snapshotInfo, true);
            }

            DataStore srcDataStore = snapshotInfo.getDataStore();

            String value = _configDao.getValue(Config.PrimaryStorageDownloadWait.toString());
            int primaryStorageDownloadWait = NumbersUtil.parseInt(value, Integer.parseInt(Config.PrimaryStorageDownloadWait.getDefaultValue()));
            CopyCommand copyCommand = new CopyCommand(snapshotInfo.getTO(), destOnStore.getTO(), primaryStorageDownloadWait, VirtualMachineManager.ExecuteInSequence.value());

            String errMsg = null;
            CopyCmdAnswer copyCmdAnswer = null;

            try {
                // If we are using a back-end snapshot, then we should still have access to it from the hosts in the cluster that hostVO is in
                // (because we passed in true as the third parameter to createVolumeFromSnapshot above).
                if (!usingBackendSnapshot) {
                    _volumeService.grantAccess(snapshotInfo, hostVO, srcDataStore);
                }

                Map<String, String> srcDetails = getSnapshotDetails(snapshotInfo);

                copyCommand.setOptions(srcDetails);

                copyCmdAnswer = (CopyCmdAnswer)_agentMgr.send(hostVO.getId(), copyCommand);

                if (!copyCmdAnswer.getResult()) {
                    // We were not able to copy. Handle it.
                    errMsg = copyCmdAnswer.getDetails();

                    throw new CloudRuntimeException(errMsg);
                }

                if (needCache) {
                    // If cached storage was needed (in case of object store as secondary
                    // storage), at this point, the data has been copied from the primary
                    // to the NFS cache by the hypervisor. We now invoke another copy
                    // command to copy this data from cache to secondary storage. We
                    // then cleanup the cache

                    destOnStore.processEvent(Event.OperationSuccessed, copyCmdAnswer);

                    CopyCommand cmd = new CopyCommand(destOnStore.getTO(), destData.getTO(), primaryStorageDownloadWait, VirtualMachineManager.ExecuteInSequence.value());
                    EndPoint ep = selector.select(destOnStore, destData);

                    if (ep == null) {
                        errMsg = "No remote endpoint to send command, check if host or ssvm is down?";

                        LOGGER.error(errMsg);

                        copyCmdAnswer = new CopyCmdAnswer(errMsg);
                    } else {
                        copyCmdAnswer = (CopyCmdAnswer)ep.sendMessage(cmd);
                    }

                    // clean up snapshot copied to staging
                    cacheMgr.deleteCacheObject(destOnStore);
                }

            } catch (CloudRuntimeException | AgentUnavailableException | OperationTimedoutException ex) {
                String msg = "Failed to create template from snapshot (Snapshot ID = " + snapshotInfo.getId() + ") : ";

                LOGGER.warn(msg, ex);

                throw new CloudRuntimeException(msg + ex.getMessage());
            } finally {
                _volumeService.revokeAccess(snapshotInfo, hostVO, srcDataStore);

                if (copyCmdAnswer == null || !copyCmdAnswer.getResult()) {
                    if (copyCmdAnswer != null && !StringUtils.isEmpty(copyCmdAnswer.getDetails())) {
                        errMsg = copyCmdAnswer.getDetails();

                        if (needCache) {
                            cacheMgr.deleteCacheObject(destOnStore);
                        }
                    }
                    else {
                        errMsg = "Unable to create template from snapshot";
                    }
                }

                try {
                    if (StringUtils.isEmpty(errMsg)) {
                        snapshotInfo.processEvent(Event.OperationSuccessed);
                    }
                    else {
                        snapshotInfo.processEvent(Event.OperationFailed);
                    }
                }
                catch (Exception ex) {
                    LOGGER.warn("Error processing snapshot event: " + ex.getMessage(), ex);
                }
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
        finally {
            if (usingBackendSnapshot) {
                deleteVolumeFromSnapshot(snapshotInfo);
            }
        }
    }

    /**
     * Creates a volume on the storage from a snapshot that resides on the secondary storage (archived snapshot).
     * @param snapshotInfo snapshot on secondary
     * @param volumeInfo volume to be created on the storage
     * @param callback  for async
     */
    private void handleCreateVolumeFromSnapshotOnSecondaryStorage(SnapshotInfo snapshotInfo, VolumeInfo volumeInfo, AsyncCompletionCallback<CopyCommandResult> callback) {
        // at this point, the snapshotInfo and volumeInfo should have the same disk offering ID (so either one should be OK to get a DiskOfferingVO instance)
        DiskOfferingVO diskOffering = _diskOfferingDao.findByIdIncludingRemoved(volumeInfo.getDiskOfferingId());
        SnapshotVO snapshot = _snapshotDao.findById(snapshotInfo.getId());

        // update the volume's hv_ss_reserve (hypervisor snapshot reserve) from a disk offering (used for managed storage)
        _volumeService.updateHypervisorSnapshotReserveForVolume(diskOffering, volumeInfo.getId(), snapshot.getHypervisorType());

        CopyCmdAnswer copyCmdAnswer = null;
        String errMsg = null;

        HostVO hostVO = null;

        try {
            // create a volume on the storage
            AsyncCallFuture<VolumeApiResult> future = _volumeService.createVolumeAsync(volumeInfo, volumeInfo.getDataStore());
            VolumeApiResult result = future.get();

            if (result.isFailed()) {
                LOGGER.error("Failed to create a volume: " + result.getResult());
                throw new CloudRuntimeException(result.getResult());
            }

            volumeInfo = _volumeDataFactory.getVolume(volumeInfo.getId(), volumeInfo.getDataStore());

            volumeInfo.processEvent(Event.MigrationRequested);

            volumeInfo = _volumeDataFactory.getVolume(volumeInfo.getId(), volumeInfo.getDataStore());

            hostVO = getHost(snapshotInfo.getDataCenterId(), false);

            // copy the volume from secondary via the hypervisor
            copyCmdAnswer = performCopyOfVdi(volumeInfo, snapshotInfo, hostVO);

            if (copyCmdAnswer == null || !copyCmdAnswer.getResult()) {
                if (copyCmdAnswer != null && !StringUtils.isEmpty(copyCmdAnswer.getDetails())) {
                    errMsg = copyCmdAnswer.getDetails();
                }
                else {
                    errMsg = "Unable to create volume from snapshot";
                }
            }
        }
        catch (Exception ex) {
            errMsg = ex.getMessage() != null ? ex.getMessage() : "Copy operation failed in 'StorageSystemDataMotionStrategy.handleCreateVolumeFromSnapshotBothOnStorageSystem'";
        }

        CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

        result.setResult(errMsg);

        callback.complete(result);
    }

    /**
     * Clones a template present on the storage to a new volume and resignatures it.
     *
     * @param templateInfo   source template
     * @param volumeInfo  destination ROOT volume
     * @param callback  for async
     */
    private void handleCreateVolumeFromTemplateBothOnStorageSystem(TemplateInfo templateInfo, VolumeInfo volumeInfo, AsyncCompletionCallback<CopyCommandResult> callback) {
        Preconditions.checkArgument(templateInfo != null, "Passing 'null' to templateInfo of handleCreateVolumeFromTemplateBothOnStorageSystem is not supported.");
        Preconditions.checkArgument(volumeInfo != null, "Passing 'null' to volumeInfo of handleCreateVolumeFromTemplateBothOnStorageSystem is not supported.");

        CopyCmdAnswer copyCmdAnswer = null;
        String errMsg = null;

        HostVO hostVO = getHost(volumeInfo.getDataCenterId(), true);

        if (hostVO == null) {
            throw new CloudRuntimeException("Unable to locate a host capable of resigning in the zone with the following ID: " + volumeInfo.getDataCenterId());
        }

        boolean computeClusterSupportsResign = clusterDao.getSupportsResigning(hostVO.getClusterId());

        if (!computeClusterSupportsResign) {
            String noSupportForResignErrMsg = "Unable to locate an applicable host with which to perform a resignature operation : Cluster ID = " + hostVO.getClusterId();

            LOGGER.warn(noSupportForResignErrMsg);

            throw new CloudRuntimeException(noSupportForResignErrMsg);
        }

        try {
            VolumeDetailVO volumeDetail = new VolumeDetailVO(volumeInfo.getId(),
                    "cloneOfTemplate",
                    String.valueOf(templateInfo.getId()),
                    false);

            volumeDetail = volumeDetailsDao.persist(volumeDetail);

            AsyncCallFuture<VolumeApiResult> future = _volumeService.createVolumeAsync(volumeInfo, volumeInfo.getDataStore());
            int storagePoolMaxWaitSeconds = NumbersUtil.parseInt(_configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);

            VolumeApiResult result = future.get(storagePoolMaxWaitSeconds, TimeUnit.SECONDS);

            if (volumeDetail != null) {
                volumeDetailsDao.remove(volumeDetail.getId());
            }

            if (result.isFailed()) {
                LOGGER.warn("Failed to create a volume: " + result.getResult());
                throw new CloudRuntimeException(result.getResult());
            }

            volumeInfo = _volumeDataFactory.getVolume(volumeInfo.getId(), volumeInfo.getDataStore());
            volumeInfo.processEvent(Event.MigrationRequested);
            volumeInfo = _volumeDataFactory.getVolume(volumeInfo.getId(), volumeInfo.getDataStore());

            copyCmdAnswer = performResignature(volumeInfo, hostVO);

            if (copyCmdAnswer == null || !copyCmdAnswer.getResult()) {
                if (copyCmdAnswer != null && !StringUtils.isEmpty(copyCmdAnswer.getDetails())) {
                    throw new CloudRuntimeException(copyCmdAnswer.getDetails());
                } else {
                    throw new CloudRuntimeException("Unable to create a volume from a template");
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException ex ) {
            volumeInfo.getDataStore().getDriver().deleteAsync(volumeInfo.getDataStore(), volumeInfo, null);

            throw new CloudRuntimeException("Create volume from template (ID = " + templateInfo.getId() + ") failed " + ex.getMessage());
        }

        CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

        result.setResult(errMsg);

        callback.complete(result);
    }

    private void handleCreateVolumeFromSnapshotBothOnStorageSystem(SnapshotInfo snapshotInfo, VolumeInfo volumeInfo, AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCmdAnswer copyCmdAnswer = null;
        String errMsg = null;

        try {
            HostVO hostVO = getHost(snapshotInfo);

            boolean usingBackendSnapshot = usingBackendSnapshotFor(snapshotInfo);
            boolean computeClusterSupportsResign = clusterDao.getSupportsResigning(hostVO.getClusterId());

            if (usingBackendSnapshot && !computeClusterSupportsResign) {
                String noSupportForResignErrMsg = "Unable to locate an applicable host with which to perform a resignature operation : Cluster ID = " + hostVO.getClusterId();

                LOGGER.warn(noSupportForResignErrMsg);

                throw new CloudRuntimeException(noSupportForResignErrMsg);
            }

            boolean canStorageSystemCreateVolumeFromVolume = canStorageSystemCreateVolumeFromVolume(snapshotInfo);
            boolean useCloning = usingBackendSnapshot || (canStorageSystemCreateVolumeFromVolume && computeClusterSupportsResign);

            VolumeDetailVO volumeDetail = null;

            if (useCloning) {
                volumeDetail = new VolumeDetailVO(volumeInfo.getId(),
                    "cloneOfSnapshot",
                    String.valueOf(snapshotInfo.getId()),
                    false);

                volumeDetail = volumeDetailsDao.persist(volumeDetail);
            }

            // at this point, the snapshotInfo and volumeInfo should have the same disk offering ID (so either one should be OK to get a DiskOfferingVO instance)
            DiskOfferingVO diskOffering = _diskOfferingDao.findByIdIncludingRemoved(volumeInfo.getDiskOfferingId());
            SnapshotVO snapshot = _snapshotDao.findById(snapshotInfo.getId());

            // update the volume's hv_ss_reserve (hypervisor snapshot reserve) from a disk offering (used for managed storage)
            _volumeService.updateHypervisorSnapshotReserveForVolume(diskOffering, volumeInfo.getId(), snapshot.getHypervisorType());

            AsyncCallFuture<VolumeApiResult> future = _volumeService.createVolumeAsync(volumeInfo, volumeInfo.getDataStore());

            VolumeApiResult result = future.get();

            if (volumeDetail != null) {
                volumeDetailsDao.remove(volumeDetail.getId());
            }

            if (result.isFailed()) {
                LOGGER.warn("Failed to create a volume: " + result.getResult());

                throw new CloudRuntimeException(result.getResult());
            }

            volumeInfo = _volumeDataFactory.getVolume(volumeInfo.getId(), volumeInfo.getDataStore());

            volumeInfo.processEvent(Event.MigrationRequested);

            volumeInfo = _volumeDataFactory.getVolume(volumeInfo.getId(), volumeInfo.getDataStore());

            if (useCloning) {
                copyCmdAnswer = performResignature(volumeInfo, hostVO);
            }
            else {
                // asking for a XenServer host here so we don't always prefer to use XenServer hosts that support resigning
                // even when we don't need those hosts to do this kind of copy work
                hostVO = getHost(snapshotInfo.getDataCenterId(), false);

                copyCmdAnswer = performCopyOfVdi(volumeInfo, snapshotInfo, hostVO);
            }

            if (copyCmdAnswer == null || !copyCmdAnswer.getResult()) {
                if (copyCmdAnswer != null && !StringUtils.isEmpty(copyCmdAnswer.getDetails())) {
                    errMsg = copyCmdAnswer.getDetails();
                }
                else {
                    errMsg = "Unable to create volume from snapshot";
                }
            }
        }
        catch (Exception ex) {
            errMsg = ex.getMessage() != null ? ex.getMessage() : "Copy operation failed in 'StorageSystemDataMotionStrategy.handleCreateVolumeFromSnapshotBothOnStorageSystem'";
        }

        CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

        result.setResult(errMsg);

        callback.complete(result);
    }

    /**
     * If the underlying storage system is making use of read-only snapshots, this gives the storage system the opportunity to
     * create a volume from the snapshot so that we can copy the VHD file that should be inside of the snapshot to secondary storage.
     *
     * The resultant volume must be writable because we need to resign the SR and the VDI that should be inside of it before we copy
     * the VHD file to secondary storage.
     *
     * If the storage system is using writable snapshots, then nothing need be done by that storage system here because we can just
     * resign the SR and the VDI that should be inside of the snapshot before copying the VHD file to secondary storage.
     */
    private void createVolumeFromSnapshot(HostVO hostVO, SnapshotInfo snapshotInfo, boolean keepGrantedAccess) {
        SnapshotDetailsVO snapshotDetails = handleSnapshotDetails(snapshotInfo.getId(), "tempVolume", "create");

        try {
            snapshotInfo.getDataStore().getDriver().createAsync(snapshotInfo.getDataStore(), snapshotInfo, null);
        }
        finally {
            _snapshotDetailsDao.remove(snapshotDetails.getId());
        }

        CopyCmdAnswer copyCmdAnswer = performResignature(snapshotInfo, hostVO, keepGrantedAccess);

        if (copyCmdAnswer == null || !copyCmdAnswer.getResult()) {
            if (copyCmdAnswer != null && !StringUtils.isEmpty(copyCmdAnswer.getDetails())) {
                throw new CloudRuntimeException(copyCmdAnswer.getDetails());
            } else {
                throw new CloudRuntimeException("Unable to create volume from snapshot");
            }
        }
    }

    /**
     * If the underlying storage system needed to create a volume from a snapshot for createVolumeFromSnapshot(HostVO, SnapshotInfo), then
     * this is its opportunity to delete that temporary volume and restore properties in snapshot_details to the way they were before the
     * invocation of createVolumeFromSnapshot(HostVO, SnapshotInfo).
     */
    private void deleteVolumeFromSnapshot(SnapshotInfo snapshotInfo) {
        SnapshotDetailsVO snapshotDetails = handleSnapshotDetails(snapshotInfo.getId(), "tempVolume", "delete");

        try {
            snapshotInfo.getDataStore().getDriver().createAsync(snapshotInfo.getDataStore(), snapshotInfo, null);
        }
        finally {
            _snapshotDetailsDao.remove(snapshotDetails.getId());
        }
    }

    private SnapshotDetailsVO handleSnapshotDetails(long csSnapshotId, String name, String value) {
        _snapshotDetailsDao.removeDetail(csSnapshotId, name);

        SnapshotDetailsVO snapshotDetails = new SnapshotDetailsVO(csSnapshotId, name, value, false);

        return _snapshotDetailsDao.persist(snapshotDetails);
    }

    private boolean canStorageSystemCreateVolumeFromVolume(SnapshotInfo snapshotInfo) {
        boolean supportsCloningVolumeFromVolume = false;

        DataStore dataStore = dataStoreMgr.getDataStore(snapshotInfo.getDataStore().getId(), DataStoreRole.Primary);

        Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

        if (mapCapabilities != null) {
            String value = mapCapabilities.get(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString());

            supportsCloningVolumeFromVolume = Boolean.valueOf(value);
        }

        return supportsCloningVolumeFromVolume;
    }

    private String getProperty(long snapshotId, String property) {
        SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(snapshotId, property);

        if (snapshotDetails != null) {
            return snapshotDetails.getValue();
        }

        return null;
    }

    private Map<String, String> getVolumeDetails(VolumeInfo volumeInfo) {
        Map<String, String> volumeDetails = new HashMap<String, String>();

        VolumeVO volumeVO = _volumeDao.findById(volumeInfo.getId());

        long storagePoolId = volumeVO.getPoolId();
        StoragePoolVO storagePoolVO = _storagePoolDao.findById(storagePoolId);

        volumeDetails.put(DiskTO.STORAGE_HOST, storagePoolVO.getHostAddress());
        volumeDetails.put(DiskTO.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));
        volumeDetails.put(DiskTO.IQN, volumeVO.get_iScsiName());

        ChapInfo chapInfo = _volumeService.getChapInfo(volumeInfo, volumeInfo.getDataStore());

        if (chapInfo != null) {
            volumeDetails.put(DiskTO.CHAP_INITIATOR_USERNAME, chapInfo.getInitiatorUsername());
            volumeDetails.put(DiskTO.CHAP_INITIATOR_SECRET, chapInfo.getInitiatorSecret());
            volumeDetails.put(DiskTO.CHAP_TARGET_USERNAME, chapInfo.getTargetUsername());
            volumeDetails.put(DiskTO.CHAP_TARGET_SECRET, chapInfo.getTargetSecret());
        }

        return volumeDetails;
    }

    private Map<String, String> getSnapshotDetails(SnapshotInfo snapshotInfo) {
        Map<String, String> snapshotDetails = new HashMap<>();

        long storagePoolId = snapshotInfo.getDataStore().getId();
        StoragePoolVO storagePoolVO = _storagePoolDao.findById(storagePoolId);

        snapshotDetails.put(DiskTO.STORAGE_HOST, storagePoolVO.getHostAddress());
        snapshotDetails.put(DiskTO.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));

        long snapshotId = snapshotInfo.getId();

        snapshotDetails.put(DiskTO.IQN, getProperty(snapshotId, DiskTO.IQN));

        snapshotDetails.put(DiskTO.CHAP_INITIATOR_USERNAME, getProperty(snapshotId, DiskTO.CHAP_INITIATOR_USERNAME));
        snapshotDetails.put(DiskTO.CHAP_INITIATOR_SECRET, getProperty(snapshotId, DiskTO.CHAP_INITIATOR_SECRET));
        snapshotDetails.put(DiskTO.CHAP_TARGET_USERNAME, getProperty(snapshotId, DiskTO.CHAP_TARGET_USERNAME));
        snapshotDetails.put(DiskTO.CHAP_TARGET_SECRET, getProperty(snapshotId, DiskTO.CHAP_TARGET_SECRET));

        return snapshotDetails;
    }

    private HostVO getHost(SnapshotInfo snapshotInfo) {
        HostVO hostVO = getHost(snapshotInfo.getDataCenterId(), true);

        if (hostVO == null) {
            hostVO = getHost(snapshotInfo.getDataCenterId(), false);

            if (hostVO == null) {
                throw new CloudRuntimeException("Unable to locate an applicable host in data center with ID = " + snapshotInfo.getDataCenterId());
            }
        }

        return hostVO;
    }

    private HostVO getHost(Long zoneId, boolean computeClusterMustSupportResign) {
        Preconditions.checkArgument(zoneId != null, "Zone ID cannot be null.");

        List<HostVO> hosts = _hostDao.listByDataCenterIdAndHypervisorType(zoneId, HypervisorType.XenServer);

        if (hosts == null) {
            return null;
        }

        List<Long> clustersToSkip = new ArrayList<>();

        Collections.shuffle(hosts, RANDOM);

        for (HostVO host : hosts) {
            if (computeClusterMustSupportResign) {
                long clusterId = host.getClusterId();

                if (clustersToSkip.contains(clusterId)) {
                    continue;
                }

                if (clusterDao.getSupportsResigning(clusterId)) {
                    return host;
                }
                else {
                    clustersToSkip.add(clusterId);
                }
            }
            else {
                return host;
            }
        }

        return null;
    }

    @Override
    public void copyAsync(Map<VolumeInfo, DataStore> volumeMap, VirtualMachineTO vmTo, Host srcHost, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCommandResult result = new CopyCommandResult(null, null);

        result.setResult("Unsupported operation requested for copying data.");

        callback.complete(result);
    }

    private Map<String, String> getDetails(DataObject dataObj) {
        if (dataObj instanceof VolumeInfo) {
            return getVolumeDetails((VolumeInfo)dataObj);
        }
        else if (dataObj instanceof SnapshotInfo) {
            return getSnapshotDetails((SnapshotInfo)dataObj);
        }

        throw new CloudRuntimeException("'dataObj' must be of type 'VolumeInfo' or 'SnapshotInfo'.");
    }

    private CopyCmdAnswer performResignature(DataObject dataObj, HostVO hostVO) {
        return performResignature(dataObj, hostVO, false);
    }

    private CopyCmdAnswer performResignature(DataObject dataObj, HostVO hostVO, boolean keepGrantedAccess) {
        long storagePoolId = dataObj.getDataStore().getId();
        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Map<String, String> details = getDetails(dataObj);

        ResignatureCommand command = new ResignatureCommand(details);

        ResignatureAnswer answer = null;

        try {
            _volumeService.grantAccess(dataObj, hostVO, dataStore);

            answer = (ResignatureAnswer)_agentMgr.send(hostVO.getId(), command);
        }
        catch (CloudRuntimeException | AgentUnavailableException | OperationTimedoutException ex) {
            keepGrantedAccess = false;

            String msg = "Failed to resign the DataObject with the following ID: " + dataObj.getId();

            LOGGER.warn(msg, ex);

            throw new CloudRuntimeException(msg + ex.getMessage());
        }
        finally {
            if (keepGrantedAccess == false) {
                _volumeService.revokeAccess(dataObj, hostVO, dataStore);
            }
        }

        if (answer == null || !answer.getResult()) {
            final String errMsg;

            if (answer != null && answer.getDetails() != null && !answer.getDetails().isEmpty()) {
                errMsg = answer.getDetails();
            }
            else {
                errMsg = "Unable to perform resignature operation in 'StorageSystemDataMotionStrategy.performResignature'";
            }

            throw new CloudRuntimeException(errMsg);
        }

        VolumeObjectTO newVolume = new VolumeObjectTO();

        newVolume.setSize(answer.getSize());
        newVolume.setPath(answer.getPath());
        newVolume.setFormat(answer.getFormat());

        return new CopyCmdAnswer(newVolume);
    }

    protected DataObject cacheSnapshotChain(SnapshotInfo snapshot, Scope scope) {
        DataObject leafData = null;
        DataStore store = cacheMgr.getCacheStorage(snapshot, scope);
        while (snapshot != null) {
            DataObject cacheData = cacheMgr.createCacheObject(snapshot, store);
            if (leafData == null) {
                leafData = cacheData;
            }
            snapshot = snapshot.getParent();
        }
        return leafData;
    }

    /**
     * Copies data from secondary storage to a primary volume
     * @param volumeInfo The primary volume
     * @param snapshotInfo  destination of the copy
     * @param hostVO the host used to copy the data
     * @return result of the copy
     */
    private CopyCmdAnswer performCopyOfVdi(VolumeInfo volumeInfo, SnapshotInfo snapshotInfo, HostVO hostVO) {
        Snapshot.LocationType locationType = snapshotInfo.getLocationType();

        String value = _configDao.getValue(Config.PrimaryStorageDownloadWait.toString());
        int primaryStorageDownloadWait = NumbersUtil.parseInt(value, Integer.parseInt(Config.PrimaryStorageDownloadWait.getDefaultValue()));

        DataObject srcData = snapshotInfo;
        CopyCmdAnswer copyCmdAnswer = null;
        DataObject cacheData = null;

        boolean needCacheStorage = needCacheStorage(snapshotInfo, volumeInfo);

        if (needCacheStorage) {
            cacheData = cacheSnapshotChain(snapshotInfo, new ZoneScope(volumeInfo.getDataCenterId()));
            srcData = cacheData;
        }

        CopyCommand copyCommand = new CopyCommand(srcData.getTO(), volumeInfo.getTO(), primaryStorageDownloadWait, VirtualMachineManager.ExecuteInSequence.value());

        try {
            if (Snapshot.LocationType.PRIMARY.equals(locationType)) {
                _volumeService.grantAccess(snapshotInfo, hostVO, snapshotInfo.getDataStore());

                Map<String, String> srcDetails = getSnapshotDetails(snapshotInfo);

                copyCommand.setOptions(srcDetails);
            }

            _volumeService.grantAccess(volumeInfo, hostVO, volumeInfo.getDataStore());

            Map<String, String> destDetails = getVolumeDetails(volumeInfo);

            copyCommand.setOptions2(destDetails);

            copyCmdAnswer = (CopyCmdAnswer)_agentMgr.send(hostVO.getId(), copyCommand);
        }
        catch (CloudRuntimeException | AgentUnavailableException | OperationTimedoutException ex) {
            String msg = "Failed to perform VDI copy : ";

            LOGGER.warn(msg, ex);

            throw new CloudRuntimeException(msg + ex.getMessage());
        }
        finally {
            if (Snapshot.LocationType.PRIMARY.equals(locationType)) {
                _volumeService.revokeAccess(snapshotInfo, hostVO, snapshotInfo.getDataStore());
            }

            _volumeService.revokeAccess(volumeInfo, hostVO, volumeInfo.getDataStore());

            if (needCacheStorage && copyCmdAnswer != null && copyCmdAnswer.getResult()) {
                cacheMgr.deleteCacheObject(cacheData);
            }
        }

        return copyCmdAnswer;
    }
}
