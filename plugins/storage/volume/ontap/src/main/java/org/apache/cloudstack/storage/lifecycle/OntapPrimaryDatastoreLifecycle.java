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
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
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
    private static final Logger logger = LogManager.getLogger(OntapPrimaryDatastoreLifecycle.class);

    private static final long ONTAP_MIN_VOLUME_SIZE_IN_BYTES = 1677721600L;

    /**
     * Creates primary storage on NetApp storage
     * @param dsInfos datastore information map
     * @return DataStore instance
     */
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        if (dsInfos == null) {
            throw new CloudRuntimeException("Datastore info map is null, cannot create primary storage");
        }
        Long zoneId = (Long) dsInfos.get("zoneId");
        Long podId = (Long) dsInfos.get("podId");
        Long clusterId = (Long) dsInfos.get("clusterId");
        String storagePoolName = (String) dsInfos.get("name");
        String providerName = (String) dsInfos.get("providerName");
        Long capacityBytes = (Long) dsInfos.get("capacityBytes");
        boolean managed = (boolean) dsInfos.get("managed");
        String tags = (String) dsInfos.get("tags");
        Boolean isTagARule = (Boolean) dsInfos.get("isTagARule");

        logger.info("Creating ONTAP primary storage pool with name: " + storagePoolName + ", provider: " + providerName +
                ", zoneId: " + zoneId + ", podId: " + podId + ", clusterId: " + clusterId);
        logger.debug("Received capacityBytes from UI: " + capacityBytes);

        // Additional details requested for ONTAP primary storage pool creation
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) dsInfos.get("details");

        capacityBytes = validateInitializeInputs(capacityBytes, podId, clusterId, zoneId, storagePoolName, providerName, managed, details);

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();
        if (clusterId != null) {
            ClusterVO clusterVO = _clusterDao.findById(clusterId);
            Preconditions.checkNotNull(clusterVO, "Unable to locate the specified cluster");
            if (clusterVO.getHypervisorType() != Hypervisor.HypervisorType.KVM) {
                throw new CloudRuntimeException("ONTAP primary storage is supported only for KVM hypervisor");
            }
            parameters.setHypervisorType(clusterVO.getHypervisorType());
        }

        details.put(OntapStorageConstants.SIZE, capacityBytes.toString());

        ProtocolType protocol = ProtocolType.valueOf(details.get(OntapStorageConstants.PROTOCOL));

        OntapStorage ontapStorage = new OntapStorage(
                details.get(OntapStorageConstants.USERNAME),
                details.get(OntapStorageConstants.PASSWORD),
                details.get(OntapStorageConstants.STORAGE_IP),
                details.get(OntapStorageConstants.SVM_NAME),
                capacityBytes,
                protocol);

        StorageStrategy storageStrategy = StorageProviderFactory.getStrategy(ontapStorage);
        boolean isValid = storageStrategy.connect();
        if (isValid) {
            // Get the DataLIF for data access
            String dataLIF = storageStrategy.getNetworkInterface();
            if (dataLIF == null || dataLIF.isEmpty()) {
                throw new CloudRuntimeException("Failed to retrieve Data LIF from ONTAP, cannot create primary storage");
            }
            logger.info("Using Data LIF for storage access: " + dataLIF);
            details.put(OntapStorageConstants.DATA_LIF, dataLIF);
            logger.info("Creating ONTAP volume '" + storagePoolName + "' with size: " + capacityBytes + " bytes (" +
                    (capacityBytes / (1024 * 1024 * 1024)) + " GB)");
            try {
                Volume volume = storageStrategy.createStorageVolume(storagePoolName, capacityBytes);
                if (volume == null) {
                    logger.error("createStorageVolume returned null for volume: " + storagePoolName);
                    throw new CloudRuntimeException("Failed to create ONTAP volume: " + storagePoolName);
                }
                logger.info("Volume object retrieved successfully. UUID: " + volume.getUuid() + ", Name: " + volume.getName());
                details.putIfAbsent(OntapStorageConstants.VOLUME_UUID, volume.getUuid());
                details.putIfAbsent(OntapStorageConstants.VOLUME_NAME, volume.getName());
            } catch (Exception e) {
                logger.error("Exception occurred while creating ONTAP volume: " + storagePoolName, e);
                throw new CloudRuntimeException("Failed to create ONTAP volume: " + storagePoolName + ". Error: " + e.getMessage(), e);
            }
        } else {
            throw new CloudRuntimeException("ONTAP details validation failed, cannot create primary storage");
        }

        // Determine storage pool type, path and port based on protocol
        String path;
        int port;
        switch (protocol) {
            case NFS3:
                parameters.setType(Storage.StoragePoolType.NetworkFilesystem);
                path = OntapStorageConstants.SLASH + storagePoolName;
                port = OntapStorageConstants.NFS3_PORT;
                // Force NFSv3 for ONTAP managed storage to avoid NFSv4 ID mapping issues
                details.put(OntapStorageConstants.NFS_MOUNT_OPTIONS, OntapStorageConstants.NFS3_MOUNT_OPTIONS_VER_3);
                logger.info("Setting NFS path for storage pool: " + path + ", port: " + port + " with mount option: vers=3");
                break;
            case ISCSI:
                parameters.setType(Storage.StoragePoolType.Iscsi);
                path = storageStrategy.getStoragePath();
                port = OntapStorageConstants.ISCSI_PORT;
                logger.info("Setting iSCSI path for storage pool: " + path + ", port: " + port);
                break;
            default:
                throw new CloudRuntimeException("Unsupported protocol: " + protocol + ", cannot create primary storage");
        }

        parameters.setHost(details.get(OntapStorageConstants.DATA_LIF));
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

    private long validateInitializeInputs(Long capacityBytes, Long podId, Long clusterId, Long zoneId,
                                          String storagePoolName, String providerName, boolean managed, Map<String, String> details) {

        // Validate and set capacity
        if (capacityBytes == null || capacityBytes <= 0) {
            logger.warn("capacityBytes not provided or invalid (" + capacityBytes + "), using ONTAP minimum size: " + ONTAP_MIN_VOLUME_SIZE_IN_BYTES);
            capacityBytes = ONTAP_MIN_VOLUME_SIZE_IN_BYTES;
        } else if (capacityBytes < ONTAP_MIN_VOLUME_SIZE_IN_BYTES) {
            logger.warn("capacityBytes (" + capacityBytes + ") is below ONTAP minimum (" + ONTAP_MIN_VOLUME_SIZE_IN_BYTES + "), adjusting to minimum");
            capacityBytes = ONTAP_MIN_VOLUME_SIZE_IN_BYTES;
        }

        // Validate scope
        if (podId == null ^ clusterId == null) {
            throw new CloudRuntimeException("Cluster Id or Pod Id is null, cannot create primary storage");
        }

        if (podId == null) {
            if (zoneId != null) {
                logger.info("Both Pod Id and Cluster Id are null, Primary storage pool will be associated with a Zone");
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

        logger.debug("ONTAP primary storage will be created as " + (managed ? "managed" : "unmanaged"));
        if (!managed) {
            throw new CloudRuntimeException("ONTAP primary storage must be managed");
        }

        //Required ONTAP detail keys
        Set<String> requiredKeys = Set.of(
                OntapStorageConstants.USERNAME,
                OntapStorageConstants.PASSWORD,
                OntapStorageConstants.SVM_NAME,
                OntapStorageConstants.PROTOCOL,
                OntapStorageConstants.STORAGE_IP
        );

        // Validate existing entries (reject unexpected keys, empty values)
        for (Map.Entry<String, String> e : details.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (!requiredKeys.contains(key)) {
                throw new CloudRuntimeException("Unexpected ONTAP detail key in URL: " + key);
            }
            if (val == null || val.isEmpty()) {
                throw new CloudRuntimeException("ONTAP primary storage creation failed, empty detail: " + key);
            }
        }

        // Detect missing required keys
        Set<String> providedKeys = new java.util.HashSet<>(details.keySet());
        if (!providedKeys.containsAll(requiredKeys)) {
            Set<String> missing = new java.util.HashSet<>(requiredKeys);
            missing.removeAll(providedKeys);
            throw new CloudRuntimeException("ONTAP primary storage creation failed, missing detail(s): " + missing);
        }

        return capacityBytes;
    }

    @Override
    public boolean attachCluster(DataStore dataStore, ClusterScope scope) {
        logger.debug("In attachCluster for ONTAP primary storage");
        if (dataStore == null) {
            throw new InvalidParameterValueException(" dataStore should not be null");
        }
        if (scope == null) {
            throw new InvalidParameterValueException(" scope should not be null");
        }
        List<String> hostsIdentifier = new ArrayList<>();
        StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
        if (storagePool == null) {
            logger.error("attachCluster : Storage Pool not found for id: " + dataStore.getId());
            throw new CloudRuntimeException(" Storage Pool not found for id: " + dataStore.getId());
        }
        PrimaryDataStoreInfo primaryStore = (PrimaryDataStoreInfo)dataStore;
        List<HostVO> hostsToConnect = _resourceMgr.getEligibleUpAndEnabledHostsInClusterForStorageConnection(primaryStore);
        logger.debug("attachCluster: Eligible Up and Enabled hosts: {} in cluster {}", hostsToConnect, primaryStore.getClusterId());

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(primaryStore.getId());
        StorageStrategy strategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);

        ProtocolType protocol = ProtocolType.valueOf(details.get(OntapStorageConstants.PROTOCOL));
        if (!validateProtocolSupportAndFetchHostsIdentifier(hostsToConnect, protocol, hostsIdentifier)) {
            String errMsg = "attachCluster: Not all hosts in the cluster support the protocol: " + protocol.name();
            logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        logger.debug("attachCluster: Attaching the pool to each of the host in the cluster: {}", primaryStore.getClusterId());
        // We need to create export policy at pool level and igroup at host level(in grantAccess)
        if (ProtocolType.NFS3.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
            // If there are no eligible host, export policy or igroup will not be created and will be taken as part of HostListener
            if (!hostsIdentifier.isEmpty()) {
                try {
                    AccessGroup accessGroupRequest = new AccessGroup();
                    accessGroupRequest.setHostsToConnect(hostsToConnect);
                    accessGroupRequest.setScope(scope);
                    accessGroupRequest.setStoragePoolId(storagePool.getId());
                    strategy.createAccessGroup(accessGroupRequest);
                } catch (Exception e) {
                    logger.error("attachCluster: Failed to create access group on storage system for cluster: " + primaryStore.getClusterId() + ". Exception: " + e.getMessage());
                    throw new CloudRuntimeException("Failed to create access group on storage system for cluster: " + primaryStore.getClusterId() + ". Exception: " + e.getMessage());
                }
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
            throw new InvalidParameterValueException("dataStore should not be null");
        }
        if (scope == null) {
            throw new InvalidParameterValueException("scope should not be null");
        }
        List<String> hostsIdentifier = new ArrayList<>();
        StoragePoolVO storagePool = storagePoolDao.findById(dataStore.getId());
        if (storagePool == null) {
            logger.error("attachZone : Storage Pool not found for id: " + dataStore.getId());
            throw new CloudRuntimeException("Storage Pool not found for id: " + dataStore.getId());
        }

        PrimaryDataStoreInfo primaryStore = (PrimaryDataStoreInfo)dataStore;
        List<HostVO> hostsToConnect = _resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, scope.getScopeId(), Hypervisor.HypervisorType.KVM);
        logger.debug(String.format("In createPool. Attaching the pool to each of the hosts in %s.", hostsToConnect));

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(primaryStore.getId());
        StorageStrategy strategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);

        logger.debug("attachZone: Eligible Up and Enabled hosts: {}", hostsToConnect);
        ProtocolType protocol = ProtocolType.valueOf(details.get(OntapStorageConstants.PROTOCOL));
        if (!validateProtocolSupportAndFetchHostsIdentifier(hostsToConnect, protocol, hostsIdentifier)) {
            String errMsg = "attachZone: Not all hosts in the zone support the protocol: " + protocol.name();
            logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

        // We need to create export policy at pool level and igroup at host level
        if (ProtocolType.NFS3.name().equalsIgnoreCase(details.get(OntapStorageConstants.PROTOCOL))) {
            // If there are no eligible host, export policy or igroup will not be created and will be taken as part of HostListener
            if (!hostsIdentifier.isEmpty()) {
                try {
                    AccessGroup accessGroupRequest = new AccessGroup();
                    accessGroupRequest.setHostsToConnect(hostsToConnect);
                    accessGroupRequest.setScope(scope);
                    accessGroupRequest.setStoragePoolId(storagePool.getId());
                    strategy.createAccessGroup(accessGroupRequest);
                } catch (Exception e) {
                    logger.error("attachZone: Failed to create access group on storage system for zone with Exception: " + e.getMessage());
                    throw new CloudRuntimeException(" Failed to create access group on storage system for zone with Exception: " + e.getMessage());
                }
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
                String protocolPrefix = OntapStorageConstants.IQN;
                for (HostVO host : hosts) {
                    if (host == null || host.getStorageUrl() == null || host.getStorageUrl().trim().isEmpty()
                            || !host.getStorageUrl().startsWith(protocolPrefix)) {
                        // TODO we will inform customer through alert for excluded host because of protocol enabled on host
                        continue;
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
                            // TODO we will inform customer through alert for excluded host because of protocol enabled on host
                            continue;
                        } else {
                            ip = ip.isEmpty() ? host.getPrivateIpAddress().trim() : ip;
                        }
                    }
                    hostIdentifiers.add(ip);
                }
                break;
            default:
                throw new CloudRuntimeException("Unsupported protocol: " + protocolType.name());
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
        logger.info("deleteDataStore: Starting deletion process for storage pool id: {}", store.getId());

        long storagePoolId = store.getId();
        // Get the StoragePool details
        StoragePool storagePool = _storageMgr.getStoragePool(storagePoolId);
        if (storagePool == null) {
            logger.warn("deleteDataStore: Storage pool not found for id: {}, skipping deletion", storagePoolId);
            return true; // Return true since the entity doesn't exist
        }

        try {
            // Fetch storage pool details
            Map<String, String> details = _datastoreDetailsDao.listDetailsKeyPairs(storagePoolId);
            if (details == null || details.isEmpty()) {
                logger.warn("deleteDataStore: No details found for storage pool id: {}, proceeding with CS entity deletion only", storagePoolId);
                return _dataStoreHelper.deletePrimaryDataStore(store);
            }

            logger.info("deleteDataStore: Deleting access groups for storage pool '{}'", storagePool.getName());

            // Get the storage strategy to interact with ONTAP
            StorageStrategy storageStrategy = OntapStorageUtils.getStrategyByStoragePoolDetails(details);

            // Cast DataStore to PrimaryDataStoreInfo to get full details
            PrimaryDataStoreInfo primaryDataStoreInfo = (PrimaryDataStoreInfo) store;
            primaryDataStoreInfo.setDetails(details);

            // Call deleteStorageVolume to delete the underlying ONTAP volume
            logger.info("deleteDataStore: Deleting ONTAP volume for storage pool '{}'", storagePool.getName());
            Volume volume = new Volume();
            volume.setUuid(details.get(OntapStorageConstants.VOLUME_UUID));
            volume.setName(details.get(OntapStorageConstants.VOLUME_NAME));
            try {
                if (volume.getUuid() == null || volume.getUuid().isEmpty() || volume.getName() == null || volume.getName().isEmpty()) {
                    logger.error("deleteDataStore: Volume UUID/Name not found in details for storage pool id: {}, cannot delete volume", storagePoolId);
                    throw new CloudRuntimeException("Volume UUID/Name not found in details, cannot delete ONTAP volume");
                }
                storageStrategy.deleteStorageVolume(volume);
                logger.info("deleteDataStore: Successfully deleted ONTAP volume '{}' (UUID: {}) for storage pool '{}'",
                        volume.getName(), volume.getUuid(), storagePool.getName());
            } catch (Exception e) {
                logger.error("deleteDataStore: Exception while retrieving volume UUID for storage pool id: {}. Error: {}",
                        storagePoolId, e.getMessage(), e);
            }
            AccessGroup accessGroup = new AccessGroup();
            accessGroup.setStoragePoolId(storagePoolId);
            // Delete access groups associated with this storage pool
            storageStrategy.deleteAccessGroup(accessGroup);
            logger.info("deleteDataStore: Successfully deleted access groups for storage pool '{}'", storagePool.getName());

        } catch (Exception e) {
            logger.error("deleteDataStore: Failed to delete access groups for storage pool id: {}. Error: {}",
                    storagePoolId, e.getMessage(), e);
            // Continue with CloudStack entity deletion even if ONTAP cleanup fails
            logger.warn("deleteDataStore: Proceeding with CloudStack entity deletion despite ONTAP cleanup failure");
        }

        // Delete the CloudStack primary data store entity
        return _dataStoreHelper.deletePrimaryDataStore(store);
    }


    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
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
