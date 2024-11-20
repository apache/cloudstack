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
package org.apache.cloudstack.storage.image;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.direct.download.DirectDownloadManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class TemplateDataFactoryImpl implements TemplateDataFactory {
    protected Logger logger = LogManager.getLogger(getClass());
    @Inject
    VMTemplateDao imageDataDao;
    @Inject
    DataStoreManager storeMgr;
    @Inject
    VMTemplatePoolDao templatePoolDao;
    @Inject
    TemplateDataStoreDao templateStoreDao;
    @Inject
    DirectDownloadManager directDownloadManager;
    @Inject
    HostDao hostDao;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;

    @Override
    public TemplateInfo getTemplateOnPrimaryStorage(long templateId, DataStore store, String configuration) {
        VMTemplateVO templ = imageDataDao.findByIdIncludingRemoved(templateId);
        if (templ == null) {
            logger.error("Could not find a template with id " + templateId);
            return null;
        }
        if (store.getRole() == DataStoreRole.Primary) {
            VMTemplateStoragePoolVO templatePoolVO = templatePoolDao.findByPoolTemplate(store.getId(), templateId, configuration);
            if (templatePoolVO != null) {
                String deployAsIsConfiguration = templatePoolVO.getDeploymentOption();
                return TemplateObject.getTemplate(templ, store, deployAsIsConfiguration);
            }
        }
        return null;
    }

    @Override
    public TemplateInfo getTemplate(long templateId) {
        VMTemplateVO templ = imageDataDao.findById(templateId);
        if (templ != null) {
            TemplateObject tmpl = TemplateObject.getTemplate(templ, null, null);
            return tmpl;
        }
        return null;
    }

    @Override
    public TemplateInfo getTemplate(long templateId, DataStore store) {
        VMTemplateVO templ = imageDataDao.findById(templateId);
        if (templ == null) {
            return null;
        }
        if (store == null && !templ.isDirectDownload()) {
            TemplateObject tmpl = TemplateObject.getTemplate(templ, null, null);
            return tmpl;
        }
        // verify if the given input parameters are consistent with our db data.
        boolean found = false;
        if (store.getRole() == DataStoreRole.Primary) {
            VMTemplateStoragePoolVO templatePoolVO = templatePoolDao.findByPoolTemplate(store.getId(), templateId, null);
            if (templatePoolVO != null) {
                found = true;
            }
        } else {
            TemplateDataStoreVO templateStoreVO = templateStoreDao.findByStoreTemplate(store.getId(), templateId);
            if (templateStoreVO != null) {
                found = true;
            }
        }

        if (logger.isDebugEnabled()) {
            if (!found) {
                logger.debug("template " + templateId + " is not in store:" + store.getId() + ", type:" + store.getRole());
            } else {
                logger.debug("template " + templateId + " is already in store:" + store.getId() + ", type:" + store.getRole());
            }
        }

        TemplateObject tmpl = TemplateObject.getTemplate(templ, store, null);
        return tmpl;
    }

    @Override
    public TemplateInfo getTemplate(long templateId, DataStoreRole storeRole) {
        TemplateDataStoreVO tmplStore = templateStoreDao.findByTemplate(templateId, storeRole);
        DataStore store = null;
        if (tmplStore != null) {
            store = storeMgr.getDataStore(tmplStore.getDataStoreId(), storeRole);
        }
        return this.getTemplate(templateId, store);
    }

    @Override
    public TemplateInfo getTemplate(long templateId, DataStoreRole storeRole, Long zoneId) {
        TemplateDataStoreVO tmplStore = templateStoreDao.findByTemplateZone(templateId, zoneId, storeRole);
        DataStore store = null;
        if (tmplStore != null) {
            store = storeMgr.getDataStore(tmplStore.getDataStoreId(), storeRole);
        }
        return this.getTemplate(templateId, store);
    }

    @Override
    public TemplateInfo getReadyTemplateOnImageStore(long templateId, Long zoneId) {
        TemplateDataStoreVO tmplStore = templateStoreDao.findByTemplateZoneReady(templateId, zoneId);
        if (tmplStore != null) {
            DataStore store = storeMgr.getDataStore(tmplStore.getDataStoreId(), DataStoreRole.Image);
            return this.getTemplate(templateId, store);
        } else {
            return null;
        }
    }

    @Override
    public TemplateInfo getTemplate(DataObject obj, DataStore store, String configuration) {
        TemplateObject tmpObj;
        if (StringUtils.isNotBlank(configuration)) {
            tmpObj = (TemplateObject)this.getTemplateOnPrimaryStorage(obj.getId(), store, configuration);
        } else {
            tmpObj = (TemplateObject)this.getTemplate(obj.getId(), store);
        }
        // carry over url set in passed in data object, for copyTemplate case
        // where url is generated on demand and not persisted in DB.
        // need to think of a more generic way to pass these runtime information
        // carried through DataObject post 4.2
        TemplateObject origTmpl = (TemplateObject)obj;
        tmpObj.setUrl(origTmpl.getUrl());
        return tmpObj;
    }

    @Override
    public TemplateInfo getReadyTemplateOnCache(long templateId) {
        TemplateDataStoreVO tmplStore = templateStoreDao.findReadyOnCache(templateId);
        if (tmplStore != null) {
            DataStore store = storeMgr.getDataStore(tmplStore.getDataStoreId(), DataStoreRole.ImageCache);
            return getTemplate(templateId, store);
        } else {
            return null;
        }
    }

    @Override
    public List<TemplateInfo> listTemplateOnCache(long templateId) {
        List<TemplateDataStoreVO> cacheTmpls = templateStoreDao.listOnCache(templateId);
        List<TemplateInfo> tmplObjs = new ArrayList<TemplateInfo>();
        for (TemplateDataStoreVO cacheTmpl : cacheTmpls) {
            long storeId = cacheTmpl.getDataStoreId();
            DataStore store = storeMgr.getDataStore(storeId, DataStoreRole.ImageCache);
            TemplateInfo tmplObj = getTemplate(templateId, store);
            tmplObjs.add(tmplObj);
        }
        return tmplObjs;
    }

    /**
     * Given existing spool refs, return one pool id existing on pools and refs
     */
    private Long getOneMatchingPoolIdFromRefs(List<VMTemplateStoragePoolVO> existingRefs, List<StoragePoolVO> pools) {
        if (!existingRefs.isEmpty()) {
            for (VMTemplateStoragePoolVO ref : existingRefs) {
                for (StoragePoolVO p : pools) {
                    if (ref.getPoolId() == p.getId()) {
                        return p.getId();
                    }
                }
            }
        }
        return pools.get(0).getId();
    }

    /**
     * Retrieve storage pools with scope = cluster or zone or local matching clusterId or dataCenterId or hostId depending on their scope
     */
    private List<StoragePoolVO> getStoragePoolsForScope(long dataCenterId, Long clusterId, long hostId, Hypervisor.HypervisorType hypervisorType) {
        List<StoragePoolVO> pools = new ArrayList<>();
        if (clusterId != null) {
            List<StoragePoolVO> clusterPools = primaryDataStoreDao.listPoolsByCluster(clusterId);
            clusterPools = clusterPools.stream().filter(p -> !p.isLocal()).collect(Collectors.toList());
            pools.addAll(clusterPools);
        }
        List<StoragePoolVO> zonePools = primaryDataStoreDao.findZoneWideStoragePoolsByHypervisor(dataCenterId, hypervisorType);
        pools.addAll(zonePools);
        List<StoragePoolVO> localPools = primaryDataStoreDao.findLocalStoragePoolsByHostAndTags(hostId, null);
        pools.addAll(localPools);
        return pools;
    }

    protected Long getBypassedTemplateExistingOrNewPoolId(VMTemplateVO templateVO, Long hostId) {
        HostVO host = hostDao.findById(hostId);
        List<StoragePoolVO> pools = getStoragePoolsForScope(host.getDataCenterId(), host.getClusterId(), hostId, host.getHypervisorType());
        if (CollectionUtils.isEmpty(pools)) {
            throw new CloudRuntimeException(String.format("No storage pool found to download template: %s", templateVO.getName()));
        }
        List<VMTemplateStoragePoolVO> existingRefs = templatePoolDao.listByTemplateId(templateVO.getId());
        return getOneMatchingPoolIdFromRefs(existingRefs, pools);
    }

    @Override
    public TemplateInfo getReadyBypassedTemplateOnPrimaryStore(long templateId, Long poolId, Long hostId) {
        VMTemplateVO templateVO = imageDataDao.findById(templateId);
        if (templateVO == null || !templateVO.isDirectDownload()) {
            return null;
        }
        Long templatePoolId = poolId;
        if (poolId == null) {
            templatePoolId = getBypassedTemplateExistingOrNewPoolId(templateVO, hostId);
        }
        VMTemplateStoragePoolVO spoolRef = templatePoolDao.findByPoolTemplate(templatePoolId, templateId, null);
        if (spoolRef == null) {
            directDownloadManager.downloadTemplate(templateId, templatePoolId, hostId);
        }
        DataStore store = storeMgr.getDataStore(templatePoolId, DataStoreRole.Primary);
        return this.getTemplate(templateId, store);
    }

    @Override
    public TemplateInfo getReadyBypassedTemplateOnManagedStorage(long templateId, TemplateInfo templateOnPrimary, Long poolId, Long hostId) {
        VMTemplateVO templateVO = imageDataDao.findById(templateId);
        if (templateVO == null || !templateVO.isDirectDownload()) {
            return null;
        }

        if (poolId == null) {
            throw new CloudRuntimeException("No storage pool specified to download template: " + templateId);
        }

        StoragePoolVO poolVO = primaryDataStoreDao.findById(poolId);
        if (poolVO == null || !poolVO.isManaged()) {
            return null;
        }

        VMTemplateStoragePoolVO spoolRef = templatePoolDao.findByPoolTemplate(poolId, templateId, null);
        if (spoolRef == null) {
            throw new CloudRuntimeException("Template not created on managed storage pool: " + poolId + " to copy the download template: " + templateId);
        } else if (spoolRef.getDownloadState() == VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED) {
            directDownloadManager.downloadTemplate(templateId, poolId, hostId);
        }

        DataStore store = storeMgr.getDataStore(poolId, DataStoreRole.Primary);
        return this.getTemplate(templateId, store);
    }

    @Override
    public boolean isTemplateMarkedForDirectDownload(long templateId) {
        VMTemplateVO templateVO = imageDataDao.findById(templateId);
        return templateVO.isDirectDownload();
    }
}
