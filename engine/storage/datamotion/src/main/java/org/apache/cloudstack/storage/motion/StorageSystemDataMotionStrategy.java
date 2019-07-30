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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

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
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageAction;
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
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo;
import com.cloud.agent.api.ModifyTargetsAnswer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.storage.CheckStorageAvailabilityCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
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
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.MigrationOptions;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Preconditions;

public class StorageSystemDataMotionStrategy implements DataMotionStrategy {
    private static final Logger LOGGER = Logger.getLogger(StorageSystemDataMotionStrategy.class);
    private static final Random RANDOM = new Random(System.nanoTime());
    private static final int LOCK_TIME_IN_SECONDS = 300;
    private static final String OPERATION_NOT_SUPPORTED = "This operation is not supported.";

    @Inject
    protected AgentManager agentManager;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private DataStoreManager dataStoreMgr;
    @Inject
    protected DiskOfferingDao _diskOfferingDao;
    @Inject
    private GuestOSCategoryDao _guestOsCategoryDao;
    @Inject
    private GuestOSDao _guestOsDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    protected PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private SnapshotDao _snapshotDao;
    @Inject
    private SnapshotDataStoreDao _snapshotDataStoreDao;
    @Inject
    private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private VMTemplateDao _vmTemplateDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private VolumeDataFactory _volumeDataFactory;
    @Inject
    private VolumeDetailsDao volumeDetailsDao;
    @Inject
    private VolumeService _volumeService;
    @Inject
    private StorageCacheManager cacheMgr;
    @Inject
    private EndPointSelector selector;
    @Inject
    VMTemplatePoolDao templatePoolDao;

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

        if (srcData instanceof VolumeInfo && destData instanceof VolumeInfo) {
            VolumeInfo srcVolumeInfo = (VolumeInfo)srcData;

            if (isVolumeOnManagedStorage(srcVolumeInfo)) {
                return StrategyPriority.HIGHEST;
            }

            VolumeInfo destVolumeInfo = (VolumeInfo)destData;

            if (isVolumeOnManagedStorage(destVolumeInfo)) {
                return StrategyPriority.HIGHEST;
            }
        }

        if (srcData instanceof VolumeInfo && destData instanceof TemplateInfo) {
            VolumeInfo srcVolumeInfo = (VolumeInfo)srcData;

            if (isVolumeOnManagedStorage(srcVolumeInfo)) {
                return StrategyPriority.HIGHEST;
            }
        }

