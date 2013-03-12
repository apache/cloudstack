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
package org.apache.cloudstack.storage.datastore;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.datacenter.entity.api.StorageEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePoolStatus;

public class PrimaryDataStoreEntityImpl implements StorageEntity {
    private PrimaryDataStoreInfo dataStore;
    
    public PrimaryDataStoreEntityImpl(PrimaryDataStoreInfo dataStore) {
        this.dataStore = dataStore;
    }
    
    @Override
    public boolean enable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean disable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deactivate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean reactivate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getUuid() {
        return this.dataStore.getUuid();
    }

    @Override
    public long getId() {
        return this.dataStore.getId();
    }

    @Override
    public String getCurrentState() {
       return null;
    }

    @Override
    public String getDesiredState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getCreatedTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getLastUpdatedTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOwner() {
        // TODO Auto-generated method stub
        return null;
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

    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public State getState() {
        //return this.dataStore.getManagedState();
        return null;
    }

    @Override
    public String getName() {
        return this.dataStore.getName();
    }

    @Override
    public StoragePoolType getPoolType() {
        return null;
    }

    @Override
    public Date getCreated() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getUpdateTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getDataCenterId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getCapacityBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getAvailableBytes() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Long getClusterId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHostAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUserInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isShared() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLocal() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public StoragePoolStatus getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Long getPodId() {
        // TODO Auto-generated method stub
        return null;
    }


    public String getStorageType() {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public void persist() {
		// TODO Auto-generated method stub
		
	}

    @Override
    public Long getStorageProviderId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isInMaintenance() {
        // TODO Auto-generated method stub
        return false;
    }

}
