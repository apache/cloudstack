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

package org.apache.cloudstack.storage.sharedfs.provider;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.sharedfs.SharedFS;
import org.apache.cloudstack.storage.sharedfs.SharedFSProvider;
import org.apache.cloudstack.storage.sharedfs.lifecycle.StorageVmSharedFSLifeCycle;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentContext;

public class StorageVmSharedFSProvider extends AdapterBase implements SharedFSProvider, Configurable {
    protected String name = String.valueOf(SharedFSProviderType.SHAREDFSVM);

    public static final ConfigKey<Integer> SHAREDFSVM_MIN_RAM_SIZE = new ConfigKey<Integer>("Advanced",
            Integer.class,
            "sharedfsvm.min.ram.size",
            "1024",
            "minimum ram size allowed for the compute offering to be used to create the sharedfs vm.",
            true,
            ConfigKey.Scope.Zone,
            SharedFS.SharedFSFeatureEnabled.key());

    public static final ConfigKey<Integer> SHAREDFSVM_MIN_CPU_COUNT = new ConfigKey<Integer>("Advanced",
            Integer.class,
            "sharedfsvm.min.cpu.count",
            "2",
            "minimum cpu count allowed for the compute offering to be used to create the sharedfs vm.",
            true,
            ConfigKey.Scope.Zone,
            SharedFS.SharedFSFeatureEnabled.key());

    protected StorageVmSharedFSLifeCycle lifecycle;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void configure() {
        lifecycle = ComponentContext.inject(StorageVmSharedFSLifeCycle.class);
    }

    @Override
    public StorageVmSharedFSLifeCycle getSharedFSLifeCycle() {
        return lifecycle;
    }

    @Override
    public String getConfigComponentName() {
        return StorageVmSharedFSProvider.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {
                SHAREDFSVM_MIN_CPU_COUNT,
                SHAREDFSVM_MIN_RAM_SIZE
        };
    }
}
