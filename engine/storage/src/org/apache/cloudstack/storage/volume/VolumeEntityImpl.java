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

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.cloud.entity.api.SnapshotEntity;
import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.StorageEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreEntityImpl;
import org.apache.cloudstack.storage.image.TemplateEntityImpl;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.volume.VolumeService.VolumeApiResult;

import com.cloud.utils.exception.CloudRuntimeException;

public class VolumeEntityImpl implements VolumeEntity {
    private VolumeInfo volumeInfo;
    private final VolumeService vs;
    private VolumeApiResult result;
    
    protected VolumeEntityImpl() {
        this.vs = null;
    }
    
    public VolumeEntityImpl(VolumeInfo volumeObject, VolumeService vs) {
        this.volumeInfo = volumeObject;
        this.vs = vs;
    }

    public VolumeInfo getVolumeInfo() {
        return volumeInfo;
    }

    @Override 
    public String getUuid() {
        return volumeInfo.getUuid();
    }

    @Override
    public long getId() {
        return volumeInfo.getId();
    }

    public String getExternalId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCurrentState() {
        return volumeInfo.getCurrentState().toString();
    }

    @Override
    public String getDesiredState() {
        return volumeInfo.getDesiredState().toString();
    }

    @Override
    public Date getCreatedTime() {
        return volumeInfo.getCreatedDate();
    }

    @Override
    public Date getLastUpdatedTime() {
        return volumeInfo.getUpdatedDate();
    }

    @Override
    public String getOwner() {
        return volumeInfo.getOwner();
    }

    @Override
    public Map<String, String> getDetails(String source) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getDetailSources() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addDetail(String source, String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delDetail(String source, String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDetail(String source, String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SnapshotEntity takeSnapshotOf(boolean full) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String reserveForMigration(long expirationTime) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void migrate(String reservationToken) {
        // TODO Auto-generated method stub

    }

    @Override
    public VolumeEntity setupForCopy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void copy(VolumeEntity dest) {
        // TODO Auto-generated method stub

    }

    @Override
    public void attachTo(String vm, long deviceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void detachFrom() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        vs.deleteVolume(volumeInfo);
    }

    @Override
    public long getSize() {
        return volumeInfo.getSize();
    }

    @Override
    public VolumeDiskType getDiskType() {
        return volumeInfo.getDiskType();
    }

    @Override
    public VolumeType getType() {
        return volumeInfo.getType();
    }

    @Override
    public StorageEntity getDataStore() {
        return new PrimaryDataStoreEntityImpl(volumeInfo.getDataStore());
    }

    @Override
    public boolean createVolumeFromTemplate(long dataStoreId, VolumeDiskType diskType, TemplateEntity template) {
        TemplateInfo ti = ((TemplateEntityImpl)template).getTemplateInfo();
        
        AsyncCallbackDispatcher<VolumeEntityImpl> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createVolumeFromTemplateAsyncCallback(null, null));
          
        vs.createVolumeFromTemplateAsync(volumeInfo, dataStoreId, diskType, ti, caller);
        try {
            synchronized (volumeInfo) {
                volumeInfo.wait();
            }
        } catch (InterruptedException e) {
           throw new CloudRuntimeException("wait volume info failed", e);
        }
        return true;
    }
    
   
    private Void createVolumeFromTemplateAsyncCallback(AsyncCompletionCallback<VolumeInfo> callback, Object context) {
        synchronized (volumeInfo) {
            volumeInfo.notify();
        }
        return null;
    }

    @Override
    public boolean createVolume(long dataStoreId, VolumeDiskType diskType) {
        AsyncCallbackDispatcher<VolumeEntityImpl> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().createVolumeCallback(null, null));
        vs.createVolumeAsync(volumeInfo, dataStoreId, diskType, caller);
        try {
            synchronized (volumeInfo) {
                volumeInfo.wait();
            }
            if (result.isSuccess()) {
                return true;
            } else {
                throw new CloudRuntimeException("Failed to create volume:" + result.getResult());
            }
        } catch (InterruptedException e) {
           throw new CloudRuntimeException("wait volume info failed", e);
        }
    }
    
    private Void createVolumeCallback(AsyncCallbackDispatcher<VolumeApiResult> callback, Object context) {
        synchronized (volumeInfo) {
            this.result = callback.getResult();
            volumeInfo.notify();
        }
        return null;
    }

}
