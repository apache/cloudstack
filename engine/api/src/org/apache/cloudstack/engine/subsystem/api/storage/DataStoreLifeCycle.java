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
package org.apache.cloudstack.engine.subsystem.api.storage;

import java.util.Map;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

public interface DataStoreLifeCycle {
    DataStore initialize(Map<String, Object> dsInfos);

    boolean attachCluster(DataStore store, ClusterScope scope);

    boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo);

    boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType);

    boolean maintain(DataStore store);

    boolean cancelMaintain(DataStore store);

    boolean deleteDataStore(DataStore store);

    boolean migrateToObjectStore(DataStore store);
}
