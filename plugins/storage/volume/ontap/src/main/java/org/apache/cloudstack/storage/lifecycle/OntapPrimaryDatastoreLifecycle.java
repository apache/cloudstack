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
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Preconditions;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.lifecycle.BasePrimaryDataStoreLifeCycleImpl;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.provider.StorageProviderFactory;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OntapPrimaryDatastoreLifecycle extends BasePrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    @Inject private ClusterDao _clusterDao;
    @Inject private StorageManager _storageMgr;
    @Inject private ResourceManager _resourceMgr;
    @Inject private PrimaryDataStoreHelper _dataStoreHelper;
    private static final Logger s_logger = (Logger)LogManager.getLogger(OntapPrimaryDatastoreLifecycle.class);

    /**
     * Creates primary storage on NetApp storage
     * @param dsInfos
     * @return
     */
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        if (dsInfos == null) {
            throw new CloudRuntimeException("Datastore info map is null, cannot create primary storage");
        }
        String url = dsInfos.get("url").toString(); // TODO: Decide on whether should the customer enter just the Management LIF IP or https://ManagementLIF
        Long zoneId = (Long) dsInfos.get("zoneId");
        Long podId = (Long)dsInfos.get("podId");
        Long clusterId = (Long)dsInfos.get("clusterId");
        String storagePoolName = dsInfos.get("name").toString();
        String providerName = dsInfos.get("providerName").toString();
        String tags = dsInfos.get("tags").toString();
        Boolean isTagARule = (Boolean) dsInfos.get("isTagARule");
        String scheme = dsInfos.get("scheme").toString();

        s_logger.info("Creating ONTAP primary storage pool with name: " + storagePoolName + ", provider: " + providerName +
                ", zoneId: " + zoneId + ", podId: " + podId + ", clusterId: " + clusterId + ", protocol: " + scheme);

        // Additional details requested for ONTAP primary storage pool creation
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>)dsInfos.get("details");
        // Validations
        if (podId != null && clusterId == null) {
            s_logger.error("Cluster Id is null, cannot create primary storage");
            return null;
        } else if (podId == null && clusterId != null) {
            s_logger.error("Pod Id is null, cannot create primary storage");
            return null;
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
                throw new CloudRuntimeException("ONTAP primary storage is not supported for KVM hypervisor");
            }
            parameters.setHypervisorType(clusterVO.getHypervisorType());
        }

        // TODO: While testing need to check what does this actually do and if the fields corresponding to each protocol should also be set
        // TODO: scheme could be 'custom' in our case and we might have to ask 'protocol' separately to the user
        String protocol = details.get(Constants.PROTOCOL);
        switch (protocol.toLowerCase()) {
            case Constants.NFS:
                parameters.setType(Storage.StoragePoolType.NetworkFilesystem);
                break;
            case Constants.ISCSI:
                parameters.setType(Storage.StoragePoolType.Iscsi);
                break;
            default:
                throw new CloudRuntimeException("Unsupported protocol: " + scheme + ", cannot create primary storage");
        }

        details.put(Constants.MANAGEMENTLIF, url);

        // Validate the ONTAP details
        if(details.get(Constants.ISDISAGGREGATED) == null || details.get(Constants.ISDISAGGREGATED).isEmpty()) {
            details.put(Constants.ISDISAGGREGATED, "false");
        }

        OntapStorage ontapStorage = new OntapStorage(details.get(Constants.USERNAME), details.get(Constants.PASSWORD),
                details.get(Constants.MANAGEMENTLIF), details.get(Constants.SVMNAME), details.get(Constants.PROTOCOL),
                Boolean.parseBoolean(details.get(Constants.ISDISAGGREGATED)));
        StorageProviderFactory storageProviderManager = new StorageProviderFactory(ontapStorage);
        StorageStrategy storageStrategy = storageProviderManager.getStrategy();
        boolean isValid = storageStrategy.connect();
        if (isValid) {
//            String volumeName = storagePoolName + "_vol"; //TODO: Figure out a better naming convention
            storageStrategy.createVolume(storagePoolName, Long.parseLong((details.get("size")))); // TODO: size should be in bytes, so see if conversion is needed
        } else {
            throw new CloudRuntimeException("ONTAP details validation failed, cannot create primary storage");
        }

        parameters.setTags(tags);
        parameters.setIsTagARule(isTagARule);
        parameters.setDetails(details);
        parameters.setUuid(UUID.randomUUID().toString());
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setClusterId(clusterId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(true);

        return _dataStoreHelper.createPrimaryDataStore(parameters);
    }

    @Override
    public boolean attachCluster(DataStore dataStore, ClusterScope scope) {
        logger.debug("In attachCluster for ONTAP primary storage");
        PrimaryDataStoreInfo primarystore = (PrimaryDataStoreInfo)dataStore;
        List<HostVO> hostsToConnect = _resourceMgr.getEligibleUpAndEnabledHostsInClusterForStorageConnection(primarystore);

        logger.debug(String.format("Attaching the pool to each of the hosts %s in the cluster: %s", hostsToConnect, primarystore.getClusterId()));
        for (HostVO host : hostsToConnect) {
            // TODO: Fetch the host IQN and add to the initiator group on ONTAP cluster
            try {
                _storageMgr.connectHostToSharedPool(host, dataStore.getId());
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
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
        List<HostVO> hostsToConnect = _resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, scope.getScopeId(), Hypervisor.HypervisorType.KVM);

        logger.debug(String.format("In createPool. Attaching the pool to each of the hosts in %s.", hostsToConnect));
        for (HostVO host : hostsToConnect) {
            // TODO: Fetch the host IQN and add to the initiator group on ONTAP cluster
            try {
                _storageMgr.connectHostToSharedPool(host, dataStore.getId());
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }
        _dataStoreHelper.attachZone(dataStore);
        return true;
    }

    @Override
    public boolean maintain(DataStore store) {
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        return true;
    }

    @Override
    public boolean deleteDataStore(DataStore store) {
        return true;
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

    }

    @Override
    public void disableStoragePool(DataStore store) {

    }

    @Override
    public void changeStoragePoolScopeToZone(DataStore store, ClusterScope clusterScope, Hypervisor.HypervisorType hypervisorType) {

    }

    @Override
    public void changeStoragePoolScopeToCluster(DataStore store, ClusterScope clusterScope, Hypervisor.HypervisorType hypervisorType) {

    }
}
