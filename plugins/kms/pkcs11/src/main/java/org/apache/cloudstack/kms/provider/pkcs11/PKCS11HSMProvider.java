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

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.crypt.DBEncryptionUtil;
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

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.inject.Inject;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PKCS11HSMProvider extends AdapterBase implements KMSProvider {
    private static final Logger logger = LogManager.getLogger(PKCS11HSMProvider.class);
    private static final String PROVIDER_NAME = "pkcs11";
    // Security note (#7): AES-CBC provides confidentiality but not authenticity (no
    // HMAC).
    // While AES-GCM is preferred, SunPKCS11 support for GCM is often buggy or
    // missing
    // depending on the underlying driver. We rely on the HSM/storage for tamper
    // resistance.
    // AES-CBC with PKCS5Padding: FIPS-compliant (NIST SP 800-38A) with universal PKCS#11 support
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final long SESSION_ACQUIRE_TIMEOUT_MS = 5000L;

    private static final int[] VALID_KEY_SIZES = {128, 192, 256};
    private final Map<Long, HSMSessionPool> sessionPools = new ConcurrentHashMap<>();
    @Inject
    private HSMProfileDao hsmProfileDao;
    @Inject
    private HSMProfileDetailsDao hsmProfileDetailsDao;
    @Inject
    private KMSKekVersionDao kmsKekVersionDao;

    @PostConstruct
    public void init() {
        logger.info("Initializing PKCS11HSMProvider");
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String createKek(KeyPurpose purpose, String label, int keyBits, Long hsmProfileId) throws KMSException {
        if (hsmProfileId == null) {
            throw KMSException.invalidParameter("HSM Profile ID is required for PKCS#11 provider");
        }
        if (StringUtils.isEmpty(label)) {
            throw KMSException.invalidParameter("KEK label cannot be empty");
        }
        return executeWithSession(hsmProfileId, session -> session.generateKey(label, keyBits, purpose));
    }

    @Override
    public void deleteKek(String kekId) throws KMSException {
        Long hsmProfileId = resolveProfileId(kekId);
        executeWithSession(hsmProfileId, session -> {
            session.deleteKey(kekId);
            return null;
        });
    }

    @Override
    public boolean isKekAvailable(String kekId) throws KMSException {
        try {
            Long hsmProfileId = resolveProfileId(kekId);
            return executeWithSession(hsmProfileId, session -> session.checkKeyExists(kekId));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public WrappedKey wrapKey(byte[] plainDek, KeyPurpose purpose, String kekLabel,
            Long hsmProfileId) throws KMSException {
        if (hsmProfileId == null) {
            hsmProfileId = resolveProfileId(kekLabel);
        }

        byte[] wrappedBlob = executeWithSession(hsmProfileId, session -> session.wrapKey(plainDek, kekLabel));
        return new WrappedKey(kekLabel, purpose, CIPHER_ALGORITHM, wrappedBlob, PROVIDER_NAME, new Date(), null);
    }

    @Override
    public byte[] unwrapKey(WrappedKey wrappedKey, Long hsmProfileId) throws KMSException {
        if (hsmProfileId == null) {
            hsmProfileId = resolveProfileId(wrappedKey.getKekId());
        }

        return executeWithSession(hsmProfileId,
                session -> session.unwrapKey(wrappedKey.getWrappedKeyMaterial(), wrappedKey.getKekId()));
    }

    @Override
    public WrappedKey generateAndWrapDek(KeyPurpose purpose, String kekLabel, int keyBits,
            Long hsmProfileId) throws KMSException {
        byte[] dekBytes = new byte[keyBits / 8];
        new SecureRandom().nextBytes(dekBytes);

        try {
            return wrapKey(dekBytes, purpose, kekLabel, hsmProfileId);
        } finally {
            Arrays.fill(dekBytes, (byte) 0);
        }
    }

    @Override
    public WrappedKey rewrapKey(WrappedKey oldWrappedKey, String newKekLabel,
            Long targetHsmProfileId) throws KMSException {
        byte[] plainKey = unwrapKey(oldWrappedKey, null);
        try {
            Long profileId = targetHsmProfileId != null ? targetHsmProfileId : resolveProfileId(newKekLabel);
            return wrapKey(plainKey, oldWrappedKey.getPurpose(), newKekLabel, profileId);
        } finally {
            Arrays.fill(plainKey, (byte) 0);
        }
    }

    /**
     * Performs health check on all configured HSM profiles.
     *
     * <p>For each configured HSM profile:
     * <ol>
     *   <li>Attempts to acquire a test session</li>
     *   <li>Verifies HSM is responsive (lightweight KeyStore operation)</li>
     *   <li>Releases the session</li>
     * </ol>
     *
     * <p>If any HSM profile fails the health check, this method throws an exception.
     * If no profiles are configured, returns true (nothing to check).
     *
     * @return true if all configured HSM profiles are healthy
     * @throws KMSException with {@code HEALTH_CHECK_FAILED} if any HSM profile is unhealthy
     */
    @Override
    public boolean healthCheck() throws KMSException {
        if (sessionPools.isEmpty()) {
            logger.debug("No HSM profiles configured for health check");
            return true;
        }

        boolean allHealthy = true;
        for (Long profileId : sessionPools.keySet()) {
            if (!checkProfileHealth(profileId)) {
                allHealthy = false;
            }
        }

        if (!allHealthy) {
            throw KMSException.healthCheckFailed("One or more HSM profiles failed health check", null);
        }

        return true;
    }

    private boolean checkProfileHealth(Long profileId) {
        try {
            Boolean result = executeWithSession(profileId, session -> {
                try {
                    session.keyStore.size(); // Verify the HSM token is currently reachable
                } catch (KeyStoreException e) {
                    return false;
                }
                return true;
            });
            logger.debug("Health check {} for HSM profile {}", result ? "passed" : "failed", profileId);
            return result;
        } catch (Exception e) {
            logger.warn("Health check failed for HSM profile {}: {}", profileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void invalidateProfileCache(Long profileId) {
        HSMSessionPool pool = sessionPools.remove(profileId);
        if (pool != null) {
            pool.invalidate();
        }
        logger.info("Invalidated HSM session pool for profile {}", profileId);
    }

    Long resolveProfileId(String kekLabel) throws KMSException {
        KMSKekVersionVO version = kmsKekVersionDao.findByKekLabel(kekLabel);
        if (version != null && version.getHsmProfileId() != null) {
            return version.getHsmProfileId();
        }
        throw new KMSException(KMSException.ErrorType.KEK_NOT_FOUND,
                "Could not resolve HSM profile for KEK: " + kekLabel);
    }

    /**
     * Executes an operation with a session from the pool, handling acquisition and release.
     *
     * @param hsmProfileId HSM profile ID
     * @param operation    Operation to execute with the session
     * @return Result of the operation
     * @throws KMSException if session acquisition fails or operation throws an exception
     */
    private <T> T executeWithSession(Long hsmProfileId, SessionOperation<T> operation) throws KMSException {
        HSMSessionPool pool = getSessionPool(hsmProfileId);
        PKCS11Session session = null;
        try {
            session = pool.acquireSession(SESSION_ACQUIRE_TIMEOUT_MS);
            return operation.execute(session);
        } finally {
            pool.releaseSession(session);
        }
    }

    HSMSessionPool getSessionPool(Long profileId) {
        return sessionPools.computeIfAbsent(profileId, id -> {
            Map<String, String> config = loadProfileConfig(id);
            int maxSessions = Integer.parseInt(config.getOrDefault("max_sessions", "10"));
            return new HSMSessionPool(id, maxSessions, this);
        });
    }

    Map<String, String> loadProfileConfig(Long profileId) {
        List<HSMProfileDetailsVO> details = hsmProfileDetailsDao.listByProfileId(profileId);
        Map<String, String> config = new HashMap<>();
        for (HSMProfileDetailsVO detail : details) {
            String value = detail.getValue();
            if (isSensitiveKey(detail.getName())) {
                value = DBEncryptionUtil.decrypt(value);
            }
            config.put(detail.getName(), value);
        }
        validateProfileConfig(config);
        return config;
    }

    /**
     * Validates HSM profile configuration for PKCS#11 provider.
     *
     * <p>
     * Validates:
     * <ul>
     * <li>{@code library}: Required, should point to PKCS#11 library</li>
     * <li>{@code slot}, {@code slot_list_index}, or {@code token_label}: At least
     * one required</li>
     * <li>{@code pin}: Required for HSM authentication</li>
     * <li>{@code max_sessions}: Optional, must be positive integer if provided</li>
     * </ul>
     *
     * @param config Configuration map from HSM profile details
     * @throws KMSException with {@code INVALID_PARAMETER} if validation fails
     */
    @Override
    public void validateProfileConfig(Map<String, String> config) throws KMSException {
        String libraryPath = config.get("library");
        if (StringUtils.isBlank(libraryPath)) {
            throw KMSException.invalidParameter("library is required for PKCS#11 HSM profile");
        }

        String slot = config.get("slot");
        String slotListIndex = config.get("slot_list_index");
        String tokenLabel = config.get("token_label");
        if (StringUtils.isAllBlank(slot, slotListIndex, tokenLabel)) {
            throw KMSException.invalidParameter(
                    "One of 'slot', 'slot_list_index', or 'token_label' is required for PKCS#11 HSM profile");
        }

        if (StringUtils.isNotBlank(slot)) {
            try {
                Integer.parseInt(slot);
            } catch (NumberFormatException e) {
                throw KMSException.invalidParameter("slot must be a valid integer: " + slot);
            }
        }

        if (StringUtils.isNotBlank(slotListIndex)) {
            try {
                int idx = Integer.parseInt(slotListIndex);
                if (idx < 0) {
                    throw KMSException.invalidParameter("slot_list_index must be a non-negative integer");
                }
            } catch (NumberFormatException e) {
                throw KMSException.invalidParameter("slot_list_index must be a valid integer: " + slotListIndex);
            }
        }

        File libraryFile = new File(libraryPath);
        if (!libraryFile.exists() && !libraryFile.isAbsolute()) {
            // The HSM library might be in the system library path
            logger.debug("Library path {} does not exist as absolute path, will rely on system library path",
                    libraryPath);
        }

        String max_sessions = config.get("max_sessions");
        if (StringUtils.isNotBlank(max_sessions)) {
            try {
                int idx = Integer.parseInt(max_sessions);
                if (idx <= 0) {
                    throw KMSException.invalidParameter("max_sessions must be greater than 0");
                }
            } catch (NumberFormatException e) {
                throw KMSException.invalidParameter("max_sessions must be a valid integer: " + max_sessions);
            }
        }
    }

    boolean isSensitiveKey(String key) {
        return KMSProvider.isSensitiveKey(key);
    }



    @Override
    public String getConfigComponentName() {
        return PKCS11HSMProvider.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[0];
    }

    @FunctionalInterface
    private interface SessionOperation<T> {
        T execute(PKCS11Session session) throws KMSException;
    }

    private static class HSMSessionPool {
        private final BlockingQueue<PKCS11Session> availableSessions;
        private final Long profileId;
        private final PKCS11HSMProvider provider;
        private final int maxSessions;
        // Counts total sessions (idle + active). Acquired on creation, released on close.
        private final Semaphore sessionPermits;
        private volatile boolean invalidated = false;

        HSMSessionPool(Long profileId, int maxSessions, PKCS11HSMProvider provider) {
            this.profileId = profileId;
            this.provider = provider;
            this.maxSessions = maxSessions;
            this.sessionPermits = new Semaphore(maxSessions);
            this.availableSessions = new ArrayBlockingQueue<>(maxSessions);
        }

        PKCS11Session acquireSession(long timeoutMs) throws KMSException {
            // Try to get an existing idle session first (no semaphore change: it already owns a permit).
            PKCS11Session session = availableSessions.poll();
            if (session != null) {
                if (session.isValid()) {
                    return session;
                }
                // Stale idle session: discard it and free its permit so a new one can be created.
                session.close();
                sessionPermits.release();
            }

            // Acquire a permit to create a new session, blocking up to timeoutMs if at capacity.
            try {
                if (!sessionPermits.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                    // One last try: a session may have been returned while we were waiting.
                    session = availableSessions.poll();
                    if (session != null && session.isValid()) {
                        return session;
                    }
                    if (session != null) {
                        session.close();
                        sessionPermits.release();
                    }
                    throw new KMSException(KMSException.ErrorType.CONNECTION_FAILED,
                            "Timed out waiting for an available HSM session for profile " + profileId
                            + " (max=" + maxSessions + ", timeout=" + timeoutMs + "ms)");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new KMSException(KMSException.ErrorType.CONNECTION_FAILED,
                        "Interrupted while waiting to acquire HSM session for profile " + profileId, e);
            }

            try {
                return createNewSession();
            } catch (KMSException e) {
                sessionPermits.release();
                throw e;
            }
        }

        private PKCS11Session createNewSession() throws KMSException {
            // Config (including decrypted PIN) is loaded fresh each time and not stored.
            return new PKCS11Session(provider.loadProfileConfig(profileId));
        }

        void releaseSession(PKCS11Session session) {
            if (session == null) return;
            if (!invalidated && session.isValid() && availableSessions.offer(session)) {
                return; // session returned to the idle pool; permit stays consumed
            }
            // Pool is invalidated, session is stale, or the idle queue is full: close immediately.
            session.close();
            sessionPermits.release();
        }

        /**
         * Marks the pool as invalidated and closes all idle sessions.
         * Any session currently checked out will be closed (and its permit released) when
         * it is returned via {@link #releaseSession} — the invalidated flag prevents re-pooling.
         */
        void invalidate() {
            invalidated = true;
            PKCS11Session session;
            while ((session = availableSessions.poll()) != null) {
                session.close();
                sessionPermits.release();
            }
        }
    }

    /**
     * Inner class representing an active PKCS#11 session with an HSM.
     * This class manages the connection to the HSM, key operations, and session lifecycle.
     *
     * <p>Key operations supported:
     * <ul>
     *   <li>Key generation: Generate AES keys directly in the HSM</li>
     *   <li>Key wrapping: Encrypt DEKs using KEKs stored in the HSM (AES-CBC/PKCS5Padding)</li>
     *   <li>Key unwrapping: Decrypt DEKs using KEKs stored in the HSM (AES-CBC/PKCS5Padding)</li>
     *   <li>Key deletion: Remove keys from the HSM</li>
     *   <li>Key existence check: Verify if a key exists in the HSM</li>
     * </ul>
     *
     * <p>Configuration requirements:
     * <ul>
     *   <li>{@code library}: Path to PKCS#11 library (required)</li>
     *   <li>{@code slot} or {@code token_label}: HSM slot/token selection (at least one required)</li>
     *   <li>{@code pin}: PIN for HSM authentication (required, sensitive)</li>
     * </ul>
     *
     * <p>Error handling: PKCS#11 specific error codes are mapped to appropriate
     * {@link KMSException.ErrorType} values for proper retry logic and error reporting.
     */
    private static class PKCS11Session {
        private static final int IV_LENGTH = 16; // 128 bits for CBC mode

        private KeyStore keyStore;
        private Provider provider;
        private String providerName;
        private Path tempConfigFile;

        /**
         * Creates a new PKCS#11 session and connects to the HSM.
         * The config map (including any sensitive values such as the PIN) is used only
         * during connection setup and is not retained as a field.
         *
         * @param config HSM profile configuration containing library, slot/token_label, and pin
         * @throws KMSException if connection fails or configuration is invalid
         */
        PKCS11Session(Map<String, String> config) throws KMSException {
            connect(config);
        }

        /**
         * Establishes connection to the PKCS#11 HSM.
         *
         * <p>This method:
         * <ol>
         *   <li>Validates required configuration (library, slot/token_label, pin)</li>
         *   <li>Creates a SunPKCS11 provider with the HSM library</li>
         *   <li>Loads the PKCS#11 KeyStore</li>
         *   <li>Authenticates using the provided PIN</li>
         * </ol>
         *
         * <p>Slot/token selection:
         * <ul>
         *   <li>If {@code token_label} is provided, it is used (more reliable)</li>
         *   <li>Otherwise, {@code slot} (numeric ID) is used</li>
         * </ul>
         *
         * @throws KMSException with appropriate ErrorType:
         *                      <ul>
         *                        <li>{@code AUTHENTICATION_FAILED} if PIN is incorrect</li>
         *                        <li>{@code INVALID_PARAMETER} if configuration is missing or invalid</li>
         *                        <li>{@code CONNECTION_FAILED} if HSM is unreachable or device error occurs</li>
         *                      </ul>
         */
        private void connect(Map<String, String> config) throws KMSException {
            try {
                // Unique suffix ensures each session gets its own provider name in java.security.Security,
                // allowing Security.removeProvider() in close() to target exactly this session's provider.
                String nameSuffix = UUID.randomUUID().toString().substring(0, 8);

                String configString = buildSunPKCS11Config(config, nameSuffix);

                // Java 9+ API: write config to temp file, then configure the provider
                tempConfigFile = Files.createTempFile("pkcs11-config-", ".cfg");
                try (FileWriter writer = new FileWriter(tempConfigFile.toFile(), StandardCharsets.UTF_8)) {
                    writer.write(configString);
                }

                Provider baseProvider = Security.getProvider("SunPKCS11");
                if (baseProvider == null) {
                    throw new KMSException(KMSException.ErrorType.CONNECTION_FAILED,
                            "SunPKCS11 provider not available in this JVM");
                }

                provider = baseProvider.configure(tempConfigFile.toAbsolutePath().toString());

                // Use the actual provider name so Security.removeProvider() in close() works correctly.
                providerName = provider.getName();

                // Security.addProvider returns -1 if a provider with this name is already registered.
                // With the UUID-based suffix this should be impossible in practice; guard defensively.
                if (Security.addProvider(provider) < 0) {
                    throw new KMSException(KMSException.ErrorType.CONNECTION_FAILED,
                            "Failed to register PKCS#11 provider '" + providerName + "': name already in use");
                }

                keyStore = KeyStore.getInstance("PKCS11", provider);

                String pin = config.get("pin");
                if (StringUtils.isEmpty(pin)) {
                    throw KMSException.invalidParameter("pin is required");
                }
                char[] pinChars = pin.toCharArray();
                keyStore.load(null, pinChars);
                Arrays.fill(pinChars, '\0');

                // The temp file is only needed during configure()/load(); delete it immediately
                // rather than holding it until the session is eventually closed.
                Files.deleteIfExists(tempConfigFile);
                tempConfigFile = null;

                logger.debug("Successfully connected to PKCS#11 HSM at {}", config.get("library"));
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
                handlePKCS11Exception(e, "Failed to initialize PKCS#11 connection");
            } catch (IOException e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("CKR_PIN_INCORRECT")) {
                    throw new KMSException(KMSException.ErrorType.AUTHENTICATION_FAILED,
                            "Incorrect PIN for HSM authentication", e);
                } else if (errorMsg != null && errorMsg.contains("CKR_SLOT_ID_INVALID")) {
                    throw KMSException.invalidParameter("Invalid slot ID: " + config.get("slot"));
                } else {
                    handlePKCS11Exception(e, "I/O error during PKCS#11 connection");
                }
            } catch (Exception e) {
                handlePKCS11Exception(e, "Unexpected error during PKCS#11 connection");
            }
        }

        /**
         * Builds SunPKCS11 provider configuration string.
         *
         * @param config HSM profile configuration
         * @return Configuration string for SunPKCS11 provider
         * @throws KMSException if required configuration is missing
         */
        private String buildSunPKCS11Config(Map<String, String> config, String nameSuffix) throws KMSException {
            String libraryPath = config.get("library");
            if (StringUtils.isBlank(libraryPath)) {
                throw KMSException.invalidParameter("library is required");
            }

            StringBuilder configBuilder = new StringBuilder();
            // Include the unique suffix so that each session is registered under a distinct
            // provider name (SunPKCS11-CloudStackHSM-{suffix}), preventing name collisions
            // across concurrent sessions and allowing clean removal via Security.removeProvider().
            configBuilder.append("name=CloudStackHSM-").append(nameSuffix).append("\n");
            configBuilder.append("library=").append(libraryPath).append("\n");

            String tokenLabel = config.get("token_label");
            String slotListIndex = config.get("slot_list_index");
            String slot = config.get("slot");

            if (StringUtils.isNotBlank(tokenLabel)) {
                configBuilder.append("tokenLabel=").append(tokenLabel).append("\n");
            } else if (StringUtils.isNotBlank(slotListIndex)) {
                configBuilder.append("slotListIndex=").append(slotListIndex).append("\n");
            } else if (StringUtils.isNotBlank(slot)) {
                configBuilder.append("slot=").append(slot).append("\n");
            } else {
                throw KMSException.invalidParameter("One of 'slot', 'slot_list_index', or 'token_label' is required");
            }

            return configBuilder.toString();
        }

        /**
         * Maps PKCS#11 specific exceptions to appropriate KMSException.ErrorType.
         *
         * <p>PKCS#11 error codes are parsed from exception messages and mapped as follows:
         * <ul>
         *   <li>{@code CKR_PIN_INCORRECT} → {@code AUTHENTICATION_FAILED}</li>
         *   <li>{@code CKR_SLOT_ID_INVALID} → {@code INVALID_PARAMETER}</li>
         *   <li>{@code CKR_KEY_NOT_FOUND} → {@code KEK_NOT_FOUND}</li>
         *   <li>{@code CKR_DEVICE_ERROR} → {@code CONNECTION_FAILED}</li>
         *   <li>{@code CKR_SESSION_HANDLE_INVALID} → {@code CONNECTION_FAILED}</li>
         *   <li>{@code CKR_KEY_ALREADY_EXISTS} → {@code KEY_ALREADY_EXISTS}</li>
         *   <li>{@code KeyStoreException} → {@code WRAP_UNWRAP_FAILED}</li>
         *   <li>Other errors → {@code KEK_OPERATION_FAILED}</li>
         * </ul>
         *
         * @param e       The exception to map
         * @param context Context description for the error message
         * @throws KMSException with appropriate ErrorType and detailed message
         */
        private void handlePKCS11Exception(Exception e, String context) throws KMSException {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getSimpleName();
            }
            logger.warn("PKCS#11 error: {} - {}", errorMsg, context, e);

            if (errorMsg.contains("CKR_PIN_INCORRECT") || errorMsg.contains("PIN_INCORRECT")) {
                throw new KMSException(KMSException.ErrorType.AUTHENTICATION_FAILED,
                        context + ": Incorrect PIN", e);
            } else if (errorMsg.contains("CKR_SLOT_ID_INVALID") || errorMsg.contains("SLOT_ID_INVALID")) {
                throw KMSException.invalidParameter(context + ": Invalid slot ID");
            } else if (errorMsg.contains("CKR_KEY_NOT_FOUND") || errorMsg.contains("KEY_NOT_FOUND")) {
                throw KMSException.kekNotFound(context + ": Key not found");
            } else if (errorMsg.contains("CKR_DEVICE_ERROR") || errorMsg.contains("DEVICE_ERROR")) {
                throw new KMSException(KMSException.ErrorType.CONNECTION_FAILED,
                        context + ": HSM device error", e);
            } else if (errorMsg.contains("CKR_SESSION_HANDLE_INVALID") || errorMsg.contains("SESSION_HANDLE_INVALID")) {
                throw new KMSException(KMSException.ErrorType.CONNECTION_FAILED,
                        context + ": Invalid session handle", e);
            } else if (errorMsg.contains("CKR_KEY_ALREADY_EXISTS") || errorMsg.contains("KEY_ALREADY_EXISTS")) {
                throw KMSException.keyAlreadyExists(context);
            } else if (e instanceof KeyStoreException) {
                throw new KMSException(KMSException.ErrorType.WRAP_UNWRAP_FAILED,
                        context + ": " + errorMsg, e);
            } else {
                throw new KMSException(KMSException.ErrorType.KEK_OPERATION_FAILED,
                        context + ": " + errorMsg, e);
            }
        }

        /**
         * Validates that the PKCS#11 session is still active and connected to the HSM.
         *
         * <p>Checks performed:
         * <ul>
         *   <li>KeyStore object is not null</li>
         *   <li>Provider is still registered in Security</li>
         *   <li>HSM is responsive (lightweight operation: get KeyStore size)</li>
         * </ul>
         *
         * @return true if session is valid and HSM is accessible, false otherwise
         */
        boolean isValid() {
            try {
                if (keyStore == null) {
                    return false;
                }

                if (provider == null || Security.getProvider(provider.getName()) == null) {
                    return false;
                }

                keyStore.size();
                return true;
            } catch (Exception e) {
                logger.debug("Session validation failed: {}", e.getMessage());
                return false;
            }
        }

        /**
         * Closes the PKCS#11 session and cleans up resources.
         *
         * <p>
         * Note: Errors during cleanup are logged but do not throw exceptions
         * to ensure cleanup continues even if some steps fail.
         */
        void close() {
            try {
                if (keyStore instanceof Closeable) {
                    ((Closeable) keyStore).close();
                }

                if (provider != null && providerName != null) {
                    try {
                        Security.removeProvider(providerName);
                    } catch (Exception e) {
                        logger.debug("Failed to remove provider {}: {}", providerName, e.getMessage());
                    }
                }

                if (tempConfigFile != null) {
                    try {
                        Files.deleteIfExists(tempConfigFile);
                    } catch (IOException e) {
                        logger.debug("Failed to delete temporary config file {}: {}", tempConfigFile, e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.warn("Error during session close: {}", e.getMessage());
            } finally {
                keyStore = null;
                provider = null;
                providerName = null;
                tempConfigFile = null;
            }
        }

        /**
         * Generates an AES key directly in the HSM with the specified label.
         *
         * <p>
         * This method generates the key natively inside the HSM using a
         * {@link KeyGenerator} configured with the PKCS#11 provider, so the key
         * material never leaves the HSM boundary. The returned PKCS#11-native key
         * reference ({@code P11Key}) is then stored in the KeyStore under the
         * requested label.
         *
         * <p>
         * Using {@code KeyGenerator} with the HSM provider is required for
         * HSMs such as NetHSM that do not support importing raw secret-key bytes
         * via {@code KeyStore.setKeyEntry()}. By generating the key on the HSM first,
         * the value passed to {@code setKeyEntry()} is already a PKCS#11 token object,
         * so no raw-bytes import is attempted.
         *
         * <p>
         * Once stored, the key:
         * <ul>
         * <li>Resides permanently in the HSM token storage</li>
         * <li>Is marked as non-extractable (CKA_EXTRACTABLE=false) by the HSM</li>
         * <li>Can only be used for cryptographic operations via the HSM</li>
         * </ul>
         *
         * @param label   Unique label for the key in the HSM
         * @param keyBits Key size in bits (128, 192, or 256)
         * @param purpose Key purpose (for logging/auditing)
         * @return The label of the generated key
         * @throws KMSException if generation fails or key already exists
         */
        String generateKey(String label, int keyBits, KeyPurpose purpose) throws KMSException {
            validateKeySize(keyBits);

            try {
                // Check if key with this label already exists
                if (keyStore.containsAlias(label)) {
                    throw KMSException.keyAlreadyExists("Key with label '" + label + "' already exists in HSM");
                }

                // Generate the AES key natively inside the HSM using the PKCS#11 provider.
                // This avoids importing raw key bytes into the HSM, which is not supported
                // by all HSMs (e.g. NetHSM rejects SecretKeySpec via storeSkey()).
                // The resulting key is a PKCS#11-native P11Key that lives inside the token.
                KeyGenerator keyGen = KeyGenerator.getInstance("AES", provider);
                keyGen.init(keyBits);
                SecretKey hsmKey = keyGen.generateKey();

                // Associate the HSM-generated key with the requested label by storing
                // it in the PKCS#11 KeyStore. Because hsmKey is already a P11Key
                // (not a software SecretKeySpec), P11KeyStore.storeSkey() stores it
                // as a persistent token object (CKA_TOKEN=true) with CKA_LABEL=label
                // without attempting any raw-bytes conversion.
                keyStore.setKeyEntry(label, hsmKey, null, null);

                logger.info("Generated AES-{} key '{}' in HSM (purpose: {})",
                        keyBits, label, purpose);
                return label;

            } catch (KeyStoreException e) {
                if (e.getMessage() != null
                        && e.getMessage().contains("found multiple secret keys sharing same CKA_LABEL")) {
                    logger.warn("Multiple duplicate keys found with label '{}' in HSM. Reusing the existing key. " +
                            "Please purge duplicate keys manually if possible.", label);
                    return label;
                }
                handlePKCS11Exception(e, "Failed to store key in HSM KeyStore");
            } catch (NoSuchAlgorithmException e) {
                handlePKCS11Exception(e, "AES KeyGenerator not available via PKCS#11 provider");
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("CKR_OBJECT_HANDLE_INVALID")
                                         || errorMsg.contains("already exists"))) {
                    throw KMSException.keyAlreadyExists("Key with label '" + label + "' already exists in HSM");
                } else {
                    handlePKCS11Exception(e, "Failed to generate key in HSM");
                }
            }
            return null;
        }

        /**
         * Validates that the key size is one of the supported AES key sizes.
         *
         * @param keyBits Key size in bits
         * @throws KMSException if key size is invalid
         */
        private void validateKeySize(int keyBits) throws KMSException {
            if (Arrays.stream(VALID_KEY_SIZES).noneMatch(size -> size == keyBits)) {
                throw KMSException.invalidParameter("Key size must be 128, 192, or 256 bits");
            }
        }

        /**
         * Wraps (encrypts) a plaintext DEK using a KEK stored in the HSM.
         *
         * <p>Uses AES-CBC with PKCS5Padding (FIPS 197 + NIST SP 800-38A):
         * <ul>
         *   <li>Generates a random 128-bit IV</li>
         *   <li>Encrypts the DEK using AES-CBC with the KEK from HSM</li>
         *   <li>Returns format: [IV (16 bytes)][ciphertext]</li>
         * </ul>
         *
         * <p>Security: The plaintext DEK should be zeroized by the caller after wrapping.
         *
         * @param plainDek Plaintext DEK to wrap (will be encrypted)
         * @param kekLabel Label of the KEK stored in the HSM
         * @return Wrapped key blob: [IV][ciphertext]
         * @throws KMSException with appropriate ErrorType:
         *                      <ul>
         *                        <li>{@code INVALID_PARAMETER} if plainDek is null or empty</li>
         *                        <li>{@code KEK_NOT_FOUND} if KEK with label doesn't exist or is not accessible</li>
         *                        <li>{@code WRAP_UNWRAP_FAILED} if wrapping operation fails</li>
         *                      </ul>
         */
        byte[] wrapKey(byte[] plainDek, String kekLabel) throws KMSException {
            if (plainDek == null || plainDek.length == 0) {
                throw KMSException.invalidParameter("Plain DEK cannot be null or empty");
            }

            SecretKey kek = getKekFromKeyStore(kekLabel);
            try {
                byte[] iv = new byte[IV_LENGTH];
                new SecureRandom().nextBytes(iv);

                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, provider);
                cipher.init(Cipher.ENCRYPT_MODE, kek, new IvParameterSpec(iv));
                byte[] ciphertext = cipher.doFinal(plainDek);

                byte[] result = new byte[IV_LENGTH + ciphertext.length];
                System.arraycopy(iv, 0, result, 0, IV_LENGTH);
                System.arraycopy(ciphertext, 0, result, IV_LENGTH, ciphertext.length);

                logger.debug("Wrapped key with KEK '{}' using AES-CBC", kekLabel);
                return result;
            } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
                handlePKCS11Exception(e, "Invalid key or data for wrapping");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                handlePKCS11Exception(e, "AES-CBC not supported by HSM");
            } catch (InvalidAlgorithmParameterException e) {
                handlePKCS11Exception(e, "Invalid IV for CBC mode");
            } catch (Exception e) {
                handlePKCS11Exception(e, "Failed to wrap key with HSM");
            } finally {
                kek = null;
            }
            return null;
        }

        /**
         * Retrieves a KEK (Key Encryption Key) from the HSM KeyStore.
         *
         * @param kekLabel Label of the KEK to retrieve
         * @return SecretKey representing the KEK
         * @throws KMSException if KEK is not found or not accessible
         */
        private SecretKey getKekFromKeyStore(String kekLabel) throws KMSException {
            try {
                Key key = keyStore.getKey(kekLabel, null);
                if (key == null) {
                    throw KMSException.kekNotFound("KEK with label '" + kekLabel + "' not found in HSM");
                }
                if (!(key instanceof SecretKey)) {
                    throw KMSException.kekNotFound("Key with label '" + kekLabel + "' is not a secret key");
                }
                return (SecretKey) key;
            } catch (UnrecoverableKeyException e) {
                throw KMSException.kekNotFound("KEK with label '" + kekLabel + "' is not accessible");
            } catch (NoSuchAlgorithmException e) {
                handlePKCS11Exception(e, "Algorithm not supported");
            } catch (KeyStoreException e) {
                handlePKCS11Exception(e, "Failed to retrieve KEK from HSM");
            }
            return null;
        }

        /**
         * Unwraps (decrypts) a wrapped DEK using a KEK stored in the HSM.
         *
         * <p>
         * Uses AES-CBC with PKCS5Padding. Expected format: [IV (16 bytes)][ciphertext].
         *
         * <p>
         * Security: The returned plaintext DEK must be zeroized by the caller after
         * use.
         *
         * @param wrappedBlob Wrapped DEK blob (IV + ciphertext)
         * @param kekLabel    Label of the KEK stored in the HSM
         * @return Plaintext DEK
         * @throws KMSException with appropriate ErrorType:
         *                      <ul>
         *                      <li>{@code INVALID_PARAMETER} if wrappedBlob is null,
         *                      empty, or too short</li>
         *                      <li>{@code KEK_NOT_FOUND} if KEK with label doesn't
         *                      exist or is not accessible</li>
         *                      <li>{@code WRAP_UNWRAP_FAILED} if unwrapping fails</li>
         *                      </ul>
         */
        byte[] unwrapKey(byte[] wrappedBlob, String kekLabel) throws KMSException {
            if (wrappedBlob == null || wrappedBlob.length == 0) {
                throw KMSException.invalidParameter("Wrapped blob cannot be null or empty");
            }

            // Minimum size: IV (16 bytes) + at least one AES block (16 bytes)
            if (wrappedBlob.length < IV_LENGTH + 16) {
                throw KMSException.invalidParameter("Wrapped blob too short: expected at least " +
                                                    (IV_LENGTH + 16) + " bytes");
            }

            SecretKey kek = getKekFromKeyStore(kekLabel);
            try {
                byte[] iv = new byte[IV_LENGTH];
                System.arraycopy(wrappedBlob, 0, iv, 0, IV_LENGTH);
                byte[] ciphertext = new byte[wrappedBlob.length - IV_LENGTH];
                System.arraycopy(wrappedBlob, IV_LENGTH, ciphertext, 0, ciphertext.length);

                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, provider);
                cipher.init(Cipher.DECRYPT_MODE, kek, new IvParameterSpec(iv));
                byte[] plainDek = cipher.doFinal(ciphertext);

                logger.debug("Unwrapped key with KEK '{}' using AES-CBC", kekLabel);
                return plainDek;
            } catch (BadPaddingException e) {
                throw KMSException.wrapUnwrapFailed(
                        "Decryption failed: wrapped key may be corrupted or KEK is incorrect", e);
            } catch (IllegalBlockSizeException | InvalidKeyException e) {
                handlePKCS11Exception(e, "Invalid key or data for unwrapping");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                handlePKCS11Exception(e, "AES-CBC not supported by HSM");
            } catch (InvalidAlgorithmParameterException e) {
                handlePKCS11Exception(e, "Invalid IV for CBC mode");
            } catch (Exception e) {
                handlePKCS11Exception(e, "Failed to unwrap key with HSM");
            } finally {
                kek = null;
            }
            return null;
        }

        /**
         * Deletes a key from the HSM.
         *
         * <p><strong>Warning:</strong> Deleting a KEK makes all DEKs wrapped with that KEK
         * permanently unrecoverable. This operation should be used with extreme caution.
         *
         * @param label Label of the key to delete
         * @throws KMSException with appropriate ErrorType:
         *                      <ul>
         *                        <li>{@code KEK_NOT_FOUND} if key with label doesn't exist</li>
         *                        <li>{@code KEK_OPERATION_FAILED} if deletion fails (e.g., key is in use)</li>
         *                      </ul>
         */
        void deleteKey(String label) throws KMSException {
            try {
                if (!keyStore.containsAlias(label)) {
                    throw KMSException.kekNotFound("Key with label '" + label + "' not found in HSM");
                }

                keyStore.deleteEntry(label);

                logger.debug("Deleted key '{}' from HSM", label);
            } catch (KeyStoreException e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("not found")) {
                    throw KMSException.kekNotFound("Key with label '" + label + "' not found in HSM");
                } else if (errorMsg != null && errorMsg.contains("in use")) {
                    throw KMSException.kekOperationFailed(
                            "Key with label '" + label + "' is in use and cannot be deleted");
                } else {
                    handlePKCS11Exception(e, "Failed to delete key from HSM");
                }
            } catch (Exception e) {
                handlePKCS11Exception(e, "Failed to delete key from HSM");
            }
        }

        /**
         * Checks if a key with the given label exists and is accessible in the HSM.
         *
         * @param label Label of the key to check
         * @return true if key exists and is accessible, false otherwise
         * @throws KMSException only for unexpected errors (KeyStoreException, etc.)
         *                      Returns false for expected cases (key not found, unrecoverable key)
         */
        boolean checkKeyExists(String label) throws KMSException {
            try {
                Key key = keyStore.getKey(label, null);
                return key != null;
            } catch (KeyStoreException e) {
                logger.debug("KeyStore error while checking key existence: {}", e.getMessage());
                return false;
            } catch (UnrecoverableKeyException e) {
                // Key exists but is not accessible (might be a different key type)
                logger.debug("Key '{}' exists but is not accessible: {}", label, e.getMessage());
                return false;
            } catch (NoSuchAlgorithmException e) {
                logger.debug("Algorithm error while checking key existence: {}", e.getMessage());
                return false;
            } catch (Exception e) {
                logger.debug("Unexpected error while checking key existence: {}", e.getMessage());
                return false;
            }
        }
    }
}
