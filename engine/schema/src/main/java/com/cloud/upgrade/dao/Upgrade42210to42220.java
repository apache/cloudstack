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
import java.util.regex.Pattern;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.crypt.EncryptionSecretKeyChecker;
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

    // A stored TOTP secret is Base32 (RFC 4648 alphabet A-Z, 2-7, optional '=' padding). An
    // encrypted value is Base64 and therefore contains characters outside this set (lowercase,
    // 0/1/8/9, '+', '/'). This shape test distinguishes a not-yet-encrypted secret from one that
    // is already encrypted, without relying on decrypt() throwing a particular exception type.
    private static final Pattern BASE32_SECRET = Pattern.compile("^[A-Z2-7]+=*$");

    /**
     * The {@code user.key_for_2fa} column was previously stored in plaintext. It is now annotated
     * with {@code @Encrypt}, so {@link DBEncryptionUtil} decrypts it on every read — meaning any
     * row left in plaintext would fail to decrypt and lock the user out of 2FA. This migration
     * encrypts existing plaintext secrets in place.
     *
     * It is a no-op when DB encryption is disabled (then {@code @Encrypt} reads return the value
     * unchanged, so nothing needs encrypting), and idempotent when enabled (values already in
     * encrypted, non-Base32 form are skipped), so re-running after a partial failure is safe.
     */
    protected void encryptExistingTwoFactorAuthenticationKeys(Connection conn) {
        if (!EncryptionSecretKeyChecker.useEncryption()) {
            logger.debug("DB encryption is disabled; user.key_for_2fa is stored as-is, nothing to migrate.");
            return;
        }

        Map<Long, String> keysToEncrypt = new LinkedHashMap<>();
        try (PreparedStatement selectStmt = conn.prepareStatement("SELECT id, key_for_2fa FROM cloud.user WHERE key_for_2fa IS NOT NULL AND key_for_2fa <> ''");
             ResultSet rs = selectStmt.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (!isPlaintextSecret(value)) {
                    // Already encrypted (or not a recognizable plaintext secret) — leave it alone.
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

    /**
     * True if the value looks like a plaintext Base32 TOTP secret (and therefore still needs
     * encrypting). Encrypted (Base64) values contain characters outside the Base32 alphabet.
     */
    protected boolean isPlaintextSecret(String value) {
        return value != null && BASE32_SECRET.matcher(value).matches();
    }
}
