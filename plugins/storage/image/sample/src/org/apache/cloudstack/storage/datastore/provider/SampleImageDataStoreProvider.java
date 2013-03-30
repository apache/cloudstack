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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataStoreProvider;
import org.apache.cloudstack.storage.datastore.driver.SampleImageDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.lifecycle.SampleImageDataStoreLifeCycle;
import org.apache.cloudstack.storage.image.ImageDataStoreDriver;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreProviderManager;
import org.apache.cloudstack.storage.image.store.lifecycle.ImageDataStoreLifeCycle;

import com.cloud.utils.component.ComponentContext;

public class SampleImageDataStoreProvider implements ImageDataStoreProvider {
    private final String name = "sample image data store provider";
    protected ImageDataStoreLifeCycle lifeCycle;
    protected ImageDataStoreDriver driver;
    @Inject
    ImageDataStoreProviderManager storeMgr;
    long id;
    String uuid;
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
        lifeCycle = ComponentContext.inject(SampleImageDataStoreLifeCycle.class);
        driver = ComponentContext.inject(SampleImageDataStoreDriverImpl.class);

        storeMgr.registerDriver(this.getName(), driver);
        return true;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types =  new HashSet<DataStoreProviderType>();
        types.add(DataStoreProviderType.IMAGE);
        return types;
    }

    @Override
    public DataStoreDriver getDataStoreDriver() {
        return this.driver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return null;
    }
}
