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
import java.util.LinkedHashMap;
import java.util.Map;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42210to42220 extends DbUpgradeAbstractImpl implements DbUpgrade {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.22.1.0", "4.22.2.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.22.2.0";
    }

    @Override
    public void performDataMigration(Connection conn) {
        encryptExistingTwoFactorAuthenticationKeys(conn);
    }

    /**
     * The {@code user.key_for_2fa} column was previously stored in plaintext. It is now annotated
     * with {@code @Encrypt}, so existing plaintext secrets must be encrypted in place. This is
     * idempotent: a value that already decrypts cleanly is left untouched.
     */
    protected void encryptExistingTwoFactorAuthenticationKeys(Connection conn) {
        Map<Long, String> keysToEncrypt = new LinkedHashMap<>();
        try (PreparedStatement selectStmt = conn.prepareStatement("SELECT id, key_for_2fa FROM cloud.user WHERE key_for_2fa IS NOT NULL AND key_for_2fa <> ''");
             ResultSet rs = selectStmt.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (isAlreadyEncrypted(value)) {
                    continue;
                }
                keysToEncrypt.put(id, value);
            }
        } catch (Exception e) {
            String message = String.format("Unable to read user.key_for_2fa values for encryption migration due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }

        if (keysToEncrypt.isEmpty()) {
            logger.debug("No plaintext user.key_for_2fa values found to encrypt.");
            return;
        }

        logger.info(String.format("Encrypting %d existing plaintext user.key_for_2fa value(s).", keysToEncrypt.size()));
        String updateSql = "UPDATE cloud.user SET key_for_2fa = ? WHERE id = ?";
        for (Map.Entry<Long, String> entry : keysToEncrypt.entrySet()) {
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, DBEncryptionUtil.encrypt(entry.getValue()));
                updateStmt.setLong(2, entry.getKey());
                updateStmt.executeUpdate();
            } catch (Exception e) {
                String message = String.format("Unable to encrypt user.key_for_2fa for user ID [%s] due to [%s].", entry.getKey(), e.getMessage());
                logger.error(message, e);
                throw new CloudRuntimeException(message, e);
            }
        }
    }

    private boolean isAlreadyEncrypted(String value) {
        try {
            DBEncryptionUtil.decrypt(value);
            return true;
        } catch (EncryptionOperationNotPossibleException e) {
            return false;
        }
    }
}
