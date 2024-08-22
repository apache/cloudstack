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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapter;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolumeStorageStats;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.provider.AdaptivePrimaryDatastoreAdapterFactoryMap;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.host.Host;

/**
 * Manages the lifecycle of a Managed Data Store in CloudStack
 */
public class AdaptiveDataStoreLifeCycleImpl extends BasePrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    PrimaryDataStoreHelper _dataStoreHelper;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    private StoragePoolAutomation _storagePoolAutomation;
    @Inject
    private PrimaryDataStoreDao _primaryDataStoreDao;
    @Inject
    private StorageManager _storageMgr;
    @Inject
    private ClusterDao _clusterDao;
    AdaptivePrimaryDatastoreAdapterFactoryMap _adapterFactoryMap;

    public AdaptiveDataStoreLifeCycleImpl(AdaptivePrimaryDatastoreAdapterFactoryMap factoryMap) {
        _adapterFactoryMap = factoryMap;
    }

    /**
     * Initialize the storage pool
     * https://hostname:port?cpg=<cpgname>&snapcpg=<snapcpg>&hostset=<hostsetname>&disabletlsvalidation=true&
     */
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        // https://hostanme:443/cpgname/hostsetname.  hostset should map to the cluster or zone (all nodes in the cluster or zone MUST be in the hostset and be configured outside cloudstack for now)
        String url = (String) dsInfos.get("url");
        Long zoneId = (Long) dsInfos.get("zoneId");
        Long podId = (Long)dsInfos.get("podId");
        Long clusterId = (Long)dsInfos.get("clusterId");
        String dsName = (String) dsInfos.get("name");
        String providerName = (String) dsInfos.get("providerName");
        Long capacityBytes = (Long) dsInfos.get("capacityBytes");
        Long capacityIops = (Long)dsInfos.get("capacityIops");
        String tags = (String)dsInfos.get("tags");
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) dsInfos.get("details");

        // validate inputs are valid/provided as required
        if (zoneId == null) throw new CloudRuntimeException("Zone Id must be specified.");

        URL uri = null;
        try {
            uri = new URL(url);
        } catch (Exception ignored) {
            throw new CloudRuntimeException(url + " is not a valid uri");
        }

        String username = null;
        String password = null;
        String token = null;
        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.split(":").length < 2) {
            // check if it was passed in the details object
            username = details.get(ProviderAdapter.API_USERNAME_KEY);
            if (username != null) {
                password = details.get(ProviderAdapter.API_PASSWORD_KEY);
                userInfo = username + ":" + password;
            } else {
                token = details.get(ProviderAdapter.API_TOKEN_KEY);
            }
        } else {
            try {
                userInfo = java.net.URLDecoder.decode(userInfo, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new CloudRuntimeException("Unexpected error parsing the provided user info; check that it does not include any invalid characters");
            }

            username = userInfo.split(":")[0];
            password = userInfo.split(":")[1];
        }

        logger.info("Registering block storage provider with user=" + username);


        if (clusterId != null) {
            Hypervisor.HypervisorType hypervisorType = getHypervisorTypeForCluster(clusterId);

            if (!hypervisorType.equals(HypervisorType.KVM)) {
                throw new CloudRuntimeException("Unsupported hypervisor type for provided cluster: " + hypervisorType.toString());
            }

            // Primary datastore is cluster-wide, check and set the podId and clusterId parameters
            if (podId == null) {
                throw new CloudRuntimeException("Pod Id must also be specified when the Cluster Id is specified for Cluster-wide primary storage.");
            }

            logger.info("Registering with clusterid=" + clusterId + " which is confirmed to be a KVM host");

        } else if (podId != null) {
            throw new CloudRuntimeException("Cluster Id must also be specified when the Pod Id is specified for Cluster-wide primary storage.");
        }

        // validate we don't have any duplication going on
        List<StoragePoolVO> storagePoolVO = _primaryDataStoreDao.findPoolsByProvider(providerName);
        if (CollectionUtils.isNotEmpty(storagePoolVO)) {
            for (StoragePoolVO poolVO : storagePoolVO) {
                Map <String, String> poolDetails = _primaryDataStoreDao.getDetails(poolVO.getId());
                String otherPoolUrl = poolDetails.get(ProviderAdapter.API_URL_KEY);
                if (dsName.equals(poolVO.getName())) {
                    throw new InvalidParameterValueException("A pool with the name [" + dsName + "] already exists, choose another name");
                }

                if (uri.toString().equals(otherPoolUrl)) {
                    throw new IllegalArgumentException("Provider URL [" + otherPoolUrl + "] is already in use by another storage pool named [" + poolVO.getName() + "], please validate you have correct API and CPG");
                }
            }
        }

        logger.info("Validated no other pool exists with this name: " + dsName);

        try {
            PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();
            parameters.setHost(uri.getHost());
            parameters.setPort(uri.getPort());
            parameters.setPath(uri.getPath() + "?" + uri.getQuery());
            parameters.setType(StoragePoolType.FiberChannel);
            parameters.setZoneId(zoneId);
            parameters.setPodId(podId);
            parameters.setClusterId(clusterId);
            parameters.setName(dsName);
            parameters.setProviderName(providerName);
            parameters.setManaged(true);
            parameters.setUsedBytes(0);
            parameters.setCapacityIops(capacityIops);
            parameters.setHypervisorType(HypervisorType.KVM);
            parameters.setTags(tags);
            parameters.setUserInfo(userInfo);
            parameters.setUuid(UUID.randomUUID().toString());

            details.put(ProviderAdapter.API_URL_KEY, uri.toString());
            if (username != null) {
                details.put(ProviderAdapter.API_USERNAME_KEY, username);
            }

            if (password != null) {
                details.put(ProviderAdapter.API_PASSWORD_KEY, DBEncryptionUtil.encrypt(password));
            }

            if (token != null) {
                details.put(ProviderAdapter.API_TOKEN_KEY, DBEncryptionUtil.encrypt(details.get(ProviderAdapter.API_TOKEN_KEY)));
            }
            // this appears to control placing the storage pool above network file system based storage pools in priority
            details.put(Storage.Capability.HARDWARE_ACCELERATION.toString(), "true");
            // this new capablity indicates the storage pool allows volumes to migrate to/from other pools (i.e. to/from NFS pools)
            details.put(Storage.Capability.ALLOW_MIGRATE_OTHER_POOLS.toString(), "true");
            parameters.setDetails(details);

            // make sure the storage array is connectable and the pod and hostgroup objects exist
            ProviderAdapter api = _adapterFactoryMap.getAPI(parameters.getUuid(), providerName, details);

            // validate the provided details are correct/valid for the provider
            api.validate();

            // if we have user-provided capacity bytes, validate they do not exceed the manaaged storage capacity bytes
            ProviderVolumeStorageStats stats = api.getManagedStorageStats();
            if (capacityBytes != null && capacityBytes != 0 && stats != null) {
                if (stats.getCapacityInBytes() > 0) {
                    if (stats.getCapacityInBytes() < capacityBytes) {
                        throw new InvalidParameterValueException("Capacity bytes provided exceeds the capacity of the storage endpoint: provided by user: " + capacityBytes + ", storage capacity from storage provider: " + stats.getCapacityInBytes());
                    }
                }
                parameters.setCapacityBytes(capacityBytes);
            }
            // if we have no user-provided capacity bytes, use the ones provided by storage
            else {
                if (stats == null || stats.getCapacityInBytes() <= 0) {
                    throw new InvalidParameterValueException("Capacity bytes not available from the storage provider, user provided capacity bytes must be specified");
                }
                parameters.setCapacityBytes(stats.getCapacityInBytes());
            }

            logger.info("Persisting [" + dsName + "] storage pool metadata to database");
            return _dataStoreHelper.createPrimaryDataStore(parameters);
        } catch (Throwable e) {
            logger.error("Problem persisting storage pool", e);
            throw new CloudRuntimeException(e);
        }
    }

   /**
     * Get the type of Hypervisor from the cluster id
     * @param clusterId
     * @return
     */
    private Hypervisor.HypervisorType getHypervisorTypeForCluster(long clusterId) {
        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new CloudRuntimeException("Unable to locate the specified cluster: " + clusterId);
        }

        return cluster.getHypervisorType();
    }

    /**
     * Attach the pool to a cluster (all hosts in a single cluster)
     */
    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        logger.info("Attaching storage pool [" + store.getName() + "] to cluster [" + scope.getScopeId() + "]");
        _dataStoreHelper.attachCluster(store);

        StoragePoolVO dataStoreVO = _storagePoolDao.findById(store.getId());

        PrimaryDataStoreInfo primarystore = (PrimaryDataStoreInfo) store;
        // Check if there is host up in this cluster
        List<HostVO> allHosts = _resourceMgr.listAllUpHosts(Host.Type.Routing, primarystore.getClusterId(), primarystore.getPodId(), primarystore.getDataCenterId());
        if (allHosts.isEmpty()) {
            _primaryDataStoreDao.expunge(primarystore.getId());
            throw new CloudRuntimeException("No host up to associate a storage pool with in cluster " + primarystore.getClusterId());
        }

        if (dataStoreVO.isManaged()) {
            //boolean success = false;
            for (HostVO h : allHosts) {
                logger.debug("adding host " + h.getName() + " to storage pool " + store.getName());
            }
        }

        logger.debug("In createPool Adding the pool to each of the hosts");
        List<HostVO> poolHosts = new ArrayList<HostVO>();
        for (HostVO h : allHosts) {
            try {
                _storageMgr.connectHostToSharedPool(h.getId(), primarystore.getId());
                poolHosts.add(h);
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + h + " and " + primarystore, e);
            }
        }

        if (poolHosts.isEmpty()) {
            logger.warn("No host can access storage pool " + primarystore + " on cluster " + primarystore.getClusterId());
            _primaryDataStoreDao.expunge(primarystore.getId());
            throw new CloudRuntimeException("Failed to access storage pool");
        }

        return true;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        logger.info("Attaching storage pool [" + store.getName() + "] to host [" + scope.getScopeId() + "]");
        _dataStoreHelper.attachHost(store, scope, existingInfo);
        return true;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        logger.info("Attaching storage pool [" + dataStore.getName() + "] to zone [" + scope.getScopeId() + "]");
        List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(hypervisorType, scope.getScopeId());
        List<HostVO> poolHosts = new ArrayList<HostVO>();
        for (HostVO host : hosts) {
            try {
                _storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
                poolHosts.add(host);
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }
        if (poolHosts.isEmpty()) {
            logger.warn("No host can access storage pool " + dataStore + " in this zone.");
            _primaryDataStoreDao.expunge(dataStore.getId());
            throw new CloudRuntimeException("Failed to create storage pool as it is not accessible to hosts.");
        }
        _dataStoreHelper.attachZone(dataStore, hypervisorType);
        return true;
    }

    /**
     * Put the storage pool in maintenance mode
     */
    @Override
    public boolean maintain(DataStore store) {
        logger.info("Placing storage pool [" + store.getName() + "] in maintainence mode");
        if (_storagePoolAutomation.maintain(store)) {
            return _dataStoreHelper.maintain(store);
        } else {
            return false;
        }
    }

    /**
     * Cancel maintenance mode
     */
    @Override
    public boolean cancelMaintain(DataStore store) {
        logger.info("Canceling storage pool maintainence for [" + store.getName() + "]");
        if (_dataStoreHelper.cancelMaintain(store)) {
            return _storagePoolAutomation.cancelMaintain(store);
        } else {
            return false;
        }
    }

    /**
     * Delete the data store
     */
    @Override
    public boolean deleteDataStore(DataStore store) {
        logger.info("Delete datastore called for [" + store.getName() + "]");
        return _dataStoreHelper.deletePrimaryDataStore(store);
    }

    /**
     * Migrate objects in this store to another store
     */
    @Override
    public boolean migrateToObjectStore(DataStore store) {
        logger.info("Migrate datastore called for [" + store.getName() + "].  This is not currently implemented for this provider at this time");
        return false;
    }

    /**
     * Update the storage pool configuration
     */
    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> newDetails) {
        _adapterFactoryMap.updateAPI(storagePool.getUuid(), storagePool.getStorageProviderName(), newDetails);
    }

    /**
     * Enable the storage pool (allows volumes from this pool)
     */
    @Override
    public void enableStoragePool(DataStore store) {
        logger.info("Enabling storage pool [" + store.getName() + "]");
        _dataStoreHelper.enable(store);
    }

    /**
     * Disable storage pool (stops new volume provisioning from pool)
     */
    @Override
    public void disableStoragePool(DataStore store) {
        logger.info("Disabling storage pool [" + store.getName() + "]");
        _dataStoreHelper.disable(store);
    }
}
