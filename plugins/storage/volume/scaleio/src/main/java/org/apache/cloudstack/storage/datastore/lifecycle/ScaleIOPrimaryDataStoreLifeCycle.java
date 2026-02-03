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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.cloud.host.HostVO;
import org.apache.cloudstack.api.ApiConstants;
import com.cloud.utils.StringUtils;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.api.StoragePoolStatistics;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClientConnectionPool;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.template.TemplateManager;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.manager.ScaleIOSDCManager;
import org.apache.cloudstack.storage.datastore.manager.ScaleIOSDCManagerImpl;

import java.util.HashMap;

public class ScaleIOPrimaryDataStoreLifeCycle extends BasePrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    @Inject
    DataCenterDao dataCenterDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private HostDao hostDao;
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
    private ScaleIOSDCManager sdcManager;

    public ScaleIOPrimaryDataStoreLifeCycle() {
        sdcManager = new ScaleIOSDCManagerImpl();
    }

    private org.apache.cloudstack.storage.datastore.api.StoragePool findStoragePool(String url, String username, String password, String storagePoolName) {
        try {
            final int clientTimeout = StorageManager.STORAGE_POOL_CLIENT_TIMEOUT.value();
            final int clientMaxConnections = StorageManager.STORAGE_POOL_CLIENT_MAX_CONNECTIONS.value();
            ScaleIOGatewayClient client = ScaleIOGatewayClient.getClient(url, username, password, false, clientTimeout, clientMaxConnections);
            List<org.apache.cloudstack.storage.datastore.api.StoragePool> storagePools = client.listStoragePools();
            for (org.apache.cloudstack.storage.datastore.api.StoragePool pool : storagePools) {
                if (pool.getName().equals(storagePoolName)) {
                    logger.info("Found PowerFlex storage pool: {}", storagePoolName);
                    final org.apache.cloudstack.storage.datastore.api.StoragePoolStatistics poolStatistics = client.getStoragePoolStatistics(pool.getId());
                    pool.setStatistics(poolStatistics);

                    String systemId = client.getSystemId(pool.getProtectionDomainId());
                    pool.setSystemId(systemId);
                    List<String> mdmAddresses = client.getMdmAddresses();
                    pool.setMdmAddresses(mdmAddresses);
                    return pool;
                }
            }
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            logger.error("Failed to add storage pool", e);
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
        String storageAccessGroups = (String)dsInfos.get(ApiConstants.STORAGE_ACCESS_GROUPS);
        Boolean isTagARule = (Boolean) dsInfos.get("isTagARule");
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

        URI uri;
        try {
            uri = new URI(UriUtils.encodeURIComponent(url));
            if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("powerflex")) {
                throw new InvalidParameterValueException("scheme is invalid for url: " + url + ", should be powerflex://username:password@gatewayhost/pool");
            }
        } catch (Exception ignored) {
            throw new InvalidParameterValueException(url + " is not a valid uri");
        }

        String storagePoolName;
        storagePoolName = URLDecoder.decode(uri.getPath(), StringUtils.getPreferredCharset());
        if (storagePoolName == null) { // if decoding fails, use getPath() anyway
            storagePoolName = uri.getPath();
        }
        storagePoolName = storagePoolName.replaceFirst("/", "");

        final String storageHost = uri.getHost();
        final int port = uri.getPort();
        String gatewayApiURL;
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
        parameters.setStorageAccessGroups(storageAccessGroups);
        parameters.setIsTagARule(isTagARule);

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
        details.put(ScaleIOGatewayClient.STORAGE_POOL_MDMS, org.apache.commons.lang3.StringUtils.join(scaleIOPool.getMdmAddresses(), ","));
        parameters.setDetails(details);

        return dataStoreHelper.createPrimaryDataStore(parameters);
    }

    @Override
    public boolean attachCluster(DataStore dataStore, ClusterScope scope) {
        final ClusterVO cluster = clusterDao.findById(scope.getScopeId());
        if (!isSupportedHypervisorType(cluster.getHypervisorType())) {
            throw new CloudRuntimeException("Unsupported hypervisor type: " + cluster.getHypervisorType().toString());
        }

        PrimaryDataStoreInfo primaryDataStoreInfo = (PrimaryDataStoreInfo) dataStore;
        List<HostVO> hostsToConnect = resourceManager.getEligibleUpAndEnabledHostsInClusterForStorageConnection(primaryDataStoreInfo);
        logger.debug(String.format("Attaching the pool to each of the hosts %s in the cluster: %s", hostsToConnect, cluster));
        List<Long> hostIds = hostsToConnect.stream().map(HostVO::getId).collect(Collectors.toList());

        storageMgr.connectHostsToPool(dataStore, hostIds, scope, false, false);

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

        logger.debug("Attaching the pool to each of the hosts in the {}",
                dataCenterDao.findById(scope.getScopeId()));
        List<HostVO> hostsToConnect = resourceManager.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, scope.getScopeId(), hypervisorType);
        logger.debug(String.format("Attaching the pool to each of the hosts %s in the zone: %s", hostsToConnect, scope.getScopeId()));
        List<Long> hostIds = hostsToConnect.stream().map(HostVO::getId).collect(Collectors.toList());

        storageMgr.connectHostsToPool(dataStore, hostIds, scope, false, false);

        dataStoreHelper.attachZone(dataStore);
        return true;
    }

    @Override
    public boolean maintain(DataStore store) {
        Map<String, String> details = new HashMap<>();
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(store.getId());
        if (storagePoolVO != null) {
            sdcManager = ComponentContext.inject(sdcManager);
            sdcManager.populateSdcSettings(details, storagePoolVO.getDataCenterId());
            StoragePoolDetailVO systemIdDetail = storagePoolDetailsDao.findDetail(store.getId(), ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
            if (systemIdDetail != null) {
                details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemIdDetail.getValue());
                StoragePoolDetailVO mdmsDetail = storagePoolDetailsDao.findDetail(store.getId(), ScaleIOGatewayClient.STORAGE_POOL_MDMS);
                if (mdmsDetail != null) {
                    details.put(ScaleIOGatewayClient.STORAGE_POOL_MDMS, mdmsDetail.getValue());
                    details.put(ScaleIOSDCManager.ConnectOnDemand.key(), "false");
                }
            }
        }

        storagePoolAutomation.maintain(store, details);
        dataStoreHelper.maintain(store);
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        Map<String, String> details = new HashMap<>();
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(store.getId());
        if (storagePoolVO != null) {
            sdcManager = ComponentContext.inject(sdcManager);
            sdcManager.populateSdcSettings(details, storagePoolVO.getDataCenterId());
            StoragePoolDetailVO systemIdDetail = storagePoolDetailsDao.findDetail(store.getId(), ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
            if (systemIdDetail != null) {
                details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemIdDetail.getValue());
                if (sdcManager.areSDCConnectionsWithinLimit(store.getId())) {
                    details.put(ScaleIOSDCManager.ConnectOnDemand.key(), String.valueOf(ScaleIOSDCManager.ConnectOnDemand.valueIn(storagePoolVO.getDataCenterId())));
                    StoragePoolDetailVO mdmsDetail = storagePoolDetailsDao.findDetail(store.getId(), ScaleIOGatewayClient.STORAGE_POOL_MDMS);
                    if (mdmsDetail != null) {
                        details.put(ScaleIOGatewayClient.STORAGE_POOL_MDMS, mdmsDetail.getValue());
                    }
                }
            }
        }

        dataStoreHelper.cancelMaintain(store);
        storagePoolAutomation.cancelMaintain(store, details);
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
        if (cleanupDatastore(dataStore)) {
            ScaleIOGatewayClientConnectionPool.getInstance().removeClient(dataStore);

            boolean isDeleted = dataStoreHelper.deletePrimaryDataStore(dataStore);
            if (isDeleted) {
                primaryDataStoreDao.removeDetails(dataStore.getId());
            }
            return isDeleted;
        }
        return false;
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
            logger.info("Storage pool successfully updated");
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