        return StrategyPriority.CANT_HANDLE;
    }

    private boolean isVolumeOnManagedStorage(VolumeInfo volumeInfo) {
        DataStore dataStore = volumeInfo.getDataStore();

        if (dataStore.getRole() == DataStoreRole.Primary) {
            long storagePooldId = dataStore.getId();
            StoragePoolVO storagePoolVO = _storagePoolDao.findById(storagePooldId);

            return storagePoolVO.isManaged();
        }

        return false;
    }

    // canHandle returns true if the storage driver for the DataObject that's passed in can support certain features (what features we
    // care about during a particular invocation of this method depend on what type of DataObject was passed in (ex. VolumeInfo versus SnapshotInfo)).
    private boolean canHandle(DataObject dataObject) {
        Preconditions.checkArgument(dataObject != null, "Passing 'null' to dataObject of canHandle(DataObject) is not supported.");

        DataStore dataStore = dataObject.getDataStore();

        if (dataStore.getRole() == DataStoreRole.Primary) {
            Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

            if (mapCapabilities == null) {
                return false;
            }

            if (dataObject instanceof VolumeInfo || dataObject instanceof SnapshotInfo) {
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
    public final StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        if (HypervisorType.KVM.equals(srcHost.getHypervisorType())) {
            return internalCanHandle(volumeMap, srcHost, destHost);
        }
        return StrategyPriority.CANT_HANDLE;
    }

    /**
     * Handles migrating volumes on managed Storage.
     */
    protected StrategyPriority internalCanHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        Set<VolumeInfo> volumeInfoSet = volumeMap.keySet();

        for (VolumeInfo volumeInfo : volumeInfoSet) {
            StoragePoolVO storagePoolVO = _storagePoolDao.findById(volumeInfo.getPoolId());

            if (storagePoolVO.isManaged()) {
                return StrategyPriority.HIGHEST;
            }
        }

        Collection<DataStore> dataStores = volumeMap.values();

        for (DataStore dataStore : dataStores) {
            StoragePoolVO storagePoolVO = _storagePoolDao.findById(dataStore.getId());

            if (storagePoolVO.isManaged()) {
                return StrategyPriority.HIGHEST;
            }

        }
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        if (srcData instanceof SnapshotInfo) {
            SnapshotInfo srcSnapshotInfo = (SnapshotInfo)srcData;

            handleCopyAsyncForSnapshot(srcSnapshotInfo, destData, callback);
        } else if (srcData instanceof TemplateInfo && destData instanceof VolumeInfo) {
            TemplateInfo srcTemplateInfo = (TemplateInfo)srcData;
            VolumeInfo destVolumeInfo = (VolumeInfo)destData;

            handleCopyAsyncForTemplateAndVolume(srcTemplateInfo, destVolumeInfo, callback);
        } else if (srcData instanceof VolumeInfo && destData instanceof VolumeInfo) {
            VolumeInfo srcVolumeInfo = (VolumeInfo)srcData;
            VolumeInfo destVolumeInfo = (VolumeInfo)destData;

            handleCopyAsyncForVolumes(srcVolumeInfo, destVolumeInfo, callback);
        } else if (srcData instanceof VolumeInfo && destData instanceof TemplateInfo &&
                (destData.getDataStore().getRole() == DataStoreRole.Image || destData.getDataStore().getRole() == DataStoreRole.ImageCache)) {
            VolumeInfo srcVolumeInfo = (VolumeInfo)srcData;
            TemplateInfo destTemplateInfo = (TemplateInfo)destData;

            handleCreateTemplateFromManagedVolume(srcVolumeInfo, destTemplateInfo, callback);
        }
        else {
            handleError(OPERATION_NOT_SUPPORTED, callback);
        }
    }

    private void handleCopyAsyncForSnapshot(SnapshotInfo srcSnapshotInfo, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        verifyFormat(srcSnapshotInfo);

        boolean canHandleSrc = canHandle(srcSnapshotInfo);

        if (canHandleSrc && (destData instanceof TemplateInfo || destData instanceof SnapshotInfo) &&
                (destData.getDataStore().getRole() == DataStoreRole.Image || destData.getDataStore().getRole() == DataStoreRole.ImageCache)) {
            handleCopyAsyncToSecondaryStorage(srcSnapshotInfo, destData, callback);
        } else if (destData instanceof VolumeInfo) {
            handleCopyAsyncForSnapshotToVolume(srcSnapshotInfo, (VolumeInfo)destData, callback);
        } else {
            handleError(OPERATION_NOT_SUPPORTED, callback);
        }
    }

    private void handleCopyAsyncForSnapshotToVolume(SnapshotInfo srcSnapshotInfo, VolumeInfo destVolumeInfo,
                                                    AsyncCompletionCallback<CopyCommandResult> callback) {
        boolean canHandleSrc = canHandle(srcSnapshotInfo);
        boolean canHandleDest = canHandle(destVolumeInfo);

        if (canHandleSrc && canHandleDest) {
            if (srcSnapshotInfo.getDataStore().getId() == destVolumeInfo.getDataStore().getId()) {
                handleCreateManagedVolumeFromManagedSnapshot(srcSnapshotInfo, destVolumeInfo, callback);
            } else {
                String errMsg = "To perform this operation, the source and destination primary storages must be the same.";

                handleError(errMsg, callback);
            }
        }
        else if (!canHandleSrc && !canHandleDest) {
            handleError(OPERATION_NOT_SUPPORTED, callback);
        }
        else if (canHandleSrc) {
            handleCreateNonManagedVolumeFromManagedSnapshot(srcSnapshotInfo, destVolumeInfo, callback);
        }
        else {
            handleCreateManagedVolumeFromNonManagedSnapshot(srcSnapshotInfo, destVolumeInfo, callback);
        }
    }

    private void handleCopyAsyncForTemplateAndVolume(TemplateInfo srcTemplateInfo, VolumeInfo destVolumeInfo, AsyncCompletionCallback<CopyCommandResult> callback) {
        boolean canHandleSrc = canHandle(srcTemplateInfo);

        if (!canHandleSrc) {
            handleError(OPERATION_NOT_SUPPORTED, callback);
        }

        handleCreateVolumeFromTemplateBothOnStorageSystem(srcTemplateInfo, destVolumeInfo, callback);
    }

    private void handleCopyAsyncForVolumes(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo, AsyncCompletionCallback<CopyCommandResult> callback) {
        if (srcVolumeInfo.getState() == Volume.State.Migrating) {
            if (isVolumeOnManagedStorage(srcVolumeInfo)) {
                if (destVolumeInfo.getDataStore().getRole() == DataStoreRole.Image || destVolumeInfo.getDataStore().getRole() == DataStoreRole.ImageCache) {
                    handleVolumeCopyFromManagedStorageToSecondaryStorage(srcVolumeInfo, destVolumeInfo, callback);
                } else if (!isVolumeOnManagedStorage(destVolumeInfo)) {
                    handleVolumeMigrationFromManagedStorageToNonManagedStorage(srcVolumeInfo, destVolumeInfo, callback);
                } else {
                    String errMsg = "The source volume to migrate and the destination volume are both on managed storage. " +
                            "Migration in this case is not yet supported.";

                    handleError(errMsg, callback);
                }
            } else if (!isVolumeOnManagedStorage(destVolumeInfo)) {
                String errMsg = "The 'StorageSystemDataMotionStrategy' does not support this migration use case.";

                handleError(errMsg, callback);
            } else {
                handleVolumeMigrationFromNonManagedStorageToManagedStorage(srcVolumeInfo, destVolumeInfo, callback);
            }
        } else if (srcVolumeInfo.getState() == Volume.State.Uploaded &&
                (srcVolumeInfo.getDataStore().getRole() == DataStoreRole.Image || srcVolumeInfo.getDataStore().getRole() == DataStoreRole.ImageCache) &&
                destVolumeInfo.getDataStore().getRole() == DataStoreRole.Primary) {
            ImageFormat imageFormat = destVolumeInfo.getFormat();

            if (!ImageFormat.QCOW2.equals(imageFormat)) {
                String errMsg = "The 'StorageSystemDataMotionStrategy' does not support this upload use case (non KVM).";

                handleError(errMsg, callback);
            }

            handleCreateVolumeFromVolumeOnSecondaryStorage(srcVolumeInfo, destVolumeInfo, destVolumeInfo.getDataCenterId(), HypervisorType.KVM, callback);
        } else {
            handleError(OPERATION_NOT_SUPPORTED, callback);
        }
    }

    private void handleError(String errMsg, AsyncCompletionCallback<CopyCommandResult> callback) {
        LOGGER.warn(errMsg);

        invokeCallback(errMsg, callback);

        throw new UnsupportedOperationException(errMsg);
    }

    private void invokeCallback(String errMsg, AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCmdAnswer copyCmdAnswer = new CopyCmdAnswer(errMsg);

        CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

        result.setResult(errMsg);

        callback.complete(result);
    }

    private void handleVolumeCopyFromManagedStorageToSecondaryStorage(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo,
                                                                      AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;
        String volumePath = null;

        try {
            if (!ImageFormat.QCOW2.equals(srcVolumeInfo.getFormat())) {
                throw new CloudRuntimeException("Currently, only the KVM hypervisor type is supported for the migration of a volume " +
                        "from managed storage to non-managed storage.");
            }

            HypervisorType hypervisorType = HypervisorType.KVM;
            VirtualMachine vm = srcVolumeInfo.getAttachedVM();

            if (vm != null && vm.getState() != VirtualMachine.State.Stopped) {
                throw new CloudRuntimeException("Currently, if a volume to copy from managed storage to secondary storage is attached to " +
                        "a VM, the VM must be in the Stopped state.");
            }

            long srcStoragePoolId = srcVolumeInfo.getPoolId();
            StoragePoolVO srcStoragePoolVO = _storagePoolDao.findById(srcStoragePoolId);

            HostVO hostVO;

            if (srcStoragePoolVO.getClusterId() != null) {
                hostVO = getHostInCluster(srcStoragePoolVO.getClusterId());
            }
            else {
                hostVO = getHost(srcVolumeInfo.getDataCenterId(), hypervisorType, false);
            }

            volumePath = copyManagedVolumeToSecondaryStorage(srcVolumeInfo, destVolumeInfo, hostVO,
                    "Unable to copy the volume from managed storage to secondary storage");
        }
        catch (Exception ex) {
            errMsg = "Migration operation failed in 'StorageSystemDataMotionStrategy.handleVolumeCopyFromManagedStorageToSecondaryStorage': " +
                    ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            CopyCmdAnswer copyCmdAnswer;

            if (errMsg != null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }
            else if (volumePath == null) {
                copyCmdAnswer = new CopyCmdAnswer("Unable to acquire a volume path");
            }
            else {
                VolumeObjectTO volumeObjectTO = (VolumeObjectTO)destVolumeInfo.getTO();

                volumeObjectTO.setPath(volumePath);

                copyCmdAnswer = new CopyCmdAnswer(volumeObjectTO);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void handleVolumeMigrationFromManagedStorageToNonManagedStorage(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo,
                                                                            AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;

        try {
            if (!ImageFormat.QCOW2.equals(srcVolumeInfo.getFormat())) {
                throw new CloudRuntimeException("Currently, only the KVM hypervisor type is supported for the migration of a volume " +
                        "from managed storage to non-managed storage.");
            }

            HypervisorType hypervisorType = HypervisorType.KVM;
            VirtualMachine vm = srcVolumeInfo.getAttachedVM();

            if (vm != null && vm.getState() != VirtualMachine.State.Stopped) {
                throw new CloudRuntimeException("Currently, if a volume to migrate from managed storage to non-managed storage is attached to " +
                        "a VM, the VM must be in the Stopped state.");
            }

            long destStoragePoolId = destVolumeInfo.getPoolId();
            StoragePoolVO destStoragePoolVO = _storagePoolDao.findById(destStoragePoolId);

            HostVO hostVO;

            if (destStoragePoolVO.getClusterId() != null) {
                hostVO = getHostInCluster(destStoragePoolVO.getClusterId());
            }
            else {
                hostVO = getHost(destVolumeInfo.getDataCenterId(), hypervisorType, false);
            }

            setCertainVolumeValuesNull(destVolumeInfo.getId());

            // migrate the volume via the hypervisor
            String path = migrateVolumeForKVM(srcVolumeInfo, destVolumeInfo, hostVO, "Unable to migrate the volume from managed storage to non-managed storage");

            updateVolumePath(destVolumeInfo.getId(), path);
        }
        catch (Exception ex) {
            errMsg = "Migration operation failed in 'StorageSystemDataMotionStrategy.handleVolumeMigrationFromManagedStorageToNonManagedStorage': " +
                    ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            CopyCmdAnswer copyCmdAnswer;

            if (errMsg != null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }
            else {
                destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId(), destVolumeInfo.getDataStore());

                DataTO dataTO = destVolumeInfo.getTO();

                copyCmdAnswer = new CopyCmdAnswer(dataTO);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void verifyFormat(ImageFormat imageFormat) {
        if (imageFormat != ImageFormat.VHD && imageFormat != ImageFormat.OVA && imageFormat != ImageFormat.QCOW2) {
            throw new CloudRuntimeException("Only the following image types are currently supported: " +
                    ImageFormat.VHD.toString() + ", " + ImageFormat.OVA.toString() + ", and " + ImageFormat.QCOW2);
        }
    }

    private void verifyFormat(SnapshotInfo snapshotInfo) {
        long volumeId = snapshotInfo.getVolumeId();

        VolumeVO volumeVO = _volumeDao.findByIdIncludingRemoved(volumeId);

        verifyFormat(volumeVO.getFormat());
    }

    private boolean usingBackendSnapshotFor(SnapshotInfo snapshotInfo) {
        String property = getSnapshotProperty(snapshotInfo.getId(), "takeSnapshot");

        return Boolean.parseBoolean(property);
    }

    private boolean needCacheStorage(DataObject srcData, DataObject destData) {
        DataTO srcTO = srcData.getTO();
        DataStoreTO srcStoreTO = srcTO.getDataStore();
        DataTO destTO = destData.getTO();
        DataStoreTO destStoreTO = destTO.getDataStore();

        // both snapshot and volume are on primary datastore - no need for a cache storage as hypervisor will copy directly
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
            LOGGER.debug("needCacheStorage true; dest at " + destTO.getPath() + ", dest role " + destStoreTO.getRole().toString() + "; src at " +
                    srcTO.getPath() + ", src role " + srcStoreTO.getRole().toString());
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

    private void handleVolumeMigrationFromNonManagedStorageToManagedStorage(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo,
                                                                            AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;

        try {
            HypervisorType hypervisorType = srcVolumeInfo.getHypervisorType();

            if (!HypervisorType.XenServer.equals(hypervisorType) && !HypervisorType.KVM.equals(hypervisorType)) {
                throw new CloudRuntimeException("Currently, only the XenServer and KVM hypervisor types are supported for the migration of a volume " +
                        "from non-managed storage to managed storage.");
            }

            if (HypervisorType.XenServer.equals(hypervisorType)) {
                handleVolumeMigrationForXenServer(srcVolumeInfo, destVolumeInfo);
            }
            else {
                handleVolumeMigrationForKVM(srcVolumeInfo, destVolumeInfo);
            }
        }
        catch (Exception ex) {
            errMsg = "Migration operation failed in 'StorageSystemDataMotionStrategy.handleVolumeMigrationFromNonManagedStorageToManagedStorage': " +
                    ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            CopyCmdAnswer copyCmdAnswer;

            if (errMsg != null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }
            else {
                destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId(), destVolumeInfo.getDataStore());

                DataTO dataTO = destVolumeInfo.getTO();

                copyCmdAnswer = new CopyCmdAnswer(dataTO);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void handleVolumeMigrationForXenServer(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo) {
        VirtualMachine vm = srcVolumeInfo.getAttachedVM();

        if (vm == null || vm.getState() != VirtualMachine.State.Running) {
            throw new CloudRuntimeException("Currently, a volume to migrate from non-managed storage to managed storage on XenServer must be attached to " +
                    "a VM in the Running state.");
        }

        destVolumeInfo.getDataStore().getDriver().createAsync(destVolumeInfo.getDataStore(), destVolumeInfo, null);

        destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId(), destVolumeInfo.getDataStore());

        handleQualityOfServiceForVolumeMigration(destVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.MIGRATION);

        HostVO hostVO = _hostDao.findById(vm.getHostId());

        _volumeService.grantAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());

        String value = _configDao.getValue(Config.MigrateWait.key());
        int waitInterval = NumbersUtil.parseInt(value, Integer.parseInt(Config.MigrateWait.getDefaultValue()));

        StoragePool destPool = (StoragePool)dataStoreMgr.getDataStore(destVolumeInfo.getDataStore().getId(), DataStoreRole.Primary);

        MigrateVolumeCommand command = new MigrateVolumeCommand(srcVolumeInfo.getId(), srcVolumeInfo.getPath(), destPool, srcVolumeInfo.getAttachedVmName(),
                srcVolumeInfo.getVolumeType(), waitInterval);

        Map<String, String> details = new HashMap<>();

        details.put(DiskTO.MANAGED, Boolean.TRUE.toString());
        details.put(DiskTO.IQN, destVolumeInfo.get_iScsiName());
        details.put(DiskTO.STORAGE_HOST, destPool.getHostAddress());

        command.setDestDetails(details);

        EndPoint ep = selector.select(srcVolumeInfo, StorageAction.MIGRATEVOLUME);

        Answer answer;

        if (ep == null) {
            String errMsg = "No remote endpoint to send command to; check if host or SSVM is down";

            LOGGER.error(errMsg);

            answer = new Answer(command, false, errMsg);
        } else {
            answer = ep.sendMessage(command);
        }

        handleQualityOfServiceForVolumeMigration(destVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.NO_MIGRATION);

        if (answer == null || !answer.getResult()) {
            handleFailedVolumeMigration(srcVolumeInfo, destVolumeInfo, hostVO);

            throw new CloudRuntimeException("Failed to migrate volume with ID " + srcVolumeInfo.getId() + " to storage pool with ID " + destPool.getId());
        } else {
            handleSuccessfulVolumeMigration(srcVolumeInfo, destPool, (MigrateVolumeAnswer)answer);
        }
    }

    private void handleSuccessfulVolumeMigration(VolumeInfo srcVolumeInfo, StoragePool destPool, MigrateVolumeAnswer migrateVolumeAnswer) {
        VolumeVO volumeVO = _volumeDao.findById(srcVolumeInfo.getId());

        volumeVO.setPath(migrateVolumeAnswer.getVolumePath());

        String chainInfo = migrateVolumeAnswer.getVolumeChainInfo();

        if (chainInfo != null) {
            volumeVO.setChainInfo(chainInfo);
        }

        volumeVO.setPodId(destPool.getPodId());
        volumeVO.setPoolId(destPool.getId());
        volumeVO.setLastPoolId(srcVolumeInfo.getPoolId());

        _volumeDao.update(srcVolumeInfo.getId(), volumeVO);
    }

    private void handleFailedVolumeMigration(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo, HostVO hostVO) {
        try {
            _volumeService.revokeAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());
        }
        catch (Exception ex) {
            LOGGER.warn("Failed to revoke access to the volume with the following ID: " + destVolumeInfo.getId());
        }

        try {
            VolumeDetailVO volumeDetailVO = new VolumeDetailVO(destVolumeInfo.getId(), PrimaryDataStoreDriver.BASIC_DELETE_BY_FOLDER,
                    Boolean.TRUE.toString(), false);

            volumeDetailsDao.persist(volumeDetailVO);

            destVolumeInfo.getDataStore().getDriver().deleteAsync(destVolumeInfo.getDataStore(), destVolumeInfo, null);

            volumeDetailsDao.removeDetails(srcVolumeInfo.getId());
        }
        catch (Exception ex) {
            LOGGER.warn(ex.getMessage());
        }

        VolumeVO volumeVO = _volumeDao.findById(srcVolumeInfo.getId());

        volumeVO.setPoolId(srcVolumeInfo.getPoolId());
        volumeVO.setLastPoolId(srcVolumeInfo.getLastPoolId());
        volumeVO.setFolder(srcVolumeInfo.getFolder());
        volumeVO.set_iScsiName(srcVolumeInfo.get_iScsiName());

        _volumeDao.update(srcVolumeInfo.getId(), volumeVO);
    }

    private void handleVolumeMigrationForKVM(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo) {
        VirtualMachine vm = srcVolumeInfo.getAttachedVM();

        if (vm != null && vm.getState() != VirtualMachine.State.Stopped) {
            throw new CloudRuntimeException("Currently, if a volume to migrate from non-managed storage to managed storage on KVM is attached to " +
                    "a VM, the VM must be in the Stopped state.");
        }

        destVolumeInfo.getDataStore().getDriver().createAsync(destVolumeInfo.getDataStore(), destVolumeInfo, null);

        VolumeVO volumeVO = _volumeDao.findById(destVolumeInfo.getId());

        volumeVO.setPath(volumeVO.get_iScsiName());

        _volumeDao.update(volumeVO.getId(), volumeVO);

        destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId(), destVolumeInfo.getDataStore());

        long srcStoragePoolId = srcVolumeInfo.getPoolId();
        StoragePoolVO srcStoragePoolVO = _storagePoolDao.findById(srcStoragePoolId);

        HostVO hostVO;

        if (srcStoragePoolVO.getClusterId() != null) {
            hostVO = getHostInCluster(srcStoragePoolVO.getClusterId());
        }
        else {
            hostVO = getHost(destVolumeInfo.getDataCenterId(), HypervisorType.KVM, false);
        }

        // migrate the volume via the hypervisor
        migrateVolumeForKVM(srcVolumeInfo, destVolumeInfo, hostVO, "Unable to migrate the volume from non-managed storage to managed storage");

        volumeVO = _volumeDao.findById(destVolumeInfo.getId());

        volumeVO.setFormat(ImageFormat.QCOW2);

        _volumeDao.update(volumeVO.getId(), volumeVO);
    }

    /**
     * This function is responsible for copying a snapshot from managed storage to secondary storage. This is used in the following two cases:
     * 1) When creating a template from a snapshot
     * 2) When createSnapshot is called with location=SECONDARY
     *
     * @param snapshotInfo source snapshot
     * @param destData destination (can be template or snapshot)
     * @param callback callback for async
     */
    private void handleCopyAsyncToSecondaryStorage(SnapshotInfo snapshotInfo, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;
        CopyCmdAnswer copyCmdAnswer = null;
        boolean usingBackendSnapshot = false;

        try {
            snapshotInfo.processEvent(Event.CopyingRequested);

            HostVO hostVO = getHost(snapshotInfo);

            boolean needCache = needCacheStorage(snapshotInfo, destData);

            DataObject destOnStore = destData;

            if (needCache) {
                // creates an object in the DB for data to be cached
                Scope selectedScope = pickCacheScopeForCopy(snapshotInfo, destData);

                destOnStore = cacheMgr.getCacheObject(snapshotInfo, selectedScope);

                destOnStore.processEvent(Event.CreateOnlyRequested);
            }

            usingBackendSnapshot = usingBackendSnapshotFor(snapshotInfo);

            if (usingBackendSnapshot) {
                final boolean computeClusterSupportsVolumeClone;

                // only XenServer, VMware, and KVM are currently supported
                if (HypervisorType.XenServer.equals(snapshotInfo.getHypervisorType())) {
                    computeClusterSupportsVolumeClone = clusterDao.getSupportsResigning(hostVO.getClusterId());
                }
                else if (HypervisorType.VMware.equals(snapshotInfo.getHypervisorType()) || HypervisorType.KVM.equals(snapshotInfo.getHypervisorType())) {
                    computeClusterSupportsVolumeClone = true;
                }
                else {
                    throw new CloudRuntimeException("Unsupported hypervisor type");
                }

                if (!computeClusterSupportsVolumeClone) {
                    String noSupportForResignErrMsg = "Unable to locate an applicable host with which to perform a resignature operation : Cluster ID = " +
                            hostVO.getClusterId();

                    LOGGER.warn(noSupportForResignErrMsg);

                    throw new CloudRuntimeException(noSupportForResignErrMsg);
                }
            }

            String vmdk = null;
            String uuid = null;
            boolean keepGrantedAccess = false;

            DataStore srcDataStore = snapshotInfo.getDataStore();

            if (usingBackendSnapshot) {
                createVolumeFromSnapshot(snapshotInfo);

                if (HypervisorType.XenServer.equals(snapshotInfo.getHypervisorType()) || HypervisorType.VMware.equals(snapshotInfo.getHypervisorType())) {
                    keepGrantedAccess = HypervisorType.XenServer.equals(snapshotInfo.getHypervisorType());

                    Map<String, String> extraDetails = null;

                    if (HypervisorType.VMware.equals(snapshotInfo.getHypervisorType())) {
                        extraDetails = new HashMap<>();

                        String extraDetailsVmdk = getSnapshotProperty(snapshotInfo.getId(), DiskTO.VMDK);

                        extraDetails.put(DiskTO.VMDK, extraDetailsVmdk);
                        extraDetails.put(DiskTO.TEMPLATE_RESIGN, Boolean.TRUE.toString());
                    }

                    copyCmdAnswer = performResignature(snapshotInfo, hostVO, extraDetails, keepGrantedAccess);

                    // If using VMware, have the host rescan its software HBA if dynamic discovery is in use.
                    if (HypervisorType.VMware.equals(snapshotInfo.getHypervisorType())) {
                        String iqn = getSnapshotProperty(snapshotInfo.getId(), DiskTO.IQN);

                        disconnectHostFromVolume(hostVO, srcDataStore.getId(), iqn);
                    }

                    verifyCopyCmdAnswer(copyCmdAnswer, snapshotInfo);

                    vmdk = copyCmdAnswer.getNewData().getPath();
                    uuid = UUID.randomUUID().toString();
                }
            }

            int primaryStorageDownloadWait = StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value();
            CopyCommand copyCommand = new CopyCommand(snapshotInfo.getTO(), destOnStore.getTO(), primaryStorageDownloadWait,
                    VirtualMachineManager.ExecuteInSequence.value());

            try {
                if (!keepGrantedAccess) {
                    _volumeService.grantAccess(snapshotInfo, hostVO, srcDataStore);
                }

                Map<String, String> srcDetails = getSnapshotDetails(snapshotInfo);

                if (isForVMware(destData)) {
                    srcDetails.put(DiskTO.VMDK, vmdk);
                    srcDetails.put(DiskTO.UUID, uuid);

                    if (destData instanceof TemplateInfo) {
                        VMTemplateVO templateDataStoreVO = _vmTemplateDao.findById(destData.getId());

                        templateDataStoreVO.setUniqueName(uuid);

                        _vmTemplateDao.update(destData.getId(), templateDataStoreVO);
                    }
                }

                copyCommand.setOptions(srcDetails);

                copyCmdAnswer = (CopyCmdAnswer)agentManager.send(hostVO.getId(), copyCommand);

                if (!copyCmdAnswer.getResult()) {
                    errMsg = copyCmdAnswer.getDetails();

                    LOGGER.warn(errMsg);

                    throw new CloudRuntimeException(errMsg);
                }

                if (needCache) {
                    // If cached storage was needed (in case of object store as secondary
                    // storage), at this point, the data has been copied from the primary
                    // to the NFS cache by the hypervisor. We now invoke another copy
                    // command to copy this data from cache to secondary storage. We
                    // then clean up the cache.

                    destOnStore.processEvent(Event.OperationSuccessed, copyCmdAnswer);

                    CopyCommand cmd = new CopyCommand(destOnStore.getTO(), destData.getTO(), primaryStorageDownloadWait,
                            VirtualMachineManager.ExecuteInSequence.value());
                    EndPoint ep = selector.select(destOnStore, destData);

                    if (ep == null) {
                        errMsg = "No remote endpoint to send command, check if host or SSVM is down";

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

                throw new CloudRuntimeException(msg + ex.getMessage(), ex);
            } finally {
                _volumeService.revokeAccess(snapshotInfo, hostVO, srcDataStore);

                // If using VMware, have the host rescan its software HBA if dynamic discovery is in use.
                if (HypervisorType.VMware.equals(snapshotInfo.getHypervisorType())) {
                    String iqn = getSnapshotProperty(snapshotInfo.getId(), DiskTO.IQN);

                    disconnectHostFromVolume(hostVO, srcDataStore.getId(), iqn);
                }

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
        }
        catch (Exception ex) {
            errMsg = ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            if (usingBackendSnapshot) {
                deleteVolumeFromSnapshot(snapshotInfo);
            }

            if (copyCmdAnswer == null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void handleCreateNonManagedVolumeFromManagedSnapshot(SnapshotInfo snapshotInfo, VolumeInfo volumeInfo,
                                                                 AsyncCompletionCallback<CopyCommandResult> callback) {
        if (!HypervisorType.XenServer.equals(snapshotInfo.getHypervisorType())) {
            String errMsg = "Creating a volume on non-managed storage from a snapshot on managed storage is currently only supported with XenServer.";

            handleError(errMsg, callback);
        }

        long volumeStoragePoolId = volumeInfo.getDataStore().getId();
        StoragePoolVO volumeStoragePoolVO = _storagePoolDao.findById(volumeStoragePoolId);

        if (volumeStoragePoolVO.getClusterId() == null) {
            String errMsg = "To create a non-managed volume from a managed snapshot, the destination storage pool must be cluster scoped.";

            handleError(errMsg, callback);
        }

        String errMsg = null;
        CopyCmdAnswer copyCmdAnswer = null;

        boolean usingBackendSnapshot = false;

        try {
            snapshotInfo.processEvent(Event.CopyingRequested);

            usingBackendSnapshot = usingBackendSnapshotFor(snapshotInfo);

            if (usingBackendSnapshot) {
                boolean computeClusterSupportsVolumeClone = clusterDao.getSupportsResigning(volumeStoragePoolVO.getClusterId());

                if (!computeClusterSupportsVolumeClone) {
                    String noSupportForResignErrMsg = "Unable to locate an applicable host with which to perform a resignature operation : Cluster ID = " +
                            volumeStoragePoolVO.getClusterId();

                    LOGGER.warn(noSupportForResignErrMsg);

                    throw new CloudRuntimeException(noSupportForResignErrMsg);
                }

                createVolumeFromSnapshot(snapshotInfo);

                HostVO hostVO = getHost(snapshotInfo.getDataCenterId(), HypervisorType.XenServer, true);

                copyCmdAnswer = performResignature(snapshotInfo, hostVO, null, true);

                verifyCopyCmdAnswer(copyCmdAnswer, snapshotInfo);
            }

            int primaryStorageDownloadWait = StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value();

            CopyCommand copyCommand = new CopyCommand(snapshotInfo.getTO(), volumeInfo.getTO(), primaryStorageDownloadWait,
                    VirtualMachineManager.ExecuteInSequence.value());

            HostVO hostVO = getHostInCluster(volumeStoragePoolVO.getClusterId());

            if (!usingBackendSnapshot) {
                long snapshotStoragePoolId = snapshotInfo.getDataStore().getId();
                DataStore snapshotDataStore = dataStoreMgr.getDataStore(snapshotStoragePoolId, DataStoreRole.Primary);

                _volumeService.grantAccess(snapshotInfo, hostVO, snapshotDataStore);
            }

            Map<String, String> srcDetails = getSnapshotDetails(snapshotInfo);

            copyCommand.setOptions(srcDetails);

            copyCmdAnswer = (CopyCmdAnswer)agentManager.send(hostVO.getId(), copyCommand);

            if (!copyCmdAnswer.getResult()) {
                errMsg = copyCmdAnswer.getDetails();

                LOGGER.warn(errMsg);

                throw new CloudRuntimeException(errMsg);
            }
        }
        catch (Exception ex) {
            errMsg = "Copy operation failed in 'StorageSystemDataMotionStrategy.handleCreateNonManagedVolumeFromManagedSnapshot': " + ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            try {
                HostVO hostVO = getHostInCluster(volumeStoragePoolVO.getClusterId());

                long snapshotStoragePoolId = snapshotInfo.getDataStore().getId();
                DataStore snapshotDataStore = dataStoreMgr.getDataStore(snapshotStoragePoolId, DataStoreRole.Primary);

                _volumeService.revokeAccess(snapshotInfo, hostVO, snapshotDataStore);
            }
            catch (Exception e) {
                LOGGER.debug("Failed to revoke access from dest volume", e);
            }

            if (usingBackendSnapshot) {
                deleteVolumeFromSnapshot(snapshotInfo);
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

            if (copyCmdAnswer == null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void verifyCopyCmdAnswer(CopyCmdAnswer copyCmdAnswer, DataObject dataObject) {
        if (copyCmdAnswer == null) {
            throw new CloudRuntimeException("Unable to create a volume from a " + dataObject.getType().toString().toLowerCase() + " (copyCmdAnswer == null)");
        }

        if (copyCmdAnswer.getResult()) {
            return;
        }

        String details = copyCmdAnswer.getDetails();

        if (StringUtils.isEmpty(details)) {
            throw new CloudRuntimeException("Unable to create a volume from a " + dataObject.getType().toString().toLowerCase() + " (no error details specified)");
        }

        throw new CloudRuntimeException(details);
    }

    /**
     * Creates a managed volume on the storage from a snapshot that resides on the secondary storage (archived snapshot).
     * @param snapshotInfo snapshot on secondary
     * @param volumeInfo volume to be created on the storage
     * @param callback for async
     */
    private void handleCreateManagedVolumeFromNonManagedSnapshot(SnapshotInfo snapshotInfo, VolumeInfo volumeInfo,
                                                                 AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;
        CopyCmdAnswer copyCmdAnswer = null;

        try {
            // at this point, the snapshotInfo and volumeInfo should have the same disk offering ID (so either one should be OK to get a DiskOfferingVO instance)
            DiskOfferingVO diskOffering = _diskOfferingDao.findByIdIncludingRemoved(volumeInfo.getDiskOfferingId());
            SnapshotVO snapshot = _snapshotDao.findById(snapshotInfo.getId());

            // update the volume's hv_ss_reserve (hypervisor snapshot reserve) from a disk offering (used for managed storage)
            _volumeService.updateHypervisorSnapshotReserveForVolume(diskOffering, volumeInfo.getId(), snapshot.getHypervisorType());

            HostVO hostVO;

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

            handleQualityOfServiceForVolumeMigration(volumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.MIGRATION);

            hostVO = getHost(snapshotInfo.getDataCenterId(), snapshotInfo.getHypervisorType(), false);

            // copy the volume from secondary via the hypervisor
            if (HypervisorType.XenServer.equals(snapshotInfo.getHypervisorType())) {
                copyCmdAnswer = performCopyOfVdi(volumeInfo, snapshotInfo, hostVO);
            }
            else {
                copyCmdAnswer = copyImageToVolume(snapshotInfo, volumeInfo, hostVO);
            }

            if (copyCmdAnswer == null || !copyCmdAnswer.getResult()) {
                if (copyCmdAnswer != null && !StringUtils.isEmpty(copyCmdAnswer.getDetails())) {
                    throw new CloudRuntimeException(copyCmdAnswer.getDetails());
                }
                else {
                    throw new CloudRuntimeException("Unable to create volume from snapshot");
                }
            }
        }
        catch (Exception ex) {
            errMsg = "Copy operation failed in 'StorageSystemDataMotionStrategy.handleCreateManagedVolumeFromNonManagedSnapshot': " + ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            handleQualityOfServiceForVolumeMigration(volumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.NO_MIGRATION);

            if (copyCmdAnswer == null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    /**
     * Clones a template present on the storage to a new volume and resignatures it.
     *
     * @param templateInfo source template
     * @param volumeInfo destination ROOT volume
     * @param callback for async
     */
    private void handleCreateVolumeFromTemplateBothOnStorageSystem(TemplateInfo templateInfo, VolumeInfo volumeInfo, AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;
        CopyCmdAnswer copyCmdAnswer = null;

        try {
            Preconditions.checkArgument(templateInfo != null, "Passing 'null' to templateInfo of " +
                            "handleCreateVolumeFromTemplateBothOnStorageSystem is not supported.");
            Preconditions.checkArgument(volumeInfo != null, "Passing 'null' to volumeInfo of " +
                            "handleCreateVolumeFromTemplateBothOnStorageSystem is not supported.");

            verifyFormat(templateInfo.getFormat());

            HostVO hostVO = null;

            final boolean computeClusterSupportsVolumeClone;

            // only XenServer, VMware, and KVM are currently supported
            // Leave host equal to null for KVM since we don't need to perform a resignature when using that hypervisor type.
            if (volumeInfo.getFormat() == ImageFormat.VHD) {
                hostVO = getHost(volumeInfo.getDataCenterId(), HypervisorType.XenServer, true);

                if (hostVO == null) {
                    throw new CloudRuntimeException("Unable to locate a host capable of resigning in the zone with the following ID: " +
                            volumeInfo.getDataCenterId());
                }

                computeClusterSupportsVolumeClone = clusterDao.getSupportsResigning(hostVO.getClusterId());

                if (!computeClusterSupportsVolumeClone) {
                    String noSupportForResignErrMsg = "Unable to locate an applicable host with which to perform a resignature operation : Cluster ID = " +
                            hostVO.getClusterId();

                    LOGGER.warn(noSupportForResignErrMsg);

                    throw new CloudRuntimeException(noSupportForResignErrMsg);
                }
            }
            else if (volumeInfo.getFormat() == ImageFormat.OVA) {
                // all VMware hosts support resigning
                hostVO = getHost(volumeInfo.getDataCenterId(), HypervisorType.VMware, false);

                if (hostVO == null) {
                    throw new CloudRuntimeException("Unable to locate a host capable of resigning in the zone with the following ID: " +
                            volumeInfo.getDataCenterId());
                }
            }

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

            if (hostVO != null) {
                Map<String, String> extraDetails = null;

                if (HypervisorType.VMware.equals(templateInfo.getHypervisorType())) {
                    extraDetails = new HashMap<>();

                    String extraDetailsVmdk = templateInfo.getUniqueName() + ".vmdk";

                    extraDetails.put(DiskTO.VMDK, extraDetailsVmdk);
                    extraDetails.put(DiskTO.EXPAND_DATASTORE, Boolean.TRUE.toString());
                }

                copyCmdAnswer = performResignature(volumeInfo, hostVO, extraDetails);

                verifyCopyCmdAnswer(copyCmdAnswer, templateInfo);

                // If using VMware, have the host rescan its software HBA if dynamic discovery is in use.
                if (HypervisorType.VMware.equals(templateInfo.getHypervisorType())) {
                    disconnectHostFromVolume(hostVO, volumeInfo.getPoolId(), volumeInfo.get_iScsiName());
                }
            }
            else {
                VolumeObjectTO newVolume = new VolumeObjectTO();

                newVolume.setSize(volumeInfo.getSize());
                newVolume.setPath(volumeInfo.getPath());
                newVolume.setFormat(volumeInfo.getFormat());

                copyCmdAnswer = new CopyCmdAnswer(newVolume);
            }
        } catch (Exception ex) {
            try {
                volumeInfo.getDataStore().getDriver().deleteAsync(volumeInfo.getDataStore(), volumeInfo, null);
            }
            catch (Exception exc) {
                LOGGER.warn("Failed to delete volume", exc);
            }

            if (templateInfo != null) {
                errMsg = "Create volume from template (ID = " + templateInfo.getId() + ") failed: " + ex.getMessage();
            }
            else {
                errMsg = "Create volume from template failed: " + ex.getMessage();
            }

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            if (copyCmdAnswer == null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void handleCreateManagedVolumeFromManagedSnapshot(SnapshotInfo snapshotInfo, VolumeInfo volumeInfo,
                                                              AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;
        CopyCmdAnswer copyCmdAnswer = null;

        boolean useCloning = true;

        try {
            verifyFormat(snapshotInfo);

            HostVO hostVO = getHost(snapshotInfo);

            boolean usingBackendSnapshot = usingBackendSnapshotFor(snapshotInfo);
            boolean computeClusterSupportsVolumeClone = true;

            if (HypervisorType.XenServer.equals(snapshotInfo.getHypervisorType())) {
                computeClusterSupportsVolumeClone = clusterDao.getSupportsResigning(hostVO.getClusterId());

                if (usingBackendSnapshot && !computeClusterSupportsVolumeClone) {
                    String noSupportForResignErrMsg = "Unable to locate an applicable host with which to perform a resignature operation : Cluster ID = " +
                            hostVO.getClusterId();

                    LOGGER.warn(noSupportForResignErrMsg);

                    throw new CloudRuntimeException(noSupportForResignErrMsg);
                }
            }

            boolean canStorageSystemCreateVolumeFromVolume = canStorageSystemCreateVolumeFromVolume(snapshotInfo.getDataStore().getId());

            useCloning = usingBackendSnapshot || (canStorageSystemCreateVolumeFromVolume && computeClusterSupportsVolumeClone);

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

            if (HypervisorType.XenServer.equals(snapshotInfo.getHypervisorType()) || HypervisorType.VMware.equals(snapshotInfo.getHypervisorType())) {
                if (useCloning) {
                    Map<String, String> extraDetails = null;

                    if (HypervisorType.VMware.equals(snapshotInfo.getHypervisorType())) {
                        extraDetails = new HashMap<>();

                        String extraDetailsVmdk = getSnapshotProperty(snapshotInfo.getId(), DiskTO.VMDK);

                        extraDetails.put(DiskTO.VMDK, extraDetailsVmdk);
                    }

                    copyCmdAnswer = performResignature(volumeInfo, hostVO, extraDetails);

                    // If using VMware, have the host rescan its software HBA if dynamic discovery is in use.
                    if (HypervisorType.VMware.equals(snapshotInfo.getHypervisorType())) {
                        disconnectHostFromVolume(hostVO, volumeInfo.getPoolId(), volumeInfo.get_iScsiName());
                    }
                } else {
                    // asking for a XenServer host here so we don't always prefer to use XenServer hosts that support resigning
                    // even when we don't need those hosts to do this kind of copy work
                    hostVO = getHost(snapshotInfo.getDataCenterId(), snapshotInfo.getHypervisorType(), false);

                    handleQualityOfServiceForVolumeMigration(volumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.MIGRATION);

                    copyCmdAnswer = performCopyOfVdi(volumeInfo, snapshotInfo, hostVO);
                }

                verifyCopyCmdAnswer(copyCmdAnswer, snapshotInfo);
            }
            else if (HypervisorType.KVM.equals(snapshotInfo.getHypervisorType())) {
                VolumeObjectTO newVolume = new VolumeObjectTO();

                newVolume.setSize(volumeInfo.getSize());
                newVolume.setPath(volumeInfo.get_iScsiName());
                newVolume.setFormat(volumeInfo.getFormat());

                copyCmdAnswer = new CopyCmdAnswer(newVolume);
            }
            else {
                throw new CloudRuntimeException("Unsupported hypervisor type");
            }
        }
        catch (Exception ex) {
            errMsg = "Copy operation failed in 'StorageSystemDataMotionStrategy.handleCreateManagedVolumeFromManagedSnapshot': " +
                    ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            if (useCloning) {
                handleQualityOfServiceForVolumeMigration(volumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.NO_MIGRATION);
            }

            if (copyCmdAnswer == null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void handleCreateVolumeFromVolumeOnSecondaryStorage(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo,
                                                                long dataCenterId, HypervisorType hypervisorType,
                                                                AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;
        CopyCmdAnswer copyCmdAnswer = null;

        try {
            // create a volume on the storage
            destVolumeInfo.getDataStore().getDriver().createAsync(destVolumeInfo.getDataStore(), destVolumeInfo, null);

            destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId(), destVolumeInfo.getDataStore());

            HostVO hostVO = getHost(dataCenterId, hypervisorType, false);

            handleQualityOfServiceForVolumeMigration(destVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.MIGRATION);

            // copy the volume from secondary via the hypervisor
            copyCmdAnswer = copyImageToVolume(srcVolumeInfo, destVolumeInfo, hostVO);

            if (copyCmdAnswer == null || !copyCmdAnswer.getResult()) {
                if (copyCmdAnswer != null && !StringUtils.isEmpty(copyCmdAnswer.getDetails())) {
                    throw new CloudRuntimeException(copyCmdAnswer.getDetails());
                }
                else {
                    throw new CloudRuntimeException("Unable to create volume from volume");
                }
            }
        }
        catch (Exception ex) {
            errMsg = "Copy operation failed in 'StorageSystemDataMotionStrategy.handleCreateVolumeFromVolumeOnSecondaryStorage': " +
                    ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            handleQualityOfServiceForVolumeMigration(destVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.NO_MIGRATION);

            if (copyCmdAnswer == null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private CopyCmdAnswer copyImageToVolume(DataObject srcDataObject, VolumeInfo destVolumeInfo, HostVO hostVO) {
        int primaryStorageDownloadWait = StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value();

        CopyCommand copyCommand = new CopyCommand(srcDataObject.getTO(), destVolumeInfo.getTO(), primaryStorageDownloadWait,
                VirtualMachineManager.ExecuteInSequence.value());

        CopyCmdAnswer copyCmdAnswer;

        try {
            _volumeService.grantAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());

            Map<String, String> destDetails = getVolumeDetails(destVolumeInfo);

            copyCommand.setOptions2(destDetails);

            copyCmdAnswer = (CopyCmdAnswer)agentManager.send(hostVO.getId(), copyCommand);
        }
        catch (CloudRuntimeException | AgentUnavailableException | OperationTimedoutException ex) {
            String msg = "Failed to copy image : ";

            LOGGER.warn(msg, ex);

            throw new CloudRuntimeException(msg + ex.getMessage(), ex);
        }
        finally {
            _volumeService.revokeAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());
        }

        VolumeObjectTO volumeObjectTO = (VolumeObjectTO)copyCmdAnswer.getNewData();

        volumeObjectTO.setFormat(ImageFormat.QCOW2);

        return copyCmdAnswer;
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
    private void createVolumeFromSnapshot(SnapshotInfo snapshotInfo) {
        SnapshotDetailsVO snapshotDetails = handleSnapshotDetails(snapshotInfo.getId(), "create");

        try {
            snapshotInfo.getDataStore().getDriver().createAsync(snapshotInfo.getDataStore(), snapshotInfo, null);
        }
        finally {
            _snapshotDetailsDao.remove(snapshotDetails.getId());
        }
    }

    /**
     * If the underlying storage system needed to create a volume from a snapshot for createVolumeFromSnapshot(SnapshotInfo), then
     * this is its opportunity to delete that temporary volume and restore properties in snapshot_details to the way they were before the
     * invocation of createVolumeFromSnapshot(SnapshotInfo).
     */
    private void deleteVolumeFromSnapshot(SnapshotInfo snapshotInfo) {
        SnapshotDetailsVO snapshotDetails = handleSnapshotDetails(snapshotInfo.getId(), "delete");

        try {
            snapshotInfo.getDataStore().getDriver().createAsync(snapshotInfo.getDataStore(), snapshotInfo, null);
        }
        finally {
            _snapshotDetailsDao.remove(snapshotDetails.getId());
        }
    }

    private void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo, PrimaryDataStoreDriver.QualityOfServiceState qualityOfServiceState) {
        try {
            ((PrimaryDataStoreDriver)volumeInfo.getDataStore().getDriver()).handleQualityOfServiceForVolumeMigration(volumeInfo, qualityOfServiceState);
        }
        catch (Exception ex) {
            LOGGER.warn(ex);
        }
    }

    private SnapshotDetailsVO handleSnapshotDetails(long csSnapshotId, String value) {
        String name = "tempVolume";

        _snapshotDetailsDao.removeDetail(csSnapshotId, name);

        SnapshotDetailsVO snapshotDetails = new SnapshotDetailsVO(csSnapshotId, name, value, false);

        return _snapshotDetailsDao.persist(snapshotDetails);
    }

    /**
     * Return expected MigrationOptions for a linked clone volume live storage migration
     */
    protected MigrationOptions createLinkedCloneMigrationOptions(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo, String srcVolumeBackingFile, String srcPoolUuid, Storage.StoragePoolType srcPoolType) {
        VMTemplateStoragePoolVO ref = templatePoolDao.findByPoolTemplate(destVolumeInfo.getPoolId(), srcVolumeInfo.getTemplateId());
        boolean updateBackingFileReference = ref == null;
        String backingFile = ref != null ? ref.getInstallPath() : srcVolumeBackingFile;
        return new MigrationOptions(srcPoolUuid, srcPoolType, backingFile, updateBackingFileReference);
    }

    /**
     * Return expected MigrationOptions for a full clone volume live storage migration
     */
    protected MigrationOptions createFullCloneMigrationOptions(VolumeInfo srcVolumeInfo, VirtualMachineTO vmTO, Host srcHost, String srcPoolUuid, Storage.StoragePoolType srcPoolType) {
        return new MigrationOptions(srcPoolUuid, srcPoolType, srcVolumeInfo.getPath());
    }

    /**
     * Prepare hosts for KVM live storage migration depending on volume type by setting MigrationOptions on destination volume:
     * - Linked clones (backing file on disk): Decide if template (backing file) should be copied to destination storage prior disk creation
     * - Full clones (no backing file): Take snapshot of the VM prior disk creation
     * Return this information
     */
    protected void setVolumeMigrationOptions(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo,
                                             VirtualMachineTO vmTO, Host srcHost, StoragePoolVO destStoragePool) {
        if (!destStoragePool.isManaged()) {
            String srcVolumeBackingFile = getVolumeBackingFile(srcVolumeInfo);

            String srcPoolUuid = srcVolumeInfo.getDataStore().getUuid();
            StoragePoolVO srcPool = _storagePoolDao.findById(srcVolumeInfo.getPoolId());
            Storage.StoragePoolType srcPoolType = srcPool.getPoolType();

            MigrationOptions migrationOptions;
            if (StringUtils.isNotBlank(srcVolumeBackingFile)) {
                migrationOptions = createLinkedCloneMigrationOptions(srcVolumeInfo, destVolumeInfo,
                        srcVolumeBackingFile, srcPoolUuid, srcPoolType);
            } else {
                migrationOptions = createFullCloneMigrationOptions(srcVolumeInfo, vmTO, srcHost, srcPoolUuid, srcPoolType);
            }
            migrationOptions.setTimeout(StorageManager.KvmStorageOnlineMigrationWait.value());
            destVolumeInfo.setMigrationOptions(migrationOptions);
        }
    }

    /**
     * For each disk to migrate:
     * <ul>
     *  <li>Create a volume on the target storage system.</li>
     *  <li>Make the newly created volume accessible to the target KVM host.</li>
     *  <li>Send a command to the target KVM host to connect to the newly created volume.</li>
     *  <li>Send a command to the source KVM host to migrate the VM and its storage.</li>
     * </ul>
     */
    @Override
    public void copyAsync(Map<VolumeInfo, DataStore> volumeDataStoreMap, VirtualMachineTO vmTO, Host srcHost, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;

        try {
            if (srcHost.getHypervisorType() != HypervisorType.KVM) {
                throw new CloudRuntimeException("Invalid hypervisor type (only KVM supported for this operation at the time being)");
            }

            verifyLiveMigrationForKVM(volumeDataStoreMap, destHost);

            VMInstanceVO vmInstance = _vmDao.findById(vmTO.getId());
            vmTO.setState(vmInstance.getState());
            List<MigrateDiskInfo> migrateDiskInfoList = new ArrayList<MigrateDiskInfo>();

            Map<String, MigrateCommand.MigrateDiskInfo> migrateStorage = new HashMap<>();
            Map<VolumeInfo, VolumeInfo> srcVolumeInfoToDestVolumeInfo = new HashMap<>();

            boolean managedStorageDestination = false;
            for (Map.Entry<VolumeInfo, DataStore> entry : volumeDataStoreMap.entrySet()) {
                VolumeInfo srcVolumeInfo = entry.getKey();
                DataStore destDataStore = entry.getValue();

                VolumeVO srcVolume = _volumeDao.findById(srcVolumeInfo.getId());
                StoragePoolVO destStoragePool = _storagePoolDao.findById(destDataStore.getId());
                StoragePoolVO sourceStoragePool = _storagePoolDao.findById(srcVolumeInfo.getPoolId());

                if (!shouldMigrateVolume(sourceStoragePool, destHost, destStoragePool)) {
                    continue;
                }

                copyTemplateToTargetFilesystemStorageIfNeeded(srcVolumeInfo, sourceStoragePool, destDataStore, destStoragePool, destHost);

                VolumeVO destVolume = duplicateVolumeOnAnotherStorage(srcVolume, destStoragePool);
                VolumeInfo destVolumeInfo = _volumeDataFactory.getVolume(destVolume.getId(), destDataStore);

                // move the volume from Allocated to Creating
                destVolumeInfo.processEvent(Event.MigrationCopyRequested);
                // move the volume from Creating to Ready
                destVolumeInfo.processEvent(Event.MigrationCopySucceeded);
                // move the volume from Ready to Migrating
                destVolumeInfo.processEvent(Event.MigrationRequested);

                setVolumeMigrationOptions(srcVolumeInfo, destVolumeInfo, vmTO, srcHost, destStoragePool);

                // create a volume on the destination storage
                destDataStore.getDriver().createAsync(destDataStore, destVolumeInfo, null);

                managedStorageDestination = destStoragePool.isManaged();
                String volumeIdentifier = managedStorageDestination ? destVolumeInfo.get_iScsiName() : destVolumeInfo.getUuid();

                destVolume = _volumeDao.findById(destVolume.getId());
                destVolume.setPath(volumeIdentifier);

                setVolumePath(destVolume);

                _volumeDao.update(destVolume.getId(), destVolume);

                postVolumeCreationActions(srcVolumeInfo, destVolumeInfo, vmTO, srcHost);

                destVolumeInfo = _volumeDataFactory.getVolume(destVolume.getId(), destDataStore);

                handleQualityOfServiceForVolumeMigration(destVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.MIGRATION);

                _volumeService.grantAccess(destVolumeInfo, destHost, destDataStore);

                String destPath = generateDestPath(destHost, destStoragePool, destVolumeInfo);

                MigrateCommand.MigrateDiskInfo migrateDiskInfo;

                boolean isNonManagedNfsToNfs = sourceStoragePool.getPoolType() == StoragePoolType.NetworkFilesystem
                        && destStoragePool.getPoolType() == StoragePoolType.NetworkFilesystem && !managedStorageDestination;
                if (isNonManagedNfsToNfs) {
                    migrateDiskInfo = new MigrateCommand.MigrateDiskInfo(srcVolumeInfo.getPath(),
                            MigrateCommand.MigrateDiskInfo.DiskType.FILE,
                            MigrateCommand.MigrateDiskInfo.DriverType.QCOW2,
                            MigrateCommand.MigrateDiskInfo.Source.FILE,
                            connectHostToVolume(destHost, destVolumeInfo.getPoolId(), volumeIdentifier));
                } else {
                    migrateDiskInfo = configureMigrateDiskInfo(srcVolumeInfo, destPath);
                    migrateDiskInfo.setSourceDiskOnStorageFileSystem(isStoragePoolTypeOfFile(sourceStoragePool));
                    migrateDiskInfoList.add(migrateDiskInfo);
                }

                migrateStorage.put(srcVolumeInfo.getPath(), migrateDiskInfo);

                srcVolumeInfoToDestVolumeInfo.put(srcVolumeInfo, destVolumeInfo);
            }

            PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(vmTO);

            try {
                Answer pfma = agentManager.send(destHost.getId(), pfmc);

                if (pfma == null || !pfma.getResult()) {
                    String details = pfma != null ? pfma.getDetails() : "null answer returned";
                    String msg = "Unable to prepare for migration due to the following: " + details;

                    throw new AgentUnavailableException(msg, destHost.getId());
                }
            }
            catch (final OperationTimedoutException e) {
                throw new AgentUnavailableException("Operation timed out", destHost.getId());
            }

            VMInstanceVO vm = _vmDao.findById(vmTO.getId());
            boolean isWindows = _guestOsCategoryDao.findById(_guestOsDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");

            boolean migrateNonSharedInc = isSourceAndDestinationPoolTypeOfNfs(volumeDataStoreMap);

            MigrateCommand migrateCommand = new MigrateCommand(vmTO.getName(), destHost.getPrivateIpAddress(), isWindows, vmTO, true);
            migrateCommand.setWait(StorageManager.KvmStorageOnlineMigrationWait.value());
            migrateCommand.setMigrateStorage(migrateStorage);
            migrateCommand.setMigrateDiskInfoList(migrateDiskInfoList);
            migrateCommand.setMigrateStorageManaged(managedStorageDestination);
            migrateCommand.setMigrateNonSharedInc(migrateNonSharedInc);

            boolean kvmAutoConvergence = StorageManager.KvmAutoConvergence.value();
            migrateCommand.setAutoConvergence(kvmAutoConvergence);

            MigrateAnswer migrateAnswer = (MigrateAnswer)agentManager.send(srcHost.getId(), migrateCommand);

            boolean success = migrateAnswer != null && migrateAnswer.getResult();

            handlePostMigration(success, srcVolumeInfoToDestVolumeInfo, vmTO, destHost);

            if (migrateAnswer == null) {
                throw new CloudRuntimeException("Unable to get an answer to the migrate command");
            }

            if (!migrateAnswer.getResult()) {
                errMsg = migrateAnswer.getDetails();

                throw new CloudRuntimeException(errMsg);
            }
        }
        catch (Exception ex) {
            errMsg = "Copy operation failed in 'StorageSystemDataMotionStrategy.copyAsync': " + ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            CopyCmdAnswer copyCmdAnswer = new CopyCmdAnswer(errMsg);

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    /**
     * Returns true if at least one of the entries on the map 'volumeDataStoreMap' has both source and destination storage pools of Network Filesystem (NFS).
     */
    protected boolean isSourceAndDestinationPoolTypeOfNfs(Map<VolumeInfo, DataStore> volumeDataStoreMap) {
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeDataStoreMap.entrySet()) {
            VolumeInfo srcVolumeInfo = entry.getKey();
            DataStore destDataStore = entry.getValue();

            StoragePoolVO destStoragePool = _storagePoolDao.findById(destDataStore.getId());
            StoragePoolVO sourceStoragePool = _storagePoolDao.findById(srcVolumeInfo.getPoolId());
            if (sourceStoragePool.getPoolType() == StoragePoolType.NetworkFilesystem && destStoragePool.getPoolType() == StoragePoolType.NetworkFilesystem) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true. This method was implemented considering the classes that extend this {@link StorageSystemDataMotionStrategy} and cannot migrate volumes from certain types of source storage pools and/or to a different kind of destiny storage pool.
     */
    protected boolean shouldMigrateVolume(StoragePoolVO sourceStoragePool, Host destHost, StoragePoolVO destStoragePool) {
        return true;
    }

    /**
     * Returns true if the storage pool type is {@link StoragePoolType.Filesystem}.
     */
    protected boolean isStoragePoolTypeOfFile(StoragePoolVO sourceStoragePool) {
        return sourceStoragePool.getPoolType() == StoragePoolType.Filesystem;
    }

    /**
     * Returns the iScsi connection path.
     */
    protected String generateDestPath(Host destHost, StoragePoolVO destStoragePool, VolumeInfo destVolumeInfo) {
        return connectHostToVolume(destHost, destVolumeInfo.getPoolId(), destVolumeInfo.get_iScsiName());
    }

    /**
     * Configures a {@link MigrateDiskInfo} object with disk type of BLOCK, Driver type RAW and Source DEV
     */
    protected MigrateCommand.MigrateDiskInfo configureMigrateDiskInfo(VolumeInfo srcVolumeInfo, String destPath) {
        return new MigrateCommand.MigrateDiskInfo(srcVolumeInfo.getPath(),
                MigrateCommand.MigrateDiskInfo.DiskType.BLOCK,
                MigrateCommand.MigrateDiskInfo.DriverType.RAW,
                MigrateCommand.MigrateDiskInfo.Source.DEV, destPath);
    }

    /**
     * Sets the volume path as the iScsi name in case of a configured iScsi.
     */
    protected void setVolumePath(VolumeVO volume) {
        volume.setPath(volume.get_iScsiName());
    }

    /**
     * For this strategy it is not necessary to copy the template before migrating the VM.
     * However, classes that extend this one may need to copy the template to the target storage pool before migrating the VM.
     */
    protected void copyTemplateToTargetFilesystemStorageIfNeeded(VolumeInfo srcVolumeInfo, StoragePool srcStoragePool, DataStore destDataStore, StoragePool destStoragePool,
            Host destHost) {
        // This method is used by classes that extend this one
    }

    /*
     * Return backing file for volume (if any), only for KVM volumes
     */
    private String getVolumeBackingFile(VolumeInfo srcVolumeInfo) {
        if (srcVolumeInfo.getHypervisorType() == HypervisorType.KVM &&
                srcVolumeInfo.getTemplateId() != null && srcVolumeInfo.getPoolId() != null) {
            VMTemplateVO template = _vmTemplateDao.findById(srcVolumeInfo.getTemplateId());
            if (template.getFormat() != null && template.getFormat() != Storage.ImageFormat.ISO) {
                VMTemplateStoragePoolVO ref = templatePoolDao.findByPoolTemplate(srcVolumeInfo.getPoolId(), srcVolumeInfo.getTemplateId());
                return ref != null ? ref.getInstallPath() : null;
            }
        }
        return null;
    }

    private void handlePostMigration(boolean success, Map<VolumeInfo, VolumeInfo> srcVolumeInfoToDestVolumeInfo, VirtualMachineTO vmTO, Host destHost) {
        if (!success) {
            try {
                PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(vmTO);

                pfmc.setRollback(true);

                Answer pfma = agentManager.send(destHost.getId(), pfmc);

                if (pfma == null || !pfma.getResult()) {
                    String details = pfma != null ? pfma.getDetails() : "null answer returned";
                    String msg = "Unable to rollback prepare for migration due to the following: " + details;

                    throw new AgentUnavailableException(msg, destHost.getId());
                }
            }
            catch (Exception e) {
                LOGGER.debug("Failed to disconnect one or more (original) dest volumes", e);
            }
        }

        for (Map.Entry<VolumeInfo, VolumeInfo> entry : srcVolumeInfoToDestVolumeInfo.entrySet()) {
            VolumeInfo srcVolumeInfo = entry.getKey();
            VolumeInfo destVolumeInfo = entry.getValue();

            handleQualityOfServiceForVolumeMigration(destVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.NO_MIGRATION);

            if (success) {
                srcVolumeInfo.processEvent(Event.OperationSuccessed);
                destVolumeInfo.processEvent(Event.OperationSuccessed);

                _volumeDao.updateUuid(srcVolumeInfo.getId(), destVolumeInfo.getId());

                VolumeVO volumeVO = _volumeDao.findById(destVolumeInfo.getId());

                volumeVO.setFormat(ImageFormat.QCOW2);

                _volumeDao.update(volumeVO.getId(), volumeVO);

                try {
                    _volumeService.destroyVolume(srcVolumeInfo.getId());

                    srcVolumeInfo = _volumeDataFactory.getVolume(srcVolumeInfo.getId());

                    AsyncCallFuture<VolumeApiResult> destroyFuture = _volumeService.expungeVolumeAsync(srcVolumeInfo);

                    if (destroyFuture.get().isFailed()) {
                        LOGGER.debug("Failed to clean up source volume on storage");
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to clean up source volume on storage", e);
                }

                // Update the volume ID for snapshots on secondary storage
                if (!_snapshotDao.listByVolumeId(srcVolumeInfo.getId()).isEmpty()) {
                    _snapshotDao.updateVolumeIds(srcVolumeInfo.getId(), destVolumeInfo.getId());
                    _snapshotDataStoreDao.updateVolumeIds(srcVolumeInfo.getId(), destVolumeInfo.getId());
                }
            }
            else {
                try {
                    disconnectHostFromVolume(destHost, destVolumeInfo.getPoolId(), destVolumeInfo.get_iScsiName());
                }
                catch (Exception e) {
                    LOGGER.debug("Failed to disconnect (new) dest volume", e);
                }

                try {
                    _volumeService.revokeAccess(destVolumeInfo, destHost, destVolumeInfo.getDataStore());
                }
                catch (Exception e) {
                    LOGGER.debug("Failed to revoke access from dest volume", e);
                }

                destVolumeInfo.processEvent(Event.OperationFailed);
                srcVolumeInfo.processEvent(Event.OperationFailed);

                try {
                    _volumeService.destroyVolume(destVolumeInfo.getId());

                    destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId());

                    AsyncCallFuture<VolumeApiResult> destroyFuture = _volumeService.expungeVolumeAsync(destVolumeInfo);

                    if (destroyFuture.get().isFailed()) {
                        LOGGER.debug("Failed to clean up dest volume on storage");
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to clean up dest volume on storage", e);
                }
            }
        }
    }

    private VolumeVO duplicateVolumeOnAnotherStorage(Volume volume, StoragePoolVO storagePoolVO) {
        Long lastPoolId = volume.getPoolId();

        VolumeVO newVol = new VolumeVO(volume);

        newVol.setInstanceId(null);
        newVol.setChainInfo(null);
        newVol.setPath(null);
        newVol.setFolder(null);
        newVol.setPodId(storagePoolVO.getPodId());
        newVol.setPoolId(storagePoolVO.getId());
        newVol.setLastPoolId(lastPoolId);

        return _volumeDao.persist(newVol);
    }

    protected String connectHostToVolume(Host host, long storagePoolId, String iqn) {
        ModifyTargetsCommand modifyTargetsCommand = getModifyTargetsCommand(storagePoolId, iqn, true);

        return sendModifyTargetsCommand(modifyTargetsCommand, host.getId()).get(0);
    }

    private void disconnectHostFromVolume(Host host, long storagePoolId, String iqn) {
        ModifyTargetsCommand modifyTargetsCommand = getModifyTargetsCommand(storagePoolId, iqn, false);

        sendModifyTargetsCommand(modifyTargetsCommand, host.getId());
    }

    private ModifyTargetsCommand getModifyTargetsCommand(long storagePoolId, String iqn, boolean add) {
        StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

        Map<String, String> details = new HashMap<>();

        details.put(ModifyTargetsCommand.IQN, iqn);
        details.put(ModifyTargetsCommand.STORAGE_TYPE, storagePool.getPoolType().name());
        details.put(ModifyTargetsCommand.STORAGE_UUID, storagePool.getUuid());
        details.put(ModifyTargetsCommand.STORAGE_HOST, storagePool.getHostAddress());
        details.put(ModifyTargetsCommand.STORAGE_PORT, String.valueOf(storagePool.getPort()));

        ModifyTargetsCommand cmd = new ModifyTargetsCommand();

        List<Map<String, String>> targets = new ArrayList<>();

        targets.add(details);

        cmd.setTargets(targets);
        cmd.setApplyToAllHostsInCluster(true);
        cmd.setAdd(add);
        cmd.setTargetTypeToRemove(ModifyTargetsCommand.TargetTypeToRemove.DYNAMIC);

        return cmd;
    }

    private List<String> sendModifyTargetsCommand(ModifyTargetsCommand cmd, long hostId) {
        ModifyTargetsAnswer modifyTargetsAnswer = (ModifyTargetsAnswer)agentManager.easySend(hostId, cmd);

        if (modifyTargetsAnswer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify targets command");
        }

        if (!modifyTargetsAnswer.getResult()) {
            String msg = "Unable to modify targets on the following host: " + hostId;

            throw new CloudRuntimeException(msg);
        }

        return modifyTargetsAnswer.getConnectedPaths();
    }

    /**
     * Update reference on template_spool_ref table of copied template to destination storage
     */
    protected void updateCopiedTemplateReference(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo) {
        VMTemplateStoragePoolVO ref = templatePoolDao.findByPoolTemplate(srcVolumeInfo.getPoolId(), srcVolumeInfo.getTemplateId());
        VMTemplateStoragePoolVO newRef = new VMTemplateStoragePoolVO(destVolumeInfo.getPoolId(), ref.getTemplateId());
        newRef.setDownloadPercent(100);
        newRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        newRef.setState(ObjectInDataStoreStateMachine.State.Ready);
        newRef.setTemplateSize(ref.getTemplateSize());
        newRef.setLocalDownloadPath(ref.getLocalDownloadPath());
        newRef.setInstallPath(ref.getInstallPath());
        templatePoolDao.persist(newRef);
    }

    /**
     * Handle post destination volume creation actions depending on the migrating volume type: full clone or linked clone
     */
    protected void postVolumeCreationActions(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo, VirtualMachineTO vmTO, Host srcHost) {
        MigrationOptions migrationOptions = destVolumeInfo.getMigrationOptions();
        if (migrationOptions != null) {
            if (migrationOptions.getType() == MigrationOptions.Type.LinkedClone && migrationOptions.isCopySrcTemplate()) {
                updateCopiedTemplateReference(srcVolumeInfo, destVolumeInfo);
            }
        }
    }

    /**
    * At a high level: The source storage cannot be managed and
    *                  the destination storages can be all managed or all not managed, not mixed.
    */
    protected void verifyLiveMigrationForKVM(Map<VolumeInfo, DataStore> volumeDataStoreMap, Host destHost) {
        Boolean storageTypeConsistency = null;
        Map<String, Storage.StoragePoolType> sourcePools = new HashMap<>();
        for (Map.Entry<VolumeInfo, DataStore> entry : volumeDataStoreMap.entrySet()) {
            VolumeInfo volumeInfo = entry.getKey();

            Long storagePoolId = volumeInfo.getPoolId();
            StoragePoolVO srcStoragePoolVO = _storagePoolDao.findById(storagePoolId);

            if (srcStoragePoolVO == null) {
                throw new CloudRuntimeException("Volume with ID " + volumeInfo.getId() + " is not associated with a storage pool.");
            }

            if (srcStoragePoolVO.isManaged()) {
                throw new CloudRuntimeException("Migrating a volume online with KVM from managed storage is not currently supported.");
            }

            DataStore dataStore = entry.getValue();
            StoragePoolVO destStoragePoolVO = _storagePoolDao.findById(dataStore.getId());

            if (destStoragePoolVO == null) {
                throw new CloudRuntimeException("Destination storage pool with ID " + dataStore.getId() + " was not located.");
            }

            if (storageTypeConsistency == null) {
                storageTypeConsistency = destStoragePoolVO.isManaged();
            } else if (storageTypeConsistency != destStoragePoolVO.isManaged()) {
                throw new CloudRuntimeException("Destination storage pools must be either all managed or all not managed");
            }

            if (!destStoragePoolVO.isManaged() && destStoragePoolVO.getPoolType() == StoragePoolType.NetworkFilesystem) {
                if (destStoragePoolVO.getScope() != ScopeType.CLUSTER) {
                    throw new CloudRuntimeException("KVM live storage migrations currently support cluster-wide " +
                            "not managed NFS destination storage");
                }
                if (!sourcePools.containsKey(srcStoragePoolVO.getUuid())) {
                    sourcePools.put(srcStoragePoolVO.getUuid(), srcStoragePoolVO.getPoolType());
                }
            }
        }
        verifyDestinationStorage(sourcePools, destHost);
    }

    /**
     * Perform storage validation on destination host for KVM live storage migrations.
     * Validate that volume source storage pools are mounted on the destination host prior the migration
     * @throws CloudRuntimeException if any source storage pool is not mounted on the destination host
     */
    private void verifyDestinationStorage(Map<String, Storage.StoragePoolType> sourcePools, Host destHost) {
        if (MapUtils.isNotEmpty(sourcePools)) {
            LOGGER.debug("Verifying source pools are already available on destination host " + destHost.getUuid());
            CheckStorageAvailabilityCommand cmd = new CheckStorageAvailabilityCommand(sourcePools);
            try {
                Answer answer = agentManager.send(destHost.getId(), cmd);
                if (answer == null || !answer.getResult()) {
                    throw new CloudRuntimeException("Storage verification failed on host "
                            + destHost.getUuid() +": " + answer.getDetails());
                }
            } catch (AgentUnavailableException | OperationTimedoutException e) {
                e.printStackTrace();
                throw new CloudRuntimeException("Cannot perform storage verification on host " + destHost.getUuid() +
                        "due to: " + e.getMessage());
            }
        }
    }

    private boolean canStorageSystemCreateVolumeFromVolume(long storagePoolId) {
        return storageSystemSupportsCapability(storagePoolId, DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString());
    }

    private boolean canStorageSystemCreateVolumeFromSnapshot(long storagePoolId) {
        return storageSystemSupportsCapability(storagePoolId, DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_SNAPSHOT.toString());
    }

    private boolean storageSystemSupportsCapability(long storagePoolId, String capability) {
        boolean supportsCapability = false;

        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Map<String, String> mapCapabilities = dataStore.getDriver().getCapabilities();

        if (mapCapabilities != null) {
            String value = mapCapabilities.get(capability);

            supportsCapability = Boolean.valueOf(value);
        }

        return supportsCapability;
    }

    private String getVolumeProperty(long volumeId, String property) {
        VolumeDetailVO volumeDetails = volumeDetailsDao.findDetail(volumeId, property);

        if (volumeDetails != null) {
            return volumeDetails.getValue();
        }

        return null;
    }

    private String getSnapshotProperty(long snapshotId, String property) {
        SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(snapshotId, property);

        if (snapshotDetails != null) {
            return snapshotDetails.getValue();
        }

        return null;
    }

    private void handleCreateTemplateFromManagedVolume(VolumeInfo volumeInfo, TemplateInfo templateInfo, AsyncCompletionCallback<CopyCommandResult> callback) {
        boolean srcVolumeDetached = volumeInfo.getAttachedVM() == null;

        String errMsg = null;
        CopyCmdAnswer copyCmdAnswer = null;

        try {
            if (!ImageFormat.QCOW2.equals(volumeInfo.getFormat())) {
                throw new CloudRuntimeException("When using managed storage, you can only create a template from a volume on KVM currently.");
            }

            volumeInfo.processEvent(Event.MigrationRequested);

            HostVO hostVO = getHost(volumeInfo.getDataCenterId(), HypervisorType.KVM, false);
            DataStore srcDataStore = volumeInfo.getDataStore();

            int primaryStorageDownloadWait = StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value();

            CopyCommand copyCommand = new CopyCommand(volumeInfo.getTO(), templateInfo.getTO(), primaryStorageDownloadWait, VirtualMachineManager.ExecuteInSequence.value());

            try {
                handleQualityOfServiceForVolumeMigration(volumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.MIGRATION);

                if (srcVolumeDetached) {
                    _volumeService.grantAccess(volumeInfo, hostVO, srcDataStore);
                }

                Map<String, String> srcDetails = getVolumeDetails(volumeInfo);

                copyCommand.setOptions(srcDetails);

                copyCmdAnswer = (CopyCmdAnswer)agentManager.send(hostVO.getId(), copyCommand);

                if (!copyCmdAnswer.getResult()) {
                    errMsg = copyCmdAnswer.getDetails();

                    LOGGER.warn(errMsg);

                    throw new CloudRuntimeException(errMsg);
                }

                VMTemplateVO vmTemplateVO = _vmTemplateDao.findById(templateInfo.getId());

                vmTemplateVO.setHypervisorType(HypervisorType.KVM);

                _vmTemplateDao.update(vmTemplateVO.getId(), vmTemplateVO);
            }
            catch (CloudRuntimeException | AgentUnavailableException | OperationTimedoutException ex) {
                String msg = "Failed to create template from volume (Volume ID = " + volumeInfo.getId() + ") : ";

                LOGGER.warn(msg, ex);

                throw new CloudRuntimeException(msg + ex.getMessage(), ex);
            }
            finally {
                if (srcVolumeDetached) {
                    try {
                        _volumeService.revokeAccess(volumeInfo, hostVO, srcDataStore);
                    }
                    catch (Exception ex) {
                        LOGGER.warn("Error revoking access to volume (Volume ID = " + volumeInfo.getId() + "): " + ex.getMessage(), ex);
                    }
                }

                handleQualityOfServiceForVolumeMigration(volumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.NO_MIGRATION);

                if (copyCmdAnswer == null || !copyCmdAnswer.getResult()) {
                    if (copyCmdAnswer != null && !StringUtils.isEmpty(copyCmdAnswer.getDetails())) {
                        errMsg = copyCmdAnswer.getDetails();
                    }
                    else {
                        errMsg = "Unable to create template from volume";
                    }
                }

                try {
                    if (StringUtils.isEmpty(errMsg)) {
                        volumeInfo.processEvent(Event.OperationSuccessed);
                    }
                    else {
                        volumeInfo.processEvent(Event.OperationFailed);
                    }
                }
                catch (Exception ex) {
                    LOGGER.warn("Error processing snapshot event: " + ex.getMessage(), ex);
                }
            }
        }
        catch (Exception ex) {
            errMsg = ex.getMessage();

            throw new CloudRuntimeException(errMsg);
        }
        finally {
            if (copyCmdAnswer == null) {
                copyCmdAnswer = new CopyCmdAnswer(errMsg);
            }

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private Map<String, String> getVolumeDetails(VolumeInfo volumeInfo) {
        long storagePoolId = volumeInfo.getPoolId();
        StoragePoolVO storagePoolVO = _storagePoolDao.findById(storagePoolId);

        if (!storagePoolVO.isManaged()) {
            return null;
        }

        Map<String, String> volumeDetails = new HashMap<>();

        VolumeVO volumeVO = _volumeDao.findById(volumeInfo.getId());

        volumeDetails.put(DiskTO.STORAGE_HOST, storagePoolVO.getHostAddress());
        volumeDetails.put(DiskTO.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));
        volumeDetails.put(DiskTO.IQN, volumeVO.get_iScsiName());

        volumeDetails.put(DiskTO.VOLUME_SIZE, String.valueOf(volumeVO.getSize()));
        volumeDetails.put(DiskTO.SCSI_NAA_DEVICE_ID, getVolumeProperty(volumeInfo.getId(), DiskTO.SCSI_NAA_DEVICE_ID));

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

        snapshotDetails.put(DiskTO.IQN, getSnapshotProperty(snapshotId, DiskTO.IQN));
        snapshotDetails.put(DiskTO.VOLUME_SIZE, String.valueOf(snapshotInfo.getSize()));
        snapshotDetails.put(DiskTO.SCSI_NAA_DEVICE_ID, getSnapshotProperty(snapshotId, DiskTO.SCSI_NAA_DEVICE_ID));

        snapshotDetails.put(DiskTO.CHAP_INITIATOR_USERNAME, getSnapshotProperty(snapshotId, DiskTO.CHAP_INITIATOR_USERNAME));
        snapshotDetails.put(DiskTO.CHAP_INITIATOR_SECRET, getSnapshotProperty(snapshotId, DiskTO.CHAP_INITIATOR_SECRET));
        snapshotDetails.put(DiskTO.CHAP_TARGET_USERNAME, getSnapshotProperty(snapshotId, DiskTO.CHAP_TARGET_USERNAME));
        snapshotDetails.put(DiskTO.CHAP_TARGET_SECRET, getSnapshotProperty(snapshotId, DiskTO.CHAP_TARGET_SECRET));

        return snapshotDetails;
    }

    private HostVO getHost(SnapshotInfo snapshotInfo) {
        HypervisorType hypervisorType = snapshotInfo.getHypervisorType();

        if (HypervisorType.XenServer.equals(hypervisorType)) {
            HostVO hostVO = getHost(snapshotInfo.getDataCenterId(), hypervisorType, true);

            if (hostVO == null) {
                hostVO = getHost(snapshotInfo.getDataCenterId(), hypervisorType, false);

                if (hostVO == null) {
                    throw new CloudRuntimeException("Unable to locate an applicable host in data center with ID = " + snapshotInfo.getDataCenterId());
                }
            }

            return hostVO;
        }

        if (HypervisorType.VMware.equals(hypervisorType) || HypervisorType.KVM.equals(hypervisorType)) {
            return getHost(snapshotInfo.getDataCenterId(), hypervisorType, false);
        }

        throw new CloudRuntimeException("Unsupported hypervisor type");
    }

    private HostVO getHostInCluster(long clusterId) {
        List<HostVO> hosts = _hostDao.findByClusterId(clusterId);

        if (hosts != null && hosts.size() > 0) {
            Collections.shuffle(hosts, RANDOM);

            for (HostVO host : hosts) {
                if (ResourceState.Enabled.equals(host.getResourceState())) {
                    return host;
                }
            }
        }

        throw new CloudRuntimeException("Unable to locate a host");
    }

    private HostVO getHost(Long zoneId, HypervisorType hypervisorType, boolean computeClusterMustSupportResign) {
        Preconditions.checkArgument(zoneId != null, "Zone ID cannot be null.");
        Preconditions.checkArgument(hypervisorType != null, "Hypervisor type cannot be null.");

        List<HostVO> hosts = _hostDao.listByDataCenterIdAndHypervisorType(zoneId, hypervisorType);

        if (hosts == null) {
            return null;
        }

        List<Long> clustersToSkip = new ArrayList<>();

        Collections.shuffle(hosts, RANDOM);

        for (HostVO host : hosts) {
            if (!ResourceState.Enabled.equals(host.getResourceState())) {
                continue;
            }

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

    private Map<String, String> getDetails(DataObject dataObj) {
        if (dataObj instanceof VolumeInfo) {
            return getVolumeDetails((VolumeInfo)dataObj);
        }
        else if (dataObj instanceof SnapshotInfo) {
            return getSnapshotDetails((SnapshotInfo)dataObj);
        }

        throw new CloudRuntimeException("'dataObj' must be of type 'VolumeInfo' or 'SnapshotInfo'.");
    }

    private boolean isForVMware(DataObject dataObj) {
        if (dataObj instanceof VolumeInfo) {
            return ImageFormat.OVA.equals(((VolumeInfo)dataObj).getFormat());
        }

        if (dataObj instanceof SnapshotInfo) {
            return ImageFormat.OVA.equals(((SnapshotInfo)dataObj).getBaseVolume().getFormat());
        }

        return dataObj instanceof TemplateInfo && HypervisorType.VMware.equals(((TemplateInfo)dataObj).getHypervisorType());
    }

    private CopyCmdAnswer performResignature(DataObject dataObj, HostVO hostVO, Map<String, String> extraDetails) {
        return performResignature(dataObj, hostVO, extraDetails, false);
    }

    private CopyCmdAnswer performResignature(DataObject dataObj, HostVO hostVO, Map<String, String> extraDetails, boolean keepGrantedAccess) {
        long storagePoolId = dataObj.getDataStore().getId();
        DataStore dataStore = dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);

        Map<String, String> details = getDetails(dataObj);

        if (extraDetails != null) {
            details.putAll(extraDetails);
        }

        ResignatureCommand command = new ResignatureCommand(details);

        ResignatureAnswer answer;

        GlobalLock lock = GlobalLock.getInternLock(dataStore.getUuid());

        if (!lock.lock(LOCK_TIME_IN_SECONDS)) {
            String errMsg = "Couldn't lock the DB (in performResignature) on the following string: " + dataStore.getUuid();

            LOGGER.warn(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        try {
            _volumeService.grantAccess(dataObj, hostVO, dataStore);

            answer = (ResignatureAnswer)agentManager.send(hostVO.getId(), command);
        }
        catch (CloudRuntimeException | AgentUnavailableException | OperationTimedoutException ex) {
            keepGrantedAccess = false;

            String msg = "Failed to resign the DataObject with the following ID: " + dataObj.getId();

            LOGGER.warn(msg, ex);

            throw new CloudRuntimeException(msg + ex.getMessage());
        }
        finally {
            lock.unlock();
            lock.releaseRef();

            if (!keepGrantedAccess) {
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

    private DataObject cacheSnapshotChain(SnapshotInfo snapshot, Scope scope) {
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

    private String migrateVolumeForKVM(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo, HostVO hostVO, String errMsg) {
        boolean srcVolumeDetached = srcVolumeInfo.getAttachedVM() == null;

        try {
            Map<String, String> srcDetails = getVolumeDetails(srcVolumeInfo);
            Map<String, String> destDetails = getVolumeDetails(destVolumeInfo);

            MigrateVolumeCommand migrateVolumeCommand = new MigrateVolumeCommand(srcVolumeInfo.getTO(), destVolumeInfo.getTO(),
                    srcDetails, destDetails, StorageManager.KvmStorageOfflineMigrationWait.value());

            if (srcVolumeDetached) {
                _volumeService.grantAccess(srcVolumeInfo, hostVO, srcVolumeInfo.getDataStore());
            }

            handleQualityOfServiceForVolumeMigration(destVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.MIGRATION);

            _volumeService.grantAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());

            MigrateVolumeAnswer migrateVolumeAnswer = (MigrateVolumeAnswer)agentManager.send(hostVO.getId(), migrateVolumeCommand);

            if (migrateVolumeAnswer == null || !migrateVolumeAnswer.getResult()) {
                if (migrateVolumeAnswer != null && !StringUtils.isEmpty(migrateVolumeAnswer.getDetails())) {
                    throw new CloudRuntimeException(migrateVolumeAnswer.getDetails());
                }
                else {
                    throw new CloudRuntimeException(errMsg);
                }
            }

            if (srcVolumeDetached) {
                _volumeService.revokeAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());
            }

            try {
                _volumeService.revokeAccess(srcVolumeInfo, hostVO, srcVolumeInfo.getDataStore());
            }
            catch (Exception e) {
                // This volume should be deleted soon, so just log a warning here.
                LOGGER.warn(e.getMessage(), e);
            }

            return migrateVolumeAnswer.getVolumePath();
        }
        catch (Exception ex) {
            try {
                _volumeService.revokeAccess(destVolumeInfo, hostVO, destVolumeInfo.getDataStore());
            }
            catch (Exception e) {
                // This volume should be deleted soon, so just log a warning here.
                LOGGER.warn(e.getMessage(), e);
            }

            if (srcVolumeDetached) {
                _volumeService.revokeAccess(srcVolumeInfo, hostVO, srcVolumeInfo.getDataStore());
            }

            String msg = "Failed to perform volume migration : ";

            LOGGER.warn(msg, ex);

            throw new CloudRuntimeException(msg + ex.getMessage(), ex);
        }
        finally {
            handleQualityOfServiceForVolumeMigration(destVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.NO_MIGRATION);
        }
    }

    private String copyManagedVolumeToSecondaryStorage(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo, HostVO hostVO, String errMsg) {
        boolean srcVolumeDetached = srcVolumeInfo.getAttachedVM() == null;

        try {
            StoragePoolVO storagePoolVO = _storagePoolDao.findById(srcVolumeInfo.getPoolId());
            Map<String, String> srcDetails = getVolumeDetails(srcVolumeInfo);

            CopyVolumeCommand copyVolumeCommand = new CopyVolumeCommand(srcVolumeInfo.getId(), destVolumeInfo.getPath(), storagePoolVO,
                    destVolumeInfo.getDataStore().getUri(), true, StorageManager.KvmStorageOfflineMigrationWait.value(), true);

            copyVolumeCommand.setSrcData(srcVolumeInfo.getTO());
            copyVolumeCommand.setSrcDetails(srcDetails);

            handleQualityOfServiceForVolumeMigration(srcVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.MIGRATION);

            if (srcVolumeDetached) {
                _volumeService.grantAccess(srcVolumeInfo, hostVO, srcVolumeInfo.getDataStore());
            }

            CopyVolumeAnswer copyVolumeAnswer = (CopyVolumeAnswer)agentManager.send(hostVO.getId(), copyVolumeCommand);

            if (copyVolumeAnswer == null || !copyVolumeAnswer.getResult()) {
                if (copyVolumeAnswer != null && !StringUtils.isEmpty(copyVolumeAnswer.getDetails())) {
                    throw new CloudRuntimeException(copyVolumeAnswer.getDetails());
                }
                else {
                    throw new CloudRuntimeException(errMsg);
                }
            }

            return copyVolumeAnswer.getVolumePath();
        }
        catch (Exception ex) {
            String msg = "Failed to perform volume copy to secondary storage : ";

            LOGGER.warn(msg, ex);

            throw new CloudRuntimeException(msg + ex.getMessage());
        }
        finally {
            if (srcVolumeDetached) {
                _volumeService.revokeAccess(srcVolumeInfo, hostVO, srcVolumeInfo.getDataStore());
            }

            handleQualityOfServiceForVolumeMigration(srcVolumeInfo, PrimaryDataStoreDriver.QualityOfServiceState.NO_MIGRATION);
        }
    }

    private void setCertainVolumeValuesNull(long volumeId) {
        VolumeVO volumeVO = _volumeDao.findById(volumeId);

        volumeVO.set_iScsiName(null);
        volumeVO.setMinIops(null);
        volumeVO.setMaxIops(null);
        volumeVO.setHypervisorSnapshotReserve(null);

        _volumeDao.update(volumeId, volumeVO);
    }

    private void updateVolumePath(long volumeId, String path) {
        VolumeVO volumeVO = _volumeDao.findById(volumeId);

        volumeVO.setPath(path);

        _volumeDao.update(volumeId, volumeVO);
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

        int primaryStorageDownloadWait = StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value();

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

            copyCmdAnswer = (CopyCmdAnswer)agentManager.send(hostVO.getId(), copyCommand);
        }
        catch (CloudRuntimeException | AgentUnavailableException | OperationTimedoutException ex) {
            String msg = "Failed to perform VDI copy : ";

            LOGGER.warn(msg, ex);

            throw new CloudRuntimeException(msg + ex.getMessage(), ex);
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
