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

package org.apache.cloudstack.kms.provider.pkcs11;

import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;
import org.apache.cloudstack.kms.HSMProfileDetailsVO;
import org.apache.cloudstack.kms.KMSKekVersionVO;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.HSMProfileDetailsDao;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.crypt.DBEncryptionUtil;

@Component
public class PKCS11HSMProvider extends AdapterBase implements KMSProvider {
    private static final Logger logger = LogManager.getLogger(PKCS11HSMProvider.class);
    private static final String PROVIDER_NAME = "pkcs11";
    
    @Inject
    private HSMProfileDao hsmProfileDao;
    
    @Inject
    private HSMProfileDetailsDao hsmProfileDetailsDao;
    
    @Inject
    private KMSKekVersionDao kmsKekVersionDao;

    // Session pool per HSM profile
    private final Map<Long, HSMSessionPool> sessionPools = new ConcurrentHashMap<>();
    
    // Profile configuration caching
    private final Map<Long, Map<String, String>> profileConfigCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        logger.info("Initializing PKCS11HSMProvider");
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[0];
    }

    @Override
    public String createKek(KeyPurpose purpose, String label, int keyBits, Long hsmProfileId) throws KMSException {
        if (hsmProfileId == null) {
            throw KMSException.invalidParameter("HSM Profile ID is required for PKCS#11 provider");
        }
        
        if (StringUtils.isEmpty(label)) {
            label = generateKekLabel(purpose);
        }

        HSMSessionPool pool = getSessionPool(hsmProfileId);
        PKCS11Session session = null;
        try {
            session = pool.acquireSession(5000);
            return session.generateKey(label, keyBits, purpose);
        } finally {
            pool.releaseSession(session);
        }
    }

    @Override
    public WrappedKey wrapKey(byte[] plainDek, KeyPurpose purpose, String kekLabel, Long hsmProfileId) throws KMSException {
        if (hsmProfileId == null) {
            hsmProfileId = resolveProfileId(kekLabel);
        }

        HSMSessionPool pool = getSessionPool(hsmProfileId);
        PKCS11Session session = null;
        try {
            session = pool.acquireSession(5000);
            byte[] wrappedBlob = session.wrapKey(plainDek, kekLabel);
            return new WrappedKey(kekLabel, purpose, "AES/GCM/NoPadding", wrappedBlob, PROVIDER_NAME, new Date(), null);
        } finally {
            pool.releaseSession(session);
        }
    }

    @Override
    public byte[] unwrapKey(WrappedKey wrappedKey, Long hsmProfileId) throws KMSException {
        if (hsmProfileId == null) {
            hsmProfileId = resolveProfileId(wrappedKey.getKekId());
        }

        HSMSessionPool pool = getSessionPool(hsmProfileId);
        PKCS11Session session = null;
        try {
            session = pool.acquireSession(5000);
            return session.unwrapKey(wrappedKey.getWrappedKeyMaterial(), wrappedKey.getKekId());
        } finally {
            pool.releaseSession(session);
        }
    }

    @Override
    public WrappedKey rewrapKey(WrappedKey oldWrappedKey, String newKekLabel, Long targetHsmProfileId) throws KMSException {
        // 1. Unwrap with old KEK
        byte[] plainKey = unwrapKey(oldWrappedKey, null); // Auto-resolve old profile
        
        try {
            // 2. Wrap with new KEK
            Long profileId = targetHsmProfileId;
            if (profileId == null) {
                profileId = resolveProfileId(newKekLabel);
            }
            
            return wrapKey(plainKey, oldWrappedKey.getPurpose(), newKekLabel, profileId);
        } finally {
            // Zeroize plaintext key
            java.util.Arrays.fill(plainKey, (byte) 0);
        }
    }

    @Override
    public WrappedKey generateAndWrapDek(KeyPurpose purpose, String kekLabel, int keyBits, Long hsmProfileId) throws KMSException {
        // Generate random DEK
        byte[] dekBytes = new byte[keyBits / 8];
        new java.security.SecureRandom().nextBytes(dekBytes);

        try {
            return wrapKey(dekBytes, purpose, kekLabel, hsmProfileId);
        } finally {
            java.util.Arrays.fill(dekBytes, (byte) 0);
        }
    }

    @Override
    public void deleteKek(String kekId) throws KMSException {
        Long hsmProfileId = resolveProfileId(kekId);
        HSMSessionPool pool = getSessionPool(hsmProfileId);
        PKCS11Session session = null;
        try {
            session = pool.acquireSession(5000);
            session.deleteKey(kekId);
        } finally {
            pool.releaseSession(session);
        }
    }

    @Override
    public List<String> listKeks(KeyPurpose purpose) throws KMSException {
        throw new KMSException(KMSException.ErrorType.OPERATION_FAILED, "Listing KEKs directly from HSMs not supported, use DB");
    }

    @Override
    public boolean isKekAvailable(String kekId) throws KMSException {
        Long hsmProfileId = resolveProfileId(kekId);
        if (hsmProfileId == null) return false;
        
        HSMSessionPool pool = getSessionPool(hsmProfileId);
        PKCS11Session session = null;
        try {
            session = pool.acquireSession(5000);
            return session.checkKeyExists(kekId);
        } catch (Exception e) {
            return false;
        } finally {
            pool.releaseSession(session);
        }
    }

    @Override
    public boolean healthCheck() throws KMSException {
        return true;
    }

    private Long resolveProfileId(String kekLabel) throws KMSException {
        KMSKekVersionVO version = kmsKekVersionDao.findByKekLabel(kekLabel);
        if (version != null && version.getHsmProfileId() != null) {
            return version.getHsmProfileId();
        }
        throw new KMSException(KMSException.ErrorType.KEK_NOT_FOUND, "Could not resolve HSM profile for KEK: " + kekLabel);
    }

    private HSMSessionPool getSessionPool(Long profileId) {
        return sessionPools.computeIfAbsent(profileId, 
            id -> new HSMSessionPool(id, loadProfileConfig(id)));
    }

    private Map<String, String> loadProfileConfig(Long profileId) {
        return profileConfigCache.computeIfAbsent(profileId, id -> {
            List<HSMProfileDetailsVO> details = hsmProfileDetailsDao.listByProfileId(id);
            Map<String, String> config = new HashMap<>();
            for (HSMProfileDetailsVO detail : details) {
                String value = detail.getValue();
                if (isSensitiveKey(detail.getName())) {
                    value = DBEncryptionUtil.decrypt(value);
                }
                config.put(detail.getName(), value);
            }
            return config;
        });
    }

    private boolean isSensitiveKey(String key) {
        return key.equalsIgnoreCase("pin") || 
               key.equalsIgnoreCase("password") || 
               key.toLowerCase().contains("secret") ||
               key.equalsIgnoreCase("private_key");
    }

    private String generateKekLabel(KeyPurpose purpose) {
        return purpose.getName() + "-kek-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Inner class for session pooling
    private static class HSMSessionPool {
        private final BlockingQueue<PKCS11Session> availableSessions;
        private final Long profileId;
        private final Map<String, String> config;
        private final int maxSessions;
        private final int minIdleSessions;
        
        HSMSessionPool(Long profileId, Map<String, String> config) {
            this.profileId = profileId;
            this.config = config;
            this.maxSessions = Integer.parseInt(config.getOrDefault("max_sessions", "10"));
            this.minIdleSessions = Integer.parseInt(config.getOrDefault("min_idle_sessions", "2"));
            this.availableSessions = new ArrayBlockingQueue<>(maxSessions);
            
            // Pre-warm
            for (int i = 0; i < minIdleSessions; i++) {
                try {
                    availableSessions.offer(createNewSession());
                } catch (Exception e) {
                    logger.warn("Failed to pre-warm session for profile {}: {}", profileId, e.getMessage());
                }
            }
        }
        
        PKCS11Session acquireSession(long timeoutMs) throws KMSException {
            try {
                PKCS11Session session = availableSessions.poll();
                if (session == null || !session.isValid()) {
                    if (session != null) {
                        session.close();
                    }
                    session = createNewSession();
                }
                return session;
            } catch (Exception e) {
                throw new KMSException(KMSException.ErrorType.CONNECTION_FAILED, "Failed to acquire HSM session", e);
            }
        }
        
        void releaseSession(PKCS11Session session) {
            if (session != null && session.isValid()) {
                if (!availableSessions.offer(session)) {
                    session.close(); // Pool full
                }
            }
        }
        
        private PKCS11Session createNewSession() throws KMSException {
            return new PKCS11Session(config);
        }
    }

    // Inner class representing a PKCS#11 session
    private static class PKCS11Session {
        private final Map<String, String> config;
        private KeyStore keyStore;
        private Provider provider;
        
        PKCS11Session(Map<String, String> config) throws KMSException {
            this.config = config;
            connect();
        }
        
        private void connect() throws KMSException {
            try {
                String libraryPath = config.get("library_path");
                // In real implementation:
                // Configure SunPKCS11 provider with library path
                // Login to keystore
                logger.debug("Simulating PKCS#11 connection to " + libraryPath);
            } catch (Exception e) {
                throw new KMSException(KMSException.ErrorType.CONNECTION_FAILED, "Failed to connect to HSM: " + e.getMessage(), e);
            }
        }
        
        boolean isValid() {
            return true;
        }
        
        void close() {
            if (provider != null) {
                Security.removeProvider(provider.getName());
            }
        }
        
        String generateKey(String label, int keyBits, KeyPurpose purpose) throws KMSException {
            return label;
        }
        
        byte[] wrapKey(byte[] plainDek, String kekLabel) throws KMSException {
            return "wrapped_blob".getBytes();
        }
        
        byte[] unwrapKey(byte[] wrappedBlob, String kekLabel) throws KMSException {
            return new byte[32]; // 256 bits
        }
        
        void deleteKey(String label) throws KMSException {
            // Stub
        }
        
        boolean checkKeyExists(String label) throws KMSException {
            return true;
        }
    }
}
