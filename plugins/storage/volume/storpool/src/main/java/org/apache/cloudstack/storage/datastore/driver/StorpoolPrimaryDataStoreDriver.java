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
package org.apache.cloudstack.storage.datastore.driver;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.RemoteHostEndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolHelper;
import org.apache.cloudstack.storage.datastore.util.StorpoolUtil;
import org.apache.cloudstack.storage.datastore.util.StorpoolUtil.SpApiResponse;
import org.apache.cloudstack.storage.datastore.util.StorpoolUtil.SpConnectionDesc;
import org.apache.cloudstack.storage.snapshot.StorPoolConfigurationManager;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.StorpoolBackupSnapshotCommand;
import com.cloud.agent.api.storage.StorpoolBackupTemplateFromSnapshotCommand;
import com.cloud.agent.api.storage.StorpoolCopyVolumeToSecondaryCommand;
import com.cloud.agent.api.storage.StorpoolDownloadTemplateCommand;
import com.cloud.agent.api.storage.StorpoolDownloadVolumeCommand;
import com.cloud.agent.api.storage.StorpoolResizeVolumeCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.kvm.storage.StorpoolStorageAdaptor;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplatePoolDaoImpl;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

public class StorpoolPrimaryDataStoreDriver implements PrimaryDataStoreDriver {

    private static final Logger log = Logger.getLogger(StorpoolPrimaryDataStoreDriver.class);

    @Inject
    private VolumeDao volumeDao;
    @Inject
    private StorageManager storageMgr;
    @Inject
    private PrimaryDataStoreDao primaryStoreDao;
    @Inject
    private EndPointSelector selector;
    @Inject
    private ConfigurationDao configDao;
    @Inject
    private TemplateDataStoreDao vmTemplateDataStoreDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private ResourceTagDao _resourceTagDao;
    @Inject
    private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject
    private SnapshotDataStoreDao snapshotDataStoreDao;
    @Inject
    private VolumeDetailsDao volumeDetailsDao;
    @Inject
    private VMTemplateDetailsDao vmTemplateDetailsDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    private VMTemplatePoolDaoImpl vmTemplatePoolDao;

