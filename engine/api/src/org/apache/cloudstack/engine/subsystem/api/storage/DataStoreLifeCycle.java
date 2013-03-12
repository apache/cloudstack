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


public interface DataStoreLifeCycle {
    public DataStore initialize(Map<String, Object> dsInfos);

    public boolean attachCluster(DataStore store, ClusterScope scope);
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo);
    boolean attachZone(DataStore dataStore, ZoneScope scope);
    
    public boolean dettach();

    public boolean unmanaged();

    public boolean maintain(long storeId);

    public boolean cancelMaintain(long storeId);

    public boolean deleteDataStore(long storeId);
}
