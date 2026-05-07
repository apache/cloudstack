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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDaoImpl;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42210to42300 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

    private PrimaryDataStoreDao storageDao;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.22.1.0", "4.23.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.23.0.0";
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42210to42300.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        unhideJsInterpretationEnabled(conn);
        normalizeStorPoolPrimaryStorageUuids();
    }

    protected PrimaryDataStoreDao getStorageDao() {
        if (storageDao == null) {
            storageDao = new PrimaryDataStoreDaoImpl();
        }
        return storageDao;
    }

    /**
     * StorPool primary storage used {@code templateName + ";" + uuid} as {@code storage_pool.uuid}.
     * Normalize to a plain UUID form so API and validation treat {@code id} like other pools.
     * Template name remains in {@code storage_pool_details} ({@code SP_TEMPLATE}).
     */
    protected void normalizeStorPoolPrimaryStorageUuids() {
        SearchBuilder<StoragePoolVO> sb = getStorageDao().createSearchBuilder();
        sb.and("poolType", sb.entity().getPoolType(), SearchCriteria.Op.EQ);
        sb.and("uuid", sb.entity().getUuid(), SearchCriteria.Op.LIKE);
        sb.done();
        SearchCriteria<StoragePoolVO> sc = sb.create();
        sc.setParameters("poolType", StoragePoolType.StorPool);
        sc.setParameters("uuid", "%;%");
        List<StoragePoolVO> pools = getStorageDao().search(sc, null);
        int updated = 0;
        for (StoragePoolVO pool : pools) {
            final String templatePrefixedPoolUuid = pool.getUuid();
            if (templatePrefixedPoolUuid == null) {
                continue;
            }
            final String[] parts = templatePrefixedPoolUuid.split(";");
            if (parts.length < 2) {
                continue;
            }
            final String realUuid = parts[1].trim();
            try {
                UUID.fromString(realUuid);
            } catch (IllegalArgumentException e) {
                logger.warn(
                        "Skipping StorPool storage pool id [{}]: value after ';' is not a valid UUID: [{}]",
                        pool.getId(), realUuid);
                continue;
            }
            pool.setUuid(realUuid);
            getStorageDao().update(pool.getId(), pool);
            updated++;
        }
        if (updated > 0) {
            logger.info("Normalized {} StorPool primary storage pool UUID(s) to plain UUID form.", updated);
        }
    }

    protected void unhideJsInterpretationEnabled(Connection conn) {
        String value = getJsInterpretationEnabled(conn);
        if (value != null) {
            updateJsInterpretationEnabledFields(conn, value);
        }
    }

    protected String getJsInterpretationEnabled(Connection conn) {
        String query = "SELECT value FROM cloud.configuration WHERE name = 'js.interpretation.enabled' AND category = 'Hidden';";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
            logger.debug("Unable to retrieve value of hidden configuration 'js.interpretation.enabled'. The configuration may already be unhidden.");
            return null;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while retrieving value of hidden configuration 'js.interpretation.enabled'.", e);
        }
    }

    protected void updateJsInterpretationEnabledFields(Connection conn, String encryptedValue) {
        String query = "UPDATE cloud.configuration SET value = ?, category = 'System', component = 'JsInterpreter', is_dynamic = 1 WHERE name = 'js.interpretation.enabled';";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            String decryptedValue = DBEncryptionUtil.decrypt(encryptedValue);
            logger.info("Updating setting 'js.interpretation.enabled' to decrypted value [{}], category 'System', component 'JsInterpreter', and is_dynamic '1'.", decryptedValue);
            pstmt.setString(1, decryptedValue);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while unhiding configuration 'js.interpretation.enabled'.", e);
        } catch (CloudRuntimeException e) {
            logger.warn("Error while decrypting configuration 'js.interpretation.enabled'. The configuration may already be decrypted.");
        }
    }
}
