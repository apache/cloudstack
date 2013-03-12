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
package org.apache.cloudstack.storage.volume.datastore;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.springframework.stereotype.Component;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class PrimaryDataStoreHelper {
    @Inject
    private PrimaryDataStoreDao dataStoreDao;
    public StoragePoolVO createPrimaryDataStore(Map<String, Object> params) {
        StoragePoolVO dataStoreVO = dataStoreDao.findPoolByUUID((String)params.get("uuid"));
        if (dataStoreVO != null) {
            throw new CloudRuntimeException("duplicate uuid: " + params.get("uuid"));
        }
        
        dataStoreVO = new StoragePoolVO();
        dataStoreVO.setStorageProviderId(Long.parseLong((String)params.get("providerId")));
        dataStoreVO.setHostAddress((String)params.get("server"));
        dataStoreVO.setPath((String)params.get("path"));
        dataStoreVO.setPoolType((StoragePoolType)params.get("protocol"));
        dataStoreVO.setPort(Integer.parseInt((String)params.get("port")));
        dataStoreVO.setName((String)params.get("name"));
        dataStoreVO.setUuid((String)params.get("uuid"));
        dataStoreVO = dataStoreDao.persist(dataStoreVO);
        return dataStoreVO;
    }
    
    public boolean deletePrimaryDataStore(long id) {
        StoragePoolVO dataStoreVO = dataStoreDao.findById(id);
        if (dataStoreVO == null) {
            throw new CloudRuntimeException("can't find store: " + id);
        }
        dataStoreDao.remove(id);
        return true;
    }
    
    public void attachCluster(DataStore dataStore) {
        //send down AttachPrimaryDataStoreCmd command to all the hosts in the cluster
        AttachPrimaryDataStoreCmd cmd = new AttachPrimaryDataStoreCmd(dataStore.getUri());
        /*for (EndPoint ep : dataStore.getEndPoints()) {
            ep.sendMessage(cmd);
        } */
    }
    
    
}
