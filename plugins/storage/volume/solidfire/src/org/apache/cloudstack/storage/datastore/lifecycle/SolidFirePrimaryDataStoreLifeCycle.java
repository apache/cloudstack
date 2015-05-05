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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.utils.exception.CloudRuntimeException;

public class SolidFirePrimaryDataStoreLifeCycle implements PrimaryDataStoreLifeCycle {
    private static final Logger s_logger = Logger.getLogger(SolidFirePrimaryDataStoreLifeCycle.class);

    @Inject private CapacityManager _capacityMgr;
    @Inject private DataCenterDao zoneDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private PrimaryDataStoreHelper dataStoreHelper;
    @Inject private ResourceManager _resourceMgr;
    @Inject private SnapshotDao _snapshotDao;
    @Inject private SnapshotDetailsDao _snapshotDetailsDao;
    @Inject private StorageManager _storageMgr;
    @Inject private StoragePoolAutomation storagePoolAutomation;

    // invoked to add primary storage that is based on the SolidFire plug-in
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        String url = (String)dsInfos.get("url");
        Long zoneId = (Long)dsInfos.get("zoneId");
        String storagePoolName = (String)dsInfos.get("name");
        String providerName = (String)dsInfos.get("providerName");
        Long capacityBytes = (Long)dsInfos.get("capacityBytes");
        Long capacityIops = (Long)dsInfos.get("capacityIops");
        String tags = (String)dsInfos.get("tags");
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>)dsInfos.get("details");

        String storageVip = SolidFireUtil.getStorageVip(url);
        int storagePort = SolidFireUtil.getStoragePort(url);

        DataCenterVO zone = zoneDao.findById(zoneId);

        String uuid = SolidFireUtil.PROVIDER_NAME + "_" + zone.getUuid() + "_" + storageVip;

        if (capacityBytes == null || capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops == null || capacityIops <= 0) {
            throw new IllegalArgumentException("'capacityIops' must be present and greater than 0.");
        }

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        parameters.setHost(storageVip);
        parameters.setPort(storagePort);
        parameters.setPath(SolidFireUtil.getModifiedUrl(url));
        parameters.setType(StoragePoolType.Iscsi);
        parameters.setUuid(uuid);
        parameters.setZoneId(zoneId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(true);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);
        parameters.setCapacityIops(capacityIops);
        parameters.setHypervisorType(HypervisorType.Any);
        parameters.setTags(tags);
        parameters.setDetails(details);

        String managementVip = SolidFireUtil.getManagementVip(url);
        int managementPort = SolidFireUtil.getManagementPort(url);

        details.put(SolidFireUtil.MANAGEMENT_VIP, managementVip);
        details.put(SolidFireUtil.MANAGEMENT_PORT, String.valueOf(managementPort));

        String clusterAdminUsername = SolidFireUtil.getValue(SolidFireUtil.CLUSTER_ADMIN_USERNAME, url);
        String clusterAdminPassword = SolidFireUtil.getValue(SolidFireUtil.CLUSTER_ADMIN_PASSWORD, url);

        details.put(SolidFireUtil.CLUSTER_ADMIN_USERNAME, clusterAdminUsername);
        details.put(SolidFireUtil.CLUSTER_ADMIN_PASSWORD, clusterAdminPassword);

        long lClusterDefaultMinIops = 100;
        long lClusterDefaultMaxIops = 15000;
        float fClusterDefaultBurstIopsPercentOfMaxIops = 1.5f;

        try {
            String clusterDefaultMinIops = SolidFireUtil.getValue(SolidFireUtil.CLUSTER_DEFAULT_MIN_IOPS, url);

            if (clusterDefaultMinIops != null && clusterDefaultMinIops.trim().length() > 0) {
                lClusterDefaultMinIops = Long.parseLong(clusterDefaultMinIops);
            }
        } catch (NumberFormatException ex) {
            s_logger.warn("Cannot parse the setting of " + SolidFireUtil.CLUSTER_DEFAULT_MIN_IOPS +
                          ", using default value: " + lClusterDefaultMinIops +
                          ". Exception: " + ex);
        }

        try {
            String clusterDefaultMaxIops = SolidFireUtil.getValue(SolidFireUtil.CLUSTER_DEFAULT_MAX_IOPS, url);

            if (clusterDefaultMaxIops != null && clusterDefaultMaxIops.trim().length() > 0) {
                lClusterDefaultMaxIops = Long.parseLong(clusterDefaultMaxIops);
            }
        } catch (NumberFormatException ex) {
            s_logger.warn("Cannot parse the setting of " + SolidFireUtil.CLUSTER_DEFAULT_MAX_IOPS +
                          ", using default value: " + lClusterDefaultMaxIops +
                          ". Exception: " + ex);
        }

        try {
            String clusterDefaultBurstIopsPercentOfMaxIops = SolidFireUtil.getValue(SolidFireUtil.CLUSTER_DEFAULT_BURST_IOPS_PERCENT_OF_MAX_IOPS, url);

            if (clusterDefaultBurstIopsPercentOfMaxIops != null && clusterDefaultBurstIopsPercentOfMaxIops.trim().length() > 0) {
                fClusterDefaultBurstIopsPercentOfMaxIops = Float.parseFloat(clusterDefaultBurstIopsPercentOfMaxIops);
            }
        } catch (NumberFormatException ex) {
            s_logger.warn("Cannot parse the setting of " + SolidFireUtil.CLUSTER_DEFAULT_BURST_IOPS_PERCENT_OF_MAX_IOPS +
                          ", using default value: " + fClusterDefaultBurstIopsPercentOfMaxIops +
                          ". Exception: " + ex);
        }

        if (lClusterDefaultMinIops > lClusterDefaultMaxIops) {
            throw new CloudRuntimeException("The parameter '" + SolidFireUtil.CLUSTER_DEFAULT_MIN_IOPS + "' must be less than or equal to the parameter '" +
                SolidFireUtil.CLUSTER_DEFAULT_MAX_IOPS + "'.");
        }

        if (Float.compare(fClusterDefaultBurstIopsPercentOfMaxIops, 1.0f) < 0) {
            throw new CloudRuntimeException("The parameter '" + SolidFireUtil.CLUSTER_DEFAULT_BURST_IOPS_PERCENT_OF_MAX_IOPS + "' must be greater than or equal to 1.");
        }

        details.put(SolidFireUtil.CLUSTER_DEFAULT_MIN_IOPS, String.valueOf(lClusterDefaultMinIops));
        details.put(SolidFireUtil.CLUSTER_DEFAULT_MAX_IOPS, String.valueOf(lClusterDefaultMaxIops));
        details.put(SolidFireUtil.CLUSTER_DEFAULT_BURST_IOPS_PERCENT_OF_MAX_IOPS, String.valueOf(fClusterDefaultBurstIopsPercentOfMaxIops));

        // this adds a row in the cloud.storage_pool table for this SolidFire cluster
        return dataStoreHelper.createPrimaryDataStore(parameters);
    }

    // do not implement this method for SolidFire's plug-in
    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return true; // should be ignored for zone-wide-only plug-ins like SolidFire's
    }

    // do not implement this method for SolidFire's plug-in
    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        return true; // should be ignored for zone-wide-only plug-ins like SolidFire's
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        dataStoreHelper.attachZone(dataStore);

        List<HostVO> xenServerHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.XenServer, scope.getScopeId());
        List<HostVO> vmWareServerHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.VMware, scope.getScopeId());
        List<HostVO> kvmHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.KVM, scope.getScopeId());
        List<HostVO> hosts = new ArrayList<HostVO>();

        hosts.addAll(xenServerHosts);
        hosts.addAll(vmWareServerHosts);
        hosts.addAll(kvmHosts);

        for (HostVO host : hosts) {
            try {
                _storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
            } catch (Exception e) {
                s_logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }

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

    // invoked to delete primary storage that is based on the SolidFire plug-in
    @Override
    public boolean deleteDataStore(DataStore store) {
        List<SnapshotVO> lstSnapshots = _snapshotDao.listAll();

        if (lstSnapshots != null) {
            for (SnapshotVO snapshot : lstSnapshots) {
                SnapshotDetailsVO snapshotDetails = _snapshotDetailsDao.findDetail(snapshot.getId(), SolidFireUtil.STORAGE_POOL_ID);

                // if this snapshot belongs to the storagePool that was passed in
                if (snapshotDetails != null && snapshotDetails.getValue() != null && Long.parseLong(snapshotDetails.getValue()) == store.getId()) {
                    throw new CloudRuntimeException("This primary storage cannot be deleted because it currently contains one or more snapshots.");
                }
            }
        }

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
        StoragePoolVO storagePoolVo = storagePoolDao.findById(storagePool.getId());

        String strCapacityBytes = details.get(PrimaryDataStoreLifeCycle.CAPACITY_BYTES);
        Long capacityBytes = strCapacityBytes != null ? Long.parseLong(strCapacityBytes) : null;

        if (capacityBytes != null) {
            long usedBytes = _capacityMgr.getUsedBytes(storagePoolVo);

            if (capacityBytes < usedBytes) {
                throw new CloudRuntimeException("Cannot reduce the number of bytes for this storage pool as it would lead to an insufficient number of bytes");
            }
        }

        String strCapacityIops = details.get(PrimaryDataStoreLifeCycle.CAPACITY_IOPS);
        Long capacityIops = strCapacityIops != null ? Long.parseLong(strCapacityIops) : null;

        if (capacityIops != null) {
            long usedIops = _capacityMgr.getUsedIops(storagePoolVo);

            if (capacityIops < usedIops) {
                throw new CloudRuntimeException("Cannot reduce the number of IOPS for this storage pool as it would lead to an insufficient number of IOPS");
            }
        }
    }

    @Override
    public void enableStoragePool(DataStore dataStore) {
        dataStoreHelper.enable(dataStore);
    }

    @Override
    public void disableStoragePool(DataStore dataStore) {
        dataStoreHelper.disable(dataStore);
    }
}
