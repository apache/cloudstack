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

import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageStoreProvider;
import org.apache.cloudstack.storage.datastore.driver.CloudStackImageStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.lifecycle.CloudStackImageStoreLifeCycleImpl;
import org.apache.cloudstack.storage.image.ImageStoreDriver;
import org.apache.cloudstack.storage.image.datastore.ImageStoreHelper;
import org.apache.cloudstack.storage.image.datastore.ImageStoreProviderManager;
import org.apache.cloudstack.storage.image.store.lifecycle.ImageStoreLifeCycle;

import com.cloud.storage.ScopeType;
import com.cloud.utils.component.ComponentContext;

@Component
public class CloudStackImageStoreProviderImpl implements ImageStoreProvider {

    private final String providerName = DataStoreProvider.NFS_IMAGE;
    protected ImageStoreLifeCycle lifeCycle;
    protected ImageStoreDriver driver;
    @Inject
    ImageStoreProviderManager storeMgr;
    @Inject
    ImageStoreHelper helper;

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return lifeCycle;
    }

    @Override
    public String getName() {
        return this.providerName;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        lifeCycle = ComponentContext.inject(CloudStackImageStoreLifeCycleImpl.class);
        driver = ComponentContext.inject(CloudStackImageStoreDriverImpl.class);

        storeMgr.registerDriver(this.getName(), driver);

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
        Set<DataStoreProviderType> types = new HashSet<DataStoreProviderType>();
        types.add(DataStoreProviderType.IMAGE);
        types.add(DataStoreProviderType.ImageCache);
        return types;
    }

    @Override
    public boolean isScopeSupported(ScopeType scope) {
        if (scope == ScopeType.ZONE)
            return true;
        return false;
    }

    @Override
    public boolean needDownloadSysTemplate() {
        return false;
    }

}
