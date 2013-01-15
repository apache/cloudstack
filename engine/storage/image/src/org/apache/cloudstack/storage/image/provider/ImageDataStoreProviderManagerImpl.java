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
package org.apache.cloudstack.storage.image.provider;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.image.db.ImageDataDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreProviderDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreProviderVO;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.image.store.ImageDataStore;
import org.springframework.stereotype.Component;

@Component
public class ImageDataStoreProviderManagerImpl implements ImageDataStoreProviderManager {
    @Inject
    ImageDataStoreProviderDao providerDao;
    @Inject
    ImageDataStoreDao dataStoreDao;
    @Inject
    ImageDataDao imageDataDao;
    @Inject
    List<ImageDataStoreProvider> providers;

    @Override
    public ImageDataStoreProvider getProvider(long providerId) {

        return null;
    }

    @Override
    public ImageDataStoreProvider getProvider(String name) {
        for (ImageDataStoreProvider provider : providers) {
            if (provider.getName().equalsIgnoreCase(name)) {
                return provider;
            }
        }
        return null;
    }

    @Override
    public ImageDataStore getDataStore(Long dataStoreId) {
        if (dataStoreId == null) {
            return null;
        }

        ImageDataStoreVO idsv = dataStoreDao.findById(dataStoreId);
        if (idsv == null) {
            return null;
        }
        
        long providerId = idsv.getProvider();
        ImageDataStoreProviderVO idspv = providerDao.findById(providerId);
        ImageDataStoreProvider provider = getProvider(idspv.getName());
        return provider.getImageDataStore(dataStoreId);
    }

    @Override
    public ImageDataStore getDataStoreFromTemplateId(long templateId) {
        ImageDataVO iddv = imageDataDao.findById(templateId);
        return getDataStore(iddv.getImageDataStoreId());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        List<ImageDataStoreProviderVO> existingProviders = providerDao.listAll();
        //TODO: hold global lock
        boolean foundExistingProvider = false;
        for (ImageDataStoreProvider provider : providers) {
            foundExistingProvider = false;
           for (ImageDataStoreProviderVO existingProvider : existingProviders) {
               if (provider.getName().equalsIgnoreCase(existingProvider.getName())) {
                   foundExistingProvider = true;
                   break;
               }
           }
           
           if (!foundExistingProvider) {
               //add a new provider into db
               ImageDataStoreProviderVO nProvider = new ImageDataStoreProviderVO();
               nProvider.setName(provider.getName());
               nProvider = providerDao.persist(nProvider);
               provider.register(nProvider.getId());
           }
           provider.init();
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
    public List<ImageDataStoreProvider> listProvider() {
        return providers;
    }
}
