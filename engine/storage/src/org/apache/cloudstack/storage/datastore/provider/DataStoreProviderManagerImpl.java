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
package org.apache.cloudstack.storage.datastore.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.db.DataStoreProviderDao;
import org.apache.cloudstack.storage.datastore.db.DataStoreProviderVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ManagerBase;

@Component
public class DataStoreProviderManagerImpl extends ManagerBase implements DataStoreProviderManager {
    private static final Logger s_logger = Logger
            .getLogger(DataStoreProviderManagerImpl.class);
    @Inject
    List<DataStoreProvider> providers;
    @Inject
    DataStoreProviderDao providerDao;
    protected Map<String, DataStoreProvider> providerMap = new HashMap<String, DataStoreProvider>();
    @Override
    public DataStoreProvider getDataStoreProviderByUuid(String uuid) {
        return providerMap.get(uuid);
    }

    @Override
    public DataStoreProvider getDataStoreProvider(String name) {
        DataStoreProviderVO dspv = providerDao.findByName(name);
        return providerMap.get(dspv.getUuid());
    }

    @Override
    public List<DataStoreProvider> getDataStoreProviders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
    		throws ConfigurationException {
        Map<String, Object> copyParams = new HashMap<String, Object>(params);

    	//TODO: hold global lock
        List<DataStoreProviderVO> providerVos = providerDao.listAll();
        for (DataStoreProvider provider : providers) {
            boolean existingProvider = false;
            DataStoreProviderVO providerVO = null;
            for (DataStoreProviderVO prov : providerVos) {
                if (prov.getName().equalsIgnoreCase(provider.getName())) {
                    existingProvider = true;
                    providerVO = prov;
                    break;
                }
            }
            String uuid = null;
            if (!existingProvider) {
                uuid = UUID.nameUUIDFromBytes(provider.getName().getBytes()).toString();
                providerVO = new DataStoreProviderVO();
                providerVO.setName(provider.getName());
                providerVO.setUuid(uuid);
                providerVO = providerDao.persist(providerVO);
            } else {
                uuid = providerVO.getUuid();
            }
            copyParams.put("uuid", uuid);
            copyParams.put("id", providerVO.getId());
            providerMap.put(uuid, provider);
            try {
                boolean registrationResult = provider.configure(copyParams);
                if (!registrationResult) {
                    providerMap.remove(uuid);
                }
            } catch(Exception e) {
                s_logger.debug("configure provider failed", e);
                providerMap.remove(uuid);
            }
        }
  
        return true;
    }

    @Override
    public DataStoreProvider getDataStoreProviderById(long id) {
        DataStoreProviderVO provider = providerDao.findById(id);
        return providerMap.get(provider.getUuid());
    }

    @Override
    public DataStoreProvider getDefaultPrimaryDataStoreProvider() {
        return this.getDataStoreProvider("ancient primary data store provider");
    }
}
