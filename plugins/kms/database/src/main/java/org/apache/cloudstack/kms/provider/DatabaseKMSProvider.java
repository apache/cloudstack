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
import com.google.crypto.tink.subtle.AesGcmJce;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;
import org.apache.cloudstack.kms.provider.database.KMSDatabaseKekObjectVO;
import org.apache.cloudstack.kms.provider.database.dao.KMSDatabaseKekObjectDao;
import com.cloud.utils.crypt.DBEncryptionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database-backed KMS provider that stores master KEKs in a PKCS#11-like object table.
 * Uses AES-256-GCM for all cryptographic operations.
 * <p>
 * This provider is suitable for deployments that don't have access to HSM hardware.
 * The master KEKs are stored encrypted in the kms_database_kek_objects table using
 * CloudStack's existing DBEncryptionUtil, with PKCS#11-compatible attributes.
 */
public class DatabaseKMSProvider extends AdapterBase implements KMSProvider {
    // Configuration keys
    public static final ConfigKey<Boolean> CacheEnabled = new ConfigKey<>(
            "Advanced",
            Boolean.class,
            "kms.database.cache.enabled",
            "true",
            "Enable in-memory caching of KEKs for better performance",
            true,
            ConfigKey.Scope.Global
    );
    private static final Logger logger = LogManager.getLogger(DatabaseKMSProvider.class);
    private static final String PROVIDER_NAME = "database";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    // PKCS#11 constants
    private static final String CKO_SECRET_KEY = "CKO_SECRET_KEY";
    private static final String CKK_AES = "CKK_AES";
    // In-memory cache of KEKs (encrypted form cached, decrypted on demand)
    private final Map<String, byte[]> kekCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    @Inject
    private KMSDatabaseKekObjectDao kekObjectDao;

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
            label = generateKekLabel(purpose);
        }

        // Check if KEK already exists
        if (kekObjectDao.existsByLabel(label)) {
            throw KMSException.keyAlreadyExists("KEK with label " + label + " already exists");
        }

        try {
            // Generate random KEK
            byte[] kekBytes = new byte[keyBits / 8];
            secureRandom.nextBytes(kekBytes);

            // Encrypt the KEK material using DBEncryptionUtil (Base64 encode first, then encrypt)
            String kekBase64 = Base64.getEncoder().encodeToString(kekBytes);
            String encryptedKek = DBEncryptionUtil.encrypt(kekBase64);
            byte[] encryptedKekBytes = encryptedKek.getBytes(StandardCharsets.UTF_8);

            // Create PKCS#11-like object
            KMSDatabaseKekObjectVO kekObject = new KMSDatabaseKekObjectVO(label, purpose, keyBits, encryptedKekBytes);
            kekObject.setObjectClass(CKO_SECRET_KEY);
            kekObject.setKeyType(CKK_AES);
            kekObject.setObjectId(label.getBytes(StandardCharsets.UTF_8));
            kekObject.setAlgorithm(ALGORITHM);
            // PKCS#11 attributes for KEK
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

            // Cache the KEK
            if (CacheEnabled.value()) {
                kekCache.put(label, kekBytes);
            }

            logger.info("Created KEK with label {} for purpose {} (PKCS#11 object ID: {})", label, purpose, kekObject.getId());
            return label;

        } catch (Exception e) {
            throw KMSException.kekOperationFailed("Failed to create KEK: " + e.getMessage(), e);
        }
    }

    @Override
    public String getConfigComponentName() {
        return DatabaseKMSProvider.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                CacheEnabled
        };
    }

    @Override
    public void deleteKek(String kekId) throws KMSException {
        KMSDatabaseKekObjectVO kekObject = kekObjectDao.findByLabel(kekId);
        if (kekObject == null) {
            throw KMSException.kekNotFound("KEK with label " + kekId + " not found");
        }

        try {
            kekObjectDao.remove(kekObject.getId());

            // Remove from cache
            byte[] cachedKek = kekCache.remove(kekId);
            if (cachedKek != null) {
                Arrays.fill(cachedKek, (byte) 0); // Zeroize
            }

            // Zeroize key material in database object
            if (kekObject.getKeyMaterial() != null) {
                Arrays.fill(kekObject.getKeyMaterial(), (byte) 0);
            }

            logger.warn("Deleted KEK with label {}. All DEKs wrapped with this KEK are now unrecoverable!", kekId);
        } catch (Exception e) {
            throw KMSException.kekOperationFailed("Failed to delete KEK: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> listKeks(KeyPurpose purpose) throws KMSException {
        try {
            List<String> keks = new ArrayList<>();

            List<KMSDatabaseKekObjectVO> kekObjects;
            if (purpose != null) {
                kekObjects = kekObjectDao.listByPurpose(purpose);
            } else {
                kekObjects = kekObjectDao.listAll();
            }

            for (KMSDatabaseKekObjectVO kekObject : kekObjects) {
                if (kekObject.getRemoved() == null) {
                    keks.add(kekObject.getLabel());
                }
            }

            logger.debug("listKeks called for purpose: {}. Found {} KEKs.", purpose, keks.size());
            return keks;
        } catch (Exception e) {
            throw KMSException.kekOperationFailed("Failed to list KEKs: " + e.getMessage(), e);
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
    public WrappedKey wrapKey(byte[] plainKey, KeyPurpose purpose, String kekLabel, Long hsmProfileId) throws KMSException {
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
            // Create AES-GCM cipher with the KEK
            // Tink's AesGcmJce automatically generates a random IV and prepends it to the ciphertext
            AesGcmJce aesgcm = new AesGcmJce(kekBytes);

            // Encrypt the DEK (Tink's encrypt returns [IV][ciphertext+tag] format)
            byte[] wrappedBlob = aesgcm.encrypt(plainKey, new byte[0]); // Empty associated data

            WrappedKey wrapped = new WrappedKey(kekLabel, purpose, ALGORITHM, wrappedBlob, PROVIDER_NAME, new Date(), null);

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
            // Create AES-GCM cipher with the KEK
            AesGcmJce aesgcm = new AesGcmJce(kekBytes);

            // Tink's decrypt expects [IV][ciphertext+tag] format (same as encrypt returns)
            byte[] blob = wrappedKey.getWrappedKeyMaterial();
            if (blob.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new KMSException(KMSException.ErrorType.WRAP_UNWRAP_FAILED,
                        "Invalid wrapped key format: too short");
            }

            // Decrypt the DEK (Tink extracts IV from the blob automatically)
            byte[] plainKey = aesgcm.decrypt(blob, new byte[0]); // Empty associated data

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
    public WrappedKey generateAndWrapDek(KeyPurpose purpose, String kekLabel, int keyBits, Long hsmProfileId) throws KMSException {
        // Database provider ignores hsmProfileId
        return generateAndWrapDek(purpose, kekLabel, keyBits);
    }

    @Override
    public WrappedKey generateAndWrapDek(KeyPurpose purpose, String kekLabel, int keyBits) throws KMSException {
        if (keyBits != 128 && keyBits != 192 && keyBits != 256) {
            throw KMSException.invalidParameter("DEK size must be 128, 192, or 256 bits");
        }

        // Generate random DEK
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
    public WrappedKey rewrapKey(WrappedKey oldWrappedKey, String newKekLabel, Long targetHsmProfileId) throws KMSException {
        // Database provider ignores targetHsmProfileId
        return rewrapKey(oldWrappedKey, newKekLabel);
    }

    @Override
    public WrappedKey rewrapKey(WrappedKey oldWrappedKey, String newKekLabel) throws KMSException {
        // Unwrap with old KEK
        byte[] plainKey = unwrapKey(oldWrappedKey);

        try {
            // Wrap with new KEK
            return wrapKey(plainKey, oldWrappedKey.getPurpose(), newKekLabel);
        } finally {
            // Zeroize plaintext DEK
            Arrays.fill(plainKey, (byte) 0);
        }
    }

    @Override
    public boolean healthCheck() throws KMSException {
        try {
            // Verify we can access KEK object DAO
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
        // Check cache first
        if (CacheEnabled.value()) {
            byte[] cached = kekCache.get(kekLabel);
            if (cached != null) {
                updateLastUsed(kekLabel);
                return Arrays.copyOf(cached, cached.length); // Return copy
            }
        }

        // Load from database
        KMSDatabaseKekObjectVO kekObject = kekObjectDao.findByLabel(kekLabel);

        if (kekObject == null || kekObject.getRemoved() != null) {
            throw KMSException.kekNotFound("KEK with label " + kekLabel + " not found");
        }

        try {
            // Decrypt the key material
            byte[] encryptedKekBytes = kekObject.getKeyMaterial();
            if (encryptedKekBytes == null || encryptedKekBytes.length == 0) {
                throw KMSException.kekNotFound("KEK value is empty for label " + kekLabel);
            }

            // Decrypt using DBEncryptionUtil
            String encryptedKek = new String(encryptedKekBytes, StandardCharsets.UTF_8);
            String kekBase64 = DBEncryptionUtil.decrypt(encryptedKek);
            byte[] kekBytes = Base64.getDecoder().decode(kekBase64);

            // Cache for future use
            if (CacheEnabled.value()) {
                kekCache.put(kekLabel, Arrays.copyOf(kekBytes, kekBytes.length));
            }

            // Update last used timestamp
            updateLastUsed(kekLabel);

            return kekBytes;

        } catch (IllegalArgumentException e) {
            throw KMSException.kekOperationFailed("Invalid KEK encoding for label " + kekLabel, e);
        } catch (Exception e) {
            throw KMSException.kekOperationFailed("Failed to decrypt KEK for label " + kekLabel + ": " + e.getMessage(), e);
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

    private String generateKekLabel(KeyPurpose purpose) {
        return purpose.getName() + "-kek-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
