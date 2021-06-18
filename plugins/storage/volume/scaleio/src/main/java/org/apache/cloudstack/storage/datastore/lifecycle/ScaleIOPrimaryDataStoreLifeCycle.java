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
package org.apache.cloudstack.storage.datastore.lifecycle;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.api.StoragePoolStatistics;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.template.TemplateManager;
import com.cloud.utils.UriUtils;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;

public class ScaleIOPrimaryDataStoreLifeCycle implements PrimaryDataStoreLifeCycle {
    private static final Logger LOGGER = Logger.getLogger(ScaleIOPrimaryDataStoreLifeCycle.class);

    @Inject
    private ClusterDao clusterDao;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    private StoragePoolHostDao storagePoolHostDao;
    @Inject
    private PrimaryDataStoreHelper dataStoreHelper;
    @Inject
    private ResourceManager resourceManager;
    @Inject
    private StorageManager storageMgr;
    @Inject
    private StoragePoolAutomation storagePoolAutomation;
    @Inject
    private CapacityManager capacityMgr;
    @Inject
    private TemplateManager templateMgr;
    @Inject
    private AgentManager agentMgr;

    public ScaleIOPrimaryDataStoreLifeCycle() {
    }

    private org.apache.cloudstack.storage.datastore.api.StoragePool findStoragePool(String url, String username, String password, String storagePoolName) {
        try {
            final int clientTimeout = StorageManager.STORAGE_POOL_CLIENT_TIMEOUT.value();
            final int clientMaxConnections = StorageManager.STORAGE_POOL_CLIENT_MAX_CONNECTIONS.value();
            ScaleIOGatewayClient client = ScaleIOGatewayClient.getClient(url, username, password, false, clientTimeout, clientMaxConnections);
            List<org.apache.cloudstack.storage.datastore.api.StoragePool> storagePools = client.listStoragePools();
            for (org.apache.cloudstack.storage.datastore.api.StoragePool pool : storagePools) {
                if (pool.getName().equals(storagePoolName)) {
                    LOGGER.info("Found PowerFlex storage pool: " + storagePoolName);
                    final org.apache.cloudstack.storage.datastore.api.StoragePoolStatistics poolStatistics = client.getStoragePoolStatistics(pool.getId());
                    pool.setStatistics(poolStatistics);

                    String systemId = client.getSystemId(pool.getProtectionDomainId());
                    pool.setSystemId(systemId);
                    return pool;
                }
            }
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            LOGGER.error("Failed to add storage pool", e);
            throw new CloudRuntimeException("Failed to establish connection with PowerFlex Gateway to find and validate storage pool: " + storagePoolName);
        }
        throw new CloudRuntimeException("Failed to find the provided storage pool name: " + storagePoolName + " in the discovered PowerFlex storage pools");
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        String url = (String) dsInfos.get("url");
        Long zoneId = (Long) dsInfos.get("zoneId");
        Long podId = (Long)dsInfos.get("podId");
        Long clusterId = (Long)dsInfos.get("clusterId");
        String dataStoreName = (String) dsInfos.get("name");
        String providerName = (String) dsInfos.get("providerName");
        Long capacityBytes = (Long)dsInfos.get("capacityBytes");
        Long capacityIops = (Long)dsInfos.get("capacityIops");
        String tags = (String)dsInfos.get("tags");
        Map<String, String> details = (Map<String, String>) dsInfos.get("details");

        if (zoneId == null) {
            throw new CloudRuntimeException("Zone Id must be specified.");
        }

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();
        if (clusterId != null) {
            // Primary datastore is cluster-wide, check and set the podId and clusterId parameters
            if (podId == null) {
                throw new CloudRuntimeException("Pod Id must also be specified when the Cluster Id is specified for Cluster-wide primary storage.");
            }

            Hypervisor.HypervisorType hypervisorType = getHypervisorTypeForCluster(clusterId);
            if (!isSupportedHypervisorType(hypervisorType)) {
                throw new CloudRuntimeException("Unsupported hypervisor type: " + hypervisorType.toString());
            }

            parameters.setPodId(podId);
            parameters.setClusterId(clusterId);
        } else if (podId != null) {
            throw new CloudRuntimeException("Cluster Id must also be specified when the Pod Id is specified for Cluster-wide primary storage.");
        }

        URI uri = null;
        try {
            uri = new URI(UriUtils.encodeURIComponent(url));
            if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("powerflex")) {
                throw new InvalidParameterValueException("scheme is invalid for url: " + url + ", should be powerflex://username:password@gatewayhost/pool");
            }
        } catch (Exception ignored) {
            throw new InvalidParameterValueException(url + " is not a valid uri");
        }

