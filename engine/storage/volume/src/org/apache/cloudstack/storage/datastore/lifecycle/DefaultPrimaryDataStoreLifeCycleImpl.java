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
package org.apache.cloudstack.storage.datastore.lifecycle;

import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.DataStoreStatus;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;

public class DefaultPrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    protected PrimaryDataStoreInfo dataStore;
    protected PrimaryDataStoreDao dataStoreDao;
    public DefaultPrimaryDataStoreLifeCycleImpl(PrimaryDataStoreDao dataStoreDao) {
        this.dataStoreDao = dataStoreDao;
    }
    
    @Override
    public void setDataStore(PrimaryDataStoreInfo dataStore) {
        this.dataStore = dataStore;
    }
    
    @Override
    public boolean initialize(Map<String, String> dsInfos) {
        PrimaryDataStoreVO dataStore = dataStoreDao.findById(this.dataStore.getId());
        dataStore.setStatus(DataStoreStatus.Initialized);
        dataStoreDao.update(this.dataStore.getId(), dataStore);
        //TODO: add extension point for each data store
        return true;
    }

    @Override
    public boolean attachCluster(ClusterScope scope) {
        PrimaryDataStoreVO dataStore = dataStoreDao.findById(this.dataStore.getId());
        dataStore.setDataCenterId(scope.getZoneId());
        dataStore.setPodId(scope.getPodId());
        dataStore.setClusterId(scope.getScopeId());
        dataStoreDao.update(this.dataStore.getId(), dataStore);
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
    public boolean maintain() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean cancelMaintain() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteDataStore() {
        // TODO Auto-generated method stub
        return false;
    }

}
