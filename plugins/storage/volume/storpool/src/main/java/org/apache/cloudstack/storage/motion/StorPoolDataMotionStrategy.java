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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.RemoteHostEndPoint;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.util.StorPoolHelper;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpApiResponse;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpConnectionDesc;
import org.apache.cloudstack.storage.snapshot.StorPoolConfigurationManager;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo;
import com.cloud.agent.api.ModifyTargetsAnswer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.storage.StorPoolBackupTemplateFromSnapshotCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class StorPoolDataMotionStrategy implements DataMotionStrategy {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private SnapshotDataFactory _snapshotDataFactory;
    @Inject
    private DataStoreManager _dataStore;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private EndPointSelector _selector;
    @Inject
    private TemplateDataStoreDao _templStoreDao;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject
    private VMTemplateDetailsDao _vmTemplateDetailsDao;
    @Inject
    private SnapshotDataStoreDao _snapshotStoreDao;
    @Inject
    private StoragePoolDetailsDao _storagePoolDetails;
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
    public StrategyPriority canHandle(DataObject srcData, DataObject destData) {
        DataObjectType srcType = srcData.getType();
        DataObjectType dstType = destData.getType();
        if (srcType == DataObjectType.SNAPSHOT && dstType == DataObjectType.TEMPLATE
                && StorPoolConfigurationManager.BypassSecondaryStorage.value()) {
            SnapshotInfo sinfo = (SnapshotInfo) srcData;
            VolumeInfo volume = sinfo.getBaseVolume();
            StoragePoolVO storagePool = _storagePool.findById(volume.getPoolId());
            if (!storagePool.getStorageProviderName().equals(StorPoolUtil.SP_PROVIDER_NAME)) {
                return StrategyPriority.CANT_HANDLE;
            }
            String snapshotName = StorPoolHelper.getSnapshotName(sinfo.getId(), sinfo.getUuid(), _snapshotStoreDao,
                    _snapshotDetailsDao);
            StorPoolUtil.spLog("StorPoolDataMotionStrategy.canHandle snapshot name=%s", snapshotName);
            if (snapshotName != null) {
                return StrategyPriority.HIGHEST;
            }
        }
        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        SnapshotObjectTO snapshot = (SnapshotObjectTO) srcData.getTO();
        TemplateObjectTO template = (TemplateObjectTO) destData.getTO();
        DataStore store = _dataStore.getDataStore(snapshot.getVolume().getDataStore().getUuid(),
                snapshot.getVolume().getDataStore().getRole());
        SnapshotInfo sInfo = _snapshotDataFactory.getSnapshot(snapshot.getId(), store);

        VolumeInfo vInfo = sInfo.getBaseVolume();
        SpConnectionDesc conn = StorPoolUtil.getSpConnection(vInfo.getDataStore().getUuid(),
                vInfo.getDataStore().getId(), _storagePoolDetails, _storagePool);
        String name = template.getUuid();
        String volumeName = "";

        String parentName = StorPoolHelper.getSnapshotName(sInfo.getId(), sInfo.getUuid(), _snapshotStoreDao,
                _snapshotDetailsDao);
        // TODO volume tags cs - template
        SpApiResponse res = StorPoolUtil.volumeCreate(name, parentName, sInfo.getSize(), null, "no", "template", null,
                conn);
        CopyCmdAnswer answer = null;
        String err = null;
        if (res.getError() != null) {
            logger.debug(String.format("Could not create volume from snapshot with ID=%s", snapshot.getId()));
            StorPoolUtil.spLog("Volume create failed with error=%s", res.getError().getDescr());
            err = res.getError().getDescr();
        } else {
            volumeName = StorPoolUtil.getNameFromResponse(res, true);
            SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(sInfo.getId(), sInfo.getUuid());

            snapshot.setPath(snapshotDetails.getValue());
            Command backupSnapshot = new StorPoolBackupTemplateFromSnapshotCommand(snapshot, template,
                    StorPoolHelper.getTimeout(StorPoolHelper.BackupSnapshotWait, _configDao),
                    VirtualMachineManager.ExecuteInSequence.value());

            try {
                // final String snapName =
                // StorpoolStorageAdaptor.getVolumeNameFromPath(((SnapshotInfo)
                // srcData).getPath(), true);
                Long clusterId = StorPoolHelper.findClusterIdByGlobalId(parentName, _clusterDao);
                EndPoint ep2 = clusterId != null
                        ? RemoteHostEndPoint
                                .getHypervisorHostEndPoint(StorPoolHelper.findHostByCluster(clusterId, _hostDao))
                        : _selector.select(sInfo, destData);
                if (ep2 == null) {
                    err = "No remote endpoint to send command, check if host or ssvm is down?";
                } else {
                    answer = (CopyCmdAnswer) ep2.sendMessage(backupSnapshot);
                    if (answer != null && answer.getResult()) {
                        SpApiResponse resSnapshot = StorPoolUtil.volumeFreeze(volumeName, conn);
                        if (resSnapshot.getError() != null) {
                            logger.debug(String.format("Could not snapshot volume with ID=%s", snapshot.getId()));
                            StorPoolUtil.spLog("Volume freeze failed with error=%s", resSnapshot.getError().getDescr());
                            err = resSnapshot.getError().getDescr();
                            StorPoolUtil.volumeDelete(volumeName, conn);
                        } else {
                            StorPoolHelper.updateVmStoreTemplate(template.getId(), template.getDataStore().getRole(),
                                    StorPoolUtil.devPath(StorPoolUtil.getNameFromResponse(res, false)), _templStoreDao);
                        }
                    } else {
                        err = "Could not copy template to secondary " + answer.getResult();
                        StorPoolUtil.volumeDelete(StorPoolUtil.getNameFromResponse(res, true), conn);
                    }
                }
            } catch (CloudRuntimeException e) {
                err = e.getMessage();
            }
        }
        _vmTemplateDetailsDao.persist(new VMTemplateDetailVO(template.getId(), StorPoolUtil.SP_STORAGE_POOL_ID,
                String.valueOf(vInfo.getDataStore().getId()), false));
        StorPoolUtil.spLog("StorPoolDataMotionStrategy.copyAsync Creating snapshot=%s for StorPool template=%s",
                volumeName, conn.getTemplateName());
        final CopyCommandResult cmd = new CopyCommandResult(null, answer);
        cmd.setResult(err);
        callback.complete(cmd);
    }

    @Override
    public StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        return canHandleLiveMigrationOnStorPool(volumeMap, srcHost, destHost);
    }

    final StrategyPriority canHandleLiveMigrationOnStorPool(Map<VolumeInfo, DataStore> volumeMap, Host srcHost,
            Host destHost) {
        if (srcHost.getId() != destHost.getId() && isDestinationStorPoolPrimaryStorage(volumeMap)) {
            return StrategyPriority.HIGHEST;
        }
        return StrategyPriority.CANT_HANDLE;
    }

    private boolean isDestinationStorPoolPrimaryStorage(Map<VolumeInfo, DataStore> volumeMap) {
        if (MapUtils.isNotEmpty(volumeMap)) {
            for (DataStore dataStore : volumeMap.values()) {
                StoragePoolVO storagePoolVO = _storagePool.findById(dataStore.getId());
                if (storagePoolVO == null
                        || !storagePoolVO.getStorageProviderName().equals(StorPoolUtil.SP_PROVIDER_NAME)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void copyAsync(Map<VolumeInfo, DataStore> volumeDataStoreMap, VirtualMachineTO vmTO, Host srcHost,
            Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        String errMsg = null;
        String newVolName = null;
        SpConnectionDesc conn = null;

        try {
            if (srcHost.getHypervisorType() != HypervisorType.KVM) {
                throw new CloudRuntimeException(String.format("Invalid hypervisor type [%s]. Only KVM supported", srcHost.getHypervisorType()));
            }

            VMInstanceVO vmInstance = _vmDao.findById(vmTO.getId());
            vmTO.setState(vmInstance.getState());
            List<MigrateDiskInfo> migrateDiskInfoList = new ArrayList<MigrateDiskInfo>();

            Map<String, MigrateCommand.MigrateDiskInfo> migrateStorage = new HashMap<>();
            Map<VolumeInfo, VolumeInfo> srcVolumeInfoToDestVolumeInfo = new HashMap<>();

            for (Map.Entry<VolumeInfo, DataStore> entry : volumeDataStoreMap.entrySet()) {
                VolumeInfo srcVolumeInfo = entry.getKey();
                if (srcVolumeInfo.getPassphraseId() != null) {
                    throw new CloudRuntimeException(String.format("Cannot live migrate encrypted volume [%s] to StorPool", srcVolumeInfo.getName()));
                }
                DataStore destDataStore = entry.getValue();

                VolumeVO srcVolume = _volumeDao.findById(srcVolumeInfo.getId());
                StoragePoolVO destStoragePool = _storagePool.findById(destDataStore.getId());

                VolumeVO destVolume = duplicateVolumeOnAnotherStorage(srcVolume, destStoragePool);

                VolumeInfo destVolumeInfo = _volumeDataFactory.getVolume(destVolume.getId(), destDataStore);

                destVolumeInfo.processEvent(Event.MigrationCopyRequested);
                destVolumeInfo.processEvent(Event.MigrationCopySucceeded);
                destVolumeInfo.processEvent(Event.MigrationRequested);

                conn = StorPoolUtil.getSpConnection(destDataStore.getUuid(), destDataStore.getId(), _storagePoolDetails,
                        _storagePool);
                SpApiResponse resp = StorPoolUtil.volumeCreate(srcVolume.getUuid(), null, srcVolume.getSize(),
                        vmTO.getUuid(), null, "volume", srcVolume.getMaxIops(), conn);

                if (resp.getError() == null) {
                    newVolName = StorPoolUtil.getNameFromResponse(resp, true);
                }

                String volumeName = StorPoolUtil.getNameFromResponse(resp, false);
                destVolume.setPath(StorPoolUtil.devPath(volumeName));
                _volumeDao.update(destVolume.getId(), destVolume);
                destVolume = _volumeDao.findById(destVolume.getId());

                destVolumeInfo = _volumeDataFactory.getVolume(destVolume.getId(), destDataStore);

                String destPath = generateDestPath(destHost, destStoragePool, destVolumeInfo);

                MigrateCommand.MigrateDiskInfo migrateDiskInfo = configureMigrateDiskInfo(srcVolumeInfo, destPath);
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
                    "Copy volume(s) of VM [%s] to storage(s) [%s] and VM to host [%s] failed in StorPoolDataMotionStrategy.copyAsync. Error message: [%s].",
                    vmTO.getId(), srcHost.getId(), destHost.getId(), ex.getMessage());
            logger.error(errMsg, ex);

            throw new CloudRuntimeException(errMsg);
        } finally {
            if (errMsg != null) {
                deleteVolumeOnFail(newVolName, conn);
            }
            CopyCmdAnswer copyCmdAnswer = new CopyCmdAnswer(errMsg);

            CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

            result.setResult(errMsg);

            callback.complete(result);
        }
    }

    private void deleteVolumeOnFail(String newVolName, SpConnectionDesc conn) {
        if (newVolName != null && conn != null) {
            StorPoolUtil.volumeDelete(newVolName, conn);
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
                    disconnectHostFromVolume(destHost, destVolumeInfo.getPoolId(), destVolumeInfo.getPath());
                } catch (Exception e) {
                    logger.debug("Failed to disconnect (new) dest volume", e);
                }

                try {
                    _volumeService.revokeAccess(destVolumeInfo, destHost, destVolumeInfo.getDataStore());
                } catch (Exception e) {
                    logger.debug("Failed to revoke access from dest volume", e);
                }

                destVolumeInfo.processEvent(Event.OperationFailed);
                srcVolumeInfo.processEvent(Event.OperationFailed);

                try {
                    _volumeService.destroyVolume(destVolumeInfo.getId());

                    destVolumeInfo = _volumeDataFactory.getVolume(destVolumeInfo.getId());

                    AsyncCallFuture<VolumeApiResult> destroyFuture = _volumeService.expungeVolumeAsync(destVolumeInfo);

                    if (destroyFuture.get().isFailed()) {
                        logger.debug("Failed to clean up dest volume on storage");
                    }
                } catch (Exception e) {
                    logger.debug("Failed to clean up dest volume on storage", e);
                }
            }
        }
    }

    private String generateDestPath(Host destHost, StoragePoolVO destStoragePool, VolumeInfo destVolumeInfo) {
        return connectHostToVolume(destHost, destVolumeInfo.getPoolId(), destVolumeInfo.getPath());
    }

    private String connectHostToVolume(Host host, long storagePoolId, String iqn) {
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
        ModifyTargetsAnswer modifyTargetsAnswer = (ModifyTargetsAnswer) _agentManager.easySend(hostId, cmd);

        if (modifyTargetsAnswer == null) {
            throw new CloudRuntimeException("Unable to get an answer to the modify targets command");
        }

        if (!modifyTargetsAnswer.getResult()) {
            String msg = "Unable to modify targets on the following host: " + hostId;

            throw new CloudRuntimeException(msg);
        }

        return modifyTargetsAnswer.getConnectedPaths();
    }

    protected MigrateCommand.MigrateDiskInfo configureMigrateDiskInfo(VolumeInfo srcVolumeInfo, String destPath) {
        return new MigrateCommand.MigrateDiskInfo(srcVolumeInfo.getPath(),
                MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, MigrateCommand.MigrateDiskInfo.DriverType.RAW,
                MigrateCommand.MigrateDiskInfo.Source.DEV, destPath);
    }
}
