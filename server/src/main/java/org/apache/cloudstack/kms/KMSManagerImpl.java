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
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
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
        // In the future, could support zone-specific providers via zone-scoped config
        return getConfiguredKmsProvider();
    }

    @Override
    public boolean isKmsEnabled(Long zoneId) {
        if (zoneId == null) {
            return false;
        }
        return KMSEnabled.valueIn(zoneId);
    }

    /**
     * Internal method to rotate a KEK (create new version and update KMS key state)
     */
    private String rotateKek(Long zoneId, KeyPurpose purpose, String oldKekLabel,
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
            String finalNewKekLabel = newKekLabel;
            String newKekId = retryOperation(() -> provider.createKek(purpose, finalNewKekLabel, keyBits));

            // Create new KEK version (marks old as Previous, new as Active)
            KMSKekVersionVO newVersion = createKekVersion(kmsKey.getId(), newKekId);

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
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEY_UNWRAP, eventDescription = "unwrapping volume key")
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
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEK_CREATE, eventDescription = "creating user KMS key")
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
            eventDescription = "generating volume key with specified KEK")
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
    private KMSKekVersionVO createKekVersion(Long kmsKeyId, String kekLabel) throws KMSException {
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

        logger.info("KMS key rotation initiated: {} -> new KEK version {} (UUID: {}). " +
                        "Background job will gradually rewrap {} wrapped key(s)",
                kmsKey, newVersion.getVersionNumber(), newVersion.getUuid(),
                kmsWrappedKeyDao.countByKmsKeyId(kmsKey.getId()));

        // Background KMSRewrapWorker will automatically detect Previous versions
        // and gradually rewrap wrapped keys in batches

        return newVersion.getUuid();
    }

    /**
     * Helper method to rewrap a single wrapped key with a new KEK version.
     * Unwraps the key, re-wraps it with the new KEK, and updates the database.
     *
     * @param wrappedKeyVO the wrapped key to rewrap
     * @param kmsKey the KMS key
     * @param newVersion the new KEK version to wrap with
     * @param provider the KMS provider
     */
    private void rewrapSingleKey(KMSWrappedKeyVO wrappedKeyVO, KMSKeyVO kmsKey,
                                  KMSKekVersionVO newVersion, KMSProvider provider) {
        byte[] dek = null;
        try {
            // Unwrap with current/old version
            dek = unwrapKey(wrappedKeyVO.getId());

            // Wrap the existing DEK with new KEK version
            WrappedKey newWrapped = provider.wrapKey(
                    dek,
                    kmsKey.getPurpose(),
                    newVersion.getKekLabel()
            );

            wrappedKeyVO.setKekVersionId(newVersion.getId());
            wrappedKeyVO.setWrappedBlob(newWrapped.getWrappedKeyMaterial());
            kmsWrappedKeyDao.update(wrappedKeyVO.getId(), wrappedKeyVO);
        } finally {
            // Always zeroize DEK from memory
            if (dek != null) {
                Arrays.fill(dek, (byte) 0);
            }
        }
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
                    KMSKeyVO kmsKey;
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

                    // zeroize passphrase bytes
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

        // Schedule background rewrap worker
        scheduleRewrapWorker();

        return true;
    }

    /**
     * Schedule the background KEK rewrap worker
     */
    private void scheduleRewrapWorker() {
        final ManagedContextTimerTask rewrapTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    processRewrapBatch();
                } catch (final Exception e) {
                    logger.error("Error while running KMS rewrap worker", e);
                }
            }
        };

        long intervalMs = KMSRewrapIntervalMs.value();
        Timer rewrapTimer = new Timer("KMSRewrapWorker");
        rewrapTimer.schedule(rewrapTask, 10000L, intervalMs); // Start after 10 seconds, run at configured interval
        logger.info("KMS rewrap worker scheduled with interval: {} ms", intervalMs);
    }

    /**
     * Background worker method that processes KEK rewrap batches.
     * Finds KEK versions marked as Previous and gradually rewraps wrapped keys
     * using the active version.
     */
    private void processRewrapBatch() {
        try {
            // Find all KEK versions marked as Previous (rotation in progress)
            List<KMSKekVersionVO> previousVersions = kmsKekVersionDao.findByStatus(KMSKekVersionVO.Status.Previous);

            if (previousVersions.isEmpty()) {
                logger.trace("No KEK versions pending rewrap");
                return;
            }

            logger.debug("Found {} KEK version(s) with status Previous - processing rewrap batches", previousVersions.size());

            int batchSize = KMSRewrapBatchSize.value();

            for (KMSKekVersionVO oldVersion : previousVersions) {
                try {
                    processVersionRewrap(oldVersion, batchSize);
                } catch (Exception e) {
                    logger.error("Error processing rewrap for KEK version {}: {}", oldVersion, e.getMessage(), e);
                    // Continue with next version
                }
            }
        } catch (Exception e) {
            logger.error("Error in rewrap worker: {}", e.getMessage(), e);
        }
    }

    /**
     * Process rewrap for a single KEK version (used by background worker)
     */
    private void processVersionRewrap(KMSKekVersionVO oldVersion, int batchSize) throws KMSException {
        KMSKeyVO kmsKey = kmsKeyDao.findById(oldVersion.getKmsKeyId());
        if (kmsKey == null) {
            logger.warn("KMS key not found for KEK version {}, skipping", oldVersion);
            return;
        }

        // Get active version for this KMS key
        KMSKekVersionVO activeVersion = kmsKekVersionDao.getActiveVersion(oldVersion.getKmsKeyId());
        if (activeVersion == null) {
            logger.warn("No active KEK version found for KMS key {}, skipping", kmsKey);
            return;
        }

        // Query wrapped keys still using the old version (limited to batch size)
        List<KMSWrappedKeyVO> keysToRewrap = kmsWrappedKeyDao.listByKekVersionId(oldVersion.getId(), batchSize);

        if (keysToRewrap.isEmpty()) {
            // All keys rewrapped - archive the old version
            logger.info("All wrapped keys rewrapped for KEK version {} (v{}) - archiving",
                    oldVersion.getUuid(), oldVersion.getVersionNumber());

            oldVersion.setStatus(KMSKekVersionVO.Status.Archived);
            kmsKekVersionDao.update(oldVersion.getId(), oldVersion);

            return;
        }

        // Get provider
        KMSProvider provider = getKMSProviderForZone(kmsKey.getZoneId());

        // Rewrap this batch using the common helper
        int successCount = 0;
        int failureCount = 0;

        for (KMSWrappedKeyVO wrappedKeyVO : keysToRewrap) {
            try {
                rewrapSingleKey(wrappedKeyVO, kmsKey, activeVersion, provider);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                logger.warn("Failed to rewrap key {} for KMS key {}: {}",
                        wrappedKeyVO.getId(), kmsKey, e.getMessage());
                // Continue with next key - will retry in next run
            }
        }

        logger.info("Rewrapped batch for KMS key {} (KEK v{} -> v{}): {} success, {} failures",
                kmsKey, oldVersion.getVersionNumber(), activeVersion.getVersionNumber(),
                successCount, failureCount);
    }

    @Override
    public boolean deleteKMSKeysByAccountId(Long accountId) {
        if (accountId == null) {
            logger.warn("Cannot delete KMS keys: account ID is null");
            return false;
        }

        try {
            // List all KMS keys owned by this account
            List<KMSKeyVO> accountKeys = kmsKeyDao.listByAccount(accountId, null, null);

            if (accountKeys == null || accountKeys.isEmpty()) {
                logger.debug("No KMS keys found for account {}", accountId);
                return true;
            }

            logger.info("Deleting {} KMS key(s) for account {}", accountKeys.size(), accountId);

            boolean allDeleted = true;
            for (KMSKeyVO key : accountKeys) {
                try {
                    KMSProvider provider = getKMSProviderForZone(key.getZoneId());

                    // Step 1: Delete all KEKs from the provider first
                    List<KMSKekVersionVO> kekVersions = kmsKekVersionDao.listByKmsKeyId(key.getId());
                    if (kekVersions != null && !kekVersions.isEmpty()) {
                        logger.debug("Deleting {} KEK version(s) from provider for KMS key {}",
                                kekVersions.size(), key.getUuid());
                        for (KMSKekVersionVO kekVersion : kekVersions) {
                            try {
                                provider.deleteKek(kekVersion.getKekLabel());
                                logger.debug("Deleted KEK {} (v{}) from provider",
                                        kekVersion.getKekLabel(), kekVersion.getVersionNumber());
                            } catch (Exception e) {
                                logger.warn("Failed to delete KEK {} from provider: {}",
                                        kekVersion.getKekLabel(), e.getMessage());
                                // Continue - still delete from database even if provider deletion fails
                            }
                        }
                    }

                    // Step 2: Delete the KMS key from database
                    // This will CASCADE delete:
                    //   - KEK versions (kms_kek_versions)
                    //   - Wrapped keys (kms_wrapped_key)
                    boolean deleted = kmsKeyDao.remove(key.getId());
                    if (deleted) {
                        logger.debug("Deleted KMS key {} as part of account {} cleanup", key.getUuid(), accountId);
                    } else {
                        logger.warn("Failed to delete KMS key {} as part of account {} cleanup",
                                key.getUuid(), accountId);
                        allDeleted = false;
                    }
                } catch (Exception e) {
                    logger.error("Error deleting KMS key {} for account {}: {}",
                            key.getUuid(), accountId, e.getMessage(), e);
                    allDeleted = false;
                }
            }

            if (allDeleted) {
                logger.info("Successfully deleted all KMS keys for account {}", accountId);
            } else {
                logger.warn("Some KMS keys for account {} could not be deleted", accountId);
            }

            return allDeleted;
        } catch (Exception e) {
            logger.error("Error during KMS key cleanup for account {}: {}", accountId, e.getMessage(), e);
            return false;
        }
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
                KMSOperationTimeoutSec,
                KMSRewrapBatchSize,
                KMSRewrapIntervalMs
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
