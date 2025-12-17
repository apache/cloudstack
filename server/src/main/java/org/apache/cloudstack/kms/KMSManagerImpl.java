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

package org.apache.cloudstack.kms;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.kms.CreateKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.DeleteKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.ListKMSKeysCmd;
import org.apache.cloudstack.api.command.user.kms.UpdateKMSKeyCmd;
import org.apache.cloudstack.api.response.KMSKeyResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.apache.cloudstack.kms.dao.KMSKeyDao;
import org.apache.cloudstack.kms.dao.KMSWrappedKeyDao;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of KMS Manager.
 * Provides high-level KMS operations with provider abstraction, zone-scoping,
 * retry logic, and audit logging.
 */
public class KMSManagerImpl extends ManagerBase implements KMSManager, PluggableService {
    private static final Logger logger = LogManager.getLogger(KMSManagerImpl.class);
    private static final Map<String, KMSProvider> kmsProviderMap = new HashMap<>();
    private static KMSProvider configuredKmsProvider;
    @Inject
    private KMSWrappedKeyDao kmsWrappedKeyDao;
    @Inject
    private KMSKeyDao kmsKeyDao;
    @Inject
    private KMSKekVersionDao kmsKekVersionDao;
    @Inject
    private AccountManager accountManager;
    @Inject
    private ResponseGenerator responseGenerator;
    private List<KMSProvider> kmsProviders;

    // ==================== Provider Management ====================

    @Override
    public List<? extends KMSProvider> listKMSProviders() {
        return kmsProviders;
    }

    @Override
    public KMSProvider getKMSProvider(String name) {
        if (StringUtils.isEmpty(name)) {
            return getConfiguredKmsProvider();
        }

        String providerName = name.toLowerCase();
        if (!kmsProviderMap.containsKey(providerName)) {
            throw new CloudRuntimeException(String.format("KMS provider '%s' not found", providerName));
        }

        KMSProvider provider = kmsProviderMap.get(providerName);
        if (provider == null) {
            throw new CloudRuntimeException(String.format("KMS provider '%s' returned is null", providerName));
        }

        return provider;
    }

    @Override
    public KMSProvider getKMSProviderForZone(Long zoneId) throws KMSException {
        // For now, use global provider
        // In future, could support zone-specific providers via zone-scoped config
        return getConfiguredKmsProvider();
    }

