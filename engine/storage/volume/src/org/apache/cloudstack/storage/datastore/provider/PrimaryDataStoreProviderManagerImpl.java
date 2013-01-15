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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreProviderDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreProviderVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;

@Component
public class PrimaryDataStoreProviderManagerImpl implements PrimaryDataStoreProviderManager {
    @Inject
    List<PrimaryDataStoreProvider> providers;
    @Inject
    PrimaryDataStoreProviderDao providerDao;
    
    @Override
    public PrimaryDataStoreProvider getDataStoreProvider(Long providerId) {
        for (PrimaryDataStoreProvider provider : providers) {
            if (provider.getId() == providerId) {
                return provider;
            }
        }
        return null;
    }

    @Override
    @DB
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        List<PrimaryDataStoreProviderVO> providerVos = providerDao.listAll();
        for (PrimaryDataStoreProvider provider : providers) {
            boolean existingProvider = false;
            for (PrimaryDataStoreProviderVO providerVo : providerVos) {
                if (providerVo.getName().equalsIgnoreCase(provider.getName())) {
                    existingProvider = true;
                    break;
                }
            }
            if (!existingProvider) {
                PrimaryDataStoreProviderVO dataStoreProvider = new PrimaryDataStoreProviderVO();
                dataStoreProvider.setName(provider.getName());
                dataStoreProvider = providerDao.persist(dataStoreProvider);
            }
            
            provider.configure();
        }
        return true;
    }

    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PrimaryDataStoreProvider getDataStoreProvider(String name) {
        for (PrimaryDataStoreProvider provider : providers) {
            if (provider.getName().equalsIgnoreCase(name)) {
                return provider;
            }
        }
        return null;
    }

    @Override
    public List<PrimaryDataStoreProvider> getDataStoreProviders() {
        return providers;
    }
}
