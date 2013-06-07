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
package org.apache.cloudstack.storage.image;

import java.util.Date;
import java.util.Set;
import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.image.ImageStoreDriver;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.download.DownloadMonitor;

public abstract class BaseImageStoreDriverImpl implements ImageStoreDriver {
    private static final Logger s_logger = Logger.getLogger(BaseImageStoreDriverImpl.class);
    @Inject
    VMTemplateDao templateDao;
    @Inject
    DownloadMonitor _downloadMonitor;
    @Inject
    VolumeDao volumeDao;
    @Inject
    VolumeDataStoreDao _volumeStoreDao;
    @Inject
    TemplateDataStoreDao _templateStoreDao;
    @Inject
    EndPointSelector _epSelector;

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
        AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> caller = AsyncCallbackDispatcher
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

    protected Void createTemplateAsyncCallback(AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> callback,
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

    protected Void createVolumeAsyncCallback(AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> callback,
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



    @Override
    public void deleteAsync(DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        DeleteCommand cmd = new DeleteCommand(data.getTO());

        CommandResult result = new CommandResult();
        try {
            EndPoint ep = _epSelector.select(data);
            Answer answer = ep.sendMessage(cmd);
            if (answer != null && !answer.getResult()) {
                result.setResult(answer.getDetails());
            }
        } catch (Exception ex) {
            s_logger.debug("Unable to destoy " + data.getType().toString() + ": " + data.getId(), ex);
            result.setResult(ex.toString());
        }
        callback.complete(result);
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
