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
import org.apache.log4j.Logger;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41700to41710 implements DbUpgrade, DbUpgradeSystemVmTemplate {

    final static Logger LOG = Logger.getLogger(Upgrade41610to41700.class);
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
        LOG.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    private void updateStorPoolStorageType() {
        storageDao = new PrimaryDataStoreDaoImpl();
        List<StoragePoolVO> storPoolPools = storageDao.findPoolsByProvider("StorPool");
        for (StoragePoolVO storagePoolVO : storPoolPools) {
            if (StoragePoolType.SharedMountPoint == storagePoolVO.getPoolType()) {
                storagePoolVO.setPoolType(StoragePoolType.StorPool);
                storageDao.update(storagePoolVO.getId(), storagePoolVO);
            }
            updateStorageTypeForStorPoolVolumes(storagePoolVO.getId());
        }
    }

    private void updateStorageTypeForStorPoolVolumes(long storagePoolId) {
        volumeDao = new VolumeDaoImpl();
        List<VolumeVO> volumes = volumeDao.findByPoolId(storagePoolId, null);
        for (VolumeVO volumeVO : volumes) {
            volumeVO.setPoolType(StoragePoolType.StorPool);
            volumeDao.update(volumeVO.getId(), volumeVO);
        }
    }
}