    @Override
    public Map<String, String> getCapabilities() {
        return null;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    @Override
    public long getUsedBytes(StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getUsedIops(StoragePool storagePool) {
        return 0;
    }

    @Override
    public boolean grantAccess(DataObject data, Host host, DataStore dataStore) {
        log.debug("grantAccess");
        return false;
    }

    @Override
    public void revokeAccess(DataObject data, Host host, DataStore dataStore) {
        log.debug("revokeAccess");
    }

    private void updateStoragePool(final long poolId, final long deltaUsedBytes) {
        StoragePoolVO storagePool = primaryStoreDao.findById(poolId);
        final long capacity = storagePool.getCapacityBytes();
        final long used = storagePool.getUsedBytes() + deltaUsedBytes;

        storagePool.setUsedBytes(used < 0 ? 0 : (used > capacity ? capacity : used));
        primaryStoreDao.update(poolId, storagePool);
    }

    private String getVMInstanceUUID(Long id) {
        return id != null ? vmInstanceDao.findById(id).getUuid() : null;
    }

    protected void _completeResponse(final CreateObjectAnswer answer, final String err, final AsyncCompletionCallback<CommandResult> callback)
    {
        final CreateCmdResult res = new CreateCmdResult(null, answer);
        res.setResult(err);
        callback.complete(res);
    }

    protected void completeResponse(final DataTO result, final AsyncCompletionCallback<CommandResult> callback)
    {
        _completeResponse(new CreateObjectAnswer(result), null, callback);
    }

    protected void completeResponse(final String err, final AsyncCompletionCallback<CommandResult> callback)
    {
        _completeResponse(new CreateObjectAnswer(err), err, callback);
    }

    @Override
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool pool) {
        return dataObject.getSize();
    }

    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool) {
        return 0;
    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject) {
        return null;
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        String path = null;
        String err = null;
        if (data.getType() == DataObjectType.VOLUME) {
            VolumeInfo vinfo = (VolumeInfo)data;
            String name = vinfo.getUuid();
            Long size = vinfo.getSize();
            SpConnectionDesc conn = StorpoolUtil.getSpConnection(dataStore.getUuid(), dataStore.getId(), storagePoolDetailsDao, primaryStoreDao);

            StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriver.createAsync volume: name=%s, uuid=%s, isAttached=%s vm=%s, payload=%s, template: %s", vinfo.getName(), vinfo.getUuid(), vinfo.isAttachedVM(), vinfo.getAttachedVmName(), vinfo.getpayload(), conn.getTemplateName());
            SpApiResponse resp = StorpoolUtil.volumeCreate(name, null, size, getVMInstanceUUID(vinfo.getInstanceId()), null, "volume", vinfo.getMaxIops(), conn);
            if (resp.getError() == null) {
                String volumeName = StorpoolUtil.getNameFromResponse(resp, false);
                path = StorpoolUtil.devPath(volumeName);

                VolumeVO volume = volumeDao.findById(vinfo.getId());
                volume.setPoolId(dataStore.getId());
                volume.setPoolType(StoragePoolType.SharedMountPoint);
                volume.setPath(path);
                volumeDao.update(volume.getId(), volume);

                updateStoragePool(dataStore.getId(), size);
                StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriver.createAsync volume: name=%s, uuid=%s, isAttached=%s vm=%s, payload=%s, template: %s", volumeName, vinfo.getUuid(), vinfo.isAttachedVM(), vinfo.getAttachedVmName(), vinfo.getpayload(), conn.getTemplateName());
            } else {
                err = String.format("Could not create StorPool volume %s. Error: %s", name, resp.getError());
            }
        } else {
            err = String.format("Invalid object type \"%s\"  passed to createAsync", data.getType());
        }

        CreateCmdResult res = new CreateCmdResult(path, new Answer(null, err == null, err));
        res.setResult(err);
        if (callback != null) {
            callback.complete(res);
        }
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        String path = null;
        String err = null;
        ResizeVolumeAnswer answer = null;

        if (data.getType() == DataObjectType.VOLUME) {
            VolumeObject vol = (VolumeObject)data;
            StoragePool pool = (StoragePool)data.getDataStore();
            ResizeVolumePayload payload = (ResizeVolumePayload)vol.getpayload();

            final String name = StorpoolStorageAdaptor.getVolumeNameFromPath(vol.getPath(), true);
            final long oldSize = vol.getSize();
            Long oldMaxIops = vol.getMaxIops();
            SpConnectionDesc conn = StorpoolUtil.getSpConnection(data.getDataStore().getUuid(), data.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);

            StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.resize: name=%s, uuid=%s, oldSize=%d, newSize=%s, shrinkOk=%s", name, vol.getUuid(), oldSize, payload.newSize, payload.shrinkOk);

            SpApiResponse resp = StorpoolUtil.volumeUpdate(name, payload.newSize, payload.shrinkOk, payload.newMaxIops, conn);
            if (resp.getError() != null) {
                err = String.format("Could not resize StorPool volume %s. Error: %s", name, resp.getError());
            } else {
                try {
                    StorpoolResizeVolumeCommand resizeCmd = new StorpoolResizeVolumeCommand(vol.getPath(), new StorageFilerTO(pool), vol.getSize(),
                                payload.newSize, payload.shrinkOk, payload.instanceName, payload.hosts == null ? false : true);
                    answer = (ResizeVolumeAnswer)storageMgr.sendToPool(pool, payload.hosts, resizeCmd);

                    if (answer == null || !answer.getResult()) {
                        err = answer != null ? answer.getDetails() : "return a null answer, resize failed for unknown reason";
                    } else {
                        path = StorpoolUtil.devPath(StorpoolUtil.getNameFromResponse(resp, false));

                        vol.setSize(payload.newSize);
                        vol.update();
                        if (payload.newMaxIops != null) {
                            VolumeVO volume = volumeDao.findById(vol.getId());
                            volume.setMaxIops(payload.newMaxIops);
                            volumeDao.update(volume.getId(), volume);
                        }

                        updateStoragePool(vol.getPoolId(), payload.newSize - oldSize);
                    }
                } catch (Exception e) {
                    log.debug("sending resize command failed", e);
                    err = e.toString();
                }
            }

            if (err != null) {
                // try restoring volume to its initial size
                resp = StorpoolUtil.volumeUpdate(name, oldSize, true, oldMaxIops, conn);
                if (resp.getError() != null) {
                    log.debug(String.format("Could not resize StorPool volume %s back to its original size. Error: %s", name, resp.getError()));
                }
            }
        } else {
            err = String.format("Invalid object type \"%s\"  passed to resize", data.getType());
        }

        CreateCmdResult res = new CreateCmdResult(path, answer);
        res.setResult(err);
        callback.complete(res);
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        String err = null;
        if (data.getType() == DataObjectType.VOLUME) {
            VolumeInfo vinfo = (VolumeInfo)data;
            String name = StorpoolStorageAdaptor.getVolumeNameFromPath(vinfo.getPath(), true);
            StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriver.deleteAsync delete volume: name=%s, uuid=%s, isAttached=%s vm=%s, payload=%s dataStore=%s", name, vinfo.getUuid(), vinfo.isAttachedVM(), vinfo.getAttachedVmName(), vinfo.getpayload(), dataStore.getUuid());
            if (name == null) {
                name = vinfo.getUuid();
            }
            SpConnectionDesc conn = StorpoolUtil.getSpConnection(dataStore.getUuid(), dataStore.getId(), storagePoolDetailsDao, primaryStoreDao);

            SpApiResponse resp = StorpoolUtil.volumeDelete(name, conn);
            if (resp.getError() == null) {
                updateStoragePool(dataStore.getId(), - vinfo.getSize());
                VolumeDetailVO detail = volumeDetailsDao.findDetail(vinfo.getId(), StorpoolUtil.SP_PROVIDER_NAME);
                if (detail != null) {
                    volumeDetailsDao.remove(detail.getId());
                }
            } else {
                if (!resp.getError().getName().equalsIgnoreCase("objectDoesNotExist")) {
                    err = String.format("Could not delete StorPool volume %s. Error: %s", name, resp.getError());
                }
            }
        } else {
            err = String.format("Invalid DataObjectType \"%s\" passed to deleteAsync", data.getType());
        }

        if (err != null) {
            log.error(err);
            StorpoolUtil.spLog(err);
        }

        CommandResult res = new CommandResult();
        res.setResult(err);
        callback.complete(res);
    }

