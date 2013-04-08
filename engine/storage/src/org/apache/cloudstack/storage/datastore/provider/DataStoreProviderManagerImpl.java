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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.response.StorageProviderResponse;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider.DataStoreProviderType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.db.DataStoreProviderDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
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
    @Inject
    PrimaryDataStoreProviderManager primaryDataStoreProviderMgr;
    @Override
    public DataStoreProvider getDataStoreProvider(String name) {
        return providerMap.get(name);
    }

    @Override
    public List<DataStoreProvider> getDataStoreProviders() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public List<StorageProviderResponse> getPrimayrDataStoreProviders() {
        List<StorageProviderResponse> providers = new ArrayList<StorageProviderResponse>();
        for (DataStoreProvider provider : providerMap.values()) {
            if (provider instanceof PrimaryDataStoreProvider) {
                StorageProviderResponse response = new StorageProviderResponse();
                response.setName(provider.getName());
                response.setType(DataStoreProvider.DataStoreProviderType.PRIMARY.toString());
                providers.add(response);
            }
        }
        return providers;
    }
    
    public List<StorageProviderResponse> getImageDataStoreProviders() {
        List<StorageProviderResponse> providers = new ArrayList<StorageProviderResponse>();
        for (DataStoreProvider provider : providerMap.values()) {
            if (provider instanceof ImageDataStoreProvider) {
                StorageProviderResponse response = new StorageProviderResponse();
                response.setName(provider.getName());
                response.setType(DataStoreProvider.DataStoreProviderType.IMAGE.toString());
                providers.add(response);
            }
        }
        return providers;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
    		throws ConfigurationException {
        Map<String, Object> copyParams = new HashMap<String, Object>(params);

        for (DataStoreProvider provider : providers) {
            String providerName = provider.getName();
            if (providerMap.get(providerName) != null) {
                s_logger.debug("Failed to register data store provider, provider name: " + providerName + " is not unique");
                return false;
            }
            
            s_logger.debug("registering data store provider:" + provider.getName());
            
            providerMap.put(providerName, provider);
            try {
                boolean registrationResult = provider.configure(copyParams);
                if (!registrationResult) {
                    providerMap.remove(providerName);
                    s_logger.debug("Failed to register data store provider: " + providerName);
                    return false;
                }
                
                Set<DataStoreProviderType> types = provider.getTypes();
                if (types.contains(DataStoreProviderType.PRIMARY)) {
                    primaryDataStoreProviderMgr.registerDriver(provider.getName(), (PrimaryDataStoreDriver)provider.getDataStoreDriver());
                    primaryDataStoreProviderMgr.registerHostListener(provider.getName(), provider.getHostListener());
                }
            } catch(Exception e) {
                s_logger.debug("configure provider failed", e);
                providerMap.remove(providerName);
            }
        }
  
        return true;
    }

    @Override
    public DataStoreProvider getDefaultPrimaryDataStoreProvider() {
        return this.getDataStoreProvider("ancient primary data store provider");
    }

    @Override
    public List<StorageProviderResponse> getDataStoreProviders(String type) {
        if (type == null) {
            throw new InvalidParameterValueException("Invalid parameter, need to specify type: either primary or image");
        }
        if (type.equalsIgnoreCase(DataStoreProvider.DataStoreProviderType.PRIMARY.toString())) {
            return this.getPrimayrDataStoreProviders();
        } else if (type.equalsIgnoreCase(DataStoreProvider.DataStoreProviderType.IMAGE.toString())) {
            return this.getImageDataStoreProviders();
        } else {
            throw new InvalidParameterValueException("Invalid parameter: " + type);
        }
    }
}
