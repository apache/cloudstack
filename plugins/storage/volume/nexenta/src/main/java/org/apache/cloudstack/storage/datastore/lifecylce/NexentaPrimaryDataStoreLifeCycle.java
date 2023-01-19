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
package org.apache.cloudstack.storage.datastore.lifecylce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.util.NexentaUtil;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;

public class NexentaPrimaryDataStoreLifeCycle
        implements PrimaryDataStoreLifeCycle {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private DataCenterDao zoneDao;
    @Inject
    private PrimaryDataStoreHelper dataStoreHelper;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    StorageManager _storageMgr;
    @Inject
    private StoragePoolAutomation storagePoolAutomation;

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        String url = (String) dsInfos.get("url");
        Long zoneId = (Long) dsInfos.get("zoneId");
        String storagePoolName = (String) dsInfos.get("name");
        String providerName = (String) dsInfos.get("providerName");
        Long capacityBytes = (Long)dsInfos.get("capacityBytes");
        Long capacityIops = (Long)dsInfos.get("capacityIops");
        String tags = (String)dsInfos.get("tags");
        Map<String, String> details = (Map<String, String>) dsInfos.get("details");
        NexentaUtil.NexentaPluginParameters params = NexentaUtil.parseNexentaPluginUrl(url);
        DataCenterVO zone = zoneDao.findById(zoneId);
        String uuid = String.format("%s_%s_%s", NexentaUtil.PROVIDER_NAME, zone.getUuid(), params.getNmsUrl().getHost());

        if (capacityBytes == null || capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops == null || capacityIops <= 0) {
            throw new IllegalArgumentException("'capacityIops' must be present and greater than 0.");
        }

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        parameters.setHost(params.getStorageHost());
        parameters.setPort(params.getStoragePort());
        parameters.setPath(params.getStoragePath());
        parameters.setType(params.getStorageType());
        parameters.setUuid(uuid);
        parameters.setZoneId(zoneId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(true);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);
        parameters.setCapacityIops(capacityIops);
        parameters.setHypervisorType(Hypervisor.HypervisorType.Any);
        parameters.setTags(tags);

        details.put(NexentaUtil.NMS_URL, params.getNmsUrl().toString());

        details.put(NexentaUtil.VOLUME, params.getVolume());
        details.put(NexentaUtil.SPARSE_VOLUMES, params.isSparseVolumes().toString());

        details.put(NexentaUtil.STORAGE_TYPE, params.getStorageType().toString());
        details.put(NexentaUtil.STORAGE_HOST, params.getStorageHost());
        details.put(NexentaUtil.STORAGE_PORT, params.getStoragePort().toString());
        details.put(NexentaUtil.STORAGE_PATH, params.getStoragePath());

        parameters.setDetails(details);

        // this adds a row in the cloud.storage_pool table for this SolidFire cluster
        return dataStoreHelper.createPrimaryDataStore(parameters);
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        return true;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return true;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, Hypervisor.HypervisorType hypervisorType) {
        dataStoreHelper.attachZone(dataStore);

        List<HostVO> xenServerHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(Hypervisor.HypervisorType.XenServer, scope.getScopeId());
        List<HostVO> vmWareServerHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(Hypervisor.HypervisorType.VMware, scope.getScopeId());
        List<HostVO> kvmHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(Hypervisor.HypervisorType.KVM, scope.getScopeId());
        List<HostVO> hosts = new ArrayList<HostVO>();

        hosts.addAll(xenServerHosts);
        hosts.addAll(vmWareServerHosts);
        hosts.addAll(kvmHosts);

        for (HostVO host : hosts) {
            try {
                _storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }

        return true;
    }

    @Override
    public boolean maintain(DataStore store) {
        storagePoolAutomation.maintain(store);
        dataStoreHelper.maintain(store);

        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        dataStoreHelper.cancelMaintain(store);
        storagePoolAutomation.cancelMaintain(store);

        return true;
    }

    @Override
    public void enableStoragePool(DataStore dataStore) {
        dataStoreHelper.enable(dataStore);
    }

    @Override
    public void disableStoragePool(DataStore dataStore) {
        dataStoreHelper.disable(dataStore);
    }

    @Override
    public boolean deleteDataStore(DataStore store) {
        return dataStoreHelper.deletePrimaryDataStore(store);
    }

    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> details) {
    }
}