        String storagePoolName = null;
        try {
            storagePoolName = URLDecoder.decode(uri.getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("[ignored] we are on a platform not supporting \"UTF-8\"!?!", e);
        }
        if (storagePoolName == null) { // if decoding fails, use getPath() anyway
            storagePoolName = uri.getPath();
        }
        storagePoolName = storagePoolName.replaceFirst("/", "");

        final String storageHost = uri.getHost();
        final int port = uri.getPort();
        String gatewayApiURL = null;
        if (port == -1) {
            gatewayApiURL = String.format("https://%s/api", storageHost);
        } else {
            gatewayApiURL = String.format("https://%s:%d/api", storageHost, port);
        }

        final String userInfo = uri.getUserInfo();
        final String gatewayUsername = userInfo.split(":")[0];
        final String gatewayPassword = userInfo.split(":")[1];

        List<StoragePoolVO> storagePoolVO = primaryDataStoreDao.findPoolsByProvider(ScaleIOUtil.PROVIDER_NAME);
        if (CollectionUtils.isNotEmpty(storagePoolVO)) {
            for (StoragePoolVO poolVO : storagePoolVO) {
                Map <String, String> poolDetails = primaryDataStoreDao.getDetails(poolVO.getId());
                String poolUrl = poolDetails.get(ScaleIOGatewayClient.GATEWAY_API_ENDPOINT);
                String poolName = poolDetails.get(ScaleIOGatewayClient.STORAGE_POOL_NAME);

                if (gatewayApiURL.equals(poolUrl) && storagePoolName.equals(poolName)) {
                    throw new IllegalArgumentException("PowerFlex storage pool: " + storagePoolName + " already exists, please specify other storage pool.");
                }
            }
        }

        final org.apache.cloudstack.storage.datastore.api.StoragePool scaleIOPool = this.findStoragePool(gatewayApiURL,
                gatewayUsername, gatewayPassword, storagePoolName);

        parameters.setZoneId(zoneId);
        parameters.setName(dataStoreName);
        parameters.setProviderName(providerName);
        parameters.setManaged(true);
        parameters.setHost(storageHost);
        parameters.setPath(scaleIOPool.getId());
        parameters.setUserInfo(userInfo);
        parameters.setType(Storage.StoragePoolType.PowerFlex);
        parameters.setHypervisorType(Hypervisor.HypervisorType.KVM);
        parameters.setUuid(UUID.randomUUID().toString());
        parameters.setTags(tags);

        StoragePoolStatistics poolStatistics = scaleIOPool.getStatistics();
        if (poolStatistics != null) {
            if (capacityBytes == null) {
                parameters.setCapacityBytes(poolStatistics.getNetMaxCapacityInBytes());
            }
            parameters.setUsedBytes(poolStatistics.getNetUsedCapacityInBytes());
        }

        if (capacityBytes != null) {
            parameters.setCapacityBytes(capacityBytes);
        }

        if (capacityIops != null) {
            parameters.setCapacityIops(capacityIops);
        }

        details.put(ScaleIOGatewayClient.GATEWAY_API_ENDPOINT, gatewayApiURL);
        details.put(ScaleIOGatewayClient.GATEWAY_API_USERNAME, DBEncryptionUtil.encrypt(gatewayUsername));
        details.put(ScaleIOGatewayClient.GATEWAY_API_PASSWORD, DBEncryptionUtil.encrypt(gatewayPassword));
        details.put(ScaleIOGatewayClient.STORAGE_POOL_NAME, storagePoolName);
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, scaleIOPool.getSystemId());
        parameters.setDetails(details);

