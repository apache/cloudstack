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

package org.apache.cloudstack.kms.provider;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.google.crypto.tink.subtle.AesGcmJce;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;
import org.apache.cloudstack.kms.HSMProfileVO;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.provider.database.KMSDatabaseKekObjectVO;
import org.apache.cloudstack.kms.provider.database.dao.KMSDatabaseKekObjectDao;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Database-backed KMS provider that stores master KEKs in a PKCS#11-like object table.
 * Uses AES-256-GCM for all cryptographic operations.
 * <p>
 * This provider is suitable for deployments that don't have access to HSM hardware.
 * The master KEKs are stored encrypted in the kms_database_kek_objects table using
 * CloudStack's existing DBEncryptionUtil, with PKCS#11-compatible attributes.
 */
public class DatabaseKMSProvider extends AdapterBase implements KMSProvider {
    private static final Logger logger = LogManager.getLogger(DatabaseKMSProvider.class);
    private static final String PROVIDER_NAME = "database";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String CKO_SECRET_KEY = "CKO_SECRET_KEY";
    private static final String CKK_AES = "CKK_AES";

    private static final String DEFAULT_PROFILE_NAME = "default";
    private static final long SYSTEM_ACCOUNT_ID = 1L;
    private static final long ROOT_DOMAIN_ID = 1L;

    private final SecureRandom secureRandom = new SecureRandom();
    @Inject
    private KMSDatabaseKekObjectDao kekObjectDao;
    @Inject
    private HSMProfileDao hsmProfileDao;

