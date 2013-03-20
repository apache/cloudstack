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
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.StorageEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreEntityImpl;

public class VolumeEntityImpl implements VolumeEntity {
    private VolumeInfo volumeInfo;
    private final VolumeService vs;
    private VolumeApiResult result;
    
    public VolumeEntityImpl() {
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
        return null;
    }

    @Override
    public String getDesiredState() {
        return null;
    }

    @Override
    public Date getCreatedTime() {
        return null;
    }

    @Override
    public Date getLastUpdatedTime() {
        return null;
    }

    @Override
    public String getOwner() {
        return null;
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
    public long getSize() {
        return volumeInfo.getSize();
    }

    @Override
    public DiskFormat getDiskType() {
         return null;
    }

    @Override
    public VolumeType getType() {
        return null;
    }

    @Override
    public StorageEntity getDataStore() {
        return new PrimaryDataStoreEntityImpl((PrimaryDataStoreInfo) volumeInfo.getDataStore());
    }

    @Override
    public void destroy() {
        /*AsyncCallFuture<VolumeApiResult> future = vs.deleteVolumeAsync(volumeInfo);
        try {
            result = future.get();
            if (!result.isSuccess()) {
                throw new CloudRuntimeException("Failed to create volume:" + result.getResult());
            }
        } catch (InterruptedException e) {
           throw new CloudRuntimeException("wait to delete volume info failed", e);
        } catch (ExecutionException e) {
            throw new CloudRuntimeException("wait to delete volume failed", e);
        }*/
    }

	@Override
	public Map<String, String> getDetails() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addDetail(String name, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delDetail(String name, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateDetail(String name, String value) {
		// TODO Auto-generated method stub
		
	}

}
