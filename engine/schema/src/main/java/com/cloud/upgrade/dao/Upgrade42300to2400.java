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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42300to2400 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

    // A removed KMS wrapped key that a volume (not expunged) or a snapshot (not destroyed) still needs
    private static final String REMOVED_KMS_WRAPPED_KEY_IN_USE = "w.removed IS NOT NULL AND ("
            + "EXISTS (SELECT 1 FROM `cloud`.`volumes` v WHERE v.kms_wrapped_key_id = w.id"
            + " AND v.removed IS NULL AND v.state <> 'Expunged')"
            + " OR EXISTS (SELECT 1 FROM `cloud`.`snapshots` s JOIN `cloud`.`volumes` v ON v.id = s.volume_id"
            + " WHERE v.kms_wrapped_key_id = w.id AND s.removed IS NULL AND s.status <> 'Destroyed'))";
    // The KMS key and the KEK the key was wrapped with still exist. Deleting a KMS key deletes its KEKs but leaves
    // its version rows as they are.
    private static final String KEK_VERSION_USABLE = "EXISTS (SELECT 1 FROM `cloud`.`kms_kek_versions` k"
            + " JOIN `cloud`.`kms_keys` kk ON kk.id = k.kms_key_id"
            + " WHERE k.id = w.kek_version_id AND k.removed IS NULL AND k.status <> 'Archived' AND kk.removed IS NULL)";

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.23.0.0", "24.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "24.0.0";
    }

    @Override
    public void performDataMigration(Connection conn) {
        restoreKmsWrappedKeysStillInUse(conn);
    }

    /**
     * In 4.23 removing a volume also removed its KMS wrapped key, even when the key was still needed by the volume
     * it was migrated to or by snapshots of the volume. Restores such keys while the KEK they were wrapped with still
     * exists, and logs the ones that can't be recovered anymore.
     */
    private void restoreKmsWrappedKeysStillInUse(Connection conn) {
        String lostKeysQuery = "SELECT w.uuid, v.uuid FROM `cloud`.`kms_wrapped_key` w"
                + " JOIN `cloud`.`volumes` v ON v.kms_wrapped_key_id = w.id"
                + " WHERE " + REMOVED_KMS_WRAPPED_KEY_IN_USE + " AND NOT " + KEK_VERSION_USABLE;
        try (PreparedStatement pstmt = conn.prepareStatement(lostKeysQuery); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                logger.warn("KMS wrapped key [{}] of volume [{}] was removed while still in use and the KEK it was "
                        + "wrapped with is gone; the volume and its snapshots can't be decrypted anymore",
                        rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to look up KMS wrapped keys that were removed while still in use", e);
        }

        String restoreQuery = "UPDATE `cloud`.`kms_wrapped_key` w SET w.removed = NULL"
                + " WHERE " + REMOVED_KMS_WRAPPED_KEY_IN_USE + " AND " + KEK_VERSION_USABLE;
        try (PreparedStatement pstmt = conn.prepareStatement(restoreQuery)) {
            int restored = pstmt.executeUpdate();
            if (restored > 0) {
                logger.info("Restored {} KMS wrapped key(s) that were removed while still in use", restored);
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to restore KMS wrapped keys that were removed while still in use", e);
        }
    }
}
