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
package org.apache.cloudstack.storage.volume;

import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.manager.PrimaryDataStoreManager;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.motion.ImageMotionService;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;

import org.springframework.stereotype.Component;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.Volume;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;

//1. change volume state
//2. orchestrator of volume, control most of the information of volume, storage pool id, voluem state, scope etc.

@Component
public class VolumeServiceImpl implements VolumeService {
    @Inject
    VolumeDao volDao;
    @Inject
    PrimaryDataStoreManager dataStoreMgr;
    @Inject
    TemplatePrimaryDataStoreManager templatePrimaryStoreMgr;
    @Inject
    ImageMotionService imageMotion;

    public VolumeServiceImpl() {
    }
    
    @Override
    public VolumeInfo createVolume(VolumeInfo volume, long dataStoreId, VolumeDiskType diskType) {
        PrimaryDataStore dataStore = dataStoreMgr.getPrimaryDataStore(dataStoreId);
        if (dataStore == null) {
            throw new CloudRuntimeException("Can't find dataStoreId: " + dataStoreId);
        }

        if (dataStore.exists(volume)) {
            return volume;
        }

        VolumeObject vo = (VolumeObject) volume;
        vo.stateTransit(Volume.Event.CreateRequested);

        try {
            VolumeInfo vi = dataStore.createVolume(vo, diskType);
            vo.stateTransit(Volume.Event.OperationSucceeded);
            return vi;
        } catch (Exception e) {
            vo.stateTransit(Volume.Event.OperationFailed);
            throw new CloudRuntimeException(e.toString());
        }
    }

    @DB
    @Override
    public boolean deleteVolume(VolumeInfo volumeId) {
        return true;
    }

