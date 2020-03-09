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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageAction;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.RevertSnapshotCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.volume.VolumeObject;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.vm.dao.VMInstanceDao;

public class CloudStackPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    @Override
    public Map<String, String> getCapabilities() {
        Map<String, String> caps = new HashMap<String, String>();
        caps.put(DataStoreCapabilities.VOLUME_SNAPSHOT_QUIESCEVM.toString(), Boolean.FALSE.toString());
        return caps;
    }

    private static final Logger s_logger = Logger.getLogger(CloudStackPrimaryDataStoreDriverImpl.class);
    @Inject
    DiskOfferingDao diskOfferingDao;
    @Inject
    VMTemplateDao templateDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    HostDao hostDao;
    @Inject
    StorageManager storageMgr;
    @Inject
    VMInstanceDao vmDao;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    PrimaryDataStoreDao primaryStoreDao;
    @Inject
    SnapshotManager snapshotMgr;
    @Inject
    EndPointSelector epSelector;
    @Inject
    ConfigurationDao configDao;
    @Inject
    TemplateManager templateManager;
    @Inject
    TemplateDataFactory templateDataFactory;

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    public Answer createVolume(VolumeInfo volume) throws StorageUnavailableException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating volume: " + volume);
        }

        CreateObjectCommand cmd = new CreateObjectCommand(volume.getTO());
        EndPoint ep = epSelector.select(volume);
        Answer answer = null;
        if (ep == null) {
            String errMsg = "No remote endpoint to send DeleteCommand, check if host or ssvm is down?";
            s_logger.error(errMsg);
            answer = new Answer(cmd, false, errMsg);
        } else {
            answer = ep.sendMessage(cmd);
        }
        return answer;
    }

    @Override
    public ChapInfo getChapInfo(DataObject dataObject) {
        return null;
    }

    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) {
        return false;
    }

    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {
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
    public long getDataObjectSizeIncludingHypervisorSnapshotReserve(DataObject dataObject, StoragePool pool) {
        return dataObject.getSize();
    }

    @Override
    public long getBytesRequiredForTemplate(TemplateInfo templateInfo, StoragePool storagePool) {
        return 0;
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        String errMsg = null;
        Answer answer = null;
        CreateCmdResult result = new CreateCmdResult(null, null);
        if (data.getType() == DataObjectType.VOLUME) {
            try {
                answer = createVolume((VolumeInfo) data);
                if ((answer == null) || (!answer.getResult())) {
                    result.setSuccess(false);
                    if (answer != null) {
                        result.setResult(answer.getDetails());
                    }
                } else {
                    result.setAnswer(answer);
                }
            } catch (StorageUnavailableException e) {
                s_logger.debug("failed to create volume", e);
                errMsg = e.toString();
            } catch (Exception e) {
                s_logger.debug("failed to create volume", e);
                errMsg = e.toString();
            }
        }
        if (errMsg != null) {
            result.setResult(errMsg);
        }

        if (callback != null) {
            callback.complete(result);
        }
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        DeleteCommand cmd = new DeleteCommand(data.getTO());

        CommandResult result = new CommandResult();
        try {
            EndPoint ep = null;
            if (data.getType() == DataObjectType.VOLUME) {
                ep = epSelector.select(data, StorageAction.DELETEVOLUME);
            } else {
                ep = epSelector.select(data);
            }
            if (ep == null) {
                String errMsg = "No remote endpoint to send DeleteCommand, check if host or ssvm is down?";
                s_logger.error(errMsg);
                result.setResult(errMsg);
            } else {
                Answer answer = ep.sendMessage(cmd);
                if (answer != null && !answer.getResult()) {
                    result.setResult(answer.getDetails());
                }
            }
        } catch (Exception ex) {
            s_logger.debug("Unable to destoy volume" + data.getId(), ex);
            result.setResult(ex.toString());
        }
        callback.complete(result);
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        DataStore store = destData.getDataStore();
        if (store.getRole() == DataStoreRole.Primary) {
            if ((srcdata.getType() == DataObjectType.TEMPLATE && destData.getType() == DataObjectType.TEMPLATE)) {
                //For CLVM, we need to copy template to primary storage at all, just fake the copy result.
                TemplateObjectTO templateObjectTO = new TemplateObjectTO();
                templateObjectTO.setPath(UUID.randomUUID().toString());
                templateObjectTO.setSize(srcdata.getSize());
                templateObjectTO.setPhysicalSize(srcdata.getSize());
                templateObjectTO.setFormat(Storage.ImageFormat.RAW);
                CopyCmdAnswer answer = new CopyCmdAnswer(templateObjectTO);
                CopyCommandResult result = new CopyCommandResult("", answer);
                callback.complete(result);
            } else if (srcdata.getType() == DataObjectType.TEMPLATE && destData.getType() == DataObjectType.VOLUME) {
                //For CLVM, we need to pass template on secondary storage to hypervisor
                int primaryStorageDownloadWait = StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value();
                StoragePoolVO storagePoolVO = primaryStoreDao.findById(store.getId());
                DataStore imageStore = templateManager.getImageStore(storagePoolVO.getDataCenterId(), srcdata.getId());
                DataObject srcData = templateDataFactory.getTemplate(srcdata.getId(), imageStore);

                CopyCommand cmd = new CopyCommand(srcData.getTO(), destData.getTO(), primaryStorageDownloadWait, true);
                EndPoint ep = epSelector.select(srcData, destData);
                Answer answer = null;
                if (ep == null) {
                    String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
                    s_logger.error(errMsg);
                    answer = new Answer(cmd, false, errMsg);
                } else {
                    answer = ep.sendMessage(cmd);
                }
                CopyCommandResult result = new CopyCommandResult("", answer);
                callback.complete(result);
            }
        }
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        //BUG fix for CLOUDSTACK-4618
        DataStore store = destData.getDataStore();
        if (store.getRole() == DataStoreRole.Primary && srcData.getType() == DataObjectType.TEMPLATE
                && (destData.getType() == DataObjectType.TEMPLATE || destData.getType() == DataObjectType.VOLUME)) {
            StoragePoolVO storagePoolVO = primaryStoreDao.findById(store.getId());
            if (storagePoolVO != null && storagePoolVO.getPoolType() == Storage.StoragePoolType.CLVM) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateCmdResult result = null;
        try {
            SnapshotObjectTO snapshotTO = (SnapshotObjectTO) snapshot.getTO();
            Object payload = snapshot.getPayload();
            if (payload != null && payload instanceof CreateSnapshotPayload) {
                CreateSnapshotPayload snapshotPayload = (CreateSnapshotPayload) payload;
                snapshotTO.setQuiescevm(snapshotPayload.getQuiescevm());
            }

            CreateObjectCommand cmd = new CreateObjectCommand(snapshotTO);
            EndPoint ep = epSelector.select(snapshot, StorageAction.TAKESNAPSHOT);
            Answer answer = null;

            if (ep == null) {
                String errMsg = "No remote endpoint to send createObjectCommand, check if host or ssvm is down?";
                s_logger.error(errMsg);
                answer = new Answer(cmd, false, errMsg);
            } else {
                answer = ep.sendMessage(cmd);
            }

            result = new CreateCmdResult(null, answer);
            if (answer != null && !answer.getResult()) {
                result.setResult(answer.getDetails());
            }

            callback.complete(result);
            return;
        } catch (Exception e) {
            s_logger.debug("Failed to take snapshot: " + snapshot.getId(), e);
            result = new CreateCmdResult(null, null);
            result.setResult(e.toString());
        }
        callback.complete(result);
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, SnapshotInfo snapshotOnPrimaryStore, AsyncCompletionCallback<CommandResult> callback) {
        SnapshotObjectTO snapshotTO = (SnapshotObjectTO)snapshot.getTO();
        RevertSnapshotCommand cmd = new RevertSnapshotCommand(snapshotTO);

        CommandResult result = new CommandResult();
        try {
            EndPoint ep = epSelector.select(snapshotOnPrimaryStore);
            if ( ep == null ){
                String errMsg = "No remote endpoint to send RevertSnapshotCommand, check if host or ssvm is down?";
                s_logger.error(errMsg);
                result.setResult(errMsg);
            } else {
                Answer answer = ep.sendMessage(cmd);
                if (answer != null && !answer.getResult()) {
                    result.setResult(answer.getDetails());
                }
            }
        } catch (Exception ex) {
            s_logger.debug("Unable to revert snapshot " + snapshot.getId(), ex);
            result.setResult(ex.toString());
        }
        callback.complete(result);
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        VolumeObject vol = (VolumeObject) data;
        StoragePool pool = (StoragePool) data.getDataStore();
        ResizeVolumePayload resizeParameter = (ResizeVolumePayload) vol.getpayload();

        ResizeVolumeCommand resizeCmd =
                new ResizeVolumeCommand(vol.getPath(), new StorageFilerTO(pool), vol.getSize(), resizeParameter.newSize, resizeParameter.shrinkOk,
                        resizeParameter.instanceName);
        CreateCmdResult result = new CreateCmdResult(null, null);
        try {
            ResizeVolumeAnswer answer = (ResizeVolumeAnswer) storageMgr.sendToPool(pool, resizeParameter.hosts, resizeCmd);
            if (answer != null && answer.getResult()) {
                long finalSize = answer.getNewSize();
                s_logger.debug("Resize: volume started at size " + vol.getSize() + " and ended at size " + finalSize);

                vol.setSize(finalSize);
                vol.update();
            } else if (answer != null) {
                result.setResult(answer.getDetails());
            } else {
                s_logger.debug("return a null answer, mark it as failed for unknown reason");
                result.setResult("return a null answer, mark it as failed for unknown reason");
            }

        } catch (Exception e) {
            s_logger.debug("sending resize command failed", e);
            result.setResult(e.toString());
        }

        callback.complete(result);
    }

    @Override
    public void handleQualityOfServiceForVolumeMigration(VolumeInfo volumeInfo, QualityOfServiceState qualityOfServiceState) {}
}
