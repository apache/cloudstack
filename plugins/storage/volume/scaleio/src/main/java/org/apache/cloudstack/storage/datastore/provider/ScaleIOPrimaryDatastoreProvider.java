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

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.driver.ScaleIOPrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.ScaleIOPrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.log4j.Logger;

import com.cloud.utils.component.ComponentContext;

public class ScaleIOPrimaryDatastoreProvider implements PrimaryDataStoreProvider {
    private static final Logger LOGGER = Logger.getLogger(ScaleIOPrimaryDatastoreProvider.class);

    private DataStoreLifeCycle lifeCycle;
    private PrimaryDataStoreDriver driver;
    private HypervisorHostListener listener;

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
        return listener;
    }

    @Override
    public String getName() {
        return ScaleIOUtil.PROVIDER_NAME;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        lifeCycle = ComponentContext.inject(ScaleIOPrimaryDataStoreLifeCycle.class);
        driver = ComponentContext.inject(ScaleIOPrimaryDataStoreDriver.class);
        listener = ComponentContext.inject(ScaleIOHostListener.class);

        return true;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types = new HashSet<DataStoreProviderType>();
        types.add(DataStoreProviderType.PRIMARY);
        return types;
    }
}
