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

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LinstorPrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private ClusterDao clusterDao;
    @Inject
    PrimaryDataStoreDao _primaryDataStoreDao;
    @Inject
    private ResourceManager resourceMgr;
    @Inject
    private StorageManager _storageMgr;
    @Inject
    PrimaryDataStoreHelper dataStoreHelper;
    @Inject
    private StoragePoolAutomation storagePoolAutomation;
    @Inject
    private CapacityManager _capacityMgr;
    @Inject
    AgentManager _agentMgr;

    public LinstorPrimaryDataStoreLifeCycleImpl()
    {
    }

    private static boolean isSupportedHypervisorType(HypervisorType hypervisorType) {
        return HypervisorType.KVM.equals(hypervisorType);
    }

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        String url = (String) dsInfos.get("url");
        Long zoneId = (Long) dsInfos.get("zoneId");
        Long podId = (Long) dsInfos.get("podId");
        Long clusterId = (Long) dsInfos.get("clusterId");
        String storagePoolName = (String) dsInfos.get("name");
        String providerName = (String) dsInfos.get("providerName");
        Long capacityIops = (Long) dsInfos.get("capacityIops");
        String tags = (String) dsInfos.get("tags");
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) dsInfos.get("details");

        final String resourceGroup = details.get(LinstorUtil.RSC_GROUP);

        final String uuid = UUID.randomUUID().toString();

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        // checks if primary datastore is clusterwide. If so, uses the clusterId to set
        // the uuid and then sets the podId and clusterId parameters
        if (clusterId != null) {
            if (podId == null) {
                throw new CloudRuntimeException("The Pod ID must be specified.");
            }
            if (zoneId == null) {
                throw new CloudRuntimeException("The Zone ID must be specified.");
            }
            ClusterVO cluster = clusterDao.findById(clusterId);
            logger.info("Linstor: Setting Linstor cluster-wide primary storage uuid to " + uuid);
            parameters.setPodId(podId);
            parameters.setClusterId(clusterId);

            HypervisorType hypervisorType = cluster.getHypervisorType();

            if (!isSupportedHypervisorType(hypervisorType)) {
                throw new CloudRuntimeException(hypervisorType + " is not a supported hypervisor type.");
            }
        }

        if (!url.contains("://")) {
            url = "http://" + url;
        }

        URL controllerURL;
        int port = 3370;
        try
        {
            controllerURL = new URL(url);
            if (!controllerURL.getProtocol().startsWith("http")) {
                throw new IllegalArgumentException("Linstor controller URL wrong protocol: " + url);
            }
            if (!controllerURL.getPath().isEmpty()) {
                throw new IllegalArgumentException("Linstor controller URL shouldn't have a path: " + url);
            }
            if (controllerURL.getPort() == -1) {
                port = controllerURL.getProtocol().equals("https") ? 3371 : 3370;
                url += ":" + port;
            }
        } catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Linstor controller URL is not valid: " + e);
        }

        long capacityBytes = LinstorUtil.getCapacityBytes(url, resourceGroup);
        if (capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops != null) {
            parameters.setCapacityIops(capacityIops);
        }

        parameters.setHost(url);
        parameters.setPort(port);
        parameters.setPath(resourceGroup);
        parameters.setType(Storage.StoragePoolType.Linstor);
        parameters.setUuid(uuid);
        parameters.setZoneId(zoneId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(false);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);
        parameters.setCapacityIops(capacityIops);
        parameters.setHypervisorType(HypervisorType.KVM);
        parameters.setTags(tags);
        parameters.setDetails(details);
        parameters.setUserInfo(resourceGroup);

        return dataStoreHelper.createPrimaryDataStore(parameters);
    }

    protected boolean createStoragePool(long hostId, StoragePool pool) {
        logger.debug("creating pool " + pool.getName() + " on  host " + hostId);

        if (pool.getPoolType() != Storage.StoragePoolType.Linstor) {
            logger.warn(" Doesn't support storage pool type " + pool.getPoolType());
            return false;
        }
        CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, pool);
        final Answer answer = _agentMgr.easySend(hostId, cmd);
        if (answer != null && answer.getResult()) {
            return true;
        } else {
            _primaryDataStoreDao.expunge(pool.getId());
            String msg = answer != null ?
                "Can not create storage pool through host " + hostId + " due to " + answer.getDetails() :
                "Can not create storage pool through host " + hostId + " due to CreateStoragePoolCommand returns null";
            logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    @Override
    public boolean attachCluster(DataStore dataStore, ClusterScope scope) {
        final PrimaryDataStoreInfo primaryDataStoreInfo = (PrimaryDataStoreInfo) dataStore;

        final ClusterVO cluster = clusterDao.findById(primaryDataStoreInfo.getClusterId());
        final HypervisorType hypervisorType = cluster.getHypervisorType();
        if (!isSupportedHypervisorType(hypervisorType)) {
            throw new CloudRuntimeException(hypervisorType + " is not a supported hypervisor type.");
        }

        // check if there is at least one host up in this cluster
        List<HostVO> allHosts = resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing,
            primaryDataStoreInfo.getClusterId(), primaryDataStoreInfo.getPodId(),
            primaryDataStoreInfo.getDataCenterId());

        if (allHosts.isEmpty()) {
            _primaryDataStoreDao.expunge(primaryDataStoreInfo.getId());

            throw new CloudRuntimeException(
                "No host up to associate a storage pool with in cluster " + primaryDataStoreInfo.getClusterId());
        }

        List<HostVO> poolHosts = new ArrayList<>();
        for (HostVO host : allHosts) {
            try {
                createStoragePool(host.getId(), primaryDataStoreInfo);

                _storageMgr.connectHostToSharedPool(host.getId(), primaryDataStoreInfo.getId());

                poolHosts.add(host);
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + primaryDataStoreInfo, e);
            }
        }

        if (poolHosts.isEmpty()) {
            logger.warn("No host can access storage pool '" + primaryDataStoreInfo + "' on cluster '"
                + primaryDataStoreInfo.getClusterId() + "'.");

            _primaryDataStoreDao.expunge(primaryDataStoreInfo.getId());

            throw new CloudRuntimeException("Failed to access storage pool");
        }

        dataStoreHelper.attachCluster(dataStore);

        return true;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        if (!isSupportedHypervisorType(hypervisorType)) {
            throw new CloudRuntimeException(hypervisorType + " is not a supported hypervisor type.");
        }

        List<HostVO> hosts = resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(hypervisorType,
            scope.getScopeId());

        for (HostVO host : hosts) {
            try {
                _storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }

        dataStoreHelper.attachZone(dataStore, hypervisorType);
        return true;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return true;
    }

    @Override
    public boolean maintain(DataStore dataStore) {
        storagePoolAutomation.maintain(dataStore);
        dataStoreHelper.maintain(dataStore);
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        dataStoreHelper.cancelMaintain(store);
        storagePoolAutomation.cancelMaintain(store);

        return true;
    }

    @Override
    public boolean deleteDataStore(DataStore store) {
        return dataStoreHelper.deletePrimaryDataStore(store);
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle#migrateToObjectStore(org.apache.cloudstack.engine.subsystem.api.storage.DataStore)
     */
    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> details) {
        StoragePoolVO storagePoolVo = _primaryDataStoreDao.findById(storagePool.getId());

        String strCapacityBytes = details.get(PrimaryDataStoreLifeCycle.CAPACITY_BYTES);
        Long capacityBytes = strCapacityBytes != null ? Long.parseLong(strCapacityBytes) : null;

        if (capacityBytes != null) {
            long usedBytes = _capacityMgr.getUsedBytes(storagePoolVo);

            if (capacityBytes < usedBytes) {
                throw new CloudRuntimeException(
                    "Cannot reduce the number of bytes for this storage pool as it would lead to an insufficient number of bytes");
            }
        }

        String strCapacityIops = details.get(PrimaryDataStoreLifeCycle.CAPACITY_IOPS);
        Long capacityIops = strCapacityIops != null ? Long.parseLong(strCapacityIops) : null;

        if (capacityIops != null) {
            long usedIops = _capacityMgr.getUsedIops(storagePoolVo);

            if (capacityIops < usedIops) {
                throw new CloudRuntimeException(
                    "Cannot reduce the number of IOPS for this storage pool as it would lead to an insufficient number of IOPS");
            }
        }
    }

    @Override
    public void enableStoragePool(DataStore store) {
        dataStoreHelper.enable(store);
    }

    @Override
    public void disableStoragePool(DataStore store) {
        dataStoreHelper.disable(store);
    }
}
