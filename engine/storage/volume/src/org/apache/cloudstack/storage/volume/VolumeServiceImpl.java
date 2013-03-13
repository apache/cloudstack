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
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.datastore.DataObjectManager;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.motion.DataMotionService;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class VolumeServiceImpl implements VolumeService {
    private static final Logger s_logger = Logger
            .getLogger(VolumeServiceImpl.class);
    @Inject
    VolumeDao volDao;
    @Inject
    PrimaryDataStoreProviderManager dataStoreMgr;
    @Inject
    ObjectInDataStoreManager objectInDataStoreMgr;
    @Inject
    DataObjectManager dataObjectMgr;
    @Inject
    DataMotionService motionSrv;
    @Inject
    TemplateInstallStrategy templateInstallStrategy;
    @Inject
    VolumeDataFactory volFactory;
    @Inject SnapshotManager snapshotMgr;
    @Inject VMInstanceDao vmDao;

    public VolumeServiceImpl() {
    }
    
    private class CreateVolumeContext<T> extends AsyncRpcConext<T> {

        private DataObject volume;
        private AsyncCallFuture<VolumeApiResult> future;
        /**
         * @param callback
         */
        public CreateVolumeContext(AsyncCompletionCallback<T> callback, DataObject volume, AsyncCallFuture<VolumeApiResult> future) {
            super(callback);
            this.volume = volume;
            this.future = future;
        }
        
        public DataObject getVolume() {
            return this.volume;
        }
        
        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
        
    }
    
    @Override
    public AsyncCallFuture<VolumeApiResult> createVolumeAsync(VolumeInfo volume, DataStore dataStore) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        DataObject volumeOnStore = dataStore.create(volume);
        volumeOnStore.processEvent(Event.CreateOnlyRequested);

        CreateVolumeContext<VolumeApiResult> context = new CreateVolumeContext<VolumeApiResult>(null, volumeOnStore, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createVolumeCallback(null, null))
        .setContext(context);
        
        dataStore.getDriver().createAsync(volumeOnStore, caller);
        return future;
    }
    
    protected Void createVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateVolumeContext<VolumeApiResult> context) {
        CreateCmdResult result = callback.getResult();
        DataObject vo = context.getVolume();
        String errMsg = null;
        if (result.isSuccess()) {
            vo.processEvent(Event.OperationSuccessed);
        } else {
            vo.processEvent(Event.OperationFailed);
            errMsg = result.getResult();
        }
        VolumeApiResult volResult = new VolumeApiResult((VolumeObject)vo);
        if (errMsg != null) {
            volResult.setResult(errMsg);
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
    public  AsyncCallFuture<VolumeApiResult> expungeVolumeAsync(VolumeInfo volume) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult result = new VolumeApiResult(volume);
        if (volume.getDataStore() == null) {
            this.volDao.remove(volume.getId());
            future.complete(result);
            return future;
        }
        
        String vmName = null;
        VolumeVO vol = this.volDao.findById(volume.getId());
        if (vol.getVolumeType() == Type.ROOT && vol.getInstanceId() != null) {
            VirtualMachine vm = vmDao.findByIdIncludingRemoved(vol
                    .getInstanceId());
            if (vm != null) {
                vmName = vm.getInstanceName();
            }
        }

        String volumePath = vol.getPath();
        Long poolId = vol.getPoolId();
        if (poolId == null || volumePath == null || volumePath.trim().isEmpty()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Marking volume that was never created as destroyed: "
                        + vol);
            }
            this.volDao.remove(vol.getId());
            future.complete(result);
            return future;
        }
        VolumeObject vo = (VolumeObject)volume;
      
        volume.processEvent(Event.ExpungeRequested);
      
        
        DeleteVolumeContext<VolumeApiResult> context = new DeleteVolumeContext<VolumeApiResult>(null, vo, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().deleteVolumeCallback(null, null))
            .setContext(context);
        
        volume.getDataStore().getDriver().deleteAsync(volume, caller);
        return future;
    }
    
    public Void deleteVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CommandResult> callback, DeleteVolumeContext<VolumeApiResult> context) {
        CommandResult result = callback.getResult();
        VolumeObject vo = context.getVolume();
        VolumeApiResult apiResult = new VolumeApiResult(vo);
        if (result.isSuccess()) {
            vo.processEvent(Event.OperationSuccessed);
            volDao.remove(vo.getId());
        } else {
            vo.processEvent(Event.OperationFailed);
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

    class CreateBaseImageContext<T> extends AsyncRpcConext<T> {
        private final VolumeInfo volume;
        private final PrimaryDataStore dataStore;
        private final TemplateInfo srcTemplate;
        private final AsyncCallFuture<VolumeApiResult> future;
        final DataObject destObj;
        public CreateBaseImageContext(AsyncCompletionCallback<T> callback, VolumeInfo volume, PrimaryDataStore datastore, 
                TemplateInfo srcTemplate,
                AsyncCallFuture<VolumeApiResult> future, DataObject destObj) {
            super(callback);
            this.volume = volume;
            this.dataStore = datastore;
            this.future = future;
            this.srcTemplate = srcTemplate;
            this.destObj = destObj;
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
       
        DataObject templateOnPrimaryStoreObj = dataStore.create(template);
        CreateBaseImageContext<CreateCmdResult> context = new CreateBaseImageContext<CreateCmdResult>(null, volume, 
                dataStore,
                template,
                future, templateOnPrimaryStoreObj);
        AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyBaseImageCallback(null, null))
        .setContext(context);
        
        templateOnPrimaryStoreObj.processEvent(Event.CreateOnlyRequested);
     
        try {
            motionSrv.copyAsync(template, templateOnPrimaryStoreObj, caller);
        } catch (Exception e) {
            s_logger.debug("failed to create template on storage", e);
            templateOnPrimaryStoreObj.processEvent(Event.OperationFailed);
            VolumeApiResult result = new VolumeApiResult(volume);
            result.setResult(e.toString());
            caller.complete(result);
        }
        return;
    }
    
    @DB
    protected Void copyBaseImageCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CreateBaseImageContext<VolumeApiResult> context) {
        CopyCommandResult result = callback.getResult();
        VolumeApiResult res = new VolumeApiResult(context.getVolume());
        
        AsyncCallFuture<VolumeApiResult> future = context.getFuture();
        DataObject templateOnPrimaryStoreObj = context.destObj;
        if (!result.isSuccess()) {
            templateOnPrimaryStoreObj.processEvent(Event.OperationFailed);
            res.setResult(result.getResult());
            future.complete(res);
            return null;
        }
        
        templateOnPrimaryStoreObj.processEvent(Event.OperationSuccessed);
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
        

        public AsyncCallFuture<VolumeApiResult> getFuture() {
            return this.future;
        }
    }
    
    @DB
    protected void createVolumeFromBaseImageAsync(VolumeInfo volume, DataObject templateOnPrimaryStore, PrimaryDataStore pd, AsyncCallFuture<VolumeApiResult> future) {
        VolumeObject vo = (VolumeObject)volume;
        CreateVolumeFromBaseImageContext<VolumeApiResult> context = new CreateVolumeFromBaseImageContext<VolumeApiResult>(null, vo, pd, templateOnPrimaryStore, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller =  AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().copyBaseImageCallBack(null, null))
        .setContext(context);

        DataObject volumeOnPrimaryStorage = pd.create(volume);
        volume.processEvent(Event.CreateOnlyRequested);

        motionSrv.copyAsync(context.templateOnStore, volumeOnPrimaryStorage, caller);
        return;
    }
    
    @DB
    public Void copyBaseImageCallBack(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CreateVolumeFromBaseImageContext<VolumeApiResult> context) {
        VolumeObject vo = context.vo;
        CopyCommandResult result = callback.getResult();
        VolumeApiResult volResult = new VolumeApiResult(vo);
       
        if (result.isSuccess()) {
            if (result.getPath() != null) {
                vo.setPath(result.getPath());
            }
            vo.processEvent(Event.OperationSuccessed); 
        } else {
            vo.processEvent(Event.OperationFailed);
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
            
        createVolumeFromBaseImageAsync(volume, templateOnPrimaryStore, pd, future);
        return future;
    }

    @Override
    @DB
    public boolean destroyVolume(long volumeId)
            throws ConcurrentOperationException {
        
        VolumeInfo vol = this.volFactory.getVolume(volumeId);
        vol.processEvent(Event.DestroyRequested);
        this.snapshotMgr.deletePoliciesForVolume(volumeId);

        vol.processEvent(Event.OperationSuccessed);
             
        return true;
    }

    @Override
    public AsyncCallFuture<VolumeApiResult> createVolumeFromSnapshot(
            VolumeInfo volume, DataStore store, SnapshotInfo snapshot) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        
        try {
        	DataObject volumeOnStore = store.create(volume);
        	volume.processEvent(Event.CreateOnlyRequested);
        	 CreateVolumeFromBaseImageContext<VolumeApiResult> context = new CreateVolumeFromBaseImageContext<VolumeApiResult>(null, 
        			 (VolumeObject)volume, store, volumeOnStore, future);
             AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller =  AsyncCallbackDispatcher.create(this);
             caller.setCallback(caller.getTarget().createVolumeFromSnapshotCallback(null, null))
             .setContext(context);
        	this.motionSrv.copyAsync(snapshot, volumeOnStore, caller);
        } catch (Exception e) {
        	s_logger.debug("create volume from snapshot failed", e);
        	VolumeApiResult result = new VolumeApiResult(volume);
        	result.setResult(e.toString());
        	future.complete(result);
        }
        
        return future;
    }
    
    protected Void createVolumeFromSnapshotCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, 
    		CreateVolumeFromBaseImageContext<VolumeApiResult> context) {
    	CopyCommandResult result = callback.getResult();
    	VolumeInfo volume = context.vo;
    	VolumeApiResult apiResult = new VolumeApiResult(volume);
    	Event event = null;
    	if (result.isFailed()) {
    		apiResult.setResult(result.getResult());
    		event = Event.OperationFailed;
    	} else {
    		event = Event.OperationSuccessed;
    	}
    	
    	try {
    		volume.processEvent(event);
    	} catch (Exception e) {
    		s_logger.debug("create volume from snapshot failed", e);
    		apiResult.setResult(e.toString());
    	}
    	
    	AsyncCallFuture<VolumeApiResult> future = context.future;
    	future.complete(apiResult);
    	return null;
    }
    
    protected VolumeVO duplicateVolumeOnAnotherStorage(Volume volume, StoragePool pool) {
        Long lastPoolId = volume.getPoolId();
        VolumeVO newVol = new VolumeVO(volume);
        newVol.setPoolId(pool.getId());
        newVol.setFolder(pool.getPath());
        newVol.setPodId(pool.getPodId());
        newVol.setPoolId(pool.getId());
        newVol.setLastPoolId(lastPoolId);
        newVol.setPodId(pool.getPodId());
        return this.volDao.persist(newVol);
    }
    

    private class CopyVolumeContext<T> extends AsyncRpcConext<T> {
        final VolumeInfo srcVolume;
        final VolumeInfo destVolume;
        final DataStore destStore;
        final AsyncCallFuture<VolumeApiResult> future;
        /**
         * @param callback
         */
        public CopyVolumeContext(AsyncCompletionCallback<T> callback, AsyncCallFuture<VolumeApiResult> future, VolumeInfo srcVolume, VolumeInfo destVolume,
                DataStore destStore) {
            super(callback);
            this.srcVolume = srcVolume;
            this.destVolume = destVolume;
            this.destStore = destStore;
            this.future = future;
        }
        
    }
    @Override
    public AsyncCallFuture<VolumeApiResult> copyVolume(VolumeInfo srcVolume,
            DataStore destStore) {
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeApiResult res = new VolumeApiResult(srcVolume);
        try {
            if (!this.snapshotMgr.canOperateOnVolume(srcVolume)) {
                s_logger.debug(
                        "There are snapshots creating on this volume, can not move this volume");
                
                res.setResult("There are snapshots creating on this volume, can not move this volume");
                future.complete(res);
                return future;
            }

            VolumeVO destVol = duplicateVolumeOnAnotherStorage(srcVolume, (StoragePool)destStore);
            VolumeInfo destVolume = this.volFactory.getVolume(destVol.getId(), destStore);
            destVolume.processEvent(Event.CreateOnlyRequested);
            srcVolume.processEvent(Event.CopyingRequested);

            CopyVolumeContext<VolumeApiResult> context = new CopyVolumeContext<VolumeApiResult>(null, future, srcVolume, 
                    destVolume,
                    destStore);
            AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> caller = AsyncCallbackDispatcher.create(this);
            caller.setCallback(caller.getTarget().copyVolumeCallBack(null, null))
            .setContext(context);
            this.motionSrv.copyAsync(srcVolume, destVolume, caller);
        } catch (Exception e) {
            s_logger.debug("Failed to copy volume", e);
            res.setResult(e.toString());
            future.complete(res);
        }
        return future;
    }
    
    protected Void copyVolumeCallBack(AsyncCallbackDispatcher<VolumeServiceImpl, CopyCommandResult> callback, CopyVolumeContext<VolumeApiResult> context) {
        VolumeInfo srcVolume = context.srcVolume;
        VolumeInfo destVolume = context.destVolume;
        CopyCommandResult result = callback.getResult();
        AsyncCallFuture<VolumeApiResult> future = context.future;
        VolumeApiResult res = new VolumeApiResult(destVolume);
        try {
            if (result.isFailed()) {
                res.setResult(result.getResult());
                destVolume.processEvent(Event.OperationFailed);
                srcVolume.processEvent(Event.OperationFailed);
                AsyncCallFuture<VolumeApiResult> destroyFuture = this.expungeVolumeAsync(destVolume);
                destroyFuture.get();
                future.complete(res);
                return null;
            }
            srcVolume.processEvent(Event.OperationSuccessed);
            destVolume.processEvent(Event.OperationSuccessed);
            AsyncCallFuture<VolumeApiResult> destroyFuture = this.expungeVolumeAsync(srcVolume);
            destroyFuture.get();
            future.complete(res);
            return null;
        } catch (Exception e) {
            s_logger.debug("Failed to process copy volume callback",e);
            res.setResult(e.toString());
            future.complete(res);
        }
        
        return null;
    }
    
    @Override
    public AsyncCallFuture<VolumeApiResult> registerVolume(VolumeInfo volume, DataStore store) {
        
        AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
        VolumeObject vo = (VolumeObject) volume;

        CreateVolumeContext<VolumeApiResult> context = new CreateVolumeContext<VolumeApiResult>(null, vo, future);
        AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().registerVolumeCallback(null, null))
        .setContext(context);
        
        dataObjectMgr.createAsync(volume, store, caller, true);
        return future;
    }
    
    protected Void registerVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateVolumeContext<VolumeApiResult> context) {
        CreateCmdResult result = callback.getResult();
        VolumeObject vo = (VolumeObject)context.volume;
        /*if (result.isFailed()) {
            vo.stateTransit(Volume.Event.OperationFailed);
        } else {
            vo.stateTransit(Volume.Event.OperationSucceeded);
        }*/
        VolumeApiResult res = new VolumeApiResult(vo);
        context.future.complete(res);
        return null;
    }
    

	@Override
	public AsyncCallFuture<VolumeApiResult> resize(VolumeInfo volume) {
		AsyncCallFuture<VolumeApiResult> future = new AsyncCallFuture<VolumeApiResult>();
		VolumeApiResult result = new VolumeApiResult(volume);
		try {
			volume.processEvent(Event.ResizeRequested);
		} catch (Exception e) {
			s_logger.debug("Failed to change state to resize", e);
			result.setResult(e.toString());
			future.complete(result);
			return future;
		}
		 CreateVolumeContext<VolumeApiResult> context = new CreateVolumeContext<VolumeApiResult>(null, volume, future);
	        AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> caller = AsyncCallbackDispatcher.create(this);
	        caller.setCallback(caller.getTarget().registerVolumeCallback(null, null))
	        .setContext(context);
		volume.getDataStore().getDriver().resize(volume, caller);
		return future;
	}
	
	protected Void resizeVolumeCallback(AsyncCallbackDispatcher<VolumeServiceImpl, CreateCmdResult> callback, CreateVolumeContext<VolumeApiResult> context) {
		CreateCmdResult result = callback.getResult();
		AsyncCallFuture<VolumeApiResult> future  = context.future;
		VolumeInfo volume = (VolumeInfo)context.volume;
		
		if (result.isFailed()) {
			try {
				volume.processEvent(Event.OperationFailed);
			} catch (Exception e) {
				s_logger.debug("Failed to change state", e);
			}
			VolumeApiResult res = new VolumeApiResult(volume);
			res.setResult(result.getResult());
			future.complete(res);
			return null;
		}
		
		try {
			volume.processEvent(Event.OperationSuccessed);
		} catch(Exception e) {
			s_logger.debug("Failed to change state", e);
			VolumeApiResult res = new VolumeApiResult(volume);
			res.setResult(result.getResult());
			future.complete(res);
			return null;
		}
		
		VolumeApiResult res = new VolumeApiResult(volume);
		future.complete(res);
		
		return null;
	}
	
	

}
