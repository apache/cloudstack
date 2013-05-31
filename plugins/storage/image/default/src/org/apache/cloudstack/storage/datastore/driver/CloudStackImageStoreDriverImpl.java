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

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.image.ImageStoreDriver;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.cloudstack.storage.snapshot.SnapshotObject;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteSnapshotBackupCommand2;
import com.cloud.agent.api.storage.DeleteTemplateCommand;
import com.cloud.agent.api.storage.DeleteVolumeCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class CloudStackImageStoreDriverImpl implements ImageStoreDriver {
    private static final Logger s_logger = Logger.getLogger(CloudStackImageStoreDriverImpl.class);
    @Inject
    VMTemplateZoneDao templateZoneDao;
    @Inject
    VMTemplateDao templateDao;
    @Inject
    DownloadMonitor _downloadMonitor;
    @Inject
    VolumeDao volumeDao;
    @Inject
    VolumeDataStoreDao _volumeStoreDao;
    @Inject
    HostDao hostDao;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    AgentManager agentMgr;
    @Inject
    SnapshotManager snapshotMgr;
    @Inject
    AccountDao _accountDao;
    @Inject
    SecondaryStorageVmManager _ssvmMgr;
    @Inject
    TemplateDataStoreDao _templateStoreDao;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    DataStoreManager _dataStoreMgr;

    @Override
    public String grantAccess(DataObject data, EndPoint ep) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        ImageStoreImpl nfsStore = (ImageStoreImpl) store;
        NfsTO nfsTO = new NfsTO();
        nfsTO.setRole(store.getRole());
        nfsTO.setUrl(nfsStore.getUri());
        return nfsTO;
    }

    @Override
    public boolean revokeAccess(DataObject data, EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<DataObject> listObjects(DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }

    class CreateContext<T> extends AsyncRpcConext<T> {
        final DataObject data;

        public CreateContext(AsyncCompletionCallback<T> callback, DataObject data) {
            super(callback);
            this.data = data;
        }
    }

    @Override
    public void createAsync(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateContext<CreateCmdResult> context = new CreateContext<CreateCmdResult>(callback, data);
        AsyncCallbackDispatcher<CloudStackImageStoreDriverImpl, DownloadAnswer> caller = AsyncCallbackDispatcher
                .create(this);
        caller.setContext(context);
        if (data.getType() == DataObjectType.TEMPLATE) {
            caller.setCallback(caller.getTarget().createTemplateAsyncCallback(null, null));
            _downloadMonitor.downloadTemplateToStorage(data, caller);
        } else if (data.getType() == DataObjectType.VOLUME) {
            caller.setCallback(caller.getTarget().createVolumeAsyncCallback(null, null));
            _downloadMonitor.downloadVolumeToStorage(data, caller);
        }
    }

    protected Void createTemplateAsyncCallback(AsyncCallbackDispatcher<CloudStackImageStoreDriverImpl, DownloadAnswer> callback,
            CreateContext<CreateCmdResult> context) {
        DownloadAnswer answer = callback.getResult();
        DataObject obj = context.data;
        DataStore store = obj.getDataStore();

        TemplateDataStoreVO tmpltStoreVO = _templateStoreDao.findByStoreTemplate(store.getId(), obj.getId());
        if (tmpltStoreVO != null) {
            TemplateDataStoreVO updateBuilder = _templateStoreDao.createForUpdate();
            updateBuilder.setDownloadPercent(answer.getDownloadPct());
            updateBuilder.setDownloadState(answer.getDownloadStatus());
            updateBuilder.setLastUpdated(new Date());
            updateBuilder.setErrorString(answer.getErrorString());
            updateBuilder.setJobId(answer.getJobId());
            updateBuilder.setLocalDownloadPath(answer.getDownloadPath());
            updateBuilder.setInstallPath(answer.getInstallPath());
            updateBuilder.setSize(answer.getTemplateSize());
            updateBuilder.setPhysicalSize(answer.getTemplatePhySicalSize());
            _templateStoreDao.update(tmpltStoreVO.getId(), updateBuilder);
            // update size in vm_template table
            VMTemplateVO tmlptUpdater = templateDao.createForUpdate();
            tmlptUpdater.setSize(answer.getTemplateSize());
            templateDao.update(obj.getId(), tmlptUpdater);
        }

        AsyncCompletionCallback<CreateCmdResult> caller = context.getParentCallback();

        if (answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR
                || answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.ABANDONED
                || answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.UNKNOWN) {
            CreateCmdResult result = new CreateCmdResult(null, null);
            result.setSuccess(false);
            result.setResult(answer.getErrorString());
            caller.complete(result);
        } else if (answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
            if (answer.getCheckSum() != null) {
                VMTemplateVO templateDaoBuilder = templateDao.createForUpdate();
                templateDaoBuilder.setChecksum(answer.getCheckSum());
                templateDao.update(obj.getId(), templateDaoBuilder);
            }

            CreateCmdResult result = new CreateCmdResult(null, null);
            caller.complete(result);
        }
        return null;
    }

    protected Void createVolumeAsyncCallback(AsyncCallbackDispatcher<CloudStackImageStoreDriverImpl, DownloadAnswer> callback,
            CreateContext<CreateCmdResult> context) {
        DownloadAnswer answer = callback.getResult();
        DataObject obj = context.data;
        DataStore store = obj.getDataStore();

        VolumeDataStoreVO volStoreVO = _volumeStoreDao.findByStoreVolume(store.getId(), obj.getId());
        if (volStoreVO != null) {
            VolumeDataStoreVO updateBuilder = _volumeStoreDao.createForUpdate();
            updateBuilder.setDownloadPercent(answer.getDownloadPct());
            updateBuilder.setDownloadState(answer.getDownloadStatus());
            updateBuilder.setLastUpdated(new Date());
            updateBuilder.setErrorString(answer.getErrorString());
            updateBuilder.setJobId(answer.getJobId());
            updateBuilder.setLocalDownloadPath(answer.getDownloadPath());
            updateBuilder.setInstallPath(answer.getInstallPath());
            updateBuilder.setSize(answer.getTemplateSize());
            updateBuilder.setPhysicalSize(answer.getTemplatePhySicalSize());
            _volumeStoreDao.update(volStoreVO.getId(), updateBuilder);
            // update size in volume table
            VolumeVO volUpdater = volumeDao.createForUpdate();
            volUpdater.setSize(answer.getTemplateSize());
            volumeDao.update(obj.getId(), volUpdater);
        }

        AsyncCompletionCallback<CreateCmdResult> caller = context.getParentCallback();

        if (answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR
                || answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.ABANDONED
                || answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.UNKNOWN) {
            CreateCmdResult result = new CreateCmdResult(null, null);
            result.setSuccess(false);
            result.setResult(answer.getErrorString());
            caller.complete(result);
        } else if (answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
            CreateCmdResult result = new CreateCmdResult(null, null);
            caller.complete(result);
        }
        return null;
    }

    private void deleteVolume(DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        VolumeVO vol = volumeDao.findById(data.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Expunging " + vol);
        }

        // Find out if the volume is present on secondary storage
        VolumeDataStoreVO volumeStore = _volumeStoreDao.findByVolume(vol.getId());
        if (volumeStore != null) {
            if (volumeStore.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                DataStore store = _dataStoreMgr.getDataStore(volumeStore.getDataStoreId(), DataStoreRole.Image);
                EndPoint ep = _epSelector.select(store);
                DeleteVolumeCommand dtCommand = new DeleteVolumeCommand(store.getTO(), volumeStore.getVolumeId(),
                        volumeStore.getInstallPath());
                Answer answer = ep.sendMessage(dtCommand);
                if (answer == null || !answer.getResult()) {
                    s_logger.debug("Failed to delete " + volumeStore + " due to "
                            + ((answer == null) ? "answer is null" : answer.getDetails()));
                    return;
                }
            } else if (volumeStore.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
                s_logger.debug("Volume: " + vol.getName() + " is currently being uploaded; cant' delete it.");
                throw new CloudRuntimeException("Please specify a volume that is not currently being uploaded.");
            }

            CommandResult result = new CommandResult();
            callback.complete(result);
            return;
        }
    }

    private void deleteTemplate(DataObject data, AsyncCompletionCallback<CommandResult> callback) {

        TemplateObject templateObj = (TemplateObject) data;
        VMTemplateVO template = templateObj.getImage();
        ImageStoreImpl store = (ImageStoreImpl) templateObj.getDataStore();
        long storeId = store.getId();
        Long sZoneId = store.getDataCenterId();
        long templateId = template.getId();

        Account account = _accountDao.findByIdIncludingRemoved(template.getAccountId());
        String eventType = "";

        if (template.getFormat().equals(ImageFormat.ISO)) {
            eventType = EventTypes.EVENT_ISO_DELETE;
        } else {
            eventType = EventTypes.EVENT_TEMPLATE_DELETE;
        }

        // TODO: need to understand why we need to mark destroyed in
        // template_store_ref table here instead of in callback.
        // Currently I did that in callback, so I removed previous code to mark
        // template_host_ref
        if (sZoneId != null) {
            UsageEventUtils.publishUsageEvent(eventType, account.getId(), sZoneId, templateId, null, null, null);
        }

        // get installpath of this template on image store
        TemplateDataStoreVO tmplStore = _templateStoreDao.findByStoreTemplate(storeId, templateId);
        String installPath = tmplStore.getInstallPath();
        if (installPath != null) {
            DeleteTemplateCommand cmd = new DeleteTemplateCommand(store.getTO(), installPath, template.getId(),
                    template.getAccountId());
            EndPoint ep = _epSelector.select(templateObj);
            Answer answer = ep.sendMessage(cmd);

            if (answer == null || !answer.getResult()) {
                s_logger.debug("Failed to deleted template at store: " + store.getName());
                CommandResult result = new CommandResult();
                result.setSuccess(false);
                result.setResult("Delete template failed");
                callback.complete(result);

            } else {
                s_logger.debug("Deleted template at: " + installPath);
                CommandResult result = new CommandResult();
                result.setSuccess(true);
                callback.complete(result);
            }

            List<VMTemplateZoneVO> templateZones = templateZoneDao.listByZoneTemplate(sZoneId, templateId);
            if (templateZones != null) {
                for (VMTemplateZoneVO templateZone : templateZones) {
                    templateZoneDao.remove(templateZone.getId());
                }
            }
        }

    }

    private void deleteSnapshot(DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        SnapshotObject snapshotObj = (SnapshotObject) data;
        DataStore secStore = snapshotObj.getDataStore();
        CommandResult result = new CommandResult();
        SnapshotVO snapshot = snapshotObj.getSnapshotVO();

        if (snapshot == null) {
            s_logger.debug("Destroying snapshot " + snapshotObj.getId()
                    + " backup failed due to unable to find snapshot ");
            result.setResult("Unable to find snapshot: " + snapshotObj.getId());
            callback.complete(result);
            return;
        }

        try {
            String backupOfSnapshot = snapshotObj.getPath();
            if (backupOfSnapshot == null) {
                callback.complete(result);
                return;
            }

            DeleteSnapshotBackupCommand2 cmd = new DeleteSnapshotBackupCommand2(secStore.getTO(), backupOfSnapshot);
            EndPoint ep = _epSelector.select(secStore);
            Answer answer = ep.sendMessage(cmd);

            if (answer != null && !answer.getResult()) {
                result.setResult(answer.getDetails());
            }
        } catch (Exception e) {
            s_logger.debug("failed to delete snapshot: " + snapshotObj.getId() + ": " + e.toString());
            result.setResult(e.toString());
        }
        callback.complete(result);
    }

    @Override
    public void deleteAsync(DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        if (data.getType() == DataObjectType.VOLUME) {
            deleteVolume(data, callback);
        } else if (data.getType() == DataObjectType.TEMPLATE) {
            deleteTemplate(data, callback);
        } else if (data.getType() == DataObjectType.SNAPSHOT) {
            deleteSnapshot(data, callback);
        }
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        // TODO Auto-generated method stub

    }

}
