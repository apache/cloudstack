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
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectStoreProvider;
import org.apache.cloudstack.storage.datastore.driver.EcsObjectStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.lifecycle.EcsObjectStoreLifeCycleImpl;
import org.apache.cloudstack.storage.object.ObjectStoreDriver;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreHelper;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.apache.cloudstack.storage.object.store.lifecycle.ObjectStoreLifeCycle;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ComponentContext;

@Component
public class EcsObjectStoreProviderImpl implements ObjectStoreProvider {

    @Inject
    ObjectStoreProviderManager storeMgr;

    @Inject
    ObjectStoreHelper helper;

    private final String providerName = "ECS";

    protected ObjectStoreLifeCycle lifeCycle;
    protected ObjectStoreDriver   driver;

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        // Follow Ceph provider pattern
        lifeCycle = ComponentContext.inject(EcsObjectStoreLifeCycleImpl.class);
        driver    = ComponentContext.inject(EcsObjectStoreDriverImpl.class);
        storeMgr.registerDriver(getName(), driver);
        return true;
    }

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return lifeCycle;
    }

    @Override
    public DataStoreDriver getDataStoreDriver() {
        return driver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return null;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types = new HashSet<>();
        types.add(DataStoreProviderType.OBJECT);
        return types;
    }
}
