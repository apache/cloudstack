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

package org.apache.cloudstack.storage.lifecycle;


import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Preconditions;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.lifecycle.BasePrimaryDataStoreLifeCycleImpl;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Volume;
import org.apache.cloudstack.storage.provider.StorageProviderFactory;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OntapPrimaryDatastoreLifecycle extends BasePrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    @Inject private ClusterDao _clusterDao;
    @Inject private StorageManager _storageMgr;
    @Inject private ResourceManager _resourceMgr;
    @Inject private PrimaryDataStoreHelper _dataStoreHelper;
    @Inject private PrimaryDataStoreDetailsDao _datastoreDetailsDao;
    @Inject private StoragePoolAutomation _storagePoolAutomation;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;
    private static final Logger s_logger = LogManager.getLogger(OntapPrimaryDatastoreLifecycle.class);

    private static final long ONTAP_MIN_VOLUME_SIZE = 1677721600L;

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        if (dsInfos == null) {
            throw new CloudRuntimeException("Datastore info map is null, cannot create primary storage");
        }
        String url = (String) dsInfos.get("url");
        Long zoneId = (Long) dsInfos.get("zoneId");
        Long podId = (Long) dsInfos.get("podId");
        Long clusterId = (Long) dsInfos.get("clusterId");
        String storagePoolName = (String) dsInfos.get("name");
        String providerName = (String) dsInfos.get("providerName");
        Long capacityBytes = (Long) dsInfos.get("capacityBytes");
        boolean managed = (boolean) dsInfos.get("managed");
        String tags = (String) dsInfos.get("tags");
        Boolean isTagARule = (Boolean) dsInfos.get("isTagARule");

        s_logger.info("Creating ONTAP primary storage pool with name: " + storagePoolName + ", provider: " + providerName +
                ", zoneId: " + zoneId + ", podId: " + podId + ", clusterId: " + clusterId);
        s_logger.debug("Received capacityBytes from UI: " + capacityBytes);

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) dsInfos.get("details");

        if (capacityBytes == null || capacityBytes <= 0) {
            s_logger.warn("capacityBytes not provided or invalid (" + capacityBytes + "), using ONTAP minimum size: " + ONTAP_MIN_VOLUME_SIZE);
            capacityBytes = ONTAP_MIN_VOLUME_SIZE;
        } else if (capacityBytes < ONTAP_MIN_VOLUME_SIZE) {
            s_logger.warn("capacityBytes (" + capacityBytes + ") is below ONTAP minimum (" + ONTAP_MIN_VOLUME_SIZE + "), adjusting to minimum");
            capacityBytes = ONTAP_MIN_VOLUME_SIZE;
        }

        if (podId == null ^ clusterId == null) {
            throw new CloudRuntimeException("Cluster Id or Pod Id is null, cannot create primary storage");
        }

        if (podId == null && clusterId == null) {
            if (zoneId != null) {
                s_logger.info("Both Pod Id and Cluster Id are null, Primary storage pool will be associated with a Zone");
            } else {
                throw new CloudRuntimeException("Pod Id, Cluster Id and Zone Id are all null, cannot create primary storage");
            }
        }

        if (storagePoolName == null || storagePoolName.isEmpty()) {
            throw new CloudRuntimeException("Storage pool name is null or empty, cannot create primary storage");
        }

        if (providerName == null || providerName.isEmpty()) {
            throw new CloudRuntimeException("Provider name is null or empty, cannot create primary storage");
        }

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();
        if (clusterId != null) {
            ClusterVO clusterVO = _clusterDao.findById(clusterId);
            Preconditions.checkNotNull(clusterVO, "Unable to locate the specified cluster");
            if (clusterVO.getHypervisorType() != Hypervisor.HypervisorType.KVM) {
                throw new CloudRuntimeException("ONTAP primary storage is supported only for KVM hypervisor");
            }
            parameters.setHypervisorType(clusterVO.getHypervisorType());
        }

        s_logger.debug("ONTAP primary storage will be created as " + (managed ? "managed" : "unmanaged"));
        if (!managed) {
            throw new CloudRuntimeException("ONTAP primary storage must be managed");
        }

        Set<String> requiredKeys = Set.of(
                Constants.USERNAME,
                Constants.PASSWORD,
                Constants.SVM_NAME,
                Constants.PROTOCOL,
                Constants.MANAGEMENT_LIF
        );

        Set<String> optionalKeys = Set.of(
                Constants.IS_DISAGGREGATED
        );

        Set<String> allowedKeys = new java.util.HashSet<>(requiredKeys);
        allowedKeys.addAll(optionalKeys);

        if (url != null && !url.isEmpty()) {
            for (String segment : url.split(Constants.SEMICOLON)) {
                if (segment.isEmpty()) {
                    continue;
                }
                String[] kv = segment.split(Constants.EQUALS, 2);
                if (kv.length == 2) {
                    details.put(kv[0].trim(), kv[1].trim());
                }
            }
        }

        for (Map.Entry<String, String> e : details.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (!allowedKeys.contains(key)) {
                throw new CloudRuntimeException("Unexpected ONTAP detail key in URL: " + key);
            }
            if (val == null || val.isEmpty()) {
                throw new CloudRuntimeException("ONTAP primary storage creation failed, empty detail: " + key);
            }
        }

        Set<String> providedKeys = new java.util.HashSet<>(details.keySet());
        if (!providedKeys.containsAll(requiredKeys)) {
            Set<String> missing = new java.util.HashSet<>(requiredKeys);
            missing.removeAll(providedKeys);
            throw new CloudRuntimeException("ONTAP primary storage creation failed, missing detail(s): " + missing);
        }

        details.put(Constants.SIZE, capacityBytes.toString());

        details.putIfAbsent(Constants.IS_DISAGGREGATED, "false");

        ProtocolType protocol = ProtocolType.valueOf(details.get(Constants.PROTOCOL));

        long volumeSize = Long.parseLong(details.get(Constants.SIZE));
        OntapStorage ontapStorage = new OntapStorage(
                details.get(Constants.USERNAME),
                details.get(Constants.PASSWORD),
                details.get(Constants.MANAGEMENT_LIF),
                details.get(Constants.SVM_NAME),
                volumeSize,
                protocol,
                Boolean.parseBoolean(details.get(Constants.IS_DISAGGREGATED).toLowerCase()));

        StorageStrategy storageStrategy = StorageProviderFactory.getStrategy(ontapStorage);
        boolean isValid = storageStrategy.connect();
        if (isValid) {
            String dataLIF = storageStrategy.getNetworkInterface();
            if (dataLIF == null || dataLIF.isEmpty()) {
                throw new CloudRuntimeException("Failed to retrieve Data LIF from ONTAP, cannot create primary storage");
            }
            s_logger.info("Using Data LIF for storage access: " + dataLIF);
            details.put(Constants.DATA_LIF, dataLIF);
            s_logger.info("Creating ONTAP volume '" + storagePoolName + "' with size: " + volumeSize + " bytes (" +
                    (volumeSize / (1024 * 1024 * 1024)) + " GB)");
            try {
                Volume volume = storageStrategy.createStorageVolume(storagePoolName, volumeSize);
                if (volume == null) {
                    s_logger.error("createStorageVolume returned null for volume: " + storagePoolName);
                    throw new CloudRuntimeException("Failed to create ONTAP volume: " + storagePoolName);
                }
                s_logger.info("Volume object retrieved successfully. UUID: " + volume.getUuid() + ", Name: " + volume.getName());
                details.putIfAbsent(Constants.VOLUME_UUID, volume.getUuid());
                details.putIfAbsent(Constants.VOLUME_NAME, volume.getName());
            } catch (Exception e) {
                s_logger.error("Exception occurred while creating ONTAP volume: " + storagePoolName, e);
                throw new CloudRuntimeException("Failed to create ONTAP volume: " + storagePoolName + ". Error: " + e.getMessage(), e);
            }
        } else {
            throw new CloudRuntimeException("ONTAP details validation failed, cannot create primary storage");
        }

        String path;
        int port;
        switch (protocol) {
            case NFS3:
                parameters.setType(Storage.StoragePoolType.NetworkFilesystem);
                path = Constants.SLASH + storagePoolName;
                port = Constants.NFS3_PORT;
                s_logger.info("Setting NFS path for storage pool: " + path + ", port: " + port);
                break;
            case ISCSI:
                parameters.setType(Storage.StoragePoolType.Iscsi);
                path = storageStrategy.getStoragePath();
                port = Constants.ISCSI_PORT;
                s_logger.info("Setting iSCSI path for storage pool: " + path + ", port: " + port);
                break;
            default:
                throw new CloudRuntimeException("Unsupported protocol: " + protocol + ", cannot create primary storage");
        }

        parameters.setHost(details.get(Constants.DATA_LIF));
        parameters.setPort(port);
        parameters.setPath(path);
        parameters.setTags(tags);
        parameters.setIsTagARule(isTagARule);
        parameters.setDetails(details);
        parameters.setUuid(UUID.randomUUID().toString());
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setClusterId(clusterId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(managed);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);

        return _dataStoreHelper.createPrimaryDataStore(parameters);
    }

    @Override
    public boolean attachCluster(DataStore dataStore, ClusterScope scope) {
        logger.debug("In attachCluster for ONTAP primary storage");
        if (dataStore == null) {
            throw new InvalidParameterValueException("attachCluster: dataStore should not be null");
        }
        if (scope == null) {
            throw new InvalidParameterValueException("attachCluster: scope should not be null");
        }
        List<String> hostsIdentifier = new ArrayList<>();
        StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
        if (storagePool == null) {
            s_logger.error("attachCluster : Storage Pool not found for id: " + dataStore.getId());
            throw new CloudRuntimeException("attachCluster : Storage Pool not found for id: " + dataStore.getId());
        }
        PrimaryDataStoreInfo primaryStore = (PrimaryDataStoreInfo)dataStore;
        List<HostVO> hostsToConnect = _resourceMgr.getEligibleUpAndEnabledHostsInClusterForStorageConnection(primaryStore);
        logger.debug("attachCluster: Eligible Up and Enabled hosts: {} in cluster {}", hostsToConnect, primaryStore.getClusterId());

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(primaryStore.getId());
        StorageStrategy strategy = Utility.getStrategyByStoragePoolDetails(details);

        ProtocolType protocol = ProtocolType.valueOf(details.get(Constants.PROTOCOL));
        if (!validateProtocolSupportAndFetchHostsIdentifier(hostsToConnect, protocol, hostsIdentifier)) {
            String errMsg = "attachCluster: Not all hosts in the cluster support the protocol: " + protocol.name();
            s_logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

        logger.debug("attachCluster: Attaching the pool to each of the host in the cluster: {}", primaryStore.getClusterId());
        if (hostsIdentifier != null && hostsIdentifier.size() > 0) {
            try {
                AccessGroup accessGroupRequest = new AccessGroup();
                accessGroupRequest.setHostsToConnect(hostsToConnect);
                accessGroupRequest.setScope(scope);
                primaryStore.setDetails(details);
                accessGroupRequest.setPrimaryDataStoreInfo(primaryStore);
                strategy.createAccessGroup(accessGroupRequest);
            } catch (Exception e) {
                s_logger.error("attachCluster: Failed to create access group on storage system for cluster: " + primaryStore.getClusterId() + ". Exception: " + e.getMessage());
                throw new CloudRuntimeException("attachCluster: Failed to create access group on storage system for cluster: " + primaryStore.getClusterId() + ". Exception: " + e.getMessage());
            }
        }
        logger.debug("attachCluster: Attaching the pool to each of the host in the cluster: {}", primaryStore.getClusterId());
        for (HostVO host : hostsToConnect) {
            try {
                _storageMgr.connectHostToSharedPool(host, dataStore.getId());
            } catch (Exception e) {
                logger.warn("attachCluster: Unable to establish a connection between " + host + " and " + dataStore, e);
                return false;
            }
        }
        _dataStoreHelper.attachCluster(dataStore);
        return true;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return false;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, Hypervisor.HypervisorType hypervisorType) {
        logger.debug("In attachZone for ONTAP primary storage");
        if (dataStore == null) {
            throw new InvalidParameterValueException("attachZone: dataStore should not be null");
        }
        if (scope == null) {
            throw new InvalidParameterValueException("attachZone: scope should not be null");
        }
        List<String> hostsIdentifier = new ArrayList<>();
        StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
        if (storagePool == null) {
            s_logger.error("attachZone : Storage Pool not found for id: " + dataStore.getId());
            throw new CloudRuntimeException("attachZone : Storage Pool not found for id: " + dataStore.getId());
        }

        PrimaryDataStoreInfo primaryStore = (PrimaryDataStoreInfo)dataStore;
        List<HostVO> hostsToConnect = _resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, scope.getScopeId(), Hypervisor.HypervisorType.KVM);
        logger.debug(String.format("In createPool. Attaching the pool to each of the hosts in %s.", hostsToConnect));

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(primaryStore.getId());
        StorageStrategy strategy = Utility.getStrategyByStoragePoolDetails(details);

        logger.debug("attachZone: Eligible Up and Enabled hosts: {}", hostsToConnect);
        ProtocolType protocol = ProtocolType.valueOf(details.get(Constants.PROTOCOL));
        if (!validateProtocolSupportAndFetchHostsIdentifier(hostsToConnect, protocol, hostsIdentifier)) {
            String errMsg = "attachZone: Not all hosts in the zone support the protocol: " + protocol.name();
            s_logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        if (hostsIdentifier != null && !hostsIdentifier.isEmpty()) {
            try {
                AccessGroup accessGroupRequest = new AccessGroup();
                accessGroupRequest.setHostsToConnect(hostsToConnect);
                accessGroupRequest.setScope(scope);
                primaryStore.setDetails(details);
                accessGroupRequest.setPrimaryDataStoreInfo(primaryStore);
                strategy.createAccessGroup(accessGroupRequest);
            } catch (Exception e) {
                s_logger.error("attachZone: Failed to create access group on storage system for zone with Exception: " + e.getMessage());
                throw new CloudRuntimeException("attachZone: Failed to create access group on storage system for zone with Exception: " + e.getMessage());
            }
        }
        for (HostVO host : hostsToConnect) {
            try {
                _storageMgr.connectHostToSharedPool(host, dataStore.getId());
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
                return  false;
            }
        }
        _dataStoreHelper.attachZone(dataStore);
        return true;
    }

    private boolean validateProtocolSupportAndFetchHostsIdentifier(List<HostVO> hosts, ProtocolType protocolType, List<String> hostIdentifiers) {
        switch (protocolType) {
            case ISCSI:
                String protocolPrefix = Constants.IQN;
                for (HostVO host : hosts) {
                    if (host == null || host.getStorageUrl() == null || host.getStorageUrl().trim().isEmpty()
                            || !host.getStorageUrl().startsWith(protocolPrefix)) {
                        return false;
                    }
                    hostIdentifiers.add(host.getStorageUrl());
                }
                break;
            case NFS3:
                String ip = "";
                for (HostVO host : hosts) {
                    if (host != null) {
                        ip =  host.getStorageIpAddress() != null ? host.getStorageIpAddress().trim() : "";
                        if (ip.isEmpty() && host.getPrivateIpAddress() != null || host.getPrivateIpAddress().trim().isEmpty()) {
                            return false;
                        } else {
                            ip = ip.isEmpty() ? host.getPrivateIpAddress().trim() : ip;
                        }
                    }
                    hostIdentifiers.add(ip);
                }
                break;
            default:
                throw new CloudRuntimeException("validateProtocolSupportAndFetchHostsIdentifier : Unsupported protocol: " + protocolType.name());
        }
        logger.info("validateProtocolSupportAndFetchHostsIdentifier: All hosts support the protocol: " + protocolType.name());
        return true;
    }

    @Override
    public boolean maintain(DataStore store) {
        logger.info("Placing storage pool {} in maintenance mode", store);
        if (_storagePoolAutomation.maintain(store)) {
            return _dataStoreHelper.maintain(store);
        } else {
            return false;
        }
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        logger.info("Cancelling storage pool maintenance for {}", store);
        if (_dataStoreHelper.cancelMaintain(store)) {
            return _storagePoolAutomation.cancelMaintain(store);
        } else {
            return false;
        }
    }

    @Override
    public boolean deleteDataStore(DataStore store) {
        s_logger.info("deleteDataStore: Starting deletion process for storage pool id: {}", store.getId());

        long storagePoolId = store.getId();
        StoragePool storagePool = _storageMgr.getStoragePool(storagePoolId);
        if (storagePool == null) {
            s_logger.warn("deleteDataStore: Storage pool not found for id: {}, skipping deletion", storagePoolId);
            return true;
        }

        try {
            Map<String, String> details = _datastoreDetailsDao.listDetailsKeyPairs(storagePoolId);
            if (details == null || details.isEmpty()) {
                s_logger.warn("deleteDataStore: No details found for storage pool id: {}, proceeding with CS entity deletion only", storagePoolId);
                return _dataStoreHelper.deletePrimaryDataStore(store);
            }

            s_logger.info("deleteDataStore: Deleting access groups for storage pool '{}'", storagePool.getName());

            StorageStrategy storageStrategy = Utility.getStrategyByStoragePoolDetails(details);

            PrimaryDataStoreInfo primaryDataStoreInfo = (PrimaryDataStoreInfo) store;
            primaryDataStoreInfo.setDetails(details);

            s_logger.info("deleteDataStore: Deleting ONTAP volume for storage pool '{}'", storagePool.getName());
            Volume volume = new Volume();
            volume.setUuid(details.get(Constants.VOLUME_UUID));
            volume.setName(details.get(Constants.VOLUME_NAME));
            try {
                if (volume.getUuid() == null || volume.getUuid().isEmpty() || volume.getName() == null || volume.getName().isEmpty()) {
                    s_logger.error("deleteDataStore: Volume UUID/Name not found in details for storage pool id: {}, cannot delete volume", storagePoolId);
                    throw new CloudRuntimeException("Volume UUID/Name not found in details, cannot delete ONTAP volume");
                }
                storageStrategy.deleteStorageVolume(volume);
                s_logger.info("deleteDataStore: Successfully deleted ONTAP volume '{}' (UUID: {}) for storage pool '{}'",
                        volume.getName(), volume.getUuid(), storagePool.getName());
            } catch (Exception e) {
                s_logger.error("deleteDataStore: Exception while retrieving volume UUID for storage pool id: {}. Error: {}",
                        storagePoolId, e.getMessage(), e);
            }
            AccessGroup accessGroup = new AccessGroup();
            accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
            storageStrategy.deleteAccessGroup(accessGroup);
            s_logger.info("deleteDataStore: Successfully deleted access groups for storage pool '{}'", storagePool.getName());

        } catch (Exception e) {
            s_logger.error("deleteDataStore: Failed to delete access groups for storage pool id: {}. Error: {}",
                    storagePoolId, e.getMessage(), e);
            s_logger.warn("deleteDataStore: Proceeding with CloudStack entity deletion despite ONTAP cleanup failure");
        }

        return _dataStoreHelper.deletePrimaryDataStore(store);
    }


    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return true;
    }

    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> details) {

    }

    @Override
    public void enableStoragePool(DataStore store) {
        _dataStoreHelper.enable(store);
    }

    @Override
    public void disableStoragePool(DataStore store) {
        _dataStoreHelper.disable(store);
    }

    @Override
    public void changeStoragePoolScopeToZone(DataStore store, ClusterScope clusterScope, Hypervisor.HypervisorType hypervisorType) {

    }

    @Override
    public void changeStoragePoolScopeToCluster(DataStore store, ClusterScope clusterScope, Hypervisor.HypervisorType hypervisorType) {

    }
}
