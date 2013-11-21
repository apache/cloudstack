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
package org.apache.cloudstack.storage.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.datastore.provider.CloudStackPrimaryDataStoreProviderImpl;
import org.apache.cloudstack.storage.endpoint.DefaultEndPointSelector;

import com.cloud.storage.snapshot.SnapshotScheduler;
import com.cloud.user.DomainManager;
import com.cloud.utils.component.ComponentContext;

public class FakeDriverTestConfiguration extends ChildTestConfiguration {
    @Bean
    public CloudStackPrimaryDataStoreProviderImpl dataStoreProvider() {
        CloudStackPrimaryDataStoreProviderImpl provider = Mockito.mock(CloudStackPrimaryDataStoreProviderImpl.class);

        return provider;
    }

    @Bean
    public DataMotionStrategy dataMotionStrategy() {
        DataMotionStrategy strategy = new MockStorageMotionStrategy();
        return strategy;
    }

    @Bean
    public SnapshotScheduler SnapshotScheduler() {
        return Mockito.mock(SnapshotScheduler.class);
    }

    @Bean
    public DomainManager DomainManager() {
        return Mockito.mock(DomainManager.class);
    }

    @Override
    @Bean
    public EndPointSelector selector() {
        return ComponentContext.inject(DefaultEndPointSelector.class);
    }
}