        return dataStoreHelper.createPrimaryDataStore(parameters);
    }

    @Override
    public boolean attachCluster(DataStore dataStore, ClusterScope scope) {
        final ClusterVO cluster = clusterDao.findById(scope.getScopeId());
        if (!isSupportedHypervisorType(cluster.getHypervisorType())) {
            throw new CloudRuntimeException("Unsupported hypervisor type: " + cluster.getHypervisorType().toString());
        }

        List<String> connectedSdcIps = null;
        try {
            ScaleIOGatewayClient client = ScaleIOGatewayClientConnectionPool.getInstance().getClient(dataStore.getId(), storagePoolDetailsDao);
            connectedSdcIps = client.listConnectedSdcIps();
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            LOGGER.error("Failed to create storage pool", e);
            throw new CloudRuntimeException("Failed to establish connection with PowerFlex Gateway to create storage pool");
        }

        if (connectedSdcIps == null || connectedSdcIps.isEmpty()) {
            LOGGER.debug("No connected SDCs found for the PowerFlex storage pool");
            throw new CloudRuntimeException("Failed to create storage pool as connected SDCs not found");
        }

        PrimaryDataStoreInfo primaryDataStoreInfo = (PrimaryDataStoreInfo) dataStore;

        List<HostVO> hostsInCluster = resourceManager.listAllUpAndEnabledHosts(Host.Type.Routing, primaryDataStoreInfo.getClusterId(),
                primaryDataStoreInfo.getPodId(), primaryDataStoreInfo.getDataCenterId());
        if (hostsInCluster.isEmpty()) {
            primaryDataStoreDao.expunge(primaryDataStoreInfo.getId());
            throw new CloudRuntimeException("No hosts are Up to associate a storage pool with in cluster: " + primaryDataStoreInfo.getClusterId());
        }

        LOGGER.debug("Attaching the pool to each of the hosts in the cluster: " + primaryDataStoreInfo.getClusterId());
        List<HostVO> poolHosts = new ArrayList<HostVO>();
        for (HostVO host : hostsInCluster) {
            try {
                if (connectedSdcIps.contains(host.getPrivateIpAddress())) {
                    storageMgr.connectHostToSharedPool(host.getId(), primaryDataStoreInfo.getId());
                    poolHosts.add(host);
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to establish a connection between " + host + " and " + primaryDataStoreInfo, e);
            }
        }

        if (poolHosts.isEmpty()) {
            LOGGER.warn("No host can access storage pool '" + primaryDataStoreInfo + "' on cluster '" + primaryDataStoreInfo.getClusterId() + "'.");
            primaryDataStoreDao.expunge(primaryDataStoreInfo.getId());
            throw new CloudRuntimeException("Failed to create storage pool in the cluster: " + primaryDataStoreInfo.getClusterId() + " as it is not accessible to hosts");
        }

        dataStoreHelper.attachCluster(dataStore);
        return true;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return true;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, Hypervisor.HypervisorType hypervisorType) {
        if (!isSupportedHypervisorType(hypervisorType)) {
            throw new CloudRuntimeException("Unsupported hypervisor type: " + hypervisorType.toString());
        }

        List<String> connectedSdcIps = null;
        try {
            ScaleIOGatewayClient client = ScaleIOGatewayClientConnectionPool.getInstance().getClient(dataStore.getId(), storagePoolDetailsDao);
            connectedSdcIps = client.listConnectedSdcIps();
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            LOGGER.error("Failed to create storage pool", e);
            throw new CloudRuntimeException("Failed to establish connection with PowerFlex Gateway to create storage pool");
        }

        if (connectedSdcIps == null || connectedSdcIps.isEmpty()) {
            LOGGER.debug("No connected SDCs found for the PowerFlex storage pool");
            throw new CloudRuntimeException("Failed to create storage pool as connected SDCs not found");
        }

        LOGGER.debug("Attaching the pool to each of the hosts in the zone: " + scope.getScopeId());
        List<HostVO> hosts = resourceManager.listAllUpAndEnabledHostsInOneZoneByHypervisor(hypervisorType, scope.getScopeId());
        List<HostVO> poolHosts = new ArrayList<HostVO>();
        for (HostVO host : hosts) {
            try {
                if (connectedSdcIps.contains(host.getPrivateIpAddress())) {
                    storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
                    poolHosts.add(host);
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }
        if (poolHosts.isEmpty()) {
            LOGGER.warn("No host can access storage pool " + dataStore + " in this zone.");
            primaryDataStoreDao.expunge(dataStore.getId());
            throw new CloudRuntimeException("Failed to create storage pool as it is not accessible to hosts.");
        }

        dataStoreHelper.attachZone(dataStore);
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
    public boolean deleteDataStore(DataStore dataStore) {
        StoragePool storagePool = (StoragePool)dataStore;
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(storagePool.getId());
        if (storagePoolVO == null) {
            return false;
        }

        List<VMTemplateStoragePoolVO> unusedTemplatesInPool = templateMgr.getUnusedTemplatesInPool(storagePoolVO);
        for (VMTemplateStoragePoolVO templatePoolVO : unusedTemplatesInPool) {
            if (templatePoolVO.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                templateMgr.evictTemplateFromStoragePool(templatePoolVO);
            }
        }

        List<StoragePoolHostVO> poolHostVOs = storagePoolHostDao.listByPoolId(dataStore.getId());
        for (StoragePoolHostVO poolHostVO : poolHostVOs) {
            DeleteStoragePoolCommand deleteStoragePoolCommand = new DeleteStoragePoolCommand(storagePool);
            final Answer answer = agentMgr.easySend(poolHostVO.getHostId(), deleteStoragePoolCommand);
            if (answer != null && answer.getResult()) {
                LOGGER.info("Successfully deleted storage pool: " + storagePool.getId() + " from host: " + poolHostVO.getHostId());
            } else {
                if (answer != null) {
                    LOGGER.error("Failed to delete storage pool: " + storagePool.getId() + " from host: " + poolHostVO.getHostId() + " , result: " + answer.getResult());
                } else {
                    LOGGER.error("Failed to delete storage pool: " + storagePool.getId() + " from host: " + poolHostVO.getHostId());
                }
            }
        }

        ScaleIOGatewayClientConnectionPool.getInstance().removeClient(dataStore.getId());

        return dataStoreHelper.deletePrimaryDataStore(dataStore);
    }

    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> details) {
        String capacityBytes = details.get(PrimaryDataStoreLifeCycle.CAPACITY_BYTES);
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(storagePool.getId());

        try {
            if (capacityBytes == null || capacityBytes.isBlank()) {
                return;
            }

            long usedBytes = capacityMgr.getUsedBytes(storagePoolVO);
            if (Long.parseLong(capacityBytes) < usedBytes) {
                throw new CloudRuntimeException("Cannot reduce the number of bytes for this storage pool as it would lead to an insufficient number of bytes");
            }

            primaryDataStoreDao.updateCapacityBytes(storagePool.getId(), Long.parseLong(capacityBytes));
            LOGGER.info("Storage pool successfully updated");
        } catch (Throwable e) {
            throw new CloudRuntimeException("Failed to update the storage pool" + e);
        }
    }

    private Hypervisor.HypervisorType getHypervisorTypeForCluster(long clusterId) {
        ClusterVO cluster = clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new CloudRuntimeException("Unable to locate the specified cluster: " + clusterId);
        }

        return cluster.getHypervisorType();
    }

    private static boolean isSupportedHypervisorType(Hypervisor.HypervisorType hypervisorType) {
        return Hypervisor.HypervisorType.KVM.equals(hypervisorType);
    }
}
