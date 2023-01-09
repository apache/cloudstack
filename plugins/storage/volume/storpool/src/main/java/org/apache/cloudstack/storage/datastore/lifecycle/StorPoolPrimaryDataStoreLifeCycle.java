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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpApiResponse;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpConnectionDesc;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.log4j.Logger;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.ScopeType;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class StorPoolPrimaryDataStoreLifeCycle implements PrimaryDataStoreLifeCycle {
    private static final Logger log = Logger.getLogger(StorPoolPrimaryDataStoreLifeCycle.class);

    @Inject
    protected PrimaryDataStoreHelper dataStoreHelper;
    @Inject
    protected StoragePoolAutomation storagePoolAutmation;
    @Inject
    private PrimaryDataStoreDao _primaryDataStoreDao;
    @Inject
    private ResourceManager resourceMgr;
    @Inject
    private StorageManager storageMgr;
    @Inject
    private SnapshotDao snapshotDao;
    @Inject
    private SnapshotDetailsDao snapshotDetailsDao;
    @Inject
    private VMTemplatePoolDao vmTemplatePoolDao;
    @Inject
    private VMTemplateDetailsDao vmTemplateDetailsDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        StorPoolUtil.spLog("initialize:");
        for (Map.Entry<String, Object> e: dsInfos.entrySet()) {
            StorPoolUtil.spLog("    %s=%s", e.getKey(), e.getValue());
        }
        StorPoolUtil.spLog("");

        log.debug("initialize");

        String name = (String)dsInfos.get("name");
        String providerName = (String)dsInfos.get("providerName");
        Long zoneId = (Long)dsInfos.get("zoneId");

        String url = (String)dsInfos.get("url");
        SpConnectionDesc conn = new SpConnectionDesc(url);
        if (conn.getHostPort() == null)
            throw new IllegalArgumentException("No SP_API_HTTP");

        if (conn.getAuthToken() == null)
            throw new IllegalArgumentException("No SP_AUTH_TOKEN");

        if (conn.getTemplateName() == null)
            throw new IllegalArgumentException("No SP_TEMPLATE");

        if (!StorPoolUtil.templateExists(conn)) {
            throw new IllegalArgumentException("No such storpool template " + conn.getTemplateName() + " or credentials are invalid");
        }

        for (StoragePoolVO sp : _primaryDataStoreDao.findPoolsByProvider("StorPool")) {
            List<StoragePoolDetailVO> spDetails = storagePoolDetailsDao.listDetails(sp.getId());
            String host = null;
            String template = null;
            String authToken = null;
            SpConnectionDesc old = null;
            for (StoragePoolDetailVO storagePoolDetailVO : spDetails) {
                switch (storagePoolDetailVO.getName()) {
                case StorPoolUtil.SP_AUTH_TOKEN:
                    authToken = storagePoolDetailVO.getValue();
                    break;
                case StorPoolUtil.SP_HOST_PORT:
                    host = storagePoolDetailVO.getValue();
                    break;
                case StorPoolUtil.SP_TEMPLATE:
                    template = storagePoolDetailVO.getValue();
                    break;
                default:
                    break;
                }
            }
            if (host != null && template != null && authToken != null) {
                old = new SpConnectionDesc(host, authToken, template);
            } else {
                old = new SpConnectionDesc(sp.getUuid());
            }
            if( old.getHostPort().equals(conn.getHostPort()) && old.getTemplateName().equals(conn.getTemplateName()) )
                throw new IllegalArgumentException("StorPool cluster and template already in use by pool " + sp.getName());
        }

        Long capacityBytes = (Long)dsInfos.get("capacityBytes");
        if (capacityBytes == null) {
            throw new IllegalArgumentException("Capcity bytes is required");
        }

        String tags = (String)dsInfos.get("tags");
        if (tags == null || tags.isEmpty()) {
            tags = name;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>)dsInfos.get("details");
        details.put(StorPoolUtil.SP_AUTH_TOKEN, conn.getAuthToken());
        details.put(StorPoolUtil.SP_HOST_PORT, conn.getHostPort());
        details.put(StorPoolUtil.SP_TEMPLATE, conn.getTemplateName());

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();
        parameters.setName(name);
        parameters.setUuid(conn.getTemplateName() + ";" + UUID.randomUUID().toString());
        parameters.setZoneId(zoneId);
        parameters.setProviderName(providerName);
        parameters.setType(StoragePoolType.StorPool);
        parameters.setHypervisorType(HypervisorType.KVM);
        parameters.setManaged(false);
        parameters.setHost("n/a");
        parameters.setPort(0);
        parameters.setPath(StorPoolUtil.SP_DEV_PATH);
        parameters.setUsedBytes(0);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setTags(tags);
        parameters.setDetails(details);

        return dataStoreHelper.createPrimaryDataStore(parameters);
    }

    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> details) {
        StorPoolUtil.spLog("updateStoragePool:");
        for (Map.Entry<String, String> e: details.entrySet()) {
            StorPoolUtil.spLog("    %s=%s", e.getKey(), e.getValue());
        }
        StorPoolUtil.spLog("");

        log.debug("updateStoragePool");
        return;
    }
    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        log.debug("attachHost");
        return true;
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        log.debug("attachCluster");
        if (!scope.getScopeType().equals(ScopeType.ZONE)) {
            throw new UnsupportedOperationException("Only Zone-Wide scope is supported!");
        }
        return true;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        log.debug("attachZone");

        if (hypervisorType != HypervisorType.KVM) {
            throw new UnsupportedOperationException("Only KVM hypervisors supported!");
        }
        List<HostVO> kvmHosts = resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.KVM, scope.getScopeId());
        for (HostVO host : kvmHosts) {
            try {
                storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
            } catch (Exception e) {
                log.warn(String.format("Unable to establish a connection between host %s and pool %s due to %s", host, dataStore, e));
            }
        }
        dataStoreHelper.attachZone(dataStore, hypervisorType);
        return true;
    }

    @Override
    public boolean maintain(DataStore dataStore) {
        log.debug("maintain");

        storagePoolAutmation.maintain(dataStore);
        dataStoreHelper.maintain(dataStore);
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        log.debug("cancelMaintain");

        dataStoreHelper.cancelMaintain(store);
        storagePoolAutmation.cancelMaintain(store);
        return true;
    }

    @Override
    public boolean deleteDataStore(DataStore store) {
        log.debug("deleteDataStore");
        long storagePoolId = store.getId();

        List<SnapshotVO> lstSnapshots = snapshotDao.listAll();

        if (lstSnapshots != null) {
            for (SnapshotVO snapshot : lstSnapshots) {
                SnapshotDetailsVO snapshotDetails = snapshotDetailsDao.findDetail(snapshot.getId(), StorPoolUtil.SP_STORAGE_POOL_ID);

                // if this snapshot belongs to the storagePool that was passed in
                if (snapshotDetails != null && snapshotDetails.getValue() != null && Long.parseLong(snapshotDetails.getValue()) == storagePoolId) {
                    throw new CloudRuntimeException("This primary storage cannot be deleted because it currently contains one or more snapshots.");
                }
            }
        }

        List<VMTemplateDetailVO> lstTemplateDetails = vmTemplateDetailsDao.listAll();

        if (lstTemplateDetails != null) {
            for (VMTemplateDetailVO vmTemplateDetailVO : lstTemplateDetails) {
                if (vmTemplateDetailVO.getName().equals(StorPoolUtil.SP_STORAGE_POOL_ID) && Long.parseLong(vmTemplateDetailVO.getValue()) == storagePoolId) {
                    throw new CloudRuntimeException("This primary storage cannot be deleted because it currently contains one or more template snapshots.");
                }
            }
        }

        List<VMTemplateStoragePoolVO> lstTemplatePoolRefs = vmTemplatePoolDao.listByPoolId(storagePoolId);

        SpConnectionDesc conn = null;
        try {
            conn = StorPoolUtil.getSpConnection(store.getUuid(), store.getId(), storagePoolDetailsDao, _primaryDataStoreDao);
        } catch (CloudRuntimeException e) {
            throw e;
        }

        if (lstTemplatePoolRefs != null) {
            for (VMTemplateStoragePoolVO templatePoolRef : lstTemplatePoolRefs) {
                SpApiResponse resp = StorPoolUtil.snapshotDelete(
                        StorPoolStorageAdaptor.getVolumeNameFromPath(templatePoolRef.getLocalDownloadPath(), true), conn);
                if (resp.getError() != null) {
                    throw new CloudRuntimeException(String.format("Could not delete StorPool's snapshot from template_spool_ref table due to %s", resp.getError()));
                }
                vmTemplatePoolDao.remove(templatePoolRef.getId());
            }
        }
        boolean isDeleted = dataStoreHelper.deletePrimaryDataStore(store);
        if (isDeleted) {
            List<StoragePoolDetailVO> volumesOnHosts = storagePoolDetailsDao.listDetails(storagePoolId);
            for (StoragePoolDetailVO storagePoolDetailVO : volumesOnHosts) {
                if (storagePoolDetailVO.getValue() != null && storagePoolDetailVO.getName().contains(StorPoolUtil.SP_VOLUME_ON_CLUSTER)) {
                    StorPoolUtil.volumeDelete(StorPoolStorageAdaptor.getVolumeNameFromPath(storagePoolDetailVO.getValue(), true), conn);
                }
            }
            storagePoolDetailsDao.removeDetails(storagePoolId);
        }
        return isDeleted;
    }

    @Override
    public boolean migrateToObjectStore(DataStore store) {
        log.debug("migrateToObjectStore");
        return false;
    }

    @Override
    public void enableStoragePool(DataStore dataStore) {
        log.debug("enableStoragePool");
        dataStoreHelper.enable(dataStore);
    }

    @Override
    public void disableStoragePool(DataStore dataStore) {
        log.debug("disableStoragePool");
        dataStoreHelper.disable(dataStore);
    }
}
