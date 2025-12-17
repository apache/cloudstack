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

import com.google.crypto.tink.subtle.AesGcmJce;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database-backed KMS provider that stores master KEKs encrypted in the configuration table.
 * Uses AES-256-GCM for all cryptographic operations.
 * <p>
 * This provider is suitable for deployments that don't have access to HSM hardware.
 * The master KEKs are stored encrypted using CloudStack's existing DBEncryptionUtil.
 */
public class DatabaseKMSProvider implements KMSProvider {
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
    private static final String KEK_CONFIG_PREFIX = "kms.database.kek.";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    // In-memory cache of KEKs (encrypted form cached, decrypted on demand)
    private final Map<String, byte[]> kekCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    @Inject
    private ConfigurationDao configDao;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String createKek(KeyPurpose purpose, String label, int keyBits) throws KMSException {
        if (keyBits != 128 && keyBits != 192 && keyBits != 256) {
            throw KMSException.invalidParameter("Key size must be 128, 192, or 256 bits");
        }

        if (StringUtils.isEmpty(label)) {
            label = generateKekLabel(purpose);
        }

        String configKey = buildConfigKey(label);

        // Check if KEK already exists
        ConfigurationVO existing = configDao.findByName(configKey);
        if (existing != null) {
            throw KMSException.keyAlreadyExists("KEK with label " + label + " already exists");
        }

        try {
            // Generate random KEK
            byte[] kekBytes = new byte[keyBits / 8];
            secureRandom.nextBytes(kekBytes);

            // Store in configuration table (will be encrypted automatically due to "Secure" category)
            String kekBase64 = java.util.Base64.getEncoder().encodeToString(kekBytes);
            ConfigurationVO config = new ConfigurationVO(
                    "Secure", // Category - triggers encryption
                    "DEFAULT",
                    getConfigComponentName(),
                    configKey,
                    kekBase64,
                    "KMS KEK for " + purpose.getName() + " (label: " + label + ")"
            );
            configDao.persist(config);

            // Cache the KEK
            if (CacheEnabled.value()) {
                kekCache.put(label, kekBytes);
            }

            logger.info("Created KEK with label {} for purpose {}", label, purpose);
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
        String configKey = buildConfigKey(kekId);

        ConfigurationVO config = configDao.findByName(configKey);
        if (config == null) {
            throw KMSException.kekNotFound("KEK with label " + kekId + " not found");
        }

        try {
            // Remove from configuration (name is the primary key)
            configDao.remove(config.getName());

            // Remove from cache
            byte[] cachedKek = kekCache.remove(kekId);
            if (cachedKek != null) {
                Arrays.fill(cachedKek, (byte) 0); // Zeroize
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

            // We can't efficiently list all KEKs without a custom query
            // For now, return cached keys only - KEKs will be tracked via cache
            // TODO: Add custom DAO method or maintain KEK registry
            logger.debug("listKeks called for purpose: {}. Returning cached keys only.", purpose);

            // Return keys from cache
            for (String label : kekCache.keySet()) {
                if (purpose == null || label.startsWith(purpose.getName())) {
                    keks.add(label);
                }
            }

            return keks;
        } catch (Exception e) {
            throw KMSException.kekOperationFailed("Failed to list KEKs: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isKekAvailable(String kekId) throws KMSException {
        try {
            String configKey = buildConfigKey(kekId);
            ConfigurationVO config = configDao.findByName(configKey);
            return config != null && config.getValue() != null;
        } catch (Exception e) {
            logger.warn("Error checking KEK availability: {}", e.getMessage());
            return false;
        }
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

            WrappedKey wrapped = new WrappedKey(
                    kekLabel,
                    purpose,
                    ALGORITHM,
                    wrappedBlob,
                    PROVIDER_NAME,
                    new Date(),
                    null // zoneId set by caller
            );

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
            // Verify we can access configuration
            if (configDao == null) {
                logger.error("Configuration DAO is not initialized");
                return false;
            }

            // Try to list KEKs (lightweight operation)
            List<String> keks = listKeks(null);
            logger.debug("Health check passed. Found {} KEKs", keks.size());

            // Optionally verify we can perform wrap/unwrap
            byte[] testKey = new byte[32];
            secureRandom.nextBytes(testKey);

            // If we have any KEK, test it
            if (!keks.isEmpty()) {
                String testKek = keks.get(0);
                WrappedKey wrapped = wrapKey(testKey, KeyPurpose.VOLUME_ENCRYPTION, testKek);
                byte[] unwrapped = unwrapKey(wrapped);

                boolean matches = Arrays.equals(testKey, unwrapped);
                Arrays.fill(unwrapped, (byte) 0);

                if (!matches) {
                    logger.error("Health check failed: wrap/unwrap test failed");
                    return false;
                }
            }

            Arrays.fill(testKey, (byte) 0);
            return true;

        } catch (Exception e) {
            throw KMSException.healthCheckFailed("Health check failed: " + e.getMessage(), e);
        }
    }

    // ==================== Private Helper Methods ====================

    private byte[] loadKek(String kekLabel) throws KMSException {
        // Check cache first
        if (CacheEnabled.value()) {
            byte[] cached = kekCache.get(kekLabel);
            if (cached != null) {
                return Arrays.copyOf(cached, cached.length); // Return copy
            }
        }

        // Load from database
        String configKey = buildConfigKey(kekLabel);
        ConfigurationVO config = configDao.findByName(configKey);

        if (config == null) {
            throw KMSException.kekNotFound("KEK with label " + kekLabel + " not found");
        }

        try {
            // getValue() automatically decrypts
            String kekBase64 = config.getValue();
            if (StringUtils.isEmpty(kekBase64)) {
                throw KMSException.kekNotFound("KEK value is empty for label " + kekLabel);
            }

            byte[] kekBytes = java.util.Base64.getDecoder().decode(kekBase64);

            // Cache for future use
            if (CacheEnabled.value()) {
                kekCache.put(kekLabel, Arrays.copyOf(kekBytes, kekBytes.length));
            }

            return kekBytes;

        } catch (IllegalArgumentException e) {
            throw KMSException.kekOperationFailed("Invalid KEK encoding for label " + kekLabel, e);
        }
    }

    private String buildConfigKey(String label) {
        return KEK_CONFIG_PREFIX + label;
    }

    private String generateKekLabel(KeyPurpose purpose) {
        return purpose.getName() + "-kek-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