    @Override
    public boolean start() {
        super.start();
        ensureDefaultHSMProfile();
        return true;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String createKek(KeyPurpose purpose, String label, int keyBits, Long hsmProfileId) throws KMSException {
        // Database provider ignores hsmProfileId
        return createKek(purpose, label, keyBits);
    }

    @Override
    public String createKek(KeyPurpose purpose, String label, int keyBits) throws KMSException {
        if (keyBits != 128 && keyBits != 192 && keyBits != 256) {
            throw KMSException.invalidParameter("Key size must be 128, 192, or 256 bits");
        }

        if (StringUtils.isEmpty(label)) {
            throw KMSException.invalidParameter("KEK label cannot be empty");
        }

        if (kekObjectDao.existsByLabel(label)) {
            throw KMSException.keyAlreadyExists("KEK with label " + label + " already exists");
        }

        byte[] kekBytes = new byte[keyBits / 8];
        try {
            secureRandom.nextBytes(kekBytes);

            // Base64 encode then encrypt the KEK material using DBEncryptionUtil
            String kekBase64 = Base64.getEncoder().encodeToString(kekBytes);
            String encryptedKek = DBEncryptionUtil.encrypt(kekBase64);
            byte[] encryptedKekBytes = encryptedKek.getBytes(StandardCharsets.UTF_8);

            KMSDatabaseKekObjectVO kekObject = new KMSDatabaseKekObjectVO(label, purpose, keyBits, encryptedKekBytes);
            kekObject.setObjectClass(CKO_SECRET_KEY);
            kekObject.setKeyType(CKK_AES);
            kekObject.setObjectId(label.getBytes(StandardCharsets.UTF_8));
            kekObject.setAlgorithm(ALGORITHM);
            kekObject.setIsSensitive(true);
            kekObject.setIsExtractable(false);
            kekObject.setIsToken(true);
            kekObject.setIsPrivate(true);
            kekObject.setIsModifiable(false);
            kekObject.setIsCopyable(false);
            kekObject.setIsDestroyable(true);
            kekObject.setAlwaysSensitive(true);
            kekObject.setNeverExtractable(true);

            kekObjectDao.persist(kekObject);

            logger.info("Created KEK with label {} for purpose {} (PKCS#11 object ID: {})", label, purpose,
                    kekObject.getId());
            return label;

        } catch (Exception e) {
            throw KMSException.kekOperationFailed("Failed to create KEK: " + e.getMessage(), e);
        } finally {
            Arrays.fill(kekBytes, (byte) 0);
        }
    }

    @Override
    public void deleteKek(String kekId) throws KMSException {
        KMSDatabaseKekObjectVO kekObject = kekObjectDao.findByLabel(kekId);
        if (kekObject == null) {
            throw KMSException.kekNotFound("KEK with label " + kekId + " not found");
        }

        try {
            kekObjectDao.remove(kekObject.getId());

            if (kekObject.getKeyMaterial() != null) {
                Arrays.fill(kekObject.getKeyMaterial(), (byte) 0);
            }

            logger.warn("Deleted KEK with label {}. All DEKs wrapped with this KEK are now unrecoverable!", kekId);
        } catch (Exception e) {
            throw KMSException.kekOperationFailed("Failed to delete KEK: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isKekAvailable(String kekId) throws KMSException {
        try {
            KMSDatabaseKekObjectVO kekObject = kekObjectDao.findByLabel(kekId);
            return kekObject != null && kekObject.getRemoved() == null && kekObject.getKeyMaterial() != null;
        } catch (Exception e) {
            logger.warn("Error checking KEK availability: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public WrappedKey wrapKey(byte[] plainKey, KeyPurpose purpose, String kekLabel,
            Long hsmProfileId) throws KMSException {
        // Database provider ignores hsmProfileId
        return wrapKey(plainKey, purpose, kekLabel);
    }

    @Override
    public WrappedKey wrapKey(byte[] plainKey, KeyPurpose purpose, String kekLabel) throws KMSException {
        if (plainKey == null || plainKey.length == 0) {
            throw KMSException.invalidParameter("Plain key cannot be null or empty");
        }

        byte[] kekBytes = loadKek(kekLabel);

        try {
            // Tink's AesGcmJce automatically generates a random IV and prepends it to the ciphertext
            AesGcmJce aesgcm = new AesGcmJce(kekBytes);
            byte[] wrappedBlob = aesgcm.encrypt(plainKey, new byte[0]);

            WrappedKey wrapped = new WrappedKey(kekLabel, purpose, ALGORITHM, wrappedBlob, PROVIDER_NAME, new Date(),
                    null);

            logger.debug("Wrapped {} key with KEK {}", purpose, kekLabel);
            return wrapped;
        } catch (Exception e) {
            throw KMSException.wrapUnwrapFailed("Failed to wrap key: " + e.getMessage(), e);
        } finally {
            // Zeroize KEK
            Arrays.fill(kekBytes, (byte) 0);
        }
    }

    @Override
    public byte[] unwrapKey(WrappedKey wrappedKey, Long hsmProfileId) throws KMSException {
        // Database provider ignores hsmProfileId
        return unwrapKey(wrappedKey);
    }

    @Override
    public byte[] unwrapKey(WrappedKey wrappedKey) throws KMSException {
        if (wrappedKey == null) {
            throw KMSException.invalidParameter("Wrapped key cannot be null");
        }

        byte[] kekBytes = loadKek(wrappedKey.getKekId());

        try {
            AesGcmJce aesgcm = new AesGcmJce(kekBytes);
            // Tink's decrypt expects [IV][ciphertext+tag] format (same as encrypt returns)
            byte[] blob = wrappedKey.getWrappedKeyMaterial();
            if (blob.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new KMSException(KMSException.ErrorType.WRAP_UNWRAP_FAILED,
                        "Invalid wrapped key format: too short");
            }

            byte[] plainKey = aesgcm.decrypt(blob, new byte[0]);

            logger.debug("Unwrapped {} key with KEK {}", wrappedKey.getPurpose(), wrappedKey.getKekId());
            return plainKey;

        } catch (KMSException e) {
            throw e;
        } catch (Exception e) {
            throw KMSException.wrapUnwrapFailed("Failed to unwrap key: " + e.getMessage(), e);
        } finally {
            // Zeroize KEK
            Arrays.fill(kekBytes, (byte) 0);
        }
    }

    @Override
    public WrappedKey generateAndWrapDek(KeyPurpose purpose, String kekLabel, int keyBits,
            Long hsmProfileId) throws KMSException {
        // Database provider ignores hsmProfileId
        return generateAndWrapDek(purpose, kekLabel, keyBits);
    }

    @Override
    public WrappedKey generateAndWrapDek(KeyPurpose purpose, String kekLabel, int keyBits) throws KMSException {
        if (keyBits != 128 && keyBits != 192 && keyBits != 256) {
            throw KMSException.invalidParameter("DEK size must be 128, 192, or 256 bits");
        }

        byte[] dekBytes = new byte[keyBits / 8];
        secureRandom.nextBytes(dekBytes);

        try {
            return wrapKey(dekBytes, purpose, kekLabel);
        } finally {
            // Zeroize DEK (wrapped version is in WrappedKey)
            Arrays.fill(dekBytes, (byte) 0);
        }
    }

    @Override
    public WrappedKey rewrapKey(WrappedKey oldWrappedKey, String newKekLabel,
            Long targetHsmProfileId) throws KMSException {
        // Database provider ignores targetHsmProfileId
        return rewrapKey(oldWrappedKey, newKekLabel);
    }

    @Override
    public WrappedKey rewrapKey(WrappedKey oldWrappedKey, String newKekLabel) throws KMSException {
        byte[] plainKey = unwrapKey(oldWrappedKey);
        try {
            return wrapKey(plainKey, oldWrappedKey.getPurpose(), newKekLabel);
        } finally {
            // Zeroize plaintext DEK
            Arrays.fill(plainKey, (byte) 0);
        }
    }

    @Override
    public boolean healthCheck() throws KMSException {
        try {
            if (kekObjectDao == null) {
                logger.error("KMSDatabaseKekObjectDao is not initialized");
                return false;
            }
            return true;

        } catch (Exception e) {
            throw KMSException.healthCheckFailed("Health check failed: " + e.getMessage(), e);
        }
    }

    private byte[] loadKek(String kekLabel) throws KMSException {
        KMSDatabaseKekObjectVO kekObject = kekObjectDao.findByLabel(kekLabel);

        if (kekObject == null || kekObject.getRemoved() != null) {
            throw KMSException.kekNotFound("KEK with label " + kekLabel + " not found");
        }

        try {
            byte[] encryptedKekBytes = kekObject.getKeyMaterial();
            if (encryptedKekBytes == null || encryptedKekBytes.length == 0) {
                throw KMSException.kekNotFound("KEK value is empty for label " + kekLabel);
            }

            String encryptedKek = new String(encryptedKekBytes, StandardCharsets.UTF_8);
            String kekBase64 = DBEncryptionUtil.decrypt(encryptedKek);
            byte[] kekBytes = Base64.getDecoder().decode(kekBase64);

            updateLastUsed(kekLabel);

            return kekBytes;

        } catch (IllegalArgumentException e) {
            throw KMSException.kekOperationFailed("Invalid KEK encoding for label " + kekLabel, e);
        } catch (Exception e) {
            throw KMSException.kekOperationFailed("Failed to decrypt KEK for label " + kekLabel + ": " + e.getMessage(),
                    e);
        }
    }

    private void updateLastUsed(String kekLabel) {
        try {
            KMSDatabaseKekObjectVO kekObject = kekObjectDao.findByLabel(kekLabel);
            if (kekObject != null && kekObject.getRemoved() == null) {
                kekObject.setLastUsed(new Date());
                kekObjectDao.update(kekObject.getId(), kekObject);
            }
        } catch (Exception e) {
            logger.debug("Failed to update last used timestamp for KEK {}: {}", kekLabel, e.getMessage());
        }
    }

    /**
     * Seeds the default database HSM profile if it does not already exist.
     * This runs at provider startup to avoid FK constraint issues that occur
     * when the INSERT is placed in the schema upgrade SQL script (the account
     * table may not yet be populated when the upgrade script executes on a
     * fresh install).
     */
    private void ensureDefaultHSMProfile() {
        try {
            SearchBuilder<HSMProfileVO> sb = hsmProfileDao.createSearchBuilder();
            sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
            sb.and("system", sb.entity().getIsPublic(), SearchCriteria.Op.EQ);
            sb.and("protocol", sb.entity().getProtocol(), SearchCriteria.Op.EQ);
            sb.done();

            SearchCriteria<HSMProfileVO> sc = sb.create();
            sc.setParameters("name", DEFAULT_PROFILE_NAME);
            sc.setParameters("system", true);
            sc.setParameters("protocol", PROVIDER_NAME);

            List<HSMProfileVO> existing = hsmProfileDao.customSearchIncludingRemoved(sc, null);
            if (existing != null && !existing.isEmpty()) {
                logger.debug("Default database HSM profile already exists (id={})", existing.get(0).getId());
                return;
            }

            HSMProfileVO profile = new HSMProfileVO(DEFAULT_PROFILE_NAME, PROVIDER_NAME,
                    SYSTEM_ACCOUNT_ID, ROOT_DOMAIN_ID, null, null);
            profile.setEnabled(false);
            profile.setIsPublic(true);
            hsmProfileDao.persist(profile);
            logger.info("Seeded default database HSM profile (id={}, uuid={})", profile.getId(), profile.getUuid());
        } catch (Exception e) {
            logger.warn("Failed to seed default database HSM profile: {}", e.getMessage(), e);
        }
    }


    @Override
    public String getConfigComponentName() {
        return DatabaseKMSProvider.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[0];
    }
}