    private void logDataObject(final String pref, DataObject data) {
        final DataStore dstore = data.getDataStore();
        String name = null;
        Long size = null;

        if (data.getType() == DataObjectType.VOLUME) {
            VolumeInfo vinfo = (VolumeInfo)data;
            name = vinfo.getName();
            size = vinfo.getSize();
        } else if (data.getType() == DataObjectType.SNAPSHOT) {
            SnapshotInfo sinfo = (SnapshotInfo)data;
            name = sinfo.getName();
            size = sinfo.getSize();
        } else if (data.getType() == DataObjectType.TEMPLATE) {
            TemplateInfo tinfo = (TemplateInfo)data;
            name = tinfo.getName();
            size = tinfo.getSize();
        }

        StorpoolUtil.spLog("%s: name=%s, size=%s, uuid=%s, type=%s, dstore=%s:%s:%s", pref, name, size, data.getUuid(), data.getType(), dstore.getUuid(), dstore.getName(), dstore.getRole());
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject dstData) {
        return true;
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject dstData, AsyncCompletionCallback<CopyCommandResult> callback) {
        StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.copyAsnc:");
        logDataObject("SRC", srcData);
        logDataObject("DST", dstData);

        final DataObjectType srcType = srcData.getType();
        final DataObjectType dstType = dstData.getType();
        String err = null;
        Answer answer = null;
        StorageSubSystemCommand cmd = null;

        try {
            if (srcType == DataObjectType.SNAPSHOT && dstType == DataObjectType.VOLUME) {
                SnapshotInfo sinfo = (SnapshotInfo)srcData;
                final String snapshotName = StorPoolHelper.getSnapshotName(srcData.getId(), srcData.getUuid(), snapshotDataStoreDao, _snapshotDetailsDao);

                VolumeInfo vinfo = (VolumeInfo)dstData;
                final String volumeName = vinfo.getUuid();
                final Long size = vinfo.getSize();
                SpConnectionDesc conn = StorpoolUtil.getSpConnection(vinfo.getDataStore().getUuid(), vinfo.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);
                SpApiResponse resp = StorpoolUtil.volumeCreate(volumeName, snapshotName, size, null, null, "volume", sinfo.getBaseVolume().getMaxIops(), conn);
                if (resp.getError() == null) {
                    updateStoragePool(dstData.getDataStore().getId(), size);

                    VolumeObjectTO to = (VolumeObjectTO)dstData.getTO();
                    to.setPath(StorpoolUtil.devPath(StorpoolUtil.getNameFromResponse(resp, false)));
                    to.setSize(size);

                    answer = new CopyCmdAnswer(to);
                    StorpoolUtil.spLog("Created volume=%s with uuid=%s from snapshot=%s with uuid=%s", StorpoolUtil.getNameFromResponse(resp, false), to.getUuid(), snapshotName, sinfo.getUuid());
                } else if (resp.getError().getName().equals("objectDoesNotExist")) {
                    //check if snapshot is on secondary storage
                    StorpoolUtil.spLog("Snapshot %s does not exists on StorPool, will try to create a volume from a snopshot on secondary storage", snapshotName);
                    SnapshotDataStoreVO snap = snapshotDataStoreDao.findBySnapshot(sinfo.getId(), DataStoreRole.Image);
                    if (snap != null && StorpoolStorageAdaptor.getVolumeNameFromPath(snap.getInstallPath(), false) == null) {
                        resp = StorpoolUtil.volumeCreate(srcData.getUuid(), null, size, null, "no", "snapshot", sinfo.getBaseVolume().getMaxIops(), conn);
                        if (resp.getError() == null) {
                            VolumeObjectTO dstTO = (VolumeObjectTO) dstData.getTO();
                            dstTO.setSize(size);
                            dstTO.setPath(StorpoolUtil.devPath(StorpoolUtil.getNameFromResponse(resp, false)));
                            cmd = new StorpoolDownloadTemplateCommand(srcData.getTO(), dstTO, StorPoolHelper.getTimeout(StorPoolHelper.PrimaryStorageDownloadWait, configDao), VirtualMachineManager.ExecuteInSequence.value(), "volume");

                            EndPoint ep = selector.select(srcData, dstData);
                            if (ep == null) {
                                err = "No remote endpoint to send command, check if host or ssvm is down?";
                            } else {
                                answer = ep.sendMessage(cmd);
                            }

                            if (answer != null && answer.getResult()) {
                                SpApiResponse resp2 = StorpoolUtil.volumeFreeze(StorpoolUtil.getNameFromResponse(resp, true), conn);
                                if (resp2.getError() != null) {
                                    err = String.format("Could not freeze Storpool volume %s. Error: %s", srcData.getUuid(), resp2.getError());
                                } else {
                                    String name = StorpoolUtil.getNameFromResponse(resp, false);
                                    SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(sinfo.getId(), sinfo.getUuid());
                                    if (snapshotDetails != null) {
                                        StorPoolHelper.updateSnapshotDetailsValue(snapshotDetails.getId(), StorpoolUtil.devPath(name), "snapshot");
                                    }else {
                                        StorPoolHelper.addSnapshotDetails(sinfo.getId(), sinfo.getUuid(), StorpoolUtil.devPath(name), _snapshotDetailsDao);
                                    }
                                    resp = StorpoolUtil.volumeCreate(volumeName, StorpoolUtil.getNameFromResponse(resp, true), size, null, null, "volume", sinfo.getBaseVolume().getMaxIops(), conn);
                                    if (resp.getError() == null) {
                                        updateStoragePool(dstData.getDataStore().getId(), size);

                                        VolumeObjectTO to = (VolumeObjectTO) dstData.getTO();
                                        to.setPath(StorpoolUtil.devPath(StorpoolUtil.getNameFromResponse(resp, false)));
                                        to.setSize(size);
                                        // successfully downloaded snapshot to primary storage
                                        answer = new CopyCmdAnswer(to);
                                        StorpoolUtil.spLog("Created volume=%s with uuid=%s from snapshot=%s with uuid=%s", name, to.getUuid(), snapshotName, sinfo.getUuid());

                                    } else {
                                        err = String.format("Could not create Storpool volume %s from snapshot %s. Error: %s", volumeName, snapshotName, resp.getError());
                                    }
                                }
                            } else {
                                err = answer != null ? answer.getDetails() : "Unknown error while downloading template. Null answer returned.";
                            }
                        } else {
                            err = String.format("Could not create Storpool volume %s from snapshot %s. Error: %s", volumeName, snapshotName, resp.getError());
                        }
                    } else {
                        err = String.format("The snapshot %s does not exists neither on primary, neither on secondary storage. Cannot create volume from snapshot", snapshotName);
                    }
                } else {
                    err = String.format("Could not create Storpool volume %s from snapshot %s. Error: %s", volumeName, snapshotName, resp.getError());
                }
            } else if (srcType == DataObjectType.SNAPSHOT && dstType == DataObjectType.SNAPSHOT) {
                // bypass secondary storage
                if (StorPoolConfigurationManager.BypassSecondaryStorage.value()) {
                    SnapshotObjectTO snapshot = (SnapshotObjectTO) srcData.getTO();
                    answer = new CopyCmdAnswer(snapshot);
                } else {
                    // copy snapshot to secondary storage (backup snapshot)
                    cmd = new StorpoolBackupSnapshotCommand(srcData.getTO(), dstData.getTO(), StorPoolHelper.getTimeout(StorPoolHelper.BackupSnapshotWait, configDao), VirtualMachineManager.ExecuteInSequence.value());

                    final String snapName =  StorpoolStorageAdaptor.getVolumeNameFromPath(((SnapshotInfo) srcData).getPath(), true);
                    SpConnectionDesc conn = StorpoolUtil.getSpConnection(srcData.getDataStore().getUuid(), srcData.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);
                    try {
                        Long clusterId = StorPoolHelper.findClusterIdByGlobalId(snapName, clusterDao);
                        EndPoint ep = clusterId != null ? RemoteHostEndPoint.getHypervisorHostEndPoint(StorPoolHelper.findHostByCluster(clusterId, hostDao)) : selector.select(srcData, dstData);
                        if (ep == null) {
                            err = "No remote endpoint to send command, check if host or ssvm is down?";
                        } else {
                            answer = ep.sendMessage(cmd);
                            // if error during snapshot backup, cleanup the StorPool snapshot
                            if (answer != null && !answer.getResult()) {
                                StorpoolUtil.spLog(String.format("Error while backing-up snapshot '%s' - cleaning up StorPool snapshot. Error: %s", snapName, answer.getDetails()));
                                SpApiResponse resp = StorpoolUtil.snapshotDelete(snapName, conn);
                                if (resp.getError() != null) {
                                    final String err2 = String.format("Failed to cleanup StorPool snapshot '%s'. Error: %s.", snapName, resp.getError());
                                    log.error(err2);
                                    StorpoolUtil.spLog(err2);
                                }
                            }
                        }
                    } catch (CloudRuntimeException e) {
                        err = e.getMessage();
                    }
                }
            } else if (srcType == DataObjectType.VOLUME && dstType == DataObjectType.TEMPLATE) {
                // create template from volume
                VolumeObjectTO volume = (VolumeObjectTO) srcData.getTO();
                TemplateObjectTO template = (TemplateObjectTO) dstData.getTO();
                SpConnectionDesc conn = StorpoolUtil.getSpConnection(srcData.getDataStore().getUuid(), srcData.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);

                String volumeName = StorpoolStorageAdaptor.getVolumeNameFromPath(volume.getPath(), true);


                cmd = new StorpoolBackupTemplateFromSnapshotCommand(volume, template,
                        StorPoolHelper.getTimeout(StorPoolHelper.PrimaryStorageDownloadWait, configDao), VirtualMachineManager.ExecuteInSequence.value());

                try {
                    Long clusterId = StorPoolHelper.findClusterIdByGlobalId(volumeName, clusterDao);
                    EndPoint ep2 = clusterId != null ? RemoteHostEndPoint.getHypervisorHostEndPoint(StorPoolHelper.findHostByCluster(clusterId, hostDao)) : selector.select(srcData, dstData);
                    if (ep2 == null) {
                        err = "No remote endpoint to send command, check if host or ssvm is down?";
                    } else {
                        answer = ep2.sendMessage(cmd);
                        if (answer != null && answer.getResult()) {
                            SpApiResponse resSnapshot = StorpoolUtil.volumeSnapshot(volumeName, template.getUuid(), null, "template", "no", conn);
                            if (resSnapshot.getError() != null) {
                                log.debug(String.format("Could not snapshot volume with ID=%s", volume.getId()));
                                StorpoolUtil.spLog("Volume snapshot failed with error=%s", resSnapshot.getError().getDescr());
                                err = resSnapshot.getError().getDescr();
                            }
                            else {
                                StorPoolHelper.updateVmStoreTemplate(template.getId(), template.getDataStore().getRole(), StorpoolUtil.devPath(StorpoolUtil.getSnapshotNameFromResponse(resSnapshot, false, StorpoolUtil.GLOBAL_ID)), vmTemplateDataStoreDao);
                                vmTemplateDetailsDao.persist(new VMTemplateDetailVO(template.getId(), StorpoolUtil.SP_STORAGE_POOL_ID, String.valueOf(srcData.getDataStore().getId()), false));
                            }
                        }else {
                            err = "Could not copy template to secondary " + answer.getResult();
                        }
                    }
                }catch (CloudRuntimeException e) {
                    err = e.getMessage();
                }
            } else if (srcType == DataObjectType.TEMPLATE && dstType == DataObjectType.TEMPLATE) {
                // copy template to primary storage
                TemplateInfo tinfo = (TemplateInfo)dstData;
                Long size = tinfo.getSize();
                if(size == null || size == 0)
                    size = 1L*1024*1024*1024;
                SpConnectionDesc conn = StorpoolUtil.getSpConnection(dstData.getDataStore().getUuid(), dstData.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);

                TemplateDataStoreVO templDataStoreVO = vmTemplateDataStoreDao.findByTemplate(tinfo.getId(), DataStoreRole.Image);

                String snapshotName = (templDataStoreVO != null && templDataStoreVO.getLocalDownloadPath() != null)
                        ? StorpoolStorageAdaptor.getVolumeNameFromPath(templDataStoreVO.getLocalDownloadPath(), true)
                        : null;
                String name = tinfo.getUuid();

                SpApiResponse resp = null;
                if (snapshotName != null) {
                    //no need to copy volume from secondary, because we have it already on primary. Just need to create a child snapshot from it.
                    //The child snapshot is needed when configuration "storage.cleanup.enabled" is true, not to clean the base snapshot and to lose everything
                    resp = StorpoolUtil.volumeCreate(name, snapshotName, size, null, "no", "template", null, conn);
                    if (resp.getError() != null) {
                        err = String.format("Could not create Storpool volume for CS template %s. Error: %s", name, resp.getError());
                    } else {
                        String volumeNameToSnapshot = StorpoolUtil.getNameFromResponse(resp, true);
                        SpApiResponse resp2 = StorpoolUtil.volumeFreeze(volumeNameToSnapshot, conn);
                        if (resp2.getError() != null) {
                            err = String.format("Could not freeze Storpool volume %s. Error: %s", name, resp2.getError());
                        } else {
                            StorpoolUtil.spLog("Storpool snapshot [%s] for a template exists. Creating template on Storpool with name [%s]", tinfo.getUuid(), name);
                            TemplateObjectTO dstTO = (TemplateObjectTO) dstData.getTO();
                            dstTO.setPath(StorpoolUtil.devPath(StorpoolUtil.getNameFromResponse(resp, false)));
                            dstTO.setSize(size);
                            answer = new CopyCmdAnswer(dstTO);
                        }
                    }
                } else {
                    resp = StorpoolUtil.volumeCreate(name, null, size, null, "no", "template", null, conn);
                    if (resp.getError() != null) {
                        err = String.format("Could not create Storpool volume for CS template %s. Error: %s", name, resp.getError());
                    } else {
                        TemplateObjectTO dstTO = (TemplateObjectTO)dstData.getTO();
                        dstTO.setPath(StorpoolUtil.devPath(StorpoolUtil.getNameFromResponse(resp, false)));
                        dstTO.setSize(size);

                        cmd = new StorpoolDownloadTemplateCommand(srcData.getTO(), dstTO, StorPoolHelper.getTimeout(StorPoolHelper.PrimaryStorageDownloadWait, configDao), VirtualMachineManager.ExecuteInSequence.value(), "volume");

                        EndPoint ep = selector.select(srcData, dstData);
                        if (ep == null) {
                            err = "No remote endpoint to send command, check if host or ssvm is down?";
                        } else {
                            answer = ep.sendMessage(cmd);
                        }

                        if (answer != null && answer.getResult()) {
                            // successfully downloaded template to primary storage
                            SpApiResponse resp2 = StorpoolUtil.volumeFreeze(StorpoolUtil.getNameFromResponse(resp, true), conn);
                            if (resp2.getError() != null) {
                                err = String.format("Could not freeze Storpool volume %s. Error: %s", name, resp2.getError());
                            }
                        } else {
                            err = answer != null ? answer.getDetails() : "Unknown error while downloading template. Null answer returned.";
                        }
                    }
                }
                if (err != null) {
                    resp = StorpoolUtil.volumeDelete(StorpoolUtil.getNameFromResponse(resp, true), conn);
                    if (resp.getError() != null) {
                        log.warn(String.format("Could not clean-up Storpool volume %s. Error: %s", name, resp.getError()));
                    }
                }
            } else if (srcType == DataObjectType.TEMPLATE && dstType == DataObjectType.VOLUME) {
                // create volume from template on Storpool PRIMARY
                TemplateInfo tinfo = (TemplateInfo)srcData;

                VolumeInfo vinfo = (VolumeInfo)dstData;
                VMTemplateStoragePoolVO templStoragePoolVO = StorPoolHelper.findByPoolTemplate(vinfo.getPoolId(), tinfo.getId());
                final String parentName = templStoragePoolVO.getLocalDownloadPath() !=null ? StorpoolStorageAdaptor.getVolumeNameFromPath(templStoragePoolVO.getLocalDownloadPath(), true) : StorpoolStorageAdaptor.getVolumeNameFromPath(templStoragePoolVO.getInstallPath(), true);
                final String name = vinfo.getUuid();
                SpConnectionDesc conn = StorpoolUtil.getSpConnection(vinfo.getDataStore().getUuid(), vinfo.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);

                Long snapshotSize = StorpoolUtil.snapshotSize(parentName, conn);
                if (snapshotSize == null) {
                    err = String.format("Snapshot=%s does not exist on StorPool. Will recreate it first on primary", parentName);
                    vmTemplatePoolDao.remove(templStoragePoolVO.getId());
                }
                if (err == null) {
                    long size = vinfo.getSize();
                    if( size < snapshotSize )
                    {
                        StorpoolUtil.spLog(String.format("provided size is too small for snapshot. Provided %d, snapshot %d. Using snapshot size", size, snapshotSize));
                        size = snapshotSize;
                    }
                    StorpoolUtil.spLog(String.format("volume size is: %d", size));
                    Long vmId = vinfo.getInstanceId();
                    SpApiResponse resp = StorpoolUtil.volumeCreate(name, parentName, size, getVMInstanceUUID(vmId),
                            getVcPolicyTag(vmId), "volume", vinfo.getMaxIops(), conn);
                    if (resp.getError() == null) {
                        updateStoragePool(dstData.getDataStore().getId(), vinfo.getSize());

                        VolumeObjectTO to = (VolumeObjectTO) vinfo.getTO();
                        to.setSize(vinfo.getSize());
                        to.setPath(StorpoolUtil.devPath(StorpoolUtil.getNameFromResponse(resp, false)));

                        answer = new CopyCmdAnswer(to);
                    } else {
                        err = String.format("Could not create Storpool volume %s. Error: %s", name, resp.getError());
                    }
                }
            } else if (srcType == DataObjectType.VOLUME && dstType == DataObjectType.VOLUME) {
                StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriver.copyAsync src Data Store=%s", srcData.getDataStore().getDriver());
                VolumeInfo dstInfo = (VolumeInfo)dstData;
                VolumeInfo srcInfo = (VolumeInfo) srcData;

                if( !(srcData.getDataStore().getDriver() instanceof StorpoolPrimaryDataStoreDriver ) ) {
                    // copy "VOLUME" to primary storage
                    String name = dstInfo.getUuid();
                    Long size = dstInfo.getSize();
                    if(size == null || size == 0)
                        size = 1L*1024*1024*1024;
                    SpConnectionDesc conn = StorpoolUtil.getSpConnection(dstData.getDataStore().getUuid(), dstData.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);
                    Long vmId = srcInfo.getInstanceId();

                    SpApiResponse resp = StorpoolUtil.volumeCreate(name, null, size, getVMInstanceUUID(vmId), getVcPolicyTag(vmId), "volume", dstInfo.getMaxIops(), conn);
                    if (resp.getError() != null) {
                        err = String.format("Could not create Storpool volume for CS template %s. Error: %s", name, resp.getError());
                    } else {
                        //updateVolume(dstData.getId());
                        VolumeObjectTO dstTO = (VolumeObjectTO)dstData.getTO();
                        dstTO.setPath(StorpoolUtil.devPath(StorpoolUtil.getNameFromResponse(resp, false)));
                        dstTO.setSize(size);

                        cmd = new StorpoolDownloadVolumeCommand(srcData.getTO(), dstTO, StorPoolHelper.getTimeout(StorPoolHelper.PrimaryStorageDownloadWait, configDao), VirtualMachineManager.ExecuteInSequence.value());

                        EndPoint ep = selector.select(srcData, dstData);

                        if( ep == null) {
                            StorpoolUtil.spLog("select(srcData, dstData) returned NULL. trying srcOnly");
                            ep = selector.select(srcData); // Storpool is zone
                        }
                        if (ep == null) {
                            err = "No remote endpoint to send command, check if host or ssvm is down?";
                        } else {
                            StorpoolUtil.spLog("Sending command to %s", ep.getHostAddr());
                            answer = ep.sendMessage(cmd);

                            if (answer != null && answer.getResult()) {
                                // successfully downloaded volume to primary storage
                            } else {
                                err = answer != null ? answer.getDetails() : "Unknown error while downloading volume. Null answer returned.";
                            }
                        }

                        if (err != null) {
                            SpApiResponse resp3 = StorpoolUtil.volumeDelete(name, conn);
                            if (resp3.getError() != null) {
                               log.warn(String.format("Could not clean-up Storpool volume %s. Error: %s", name, resp3.getError()));
                            }
                        }
                    }
                } else {
                    // download volume - first copies to secondary
                    VolumeObjectTO srcTO = (VolumeObjectTO)srcData.getTO();
                    StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.copyAsnc SRC path=%s ", srcTO.getPath());
                    StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.copyAsnc DST canonicalName=%s ", dstData.getDataStore().getClass().getCanonicalName());
                    PrimaryDataStoreTO checkStoragePool = dstData.getTO().getDataStore() instanceof PrimaryDataStoreTO ? (PrimaryDataStoreTO)dstData.getTO().getDataStore() : null;
                    final String name = StorpoolStorageAdaptor.getVolumeNameFromPath(srcTO.getPath(), true);
                    StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.copyAsnc DST tmpSnapName=%s ,srcUUID=%s", name, srcTO.getUuid());

                    if (checkStoragePool != null && checkStoragePool.getPoolType().equals(StoragePoolType.SharedMountPoint)) {
                        SpConnectionDesc conn = StorpoolUtil.getSpConnection(dstData.getDataStore().getUuid(), dstData.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);
                        String baseOn = StorpoolStorageAdaptor.getVolumeNameFromPath(srcTO.getPath(), true);
                        //uuid tag will be the same as srcData.uuid
                        String volumeName = srcData.getUuid();
                        StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.copyAsnc volumeName=%s, baseOn=%s", volumeName, baseOn);
                        final SpApiResponse response = StorpoolUtil.volumeCopy(volumeName, baseOn, "volume", srcInfo.getMaxIops(), conn);
                        srcTO.setSize(srcData.getSize());
                        srcTO.setPath(StorpoolUtil.devPath(StorpoolUtil.getNameFromResponse(response, false)));
                        StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.copyAsnc DST to=%s", srcTO);

                        answer = new CopyCmdAnswer(srcTO);
                    } else {
                        SpConnectionDesc conn = StorpoolUtil.getSpConnection(srcData.getDataStore().getUuid(), srcData.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);
                        final SpApiResponse resp = StorpoolUtil.volumeSnapshot(name, srcTO.getUuid(), srcInfo.getInstanceId() != null ? getVMInstanceUUID(srcInfo.getInstanceId()) : null, "temporary", null, conn);
                        String snapshotName = StorpoolUtil.getSnapshotNameFromResponse(resp, true, StorpoolUtil.GLOBAL_ID);
                        if (resp.getError() == null) {
                            srcTO.setPath(StorpoolUtil.devPath(
                                    StorpoolUtil.getSnapshotNameFromResponse(resp, false, StorpoolUtil.GLOBAL_ID)));

                            cmd = new StorpoolCopyVolumeToSecondaryCommand(srcTO, dstData.getTO(), StorPoolHelper.getTimeout(StorPoolHelper.CopyVolumeWait, configDao), VirtualMachineManager.ExecuteInSequence.value());

                            StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.copyAsnc command=%s ", cmd);

                            try {
                                Long clusterId = StorPoolHelper.findClusterIdByGlobalId(snapshotName, clusterDao);
                                EndPoint ep = clusterId != null ? RemoteHostEndPoint.getHypervisorHostEndPoint(StorPoolHelper.findHostByCluster(clusterId, hostDao)) : selector.select(srcData, dstData);
                                StorpoolUtil.spLog("selector.select(srcData, dstData) ", ep);
                                if (ep == null) {
                                    ep = selector.select(dstData);
                                    StorpoolUtil.spLog("selector.select(srcData) ", ep);
                                }

                                if (ep == null) {
                                    err = "No remote endpoint to send command, check if host or ssvm is down?";
                                } else {
                                    answer = ep.sendMessage(cmd);
                                    StorpoolUtil.spLog("Answer: details=%s, result=%s", answer.getDetails(), answer.getResult());
                                }
                            } catch (CloudRuntimeException e) {
                                err = e.getMessage();
                            }
                        } else {
                            err = String.format("Failed to create temporary StorPool snapshot while trying to download volume %s (uuid %s). Error: %s", srcTO.getName(), srcTO.getUuid(), resp.getError());
                        }
                        final SpApiResponse resp2 = StorpoolUtil.snapshotDelete(snapshotName, conn);
                        if (resp2.getError() != null) {
                            final String err2 = String.format("Failed to delete temporary StorPool snapshot %s. Error: %s", StorpoolUtil.getNameFromResponse(resp, true), resp2.getError());
                            log.error(err2);
                            StorpoolUtil.spLog(err2);
                        }
                    }
                }
            } else {
                err = String.format("Unsupported copy operation from %s (type %s) to %s (type %s)", srcData.getUuid(), srcType, dstData.getUuid(), dstType);
            }
        } catch (Exception e) {
            StorpoolUtil.spLog("Caught exception: %s", e.toString());
            err = e.toString();
        }

        if (answer != null && !answer.getResult()) {
            err = answer.getDetails();
        }

        if (err != null) {
            StorpoolUtil.spLog("Failed due to %s", err);

            log.error(err);
            answer = new Answer(cmd, false, err);
        }

        CopyCommandResult res = new CopyCommandResult(null, answer);
        res.setResult(err);
        callback.complete(res);
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        String snapshotName = snapshot.getUuid();
        VolumeInfo vinfo = snapshot.getBaseVolume();
        String volumeName = StorpoolStorageAdaptor.getVolumeNameFromPath(vinfo.getPath(), true);
        SpConnectionDesc conn = StorpoolUtil.getSpConnection(vinfo.getDataStore().getUuid(), vinfo.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);
        Long vmId = vinfo.getInstanceId();
        if (volumeName != null) {
            StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriver.takeSnapshot volumename=%s vmInstance=%s",volumeName, vmId);
        }else {
            throw new UnsupportedOperationException("The path should be: " + StorpoolUtil.SP_DEV_PATH);
        }

        CreateObjectAnswer answer = null;
        String err = null;

        SpApiResponse resp = StorpoolUtil.volumeSnapshot(volumeName, snapshotName, vmId != null ? getVMInstanceUUID(vmId) : null, "snapshot", null, conn);

        if (resp.getError() != null) {
            err = String.format("Could not snapshot StorPool volume %s. Error %s", volumeName, resp.getError());
            answer = new CreateObjectAnswer(err);
        } else {
            String name = StorpoolUtil.getSnapshotNameFromResponse(resp, true, StorpoolUtil.GLOBAL_ID);
            SnapshotObjectTO snapTo = (SnapshotObjectTO)snapshot.getTO();
            snapTo.setPath(StorpoolUtil.devPath(name.split("~")[1]));
            answer = new CreateObjectAnswer(snapTo);
            StorPoolHelper.addSnapshotDetails(snapshot.getId(), snapshot.getUuid(), snapTo.getPath(), _snapshotDetailsDao);
            //add primary storage of snapshot
            StorPoolHelper.addSnapshotDetails(snapshot.getId(), StorpoolUtil.SP_STORAGE_POOL_ID, String.valueOf(snapshot.getDataStore().getId()), _snapshotDetailsDao);
            StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.takeSnapshot: snapshot: name=%s, uuid=%s, volume: name=%s, uuid=%s", name, snapshot.getUuid(), volumeName, vinfo.getUuid());
        }

        CreateCmdResult res = new CreateCmdResult(null, answer);
        res.setResult(err);
        callback.complete(res);
    }

