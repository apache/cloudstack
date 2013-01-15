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

import javax.inject.Inject;

import org.apache.cloudstack.storage.image.db.ImageDataStoreDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreProviderDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreProviderVO;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;
import org.apache.cloudstack.storage.image.driver.ImageDataStoreDriver;
import org.apache.cloudstack.storage.image.driver.ImageDataStoreDriverImpl;
import org.apache.cloudstack.storage.image.store.ImageDataStore;
import org.apache.cloudstack.storage.image.store.ImageDataStoreImpl;
import org.apache.cloudstack.storage.image.store.lifecycle.DefaultImageDataStoreLifeCycle;
import org.apache.cloudstack.storage.image.store.lifecycle.ImageDataStoreLifeCycle;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ComponentContext;

@Component
public class DefaultImageDataStoreProvider implements ImageDataStoreProvider {
    private final String providerName = "DefaultProvider";
    @Inject
    ImageDataStoreProviderDao providerDao;
    @Inject
    ImageDataStoreDao imageStoreDao;
    ImageDataStoreProviderVO provider;

    @Override
    public ImageDataStore getImageDataStore(long imageStoreId) {
        ImageDataStoreVO idsv = imageStoreDao.findById(imageStoreId);
        ImageDataStoreDriver driver = new ImageDataStoreDriverImpl();
        ImageDataStore ids = new ImageDataStoreImpl(idsv, driver, false);
        ids = ComponentContext.inject(ids);
        return ids;
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public boolean register(long providerId) {
        return true;
    }

    @Override
    public boolean init() {
        provider = providerDao.findByName(providerName);
        return true;
    }

    @Override
    public ImageDataStoreLifeCycle getLifeCycle() {
        return new DefaultImageDataStoreLifeCycle(this, provider, imageStoreDao);
    }
}
