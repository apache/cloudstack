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
import com.cloud.utils.EnumUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.kms.MigrateVolumesToKMSCmd;
import org.apache.cloudstack.api.command.admin.kms.RotateKMSKeyCmd;
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
import org.apache.cloudstack.secret.PassphraseVO;
import org.apache.cloudstack.secret.dao.PassphraseDao;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private PassphraseDao passphraseDao;
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

        KMSProvider provider = getKMSProviderForZone(zoneId);

        // Check if any wrapped keys use this KEK
        KMSKeyVO key = kmsKeyDao.findByKekLabel(kekId, provider.getProviderName());
        if (key != null) {
            long wrappedKeyCount = kmsKeyDao.countWrappedKeysByKmsKey(key.getId());
            if (wrappedKeyCount > 0) {
                throw KMSException.invalidParameter("Cannot delete KEK: " + wrappedKeyCount +
                        " wrapped key(s) still reference the corresponding KMS key");
            }
        }

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
                    kmsKey, newVersion.getVersionNumber(), newVersion.getVersionNumber(),
                    newVersion.getVersionNumber() - 1);

            // Schedule background job to rewrap all DEKs
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

        logger.info("Created KMS key ({}) with initial KEK version {} for account {} in zone {}",
                kmsKey, initialVersion.getVersionNumber(), accountId, zoneId);
        return kmsKey;
    }

    @Override
    public List<? extends KMSKey> listUserKMSKeys(Long accountId, Long domainId, Long zoneId,
            KeyPurpose purpose, KMSKey.State state) {
        return kmsKeyDao.listAccessibleKeys(accountId, domainId, zoneId, purpose, state);
    }

    @Override
    public KMSKey getUserKMSKey(String uuid, Long callerAccountId) {
        KMSKeyVO key = kmsKeyDao.findByUuid(uuid);
        if (key == null || key.getState() == KMSKey.State.Deleted) {
            return null;
        }

        if (!hasPermission(callerAccountId, key)) {
            return null;
        }
        return key;
    }

    @Override
    public boolean hasPermission(Long callerAccountId, KMSKey key) {
        if (key == null || key.getState() == KMSKey.State.Deleted) {
            return false;
        }

        if (key.getAccountId() == callerAccountId) {
            return true;
        }

        Account caller = accountManager.getAccount(callerAccountId);
        Account owner = accountManager.getAccount(key.getAccountId());

        if (caller == null || owner == null) {
            return false;
        }

        try {
            accountManager.checkAccess(caller, null, true, owner);
            return true;
        } catch (PermissionDeniedException e) {
            return false;
        }
    }

    private void deleteUserKMSKey(KMSKeyVO key, Long callerAccountId) throws KMSException {
        if (!hasPermission(callerAccountId, key)) {
            throw KMSException.invalidParameter("No permission to delete KMS key: " + key.getUuid());
        }

        // Check if key is in use

        // TODO: Check if there are any volumes linked with the kms key and delete accordingly.
        // The below check seems incorrect here.
        long wrappedKeyCount = kmsKeyDao.countWrappedKeysByKmsKey(key.getId());
        if (wrappedKeyCount > 0) {
            throw KMSException.invalidParameter("Cannot delete KMS key: " + wrappedKeyCount +
                                                " wrapped key(s) still reference this key");
        }

        // Soft delete
        key.setState(KMSKey.State.Deleted);
        key.setRemoved(new Date());
        kmsKeyDao.update(key.getId(), key);

        logger.info("Deleted KMS key {}", key);
    }

    private KMSKey updateUserKMSKey(KMSKeyVO key, Long callerAccountId,
            String name, String description, KMSKey.State state) throws KMSException {
        if (!hasPermission(callerAccountId, key)) {
            throw KMSException.invalidParameter("No permission to update KMS key: " + key.getUuid());
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
            logger.info("Updated KMS key {}", key);
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
        List<KMSKekVersionVO> versions = kmsKekVersionDao.getVersionsForDecryption(kmsKey.getId());
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

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEY_WRAP,
            eventDescription = "generating volume key with specified KEK", async = false)
    public WrappedKey generateVolumeKeyWithKek(KMSKey kmsKey, Long callerAccountId) throws KMSException {
        // Get and validate KMS key
        if (kmsKey == null) {
            throw KMSException.kekNotFound("KMS key not found");
        }

        if (kmsKey.getState() != KMSKey.State.Enabled) {
            throw KMSException.invalidParameter("KMS key is not enabled: " + kmsKey);
        }

        if (kmsKey.getPurpose() != KeyPurpose.VOLUME_ENCRYPTION) {
            throw KMSException.invalidParameter("KMS key purpose is not VOLUME_ENCRYPTION: " + kmsKey);
        }

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
            wrappedKey = new WrappedKey(
                    wrappedKeyVO.getUuid(),
                    wrappedKey.getKekId(),
                    wrappedKey.getPurpose(),
                    wrappedKey.getAlgorithm(),
                    wrappedKey.getWrappedKeyMaterial(),
                    wrappedKey.getProviderName(),
                    wrappedKey.getCreated(),
                    wrappedKey.getZoneId()
            );
        } catch (Exception e) {
            throw handleKmsException(e);
        }

        logger.debug("Generated volume key using KMS key {} with KEK version {}, wrapped key UUID: {}",
                kmsKey, activeVersion.getVersionNumber(), wrappedKey.getUuid());
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
                                                ". Valid values: volume, tls");
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
            return createEmptyListResponse();
        }

        KeyPurpose keyPurpose = null;
        if (cmd.getPurpose() != null) {
            try {
                keyPurpose = KeyPurpose.fromString(cmd.getPurpose());
            } catch (IllegalArgumentException e) {
                throw KMSException.invalidParameter("Invalid purpose: " + cmd.getPurpose() + ". Valid values: volume, tls");
            }
        }

        KMSKey.State keyState = null;
        if (cmd.getState() != null) {
            keyState = EnumUtils.getEnumIgnoreCase(KMSKey.State.class, cmd.getState());
            if (keyState == null) {
                throw KMSException.invalidParameter("Invalid state: " + cmd.getState() + ". Valid values: Enabled, Disabled");
            }
        }

        if (cmd.getId() != null) {
            KMSKeyVO key = kmsKeyDao.findById(cmd.getId());
            if (key == null || key.getState() == KMSKey.State.Deleted) {
                return createEmptyListResponse();
            }

            if (hasPermission(caller.getId(), key)) {
                List<KMSKeyResponse> responses = new ArrayList<>();
                responses.add(responseGenerator.createKMSKeyResponse(key));
                ListResponse<KMSKeyResponse> listResponse = new ListResponse<>();
                listResponse.setResponses(responses, responses.size());
                return listResponse;
            }
            return createEmptyListResponse();
        }

        List<? extends KMSKey> keys = listUserKMSKeys(
                caller.getId(),
                caller.getDomainId(),
                cmd.getZoneId(),
                keyPurpose,
                keyState
        );

        List<KMSKeyResponse> responses = new ArrayList<>();
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

        KMSKey.State keyState = null;
        if (cmd.getState() != null) {
            keyState = EnumUtils.getEnumIgnoreCase(KMSKey.State.class, cmd.getState());
            if (keyState == KMSKey.State.Deleted) {
                throw KMSException.invalidParameter("Cannot set state to Deleted. Use deleteKMSKey instead.");
            }
            if (keyState == null) {
                throw KMSException.invalidParameter("Invalid state: " + cmd.getState() + ". Valid values: Enabled, Disabled");
            }
        }

        KMSKeyVO key = kmsKeyDao.findById(cmd.getId());
        if (key == null) {
            throw KMSException.kekNotFound("KMS key not found: " + cmd.getId());
        }

        KMSKey updatedKey = updateUserKMSKey(key, callerAccountId,
                cmd.getName(), cmd.getDescription(), keyState);
        return responseGenerator.createKMSKeyResponse(updatedKey);
    }

    @Override
    public SuccessResponse deleteKMSKey(DeleteKMSKeyCmd cmd) throws KMSException {
        Long callerAccountId = CallContext.current().getCallingAccount().getId();

        KMSKeyVO key = kmsKeyDao.findById(cmd.getId());
        if (key == null) {
            throw KMSException.kekNotFound("KMS key not found: " + cmd.getId());
        }

        deleteUserKMSKey(key, callerAccountId);
        return new SuccessResponse();
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

    // ==================== Admin Operations ====================

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEK_ROTATE, eventDescription = "rotating KMS key", async = true)
    public String rotateKMSKey(RotateKMSKeyCmd cmd) throws KMSException {
        Integer keyBits = cmd.getKeyBits();

        KMSKeyVO kmsKey = kmsKeyDao.findById(cmd.getId());
        if (kmsKey == null) {
            throw KMSException.kekNotFound("KMS key not found: " + cmd.getId());
        }

        if (kmsKey.getState() != KMSKey.State.Enabled) {
            throw KMSException.invalidParameter("KMS key is not enabled: " + kmsKey);
        }

        // Get current active version to determine key bits if not provided
        int newKeyBits = keyBits != null ? keyBits : kmsKey.getKeyBits();
        KMSKekVersionVO currentActive = getActiveKekVersion(kmsKey.getId());

        rotateKek(
                kmsKey.getZoneId(),
                kmsKey.getPurpose(),
                currentActive.getKekLabel(),
                null, // auto-generate new label
                newKeyBits
        );

        KMSKekVersionVO newVersion = getActiveKekVersion(kmsKey.getId());

        logger.info("KMS key rotation completed: {} -> new KEK version {} (UUID: {})",
                kmsKey, newVersion.getVersionNumber(), newVersion.getUuid());

        // Perform rewrapping of existing wrapped keys
        // This runs within the async job context
        rewrapWrappedKeysForKMSKey(kmsKey.getId(), newVersion.getId(), 50);

        return newVersion.getUuid();
    }

    @Override
    public int rewrapWrappedKeysForKMSKey(Long kmsKeyId, Long newKekVersionId, int batchSize) throws KMSException {
        if (kmsKeyId == null || newKekVersionId == null) {
            throw KMSException.invalidParameter("kmsKeyId and newKekVersionId must be specified");
        }

        if (batchSize <= 0) {
            batchSize = 50; // Default batch size
        }

        // Get KMS key and new version
        KMSKeyVO kmsKey = kmsKeyDao.findById(kmsKeyId);
        if (kmsKey == null) {
            throw KMSException.kekNotFound("KMS key not found: " + kmsKeyId);
        }

        KMSKekVersionVO newVersion = kmsKekVersionDao.findById(newKekVersionId);
        if (newVersion == null || !newVersion.getKmsKeyId().equals(kmsKeyId)) {
            throw KMSException.kekNotFound("KEK version not found or doesn't belong to KMS key: " + newKekVersionId);
        }

        KMSProvider provider = getKMSProviderForZone(kmsKey.getZoneId());

        // Get all wrapped keys that need rewrap
        List<KMSWrappedKeyVO> wrappedKeys = kmsWrappedKeyDao.listWrappedKeysForRewrap(kmsKeyId, newKekVersionId);
        int totalKeys = wrappedKeys.size();
        int successCount = 0;
        int failureCount = 0;

        logger.info("Starting rewrap operation for {} wrapped keys (KMS key: {}, new version: {})",
                totalKeys, kmsKey, newKekVersionId);

        for (int i = 0; i < wrappedKeys.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, wrappedKeys.size());
            List<KMSWrappedKeyVO> batch = wrappedKeys.subList(i, endIndex);

            for (KMSWrappedKeyVO wrappedKeyVO : batch) {
                byte[] dek = null;
                try {
                    // Unwrap with old version
                    dek = unwrapKey(wrappedKeyVO.getId());

                    // Wrap the existing DEK with new active version
                    WrappedKey newWrapped = provider.wrapKey(
                            dek,
                            kmsKey.getPurpose(),
                            newVersion.getKekLabel()
                    );

                    wrappedKeyVO.setKekVersionId(newKekVersionId);
                    wrappedKeyVO.setWrappedBlob(newWrapped.getWrappedKeyMaterial());
                    kmsWrappedKeyDao.update(wrappedKeyVO.getId(), wrappedKeyVO);

                    successCount++;
                    logger.debug("Rewrapped key {} (batch {}/{})", wrappedKeyVO.getId(),
                            (i / batchSize) + 1, (totalKeys + batchSize - 1) / batchSize);
                } catch (Exception e) {
                    failureCount++;
                    logger.warn("Failed to rewrap key {}: {}", wrappedKeyVO.getId(), e.getMessage());
                } finally {
                    // Zeroize DEK
                    if (dek != null) {
                        Arrays.fill(dek, (byte) 0);
                    }
                }
            }

            logger.info("Processed batch {}/{}: {} success, {} failures",
                    (i / batchSize) + 1, (totalKeys + batchSize - 1) / batchSize, successCount, failureCount);
        }

        // Archive old versions if no wrapped keys reference them
        List<KMSKekVersionVO> oldVersions = kmsKekVersionDao.getVersionsForDecryption(kmsKeyId);
        for (KMSKekVersionVO oldVersion : oldVersions) {
            if (oldVersion.getStatus() == KMSKekVersionVO.Status.Previous) {
                List<KMSWrappedKeyVO> keysUsingVersion = kmsWrappedKeyDao.listByKekVersionId(oldVersion.getId());
                if (keysUsingVersion.isEmpty()) {
                    oldVersion.setStatus(KMSKekVersionVO.Status.Archived);
                    kmsKekVersionDao.update(oldVersion.getId(), oldVersion);
                    logger.info("Archived KEK version {} (no wrapped keys using it)", oldVersion.getVersionNumber());
                }
            }
        }

        logger.info("Rewrap operation completed: {} success, {} failures out of {} total",
                successCount, failureCount, totalKeys);

        return successCount;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_MIGRATE_TO_KMS, eventDescription = "migrating volumes to KMS", async = true)
    public int migrateVolumesToKMS(MigrateVolumesToKMSCmd cmd) throws KMSException {
        Long zoneId = cmd.getZoneId();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();

        if (zoneId == null) {
            throw KMSException.invalidParameter("zoneId must be specified");
        }

        validateKmsEnabled(zoneId);

        Long accountId = null;
        if (accountName != null) {
            accountId = accountManager.finalyzeAccountId(accountName, domainId, null, true);
        }

        int pageSize = 100; // Process 100 volumes per page to avoid OutOfMemoryError

        // Get provider
        KMSProvider provider = getKMSProviderForZone(zoneId);

        int successCount = 0;
        int failureCount = 0;
        logger.info("Starting migration of volumes to KMS (zone: {}, account: {}, domain: {})",
                zoneId, accountId, domainId);

        Pair<List<VolumeVO>, Integer> volumeListPair = volumeDao.listVolumesForKMSMigration(zoneId, accountId, domainId, pageSize);
        List<VolumeVO> volumes = volumeListPair.first();
        int totalCount = volumeListPair.second();

        while (true) {

            if (CollectionUtils.isEmpty(volumes) || totalCount == 0) {
                break;
            }

            for (VolumeVO volume : volumes) {
                try {
                    // Load passphrase
                    PassphraseVO passphrase = passphraseDao.findById(volume.getPassphraseId());
                    if (passphrase == null) {
                        logger.warn("Passphrase not found for volume {}: {}", volume.getId(), volume.getPassphraseId());
                        failureCount++;
                        continue;
                    }

                    // Get passphrase bytes
                    // Note: PassphraseVO.getPassphrase() returns Base64-encoded bytes
                    // This is consistent with how hypervisors (KVM/QEMU) expect the key format
                    // The KMS will store the same format, maintaining compatibility
                    byte[] passphraseBytes = passphrase.getPassphrase();

                    // Get or create KMS key for account
                    KMSKeyVO kmsKey = null;
                    List<? extends KMSKey> accountKeys = listUserKMSKeys(
                            volume.getAccountId(),
                            volume.getDomainId(),
                            zoneId,
                            KeyPurpose.VOLUME_ENCRYPTION,
                            KMSKey.State.Enabled
                    );

                    if (!accountKeys.isEmpty()) {
                        kmsKey = (KMSKeyVO) accountKeys.get(0); // Use first available key
                    } else {
                        // Create new KMS key for account
                        String keyName = "Volume-Encryption-Key-" + volume.getAccountId();
                        kmsKey = (KMSKeyVO) createUserKMSKey(
                                volume.getAccountId(),
                                volume.getDomainId(),
                                zoneId,
                                keyName,
                                "Auto-created for volume migration",
                                KeyPurpose.VOLUME_ENCRYPTION,
                                256 // Default to 256 bits
                        );
                        logger.info("Created KMS key {} for account {} during migration", kmsKey, volume.getAccountId());
                    }

                    // Get active KEK version
                    KMSKekVersionVO activeVersion = getActiveKekVersion(kmsKey.getId());

                    // Wrap existing passphrase bytes as DEK (don't generate new DEK)
                    WrappedKey wrappedKey = provider.wrapKey(
                            passphraseBytes,
                            KeyPurpose.VOLUME_ENCRYPTION,
                            activeVersion.getKekLabel()
                    );

                    // Store wrapped key
                    KMSWrappedKeyVO wrappedKeyVO = new KMSWrappedKeyVO(
                            kmsKey.getId(),
                            activeVersion.getId(),
                            zoneId,
                            wrappedKey.getWrappedKeyMaterial()
                    );
                    wrappedKeyVO = kmsWrappedKeyDao.persist(wrappedKeyVO);

                    // Update volume
                    volume.setKmsWrappedKeyId(wrappedKeyVO.getId());
                    volume.setKmsKeyId(kmsKey.getId());
                    volume.setPassphraseId(null); // Clear passphrase reference
                    volumeDao.update(volume.getId(), volume);

                    // Zeroize passphrase bytes
                    if (passphraseBytes != null) {
                        Arrays.fill(passphraseBytes, (byte) 0);
                    }

                    successCount++;
                    logger.debug("Migrated volume's encryption {} to KMS (batch {})", volume, kmsKey);
                } catch (Exception e) {
                    failureCount++;
                    logger.warn("Failed to migrate volume {}: {}", volume.getId(), e.getMessage());
                    // Continue with next volume
                }
            }

            logger.debug("Processed {} volumes. success: {}, failure: {}", volumes.size(),
                    successCount, failureCount);
            volumeListPair = volumeDao.listVolumesForKMSMigration(zoneId, accountId, domainId, pageSize);
            volumes = volumeListPair.first();
            if (totalCount == volumeListPair.second()) {
                logger.debug("{} volumes pending for migration because passphrase was not found or migration failed", totalCount);
                break;
            }
            totalCount = volumeListPair.second();
        }
        logger.info("Migration operation completed: {} total volumes processed, {} success, {} failures",
                successCount + failureCount, successCount, failureCount);

        return successCount;
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
                        Thread.sleep((long) retryDelay * (attempt + 1));
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
        String providerKey = providerName != null ? providerName.toLowerCase() : null;
        if (providerKey != null && kmsProviderMap.containsKey(providerKey) && kmsProviderMap.get(providerKey) != null) {
            configuredKmsProvider = kmsProviderMap.get(providerKey);
            return configuredKmsProvider;
        }

        throw new CloudRuntimeException("Failed to find default configured KMS provider plugin: " + providerName);
    }

    public void setKmsProviders(List<KMSProvider> kmsProviders) {
        this.kmsProviders = kmsProviders;
        initializeKmsProviderMap();
    }

    // ==================== API Response Methods ====================

    /**
     * Helper method to create an empty list response
     */
    private ListResponse<KMSKeyResponse> createEmptyListResponse() {
        ListResponse<KMSKeyResponse> response = new ListResponse<>();
        response.setResponses(new ArrayList<>(), 0);
        return response;
    }

    private void initializeKmsProviderMap() {
        if (kmsProviders == null) {
            return;
        }
        kmsProviderMap.clear();
        for (KMSProvider provider : kmsProviders) {
            if (provider != null) {
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
        String providerKey = configuredProviderName != null ? configuredProviderName.toLowerCase() : null;
        if (providerKey != null && kmsProviderMap.containsKey(providerKey)) {
            configuredKmsProvider = kmsProviderMap.get(providerKey);
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
        cmdList.add(RotateKMSKeyCmd.class);
        cmdList.add(MigrateVolumesToKMSCmd.class);

        return cmdList;
    }

    @FunctionalInterface
    private interface KmsOperation<T> {
        T execute() throws Exception;
    }
}