    @Override
    public boolean isKmsEnabled(Long zoneId) {
        if (zoneId == null) {
            return false;
        }
        return KMSEnabled.valueIn(zoneId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEK_CREATE, eventDescription = "creating KEK", async = false)
    public String createKek(Long zoneId, KeyPurpose purpose, String label, int keyBits) throws KMSException {
        validateKmsEnabled(zoneId);

        KMSProvider provider = getKMSProviderForZone(zoneId);

        try {
            logger.info("Creating KEK for zone {} with purpose {} and {} bits", zoneId, purpose, keyBits);
            return retryOperation(() -> provider.createKek(purpose, label, keyBits));
        } catch (Exception e) {
            logger.error("Failed to create KEK for zone {}: {}", zoneId, e.getMessage());
            throw handleKmsException(e);
        }
    }

    // ==================== KEK Management ====================

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEK_DELETE, eventDescription = "deleting KEK", async = false)
    public void deleteKek(Long zoneId, String kekId) throws KMSException {
        validateKmsEnabled(zoneId);

        // TODO: Check if any wrapped keys use this KEK
        // This requires finding KMSKeyVO by kekLabel first, then checking wrapped keys
        // For now, allow deletion (will be fixed in Phase 5)

        KMSProvider provider = getKMSProviderForZone(zoneId);

        try {
            logger.warn("Deleting KEK {} for zone {}", kekId, zoneId);
            retryOperation(() -> {
                provider.deleteKek(kekId);
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to delete KEK {} for zone {}: {}", kekId, zoneId, e.getMessage());
            throw handleKmsException(e);
        }
    }

    @Override
    public List<String> listKeks(Long zoneId, KeyPurpose purpose) throws KMSException {
        validateKmsEnabled(zoneId);

        KMSProvider provider = getKMSProviderForZone(zoneId);

        try {
            return retryOperation(() -> provider.listKeks(purpose));
        } catch (Exception e) {
            logger.error("Failed to list KEKs for zone {}: {}", zoneId, e.getMessage());
            throw handleKmsException(e);
        }
    }

    @Override
    public boolean isKekAvailable(Long zoneId, String kekId) throws KMSException {
        if (!isKmsEnabled(zoneId)) {
            return false;
        }

        try {
            KMSProvider provider = getKMSProviderForZone(zoneId);
            return provider.isKekAvailable(kekId);
        } catch (Exception e) {
            logger.warn("Error checking KEK availability: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEK_ROTATE, eventDescription = "rotating KEK", async = true)
    public String rotateKek(Long zoneId, KeyPurpose purpose, String oldKekLabel,
            String newKekLabel, int keyBits) throws KMSException {
        validateKmsEnabled(zoneId);

        if (StringUtils.isEmpty(oldKekLabel)) {
            throw KMSException.invalidParameter("oldKekLabel must be specified");
        }

        KMSProvider provider = getKMSProviderForZone(zoneId);

        try {
            logger.info("Starting KEK rotation from {} to {} for zone {} and purpose {}",
                    oldKekLabel, newKekLabel, zoneId, purpose);

            // Find KMS key by old KEK label
            KMSKeyVO kmsKey = kmsKeyDao.findByKekLabel(oldKekLabel, provider.getProviderName());
            if (kmsKey == null) {
                throw KMSException.kekNotFound("KMS key not found for KEK label: " + oldKekLabel);
            }

            // Generate new KEK label if not provided
            if (StringUtils.isEmpty(newKekLabel)) {
                newKekLabel = purpose.getName() + "-kek-" + UUID.randomUUID().toString().substring(0, 8);
            }

            // Create new KEK in provider
            String newKekId = provider.createKek(purpose, newKekLabel, keyBits);

            // Create new KEK version (marks old as Previous, new as Active)
            KMSKekVersionVO newVersion = createKekVersion(kmsKey.getId(), newKekId, keyBits);

            logger.info("KEK rotation: KMS key {} now has {} versions (active: v{}, previous: v{})",
                    kmsKey.getUuid(), newVersion.getVersionNumber(), newVersion.getVersionNumber(),
                    newVersion.getVersionNumber() - 1);

            // TODO: Schedule background job to rewrap all DEKs (Phase 5)
            // This will gradually rewrap wrapped keys to use the new KEK version

            return newKekId;

        } catch (Exception e) {
            logger.error("KEK rotation failed for zone {}: {}", zoneId, e.getMessage());
            throw handleKmsException(e);
        }
    }

    // ==================== DEK Operations ====================

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEY_UNWRAP, eventDescription = "unwrapping volume key", async = false)
    public byte[] unwrapVolumeKey(WrappedKey wrappedKey, Long zoneId) throws KMSException {
        validateKmsEnabled(zoneId);

        return unwrapDek(wrappedKey);
    }

    private byte[] unwrapDek(WrappedKey wrappedKey) throws KMSException {
        // Determine provider from wrapped key
        String providerName = wrappedKey.getProviderName();
        KMSProvider provider = getKMSProvider(providerName);

        try {
            logger.debug("Unwrapping {} key", wrappedKey.getPurpose());
            return retryOperation(() -> provider.unwrapKey(wrappedKey));
        } catch (Exception e) {
            logger.error("Failed to unwrap key: {}", e.getMessage());
            throw handleKmsException(e);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_HEALTH_CHECK, eventDescription = "KMS health check", async = false)
    public boolean healthCheck(Long zoneId) throws KMSException {
        if (!isKmsEnabled(zoneId)) {
            logger.debug("KMS is not enabled for zone {}", zoneId);
            return false;
        }

        try {
            KMSProvider provider = getKMSProviderForZone(zoneId);
            return provider.healthCheck();
        } catch (Exception e) {
            logger.error("Health check failed for zone {}: {}", zoneId, e.getMessage());
            throw handleKmsException(e);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEK_CREATE, eventDescription = "creating user KMS key", async = false)
    public KMSKey createUserKMSKey(Long accountId, Long domainId, Long zoneId,
            String name, String description, KeyPurpose purpose,
            Integer keyBits) throws KMSException {
        validateKmsEnabled(zoneId);

        KMSProvider provider = getKMSProviderForZone(zoneId);

        // Generate unique KEK label
        String kekLabel = purpose.getName() + "-kek-" + UUID.randomUUID().toString().substring(0, 8);

        // Create KEK in provider
        String providerKekLabel;
        try {
            providerKekLabel = retryOperation(() -> provider.createKek(purpose, kekLabel, keyBits));
        } catch (Exception e) {
            throw handleKmsException(e);
        }

        // Create metadata entry
        KMSKeyVO kmsKey = new KMSKeyVO(name, description, providerKekLabel, purpose,
                accountId, domainId, zoneId, provider.getProviderName(),
                "AES/GCM/NoPadding", keyBits);
        kmsKey = kmsKeyDao.persist(kmsKey);

        // Create initial KEK version (version 1, status=Active)
        KMSKekVersionVO initialVersion = new KMSKekVersionVO(kmsKey.getId(), 1, providerKekLabel,
                KMSKekVersionVO.Status.Active);
        initialVersion = kmsKekVersionDao.persist(initialVersion);

        logger.info("Created KMS key '{}' (UUID: {}) with initial KEK version {} for account {} in zone {}",
                name, kmsKey.getUuid(), initialVersion.getVersionNumber(), accountId, zoneId);
        return kmsKey;
    }

    @Override
    public List<? extends KMSKey> listUserKMSKeys(Long accountId, Long domainId, Long zoneId,
            KeyPurpose purpose, KMSKey.State state) {
        // List keys accessible to the account (owned by account or in domain)
        return kmsKeyDao.listAccessibleKeys(accountId, domainId, zoneId, purpose, state);
    }

    // ==================== Health Check ====================

    @Override
    public KMSKey getUserKMSKey(String uuid, Long callerAccountId) {
        KMSKeyVO key = kmsKeyDao.findByUuid(uuid);
        if (key == null || key.getState() == KMSKey.State.Deleted) {
            return null;
        }
        // Check permission
        if (!hasPermission(callerAccountId, uuid)) {
            return null;
        }
        return key;
    }

    // ==================== Helper Methods ====================

    @Override
    public boolean hasPermission(Long callerAccountId, String keyUuid) {
        KMSKeyVO key = kmsKeyDao.findByUuid(keyUuid);
        if (key == null || key.getState() == KMSKey.State.Deleted) {
            return false;
        }

        // Owner always has permission
        if (key.getAccountId() == callerAccountId) {
            return true;
        }

        // TODO: Domain admin can access keys in their domain/subdomains
        // For now, only owner has permission
        return false;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEK_DELETE, eventDescription = "deleting user KMS key", async = false)
    public void deleteUserKMSKey(String uuid, Long callerAccountId) throws KMSException {
        KMSKeyVO key = kmsKeyDao.findByUuid(uuid);
        if (key == null) {
            throw KMSException.kekNotFound("KMS key not found: " + uuid);
        }

        // Check permission
        if (!hasPermission(callerAccountId, uuid)) {
            throw KMSException.invalidParameter("No permission to delete KMS key: " + uuid);
        }

        // Check if key is in use
        long wrappedKeyCount = kmsKeyDao.countWrappedKeysByKmsKey(key.getId());
        if (wrappedKeyCount > 0) {
            throw KMSException.invalidParameter("Cannot delete KMS key: " + wrappedKeyCount +
                                                " wrapped key(s) still reference this key");
        }

        // Soft delete
        key.setState(KMSKey.State.Deleted);
        key.setRemoved(new java.util.Date());
        kmsKeyDao.update(key.getId(), key);

        // Optionally delete KEK from provider (but keep metadata for audit)
        // provider.deleteKek(key.getKekLabel());

        logger.info("Deleted KMS key '{}' (UUID: {})", key.getName(), uuid);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEK_CREATE, eventDescription = "updating user KMS key", async = false)
    public KMSKey updateUserKMSKey(String uuid, Long callerAccountId,
            String name, String description, KMSKey.State state) throws KMSException {
        KMSKeyVO key = kmsKeyDao.findByUuid(uuid);
        if (key == null) {
            throw KMSException.kekNotFound("KMS key not found: " + uuid);
        }

        // Check permission
        if (!hasPermission(callerAccountId, uuid)) {
            throw KMSException.invalidParameter("No permission to update KMS key: " + uuid);
        }

        boolean updated = false;
        if (name != null && !name.equals(key.getName())) {
            key.setName(name);
            updated = true;
        }
        if (description != null && !description.equals(key.getDescription())) {
            key.setDescription(description);
            updated = true;
        }
        if (state != null && state != key.getState()) {
            if (state == KMSKey.State.Deleted) {
                throw KMSException.invalidParameter("Cannot set state to Deleted. Use deleteKMSKey instead.");
            }
            key.setState(state);
            updated = true;
        }

        if (updated) {
            kmsKeyDao.update(key.getId(), key);
            logger.info("Updated KMS key '{}' (UUID: {})", key.getName(), uuid);
        }

        return key;
    }

    /**
     * Unwrap a DEK by wrapped key ID, trying multiple KEK versions if needed
     */
    @Override
    public byte[] unwrapKey(Long wrappedKeyId) throws KMSException {
        KMSWrappedKeyVO wrappedVO = kmsWrappedKeyDao.findById(wrappedKeyId);
        if (wrappedVO == null) {
            throw KMSException.kekNotFound("Wrapped key not found: " + wrappedKeyId);
        }

        KMSKeyVO kmsKey = kmsKeyDao.findById(wrappedVO.getKmsKeyId());
        if (kmsKey == null) {
            throw KMSException.kekNotFound("KMS key not found for wrapped key: " + wrappedKeyId);
        }

        KMSProvider provider = getKMSProvider(kmsKey.getProviderName());

        // Try the specific version first if available
        if (wrappedVO.getKekVersionId() != null) {
            KMSKekVersionVO version = kmsKekVersionDao.findById(wrappedVO.getKekVersionId());
            if (version != null && version.getStatus() != KMSKekVersionVO.Status.Archived) {
                try {
                    WrappedKey wrapped = new WrappedKey(version.getKekLabel(), kmsKey.getPurpose(),
                            kmsKey.getAlgorithm(), wrappedVO.getWrappedBlob(),
                            kmsKey.getProviderName(), wrappedVO.getCreated(), kmsKey.getZoneId());
                    byte[] dek = retryOperation(() -> provider.unwrapKey(wrapped));
                    logger.debug("Successfully unwrapped key {} with KEK version {}", wrappedKeyId,
                            version.getVersionNumber());
                    return dek;
                } catch (Exception e) {
                    logger.warn("Failed to unwrap with version {}: {}", version.getVersionNumber(), e.getMessage());
                }
            }
        }

        // Fallback: try all available versions for decryption
        List<KMSKekVersionVO> versions = getKekVersionsForDecryption(kmsKey.getId());
        for (KMSKekVersionVO version : versions) {
            try {
                WrappedKey wrapped = new WrappedKey(version.getKekLabel(), kmsKey.getPurpose(),
                        kmsKey.getAlgorithm(), wrappedVO.getWrappedBlob(),
                        kmsKey.getProviderName(), wrappedVO.getCreated(), kmsKey.getZoneId());
                byte[] dek = retryOperation(() -> provider.unwrapKey(wrapped));
                logger.info("Successfully unwrapped key {} with KEK version {} (fallback)", wrappedKeyId,
                        version.getVersionNumber());
                return dek;
            } catch (Exception e) {
                logger.debug("Failed to unwrap with version {}: {}", version.getVersionNumber(), e.getMessage());
            }
        }

        throw KMSException.wrapUnwrapFailed("Failed to unwrap key with any available KEK version");
    }

    // ==================== Lifecycle Methods ====================

    /**
     * Get all KEK versions that can be used for decryption (Active and Previous)
     */
    private List<KMSKekVersionVO> getKekVersionsForDecryption(Long kmsKeyId) {
        return kmsKekVersionDao.getVersionsForDecryption(kmsKeyId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEY_WRAP,
            eventDescription = "generating volume key with specified KEK", async = false)
    public WrappedKey generateVolumeKeyWithKek(String kekUuid, Long callerAccountId) throws KMSException {
        // Get and validate KMS key
        KMSKey kmsKey = getUserKMSKey(kekUuid, callerAccountId);
        if (kmsKey == null) {
            throw KMSException.kekNotFound("KMS key not found or no permission: " + kekUuid);
        }

        if (kmsKey.getState() != KMSKey.State.Enabled) {
            throw KMSException.invalidParameter("KMS key is not enabled: " + kekUuid);
        }

        if (kmsKey.getPurpose() != KeyPurpose.VOLUME_ENCRYPTION) {
            throw KMSException.invalidParameter("KMS key purpose is not VOLUME_ENCRYPTION: " + kekUuid);
        }

        // Get provider
        KMSProvider provider = getKMSProviderForZone(kmsKey.getZoneId());

        // Get active KEK version
        KMSKekVersionVO activeVersion = getActiveKekVersion(kmsKey.getId());

        // Generate and wrap DEK using active KEK version
        int dekSize = KMSDekSizeBits.value();
        WrappedKey wrappedKey;
        try {
            wrappedKey = retryOperation(() ->
                    provider.generateAndWrapDek(KeyPurpose.VOLUME_ENCRYPTION, activeVersion.getKekLabel(), dekSize));
            // Store the wrapped key in database
            KMSWrappedKeyVO wrappedKeyVO = new KMSWrappedKeyVO(kmsKey.getId(), activeVersion.getId(),
                    kmsKey.getZoneId(), wrappedKey.getWrappedKeyMaterial());
            wrappedKeyVO = kmsWrappedKeyDao.persist(wrappedKeyVO);

            // Return WrappedKey with database UUID so it can be looked up later
            // Note: Volume creation code should look up by UUID and set volume.kmsWrappedKeyId
            WrappedKey persistedWrappedKey = new WrappedKey(
                    wrappedKeyVO.getUuid(),
                    wrappedKey.getKekId(),
                    wrappedKey.getPurpose(),
                    wrappedKey.getAlgorithm(),
                    wrappedKey.getWrappedKeyMaterial(),
                    wrappedKey.getProviderName(),
                    wrappedKey.getCreated(),
                    wrappedKey.getZoneId()
            );
            wrappedKey = persistedWrappedKey;
        } catch (Exception e) {
            throw handleKmsException(e);
        }

        logger.debug("Generated volume key using KMS key '{}' (UUID: {}) with KEK version {}, wrapped key UUID: {}",
                kmsKey.getName(), kekUuid, activeVersion.getVersionNumber(), wrappedKey.getId());
        return wrappedKey;
    }

    /**
     * Get the active KEK version for a KMS key
     */
    private KMSKekVersionVO getActiveKekVersion(Long kmsKeyId) throws KMSException {
        KMSKekVersionVO activeVersion = kmsKekVersionDao.getActiveVersion(kmsKeyId);
        if (activeVersion == null) {
            throw KMSException.kekNotFound("No active KEK version found for KMS key ID: " + kmsKeyId);
        }
        return activeVersion;
    }

    // ==================== Configurable Implementation ====================

    @Override
    public KMSKeyResponse createKMSKey(CreateKMSKeyCmd cmd) throws KMSException {
        Account caller = CallContext.current().getCallingAccount();
        Account targetAccount = caller;

        // If account/domain specified, validate permissions and resolve account
        if (cmd.getAccountName() != null || cmd.getDomainId() != null) {
            // Only admins and domain admins can create keys for other accounts
            if (!accountManager.isAdmin(caller.getId()) &&
                !accountManager.isDomainAdmin(caller.getId())) {
                throw new ServerApiException(ApiErrorCode.UNAUTHORIZED,
                        "Only admins and domain admins can create keys for other accounts");
            }

            if (cmd.getAccountName() != null && cmd.getDomainId() != null) {
                targetAccount = accountManager.getActiveAccountByName(cmd.getAccountName(), cmd.getDomainId());
                if (targetAccount == null) {
                    throw KMSException.invalidParameter(
                            "Unable to find account " + cmd.getAccountName() + " in domain " + cmd.getDomainId());
                }
                // Check access
                accountManager.checkAccess(caller, null, true, targetAccount);
            } else {
                throw KMSException.invalidParameter("Both accountName and domainId must be specified together");
            }
        }

        // Validate purpose
        KeyPurpose keyPurpose;
        try {
            keyPurpose = KeyPurpose.fromString(cmd.getPurpose());
        } catch (IllegalArgumentException e) {
            throw KMSException.invalidParameter("Invalid purpose: " + cmd.getPurpose() +
                                                ". Valid values: VOLUME_ENCRYPTION, TLS_CERT, CONFIG_SECRET");
        }

        // Validate key bits
        int bits = cmd.getKeyBits();
        if (bits != 128 && bits != 192 && bits != 256) {
            throw KMSException.invalidParameter("Key bits must be 128, 192, or 256");
        }

        // Create the KMS key
        KMSKey kmsKey = createUserKMSKey(
                targetAccount.getId(),
                targetAccount.getDomainId(),
                cmd.getZoneId(),
                cmd.getName(),
                cmd.getDescription(),
                keyPurpose,
                bits
        );

        return responseGenerator.createKMSKeyResponse(kmsKey);
    }

    // ==================== KEK Version Management ====================

    @Override
    public ListResponse<KMSKeyResponse> listKMSKeys(ListKMSKeysCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        if (caller == null) {
            ListResponse<KMSKeyResponse> response = new ListResponse<>();
            response.setResponses(new java.util.ArrayList<>(), 0);
            return response;
        }

        // Parse purpose if provided
        KeyPurpose keyPurpose = null;
        if (cmd.getPurpose() != null) {
            try {
                keyPurpose = KeyPurpose.fromString(cmd.getPurpose());
            } catch (IllegalArgumentException e) {
                // Invalid purpose - will be ignored
            }
        }

        // Parse state if provided
        KMSKey.State keyState = null;
        if (cmd.getState() != null) {
            try {
                keyState = KMSKey.State.valueOf(cmd.getState());
            } catch (IllegalArgumentException e) {
                // Invalid state - will be ignored
            }
        }

        // If specific ID requested
        if (cmd.getId() != null) {
            // Look up key by ID to get UUID
            KMSKeyVO key = kmsKeyDao.findById(cmd.getId());
            if (key == null) {
                // Key not found - return empty list
                ListResponse<KMSKeyResponse> listResponse = new ListResponse<>();
                listResponse.setResponses(new java.util.ArrayList<>(), 0);
                return listResponse;
            }
            KMSKey kmsKey = getUserKMSKey(key.getUuid(), caller.getId());
            List<KMSKeyResponse> responses = new java.util.ArrayList<>();
            if (kmsKey != null && hasPermission(caller.getId(), kmsKey.getUuid())) {
                responses.add(responseGenerator.createKMSKeyResponse(kmsKey));
            }
            ListResponse<KMSKeyResponse> listResponse = new ListResponse<>();
            listResponse.setResponses(responses, responses.size());
            return listResponse;
        }

        // List accessible keys
        List<? extends KMSKey> keys = listUserKMSKeys(
                caller.getId(),
                caller.getDomainId(),
                cmd.getZoneId(),
                keyPurpose,
                keyState
        );

        List<KMSKeyResponse> responses = new java.util.ArrayList<>();
        for (KMSKey key : keys) {
            responses.add(responseGenerator.createKMSKeyResponse(key));
        }

        ListResponse<KMSKeyResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(responses, responses.size());
        return listResponse;
    }

    @Override
    public KMSKeyResponse updateKMSKey(UpdateKMSKeyCmd cmd) throws KMSException {
        Long callerAccountId = CallContext.current().getCallingAccount().getId();

        // Parse state if provided
        KMSKey.State keyState = null;
        if (cmd.getState() != null) {
            try {
                keyState = KMSKey.State.valueOf(cmd.getState());
                if (keyState == KMSKey.State.Deleted) {
                    throw KMSException.invalidParameter("Cannot set state to Deleted. Use deleteKMSKey instead.");
                }
            } catch (IllegalArgumentException e) {
                throw KMSException.invalidParameter(
                        "Invalid state: " + cmd.getState() + ". Valid values: Enabled, Disabled");
            }
        }

        // Look up key by ID to get UUID
        KMSKeyVO key = kmsKeyDao.findById(cmd.getId());
        if (key == null) {
            throw KMSException.kekNotFound("KMS key not found: " + cmd.getId());
        }

        KMSKey updatedKey = updateUserKMSKey(key.getUuid(), callerAccountId,
                cmd.getName(), cmd.getDescription(), keyState);
        return responseGenerator.createKMSKeyResponse(updatedKey);
    }

    @Override
    public SuccessResponse deleteKMSKey(DeleteKMSKeyCmd cmd) throws KMSException {
        Long callerAccountId = CallContext.current().getCallingAccount().getId();

        // Look up key by ID to get UUID
        KMSKeyVO key = kmsKeyDao.findById(cmd.getId());
        if (key == null) {
            throw KMSException.kekNotFound("KMS key not found: " + cmd.getId());
        }

        deleteUserKMSKey(key.getUuid(), callerAccountId);
        SuccessResponse response = new SuccessResponse();
        return response;
    }

    // ==================== User KEK Management ====================

    /**
     * Create a new KEK version for a KMS key
     */
    private KMSKekVersionVO createKekVersion(Long kmsKeyId, String kekLabel, int keyBits) throws KMSException {
        // Get existing versions to determine next version number
        List<KMSKekVersionVO> existingVersions = kmsKekVersionDao.listByKmsKeyId(kmsKeyId);
        int nextVersion = existingVersions.stream()
                                  .mapToInt(KMSKekVersionVO::getVersionNumber)
                                  .max()
                                  .orElse(0) + 1;

        // Mark current active version as Previous
        KMSKekVersionVO currentActive = kmsKekVersionDao.getActiveVersion(kmsKeyId);
        if (currentActive != null) {
            currentActive.setStatus(KMSKekVersionVO.Status.Previous);
            kmsKekVersionDao.update(currentActive.getId(), currentActive);
        }

        // Create new active version
        KMSKekVersionVO newVersion = new KMSKekVersionVO(kmsKeyId, nextVersion, kekLabel,
                KMSKekVersionVO.Status.Active);
        newVersion = kmsKekVersionDao.persist(newVersion);

        logger.info("Created KEK version {} for KMS key {} (label: {})", nextVersion, kmsKeyId, kekLabel);
        return newVersion;
    }

    private void validateKmsEnabled(Long zoneId) throws KMSException {
        if (zoneId == null) {
            throw KMSException.invalidParameter("Zone ID cannot be null");
        }

        if (!isKmsEnabled(zoneId)) {
            throw KMSException.providerNotInitialized(
                    "KMS is not enabled for zone " + zoneId + ". Set kms.enabled=true for this zone.");
        }
    }

    private <T> T retryOperation(KmsOperation<T> operation) throws Exception {
        int maxRetries = KMSRetryCount.value();
        int retryDelay = KMSRetryDelayMs.value();

        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;

                // Check if retryable
                if (e instanceof KMSException && !((KMSException) e).isRetryable()) {
                    throw e;
                }

                if (attempt < maxRetries) {
                    logger.warn("KMS operation failed (attempt {}/{}): {}. Retrying...",
                            attempt + 1, maxRetries + 1, e.getMessage());

                    try {
                        Thread.sleep((long) retryDelay * (attempt + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CloudRuntimeException("Interrupted during retry", ie);
                    }
                } else {
                    logger.error("KMS operation failed after {} attempts", maxRetries + 1);
                }
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new CloudRuntimeException("KMS operation failed with no exception details");
    }

    private KMSException handleKmsException(Exception e) {
        if (e instanceof KMSException) {
            return (KMSException) e;
        }
        return KMSException.transientError("KMS operation failed: " + e.getMessage(), e);
    }

    private KMSProvider getConfiguredKmsProvider() {
        if (configuredKmsProvider != null) {
            return configuredKmsProvider;
        }

        String providerName = KMSProviderPlugin.value();
        if (kmsProviderMap.containsKey(providerName) && kmsProviderMap.get(providerName) != null) {
            configuredKmsProvider = kmsProviderMap.get(providerName);
            return configuredKmsProvider;
        }

        throw new CloudRuntimeException("Failed to find default configured KMS provider plugin: " + providerName);
    }

    public void setKmsProviders(List<KMSProvider> kmsProviders) {
        this.kmsProviders = kmsProviders;
        initializeKmsProviderMap();
    }

    // ==================== API Response Methods ====================

    private void initializeKmsProviderMap() {
        if (kmsProviderMap != null && kmsProviderMap.size() != kmsProviders.size()) {
            for (KMSProvider provider : kmsProviders) {
                kmsProviderMap.put(provider.getProviderName().toLowerCase(), provider);
                logger.info("Registered KMS provider: {}", provider.getProviderName());
            }
        }
    }

    @Override
    public boolean start() {
        super.start();
        initializeKmsProviderMap();

        String configuredProviderName = KMSProviderPlugin.value();
        if (kmsProviderMap.containsKey(configuredProviderName)) {
            configuredKmsProvider = kmsProviderMap.get(configuredProviderName);
            logger.info("Configured KMS provider: {}", configuredKmsProvider.getProviderName());
        }

        if (configuredKmsProvider == null) {
            logger.warn("No valid configured KMS provider found. KMS functionality will be unavailable.");
            // Don't fail - KMS is optional
            return true;
        }

        // Run health check on startup
        try {
            boolean healthy = configuredKmsProvider.healthCheck();
            if (healthy) {
                logger.info("KMS provider {} health check passed", configuredKmsProvider.getProviderName());
            } else {
                logger.warn("KMS provider {} health check failed", configuredKmsProvider.getProviderName());
            }
        } catch (Exception e) {
            logger.warn("KMS provider health check error: {}", e.getMessage());
        }

        return true;
    }

    @Override
    public String getConfigComponentName() {
        return KMSManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                KMSProviderPlugin,
                KMSEnabled,
                KMSDekSizeBits,
                KMSRetryCount,
                KMSRetryDelayMs,
                KMSOperationTimeoutSec
        };
    }

    @Override
    public List<Class<?>> getCommands() {
         List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ListKMSKeysCmd.class);
        cmdList.add(CreateKMSKeyCmd.class);
        cmdList.add(UpdateKMSKeyCmd.class);
        cmdList.add(DeleteKMSKeyCmd.class);

        return cmdList;
    }

    @FunctionalInterface
    private interface KmsOperation<T> {
        T execute() throws Exception;
    }
}

