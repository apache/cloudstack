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
package org.apache.cloudstack.storage.image.store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.storage.image.ImageDataStoreDriver;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreHelper;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreManager;
import org.apache.cloudstack.storage.image.driver.AncientImageDataStoreDriverImpl;
import org.apache.cloudstack.storage.image.store.lifecycle.DefaultImageDataStoreLifeCycle;
import org.apache.cloudstack.storage.image.store.lifecycle.ImageDataStoreLifeCycle;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ComponentContext;

@Component
public class AncientImageDataStoreProvider implements ImageDataStoreProvider {

    private final String name = "ancient image data store";
    protected ImageDataStoreLifeCycle lifeCycle;
    protected ImageDataStoreDriver driver;
    @Inject
    ImageDataStoreManager storeMgr;
    @Inject
    ImageDataStoreHelper helper;

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return lifeCycle;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        lifeCycle = ComponentContext.inject(DefaultImageDataStoreLifeCycle.class);
        driver = ComponentContext.inject(AncientImageDataStoreDriverImpl.class);

        storeMgr.registerDriver(this.getName(), driver);
        
        Map<String, Object> infos = new HashMap<String, Object>();
        String dataStoreName = UUID.nameUUIDFromBytes(this.name.getBytes()).toString();
        infos.put("name", dataStoreName);
        infos.put("uuid", dataStoreName);
        infos.put("protocol", "http");
        infos.put("scope", ScopeType.GLOBAL);
        infos.put("providerName", this.getName());
        DataStoreLifeCycle lifeCycle = this.getDataStoreLifeCycle();
        lifeCycle.initialize(infos);
        return true;
    }

    @Override
    public DataStoreDriver getDataStoreDriver() {
        return this.driver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return null;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types =  new HashSet<DataStoreProviderType>();
        types.add(DataStoreProviderType.IMAGE);
        return types;
    }

}
