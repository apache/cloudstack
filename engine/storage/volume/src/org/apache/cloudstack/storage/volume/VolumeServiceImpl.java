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

import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.BaseImage;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCallbackHandler;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.manager.PrimaryDataStoreManager;
import org.apache.cloudstack.storage.image.TemplateEntityImpl;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.motion.ImageMotionService;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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

    protected void createBaseImageAsync(PrimaryDataStore dataStore, TemplateInfo template, AsyncCompletionCallback<TemplateOnPrimaryDataStoreObject> callback) {
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

        AsyncCallbackDispatcher caller = new AsyncCallbackDispatcher(this)
            .setParentCallback(callback)
            .setOperationName("volumeservice.createbaseimage.callback")
            .setContextParam("templateOnPrimary", templateOnPrimaryStoreObj);

        imageMotion.copyTemplateAsync(templateOnPrimaryStoreObj, caller);
    }

    @AsyncCallbackHandler(operationName="volumeservice.createbaseimage.callback")
    public void createBaseImageCallback(AsyncCallbackDispatcher callback) {
        CommandResult result = callback.getResult();
        TemplateOnPrimaryDataStoreObject templateOnPrimaryStoreObj = callback.getContextParam("templateOnPrimary");
        if (result.isSuccess()) {
            templateOnPrimaryStoreObj.updateStatus(Status.DOWNLOADED);
            templateOnPrimaryStoreObj.stateTransit(TemplateOnPrimaryDataStoreStateMachine.Event.OperationSuccessed);
        } else {
            templateOnPrimaryStoreObj.updateStatus(Status.ABANDONED);
            templateOnPrimaryStoreObj.stateTransit(TemplateOnPrimaryDataStoreStateMachine.Event.OperationFailed);
        }
        
        AsyncCallbackDispatcher parentCaller = callback.getParentCallback();
        VolumeInfo volume = parentCaller.getContextParam("volume");
        PrimaryDataStore pd = parentCaller.getContextParam("primary");
        AsyncCallbackDispatcher caller = new AsyncCallbackDispatcher(this)
              .setParentCallback(parentCaller)
              .setOperationName("volumeservice.createvolumefrombaseimage.callback");
        
        createVolumeFromBaseImageAsync(volume, templateOnPrimaryStoreObj, pd, caller);
    }
    
    protected void createVolumeFromBaseImageAsync(VolumeInfo volume, TemplateOnPrimaryDataStoreInfo templateOnPrimaryStore, PrimaryDataStore pd, AsyncCompletionCallback<VolumeObject> callback) {
        VolumeObject vo = (VolumeObject) volume;
        try {
            vo.stateTransit(Volume.Event.CreateRequested);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.toString());
        }


        AsyncCallbackDispatcher caller = new AsyncCallbackDispatcher(this)
        .setParentCallback(callback)
        .setOperationName("volumeservice.createVolumeFromBaseImageCallback")
        .setContextParam("volume", vo);

        pd.createVoluemFromBaseImageAsync(volume, templateOnPrimaryStore, caller);
    }
    
    @AsyncCallbackHandler(operationName="volumeservice.createVolumeFromBaseImageCallback")
    public void createVolumeFromBaseImageCallback(AsyncCallbackDispatcher callback) {
        VolumeObject vo = callback.getContextParam("volume");
        CommandResult result = callback.getResult();
        if (result.isSuccess()) {
            vo.stateTransit(Volume.Event.OperationSucceeded); 
        } else {
            vo.stateTransit(Volume.Event.OperationFailed);
        }
        
        AsyncCallbackDispatcher parentCall = callback.getParentCallback();
        parentCall.complete(vo);
    }
    
    @AsyncCallbackHandler(operationName="volumeservice.createvolumefrombaseimage.callback")
    public void createVolumeFromBaseImageAsyncCallback(AsyncCallbackDispatcher callback) {
        AsyncCallbackDispatcher parentCall = callback.getParentCallback();
        VolumeObject vo = callback.getResult();
        parentCall.complete(vo);
    }

    @Override
    public void createVolumeFromTemplateAsync(VolumeInfo volume, long dataStoreId, VolumeDiskType diskType, TemplateInfo template, AsyncCompletionCallback<VolumeInfo> callback) {
        PrimaryDataStore pd = dataStoreMgr.getPrimaryDataStore(dataStoreId);
        TemplateOnPrimaryDataStoreInfo templateOnPrimaryStore = pd.getTemplate(template);
        if (templateOnPrimaryStore == null) {
            AsyncCallbackDispatcher caller = new AsyncCallbackDispatcher(this).setParentCallback(callback)
                    .setOperationName("volumeservice.createbaseimage.callback")
                    .setContextParam("volume", volume)
                    .setContextParam("primary", pd);
             createBaseImageAsync(pd, template, caller);
            return;
        }

        AsyncCallbackDispatcher caller = new AsyncCallbackDispatcher(this)
            .setParentCallback(callback)
            .setOperationName("volumeservice.createvolumefrombaseimage.callback");
            
        createVolumeFromBaseImageAsync(volume, templateOnPrimaryStore, pd, caller);
    }

    @Override
    public TemplateOnPrimaryDataStoreInfo grantAccess(TemplateOnPrimaryDataStoreInfo template, EndPoint endPoint) {
        // TODO Auto-generated method stub
        return null;
    }
}
