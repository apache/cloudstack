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
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;
import org.springframework.stereotype.Component;

import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class PrimaryDataStoreHelper {
    @Inject
    private PrimaryDataStoreDao dataStoreDao;
    public PrimaryDataStoreVO createPrimaryDataStore(Map<String, String> params) {
        PrimaryDataStoreVO dataStoreVO = dataStoreDao.findPoolByUUID(params.get("uuid"));
        if (dataStoreVO != null) {
            throw new CloudRuntimeException("duplicate uuid: " + params.get("uuid"));
        }
        
        dataStoreVO = new PrimaryDataStoreVO();
        dataStoreVO.setStorageProviderId(Long.parseLong(params.get("providerId")));
        dataStoreVO.setHostAddress(params.get("server"));
        dataStoreVO.setPath(params.get("path"));
        dataStoreVO.setPoolType(params.get("protocol"));
        dataStoreVO.setPort(Integer.parseInt(params.get("port")));
        dataStoreVO.setName(params.get("name"));
        dataStoreVO.setUuid(params.get("uuid"));
        dataStoreVO = dataStoreDao.persist(dataStoreVO);
        return dataStoreVO;
    }
    
    public boolean deletePrimaryDataStore(long id) {
        PrimaryDataStoreVO dataStoreVO = dataStoreDao.findById(id);
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
