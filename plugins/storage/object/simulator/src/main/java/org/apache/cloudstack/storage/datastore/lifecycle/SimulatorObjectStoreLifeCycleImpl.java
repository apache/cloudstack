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
package org.apache.cloudstack.storage.datastore.lifecycle;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.Discoverer;
import com.cloud.resource.ResourceManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreHelper;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.apache.cloudstack.storage.object.store.lifecycle.ObjectStoreLifeCycle;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulatorObjectStoreLifeCycleImpl implements ObjectStoreLifeCycle {
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    protected ObjectStoreDao objectStoreDao;
    @Inject
    ObjectStoreHelper objectStoreHelper;
    @Inject
    ObjectStoreProviderManager objectStoreMgr;

    protected List<? extends Discoverer> _discoverers;

    public List<? extends Discoverer> getDiscoverers() {
        return _discoverers;
    }

    public void setDiscoverers(List<? extends Discoverer> discoverers) {
        this._discoverers = discoverers;
    }

    public SimulatorObjectStoreLifeCycleImpl() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {

        String url = (String)dsInfos.get("url");
        String name = (String)dsInfos.get("name");
        String providerName = (String)dsInfos.get("providerName");
        Map<String, String> details = (Map<String, String>)dsInfos.get("details");

        Map<String, Object> objectStoreParameters = new HashMap();
        objectStoreParameters.put("name", name);
        objectStoreParameters.put("url", url);
        objectStoreParameters.put("providerName", providerName);

        ObjectStoreVO ids = objectStoreHelper.createObjectStore(objectStoreParameters, details);
        return objectStoreMgr.getObjectStore(ids.getId());
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        return false;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return false;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        return false;
    }

    @Override
    public boolean maintain(DataStore store) {
        return false;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        return false;
    }

    @Override
    public boolean deleteDataStore(DataStore store) {
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle#migrateToObjectStore(org.apache.cloudstack.engine.subsystem.api.storage.DataStore)
     */
    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

}
