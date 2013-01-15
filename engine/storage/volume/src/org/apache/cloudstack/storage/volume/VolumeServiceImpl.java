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
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.manager.PrimaryDataStoreManager;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.motion.ImageMotionService;
import org.apache.cloudstack.storage.volume.VolumeService.VolumeApiResult;
import org.apache.cloudstack.storage.volume.db.VolumeDao2;
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
    VolumeDao2 volDao;
    @Inject
    PrimaryDataStoreManager dataStoreMgr;
    @Inject
    ObjectInDataStoreManager objectInDataStoreMgr;
    @Inject
    ImageMotionService imageMotion;

    public VolumeServiceImpl() {
    }
    
    private class CreateVolumeContext<T> extends AsyncRpcConext<T> {

        private VolumeObject volume;
        private AsyncCallFuture<VolumeApiResult> future;
        /**
         * @param callback
         */
        public CreateVolumeContext(AsyncCompletionCallback<T> callback, VolumeObject volume, AsyncCallFuture<VolumeApiResult> future) {
            super(callback);
            this.volume = volume;
            this.future = future;
        }
        
        public VolumeObject getVolume() {
            return this.volume;
        }
        
        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
        
    }
    
    @Override
    public AsyncCallFuture<VolumeApiResult> createVolumeAsync(VolumeInfo volume, long dataStoreId, VolumeDiskType diskType) {
        PrimaryDataStore dataStore = dataStoreMgr.getPrimaryDataStore(dataStoreId);
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult result = new VolumeApiResult(volume);
        
        if (dataStore == null) {
            result.setResult("Can't find dataStoreId: " + dataStoreId);
            future.complete(result);
            return future;
        }

        if (dataStore.exists(volume)) {
            result.setResult("Volume: " + volume.getId() + " already exists on primary data store: " + dataStoreId);
            future.complete(result);
            return future;
        }

        VolumeObject vo = (VolumeObject) volume;
        vo.stateTransit(Volume.Event.CreateRequested);

        CreateVolumeContext<VolumeApiResult> context = new CreateVolumeContext<VolumeApiResult>(null, vo, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createVolumeCallback(null, null))
        .setContext(context);
        
        dataStore.createVolumeAsync(vo, diskType, caller);
        return future;
    }
    
    protected Void createVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> callback, CreateVolumeContext<VolumeApiResult> context) {
        CommandResult result = callback.getResult();
        VolumeObject vo = context.getVolume();
        VolumeApiResult volResult = new VolumeApiResult(vo);
        if (result.isSuccess()) {
            vo.stateTransit(Volume.Event.OperationSucceeded);
        } else {
            vo.stateTransit(Volume.Event.OperationFailed);
            volResult.setResult(result.getResult());
        }
        
        context.getFuture().complete(volResult);
        return null;
    }
    
    private class DeleteVolumeContext<T> extends AsyncRpcConext<T> {
        private final VolumeObject volume;
        private AsyncCallFuture<VolumeApiResult> future;
        /**
         * @param callback
         */
        public DeleteVolumeContext(AsyncCompletionCallback<T> callback, VolumeObject volume, AsyncCallFuture<VolumeApiResult> future) {
            super(callback);
            this.volume = volume;
            this.future = future;
        }
        
        public VolumeObject getVolume() {
            return this.volume;
        }
        
        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
    }

    @DB
    @Override
    public  AsyncCallFuture<VolumeApiResult> deleteVolumeAsync(VolumeInfo volume) {
        VolumeObject vo = (VolumeObject)volume;
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult result = new VolumeApiResult(volume);
        
        PrimaryDataStore dataStore = vo.getDataStore();
        vo.stateTransit(Volume.Event.DestroyRequested);
        if (dataStore == null) {
            vo.stateTransit(Volume.Event.OperationSucceeded);
            volDao.remove(vo.getId());
            future.complete(result);
            return future;
        }
        
        DeleteVolumeContext<VolumeApiResult> context = new DeleteVolumeContext<VolumeApiResult>(null, vo, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteVolumeCallback(null, null))
            .setContext(context);
        
        dataStore.deleteVolumeAsync(volume, caller);
        return future;
    }
    
    public Void deleteVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> callback, DeleteVolumeContext<VolumeApiResult> context) {
        CommandResult result = callback.getResult();
        VolumeObject vo = context.getVolume();
        VolumeApiResult apiResult = new VolumeApiResult(vo);
        if (result.isSuccess()) {
            vo.stateTransit(Volume.Event.OperationSucceeded);
            volDao.remove(vo.getId());
        } else {
            vo.stateTransit(Volume.Event.OperationFailed);
            apiResult.setResult(result.getResult());
        }
        context.getFuture().complete(apiResult);
        return null;
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
        private final TemplateInfo template;
        private final AsyncCallFuture<VolumeApiResult> future;
        public CreateBaseImageContext(AsyncCompletionCallback<T> callback, VolumeInfo volume, PrimaryDataStore datastore, TemplateInfo template, 
                AsyncCallFuture<VolumeApiResult> future) {
            super(callback);
            this.volume = volume;
            this.dataStore = datastore;
            this.template = template;
            this.future = future;
        }
        
        public VolumeInfo getVolume() {
            return this.volume;
        }
        
        public PrimaryDataStore getDataStore() {
            return this.dataStore;
        }
        
        public TemplateInfo getTemplate() {
            return this.template;
        }
        
        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
        
    }
    @DB
    protected void createBaseImageAsync(VolumeInfo volume, PrimaryDataStore dataStore, TemplateInfo template, AsyncCallFuture<VolumeApiResult> future) {
        TemplateInfo templateOnPrimaryStoreObj = objectInDataStoreMgr.create(template, dataStore);
        /*templateOnPrimaryStoreObj.stateTransit(ObjectInDataStoreStateMachine.Event.CreateRequested);
        templateOnPrimaryStoreObj.updateStatus(Status.CREATING);
        try {
            dataStore.installTemplate(templateOnPrimaryStoreObj);
            templateOnPrimaryStoreObj.updateStatus(Status.CREATED);
        } catch (Exception e) {
            templateOnPrimaryStoreObj.updateStatus(Status.ABANDONED);
            templateOnPrimaryStoreObj.stateTransit(ObjectInDataStoreStateMachine.Event.OperationFailed);
            VolumeApiResult result = new VolumeApiResult(volume);
            result.setResult(e.toString());
            future.complete(result);
            return;
        }

        templateOnPrimaryStoreObj.updateStatus(Status.DOWNLOAD_IN_PROGRESS);
   */
        CreateBaseImageContext<VolumeApiResult> context = new CreateBaseImageContext<VolumeApiResult>(null, volume, dataStore, templateOnPrimaryStoreObj, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().createBaseImageCallback(null, null))
            .setContext(context);
 
        objectInDataStoreMgr.update(templateOnPrimaryStoreObj, ObjectInDataStoreStateMachine.Event.CreateRequested);

        imageMotion.copyTemplateAsync(templateOnPrimaryStoreObj, template, caller);
    }

    @DB
    protected Void createBaseImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> callback, CreateBaseImageContext<VolumeApiResult> context) {
        CommandResult result = callback.getResult();
        TemplateInfo templateOnPrimaryStoreObj = context.getTemplate();
        if (result.isSuccess()) {
            objectInDataStoreMgr.update(templateOnPrimaryStoreObj, ObjectInDataStoreStateMachine.Event.OperationSuccessed);
        } else {
            objectInDataStoreMgr.update(templateOnPrimaryStoreObj, ObjectInDataStoreStateMachine.Event.OperationFailed);
        }
        
        AsyncCallFuture<VolumeApiResult> future = context.getFuture();
        VolumeInfo volume = context.getVolume();
        PrimaryDataStore pd = context.getDataStore();
        createVolumeFromBaseImageAsync(volume, templateOnPrimaryStoreObj, pd, future);
        return null;
    }
    
    private class CreateVolumeFromBaseImageContext<T> extends AsyncRpcConext<T> {
        private final VolumeObject vo;
        private final AsyncCallFuture<VolumeApiResult> future;
        public CreateVolumeFromBaseImageContext(AsyncCompletionCallback<T> callback, VolumeObject vo, AsyncCallFuture<VolumeApiResult> future) {
            super(callback);
            this.vo = vo;
            this.future = future;
        }
        
        public VolumeObject getVolumeObject() {
            return this.vo;
        }
        
        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
    }
    
    @DB
    protected void createVolumeFromBaseImageAsync(VolumeInfo volume, TemplateInfo templateOnPrimaryStore, PrimaryDataStore pd, AsyncCallFuture<VolumeApiResult> future) {
        VolumeObject vo = (VolumeObject) volume;
        try {
            vo.stateTransit(Volume.Event.CreateRequested);
        } catch (Exception e) {
            VolumeApiResult result = new VolumeApiResult(volume);
            result.setResult(e.toString());
            future.complete(result);
            return;
        }

        CreateVolumeFromBaseImageContext<VolumeApiResult> context = new CreateVolumeFromBaseImageContext<VolumeApiResult>(null, vo, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> caller =  AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createVolumeFromBaseImageCallback(null, null))
        .setContext(context);

        pd.createVoluemFromBaseImageAsync(volume, templateOnPrimaryStore, caller);
    }
    
    @DB
    public Object createVolumeFromBaseImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> callback, CreateVolumeFromBaseImageContext<VolumeApiResult> context) {
        VolumeObject vo = context.getVolumeObject();
        CommandResult result = callback.getResult();
        VolumeApiResult volResult = new VolumeApiResult(vo);
        if (result.isSuccess()) {
            vo.stateTransit(Volume.Event.OperationSucceeded); 
        } else {
            vo.stateTransit(Volume.Event.OperationFailed);
            volResult.setResult(result.getResult());
        }
        
        AsyncCallFuture<VolumeApiResult> future = context.getFuture();
        future.complete(volResult);
        return null;
    }

    @DB
    @Override
    public AsyncCallFuture<VolumeApiResult> createVolumeFromTemplateAsync(VolumeInfo volume, long dataStoreId, VolumeDiskType diskType, TemplateInfo template) {
        PrimaryDataStore pd = dataStoreMgr.getPrimaryDataStore(dataStoreId);
        TemplateOnPrimaryDataStoreInfo templateOnPrimaryStore = pd.getTemplate(template);
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult result = new VolumeApiResult(volume);
        
        if (templateOnPrimaryStore == null) {
            createBaseImageAsync(volume, pd, template, future);
            return future;
        }
            
        createVolumeFromBaseImageAsync(volume, template, pd, future);
        return future;
    }

    @Override
    public TemplateOnPrimaryDataStoreInfo grantAccess(TemplateOnPrimaryDataStoreInfo template, EndPoint endPoint) {
        // TODO Auto-generated method stub
        return null;
    }
}
