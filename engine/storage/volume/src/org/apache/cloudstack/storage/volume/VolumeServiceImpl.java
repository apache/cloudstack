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
import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.datastore.DataObjectManager;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.motion.ImageMotionService;
import org.apache.cloudstack.storage.volume.db.VolumeDao2;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.springframework.stereotype.Component;

import com.cloud.storage.Volume;
import com.cloud.utils.db.DB;

//1. change volume state
//2. orchestrator of volume, control most of the information of volume, storage pool id, voluem state, scope etc.

@Component
public class VolumeServiceImpl implements VolumeService {
    @Inject
    VolumeDao2 volDao;
    @Inject
    PrimaryDataStoreProviderManager dataStoreMgr;
    @Inject
    ObjectInDataStoreManager objectInDataStoreMgr;
    @Inject
    DataObjectManager dataObjectMgr;
    @Inject
    ImageMotionService imageMotion;
    @Inject
    TemplateInstallStrategy templateInstallStrategy;

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
    public AsyncCallFuture<VolumeApiResult> createVolumeAsync(VolumeInfo volume, long dataStoreId) {
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
        AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createVolumeCallback(null, null))
        .setContext(context);
        
        dataObjectMgr.createAsync(volume, dataStore, caller, true);
        return future;
    }
    
    protected Void createVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateVolumeContext<VolumeApiResult> context) {
        CreateCmdResult result = callback.getResult();
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
        
        DataStore dataStore = vo.getDataStore();
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
        
        dataObjectMgr.deleteAsync(volume, caller);
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

    class CreateBaseImageContext<T> extends AsyncRpcConext<T> {
        private final VolumeInfo volume;
        private final PrimaryDataStore dataStore;
        private final TemplateInfo srcTemplate;
        private final AsyncCallFuture<VolumeApiResult> future;
        public CreateBaseImageContext(AsyncCompletionCallback<T> callback, VolumeInfo volume, PrimaryDataStore datastore, 
                TemplateInfo srcTemplate,
                AsyncCallFuture<VolumeApiResult> future) {
            super(callback);
            this.volume = volume;
            this.dataStore = datastore;
            this.future = future;
            this.srcTemplate = srcTemplate;
        }
        
        public VolumeInfo getVolume() {
            return this.volume;
        }
        
        public PrimaryDataStore getDataStore() {
            return this.dataStore;
        }

        public TemplateInfo getSrcTemplate() {
            return this.srcTemplate;
        }
        
        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
        
    }
    
    static class CreateBaseImageResult extends CommandResult {
        final TemplateInfo template;
        public CreateBaseImageResult(TemplateInfo template) {
            super();
            this.template = template;
        }
    }
    
    @DB
    protected void createBaseImageAsync(VolumeInfo volume, PrimaryDataStore dataStore, TemplateInfo template, AsyncCallFuture<VolumeApiResult> future) {
        CreateBaseImageContext<CreateCmdResult> context = new CreateBaseImageContext<CreateCmdResult>(null, volume, 
                dataStore,
                template,
                future);
        
        AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyBaseImageCallback(null, null))
        .setContext(context);
        DataObject templateOnPrimaryStoreObj = dataObjectMgr.createInternalStateOnly(template, dataStore);
     
        dataObjectMgr.copyAsync(context.srcTemplate, templateOnPrimaryStoreObj, caller);
        return;
    }
    
    @DB
    protected Void copyBaseImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateBaseImageContext<VolumeApiResult> context) {
        CreateCmdResult result = callback.getResult();
        VolumeApiResult res = new VolumeApiResult(context.getVolume());
        
        AsyncCallFuture<VolumeApiResult> future = context.getFuture();
        if (!result.isSuccess()) {
            res.setResult(result.getResult());
            future.complete(res);
            return null;
        }
        DataObject templateOnPrimaryStoreObj = objectInDataStoreMgr.get(context.srcTemplate, context.dataStore);

        createVolumeFromBaseImageAsync(context.volume, templateOnPrimaryStoreObj, context.dataStore, future);
        return null;
    }
    
    private class CreateVolumeFromBaseImageContext<T> extends AsyncRpcConext<T> {
        private final VolumeObject vo;
        private final AsyncCallFuture<VolumeApiResult> future;
        private final DataStore primaryStore;
        private final DataObject templateOnStore;
        public CreateVolumeFromBaseImageContext(AsyncCompletionCallback<T> callback, VolumeObject vo, 
                DataStore primaryStore,
                DataObject templateOnStore,
                AsyncCallFuture<VolumeApiResult> future) {
            super(callback);
            this.vo = vo;
            this.future = future;
            this.primaryStore = primaryStore;
            this.templateOnStore = templateOnStore;
        }
        
        public VolumeObject getVolumeObject() {
            return this.vo;
        }
        
        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
    }
    
    @DB
    protected void createVolumeFromBaseImageAsync(VolumeInfo volume, DataObject templateOnPrimaryStore, PrimaryDataStore pd, AsyncCallFuture<VolumeApiResult> future) {
        VolumeObject vo = (VolumeObject) volume;
        try {
            vo.stateTransit(Volume.Event.CreateRequested);
        } catch (Exception e) {
            VolumeApiResult result = new VolumeApiResult(volume);
            result.setResult(e.toString());
            future.complete(result);
            return;
        }

        CreateVolumeFromBaseImageContext<VolumeApiResult> context = new CreateVolumeFromBaseImageContext<VolumeApiResult>(null, vo, pd, templateOnPrimaryStore, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> caller =  AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyBaseImageCallBack(null, null))
        .setContext(context);

        DataObject volumeOnPrimaryStorage = dataObjectMgr.createInternalStateOnly(volume, pd);
        dataObjectMgr.copyAsync(context.templateOnStore, volumeOnPrimaryStorage, caller);
        return;
    }
    
    @DB
    public Void copyBaseImageCallBack(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateVolumeFromBaseImageContext<VolumeApiResult> context) {
        VolumeObject vo = context.getVolumeObject();
        CreateCmdResult result = callback.getResult();
        VolumeApiResult volResult = new VolumeApiResult(vo);
       
        if (result.isSuccess()) {
            if (result.getPath() != null) {
                vo.setPath(result.getPath());
            }
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
    public AsyncCallFuture<VolumeApiResult> createVolumeFromTemplateAsync(VolumeInfo volume, long dataStoreId, TemplateInfo template) {
        PrimaryDataStore pd = dataStoreMgr.getPrimaryDataStore(dataStoreId);
        TemplateInfo templateOnPrimaryStore = pd.getTemplate(template.getId());
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
