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
package com.cloud.upgrade.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.util.List;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDaoImpl;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41700to41710 extends DbUpgradeAbstractImpl implements DbUpgradeSystemVmTemplate {

    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    private PrimaryDataStoreDao storageDao;
    private VolumeDao volumeDao;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.17.0.0", "4.17.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.17.1.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41700to41710.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateStorPoolStorageType();
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41700to41710-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    private void initSystemVmTemplateRegistration() {
        systemVmTemplateRegistration = new SystemVmTemplateRegistration("");
    }

    @Override
    public void updateSystemVmTemplates(Connection conn) {
        logger.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    protected PrimaryDataStoreDao getStorageDao() {
        if (storageDao == null) {
            storageDao = new PrimaryDataStoreDaoImpl();
        }
        return storageDao;
    }

    protected VolumeDao getVolumeDao() {
        if (volumeDao == null) {
            volumeDao = new VolumeDaoImpl();
        }
        return volumeDao;
    }

    /*
    GenericDao.customSearch using GenericSearchBuilder and GenericDao.update using
    GenericDao.createSearchBuilder used here to prevent any future issues when new fields
    are added to StoragePoolVO or VolumeVO and this upgrade path starts to fail.
     */
    protected void updateStorPoolStorageType() {
        StoragePoolVO pool = getStorageDao().createForUpdate();
        pool.setPoolType(StoragePoolType.StorPool);
        SearchBuilder<StoragePoolVO> sb = getStorageDao().createSearchBuilder();
        sb.and("provider", sb.entity().getStorageProviderName(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getPoolType(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<StoragePoolVO> sc = sb.create();
        sc.setParameters("provider", StoragePoolType.StorPool.name());
        sc.setParameters("type", StoragePoolType.SharedMountPoint.name());
        getStorageDao().update(pool, sc);

        GenericSearchBuilder<StoragePoolVO, Long> gSb = getStorageDao().createSearchBuilder(Long.class);
        gSb.selectFields(gSb.entity().getId());
        gSb.and("provider", gSb.entity().getStorageProviderName(), SearchCriteria.Op.EQ);
        gSb.done();
        SearchCriteria<Long> gSc = gSb.create();
        gSc.setParameters("provider", StoragePoolType.StorPool.name());
        List<Long> poolIds = getStorageDao().customSearch(gSc, null);
        updateStorageTypeForStorPoolVolumes(poolIds);
    }

    protected void updateStorageTypeForStorPoolVolumes(List<Long> storagePoolIds) {
        if (CollectionUtils.isEmpty(storagePoolIds)) {
            return;
        }
        VolumeVO volume = getVolumeDao().createForUpdate();
        volume.setPoolType(StoragePoolType.StorPool);
        SearchBuilder<VolumeVO> sb = getVolumeDao().createSearchBuilder();
        sb.and("poolId", sb.entity().getPoolId(), SearchCriteria.Op.IN);
        sb.done();
        SearchCriteria<VolumeVO> sc = sb.create();
        sc.setParameters("poolId", storagePoolIds.toArray());
        getVolumeDao().update(volume, sc);
    }
}
