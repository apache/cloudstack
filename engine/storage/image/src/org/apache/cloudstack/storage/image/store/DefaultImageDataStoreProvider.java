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

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.provider.ImageDataStoreProvider;
import org.apache.cloudstack.storage.image.ImageDataStoreDriver;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreManager;
import org.apache.cloudstack.storage.image.driver.DefaultImageDataStoreDriverImpl;
import org.apache.cloudstack.storage.image.store.lifecycle.DefaultImageDataStoreLifeCycle;
import org.apache.cloudstack.storage.image.store.lifecycle.ImageDataStoreLifeCycle;

import com.cloud.utils.component.ComponentContext;

public class DefaultImageDataStoreProvider implements ImageDataStoreProvider {
    private final String name = "default image data store";
    protected ImageDataStoreLifeCycle lifeCycle;
    protected ImageDataStoreDriver driver;
    @Inject
    ImageDataStoreManager storeMgr;
    long id;
    String uuid;
    @Override
    public DataStoreLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        lifeCycle = ComponentContext.inject(DefaultImageDataStoreLifeCycle.class);
        driver = ComponentContext.inject(DefaultImageDataStoreDriverImpl.class);
        uuid = (String)params.get("uuid");
        id = (Long)params.get("id");
        storeMgr.registerDriver(uuid, driver);
        return true;
    }

}
