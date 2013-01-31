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

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.driver.DefaultPrimaryDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultPrimaryDataStoreLifeCycleImpl;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ComponentContext;

@Component
public class DefaultPrimaryDatastoreProviderImpl implements PrimaryDataStoreProvider {
    private final String providerName = "default primary data store provider";
    protected PrimaryDataStoreDriver driver;
    @Inject
    PrimaryDataStoreProviderManager storeMgr;

    protected DataStoreLifeCycle lifecyle;
    protected String uuid;
    protected long id;
    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public DataStoreLifeCycle getLifeCycle() {
        return this.lifecyle;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        lifecyle = ComponentContext.inject(DefaultPrimaryDataStoreLifeCycleImpl.class);
        driver = ComponentContext.inject(DefaultPrimaryDataStoreDriverImpl.class);
        HypervisorHostListener listener = ComponentContext.inject(DefaultHostListener.class);
        uuid = (String)params.get("uuid");
        id = (Long)params.get("id");
        storeMgr.registerDriver(uuid, this.driver);
        storeMgr.registerHostListener(uuid, listener);
        return true;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public long getId() {
        return this.id;
    }

}