    @Override
    public boolean cloneVolume(long volumeId, long baseVolId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean createVolumeFromSnapshot(long volumeId, long snapshotId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean rokeAccess(long volumeId, long endpointId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public VolumeEntity allocateVolumeInDb(long size, VolumeType type, String volName, Long templateId) {
        VolumeVO vo = volDao.allocVolume(size, type, volName, templateId);
        return new VolumeEntityImpl(VolumeObject.getVolumeObject(null, vo), this);
    }

    @Override
    public VolumeEntity getVolumeEntity(long volumeId) {
        VolumeVO vo = volDao.findById(volumeId);
        if (vo == null) {
            return null;
        }

        if (vo.getPoolId() == null) {
            return new VolumeEntityImpl(VolumeObject.getVolumeObject(null, vo), this);
        } else {
            PrimaryDataStore dataStore = dataStoreMgr.getPrimaryDataStore(vo.getPoolId());
            return new VolumeEntityImpl(dataStore.getVolume(volumeId), this);
        }
    }

    @Override
    public String grantAccess(VolumeInfo volume, EndPoint endpointId) {
        // TODO Auto-generated method stub
        return null;
    }

    private class CreateBaseImageContext<T> extends AsyncRpcConext<T> {
        private final VolumeInfo volume;
        private final PrimaryDataStore dataStore;
        private final TemplateOnPrimaryDataStoreObject template;
        public CreateBaseImageContext(AsyncCompletionCallback<T> callback, VolumeInfo volume, PrimaryDataStore datastore, TemplateOnPrimaryDataStoreObject template) {
            super(callback);
            this.volume = volume;
            this.dataStore = datastore;
            this.template = template;
        }
        
        public VolumeInfo getVolume() {
            return this.volume;
        }
        
        public PrimaryDataStore getDataStore() {
            return this.dataStore;
        }
        
        public TemplateOnPrimaryDataStoreObject getTemplate() {
            return this.template;
        }
        
    }
    @DB
    protected void createBaseImageAsync(VolumeInfo volume, PrimaryDataStore dataStore, TemplateInfo template, AsyncCompletionCallback<VolumeInfo> callback) {
        TemplateOnPrimaryDataStoreObject templateOnPrimaryStoreObj = (TemplateOnPrimaryDataStoreObject) templatePrimaryStoreMgr.createTemplateOnPrimaryDataStore(template, dataStore);
        templateOnPrimaryStoreObj.stateTransit(TemplateOnPrimaryDataStoreStateMachine.Event.CreateRequested);
        templateOnPrimaryStoreObj.updateStatus(Status.CREATING);
        try {
            dataStore.installTemplate(templateOnPrimaryStoreObj);
            templateOnPrimaryStoreObj.updateStatus(Status.CREATED);
        } catch (Exception e) {
            templateOnPrimaryStoreObj.updateStatus(Status.ABANDONED);
            templateOnPrimaryStoreObj.stateTransit(TemplateOnPrimaryDataStoreStateMachine.Event.OperationFailed);
            throw new CloudRuntimeException(e.toString());
        }

        templateOnPrimaryStoreObj.updateStatus(Status.DOWNLOAD_IN_PROGRESS);

        CreateBaseImageContext<VolumeInfo> context = new CreateBaseImageContext<VolumeInfo>(callback, volume, dataStore, templateOnPrimaryStoreObj);
        AsyncCallbackDispatcher<VolumeServiceImpl> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createBaseImageCallback(null, null))
            .setContext(context);

        imageMotion.copyTemplateAsync(templateOnPrimaryStoreObj, caller);
    }

    @DB
    public Object createBaseImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl> callback, CreateBaseImageContext<VolumeInfo> context) {
        CommandResult result = callback.getResult();
        TemplateOnPrimaryDataStoreObject templateOnPrimaryStoreObj = context.getTemplate();
        if (result.isSuccess()) {
            templateOnPrimaryStoreObj.stateTransit(TemplateOnPrimaryDataStoreStateMachine.Event.OperationSuccessed);
        } else {
            templateOnPrimaryStoreObj.stateTransit(TemplateOnPrimaryDataStoreStateMachine.Event.OperationFailed);
        }
        
        AsyncCompletionCallback<VolumeInfo> parentCaller = context.getParentCallback();
        VolumeInfo volume = context.getVolume();
        PrimaryDataStore pd = context.getDataStore();

        createVolumeFromBaseImageAsync(volume, templateOnPrimaryStoreObj, pd, parentCaller);
        return null;
    }
    
    private class CreateVolumeFromBaseImageContext<T> extends AsyncRpcConext<T> {
        private final VolumeObject vo;
        public CreateVolumeFromBaseImageContext(AsyncCompletionCallback<T> callback, VolumeObject vo) {
            super(callback);
            this.vo = vo;
        }
        
        public VolumeObject getVolumeObject() {
            return this.vo;
        }
    }
    
    @DB
    protected void createVolumeFromBaseImageAsync(VolumeInfo volume, TemplateOnPrimaryDataStoreInfo templateOnPrimaryStore, PrimaryDataStore pd, AsyncCompletionCallback<VolumeInfo> callback) {
        VolumeObject vo = (VolumeObject) volume;
        try {
            vo.stateTransit(Volume.Event.CreateRequested);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.toString());
        }

        CreateVolumeFromBaseImageContext<VolumeInfo> context = new CreateVolumeFromBaseImageContext<VolumeInfo>(callback, vo);
        AsyncCallbackDispatcher<VolumeServiceImpl> caller =  AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createVolumeFromBaseImageCallback(null, null))
        .setContext(context);

        pd.createVoluemFromBaseImageAsync(volume, templateOnPrimaryStore, caller);
    }
    
    @DB
    public Object createVolumeFromBaseImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl> callback, CreateVolumeFromBaseImageContext<VolumeInfo> context) {
        VolumeObject vo = context.getVolumeObject();
        CommandResult result = callback.getResult();
        if (result.isSuccess()) {
            vo.stateTransit(Volume.Event.OperationSucceeded); 
        } else {
            vo.stateTransit(Volume.Event.OperationFailed);
        }
        
        AsyncCompletionCallback<VolumeInfo> parentCall = context.getParentCallback();
        parentCall.complete(vo);
        return null;
    }

    @DB
    @Override
    public void createVolumeFromTemplateAsync(VolumeInfo volume, long dataStoreId, VolumeDiskType diskType, TemplateInfo template, AsyncCompletionCallback<VolumeInfo> callback) {
        PrimaryDataStore pd = dataStoreMgr.getPrimaryDataStore(dataStoreId);
        TemplateOnPrimaryDataStoreInfo templateOnPrimaryStore = pd.getTemplate(template);
        if (templateOnPrimaryStore == null) {
            createBaseImageAsync(volume, pd, template, callback);
            return;
        }
            
        createVolumeFromBaseImageAsync(volume, templateOnPrimaryStore, pd, callback);
    }

    @Override
    public TemplateOnPrimaryDataStoreInfo grantAccess(TemplateOnPrimaryDataStoreInfo template, EndPoint endPoint) {
        // TODO Auto-generated method stub
        return null;
    }
}
