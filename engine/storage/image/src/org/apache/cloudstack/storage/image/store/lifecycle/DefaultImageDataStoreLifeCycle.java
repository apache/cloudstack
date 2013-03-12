// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.image.store.lifecycle;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreHelper;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreManager;
import org.apache.cloudstack.storage.image.db.ImageDataStoreDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;

import com.cloud.agent.api.StoragePoolInfo;

public class DefaultImageDataStoreLifeCycle implements ImageDataStoreLifeCycle {
    @Inject
	protected ImageDataStoreDao imageStoreDao;
	@Inject
	ImageDataStoreHelper imageStoreHelper;
	@Inject
	ImageDataStoreManager imageStoreMgr;
	public DefaultImageDataStoreLifeCycle() {
	}


    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        ImageDataStoreVO ids = imageStoreHelper.createImageDataStore(dsInfos);
        return imageStoreMgr.getImageDataStore(ids.getId());
    }


    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean attachHost(DataStore store, HostScope scope,
            StoragePoolInfo existingInfo) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean dettach() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean unmanaged() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean maintain(long storeId) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean cancelMaintain(long storeId) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean deleteDataStore(long storeId) {
        // TODO Auto-generated method stub
        return false;
    }


  


}
