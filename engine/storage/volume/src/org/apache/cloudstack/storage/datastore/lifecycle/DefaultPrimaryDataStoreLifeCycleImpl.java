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
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
import org.apache.cloudstack.storage.datastore.DataStoreStatus;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;

public class DefaultPrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    protected PrimaryDataStore dataStore;
    protected PrimaryDataStoreDao dataStoreDao;
    public DefaultPrimaryDataStoreLifeCycleImpl(PrimaryDataStoreDao dataStoreDao) {
        this.dataStoreDao = dataStoreDao;
    }
    
    @Override
    public void setDataStore(PrimaryDataStoreInfo dataStore) {
        this.dataStore = (PrimaryDataStore)dataStore;
    }
    
    @Override
    public boolean initialize(Map<String, String> dsInfos) {
        PrimaryDataStoreVO dataStore = dataStoreDao.findById(this.dataStore.getId());
        dataStore.setStatus(DataStoreStatus.Initialized);
        dataStoreDao.update(this.dataStore.getId(), dataStore);
        //TODO: add extension point for each data store
        return true;
    }

    protected void attachCluster() {
        //send down AttachPrimaryDataStoreCmd command to all the hosts in the cluster
        AttachPrimaryDataStoreCmd cmd = new AttachPrimaryDataStoreCmd(this.dataStore.getDataStoreTO());
        for (EndPoint ep : dataStore.getEndPoints()) {
            ep.sendMessage(cmd);
        } 
    }
    
    @Override
    public boolean attachCluster(ClusterScope scope) {
        PrimaryDataStoreVO dataStoreVO = dataStoreDao.findById(this.dataStore.getId());
        dataStoreVO.setDataCenterId(scope.getZoneId());
        dataStoreVO.setPodId(scope.getPodId());
        dataStoreVO.setClusterId(scope.getScopeId());
        dataStoreVO.setStatus(DataStoreStatus.Attaching);
        dataStoreDao.update(dataStoreVO.getId(), dataStoreVO);
        
        attachCluster();
        
        dataStoreVO = dataStoreDao.findById(this.dataStore.getId());
        dataStoreVO.setStatus(DataStoreStatus.Up);
        dataStoreDao.update(dataStoreVO.getId(), dataStoreVO);
        
        return true;
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
