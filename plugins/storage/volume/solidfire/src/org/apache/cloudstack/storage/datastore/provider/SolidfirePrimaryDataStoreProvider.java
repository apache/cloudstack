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

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.driver.SolidfirePrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.SolidFirePrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ComponentContext;

@Component
public class SolidfirePrimaryDataStoreProvider implements PrimaryDataStoreProvider {
    private DataStoreLifeCycle lifecycle;
    private PrimaryDataStoreDriver driver;
    private HypervisorHostListener listener;

    SolidfirePrimaryDataStoreProvider() {
    }

    @Override
    public String getName() {
        return SolidFireUtil.PROVIDER_NAME;
    }

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return lifecycle;
    }

    @Override
    public PrimaryDataStoreDriver getDataStoreDriver() {
        return driver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return listener;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        lifecycle = ComponentContext.inject(SolidFirePrimaryDataStoreLifeCycle.class);
        driver = ComponentContext.inject(SolidfirePrimaryDataStoreDriver.class);
        listener = ComponentContext.inject(SolidFireHostListener.class);

        return true;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types =  new HashSet<DataStoreProviderType>();

        types.add(DataStoreProviderType.PRIMARY);

        return types;
    }
}
