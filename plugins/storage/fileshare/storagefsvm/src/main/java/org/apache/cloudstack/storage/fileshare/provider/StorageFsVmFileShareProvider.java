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

package org.apache.cloudstack.storage.fileshare.provider;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareLifeCycle;
import org.apache.cloudstack.storage.fileshare.FileShareProvider;
import org.apache.cloudstack.storage.fileshare.lifecycle.StorageFsVmFileShareLifeCycle;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentContext;

public class StorageFsVmFileShareProvider extends AdapterBase implements FileShareProvider, Configurable {
    protected String name = String.valueOf(FileShareProviderType.STORAGEFSVM);

    public static final ConfigKey<Integer> STORAGEFSVM_MIN_RAM_SIZE = new ConfigKey<Integer>("Advanced",
            Integer.class,
            "storagefsvm.min.ram.size",
            "1024",
            "minimum ram size allowed for the compute offering to be used to create storagefsvm for file shares.",
            true,
            ConfigKey.Scope.Zone,
            FileShare.FileShareFeatureEnabled.key());

    public static final ConfigKey<Integer> STORAGEFSVM_MIN_CPU_COUNT = new ConfigKey<Integer>("Advanced",
            Integer.class,
            "storagefsvm.min.cpu.count",
            "2",
            "minimum cpu count allowed for the compute offering to be used to create storagefsvm for file shares.",
            true,
            ConfigKey.Scope.Zone,
            FileShare.FileShareFeatureEnabled.key());

    protected FileShareLifeCycle lifecycle;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void configure() {
        lifecycle = ComponentContext.inject(StorageFsVmFileShareLifeCycle.class);
    }

    @Override
    public FileShareLifeCycle getFileShareLifeCycle() {
        return lifecycle;
    }

    @Override
    public String getConfigComponentName() {
        return StorageFsVmFileShareProvider.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {
                STORAGEFSVM_MIN_CPU_COUNT,
                STORAGEFSVM_MIN_RAM_SIZE
        };
    }
}
