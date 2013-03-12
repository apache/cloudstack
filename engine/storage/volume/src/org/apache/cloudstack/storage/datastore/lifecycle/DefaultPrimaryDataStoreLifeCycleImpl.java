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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
import org.apache.cloudstack.storage.command.CreatePrimaryDataStoreCmd;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.endpoint.EndPointSelector;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePoolStatus;

public class DefaultPrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    @Inject
    EndPointSelector selector;
    @Inject
    PrimaryDataStoreDao dataStoreDao;
    @Inject
    HostDao hostDao;
    @Inject
    PrimaryDataStoreHelper primaryStoreHelper;
    @Inject
    PrimaryDataStoreProviderManager providerMgr;
    public DefaultPrimaryDataStoreLifeCycleImpl() {
    }
    
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        
        StoragePoolVO storeVO = primaryStoreHelper.createPrimaryDataStore(dsInfos); 
        return providerMgr.getPrimaryDataStore(storeVO.getId());
    }

    protected void attachCluster(DataStore store) {
        //send down AttachPrimaryDataStoreCmd command to all the hosts in the cluster
        List<EndPoint> endPoints = selector.selectAll(store);
        CreatePrimaryDataStoreCmd createCmd = new CreatePrimaryDataStoreCmd(store.getUri());
        EndPoint ep = endPoints.get(0);
        HostVO host = hostDao.findById(ep.getId());
        if (host.getHypervisorType() == HypervisorType.XenServer) {
            ep.sendMessage(createCmd);
        }
        
        endPoints.get(0).sendMessage(createCmd);
        AttachPrimaryDataStoreCmd cmd = new AttachPrimaryDataStoreCmd(store.getUri());
        for (EndPoint endp : endPoints) {
            endp.sendMessage(cmd);
        }
    }
    
    @Override
    public boolean attachCluster(DataStore dataStore, ClusterScope scope) {
        StoragePoolVO dataStoreVO = dataStoreDao.findById(dataStore.getId());
        dataStoreVO.setDataCenterId(scope.getZoneId());
        dataStoreVO.setPodId(scope.getPodId());
        dataStoreVO.setClusterId(scope.getScopeId());
        dataStoreVO.setStatus(StoragePoolStatus.Attaching);
        dataStoreVO.setScope(scope.getScopeType());
        dataStoreDao.update(dataStoreVO.getId(), dataStoreVO);
        
        
        attachCluster(dataStore);
        
        dataStoreVO = dataStoreDao.findById(dataStore.getId());
        dataStoreVO.setStatus(StoragePoolStatus.Up);
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



    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope,
            StoragePoolInfo existingInfo) {
        // TODO Auto-generated method stub
        return false;
    }

}