    @Override
    public void revertSnapshot(final SnapshotInfo snapshot, final SnapshotInfo snapshotOnPrimaryStore, final AsyncCompletionCallback<CommandResult> callback) {
        final VolumeInfo vinfo = snapshot.getBaseVolume();
        final String snapshotName = StorPoolHelper.getSnapshotName(snapshot.getId(), snapshot.getUuid(), snapshotDataStoreDao, _snapshotDetailsDao);
        final String volumeName = StorpoolStorageAdaptor.getVolumeNameFromPath(vinfo.getPath(), true);
        StorpoolUtil.spLog("StorpoolPrimaryDataStoreDriverImpl.revertSnapshot: snapshot: name=%s, uuid=%s, volume: name=%s, uuid=%s", snapshotName, snapshot.getUuid(), volumeName, vinfo.getUuid());
        SpConnectionDesc conn = StorpoolUtil.getSpConnection(vinfo.getDataStore().getUuid(), vinfo.getDataStore().getId(), storagePoolDetailsDao, primaryStoreDao);
        String err = null;

        VolumeDetailVO detail = volumeDetailsDao.findDetail(vinfo.getId(), StorpoolUtil.SP_PROVIDER_NAME);
        if (detail != null) {
            //Rename volume to its global id only if it was migrated from UUID to global id
            SpApiResponse updateVolumeResponse = StorpoolUtil.volumeUpdateRename(StorpoolStorageAdaptor.getVolumeNameFromPath(vinfo.getPath(), true), "", StorpoolStorageAdaptor.getVolumeNameFromPath(detail.getValue(), false), conn);

            if (updateVolumeResponse.getError() != null) {
                StorpoolUtil.spLog("Could not update StorPool's volume %s to it's globalId due to %s", StorpoolStorageAdaptor.getVolumeNameFromPath(vinfo.getPath(), true), updateVolumeResponse.getError().getDescr());
                err = String.format("Could not update StorPool's volume %s to it's globalId due to %s", StorpoolStorageAdaptor.getVolumeNameFromPath(vinfo.getPath(), true), updateVolumeResponse.getError().getDescr());
                completeResponse(err, callback);
                return;
            }
            volumeDetailsDao.remove(detail.getId());
        }

        SpApiResponse resp = StorpoolUtil.detachAllForced(volumeName, false, conn);
        if (resp.getError() != null) {
            err = String.format("Could not detach StorPool volume %s due to %s", volumeName, resp.getError());
            completeResponse(err, callback);
            return;
        }
        SpApiResponse response = StorpoolUtil.volumeRevert(volumeName, snapshotName, conn);
        if (response.getError() != null) {
            err = String.format(
                    "Could not revert StorPool volume %s to the %s snapshot: could not create the new volume: error %s",
                    volumeName, snapshotName, response.getError());
            completeResponse(err, callback);
            return;
        }

        if (vinfo.getMaxIops() != null) {
            response = StorpoolUtil.volumeUpadateTags(volumeName, null, vinfo.getMaxIops(), conn, null);
            if (response.getError() != null) {
                StorpoolUtil.spLog("Volume was reverted successfully but max iops could not be set due to %s", response.getError().getDescr());
            }
        }

        final VolumeObjectTO to = (VolumeObjectTO)vinfo.getTO();
        completeResponse(to, callback);
    }

