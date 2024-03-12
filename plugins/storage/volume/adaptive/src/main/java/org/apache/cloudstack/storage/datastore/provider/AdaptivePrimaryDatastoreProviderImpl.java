// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.datastore.provider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ComponentContext;

import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterFactory;
import org.apache.cloudstack.storage.datastore.driver.AdaptiveDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.lifecycle.AdaptiveDataStoreLifeCycleImpl;

@Component
public abstract class AdaptivePrimaryDatastoreProviderImpl implements PrimaryDataStoreProvider {
    protected Logger logger = LogManager.getLogger(getClass());

    AdaptiveDataStoreDriverImpl driver;

    HypervisorHostListener listener;

    AdaptivePrimaryDatastoreAdapterFactoryMap factoryMap = new AdaptivePrimaryDatastoreAdapterFactoryMap();

    DataStoreLifeCycle lifecycle;

    AdaptivePrimaryDatastoreProviderImpl(ProviderAdapterFactory f) {
        logger.info("Creating " + f.getProviderName());
        factoryMap.register(f);
    }

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return this.lifecycle;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        logger.info("Configuring " + getName());
        driver = new AdaptiveDataStoreDriverImpl(factoryMap);
        driver.setProviderName(getName());
        lifecycle = ComponentContext.inject(new AdaptiveDataStoreLifeCycleImpl(factoryMap));
        driver = ComponentContext.inject(driver);
        listener = ComponentContext.inject(new AdaptivePrimaryHostListener(factoryMap));
        return true;
    }

    @Override
    public PrimaryDataStoreDriver getDataStoreDriver() {
        return this.driver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return this.listener;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types = new HashSet<DataStoreProviderType>();
        types.add(DataStoreProviderType.PRIMARY);
        return types;
    }

}
