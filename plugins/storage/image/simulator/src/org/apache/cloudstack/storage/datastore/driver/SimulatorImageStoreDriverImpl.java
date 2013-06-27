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


import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.image.BaseImageStoreDriverImpl;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import javax.inject.Inject;
import java.util.Date;

public class SimulatorImageStoreDriverImpl extends BaseImageStoreDriverImpl {

    @Inject
    TemplateDataStoreDao _templateStoreDao;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    VolumeDataStoreDao _volumeStoreDao;

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        ImageStoreImpl nfsStore = (ImageStoreImpl) store;
        NfsTO nfsTO = new NfsTO();
        nfsTO.setRole(store.getRole());
        nfsTO.setUrl(nfsStore.getUri());
        return nfsTO;
    }

    class CreateContext<T> extends AsyncRpcContext<T> {
        final DataObject data;

        public CreateContext(AsyncCompletionCallback<T> callback, DataObject data) {
            super(callback);
            this.data = data;
        }
    }

    public String createEntityExtractUrl(DataStore store, String installPath, Storage.ImageFormat format) {
        return null;
    }

    @Override
    public void createAsync(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateContext<CreateCmdResult> context = new CreateContext<CreateCmdResult>(callback, data);
        AsyncCallbackDispatcher<SimulatorImageStoreDriverImpl, DownloadAnswer> caller = AsyncCallbackDispatcher
                .create(this);
        caller.setContext(context);
        if (data.getType() == DataObjectType.TEMPLATE) {
            this.createTemplateAsyncCallback(caller, context);
        } else if (data.getType() == DataObjectType.VOLUME) {
            this.createVolumeAsyncCallback(caller, context);
        }
    }

    protected Void createTemplateAsyncCallback(AsyncCallbackDispatcher<SimulatorImageStoreDriverImpl, DownloadAnswer> callback,
                                               CreateContext<CreateCmdResult> context) {
        DataObject obj = context.data;
        DataStore store = obj.getDataStore();
        TemplateObjectTO templateTO = (TemplateObjectTO)context.data.getTO();

        TemplateDataStoreVO tmpltStoreVO = _templateStoreDao.findByStoreTemplate(store.getId(), obj.getId());
        if (tmpltStoreVO != null) {
            TemplateDataStoreVO updateBuilder = _templateStoreDao.createForUpdate();
            updateBuilder.setDownloadPercent(100);
            updateBuilder.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            updateBuilder.setLastUpdated(new Date());
            updateBuilder.setSize(new Long(5 * 1024L * 1024L));
            updateBuilder.setPhysicalSize(new Long(5 * 1024L * 1024L));
            updateBuilder.setDownloadUrl(templateTO.getOrigUrl());
            updateBuilder.setInstallPath(templateTO.getPath());
            updateBuilder.setTemplateId(templateTO.getId());
            updateBuilder.setState(ObjectInDataStoreStateMachine.State.Ready);
            _templateStoreDao.update(tmpltStoreVO.getId(), updateBuilder);
            // update size in vm_template table
            VMTemplateVO tmlptUpdater = _templateDao.createForUpdate();
            tmlptUpdater.setSize(new Long(5 * 1024l * 1024l));
            _templateDao.update(obj.getId(), tmlptUpdater);
        }
        return null;
    }

    protected Void createVolumeAsyncCallback(AsyncCallbackDispatcher<SimulatorImageStoreDriverImpl, DownloadAnswer> callback,
                                             CreateContext<CreateCmdResult> context) {
        DataObject obj = context.data;
        DataStore store = obj.getDataStore();
        VolumeObjectTO volumeTO = (VolumeObjectTO) context.data.getTO();

        VolumeDataStoreVO volStoreVO = _volumeStoreDao.findByStoreVolume(store.getId(), obj.getId());
        if (volStoreVO != null) {
            VolumeDataStoreVO updateBuilder = _volumeStoreDao.createForUpdate();
            updateBuilder.setDownloadPercent(100);
            updateBuilder.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            updateBuilder.setLastUpdated(new Date());
            updateBuilder.setInstallPath(volumeTO.getPath());
            updateBuilder.setVolumeId(volumeTO.getVolumeId());
            updateBuilder.setSize(volumeTO.getSize());
            updateBuilder.setPhysicalSize(volumeTO.getSize());
            updateBuilder.setState(ObjectInDataStoreStateMachine.State.Ready);
            _volumeStoreDao.update(volStoreVO.getId(), updateBuilder);
            // update size in volume table
            VolumeVO volUpdater = _volumeDao.createForUpdate();
            volUpdater.setSize(volumeTO.getSize());
            _volumeDao.update(obj.getId(), volUpdater);
        }
        return null;
    }
}