    private String getVcPolicyTag(Long vmId) {
        ResourceTag resourceTag = vmId != null ? _resourceTagDao.findByKey(vmId, ResourceObjectType.UserVm, StorpoolUtil.SP_VC_POLICY) : null;
        return resourceTag != null ? resourceTag.getValue() : null;
    }

    public void handleQualityOfServiceForVolumeMigration(VolumeInfo arg0, QualityOfServiceState arg1) {
        StorpoolUtil.spLog("handleQualityOfServiceForVolumeMigration with volume name=%s", arg0.getName());
    }


    public void copyAsync(DataObject srcData, DataObject destData, Host destHost,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        copyAsync(srcData, destData, callback);
    }

    public boolean canProvideStorageStats() {
        return false;
    }

    public Pair<Long, Long> getStorageStats(StoragePool storagePool) {
        return null;
    }

    public boolean canProvideVolumeStats() {
        return false;
    }

    public Pair<Long, Long> getVolumeStats(StoragePool storagePool, String volumeId) {
        return null;
    }

    public boolean canHostAccessStoragePool(Host host, StoragePool pool) {
        return false;
    }

    @Override
    public boolean vmInfoNeeded() {
        return true;
    }

    @Override
    public void provideVMInfo(long vmId, long volumeId) {
        VolumeVO volume = volumeDao.findById(volumeId);
        StoragePoolVO poolVO = primaryStoreDao.findById(volume.getPoolId());
        if (poolVO != null) {
            SpConnectionDesc conn = StorpoolUtil.getSpConnection(poolVO.getUuid(), poolVO.getId(), storagePoolDetailsDao, primaryStoreDao);
            String volName = StorpoolStorageAdaptor.getVolumeNameFromPath(volume.getPath(), true);
            VMInstanceVO userVM = vmInstanceDao.findById(vmId);
            String vcPolicy = getVcPolicyTag(vmId);
            SpApiResponse resp = StorpoolUtil.volumeUpadateTags(volName, userVM.getId() == volume.getInstanceId() ? userVM.getUuid() : "", null, conn, vcPolicy);
            if (resp.getError() != null) {
                log.warn(String.format("Could not update VC policy tags of a volume with id [%s]", volume.getUuid()));
            }
        }
    }

    @Override
    public boolean vmTagsNeeded(String tagKey) {
        return tagKey != null && tagKey.equals(StorpoolUtil.SP_VC_POLICY);
    }

    @Override
    public void provideVMTags(long vmId, long volumeId, String tagValue) {
        VolumeVO volume = volumeDao.findById(volumeId);
        StoragePoolVO poolVO = primaryStoreDao.findById(volume.getPoolId());
        if (poolVO != null) {
            SpConnectionDesc conn = StorpoolUtil.getSpConnection(poolVO.getUuid(), poolVO.getId(), storagePoolDetailsDao, primaryStoreDao);
            String volName = StorpoolStorageAdaptor.getVolumeNameFromPath(volume.getPath(), true);
            SpApiResponse resp = StorpoolUtil.volumeUpadateVCTags(volName, conn, tagValue);
            if (resp.getError() != null) {
                log.warn(String.format("Could not update VC policy tags of a volume with id [%s]", volume.getUuid()));
            }
        }
    }
}
